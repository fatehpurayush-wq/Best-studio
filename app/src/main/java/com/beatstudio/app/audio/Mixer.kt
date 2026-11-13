package com.beatstudio.app.audio

import com.beatstudio.app.audio.effects.*
import com.beatstudio.app.domain.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class TrackEffectRack(sampleRate: Int = 44100) {
    val reverb = StereoReverb(sampleRate)
    val delay = StereoDelay(sampleRate)
    val distortion = DistortionEffect(sampleRate.toFloat())
    val bitcrusher = BitcrusherEffect()
    val tape = TapeLofiEffect(sampleRate.toFloat())
    val vinyl = VinylEffect(sampleRate.toFloat())
    val filterL = StateVariableFilter()
    val filterR = StateVariableFilter()
    val eq = MultiBandEq(sampleRate.toFloat())
    val compressor = CompressorEffect(sampleRate.toFloat())
    val sidechain = SidechainDucker(sampleRate.toFloat())
    val chorus = ChorusEffect(sampleRate.toFloat())
    val phaser = PhaserEffect(sampleRate.toFloat())
    val flanger = FlangerEffect(sampleRate.toFloat())
    val pitchShifter = PitchShifterEffect()
    val stutter = StutterGateEffect()

    @Volatile var peakLevel: Float = 0f

    fun reset() {
        reverb.reset()
        delay.reset()
        distortion.reset()
        bitcrusher.reset()
        tape.reset()
        vinyl.reset()
        filterL.reset()
        filterR.reset()
        eq.reset()
        compressor.reset()
        sidechain.reset()
        chorus.reset()
        phaser.reset()
        flanger.reset()
        pitchShifter.reset()
        stutter.reset()
        peakLevel = 0f
    }

    fun processTrackEffects(
        bufL: FloatArray,
        bufR: FloatArray,
        numFrames: Int,
        fx: EffectSettings,
        isProMode: Boolean,
        subStepFraction: Float,
        filterCutoffAutoOverride: Float? = null,
        reverbMixAutoOverride: Float? = null,
        distDriveAutoOverride: Float? = null
    ) {
        if (fx.pitchShiftEnabled) {
            pitchShifter.processBlock(bufL, bufR, numFrames, fx.pitchSemitones, fx.pitchMix)
        }
        if (fx.distortionEnabled || distDriveAutoOverride != null) {
            val drive = distDriveAutoOverride ?: fx.distortionDrive
            distortion.processBlock(bufL, bufR, numFrames, drive, fx.distortionTone, fx.distortionMix, fx.isOverdrive)
        }
        if (fx.bitcrusherEnabled) {
            bitcrusher.processBlock(bufL, bufR, numFrames, fx.bitDepth, fx.downsampleFactor, fx.bitcrusherMix)
        }
        if (fx.filterEnabled || filterCutoffAutoOverride != null) {
            val normCut = filterCutoffAutoOverride ?: fx.filterCutoff
            val cutoffHz = 45f * (380f).pow(normCut.coerceIn(0f, 1f))
            for (i in 0 until numFrames) {
                bufL[i] = filterL.process(bufL[i], cutoffHz, fx.filterResonance, 44100f, fx.filterMode)
                bufR[i] = filterR.process(bufR[i], cutoffHz, fx.filterResonance, 44100f, fx.filterMode)
            }
        }
        if (fx.eqEnabled) {
            if (isProMode) {
                eq.processPro8Band(bufL, bufR, numFrames, fx.eqEightBands)
            } else {
                eq.processSimple3Band(bufL, bufR, numFrames, fx.eqLow, fx.eqMid, fx.eqHigh)
            }
        }
        if (fx.tapeEnabled) {
            tape.processBlock(bufL, bufR, numFrames, fx.tapeWobble, fx.tapeHiss, fx.tapeWarmth)
        }
        if (fx.vinylEnabled) {
            vinyl.processBlock(bufL, bufR, numFrames, fx.vinylLevel, fx.vinylDensity)
        }
        if (fx.chorusEnabled) {
            chorus.processBlock(bufL, bufR, numFrames, fx.chorusRate, fx.chorusDepth, fx.chorusMix)
        }
        if (fx.phaserEnabled) {
            phaser.processBlock(bufL, bufR, numFrames, fx.phaserRate, fx.phaserDepth, fx.phaserMix)
        }
        if (fx.flangerEnabled) {
            flanger.processBlock(bufL, bufR, numFrames, fx.flangerRate, fx.flangerFeedback, fx.flangerMix)
        }
        if (fx.compressorEnabled) {
            compressor.processBlock(bufL, bufR, numFrames, fx.compThreshold, fx.compRatio, fx.compMakeup)
        }
        if (fx.sidechainEnabled) {
            sidechain.processBlock(bufL, bufR, numFrames, fx.sidechainAmount, fx.sidechainRelease)
        }
        if (fx.stutterEnabled) {
            stutter.processBlock(bufL, bufR, numFrames, subStepFraction, fx.stutterRateSteps, fx.stutterDepth)
        }
        if (fx.delayEnabled) {
            delay.processBlock(bufL, bufR, numFrames, fx.delayTimeMs, fx.delayFeedback, fx.delayMix, fx.delayPingPong)
        }
        if (fx.reverbEnabled || reverbMixAutoOverride != null) {
            val rMix = reverbMixAutoOverride ?: fx.reverbMix
            reverb.processBlock(bufL, bufR, numFrames, fx.reverbType, rMix, fx.reverbSize, fx.reverbDamp)
        }
        StereoWidener.processBlock(bufL, bufR, numFrames, fx.stereoWidth)
    }
}

class Mixer(private val sampleRate: Int = 44100, private val maxBlockSize: Int = 512) {
    private val trackRacks = HashMap<String, TrackEffectRack>()
    private val masterRack = TrackEffectRack(sampleRate)
    val limiter = MasterLimiter(sampleRate.toFloat())

    private val scratchL = FloatArray(maxBlockSize)
    private val scratchR = FloatArray(maxBlockSize)

    fun reset() {
        trackRacks.values.forEach { it.reset() }
        masterRack.reset()
        limiter.reset()
    }

    fun triggerSidechainKick() {
        trackRacks.values.forEach { it.sidechain.triggerKickPulse() }
        masterRack.sidechain.triggerKickPulse()
    }

    fun getTrackPeak(trackId: String): Float = trackRacks[trackId]?.peakLevel ?: 0f

    fun mixProjectBlock(
        project: Project,
        voiceManager: VoiceManager,
        metronomeL: FloatArray,
        metronomeR: FloatArray,
        outL: FloatArray,
        outR: FloatArray,
        numFrames: Int,
        isProMode: Boolean,
        currentBarFloat: Float,
        subStepFraction: Float,
        soloTrackIdOverride: String? = null
    ) {
        for (i in 0 until numFrames) {
            outL[i] = metronomeL[i]
            outR[i] = metronomeR[i]
        }

        val anyTrackSolo = project.tracks.any { it.solo }

        for (tIdx in project.tracks.indices) {
            val track = project.tracks[tIdx]
            val audible = when {
                soloTrackIdOverride != null -> track.id == soloTrackIdOverride
                anyTrackSolo -> track.solo && !track.muted
                else -> !track.muted
            }

            scratchL.fill(0f, 0, numFrames)
            scratchR.fill(0f, 0, numFrames)

            // Always advance voices for tracks so envelopes finish cleanly, even if muted
            voiceManager.renderTrackSample(track.id, scratchL, scratchR, numFrames)

            if (!audible) {
                trackRacks[track.id]?.peakLevel = 0f
                continue
            }

            val rack = trackRacks.getOrPut(track.id) { TrackEffectRack(sampleRate) }

            val volAuto = sampleAutomation(track, AutomationTarget.VOLUME, currentBarFloat)
            val cutAuto = sampleAutomation(track, AutomationTarget.FILTER_CUTOFF, currentBarFloat)
            val revAuto = sampleAutomation(track, AutomationTarget.REVERB_MIX, currentBarFloat)
            val distAuto = sampleAutomation(track, AutomationTarget.DISTORTION_DRIVE, currentBarFloat)

            rack.processTrackEffects(
                bufL = scratchL,
                bufR = scratchR,
                numFrames = numFrames,
                fx = track.effects,
                isProMode = isProMode,
                subStepFraction = subStepFraction,
                filterCutoffAutoOverride = cutAuto,
                reverbMixAutoOverride = revAuto,
                distDriveAutoOverride = distAuto
            )

            // Constant-power stereo pan + track volume
            val panNorm = ((track.pan.coerceIn(-1f, 1f) + 1f) * 0.5f) * (PI.toFloat() * 0.5f)
            val gainL = cos(panNorm) * (volAuto ?: track.volume)
            val gainR = sin(panNorm) * (volAuto ?: track.volume)

            var trackPeak = 0f
            for (i in 0 until numFrames) {
                val l = scratchL[i] * gainL
                val r = scratchR[i] * gainR
                outL[i] += l
                outR[i] += r
                val p = max(abs(l), abs(r))
                if (p > trackPeak) trackPeak = p
            }
            rack.peakLevel = trackPeak
        }

        // Master FX Rack (including Phonk Drive & Slowed+Reverb)
        val masterFx = if (project.slowedReverbEnabled) {
            SlowedReverb.applySlowedReverbToMaster(project.masterEffects, true)
        } else {
            project.masterEffects
        }
        masterRack.processTrackEffects(
            bufL = outL,
            bufR = outR,
            numFrames = numFrames,
            fx = masterFx,
            isProMode = isProMode,
            subStepFraction = subStepFraction
        )

        // Master Volume + Brickwall Limiter + Output Metering
        limiter.processBlock(outL, outR, numFrames, project.masterVolume)
    }

    private fun sampleAutomation(track: Track, target: AutomationTarget, barPos: Float): Float? {
        val lane = track.automation.firstOrNull { it.enabled && it.target == target && it.points.isNotEmpty() }
            ?: return null
        val pts = lane.points.sortedBy { it.barPosition }
        if (barPos <= pts.first().barPosition) return pts.first().value
        if (barPos >= pts.last().barPosition) return pts.last().value
        for (i in 0 until pts.lastIndex) {
            val a = pts[i]
            val b = pts[i + 1]
            if (barPos >= a.barPosition && barPos <= b.barPosition) {
                val span = (b.barPosition - a.barPosition).coerceAtLeast(0.001f)
                val t = (barPos - a.barPosition) / span
                return a.value + (b.value - a.value) * t
            }
        }
        return null
    }
}
