package com.beatstudio.app.audio.effects

import kotlin.math.pow
import kotlin.math.round

class BitcrusherEffect {
    private var holdCounter = 0
    private var heldL = 0f
    private var heldR = 0f

    fun reset() {
        holdCounter = 0
        heldL = 0f
        heldR = 0f
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        bitDepth: Float,
        downsampleFactor: Float,
        mix: Float
    ) {
        val wet = mix.coerceIn(0f, 1f)
        if (wet <= 0.001f) return
        val levels = 2f.pow(bitDepth.coerceIn(2f, 16f))
        val step = downsampleFactor.toInt().coerceIn(1, 32)
        val dry = 1f - wet

        for (i in 0 until numFrames) {
            val dryL = bufL[i]
            val dryR = bufR[i]

            if (holdCounter <= 0) {
                heldL = round(dryL * levels) / levels
                heldR = round(dryR * levels) / levels
                holdCounter = step
            }
            holdCounter--

            bufL[i] = dryL * dry + heldL * wet
            bufR[i] = dryR * dry + heldR * wet
        }
    }
}
