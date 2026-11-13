package com.beatstudio.app.audio

import com.beatstudio.app.audio.drums.*
import com.beatstudio.app.audio.instruments.SampleBuffer
import com.beatstudio.app.audio.instruments.SamplerVoice
import com.beatstudio.app.audio.instruments.SynthVoice
import com.beatstudio.app.domain.DrumSoundType
import com.beatstudio.app.domain.SamplerParams
import com.beatstudio.app.domain.SynthParams

/**
 * 32-voice pre-allocated polyphony manager. Zero allocations during audio rendering.
 */
class VoiceManager(private val sampleRate: Float = 44100f) {

    companion object {
        const val MAX_VOICES = 32
    }

    enum class VoiceKind {
        NONE, SYNTH, DRUM_KICK, DRUM_808, DRUM_SNARE, DRUM_CLAP, DRUM_HIHAT, DRUM_COWBELL, DRUM_PERC, SAMPLER
    }

    private class VoiceSlot {
        var kind = VoiceKind.NONE
        var trackId: String = ""
        var noteId: String = ""
        var midiPitch: Int = 60
        var age: Long = 0L
        var pan: Float = 0f
        var gain: Float = 1f

        val synth = SynthVoice()
        val kick = KickVoice()
        val drum808 = Drum808Voice()
        val snare = SnareVoice()
        val clap = ClapVoice()
        val hihat = HiHatVoice()
        val cowbell = CowbellVoice()
        val perc = PercussionVoice()
        val sampler = SamplerVoice()

        fun isActive(): Boolean = when (kind) {
            VoiceKind.SYNTH -> synth.isActive
            VoiceKind.DRUM_KICK -> kick.active
            VoiceKind.DRUM_808 -> drum808.active
            VoiceKind.DRUM_SNARE -> snare.active
            VoiceKind.DRUM_CLAP -> clap.active
            VoiceKind.DRUM_HIHAT -> hihat.active
            VoiceKind.DRUM_COWBELL -> cowbell.active
            VoiceKind.DRUM_PERC -> perc.active
            VoiceKind.SAMPLER -> sampler.isActive
            VoiceKind.NONE -> false
        }

        fun forceStop() {
            synth.forceStop()
            sampler.forceStop()
            kind = VoiceKind.NONE
        }
    }

    private val slots = Array(MAX_VOICES) { VoiceSlot() }
    private var voiceCounter = 0L
    private val stereoScratch = FloatArray(2)
    private var lastSynthFreqByTrack = HashMap<String, Float>()

    fun stopAll() {
        for (slot in slots) {
            slot.forceStop()
        }
    }

    fun triggerSynth(
        trackId: String,
        noteId: String,
        midiPitch: Int,
        velocity: Float,
        params: SynthParams
    ) {
        val slot = allocateSlot()
        slot.kind = VoiceKind.SYNTH
        slot.trackId = trackId
        slot.noteId = noteId
        slot.midiPitch = midiPitch
        slot.age = ++voiceCounter
        slot.gain = 1f
        val prevFreq = lastSynthFreqByTrack[trackId]
        slot.synth.trigger(midiPitch, velocity, params, sampleRate, prevFreq)
        lastSynthFreqByTrack[trackId] = com.beatstudio.app.music.Scales.midiToFreq(midiPitch.toFloat())
    }

    fun releaseNote(trackId: String, noteId: String) {
        for (slot in slots) {
            if (slot.trackId == trackId && slot.noteId == noteId) {
                when (slot.kind) {
                    VoiceKind.SYNTH -> slot.synth.release()
                    VoiceKind.SAMPLER -> slot.sampler.release()
                    VoiceKind.DRUM_808 -> slot.drum808.release()
                    else -> Unit
                }
            }
        }
    }

    fun releasePitch(trackId: String, midiPitch: Int) {
        for (slot in slots) {
            if (slot.trackId == trackId && slot.midiPitch == midiPitch) {
                when (slot.kind) {
                    VoiceKind.SYNTH -> slot.synth.release()
                    VoiceKind.SAMPLER -> slot.sampler.release()
                    VoiceKind.DRUM_808 -> slot.drum808.release()
                    else -> Unit
                }
            }
        }
    }

    fun triggerDrum(
        trackId: String,
        soundType: DrumSoundType,
        midiPitch: Int,
        velocity: Float,
        pitchOffset: Float,
        rowGain: Float
    ) {
        // Closed hi-hat chokes open hi-hat on the same track
        if (soundType == DrumSoundType.HIHAT_CLOSED) {
            for (s in slots) {
                if (s.trackId == trackId && s.kind == VoiceKind.DRUM_HIHAT && s.isActive()) {
                    s.kind = VoiceKind.NONE
                }
            }
        }

        val slot = allocateSlot()
        slot.trackId = trackId
        slot.noteId = ""
        slot.midiPitch = midiPitch
        slot.age = ++voiceCounter
        slot.gain = rowGain

        when (soundType) {
            DrumSoundType.KICK_PUNCHY, DrumSoundType.KICK_SUB, DrumSoundType.KICK_808 -> {
                slot.kind = VoiceKind.DRUM_KICK
                slot.kick.trigger(soundType, velocity, pitchOffset)
            }
            DrumSoundType.BASS_808 -> {
                slot.kind = VoiceKind.DRUM_808
                slot.drum808.trigger(midiPitch, velocity, pitchOffset)
            }
            DrumSoundType.SNARE -> {
                slot.kind = VoiceKind.DRUM_SNARE
                slot.snare.trigger(velocity, pitchOffset)
            }
            DrumSoundType.CLAP -> {
                slot.kind = VoiceKind.DRUM_CLAP
                slot.clap.trigger(velocity, pitchOffset)
            }
            DrumSoundType.HIHAT_CLOSED -> {
                slot.kind = VoiceKind.DRUM_HIHAT
                slot.hihat.trigger(isOpen = false, vel = velocity, pitchOffsetSemitones = pitchOffset)
            }
            DrumSoundType.HIHAT_OPEN -> {
                slot.kind = VoiceKind.DRUM_HIHAT
                slot.hihat.trigger(isOpen = true, vel = velocity, pitchOffsetSemitones = pitchOffset)
            }
            DrumSoundType.PHONK_COWBELL -> {
                slot.kind = VoiceKind.DRUM_COWBELL
                slot.cowbell.trigger(midiPitch, velocity, pitchOffset)
            }
            else -> {
                slot.kind = VoiceKind.DRUM_PERC
                slot.perc.trigger(soundType, velocity, pitchOffset)
            }
        }
    }

    fun triggerSample(
        trackId: String,
        noteId: String,
        sampleBuffer: SampleBuffer,
        midiPitch: Int,
        velocity: Float,
        samplerParams: SamplerParams
    ) {
        val slot = allocateSlot()
        slot.kind = VoiceKind.SAMPLER
        slot.trackId = trackId
        slot.noteId = noteId
        slot.midiPitch = midiPitch
        slot.age = ++voiceCounter
        slot.gain = 1f
        slot.sampler.trigger(sampleBuffer, midiPitch, velocity, samplerParams, sampleRate)
    }

    fun activeVoiceCount(): Int {
        var c = 0
        for (slot in slots) {
            if (slot.isActive()) c++
        }
        return c
    }

    fun renderTrackSample(trackId: String, outL: FloatArray, outR: FloatArray, numFrames: Int) {
        for (s in slots) {
            if (s.trackId != trackId || !s.isActive()) continue
            val g = s.gain
            when (s.kind) {
                VoiceKind.SYNTH -> {
                    for (i in 0 until numFrames) {
                        s.synth.nextStereoSample(sampleRate, stereoScratch)
                        outL[i] += stereoScratch[0] * g
                        outR[i] += stereoScratch[1] * g
                    }
                }
                VoiceKind.DRUM_KICK -> {
                    for (i in 0 until numFrames) {
                        val v = s.kick.nextSample(sampleRate) * g
                        outL[i] += v; outR[i] += v
                    }
                }
                VoiceKind.DRUM_808 -> {
                    for (i in 0 until numFrames) {
                        val v = s.drum808.nextSample(sampleRate) * g
                        outL[i] += v; outR[i] += v
                    }
                }
                VoiceKind.DRUM_SNARE -> {
                    for (i in 0 until numFrames) {
                        val v = s.snare.nextSample(sampleRate) * g
                        outL[i] += v; outR[i] += v
                    }
                }
                VoiceKind.DRUM_CLAP -> {
                    for (i in 0 until numFrames) {
                        val v = s.clap.nextSample(sampleRate) * g
                        outL[i] += v; outR[i] += v
                    }
                }
                VoiceKind.DRUM_HIHAT -> {
                    for (i in 0 until numFrames) {
                        val v = s.hihat.nextSample(sampleRate) * g
                        outL[i] += v; outR[i] += v
                    }
                }
                VoiceKind.DRUM_COWBELL -> {
                    for (i in 0 until numFrames) {
                        val v = s.cowbell.nextSample(sampleRate) * g
                        outL[i] += v; outR[i] += v
                    }
                }
                VoiceKind.DRUM_PERC -> {
                    for (i in 0 until numFrames) {
                        val v = s.perc.nextSample(sampleRate) * g
                        outL[i] += v; outR[i] += v
                    }
                }
                VoiceKind.SAMPLER -> {
                    for (i in 0 until numFrames) {
                        val v = s.sampler.nextSample() * g
                        outL[i] += v; outR[i] += v
                    }
                }
                VoiceKind.NONE -> Unit
            }
            if (!s.isActive()) {
                s.kind = VoiceKind.NONE
            }
        }
    }

    private fun allocateSlot(): VoiceSlot {
        for (slot in slots) {
            if (slot.kind == VoiceKind.NONE || !slot.isActive()) {
                slot.forceStop()
                return slot
            }
        }
        // Voice stealing: pick oldest slot
        var oldest = slots[0]
        for (slot in slots) {
            if (slot.age < oldest.age) {
                oldest = slot
            }
        }
        oldest.forceStop()
        return oldest
    }
}
