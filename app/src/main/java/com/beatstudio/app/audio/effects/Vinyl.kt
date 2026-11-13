package com.beatstudio.app.audio.effects

import com.beatstudio.app.audio.Oscillator
import com.beatstudio.app.audio.StateVariableFilter

class VinylEffect(private val sampleRate: Float = 44100f) {
    private val noise = Oscillator()
    private val filter = StateVariableFilter()
    private var popEnv = 0f

    fun reset() {
        filter.reset()
        popEnv = 0f
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        level: Float,
        density: Float
    ) {
        val lvl = level.coerceIn(0f, 1f)
        if (lvl <= 0.001f) return
        val threshold = 1.0f - (0.0002f + density.coerceIn(0f, 1f) * 0.0045f)

        for (i in 0 until numFrames) {
            val n = noise.nextWhiteNoise()
            if (n > threshold) {
                popEnv = 0.55f + (n - threshold) * 50f
            }
            popEnv *= 0.92f
            val dust = filter.process(n * 0.04f + popEnv, 3200f, 0.2f, sampleRate, mode = 2) * lvl * 0.45f
            bufL[i] += dust
            bufR[i] += dust * 0.92f
        }
    }
}
