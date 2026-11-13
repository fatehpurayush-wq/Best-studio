package com.beatstudio.app.audio.effects

import com.beatstudio.app.domain.ReverbType

class StereoReverb(sampleRate: Int = 44100) {
    private val combLengthsL = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491)
    private val combLengthsR = intArrayOf(1139, 1211, 1300, 1379, 1445, 1514)
    private val allPassLengths = intArrayOf(556, 441, 341, 225)

    private val combsL = Array(combLengthsL.size) { FloatArray((combLengthsL[it] * sampleRate / 44100f).toInt().coerceAtLeast(64)) }
    private val combsR = Array(combLengthsR.size) { FloatArray((combLengthsR[it] * sampleRate / 44100f).toInt().coerceAtLeast(64)) }
    private val combIdxL = IntArray(combLengthsL.size)
    private val combIdxR = IntArray(combLengthsR.size)
    private val combFilterL = FloatArray(combLengthsL.size)
    private val combFilterR = FloatArray(combLengthsR.size)

    private val allPassL = Array(allPassLengths.size) { FloatArray((allPassLengths[it] * sampleRate / 44100f).toInt().coerceAtLeast(32)) }
    private val allPassR = Array(allPassLengths.size) { FloatArray(((allPassLengths[it] + 23) * sampleRate / 44100f).toInt().coerceAtLeast(32)) }
    private val apIdxL = IntArray(allPassLengths.size)
    private val apIdxR = IntArray(allPassLengths.size)

    fun reset() {
        combsL.forEach { it.fill(0f) }
        combsR.forEach { it.fill(0f) }
        allPassL.forEach { it.fill(0f) }
        allPassR.forEach { it.fill(0f) }
        combFilterL.fill(0f)
        combFilterR.fill(0f)
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        type: ReverbType,
        mix: Float,
        size: Float,
        damp: Float
    ) {
        val wet = mix.coerceIn(0f, 1f)
        if (wet <= 0.001f) return

        val typeRoomBoost = when (type) {
            ReverbType.ROOM -> 0.72f
            ReverbType.HALL -> 0.84f
            ReverbType.PLATE -> 0.80f
            ReverbType.HUGE -> 0.93f
        }
        val feedback = (0.5f + size.coerceIn(0f, 1f) * 0.44f).coerceAtMost(0.96f) * (0.65f + typeRoomBoost * 0.35f)
        val damping = when (type) {
            ReverbType.PLATE -> (damp * 0.45f).coerceIn(0.05f, 0.7f)
            else -> (0.15f + damp * 0.65f).coerceIn(0.05f, 0.85f)
        }
        val dampInv = 1f - damping
        val dry = 1f - wet * 0.45f

        for (i in 0 until numFrames) {
            val inL = bufL[i]
            val inR = bufR[i]
            val monoIn = (inL + inR) * 0.03f

            var outL = 0f
            var outR = 0f

            for (c in combsL.indices) {
                val bL = combsL[c]
                val idxL = combIdxL[c]
                val yL = bL[idxL]
                combFilterL[c] = yL * dampInv + combFilterL[c] * damping
                bL[idxL] = monoIn + combFilterL[c] * feedback
                combIdxL[c] = if (idxL + 1 >= bL.size) 0 else idxL + 1
                outL += yL

                val bR = combsR[c]
                val idxR = combIdxR[c]
                val yR = bR[idxR]
                combFilterR[c] = yR * dampInv + combFilterR[c] * damping
                bR[idxR] = monoIn + combFilterR[c] * feedback
                combIdxR[c] = if (idxR + 1 >= bR.size) 0 else idxR + 1
                outR += yR
            }

            for (a in allPassL.indices) {
                val apL = allPassL[a]
                val iL = apIdxL[a]
                val bufOutL = apL[iL]
                apL[iL] = outL + bufOutL * 0.5f
                outL = bufOutL - outL
                apIdxL[a] = if (iL + 1 >= apL.size) 0 else iL + 1

                val apR = allPassR[a]
                val iR = apIdxR[a]
                val bufOutR = apR[iR]
                apR[iR] = outR + bufOutR * 0.5f
                outR = bufOutR - outR
                apIdxR[a] = if (iR + 1 >= apR.size) 0 else iR + 1
            }

            bufL[i] = inL * dry + outL * wet * 0.35f
            bufR[i] = inR * dry + outR * wet * 0.35f
        }
    }
}
