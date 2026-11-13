package com.beatstudio.app.audio.effects

import kotlin.math.exp

class SidechainDucker(private val sampleRate: Float = 44100f) {
    private var duckEnvelope = 0f

    fun reset() {
        duckEnvelope = 0f
    }

    fun triggerKickPulse() {
        duckEnvelope = 1f
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        amount: Float,
        releaseNorm: Float
    ) {
        val depth = amount.coerceIn(0f, 1f)
        if (depth <= 0.001f) return
        val releaseSec = 0.06f + releaseNorm.coerceIn(0f, 1f) * 0.42f
        val decayCoeff = exp(-1f / (releaseSec * sampleRate))

        for (i in 0 until numFrames) {
            val gain = (1f - duckEnvelope * depth * 0.88f).coerceIn(0.08f, 1f)
            bufL[i] *= gain
            bufR[i] *= gain
            duckEnvelope *= decayCoeff
        }
    }
}
