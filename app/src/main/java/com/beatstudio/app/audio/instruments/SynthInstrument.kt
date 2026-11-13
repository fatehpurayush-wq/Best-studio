package com.beatstudio.app.audio.instruments

import com.beatstudio.app.audio.Envelope
import com.beatstudio.app.audio.Lfo
import com.beatstudio.app.audio.Oscillator
import com.beatstudio.app.audio.StateVariableFilter
import com.beatstudio.app.domain.LfoTarget
import com.beatstudio.app.domain.OscWaveform
import com.beatstudio.app.domain.SynthParams
import com.beatstudio.app.domain.SynthPresetId
import com.beatstudio.app.music.Scales
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

class SynthVoice {
    private val oscs = Array(5) { Oscillator() }
    private val subOsc = Oscillator()
    private val modOsc = Oscillator()
    private val env = Envelope()
    private val filter = StateVariableFilter()
    private val lfo = Lfo()

    private var params = SynthParams()
    private var currentFreq = 261.63f
    private var targetFreq = 261.63f
    private var velocity = 0.85f
    private var midiPitch = 60

    val isActive: Boolean
        get() = env.isActive

    val currentMidi: Int
        get() = midiPitch

    fun trigger(midi: Int, vel: Float, synthParams: SynthParams, sampleRate: Float, previousFreq: Float? = null) {
        midiPitch = midi
        params = synthParams
        velocity = vel.coerceIn(0.05f, 1f)
        targetFreq = Scales.midiToFreq(midi.toFloat())
        currentFreq = if (previousFreq != null && synthParams.glideTime > 0.02f) previousFreq else targetFreq

        val atkSec = 0.003f + synthParams.attack.pow(2f) * 1.4f
        val decSec = 0.03f + synthParams.decay.pow(2f) * 1.8f
        val relSec = 0.015f + synthParams.release.pow(2f) * 2.6f
        env.configure(atkSec, decSec, synthParams.sustain, relSec, sampleRate)
        env.trigger()

        if (previousFreq == null) {
            filter.reset()
            lfo.reset()
            oscs.forEachIndexed { i, o -> o.reset(i * 0.17f) }
            subOsc.reset(0f)
            modOsc.reset(0f)
        }
    }

    fun release() {
        env.release()
    }

    fun forceStop() {
        env.forceStop()
    }

    fun nextStereoSample(sampleRate: Float, outLR: FloatArray) {
        val envVal = env.nextSample()
        if (!env.isActive) {
            outLR[0] = 0f
            outLR[1] = 0f
            return
        }

        // Portamento glide
        val glideAlpha = if (params.glideTime > 0.02f) {
            (0.0004f + (1f - params.glideTime) * 0.02f)
        } else 1f
        currentFreq += (targetFreq - currentFreq) * glideAlpha

        val lfoHz = 0.2f + params.lfoRate * 12f
        val lfoVal = lfo.nextSample(lfoHz, sampleRate) * params.lfoDepth

        val pitchMod = if (params.lfoTarget == LfoTarget.PITCH) 2f.pow(lfoVal * 0.5f / 12f) else 1f
        val effectiveFreq = (currentFreq * pitchMod).coerceIn(22f, 12000f)

        val unisonCount = params.unisonVoices.coerceIn(1, 5)
        val detuneAmt = params.detune * 0.028f

        var sumL = 0f
        var sumR = 0f

        when (params.presetId) {
            SynthPresetId.BELL, SynthPresetId.EP_RHODES -> {
                val modRatio = if (params.presetId == SynthPresetId.BELL) 3.5f else 2.0f
                val modIndex = (if (params.presetId == SynthPresetId.BELL) 2.2f else 0.9f) *
                    (0.3f + params.tone * 0.9f) * envVal
                val modSig = modOsc.nextSample(effectiveFreq * modRatio, sampleRate, OscWaveform.SINE) * modIndex
                val carrierPhase = oscs[0].phase
                oscs[0].phase = (carrierPhase + effectiveFreq / sampleRate) % 1f
                val sig = sin(((carrierPhase * 2.0 * PI) + modSig).toFloat())
                sumL = sig
                sumR = sig
            }
            SynthPresetId.FLUTE_SYNTH -> {
                val main = oscs[0].nextSample(effectiveFreq, sampleRate, OscWaveform.TRIANGLE)
                val breath = subOsc.nextWhiteNoise() * 0.14f * params.brightness
                val sig = main * 0.85f + breath
                sumL = sig
                sumR = sig
            }
            SynthPresetId.DARK_CHOIR_PAD -> {
                // Multi-oscillator detuned saw choir + subtle formant emphasis
                for (u in 0 until unisonCount) {
                    val spread = if (unisonCount == 1) 0f else (u.toFloat() / (unisonCount - 1) - 0.5f) * 2f
                    val f = effectiveFreq * (1f + spread * detuneAmt)
                    val s = oscs[u].nextSample(f, sampleRate, OscWaveform.SAW)
                    val pan = 0.5f + spread * 0.45f
                    sumL += s * (1f - pan) * 1.4f
                    sumR += s * pan * 1.4f
                }
                sumL /= unisonCount
                sumR /= unisonCount
                val sub = subOsc.nextSample(effectiveFreq * 0.5f, sampleRate, OscWaveform.SINE) * 0.35f
                sumL += sub
                sumR += sub
            }
            else -> {
                for (u in 0 until unisonCount) {
                    val spread = if (unisonCount == 1) 0f else (u.toFloat() / (unisonCount - 1) - 0.5f) * 2f
                    val f = effectiveFreq * (1f + spread * detuneAmt)
                    val s = oscs[u].nextSample(f, sampleRate, params.waveform)
                    val pan = 0.5f + spread * 0.4f
                    sumL += s * (1f - pan) * 1.35f
                    sumR += s * pan * 1.35f
                }
                sumL /= unisonCount
                sumR /= unisonCount

                // Tone warmth sub oscillator
                if (params.tone > 0.25f) {
                    val sub = subOsc.nextSample(effectiveFreq * 0.5f, sampleRate, OscWaveform.SINE) * (params.tone * 0.32f)
                    sumL += sub
                    sumR += sub
                }
            }
        }

        // Dynamic filter cutoff with brightness + envelope + optional LFO
        val baseNormCutoff = ((params.cutoff * 0.65f) + (params.brightness * 0.35f)).coerceIn(0.05f, 0.99f)
        val envBoost = if (params.presetId == SynthPresetId.PLUCK || params.presetId == SynthPresetId.LOFI_PIANO) {
            envVal * 0.4f
        } else {
            envVal * 0.15f
        }
        val lfoCutMod = if (params.lfoTarget == LfoTarget.CUTOFF) lfoVal * 0.25f else 0f
        val normCut = (baseNormCutoff + envBoost + lfoCutMod).coerceIn(0.04f, 0.99f)
        val cutoffHz = 55f * (320f).pow(normCut)

        val monoIn = (sumL + sumR) * 0.5f
        val filtered = filter.process(monoIn, cutoffHz, params.resonance, sampleRate, params.filterType)
        val stereoWidth = (sumL - sumR) * 0.5f
        val outL = filtered + stereoWidth * 0.8f
        val outR = filtered - stereoWidth * 0.8f

        val ampMod = if (params.lfoTarget == LfoTarget.AMP) (1f - params.lfoDepth * 0.45f * (0.5f + 0.5f * lfoVal)) else 1f
        val gain = envVal * velocity * ampMod * 0.42f

        outLR[0] = tanh(outL * gain * 1.25f)
        outLR[1] = tanh(outR * gain * 1.25f)
    }
}
