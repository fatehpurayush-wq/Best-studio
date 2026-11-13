package com.beatstudio.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val pitch: Int, // MIDI note 0..127 (for drums, row index or MIDI pitch; for melody, MIDI pitch e.g. 60 = C4)
    val startStep: Float, // 0f..(totalSteps) in 16th-note units (supports fractional triplets/drag)
    val durationSteps: Float = 1f,
    val velocity: Float = 0.85f, // 0f..1f
    val probability: Float = 1f, // 0f..1f
    val isRoll: Boolean = false,
    val rollSubdivision: Int = 2 // 2, 3, or 4 sub-hits inside step
)
