package com.beatstudio.app.audio.effects

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

class CompressorEffect(private val sampleRate: Float = 44100f) {
    private var env = 0f

    fun reset() {
        env = 0f
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        thresholdNorm: Float,
        ratioNorm: Float,
        makeupNorm: Float
    ) {
        val threshold = 0.08f + (1f - thresholdNorm.coerceIn(0f, 1f)) * 0.72f
        val ratio = 1.5f + ratioNorm.coerceIn(0f, 1f) * 10.5f
        val makeup = 1f + makeupNorm.coerceIn(0f, 1f) * 1.8f

        val attackCoeff = exp(-1f / (0.008f * sampleRate))
        val releaseCoeff = exp(-1f / (0.12f * sampleRate))

        for (i in 0 until numFrames) {
            val peak = max(abs(bufL[i]), abs(bufR[i]))
            val coeff = if (peak > env) attackCoeff else releaseCoeff
            env = coeff * env + (1f - coeff) * peak

            val gain = if (env > threshold) {
                val over = env / threshold
                (over.pow(1f / ratio - 1f)).coerceIn(0.15f, 1f)
            } else 1f

            val totalGain = gain * makeup
            bufL[i] *= totalGain
            bufR[i] *= totalGain
        }
    }
}
