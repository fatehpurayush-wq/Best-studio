package com.beatstudio.app.audio.effects

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class MultiBandEq(private val sampleRate: Float = 44100f) {
    // 8 bands: 60, 140, 320, 750, 1600, 3500, 7200, 13000 Hz
    private val bandFreqs = floatArrayOf(60f, 140f, 320f, 750f, 1600f, 3500f, 7200f, 13000f)
    private val bandsL = Array(8) { BiquadPeak() }
    private val bandsR = Array(8) { BiquadPeak() }

    fun reset() {
        bandsL.forEach { it.reset() }
        bandsR.forEach { it.reset() }
    }

    fun processSimple3Band(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        lowNorm: Float,
        midNorm: Float,
        highNorm: Float
    ) {
        val lowDb = (lowNorm - 0.5f) * 24f
        val midDb = (midNorm - 0.5f) * 24f
        val highDb = (highNorm - 0.5f) * 24f

        bandsL[0].configure(120f, 0.8f, lowDb, sampleRate)
        bandsR[0].configure(120f, 0.8f, lowDb, sampleRate)
        bandsL[3].configure(1000f, 0.9f, midDb, sampleRate)
        bandsR[3].configure(1000f, 0.9f, midDb, sampleRate)
        bandsL[6].configure(7500f, 0.8f, highDb, sampleRate)
        bandsR[6].configure(7500f, 0.8f, highDb, sampleRate)

        for (i in 0 until numFrames) {
            var l = bufL[i]
            var r = bufR[i]
            l = bandsL[0].process(l)
            l = bandsL[3].process(l)
            l = bandsL[6].process(l)

            r = bandsR[0].process(r)
            r = bandsR[3].process(r)
            r = bandsR[6].process(r)

            bufL[i] = l
            bufR[i] = r
        }
    }

    fun processPro8Band(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        bandNorms: List<Float>
    ) {
        for (b in 0 until 8) {
            val norm = bandNorms.getOrElse(b) { 0.5f }
            val db = (norm - 0.5f) * 24f
            bandsL[b].configure(bandFreqs[b], 1.2f, db, sampleRate)
            bandsR[b].configure(bandFreqs[b], 1.2f, db, sampleRate)
        }
        for (i in 0 until numFrames) {
            var l = bufL[i]
            var r = bufR[i]
            for (b in 0 until 8) {
                l = bandsL[b].process(l)
                r = bandsR[b].process(r)
            }
            bufL[i] = l
            bufR[i] = r
        }
    }

    private class BiquadPeak {
        private var b0 = 1f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f
        private var lastDb = 0f

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }

        fun configure(freq: Float, q: Float, gainDb: Float, sr: Float) {
            if (kotlin.math.abs(gainDb - lastDb) < 0.05f && lastDb != 0f) return
            lastDb = gainDb
            val a = 10f.pow(gainDb / 40f)
            val w0 = (2.0 * PI * freq / sr).toFloat()
            val alpha = sin(w0) / (2f * q)
            val cosW0 = cos(w0)

            val a0 = 1f + alpha / a
            b0 = (1f + alpha * a) / a0
            b1 = (-2f * cosW0) / a0
            b2 = (1f - alpha * a) / a0
            a1 = (-2f * cosW0) / a0
            a2 = (1f - alpha / a) / a0
        }

        fun process(x: Float): Float {
            if (kotlin.math.abs(lastDb) < 0.1f) return x
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x
            y2 = y1; y1 = y
            return y
        }
    }
}
