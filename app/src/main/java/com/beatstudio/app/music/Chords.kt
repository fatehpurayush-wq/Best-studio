package com.beatstudio.app.music

import com.beatstudio.app.domain.GenreId
import com.beatstudio.app.domain.MusicalScale
import com.beatstudio.app.domain.Note
import java.util.UUID

data class ChordPreset(
    val id: String,
    val titleHinglish: String,
    val subtitle: String,
    val degrees: List<Int>, // scale degrees 0-indexed
    val seventh: Boolean = false
)

object Chords {
    fun presetsForGenre(genre: GenreId): List<ChordPreset> {
        return when (genre) {
            GenreId.PHONK, GenreId.DRIFT_PHONK -> listOf(
                ChordPreset("phonk_dark", "Dark Memphis", "i - bVI - bVII - i", listOf(0, 5, 6, 0)),
                ChordPreset("phonk_drift", "Drift Tension", "i - iv - bVI - V", listOf(0, 3, 5, 4)),
                ChordPreset("phonk_minimal", "2-Chord Bounce", "i - bII - i - bVII", listOf(0, 1, 0, 6))
            )
            GenreId.LOFI, GenreId.HINDI_LOFI -> listOf(
                ChordPreset("lofi_chill", "Dusty Jazz 7th", "ii7 - V7 - I7 - vi7", listOf(1, 4, 0, 5), seventh = true),
                ChordPreset("lofi_rain", "Barish Mood", "i7 - bVI7 - bIII7 - bVII7", listOf(0, 5, 2, 6), seventh = true),
                ChordPreset("lofi_desi", "Desi Lo-fi", "i - bII - iv - i", listOf(0, 1, 3, 0), seventh = true)
            )
            GenreId.TRAP -> listOf(
                ChordPreset("trap_anthem", "Trap Anthem", "i - bVI - bIII - bVII", listOf(0, 5, 2, 6)),
                ChordPreset("trap_dark", "Dark Bell Loop", "i - bVI - V - i", listOf(0, 5, 4, 0))
            )
            GenreId.BOOM_BAP -> listOf(
                ChordPreset("boombap_soul", "90s Soul", "i7 - iv7 - bVI7 - V7", listOf(0, 3, 5, 4), seventh = true),
                ChordPreset("boombap_jazzy", "Jazzy Keys", "ii7 - V7 - I7 - vi7", listOf(1, 4, 0, 5), seventh = true)
            )
            GenreId.HOUSE, GenreId.EDM -> listOf(
                ChordPreset("house_uplift", "Club Uplift", "vi - IV - I - V", listOf(5, 3, 0, 4)),
                ChordPreset("house_deep", "Deep House 7th", "i7 - iv7 - bVII7 - bIII7", listOf(0, 3, 6, 2), seventh = true)
            )
            GenreId.SYNTHWAVE -> listOf(
                ChordPreset("synth_80s", "80s Neon", "i - bVI - bIII - bVII", listOf(0, 5, 2, 6)),
                ChordPreset("synth_drive", "Night Drive", "IV - V - vi - I", listOf(3, 4, 5, 0))
            )
            GenreId.REGGAETON -> listOf(
                ChordPreset("reg_perreo", "Perreo Minor", "i - bVII - bVI - V", listOf(0, 6, 5, 4)),
                ChordPreset("reg_summer", "Latin Vibe", "vi - IV - I - V", listOf(5, 3, 0, 4))
            )
            GenreId.AFRO_BEATS -> listOf(
                ChordPreset("afro_groove", "Afro Warmth", "I7 - V - vi7 - IV", listOf(0, 4, 5, 3), seventh = true),
                ChordPreset("afro_sunset", "Lagos Sunset", "iv7 - bVII7 - i7 - bVI7", listOf(3, 6, 0, 5), seventh = true)
            )
            GenreId.AMBIENT -> listOf(
                ChordPreset("amb_space", "Space Float", "I7 - iii7 - vi7 - IV7", listOf(0, 2, 5, 3), seventh = true),
                ChordPreset("amb_drift", "Dream Pad", "i7 - bVI7 - iv7 - i7", listOf(0, 5, 3, 0), seventh = true)
            )
        }
    }

    fun buildChordProgressionNotes(
        preset: ChordPreset,
        rootNote: Int,
        scale: MusicalScale,
        stepCount: Int,
        baseOctaveMidi: Int = 60,
        velocity: Float = 0.78f
    ): List<Note> {
        val result = mutableListOf<Note>()
        val chordsCount = preset.degrees.size.coerceAtLeast(1)
        val stepsPerChord = (stepCount.toFloat() / chordsCount).coerceAtLeast(2f)
        preset.degrees.forEachIndexed { idx, deg ->
            val start = idx * stepsPerChord
            val duration = (stepsPerChord - 0.15f).coerceAtLeast(1f)
            val offsets = if (preset.seventh) listOf(0, 2, 4, 6) else listOf(0, 2, 4)
            offsets.forEach { stepDeg ->
                val midi = Scales.scaleDegreeMidi(rootNote, scale, deg + stepDeg, baseOctaveMidi)
                result.add(
                    Note(
                        id = UUID.randomUUID().toString(),
                        pitch = midi,
                        startStep = start,
                        durationSteps = duration,
                        velocity = velocity
                    )
                )
            }
        }
        return result
    }
}
