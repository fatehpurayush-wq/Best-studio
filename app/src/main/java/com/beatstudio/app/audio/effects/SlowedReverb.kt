package com.beatstudio.app.audio.effects

import com.beatstudio.app.domain.EffectSettings
import com.beatstudio.app.domain.Project
import com.beatstudio.app.domain.ReverbType
import kotlin.math.roundToInt

object SlowedReverb {
    const val SLOWED_TEMPO_FACTOR = 0.82f
    const val SLOWED_PITCH_SEMITONES = -2.5f

    fun effectiveBpm(project: Project): Float {
        return if (project.slowedReverbEnabled) {
            (project.bpm * SLOWED_TEMPO_FACTOR).coerceIn(32f, 220f)
        } else {
            project.bpm.toFloat()
        }
    }

    fun effectiveSlowedPitchOffset(project: Project): Float {
        return if (project.slowedReverbEnabled) SLOWED_PITCH_SEMITONES else 0f
    }

    fun applyPhonkDrivePreset(current: EffectSettings, enabled: Boolean): EffectSettings {
        return if (enabled) {
            current.copy(
                distortionEnabled = true,
                distortionDrive = 0.45f,
                distortionTone = 0.62f,
                distortionMix = 0.36f,
                compressorEnabled = true,
                compThreshold = 0.62f,
                compRatio = 0.55f,
                eqEnabled = true,
                eqLow = 0.64f,
                eqMid = 0.48f,
                eqHigh = 0.58f
            )
        } else {
            current.copy(
                distortionEnabled = false,
                eqLow = 0.5f,
                eqMid = 0.5f,
                eqHigh = 0.5f
            )
        }
    }

    fun applySlowedReverbToMaster(current: EffectSettings, enabled: Boolean): EffectSettings {
        return if (enabled) {
            current.copy(
                reverbEnabled = true,
                reverbType = ReverbType.HUGE,
                reverbMix = 0.52f,
                reverbSize = 0.90f,
                reverbDamp = 0.58f,
                pitchShiftEnabled = true,
                pitchSemitones = SLOWED_PITCH_SEMITONES,
                pitchMix = 1.0f
            )
        } else {
            current.copy(
                reverbEnabled = false,
                pitchShiftEnabled = false,
                pitchSemitones = 0f
            )
        }
    }

    fun displayEffectiveBpm(project: Project): Int = effectiveBpm(project).roundToInt()
}
