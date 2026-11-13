package com.beatstudio.app.audio.effects

class StereoDelay(private val sampleRate: Int = 44100) {
    private val maxSamples = sampleRate * 2
    private val bufL = FloatArray(maxSamples)
    private val bufR = FloatArray(maxSamples)
    private var writeIdx = 0

    fun reset() {
        bufL.fill(0f)
        bufR.fill(0f)
        writeIdx = 0
    }

    fun processBlock(
        inL: FloatArray,
        inR: FloatArray,
        numFrames: Int,
        timeMs: Float,
        feedback: Float,
        mix: Float,
        pingPong: Boolean
    ) {
        val wet = mix.coerceIn(0f, 1f)
        if (wet <= 0.001f) return
        val delaySamples = ((timeMs.coerceIn(30f, 1500f) / 1000f) * sampleRate).toInt().coerceIn(1, maxSamples - 1)
        val fb = feedback.coerceIn(0f, 0.88f)

        for (i in 0 until numFrames) {
            var readIdx = writeIdx - delaySamples
            if (readIdx < 0) readIdx += maxSamples

            val dL = bufL[readIdx]
            val dR = bufR[readIdx]

            val xL = inL[i]
            val xR = inR[i]

            if (pingPong) {
                val mono = (xL + xR) * 0.5f
                bufL[writeIdx] = mono + dR * fb
                bufR[writeIdx] = dL * fb
            } else {
                bufL[writeIdx] = xL + dL * fb
                bufR[writeIdx] = xR + dR * fb
            }

            inL[i] = xL + dL * wet
            inR[i] = xR + dR * wet

            writeIdx++
            if (writeIdx >= maxSamples) writeIdx = 0
        }
    }
}
