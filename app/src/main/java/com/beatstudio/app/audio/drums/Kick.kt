package com.beatstudio.app.audio.drums

import com.beatstudio.app.domain.DrumSoundType
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

class KickVoice {
    private var phase = 0f
    private var timeSec = 0f
    private var durationSec = 0.45f
    private var startFreq = 160f
    private var endFreq = 46f
    private var pitchDecay = 0.045f
    private var ampDecay = 0.18f
    private var drive = 1.6f
    private var clickAmount = 0.35f
    private var velocity = 1f
    var active = false
        private set

    fun trigger(type: DrumSoundType, vel: Float, pitchOffsetSemitones: Float) {
        active = true
        phase = 0f
        timeSec = 0f
        velocity = vel
        val pitchScale = 2f.pow(pitchOffsetSemitones / 12f)
        when (type) {
            DrumSoundType.KICK_PUNCHY -> {
                startFreq = 190f * pitchScale
                endFreq = 50f * pitchScale
                pitchDecay = 0.038f
                ampDecay = 0.18f
                durationSec = 0.42f
                drive = 2.0f
                clickAmount = 0.45f
            }
            DrumSoundType.KICK_SUB -> {
                startFreq = 115f * pitchScale
                endFreq = 38f * pitchScale
                pitchDecay = 0.055f
                ampDecay = 0.32f
                durationSec = 0.60f
                drive = 1.3f
                clickAmount = 0.15f
            }
            else -> { // KICK_808
                startFreq = 155f * pitchScale
                endFreq = 42f * pitchScale
                pitchDecay = 0.048f
                ampDecay = 0.45f
                durationSec = 0.85f
                drive = 1.85f
                clickAmount = 0.25f
            }
        }
    }

    fun nextSample(sampleRate: Float): Float {
        if (!active) return 0f
        val dt = 1f / sampleRate
        timeSec += dt
        if (timeSec >= durationSec) {
            active = false
            return 0f
        }

        val fadeOut = if (timeSec > durationSec - 0.01f) {
            ((durationSec - timeSec) / 0.01f).coerceIn(0f, 1f)
        } else 1f

        val freq = endFreq + (startFreq - endFreq) * exp(-timeSec / pitchDecay)
        phase += freq * dt
        if (phase >= 1f) phase -= 1f

        val body = sin((phase * 2.0 * PI).toFloat())
        val ampEnv = exp(-timeSec / ampDecay)
        val click = if (timeSec < 0.008f) {
            sin((timeSec * 2200.0 * PI).toFloat()) * (1f - timeSec / 0.008f) * clickAmount
        } else 0f

        val raw = (body * ampEnv + click) * drive
        return tanh(raw) * velocity * 0.9f * fadeOut
    }
}
