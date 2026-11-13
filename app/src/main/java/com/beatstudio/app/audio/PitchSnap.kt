package com.beatstudio.app.audio

import com.beatstudio.app.domain.MusicalScale
import com.beatstudio.app.music.Scales
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow

/**
 * Offline & block-based simple Auto-Tune style pitch detector + scale snapper.
 * Uses normalized autocorrelation for pitch detection and a Hann-windowed granular pitch shifter.
 */
object PitchSnap {

    fun processBufferToScale(
        input: FloatArray,
        sampleRate: Int,
        rootNote: Int,
        scale: MusicalScale,
        strength: Float = 0.85f
    ): FloatArray {
        if (input.size < 1024 || scale == MusicalScale.CHROMATIC || strength <= 0.01f) {
            return input.copyOf()
        }
        val output = FloatArray(input.size)
        val frameSize = 1024
        val hop = 256
        val window = FloatArray(frameSize) { i ->
            (0.5 * (1.0 - cos(2.0 * PI * i / frameSize))).toFloat()
        }
        val normAcc = FloatArray(input.size)

        var pos = 0
        var lastRatio = 1f
        while (pos + frameSize <= input.size) {
            val detectedFreq = detectPitchHz(input, pos, frameSize, sampleRate)
            val targetRatio = if (detectedFreq > 75f && detectedFreq < 950f) {
                val snapped = Scales.snapFreqToScale(detectedFreq, rootNote, scale)
                val rawRatio = (snapped / detectedFreq).coerceIn(0.75f, 1.35f)
                1f + (rawRatio - 1f) * strength.coerceIn(0f, 1f)
            } else {
                1f
            }
            lastRatio = lastRatio * 0.65f + targetRatio * 0.35f

            for (i in 0 until frameSize) {
                val srcOffset = i * lastRatio
                val s0 = (pos + srcOffset.toInt()).coerceIn(0, input.lastIndex)
                val s1 = (s0 + 1).coerceAtMost(input.lastIndex)
                val frac = srcOffset - srcOffset.toInt()
                val sample = input[s0] * (1f - frac) + input[s1] * frac
                val w = window[i]
                output[pos + i] += sample * w
                normAcc[pos + i] += w
            }
            pos += hop
        }

        for (i in output.indices) {
            val n = normAcc[i]
            output[i] = if (n > 0.001f) (output[i] / n).coerceIn(-1f, 1f) else input[i]
        }
        return output
    }

    fun detectPitchHz(buffer: FloatArray, offset: Int, len: Int, sampleRate: Int): Float {
        var rms = 0f
        for (i in 0 until len) {
            val v = buffer[offset + i]
            rms += v * v
        }
        rms = kotlin.math.sqrt(rms / len)
        if (rms < 0.015f) return 0f

        val minLag = (sampleRate / 900).coerceAtLeast(16)
        val maxLag = (sampleRate / 75).coerceAtMost(len / 2)
        var bestLag = 0
        var bestCorr = 0f

        for (lag in minLag..maxLag) {
            var corr = 0f
            var energy1 = 0f
            var energy2 = 0f
            val count = len - lag
            var i = 0
            while (i < count) {
                val a = buffer[offset + i]
                val b = buffer[offset + i + lag]
                corr += a * b
                energy1 += a * a
                energy2 += b * b
                i += 2
            }
            val denom = kotlin.math.sqrt(energy1 * energy2) + 1e-6f
            val normCorr = corr / denom
            if (normCorr > bestCorr) {
                bestCorr = normCorr
                bestLag = lag
            }
        }

        return if (bestCorr > 0.65f && bestLag > 0) {
            sampleRate.toFloat() / bestLag
        } else {
            0f
        }
    }
}
