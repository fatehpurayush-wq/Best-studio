package com.beatstudio.app.audio.effects

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/**
 * Dual-grain Hann-windowed real-time pitch shifter with zero allocations in processBlock.
 */
class PitchShifterEffect {
    private val bufSize = 4096
    private val ringL = FloatArray(bufSize)
    private val ringR = FloatArray(bufSize)
    private var writeIdx = 0
    private var grainPhase = 0f

    fun reset() {
        ringL.fill(0f)
        ringR.fill(0f)
        writeIdx = 0
        grainPhase = 0f
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        semitones: Float,
        mix: Float
    ) {
        val wet = mix.coerceIn(0f, 1f)
        if (wet <= 0.001f || kotlin.math.abs(semitones) < 0.05f) return

        val ratio = 2f.pow(semitones.coerceIn(-12f, 12f) / 12f)
        val grainSamples = 1536f
        val phaseInc = (1f - ratio) / grainSamples
        val dry = 1f - wet

        for (i in 0 until numFrames) {
            val inL = bufL[i]
            val inR = bufR[i]
            ringL[writeIdx] = inL
            ringR[writeIdx] = inR

            val p1 = grainPhase
            val p2 = (grainPhase + 0.5f) % 1f

            val w1 = 0.5f * (1f - cos((2.0 * PI * p1).toFloat()))
            val w2 = 0.5f * (1f - cos((2.0 * PI * p2).toFloat()))

            val d1 = 48f + p1 * grainSamples
            val d2 = 48f + p2 * grainSamples

            val s1L = readInterp(ringL, d1)
            val s2L = readInterp(ringL, d2)
            val s1R = readInterp(ringR, d1)
            val s2R = readInterp(ringR, d2)

            val shiftedL = s1L * w1 + s2L * w2
            val shiftedR = s1R * w1 + s2R * w2

            bufL[i] = inL * dry + shiftedL * wet
            bufR[i] = inR * dry + shiftedR * wet

            grainPhase = (grainPhase + phaseInc) % 1f
            if (grainPhase < 0f) grainPhase += 1f
            writeIdx = (writeIdx + 1) % bufSize
        }
    }

    private fun readInterp(buf: FloatArray, delay: Float): Float {
        var pos = writeIdx - delay
        while (pos < 0f) pos += bufSize
        val i0 = pos.toInt() % bufSize
        val i1 = (i0 + 1) % bufSize
        val frac = pos - pos.toInt()
        return buf[i0] * (1f - frac) + buf[i1] * frac
    }
}
