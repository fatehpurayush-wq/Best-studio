package com.beatstudio.app.music

import com.beatstudio.app.domain.ArrangeBlock
import com.beatstudio.app.domain.Project
import com.beatstudio.app.domain.TrackType
import java.util.UUID

object ArrangeGenerator {

    fun generateSongArrangement(project: Project): Project {
        val blocks = mutableListOf<ArrangeBlock>()
        // Standard 16-bar song structure:
        // Bars 0..2: Intro (Pad + Melody)
        // Bars 2..6: Verse (Drums A + Bass A + Pad A)
        // Bars 6..12: Drop (All tracks Pattern B / A)
        // Bars 12..16: Outro (Pad + Melody fade)
        project.tracks.forEach { track ->
            val hasB = track.patterns.containsKey("B") && (
                (track.patterns["B"]?.notes?.isNotEmpty() == true) ||
                    (track.patterns["B"]?.drumRows?.values?.any { r -> r.any { it.active } } == true)
                )
            val mainPat = "A"
            val dropPat = if (hasB) "B" else "A"

            when (track.type) {
                TrackType.DRUMS -> {
                    blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, mainPat, 2, 4, "Verse"))
                    blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, dropPat, 6, 6, "Drop"))
                }
                TrackType.INSTRUMENT -> {
                    val isBass = track.id.contains("bass")
                    if (isBass) {
                        blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, mainPat, 2, 4, "Verse"))
                        blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, dropPat, 6, 6, "Drop"))
                    } else {
                        blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, mainPat, 0, 2, "Intro"))
                        blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, mainPat, 2, 4, "Verse"))
                        blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, dropPat, 6, 6, "Drop"))
                        blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, mainPat, 12, 4, "Outro"))
                    }
                }
                TrackType.SAMPLER, TrackType.VOCAL -> {
                    val hasNotes = track.patterns["A"]?.notes?.isNotEmpty() == true || track.vocalFilePath != null || track.samplerParams.sampleFilePath != null
                    if (hasNotes) {
                        blocks.add(ArrangeBlock(UUID.randomUUID().toString(), track.id, mainPat, 2, 10, "Verse/Drop"))
                    }
                }
            }
        }
        return project.copy(
            arrangement = blocks,
            loopStartBar = 0,
            loopEndBar = 16,
            updatedAt = System.currentTimeMillis()
        )
    }
}
