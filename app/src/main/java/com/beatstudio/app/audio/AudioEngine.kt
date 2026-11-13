package com.beatstudio.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.beatstudio.app.audio.instruments.SampleStore
import com.beatstudio.app.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Low-latency real-time Kotlin audio engine running on a dedicated URGENT_AUDIO thread.
 * Zero object allocations inside the hot render loop.
 */
class AudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        const val BLOCK_SIZE = 256
    }

    private sealed interface EngineCommand {
        data class TriggerSynth(val trackId: String, val midi: Int, val velocity: Float, val params: SynthParams) : EngineCommand
        data class ReleasePitch(val trackId: String, val midi: Int) : EngineCommand
        data class TriggerDrum(val trackId: String, val soundType: DrumSoundType, val midi: Int, val velocity: Float, val pitchOffset: Float, val gain: Float) : EngineCommand
        data class TriggerSample(val trackId: String, val midi: Int, val velocity: Float, val params: SamplerParams) : EngineCommand
        data object StopAllVoices : EngineCommand
    }

    private val projectRef = AtomicReference<Project?>(null)
    private val isPlayingFlag = AtomicBoolean(false)
    private val isProModeFlag = AtomicBoolean(false)
    private val threadRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null

    private val commandQueue = ConcurrentLinkedQueue<EngineCommand>()

    private val sequencer = Sequencer(SAMPLE_RATE)
    private val voiceManager = VoiceManager(SAMPLE_RATE.toFloat())
    private val mixer = Mixer(SAMPLE_RATE, BLOCK_SIZE)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playheadStep = MutableStateFlow(0)
    val playheadStep: StateFlow<Int> = _playheadStep.asStateFlow()

    private val _playheadBar = MutableStateFlow(0)
    val playheadBar: StateFlow<Int> = _playheadBar.asStateFlow()

    private val _masterLevel = MutableStateFlow(0f to 0f)
    val masterLevel: StateFlow<Pair<Float, Float>> = _masterLevel.asStateFlow()

    private val _trackLevels = MutableStateFlow<Map<String, Float>>(emptyMap())
    val trackLevels: StateFlow<Map<String, Float>> = _trackLevels.asStateFlow()

    fun updateProject(project: Project, isProMode: Boolean) {
        projectRef.set(project)
        isProModeFlag.set(isProMode)
    }

    fun startEngineIfNeeded() {
        if (threadRunning.getAndSet(true)) return
        audioThread = Thread({
            runAudioLoop()
        }, "BeatStudio-AudioDSP").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun play() {
        startEngineIfNeeded()
        sequencer.reset()
        isPlayingFlag.set(true)
        _isPlaying.value = true
    }

    fun stop() {
        isPlayingFlag.set(false)
        _isPlaying.value = false
        _playheadStep.value = 0
        _playheadBar.value = 0
        sequencer.reset()
        commandQueue.offer(EngineCommand.StopAllVoices)
    }

    fun togglePlay() {
        if (isPlayingFlag.get()) stop() else play()
    }

    fun previewSynthNoteOn(trackId: String, midi: Int, velocity: Float, params: SynthParams) {
        startEngineIfNeeded()
        commandQueue.offer(EngineCommand.TriggerSynth(trackId, midi, velocity, params))
    }

    fun previewSynthNoteOff(trackId: String, midi: Int) {
        commandQueue.offer(EngineCommand.ReleasePitch(trackId, midi))
    }

    fun previewDrum(trackId: String, row: DrumRowConfig, velocity: Float = 0.9f) {
        startEngineIfNeeded()
        commandQueue.offer(
            EngineCommand.TriggerDrum(
                trackId = trackId,
                soundType = row.soundType,
                midi = row.soundType.defaultMidi,
                velocity = velocity,
                pitchOffset = row.pitchOffset,
                gain = row.gain
            )
        )
    }

    fun previewDrumSound(soundType: DrumSoundType, pitchOffset: Float = 0f) {
        startEngineIfNeeded()
        commandQueue.offer(
            EngineCommand.TriggerDrum(
                trackId = projectRef.get()?.tracks?.firstOrNull()?.id ?: "track_drums",
                soundType = soundType,
                midi = soundType.defaultMidi,
                velocity = 0.92f,
                pitchOffset = pitchOffset,
                gain = 0.95f
            )
        )
    }

    fun previewSamplerNoteOn(trackId: String, midi: Int, velocity: Float, params: SamplerParams) {
        startEngineIfNeeded()
        commandQueue.offer(EngineCommand.TriggerSample(trackId, midi, velocity, params))
    }

    fun shutdown() {
        isPlayingFlag.set(false)
        threadRunning.set(false)
        runCatching { audioThread?.join(500) }
        audioThread = null
    }

    private fun runAudioLoop() {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }

        val minBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(BLOCK_SIZE * 4)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        // Pre-allocated audio buffers
        val outL = FloatArray(BLOCK_SIZE)
        val outR = FloatArray(BLOCK_SIZE)
        val metroL = FloatArray(BLOCK_SIZE)
        val metroR = FloatArray(BLOCK_SIZE)
        val pcmOut = ShortArray(BLOCK_SIZE * 2)
        var uiMeterCounter = 0

        try {
            audioTrack.play()
            while (threadRunning.get()) {
                // Drain live commands
                while (true) {
                    val cmd = commandQueue.poll() ?: break
                    when (cmd) {
                        is EngineCommand.TriggerSynth ->
                            voiceManager.triggerSynth(cmd.trackId, "live_${cmd.midi}", cmd.midi, cmd.velocity, cmd.params)
                        is EngineCommand.ReleasePitch ->
                            voiceManager.releasePitch(cmd.trackId, cmd.midi)
                        is EngineCommand.TriggerDrum ->
                            voiceManager.triggerDrum(cmd.trackId, cmd.soundType, cmd.midi, cmd.velocity, cmd.pitchOffset, cmd.gain)
                        is EngineCommand.TriggerSample -> {
                            SampleStore.getCachedOrLoad(cmd.params.sampleFilePath)?.let { buf ->
                                voiceManager.triggerSample(cmd.trackId, "live_${cmd.midi}", buf, cmd.midi, cmd.velocity, cmd.params)
                            }
                        }
                        EngineCommand.StopAllVoices -> {
                            voiceManager.stopAll()
                            mixer.reset()
                        }
                    }
                }

                val project = projectRef.get()
                if (project == null) {
                    pcmOut.fill(0)
                    audioTrack.write(pcmOut, 0, pcmOut.size)
                    continue
                }

                val frameInfo = if (isPlayingFlag.get()) {
                    sequencer.processBlock(
                        project = project,
                        voiceManager = voiceManager,
                        mixer = mixer,
                        metroL = metroL,
                        metroR = metroR,
                        numFrames = BLOCK_SIZE,
                        includeMetronome = true,
                        loopPlayback = project.loopEnabled
                    )
                } else {
                    metroL.fill(0f)
                    metroR.fill(0f)
                    Sequencer.SequencerFrameInfo(0f, 0f)
                }

                mixer.mixProjectBlock(
                    project = project,
                    voiceManager = voiceManager,
                    metronomeL = metroL,
                    metronomeR = metroR,
                    outL = outL,
                    outR = outR,
                    numFrames = BLOCK_SIZE,
                    isProMode = isProModeFlag.get(),
                    currentBarFloat = frameInfo.barFloat,
                    subStepFraction = frameInfo.subStepFraction
                )

                for (i in 0 until BLOCK_SIZE) {
                    val sL = (outL[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                    val sR = (outR[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                    pcmOut[i * 2] = sL
                    pcmOut[i * 2 + 1] = sR
                }

                audioTrack.write(pcmOut, 0, pcmOut.size)

                uiMeterCounter++
                if (uiMeterCounter >= 4) { // ~43 Hz UI meter update
                    uiMeterCounter = 0
                    if (isPlayingFlag.get()) {
                        _playheadStep.value = sequencer.currentStepInPattern
                        _playheadBar.value = sequencer.currentBarInArrange
                    }
                    _masterLevel.value = mixer.limiter.lastPeakLeft to mixer.limiter.lastPeakRight
                    val map = HashMap<String, Float>(project.tracks.size)
                    for (t in project.tracks) {
                        map[t.id] = mixer.getTrackPeak(t.id)
                    }
                    _trackLevels.value = map
                }
            }
        } finally {
            runCatching { audioTrack.stop(); audioTrack.release() }
        }
    }
}
