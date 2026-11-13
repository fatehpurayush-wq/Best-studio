package com.beatstudio.app.audio.drums

import com.beatstudio.app.audio.Oscillator
import com.beatstudio.app.audio.StateVariableFilter
import kotlin.math.exp
import kotlin.math.tanh

class ClapVoice {
    private var timeSec = 0f
    private var durationSec = 0.34f
    private var velocity = 1f
    private val noiseOsc = Oscillator()
    private val bpFilter = StateVariableFilter()
    var active = false
        private set

    fun trigger(vel: Float, pitchOffsetSemitones: Float) {
        active = true
        timeSec = 0f
        velocity = vel
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

        // Classic 4-burst staggered clap envelope
        val burstEnv = when {
            timeSec < 0.011f -> exp(-timeSec / 0.004f)
            timeSec < 0.022f -> exp(-(timeSec - 0.011f) / 0.004f)
            timeSec < 0.034f -> exp(-(timeSec - 0.022f) / 0.005f)
            else -> exp(-(timeSec - 0.034f) / 0.085f)
        }

        val filteredNoise = bpFilter.process(noiseOsc.nextWhiteNoise(), 1350f, 0.55f, sampleRate, mode = 2)
        val fade = if (timeSec > durationSec - 0.01f) ((durationSec - timeSec) / 0.01f).coerceIn(0f, 1f) else 1f
        return tanh(filteredNoise * burstEnv * 2.4f) * velocity * 0.8f * fade
    }
}
