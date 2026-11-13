package com.beatstudio.app.audio

import kotlin.math.PI
import kotlin.math.sin

class Lfo {
    private var phase = 0f

    fun reset() {
        phase = 0f
    }

    fun nextSample(rateHz: Float, sampleRate: Float): Float {
        val dt = (rateHz / sampleRate).coerceIn(0.000001f, 0.2f)
        val value = sin((phase * 2.0 * PI).toFloat())
        phase += dt
        if (phase >= 1f) phase -= 1f
        return value
    }
}
