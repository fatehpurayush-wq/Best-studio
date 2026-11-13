package com.beatstudio.app.audio.effects

import com.beatstudio.app.audio.Lfo

class ChorusEffect(private val sampleRate: Float = 44100f) {
    private val bufSize = 4096
    private val delayL = FloatArray(bufSize)
    private val delayR = FloatArray(bufSize)
    private var writeIdx = 0
    private val lfoL = Lfo()
    private val lfoR = Lfo()

    fun reset() {
        delayL.fill(0f)
        delayR.fill(0f)
        writeIdx = 0
        lfoL.reset()
        lfoR.reset()
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
        val rateHz = 0.25f + rateNorm.coerceIn(0f, 1f) * 4.5f
        val depthSamples = 40f + depthNorm.coerceIn(0f, 1f) * 260f
        val baseSamples = 480f
        val dry = 1f - wet * 0.4f

        for (i in 0 until numFrames) {
            val xL = bufL[i]
            val xR = bufR[i]
            delayL[writeIdx] = xL
            delayR[writeIdx] = xR

            val modL = lfoL.nextSample(rateHz, sampleRate)
            val modR = lfoR.nextSample(rateHz * 1.07f, sampleRate)

            val offL = (baseSamples + modL * depthSamples).coerceIn(2f, (bufSize - 3).toFloat())
            val offR = (baseSamples + modR * depthSamples).coerceIn(2f, (bufSize - 3).toFloat())

            val wetL = readInterp(delayL, offL)
            val wetR = readInterp(delayR, offR)

            bufL[i] = xL * dry + wetL * wet
            bufR[i] = xR * dry + wetR * wet

            writeIdx = (writeIdx + 1) % bufSize
        }
    }

    private fun readInterp(buf: FloatArray, offset: Float): Float {
        var pos = writeIdx - offset
        if (pos < 0f) pos += bufSize
        val i0 = pos.toInt() % bufSize
        val i1 = (i0 + 1) % bufSize
        val frac = pos - pos.toInt()
        return buf[i0] * (1f - frac) + buf[i1] * frac
    }
}
