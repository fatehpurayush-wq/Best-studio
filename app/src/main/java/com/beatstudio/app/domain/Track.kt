package com.beatstudio.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class DrumRowConfig(
    val rowIndex: Int,
    val soundType: DrumSoundType,
    val label: String = soundType.labelHinglish,
    val pitchOffset: Float = 0f, // -12..+12 semitones (great for tunable cowbell & 808)
    val gain: Float = 0.9f,
    val muted: Boolean = false
)

@Serializable
data class Track(
    val id: String,
    val name: String,
    val type: TrackType,
    val colorHex: Long,
    val volume: Float = 0.85f, // 0..1.2
    val pan: Float = 0f, // -1..+1
    val muted: Boolean = false,
    val solo: Boolean = false,
    val activePatternId: String = "A",
    val patterns: Map<String, Pattern> = mapOf("A" to Pattern(id = "A")),
    val drumRows: List<DrumRowConfig> = emptyList(),
    val synthParams: SynthParams = SynthParams(),
    val samplerParams: SamplerParams = SamplerParams(),
    val vocalFilePath: String? = null,
    val effects: EffectSettings = EffectSettings(),
    val automation: List<AutomationLane> = emptyList()
)
