package com.beatstudio.app.audio.effects

import com.beatstudio.app.audio.StateVariableFilter
import kotlin.math.tanh

class DistortionEffect(private val sampleRate: Float = 44100f) {
    private val toneFilterL = StateVariableFilter()
    private val toneFilterR = StateVariableFilter()

    fun reset() {
        toneFilterL.reset()
        toneFilterR.reset()
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        drive: Float,
        tone: Float,
        mix: Float,
        isOverdrive: Boolean
    ) {
        val wet = mix.coerceIn(0f, 1f)
        if (wet <= 0.001f) return

        val gain = 1f + drive.coerceIn(0f, 1f) * (if (isOverdrive) 10f else 18f)
        val cutoffHz = 900f + tone.coerceIn(0f, 1f) * 14000f
        val dry = 1f - wet

        for (i in 0 until numFrames) {
            val dryL = bufL[i]
            val dryR = bufR[i]

            val satL = if (isOverdrive) {
                // Warm asymmetric soft clipping
                val x = dryL * gain
                if (x >= 0f) tanh(x) else tanh(x * 0.82f)
            } else {
                tanh(dryL * gain).coerceIn(-0.92f, 0.92f)
            }

            val satR = if (isOverdrive) {
                val x = dryR * gain
                if (x >= 0f) tanh(x) else tanh(x * 0.82f)
            } else {
                tanh(dryR * gain).coerceIn(-0.92f, 0.92f)
            }

            val shapedL = toneFilterL.process(satL, cutoffHz, 0.1f, sampleRate, mode = 0)
            val shapedR = toneFilterR.process(satR, cutoffHz, 0.1f, sampleRate, mode = 0)

            bufL[i] = dryL * dry + shapedL * wet
            bufR[i] = dryR * dry + shapedR * wet
        }
    }
}
