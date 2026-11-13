package com.beatstudio.app.audio.effects

import com.beatstudio.app.audio.Lfo

class PhaserEffect(private val sampleRate: Float = 44100f) {
    private val stagesL = FloatArray(4)
    private val stagesR = FloatArray(4)
    private val lfo = Lfo()

    fun reset() {
        stagesL.fill(0f)
        stagesR.fill(0f)
        lfo.reset()
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        rateNorm: Float,
        depthNorm: Float,
        mix: Float
    ) {
        val wet = mix.coerceIn(0f, 1f)
        if (wet <= 0.001f) return
        val rateHz = 0.15f + rateNorm.coerceIn(0f, 1f) * 3.8f
        val depth = depthNorm.coerceIn(0f, 1f)
        val dry = 1f - wet * 0.45f

        for (i in 0 until numFrames) {
            val lfoVal = 0.5f + 0.5f * lfo.nextSample(rateHz, sampleRate)
            val freq = 220f + lfoVal * depth * 3200f
            val a1 = ((1f - (2f * 3.14159f * freq / sampleRate)) / (1f + (2f * 3.14159f * freq / sampleRate))).coerceIn(-0.95f, 0.95f)

            var yL = bufL[i]
            var yR = bufR[i]
            for (s in 0 until 4) {
                val outL = a1 * (yL - stagesL[s]) + stagesL[s]
                stagesL[s] = yL + a1 * outL * 0.35f
                yL = outL

                val outR = a1 * (yR - stagesR[s]) + stagesR[s]
                stagesR[s] = yR + a1 * outR * 0.35f
                yR = outR
            }

            bufL[i] = bufL[i] * dry + yL * wet
            bufR[i] = bufR[i] * dry + yR * wet
        }
    }
}
