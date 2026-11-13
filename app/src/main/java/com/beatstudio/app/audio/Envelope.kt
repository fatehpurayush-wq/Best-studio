package com.beatstudio.app.audio

import kotlin.math.max

class Envelope {
    enum class Stage { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE }

    var stage: Stage = Stage.IDLE
        private set
    var level: Float = 0f
        private set

    private var attackRate = 0.01f
    private var decayRate = 0.001f
    private var sustainLevel = 0.7f
    private var releaseRate = 0.002f

    fun configure(
        attackSec: Float,
        decaySec: Float,
        sustain: Float,
        releaseSec: Float,
        sampleRate: Float
    ) {
        val minFadeSec = 0.003f // 3ms minimum to prevent clicks
        val aSamples = max(1f, max(minFadeSec, attackSec) * sampleRate)
        val dSamples = max(1f, max(minFadeSec, decaySec) * sampleRate)
        val rSamples = max(1f, max(minFadeSec, releaseSec) * sampleRate)

        attackRate = 1f / aSamples
        sustainLevel = sustain.coerceIn(0f, 1f)
        decayRate = (1f - sustainLevel).coerceAtLeast(0.0001f) / dSamples
        releaseRate = 1f / rSamples
    }

    fun trigger() {
        stage = Stage.ATTACK
    }

    fun release() {
        if (stage != Stage.IDLE) {
            stage = Stage.RELEASE
        }
    }

    fun forceStop() {
        stage = Stage.IDLE
        level = 0f
    }

    fun nextSample(): Float {
        when (stage) {
            Stage.IDLE -> return 0f
            Stage.ATTACK -> {
                level += attackRate
                if (level >= 1f) {
                    level = 1f
                    stage = Stage.DECAY
                }
            }
            Stage.DECAY -> {
                level -= decayRate
                if (level <= sustainLevel) {
                    level = sustainLevel
                    stage = Stage.SUSTAIN
                }
            }
            Stage.SUSTAIN -> {
                level = sustainLevel
            }
            Stage.RELEASE -> {
                level -= releaseRate
                if (level <= 0f) {
                    level = 0f
                    stage = Stage.IDLE
                }
            }
        }
        return level
    }

    val isActive: Boolean
        get() = stage != Stage.IDLE
}
