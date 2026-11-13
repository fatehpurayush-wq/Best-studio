package com.beatstudio.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class DrumStep(
    val active: Boolean = false,
    val velocity: Float = 0.85f,
    val probability: Float = 1f,
    val rollCount: Int = 1 // 1 = normal, 2 = 32nd roll, 3 = triplet roll, 4 = 64th roll
)

@Serializable
data class Pattern(
    val id: String, // "A".."H"
    val name: String = "Pattern $id",
    val stepCount: Int = 16, // 16 or 32
    val isTripletGrid: Boolean = false,
    val drumRows: Map<Int, List<DrumStep>> = emptyMap(), // rowIndex -> list of steps
    val notes: List<Note> = emptyList()
)

@Serializable
data class ArrangeBlock(
    val id: String,
    val trackId: String,
    val patternId: String, // "A".."H"
    val startBar: Int,
    val lengthBars: Int = 2,
    val sectionLabel: String = "Beat", // "Intro", "Verse", "Drop", "Outro"
    val looped: Boolean = true
)

@Serializable
data class AutomationPoint(
    val barPosition: Float, // 0f..totalBars
    val value: Float // 0f..1f
)

@Serializable
enum class AutomationTarget(val labelHinglish: String) {
    VOLUME("Volume"),
    FILTER_CUTOFF("Filter Cutoff"),
    REVERB_MIX("Reverb"),
    DISTORTION_DRIVE("Distortion")
}

@Serializable
data class AutomationLane(
    val target: AutomationTarget,
    val points: List<AutomationPoint> = emptyList(),
    val enabled: Boolean = false
)
