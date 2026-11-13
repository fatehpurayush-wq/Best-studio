package com.beatstudio.app.audio.drums

import com.beatstudio.app.music.Scales
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

class Drum808Voice {
    private var phase = 0f
    private var timeSec = 0f
    private var currentFreq = 45f
    private var targetFreq = 45f
    private var glideCoeff = 0.003f
    private var ampDecay = 0.95f
    private var durationSec = 1.6f
    private var velocity = 1f
    private var releasing = false
    private var releaseLevel = 1f
    var active = false
        private set

    fun trigger(midiPitch: Int, vel: Float, pitchOffsetSemitones: Float, glideTimeNorm: Float = 0.25f) {
        val nextFreq = Scales.midiToFreq(midiPitch + pitchOffsetSemitones).coerceIn(26f, 220f)
        if (!active) {
            currentFreq = nextFreq * 2.2f // subtle punchy pitch drop on initial hit
            phase = 0f
        }
        targetFreq = nextFreq
        glideCoeff = (0.0005f + (1f - glideTimeNorm.coerceIn(0f, 1f)) * 0.015f)
        active = true
        releasing = false
        releaseLevel = 1f
        timeSec = 0f
        velocity = vel
    }

    fun release() {
        releasing = true
    }

    fun nextSample(sampleRate: Float): Float {
        if (!active) return 0f
        val dt = 1f / sampleRate
        timeSec += dt
        if (timeSec >= durationSec) {
            active = false
            return 0f
        }

        currentFreq += (targetFreq - currentFreq) * glideCoeff
        phase += currentFreq * dt
        if (phase >= 1f) phase -= 1f

        val fundamental = sin((phase * 2.0 * PI).toFloat())
        val secondHarmonic = sin((phase * 4.0 * PI).toFloat()) * 0.28f
        val attackRamp = (timeSec / 0.004f).coerceIn(0f, 1f)
        val env = exp(-timeSec / ampDecay) * attackRamp

        if (releasing) {
            releaseLevel -= dt / 0.04f
            if (releaseLevel <= 0f) {
                active = false
                return 0f
            }
        }

        val saturated = tanh((fundamental + secondHarmonic) * 1.9f)
        return saturated * env * releaseLevel * velocity * 0.85f
    }
}
