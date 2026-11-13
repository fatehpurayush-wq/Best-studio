package com.beatstudio.app.audio.drums

import com.beatstudio.app.audio.Oscillator
import com.beatstudio.app.audio.StateVariableFilter
import com.beatstudio.app.domain.DrumSoundType
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

class PercussionVoice {
    private var type = DrumSoundType.RIM
    private var phase1 = 0f
    private var phase2 = 0f
    private var phase3 = 0f
    private var timeSec = 0f
    private var durationSec = 0.25f
    private var velocity = 1f
    private var pitchScale = 1f
    private val noiseOsc = Oscillator()
    private val filter = StateVariableFilter()
    var active = false
        private set

    fun trigger(soundType: DrumSoundType, vel: Float, pitchOffsetSemitones: Float) {
        type = soundType
        active = true
        phase1 = 0f
        phase2 = 0f
        phase3 = 0f
        timeSec = 0f
        velocity = vel
        pitchScale = 2f.pow(pitchOffsetSemitones / 12f)
        filter.reset()

        durationSec = when (soundType) {
            DrumSoundType.RIM -> 0.08f
            DrumSoundType.TOM -> 0.35f
            DrumSoundType.SHAKER -> 0.12f
            DrumSoundType.TAMBOURINE -> 0.22f
            DrumSoundType.CRASH -> 1.4f
            DrumSoundType.MEMPHIS_HIT -> 0.45f
            DrumSoundType.VINYL_CRACKLE -> 0.6f
            DrumSoundType.FX_RISER -> 1.8f
            DrumSoundType.FX_IMPACT -> 1.3f
            DrumSoundType.FX_REVERSE_CYMBAL -> 1.2f
            DrumSoundType.FX_NOISE_SWEEP -> 1.5f
            else -> 0.2f
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

        val attack = (timeSec / 0.002f).coerceIn(0f, 1f)
        val fadeOut = if (timeSec > durationSec - 0.01f) ((durationSec - timeSec) / 0.01f).coerceIn(0f, 1f) else 1f

        val out = when (type) {
            DrumSoundType.RIM -> {
                val f1 = 440f * pitchScale
                val f2 = 1650f * pitchScale
                phase1 = (phase1 + f1 * dt) % 1f
                phase2 = (phase2 + f2 * dt) % 1f
                val sig = sin((phase1 * 2.0 * PI).toFloat()) * 0.6f + sin((phase2 * 2.0 * PI).toFloat()) * 0.4f
                tanh(sig * 2.2f) * exp(-timeSec / 0.016f)
            }
            DrumSoundType.TOM -> {
                val f = (195f * pitchScale) * (1f + 0.6f * exp(-timeSec / 0.04f))
                phase1 = (phase1 + f * dt) % 1f
                sin((phase1 * 2.0 * PI).toFloat()) * exp(-timeSec / 0.11f)
            }
            DrumSoundType.SHAKER -> {
                val env = (timeSec / 0.025f).coerceAtMost(1f) * exp(-timeSec / 0.035f)
                filter.process(noiseOsc.nextWhiteNoise(), 5400f * pitchScale, 0.4f, sampleRate, mode = 2) * env * 1.4f
            }
            DrumSoundType.TAMBOURINE -> {
                val jingle = filter.process(noiseOsc.nextWhiteNoise(), 7800f * pitchScale, 0.65f, sampleRate, mode = 2)
                val env = exp(-timeSec / 0.055f)
                jingle * env * 1.6f
            }
            DrumSoundType.CRASH -> {
                val raw = noiseOsc.nextWhiteNoise()
                val hp = filter.process(raw, 4200f, 0.3f, sampleRate, mode = 1)
                hp * exp(-timeSec / 0.45f) * 0.7f
            }
            DrumSoundType.MEMPHIS_HIT -> {
                // Dark dissonant Memphis brass/stab hit (vocal-free synth chord stab)
                val f1 = 130.8f * pitchScale
                val f2 = f1 * 1.189f // minor 3rd
                val f3 = f1 * 1.498f // 5th
                phase1 = (phase1 + f1 * dt) % 1f
                phase2 = (phase2 + f2 * dt) % 1f
                phase3 = (phase3 + f3 * dt) % 1f
                val saw = ((2f * phase1 - 1f) + (2f * phase2 - 1f) + (2f * phase3 - 1f)) * 0.33f
                val f = filter.process(saw, 1100f * exp(-timeSec / 0.18f) + 280f, 0.45f, sampleRate, mode = 0)
                tanh(f * 2.1f) * exp(-timeSec / 0.16f)
            }
            DrumSoundType.VINYL_CRACKLE -> {
                val n = noiseOsc.nextWhiteNoise()
                val pop = if (n > 0.985f) n * 0.9f else n * 0.06f
                filter.process(pop, 2800f, 0.1f, sampleRate, mode = 2) * 0.55f
            }
            DrumSoundType.FX_RISER -> {
                val prog = (timeSec / durationSec).coerceIn(0f, 1f)
                val f = 140f + prog * prog * 2400f
                phase1 = (phase1 + f * dt) % 1f
                val saw = 2f * phase1 - 1f
                val filtered = filter.process(saw * 0.6f + noiseOsc.nextWhiteNoise() * 0.4f, 250f + prog * 7000f, 0.55f, sampleRate, mode = 0)
                filtered * prog * 0.7f
            }
            DrumSoundType.FX_IMPACT -> {
                val subF = 110f * exp(-timeSec / 0.08f) + 32f
                phase1 = (phase1 + subF * dt) % 1f
                val sub = sin((phase1 * 2.0 * PI).toFloat()) * exp(-timeSec / 0.45f)
                val burst = filter.process(noiseOsc.nextWhiteNoise(), 1600f * exp(-timeSec / 0.25f) + 180f, 0.2f, sampleRate, mode = 0) * exp(-timeSec / 0.35f)
                tanh((sub + burst * 0.7f) * 1.5f) * 0.85f
            }
            DrumSoundType.FX_REVERSE_CYMBAL -> {
                val prog = (timeSec / durationSec).coerceIn(0f, 1f)
                val env = prog * prog * prog
                val hp = filter.process(noiseOsc.nextWhiteNoise(), 3500f + prog * 4000f, 0.35f, sampleRate, mode = 1)
                hp * env * 0.75f
            }
            DrumSoundType.FX_NOISE_SWEEP -> {
                val prog = (timeSec / durationSec).coerceIn(0f, 1f)
                val cutoff = 300f + sin((prog * PI).toFloat()) * 6500f
                val bp = filter.process(noiseOsc.nextWhiteNoise(), cutoff, 0.65f, sampleRate, mode = 2)
                bp * sin((prog * PI).toFloat()) * 0.85f
            }
            else -> 0f
        }

        return out * attack * fadeOut * velocity * 0.85f
    }
}
