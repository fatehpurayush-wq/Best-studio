package com.beatstudio.app.export

import com.beatstudio.app.audio.AudioEngine
import com.beatstudio.app.audio.Mixer
import com.beatstudio.app.audio.Sequencer
import com.beatstudio.app.audio.VoiceManager
import com.beatstudio.app.domain.Project
import com.beatstudio.app.music.MidiIo
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat(val labelHinglish: String, val extension: String, val mimeType: String) {
    WAV_16("WAV 16-bit", "wav", "audio/wav"),
    WAV_24("WAV 24-bit", "wav", "audio/wav"),
    M4A_AAC("M4A (AAC)", "m4a", "audio/mp4"),
    MIDI("MIDI (.mid)", "mid", "audio/midi")
}

object OfflineRenderer {

    fun renderToFile(
        project: Project,
        outputFile: File,
        format: ExportFormat,
        isProMode: Boolean,
        soloTrackId: String? = null,
        isCancelled: () -> Boolean,
        onProgress: (Float) -> Unit
    ): Boolean {
        if (format == ExportFormat.MIDI) {
            val midiBytes = MidiIo.exportProjectToMidiBytes(project)
            FileOutputStream(outputFile).use { it.write(midiBytes) }
            onProgress(1f)
            return true
        }

        val sampleRate = AudioEngine.SAMPLE_RATE
        val blockSize = AudioEngine.BLOCK_SIZE
        val sequencer = Sequencer(sampleRate)
        val voiceManager = VoiceManager(sampleRate.toFloat())
        val mixer = Mixer(sampleRate, blockSize)

        val stepsPerBar = project.timeSignatureTop * 4
        val sps = sequencer.samplesPerStep(project)
        val totalFrames = if (project.isArrangeMode && project.arrangement.isNotEmpty()) {
            sequencer.totalArrangeSamples(project)
        } else {
            val maxSteps = project.tracks.maxOfOrNull { it.patterns[it.activePatternId]?.stepCount ?: 16 } ?: 16
            (maxSteps * sps).toLong().coerceAtLeast(sampleRate.toLong())
        }
        // Add 1.5s reverb/release tail so endings never clip abruptly
        val tailFrames = (sampleRate * 1.5).toLong()
        val renderTotal = totalFrames + tailFrames

        val outL = FloatArray(blockSize)
        val outR = FloatArray(blockSize)
        val metroL = FloatArray(blockSize)
        val metroR = FloatArray(blockSize)

        val wavWriter = if (format == ExportFormat.WAV_16 || format == ExportFormat.WAV_24) {
            WavWriter(
                file = outputFile,
                sampleRate = sampleRate,
                channels = 2,
                bitsPerSample = if (format == ExportFormat.WAV_24) 24 else 16
            )
        } else null

        val aacEncoder = if (format == ExportFormat.M4A_AAC) {
            AacEncoder(outputFile, sampleRate = sampleRate, channels = 2)
        } else null

        try {
            var rendered = 0L
            while (rendered < renderTotal) {
                if (isCancelled()) return false
                val framesThisBlock = minOf(blockSize.toLong(), renderTotal - rendered).toInt()
                val inSongBody = rendered < totalFrames

                val frameInfo = if (inSongBody) {
                    sequencer.processBlock(
                        project = project,
                        voiceManager = voiceManager,
                        mixer = mixer,
                        metroL = metroL,
                        metroR = metroR,
                        numFrames = framesThisBlock,
                        includeMetronome = false,
                        loopPlayback = false
                    )
                } else {
                    metroL.fill(0f, 0, framesThisBlock)
                    metroR.fill(0f, 0, framesThisBlock)
                    Sequencer.SequencerFrameInfo(0f, 0f)
                }

                mixer.mixProjectBlock(
                    project = project,
                    voiceManager = voiceManager,
                    metronomeL = metroL,
                    metronomeR = metroR,
                    outL = outL,
                    outR = outR,
                    numFrames = framesThisBlock,
                    isProMode = isProMode,
                    currentBarFloat = frameInfo.barFloat,
                    subStepFraction = frameInfo.subStepFraction,
                    soloTrackIdOverride = soloTrackId
                )

                // Smooth 10ms fade-out at the very end of tail
                if (rendered + framesThisBlock >= renderTotal - 441) {
                    for (i in 0 until framesThisBlock) {
                        val absPos = rendered + i
                        if (absPos >= renderTotal - 441) {
                            val fade = ((renderTotal - absPos).toFloat() / 441f).coerceIn(0f, 1f)
                            outL[i] *= fade
                            outR[i] *= fade
                        }
                    }
                }

                wavWriter?.writeStereoFloatBlock(outL, outR, framesThisBlock)
                aacEncoder?.writeStereoFloatBlock(outL, outR, framesThisBlock)

                rendered += framesThisBlock
                onProgress((rendered.toFloat() / renderTotal.toFloat()).coerceIn(0f, 1f))
            }
            return true
        } finally {
            runCatching { wavWriter?.close() }
            runCatching { aacEncoder?.finish() }
        }
    }
}
