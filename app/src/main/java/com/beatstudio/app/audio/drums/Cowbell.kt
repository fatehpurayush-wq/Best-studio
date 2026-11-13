package com.beatstudio.app.audio.drums

import com.beatstudio.app.audio.StateVariableFilter
import com.beatstudio.app.music.Scales
import kotlin.math.exp
import kotlin.math.tanh

class CowbellVoice {
    private var phase1 = 0f
    private var phase2 = 0f
    private var timeSec = 0f
    private var durationSec = 0.42f
    private var freq1 = 540f
    private var freq2 = 800f
    private var velocity = 1f
    private val bpFilter = StateVariableFilter()
    var active = false
        private set

    fun trigger(midiPitch: Int, vel: Float, pitchOffsetSemitones: Float) {
        active = true
        phase1 = 0f
        phase2 = 0f
        timeSec = 0f
        velocity = vel
        // Default MIDI 56 -> ~540 Hz & 800 Hz TR-808 Phonk cowbell ratio (1.4815)
        val baseRatio = Scales.midiToFreq(midiPitch + pitchOffsetSemitones) / Scales.midiToFreq(56f)
        freq1 = (540f * baseRatio).coerceIn(140f, 2400f)
        freq2 = freq1 * 1.4815f
        bpFilter.reset()
    }

    fun nextSample(sampleRate: Float): Float {
        if (!active) return 0f
        val dt = 1f / sampleRate
        timeSec += dt
        if (timeSec >= durationSec) {
            active = false
            return 0f
        }

        phase1 = (phase1 + freq1 * dt) % 1f
        phase2 = (phase2 + freq2 * dt) % 1f

        val sq1 = if (phase1 < 0.5f) 1f else -1f
        val sq2 = if (phase2 < 0.5f) 1f else -1f
        val raw = (sq1 + sq2) * 0.5f

        val filtered = bpFilter.process(raw, (freq1 + freq2) * 0.52f, 0.55f, sampleRate, mode = 2)
        val env = exp(-timeSec / 0.095f) * 0.7f + exp(-timeSec / 0.018f) * 0.3f
        val attack = (timeSec / 0.002f).coerceIn(0f, 1f)
        val fade = if (timeSec > durationSec - 0.01f) ((durationSec - timeSec) / 0.01f).coerceIn(0f, 1f) else 1f
        return tanh(filtered * env * attack * 2.2f) * velocity * 0.78f * fade
    }
}
