package com.beatstudio.app.music

import com.beatstudio.app.domain.*
import java.util.UUID
import kotlin.random.Random

object AutoBeatGenerator {

    fun generateFullBeat(
        project: Project,
        variation: Float = 0.35f,
        seed: Int = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
    ): Project {
        val rng = Random(seed)
        val genre = project.genre
        val root = project.rootNote
        val scale = project.scale
        val chordPresets = Chords.presetsForGenre(genre)
        val chosenChord = chordPresets[rng.nextInt(chordPresets.size)]

        val updatedTracks = project.tracks.map { track ->
            val patId = track.activePatternId
            val currentPat = track.patterns[patId] ?: Pattern(id = patId)
            val stepCount = currentPat.stepCount

            val newPattern = when (track.type) {
                TrackType.DRUMS -> generateDrumPattern(
                    pattern = currentPat,
                    rows = track.drumRows,
                    genre = genre,
                    variation = variation,
                    rng = rng
                )
                TrackType.INSTRUMENT -> {
                    val isBass = track.id.contains("bass") ||
                        track.synthParams.presetId in listOf(SynthPresetId.SUB_BASS, SynthPresetId.REESE_BASS, SynthPresetId.BASS_808)
                    val isPad = track.id.contains("pad") ||
                        track.synthParams.presetId in listOf(SynthPresetId.WARM_PAD, SynthPresetId.STRING_PAD, SynthPresetId.DARK_CHOIR_PAD)

                    val notes = when {
                        isBass -> generateBassNotes(chosenChord, root, scale, stepCount, genre, variation, rng)
                        isPad -> Chords.buildChordProgressionNotes(chosenChord, root, scale, stepCount, baseOctaveMidi = 54, velocity = 0.68f)
                        else -> generateMelodyNotes(chosenChord, root, scale, stepCount, genre, variation, rng)
                    }
                    currentPat.copy(notes = notes)
                }
                else -> currentPat
            }

            // Also populate Pattern B with an energetic variation if not already edited
            val patB = if (patId == "A") {
                val varRng = Random(seed + 99)
                when (track.type) {
                    TrackType.DRUMS -> generateDrumPattern(
                        pattern = Pattern(id = "B", name = "Pattern B", stepCount = stepCount),
                        rows = track.drumRows,
                        genre = genre,
                        variation = (variation + 0.3f).coerceAtMost(1f),
                        rng = varRng
                    )
                    TrackType.INSTRUMENT -> {
                        val isBass = track.id.contains("bass")
                        val isPad = track.id.contains("pad")
                        val notesB = when {
                            isBass -> generateBassNotes(chosenChord, root, scale, stepCount, genre, (variation + 0.3f).coerceAtMost(1f), varRng)
                            isPad -> Chords.buildChordProgressionNotes(chosenChord, root, scale, stepCount, baseOctaveMidi = 60, velocity = 0.72f)
                            else -> generateMelodyNotes(chosenChord, root, scale, stepCount, genre, (variation + 0.25f).coerceAtMost(1f), varRng)
                        }
                        Pattern(id = "B", name = "Pattern B", stepCount = stepCount, notes = notesB)
                    }
                    else -> track.patterns["B"] ?: Pattern(id = "B", name = "Pattern B", stepCount = stepCount)
                }
            } else {
                track.patterns["B"]
            }

            val newPatterns = track.patterns.toMutableMap().apply {
                put(patId, newPattern)
                if (patB != null && patId == "A") put("B", patB)
            }
            track.copy(patterns = newPatterns)
        }

        return project.copy(tracks = updatedTracks, updatedAt = System.currentTimeMillis())
    }

    private fun generateDrumPattern(
        pattern: Pattern,
        rows: List<DrumRowConfig>,
        genre: GenreId,
        variation: Float,
        rng: Random
    ): Pattern {
        val steps = pattern.stepCount
        val drumRowsMap = mutableMapOf<Int, List<DrumStep>>()

        rows.forEach { row ->
            val list = MutableList(steps) { DrumStep() }
            for (s in 0 until steps) {
                val m = s % 16
                val activeAndRoll = when (row.soundType) {
                    DrumSoundType.KICK_PUNCHY, DrumSoundType.KICK_SUB, DrumSoundType.KICK_808 -> when (genre) {
                        GenreId.HOUSE, GenreId.EDM -> m in listOf(0, 4, 8, 12)
                        GenreId.REGGAETON -> m in listOf(0, 4, 8, 12)
                        GenreId.TRAP -> m in listOf(0, 3, 10) || (variation > 0.45f && m == 14 && rng.nextFloat() < 0.6f)
                        GenreId.PHONK, GenreId.DRIFT_PHONK -> m in listOf(0, 6, 8, 14) || (variation > 0.5f && m == 3 && rng.nextFloat() < 0.5f)
                        GenreId.AFRO_BEATS -> m in listOf(0, 6, 10, 14)
                        else -> m in listOf(0, 7, 10) || (variation > 0.4f && m == 15 && rng.nextFloat() < 0.4f)
                    }
                    DrumSoundType.BASS_808 -> when (genre) {
                        GenreId.PHONK, GenreId.DRIFT_PHONK -> m in listOf(0, 6, 10, 14)
                        GenreId.TRAP -> m in listOf(0, 3, 10)
                        else -> m in listOf(0, 8)
                    }
                    DrumSoundType.SNARE, DrumSoundType.CLAP -> when (genre) {
                        GenreId.PHONK, GenreId.DRIFT_PHONK, GenreId.TRAP -> m in listOf(8) || (row.soundType == DrumSoundType.CLAP && m == 8)
                        GenreId.REGGAETON -> m in listOf(3, 6, 11, 14)
                        GenreId.HOUSE, GenreId.EDM -> row.soundType == DrumSoundType.CLAP && m in listOf(4, 12)
                        else -> m in listOf(4, 12)
                    }
                    DrumSoundType.HIHAT_CLOSED -> when (genre) {
                        GenreId.TRAP, GenreId.PHONK, GenreId.DRIFT_PHONK -> m % 2 == 0 || (variation > 0.2f && m in listOf(7, 13, 15))
                        GenreId.HOUSE, GenreId.EDM -> m % 2 == 0
                        else -> m % 2 == 0 || (variation > 0.5f && rng.nextFloat() < 0.25f)
                    }
                    DrumSoundType.HIHAT_OPEN -> when (genre) {
                        GenreId.HOUSE, GenreId.EDM -> m in listOf(2, 6, 10, 14)
                        else -> m == 14 && rng.nextFloat() < (0.5f + variation * 0.4f)
                    }
                    DrumSoundType.PHONK_COWBELL -> m in listOf(0, 2, 4, 6, 9, 12, 14) && (rng.nextFloat() < (0.7f + variation * 0.3f))
                    DrumSoundType.MEMPHIS_HIT -> m in listOf(0, 12) && rng.nextFloat() < 0.7f
                    DrumSoundType.RIM -> when (genre) {
                        GenreId.AFRO_BEATS -> m in listOf(3, 6, 10, 14)
                        else -> m in listOf(6, 15) && rng.nextFloat() < (0.4f + variation * 0.5f)
                    }
                    DrumSoundType.SHAKER -> m % 2 == 1
                    DrumSoundType.TAMBOURINE -> m in listOf(4, 12)
                    DrumSoundType.VINYL_CRACKLE -> m == 0
                    DrumSoundType.CRASH -> s == 0
                    DrumSoundType.TOM -> m in listOf(12, 14, 15) && rng.nextFloat() < variation * 0.7f
                    else -> false
                }

                val isRoll = (row.soundType == DrumSoundType.HIHAT_CLOSED) &&
                    (genre == GenreId.TRAP || genre == GenreId.PHONK || genre == GenreId.DRIFT_PHONK) &&
                    (m == 7 || m == 15) && rng.nextFloat() < (0.35f + variation * 0.55f)

                val vel = when {
                    row.soundType == DrumSoundType.HIHAT_CLOSED && m % 4 != 0 -> 0.58f + rng.nextFloat() * 0.18f
                    activeAndRoll -> 0.82f + rng.nextFloat() * 0.16f
                    else -> 0.8f
                }

                list[s] = DrumStep(
                    active = activeAndRoll,
                    velocity = vel.coerceIn(0.25f, 1f),
                    probability = if (variation > 0.6f && m % 4 == 3 && activeAndRoll) 0.8f else 1f,
                    rollCount = if (isRoll) (if (rng.nextBoolean()) 3 else 2) else 1
                )
            }
            drumRowsMap[row.rowIndex] = list
        }

        return pattern.copy(drumRows = drumRowsMap)
    }

    private fun generateBassNotes(
        chord: ChordPreset,
        rootNote: Int,
        scale: MusicalScale,
        stepCount: Int,
        genre: GenreId,
        variation: Float,
        rng: Random
    ): List<Note> {
        val notes = mutableListOf<Note>()
        val degrees = chord.degrees
        val stepsPerChord = (stepCount / degrees.size.coerceAtLeast(1)).coerceAtLeast(2)

        degrees.forEachIndexed { idx, deg ->
            val chordStart = idx * stepsPerChord
            val bassMidi = Scales.scaleDegreeMidi(rootNote, scale, deg, baseOctaveMidi = 36)
            when (genre) {
                GenreId.HOUSE, GenreId.EDM, GenreId.SYNTHWAVE -> {
                    for (s in 0 until stepsPerChord step 2) {
                        val octaveJump = if (s % 4 == 2 && variation > 0.35f) 12 else 0
                        notes.add(
                            Note(
                                id = UUID.randomUUID().toString(),
                                pitch = (bassMidi + octaveJump).coerceIn(28, 60),
                                startStep = (chordStart + s).toFloat(),
                                durationSteps = 1.5f,
                                velocity = 0.88f
                            )
                        )
                    }
                }
                GenreId.PHONK, GenreId.DRIFT_PHONK, GenreId.TRAP -> {
                    notes.add(
                        Note(
                            id = UUID.randomUUID().toString(),
                            pitch = bassMidi,
                            startStep = chordStart.toFloat(),
                            durationSteps = 2.5f,
                            velocity = 0.95f
                        )
                    )
                    if (stepsPerChord >= 4) {
                        val glideNote = if (rng.nextFloat() < variation + 0.3f) bassMidi + 12 else bassMidi
                        notes.add(
                            Note(
                                id = UUID.randomUUID().toString(),
                                pitch = Scales.snapToScale(glideNote, rootNote, scale),
                                startStep = (chordStart + stepsPerChord - 1).toFloat(),
                                durationSteps = 1f,
                                velocity = 0.85f
                            )
                        )
                    }
                }
                else -> {
                    notes.add(
                        Note(
                            id = UUID.randomUUID().toString(),
                            pitch = bassMidi,
                            startStep = chordStart.toFloat(),
                            durationSteps = (stepsPerChord - 0.5f).coerceAtLeast(1f),
                            velocity = 0.86f
                        )
                    )
                }
            }
        }
        return notes
    }

    private fun generateMelodyNotes(
        chord: ChordPreset,
        rootNote: Int,
        scale: MusicalScale,
        stepCount: Int,
        genre: GenreId,
        variation: Float,
        rng: Random
    ): List<Note> {
        val notes = mutableListOf<Note>()
        val scaleNotes = Scales.getScaleMidiNotes(rootNote, scale, lowMidi = 60, highMidi = 84)
        if (scaleNotes.isEmpty()) return emptyList()

        val density = when (genre) {
            GenreId.AMBIENT -> 0.25f
            GenreId.LOFI, GenreId.HINDI_LOFI -> 0.45f
            GenreId.SYNTHWAVE, GenreId.EDM -> 0.72f
            else -> 0.58f
        } + variation * 0.2f

        var prevIdx = rng.nextInt(scaleNotes.size / 2)
        var s = 0
        while (s < stepCount) {
            if (rng.nextFloat() < density) {
                val jump = rng.nextInt(-2, 3)
                prevIdx = (prevIdx + jump).coerceIn(0, scaleNotes.lastIndex)
                val dur = when (genre) {
                    GenreId.AMBIENT -> 4f
                    GenreId.LOFI, GenreId.HINDI_LOFI -> if (rng.nextBoolean()) 2f else 1.5f
                    else -> if (rng.nextFloat() < 0.7f) 1f else 2f
                }
                notes.add(
                    Note(
                        id = UUID.randomUUID().toString(),
                        pitch = scaleNotes[prevIdx],
                        startStep = s.toFloat(),
                        durationSteps = dur.coerceAtMost((stepCount - s).toFloat()),
                        velocity = 0.72f + rng.nextFloat() * 0.22f
                    )
                )
                s += dur.toInt().coerceAtLeast(1)
            } else {
                s += if (genre == GenreId.SYNTHWAVE) 1 else 2
            }
        }
        return notes
    }
}
