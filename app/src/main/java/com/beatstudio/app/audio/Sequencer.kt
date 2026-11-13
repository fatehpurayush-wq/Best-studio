package com.beatstudio.app.audio

import com.beatstudio.app.audio.effects.SlowedReverb
import com.beatstudio.app.audio.instruments.SampleStore
import com.beatstudio.app.domain.*
import com.beatstudio.app.music.Scales
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class Sequencer(private val sampleRate: Int = 44100) {

    private var samplePosInProject = 0L
    private var lastGlobalStep = -1
    private var subRollCounter = 0

    // Scheduled note-offs: (releaseAtSample, trackId, noteId)
    private data class ScheduledRelease(
        var active: Boolean = false,
        var releaseSample: Long = 0L,
        var trackId: String = "",
        var noteId: String = ""
    )

    // Scheduled hi-hat rolls: (triggerAtSample, trackId, soundType, midi, vel, pitchOff, rowGain)
    private data class ScheduledRollHit(
        var active: Boolean = false,
        var triggerSample: Long = 0L,
        var trackId: String = "",
        var soundType: DrumSoundType = DrumSoundType.HIHAT_CLOSED,
        var midiPitch: Int = 42,
        var velocity: Float = 0.8f,
        var pitchOffset: Float = 0f,
        var rowGain: Float = 0.8f
    )

    private val releaseQueue = Array(64) { ScheduledRelease() }
    private val rollQueue = Array(32) { ScheduledRollHit() }
    private val rng = Random(42)

    // Metronome click synthesis state
    private var metroClickTime = -1f
    private var metroAccent = false

    @Volatile var currentStepInPattern: Int = 0
        private set
    @Volatile var currentBarInArrange: Int = 0
        private set

    fun reset() {
        samplePosInProject = 0L
        lastGlobalStep = -1
        subRollCounter = 0
        currentStepInPattern = 0
        currentBarInArrange = 0
        metroClickTime = -1f
        for (r in releaseQueue) r.active = false
        for (h in rollQueue) h.active = false
    }

    fun samplesPerStep(project: Project, isTriplet: Boolean = false): Double {
        val effBpm = SlowedReverb.effectiveBpm(project).toDouble().coerceIn(30.0, 240.0)
        val stepsPerBeat = if (isTriplet) 3.0 else 4.0
        val secondsPerStep = 60.0 / (effBpm * stepsPerBeat)
        return secondsPerStep * sampleRate
    }

    fun totalArrangeSamples(project: Project): Long {
        val bars = (project.loopEndBar - project.loopStartBar).coerceAtLeast(1)
        val stepsPerBar = project.timeSignatureTop * 4
        val totalSteps = bars * stepsPerBar
        return (totalSteps * samplesPerStep(project)).toLong()
    }

    fun processBlock(
        project: Project,
        voiceManager: VoiceManager,
        mixer: Mixer,
        metroL: FloatArray,
        metroR: FloatArray,
        numFrames: Int,
        includeMetronome: Boolean,
        loopPlayback: Boolean = true
    ): SequencerFrameInfo {
        metroL.fill(0f, 0, numFrames)
        metroR.fill(0f, 0, numFrames)

        val stepsPerBar = project.timeSignatureTop * 4
        val sps = samplesPerStep(project, isTriplet = false)

        val loopBars = if (project.isArrangeMode) {
            (project.loopEndBar - project.loopStartBar).coerceAtLeast(1)
        } else {
            val maxPatternSteps = project.tracks.maxOfOrNull {
                it.patterns[it.activePatternId]?.stepCount ?: 16
            } ?: 16
            (maxPatternSteps / stepsPerBar).coerceAtLeast(1)
        }
        val loopStartSample = if (project.isArrangeMode) {
            (project.loopStartBar * stepsPerBar * sps).toLong()
        } else 0L
        val loopLengthSamples = (loopBars * stepsPerBar * sps).toLong().coerceAtLeast(1L)

        var subStepFrac = 0f

        for (i in 0 until numFrames) {
            var curSample = samplePosInProject
            if (loopPlayback && curSample >= loopStartSample + loopLengthSamples) {
                curSample = loopStartSample + ((curSample - loopStartSample) % loopLengthSamples)
                samplePosInProject = curSample
                lastGlobalStep = -1
            }

            val globalStepDouble = curSample / sps
            val globalStep = globalStepDouble.toInt()
            subStepFrac = (globalStepDouble - globalStep).toFloat()

            // Process scheduled note-offs
            for (rel in releaseQueue) {
                if (rel.active && curSample >= rel.releaseSample) {
                    voiceManager.releaseNote(rel.trackId, rel.noteId)
                    rel.active = false
                }
            }

            // Process scheduled hi-hat rolls
            for (hit in rollQueue) {
                if (hit.active && curSample >= hit.triggerSample) {
                    voiceManager.triggerDrum(
                        trackId = hit.trackId,
                        soundType = hit.soundType,
                        midiPitch = hit.midiPitch,
                        velocity = hit.velocity,
                        pitchOffset = hit.pitchOffset,
                        rowGain = hit.rowGain
                    )
                    hit.active = false
                }
            }

            // Trigger new step events
            if (globalStep != lastGlobalStep) {
                lastGlobalStep = globalStep
                val barIdx = globalStep / stepsPerBar
                val stepInBar = globalStep % stepsPerBar
                currentBarInArrange = barIdx
                currentStepInPattern = stepInBar

                // Metronome on beat boundaries
                if (includeMetronome && project.metronomeEnabled && stepInBar % 4 == 0) {
                    metroClickTime = 0f
                    metroAccent = (stepInBar == 0)
                }

                // Trigger tracks
                for (track in project.tracks) {
                    val patternId = if (project.isArrangeMode) {
                        val block = project.arrangement.firstOrNull { b ->
                            b.trackId == track.id && barIdx >= b.startBar && barIdx < b.startBar + b.lengthBars
                        }
                        block?.patternId
                    } else {
                        track.activePatternId
                    } ?: continue

                    val pattern = track.patterns[patternId] ?: continue
                    val patStepCount = pattern.stepCount.coerceAtLeast(1)
                    val stepInPat = globalStep % patStepCount
                    if (!project.isArrangeMode) {
                        currentStepInPattern = stepInPat
                    }

                    // Swing delay on odd 16th steps
                    val swingSamples = if (stepInPat % 2 == 1 && project.swing > 0.01f) {
                        (sps * project.swing * 0.45).toLong()
                    } else 0L

                    when (track.type) {
                        TrackType.DRUMS -> {
                            for (row in track.drumRows) {
                                if (row.muted) continue
                                val step = pattern.drumRows[row.rowIndex]?.getOrNull(stepInPat) ?: continue
                                if (!step.active) continue
                                if (step.probability < 0.99f && rng.nextFloat() > step.probability) continue

                                val rolls = step.rollCount.coerceIn(1, 4)
                                if (rolls == 1 && swingSamples == 0L) {
                                    voiceManager.triggerDrum(
                                        trackId = track.id,
                                        soundType = row.soundType,
                                        midiPitch = row.soundType.defaultMidi,
                                        velocity = step.velocity,
                                        pitchOffset = row.pitchOffset,
                                        rowGain = row.gain
                                    )
                                    if (row.soundType in listOf(DrumSoundType.KICK_PUNCHY, DrumSoundType.KICK_SUB, DrumSoundType.KICK_808)) {
                                        mixer.triggerSidechainKick()
                                    }
                                } else {
                                    val subInterval = (sps / rolls).toLong().coerceAtLeast(100L)
                                    for (r in 0 until rolls) {
                                        scheduleRollHit(
                                            triggerSample = curSample + swingSamples + r * subInterval,
                                            trackId = track.id,
                                            soundType = row.soundType,
                                            midiPitch = row.soundType.defaultMidi,
                                            velocity = step.velocity * (1f - r * 0.1f),
                                            pitchOffset = row.pitchOffset,
                                            rowGain = row.gain
                                        )
                                    }
                                }
                            }
                        }
                        TrackType.INSTRUMENT -> {
                            for (note in pattern.notes) {
                                val noteStepInt = note.startStep.toInt()
                                if (noteStepInt == stepInPat) {
                                    if (note.probability < 0.99f && rng.nextFloat() > note.probability) continue
                                    val durSamples = (note.durationSteps * sps).toLong().coerceAtLeast(400L)
                                    val snappedMidi = Scales.snapToScale(note.pitch, project.rootNote, project.scale)
                                    voiceManager.triggerSynth(
                                        trackId = track.id,
                                        noteId = note.id,
                                        midiPitch = snappedMidi,
                                        velocity = note.velocity,
                                        params = track.synthParams
                                    )
                                    scheduleRelease(curSample + durSamples, track.id, note.id)
                                }
                            }
                        }
                        TrackType.SAMPLER, TrackType.VOCAL -> {
                            val filePath = if (track.type == TrackType.VOCAL) track.vocalFilePath else track.samplerParams.sampleFilePath
                            val sampleBuf = SampleStore.getCachedOrLoad(filePath)
                            if (sampleBuf != null) {
                                if (track.type == TrackType.VOCAL && pattern.notes.isEmpty()) {
                                    // Trigger vocal once at bar 0 of block/loop
                                    if (stepInPat == 0 && (project.isArrangeMode || barIdx == 0)) {
                                        voiceManager.triggerSample(
                                            trackId = track.id,
                                            noteId = "vocal_auto",
                                            sampleBuffer = sampleBuf,
                                            midiPitch = 60,
                                            velocity = 0.95f,
                                            samplerParams = track.samplerParams.copy(rootMidiNote = 60)
                                        )
                                    }
                                } else {
                                    for (note in pattern.notes) {
                                        if (note.startStep.toInt() == stepInPat) {
                                            val durSamples = (note.durationSteps * sps).toLong().coerceAtLeast(400L)
                                            voiceManager.triggerSample(
                                                trackId = track.id,
                                                noteId = note.id,
                                                sampleBuffer = sampleBuf,
                                                midiPitch = note.pitch,
                                                velocity = note.velocity,
                                                samplerParams = track.samplerParams
                                            )
                                            scheduleRelease(curSample + durSamples, track.id, note.id)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Render metronome click if active
            if (metroClickTime >= 0f) {
                val freq = if (metroAccent) 1500f else 1000f
                val env = exp(-metroClickTime / 0.012f)
                val sig = sin((metroClickTime * 2.0 * PI * freq).toFloat()) * env * 0.35f
                metroL[i] = sig
                metroR[i] = sig
                metroClickTime += 1f / sampleRate
                if (metroClickTime > 0.05f) metroClickTime = -1f
            }

            samplePosInProject++
        }

        val currentBarFloat = (samplePosInProject.toDouble() / (stepsPerBar * sps)).toFloat()
        return SequencerFrameInfo(currentBarFloat, subStepFrac)
    }

    private fun scheduleRelease(releaseSample: Long, trackId: String, noteId: String) {
        for (slot in releaseQueue) {
            if (!slot.active) {
                slot.active = true
                slot.releaseSample = releaseSample
                slot.trackId = trackId
                slot.noteId = noteId
                return
            }
        }
    }

    private fun scheduleRollHit(
        triggerSample: Long,
        trackId: String,
        soundType: DrumSoundType,
        midiPitch: Int,
        velocity: Float,
        pitchOffset: Float,
        rowGain: Float
    ) {
        for (slot in rollQueue) {
            if (!slot.active) {
                slot.active = true
                slot.triggerSample = triggerSample
                slot.trackId = trackId
                slot.soundType = soundType
                slot.midiPitch = midiPitch
                slot.velocity = velocity
                slot.pitchOffset = pitchOffset
                slot.rowGain = rowGain
                return
            }
        }
    }

    data class SequencerFrameInfo(val barFloat: Float, val subStepFraction: Float)
}
