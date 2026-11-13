package com.beatstudio.app.audio.effects

import com.beatstudio.app.audio.Lfo

class FlangerEffect(private val sampleRate: Float = 44100f) {
    private val bufSize = 1024
    private val delayL = FloatArray(bufSize)
    private val delayR = FloatArray(bufSize)
    private var writeIdx = 0
    private val lfo = Lfo()

    fun reset() {
        delayL.fill(0f)
        delayR.fill(0f)
        writeIdx = 0
        lfo.reset()
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        rateNorm: Float,
        feedbackNorm: Float,
        mix: Float
    ) {
        val wet = mix.coerceIn(0f, 1f)
        if (wet <= 0.001f) return
        val rateHz = 0.12f + rateNorm.coerceIn(0f, 1f) * 3.2f
        val fb = feedbackNorm.coerceIn(0f, 0.85f)
        val dry = 1f - wet * 0.4f

        for (i in 0 until numFrames) {
            val lfoVal = 0.5f + 0.5f * lfo.nextSample(rateHz, sampleRate)
            val offset = (12f + lfoVal * 220f).coerceIn(2f, (bufSize - 3).toFloat())

            var pos = writeIdx - offset
            if (pos < 0f) pos += bufSize
            val i0 = pos.toInt() % bufSize
            val i1 = (i0 + 1) % bufSize
            val frac = pos - pos.toInt()

            val dL = delayL[i0] * (1f - frac) + delayL[i1] * frac
            val dR = delayR[i0] * (1f - frac) + delayR[i1] * frac

            val inL = bufL[i]
            val inR = bufR[i]

            delayL[writeIdx] = inL + dL * fb
            delayR[writeIdx] = inR + dR * fb

            bufL[i] = inL * dry + dL * wet
            bufR[i] = inR * dry + dR * wet

            writeIdx = (writeIdx + 1) % bufSize
        }
    }
}
