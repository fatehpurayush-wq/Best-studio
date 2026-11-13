package com.beatstudio.app.audio.drums

import com.beatstudio.app.audio.Oscillator
import com.beatstudio.app.audio.StateVariableFilter
import kotlin.math.exp
import kotlin.math.pow

class HiHatVoice {
    private val oscPhases = FloatArray(6)
    private val baseRatios = floatArrayOf(263f, 389f, 517f, 653f, 811f, 1021f)
    private var timeSec = 0f
    private var durationSec = 0.09f
    private var decaySec = 0.035f
    private var velocity = 1f
    private var pitchScale = 1f
    private val noiseOsc = Oscillator()
    private val hpFilter = StateVariableFilter()
    var active = false
        private set

    fun trigger(isOpen: Boolean, vel: Float, pitchOffsetSemitones: Float) {
        active = true
        timeSec = 0f
        velocity = vel
        pitchScale = 2f.pow(pitchOffsetSemitones / 12f)
        decaySec = if (isOpen) 0.24f else 0.028f
        durationSec = if (isOpen) 0.55f else 0.09f
        hpFilter.reset()
    }

    fun nextSample(sampleRate: Float): Float {
        if (!active) return 0f
        val dt = 1f / sampleRate
        timeSec += dt
        if (timeSec >= durationSec) {
            active = false
            return 0f
        }

        var metalSum = 0f
        for (i in 0 until 6) {
            val f = baseRatios[i] * pitchScale
            oscPhases[i] = (oscPhases[i] + f * dt) % 1f
            metalSum += if (oscPhases[i] < 0.5f) 1f else -1f
        }
        metalSum /= 6f

        val raw = metalSum * 0.65f + noiseOsc.nextWhiteNoise() * 0.35f
        val crisp = hpFilter.process(raw, 7200f * pitchScale.coerceIn(0.6f, 1.5f), 0.35f, sampleRate, mode = 1)
        val env = exp(-timeSec / decaySec) * (timeSec / 0.0015f).coerceIn(0f, 1f)
        val fade = if (timeSec > durationSec - 0.005f) ((durationSec - timeSec) / 0.005f).coerceIn(0f, 1f) else 1f
        return crisp * env * velocity * 0.65f * fade
    }
}
