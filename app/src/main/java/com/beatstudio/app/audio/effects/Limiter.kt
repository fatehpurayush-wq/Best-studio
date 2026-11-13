package com.beatstudio.app.audio.effects

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.tanh

class MasterLimiter(private val sampleRate: Float = 44100f) {
    private var env = 1f
    @Volatile var lastPeakLeft = 0f
        private set
    @Volatile var lastPeakRight = 0f
        private set

    fun reset() {
        env = 1f
        lastPeakLeft = 0f
        lastPeakRight = 0f
    }

    fun processBlock(bufL: FloatArray, bufR: FloatArray, numFrames: Int, masterGain: Float) {
        val ceiling = 0.96f
        val releaseCoeff = exp(-1f / (0.06f * sampleRate))
        var maxL = 0f
        var maxR = 0f

        for (i in 0 until numFrames) {
            var l = bufL[i] * masterGain
            var r = bufR[i] * masterGain

            val peak = max(abs(l), abs(r))
            val targetGain = if (peak > ceiling) ceiling / peak else 1f
            if (targetGain < env) {
                env = targetGain
            } else {
                env = releaseCoeff * env + (1f - releaseCoeff) * targetGain
            }

            l = tanh(l * env * 1.05f).coerceIn(-0.98f, 0.98f)
            r = tanh(r * env * 1.05f).coerceIn(-0.98f, 0.98f)

            bufL[i] = l
            bufR[i] = r

            if (abs(l) > maxL) maxL = abs(l)
            if (abs(r) > maxR) maxR = abs(r)
        }

        lastPeakLeft = maxL
        lastPeakRight = maxR
    }
}

object StereoWidener {
    fun processBlock(bufL: FloatArray, bufR: FloatArray, numFrames: Int, width: Float) {
        val w = width.coerceIn(0f, 2f)
        if (abs(w - 1f) < 0.01f) return
        for (i in 0 until numFrames) {
            val mid = (bufL[i] + bufR[i]) * 0.5f
            val side = (bufL[i] - bufR[i]) * 0.5f * w
            bufL[i] = mid + side
            bufR[i] = mid - side
        }
    }
}

class StutterGateEffect {
    private var smoothGain = 1f

    fun reset() {
        smoothGain = 1f
    }

    fun processBlock(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        subStepFraction: Float, // 0..1 within current step
        rateSteps: Int,
        depth: Float
    ) {
        val gateOpen = if (rateSteps <= 1) {
            subStepFraction < 0.55f
        } else {
            ((subStepFraction * 2f) % 1f) < 0.55f
        }
        val target = if (gateOpen) 1f else (1f - depth.coerceIn(0f, 1f))
        for (i in 0 until numFrames) {
            smoothGain += (target - smoothGain) * 0.08f
            bufL[i] *= smoothGain
            bufR[i] *= smoothGain
        }
    }
}
