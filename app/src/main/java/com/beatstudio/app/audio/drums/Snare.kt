package com.beatstudio.app.audio.drums

import com.beatstudio.app.audio.Oscillator
import com.beatstudio.app.audio.StateVariableFilter
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

class SnareVoice {
    private var phase1 = 0f
    private var phase2 = 0f
    private var timeSec = 0f
    private var durationSec = 0.28f
    private var baseFreq = 185f
    private var velocity = 1f
    private val noiseOsc = Oscillator()
    private val filter = StateVariableFilter()
    var active = false
        private set

    fun trigger(vel: Float, pitchOffsetSemitones: Float) {
        active = true
        phase1 = 0f
        phase2 = 0f
        timeSec = 0f
        velocity = vel
        baseFreq = 185f * 2f.pow(pitchOffsetSemitones / 12f)
        filter.reset()
    }

    fun nextSample(sampleRate: Float): Float {
        if (!active) return 0f
        val dt = 1f / sampleRate
        timeSec += dt
        if (timeSec >= durationSec) {
            active = false
            return 0f
        }

        val pitchEnv = exp(-timeSec / 0.025f)
        val f1 = baseFreq * (1f + 0.8f * pitchEnv)
        val f2 = f1 * 1.52f
        phase1 = (phase1 + f1 * dt) % 1f
        phase2 = (phase2 + f2 * dt) % 1f

        val tone = (sin((phase1 * 2.0 * PI).toFloat()) + 0.5f * sin((phase2 * 2.0 * PI).toFloat())) *
            exp(-timeSec / 0.065f)

        val noise = filter.process(noiseOsc.nextWhiteNoise(), 2400f, 0.25f, sampleRate, mode = 1)
        val noiseEnv = exp(-timeSec / 0.11f)

        val fade = if (timeSec > durationSec - 0.01f) ((durationSec - timeSec) / 0.01f).coerceIn(0f, 1f) else 1f
        return tanh((tone * 0.65f + noise * noiseEnv * 0.85f) * 1.4f) * velocity * 0.8f * fade
    }
}
