package com.beatstudio.app.audio

import kotlin.math.PI
import kotlin.math.sin

class StateVariableFilter {
    private var low = 0f
    private var band = 0f
    private var high = 0f

    fun reset() {
        low = 0f
        band = 0f
        high = 0f
    }

    /**
     * mode: 0 = Low-Pass, 1 = High-Pass, 2 = Band-Pass
     */
    fun process(input: Float, cutoffHz: Float, resonance: Float, sampleRate: Float, mode: Int = 0): Float {
        val clampedFreq = cutoffHz.coerceIn(30f, sampleRate * 0.42f)
        val f = (2.0 * sin(PI * clampedFreq / sampleRate)).toFloat().coerceIn(0.001f, 0.95f)
        val q = (1f - resonance.coerceIn(0f, 0.92f)).coerceIn(0.08f, 1.0f)

        // Two-pass oversampled SVF for stability at high frequencies
        for (i in 0 until 2) {
            low += f * band
            high = input - low - q * band
            band += f * high
        }

        return when (mode) {
            1 -> high
            2 -> band
            else -> low
        }
    }
}
