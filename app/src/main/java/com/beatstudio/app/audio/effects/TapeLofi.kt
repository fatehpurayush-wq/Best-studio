package com.beatstudio.app.audio.effects

import com.beatstudio.app.audio.Lfo
import com.beatstudio.app.audio.Oscillator
import com.beatstudio.app.audio.StateVariableFilter
import kotlin.math.tanh

class TapeLofiEffect(private val sampleRate: Float = 44100f) {
    private val delaySize = 2048
    private val delayL = FloatArray(delaySize)
    private val delayR = FloatArray(delaySize)
    private var writeIdx = 0
    private val wowLfo = Lfo()
    private val flutterLfo = Lfo()
    private val hissOsc = Oscillator()
    private val lpL = StateVariableFilter()
    private val lpR = StateVariableFilter()
    private val hissFilter = StateVariableFilter()

    fun reset() {
        delayL.fill(0f)
        delayR.fill(0f)
        writeIdx = 0
        wowLfo.reset()
        flutterLfo.reset()
        lpL.reset()
        lpR.reset()
        hissFilter.reset()
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        wobble: Float,
        hiss: Float,
        warmth: Float
    ) {
        val cutoff = (16000f - warmth.coerceIn(0f, 1f) * 12500f).coerceIn(1600f, 16000f)
        val wobbleDepthSamples = wobble.coerceIn(0f, 1f) * 95f
        val hissGain = hiss.coerceIn(0f, 1f) * 0.025f

        for (i in 0 until numFrames) {
            delayL[writeIdx] = bufL[i]
            delayR[writeIdx] = bufR[i]

            val wow = wowLfo.nextSample(0.65f, sampleRate)
            val flutter = flutterLfo.nextSample(5.4f, sampleRate) * 0.3f
            val delayOffset = (120f + (wow + flutter) * wobbleDepthSamples).coerceIn(2f, (delaySize - 3).toFloat())

            var readPos = writeIdx - delayOffset
            if (readPos < 0f) readPos += delaySize
            val idx0 = readPos.toInt() % delaySize
            val idx1 = (idx0 + 1) % delaySize
            val frac = readPos - readPos.toInt()

            val wL = delayL[idx0] * (1f - frac) + delayL[idx1] * frac
            val wR = delayR[idx0] * (1f - frac) + delayR[idx1] * frac

            val tapeHiss = hissFilter.process(hissOsc.nextWhiteNoise(), 4500f, 0.1f, sampleRate, mode = 2) * hissGain
            val satL = tanh((wL + tapeHiss) * (1f + warmth * 0.6f))
            val satR = tanh((wR + tapeHiss) * (1f + warmth * 0.6f))

            bufL[i] = lpL.process(satL, cutoff, 0.12f, sampleRate, mode = 0)
            bufR[i] = lpR.process(satR, cutoff, 0.12f, sampleRate, mode = 0)

            writeIdx++
            if (writeIdx >= delaySize) writeIdx = 0
        }
    }
}
