package com.beatstudio.app.audio

import com.beatstudio.app.domain.OscWaveform
import kotlin.math.PI
import kotlin.math.sin

class Oscillator {
    var phase: Float = 0f
    private var noiseSeed: Int = 0x13579BDF

    fun reset(initialPhase: Float = 0f) {
        phase = initialPhase
    }

    fun nextSample(freqHz: Float, sampleRate: Float, waveform: OscWaveform): Float {
        val dt = (freqHz / sampleRate).coerceIn(0.00001f, 0.48f)
        val p = phase
        phase += dt
        if (phase >= 1f) phase -= 1f

        return when (waveform) {
            OscWaveform.SINE -> sin((p * 2.0 * PI).toFloat())
            OscWaveform.SAW -> {
                var value = 2f * p - 1f
                value -= polyBlep(p, dt)
                value
            }
            OscWaveform.SQUARE -> {
                var value = if (p < 0.5f) 1f else -1f
                value += polyBlep(p, dt)
                value -= polyBlep((p + 0.5f) % 1f, dt)
                value
            }
            OscWaveform.TRIANGLE -> {
                val tri = if (p < 0.5f) 4f * p - 1f else 3f - 4f * p
                tri
            }
            OscWaveform.NOISE -> nextWhiteNoise()
        }
    }

    fun nextWhiteNoise(): Float {
        noiseSeed = noiseSeed * 1664525 + 1013904223
        return ((noiseSeed shr 8) and 0xFFFFFF) / 8388608f - 1f
    }

    private fun polyBlep(t: Float, dt: Float): Float {
        return when {
            t < dt -> {
                val x = t / dt
                x + x - x * x - 1f
            }
            t > 1f - dt -> {
                val x = (t - 1f) / dt
                x * x + x + x + 1f
            }
            else -> 0f
        }
    }
}
