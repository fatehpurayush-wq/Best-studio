package com.beatstudio.app.domain

import kotlinx.serialization.Serializable

@Serializable
enum class GenreId(val labelHinglish: String, val defaultBpm: Int, val tagColorHex: Long) {
    PHONK("Phonk", 145, 0xFFFF3D5A),
    DRIFT_PHONK("Drift Phonk", 156, 0xFFFF1744),
    LOFI("Lo-fi", 78, 0xFF7C4DFF),
    TRAP("Trap", 140, 0xFF00E5FF),
    BOOM_BAP("Boom Bap", 92, 0xFFFF9100),
    HOUSE("House", 124, 0xFF00E676),
    SYNTHWAVE("Synthwave", 108, 0xFFFF4081),
    REGGAETON("Reggaeton", 96, 0xFFFFEA00),
    AMBIENT("Ambient", 68, 0xFF40C4FF),
    EDM("EDM", 128, 0xFF18FFFF),
    AFRO_BEATS("Afro Beats", 104, 0xFFFFAB40),
    HINDI_LOFI("Hindi Lo-fi", 82, 0xFFE040FB)
}

@Serializable
enum class MusicalScale(val labelHinglish: String, val intervals: List<Int>) {
    MINOR("Minor", listOf(0, 2, 3, 5, 7, 8, 10)),
    MAJOR("Major", listOf(0, 2, 4, 5, 7, 9, 11)),
    HARMONIC_MINOR("Harmonic Minor", listOf(0, 2, 3, 5, 7, 8, 11)),
    PHRYGIAN("Phrygian", listOf(0, 1, 3, 5, 7, 8, 10)),
    DORIAN("Dorian", listOf(0, 2, 3, 5, 7, 9, 10)),
    PENTATONIC_MINOR("Pentatonic Minor", listOf(0, 3, 5, 7, 10)),
    PENTATONIC_MAJOR("Pentatonic Major", listOf(0, 2, 4, 7, 9)),
    LYDIAN("Lydian", listOf(0, 2, 4, 6, 7, 9, 11)),
    MIXOLYDIAN("Mixolydian", listOf(0, 2, 4, 5, 7, 9, 10)),
    BHAIRAV("Bhairav (Desi)", listOf(0, 1, 4, 5, 7, 8, 11)),
    CHROMATIC("All Notes (Free)", (0..11).toList())
}

@Serializable
data class Project(
    val id: String,
    val name: String,
    val genre: GenreId = GenreId.LOFI,
    val bpm: Int = 120,
    val swing: Float = 0.0f, // 0..0.6
    val rootNote: Int = 0, // 0=C, 1=C#, ... 11=B
    val scale: MusicalScale = MusicalScale.MINOR,
    val timeSignatureTop: Int = 4, // 3 or 4
    val timeSignatureBottom: Int = 4,
    val loopEnabled: Boolean = true,
    val loopStartBar: Int = 0,
    val loopEndBar: Int = 4,
    val metronomeEnabled: Boolean = false,
    val slowedReverbEnabled: Boolean = false,
    val phonkDriveEnabled: Boolean = false,
    val masterVolume: Float = 0.9f,
    val masterEffects: EffectSettings = EffectSettings(),
    val tracks: List<Track> = emptyList(),
    val arrangement: List<ArrangeBlock> = emptyList(),
    val isArrangeMode: Boolean = false,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
