package com.beatstudio.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatstudio.app.domain.ArrangeBlock
import com.beatstudio.app.domain.Project
import com.beatstudio.app.domain.Track
import com.beatstudio.app.ui.components.AppButton
import com.beatstudio.app.ui.components.AppChipButton
import com.beatstudio.app.ui.theme.*

@Composable
fun ArrangeView(
    project: Project,
    playheadBar: Int,
    isPlaying: Boolean,
    onBackToPattern: () -> Unit,
    onGenerateSong: () -> Unit,
    onSetLoopRange: (Int, Int) -> Unit,
    onAddBlock: (trackId: String, patternId: String, startBar: Int, lengthBars: Int, label: String) -> Unit,
    onDuplicateBlock: (ArrangeBlock) -> Unit,
    onSplitBlock: (ArrangeBlock) -> Unit,
    onDeleteBlock: (String) -> Unit
) {
    val barWidthDp = 42.dp
    val totalBars = 16
    val timelineScroll = rememberScrollState()
    var selectedBlock by remember { mutableStateOf<ArrangeBlock?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppChipButton(
                text = "Wapas Pattern Grid",
                selected = false,
                icon = Icons.Default.GridView,
                onClick = onBackToPattern
            )

            AppButton(
                text = "Song banao",
                icon = Icons.Default.AutoAwesome,
                containerColor = StudioAmber,
                contentColor = StudioObsidian,
                onClick = onGenerateSong,
                tag = "arrange_song_banao_btn"
            )
        }

        // Loop Range Bar
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Loop Range:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppChipButton("Bars 1-4", project.loopEndBar == 4, { onSetLoopRange(0, 4) })
            AppChipButton("Bars 1-8", project.loopEndBar == 8, { onSetLoopRange(0, 8) })
            AppChipButton("Full 16 Bars", project.loopEndBar == 16, { onSetLoopRange(0, 16) })
        }

        // Timeline Header Bars
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(92.dp))
            Row(modifier = Modifier.weight(1f).horizontalScroll(timelineScroll)) {
                for (b in 0 until totalBars) {
                    val isCur = isPlaying && playheadBar == b
                    val label = when (b) {
                        0 -> "Intro"
                        2 -> "Verse"
                        6 -> "Drop"
                        12 -> "Outro"
                        else -> "${b + 1}"
                    }
                    Surface(
                        color = if (isCur) NeonCyan else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.width(barWidthDp).height(26.dp).padding(end = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCur) StudioObsidian else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Track Lanes
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(project.tracks, key = { it.id }) { track ->
                val c = Color(track.colorHex)
                val blocks = project.arrangement.filter { it.trackId == track.id }

                Row(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = c.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, c.copy(alpha = 0.7f)),
                        modifier = Modifier.width(88.dp).fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = c,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(StudioObsidian, RoundedCornerShape(10.dp))
                            .horizontalScroll(timelineScroll)
                    ) {
                        Row(modifier = Modifier.width(barWidthDp * totalBars).fillMaxHeight()) {
                            for (b in 0 until totalBars step 2) {
                                Surface(
                                    onClick = { onAddBlock(track.id, track.activePatternId, b, 2, "Beat") },
                                    color = Color.Transparent,
                                    border = BorderStroke(0.5.dp, StudioOutline.copy(alpha = 0.4f)),
                                    modifier = Modifier.width(barWidthDp * 2).fillMaxHeight()
                                ) {}
                            }
                        }

                        blocks.forEach { block ->
                            Surface(
                                onClick = { selectedBlock = block },
                                shape = RoundedCornerShape(10.dp),
                                color = c.copy(alpha = 0.35f),
                                border = BorderStroke(1.5.dp, c),
                                modifier = Modifier
                                    .offset(x = barWidthDp * block.startBar)
                                    .width(barWidthDp * block.lengthBars)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${block.sectionLabel} • ${block.patternId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedBlock?.let { blk ->
        AlertDialog(
            onDismissRequest = { selectedBlock = null },
            title = { Text("${blk.sectionLabel} Block (Pattern ${blk.patternId})") },
            text = { Text("Bar ${blk.startBar + 1} se ${blk.startBar + blk.lengthBars} tak") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onDuplicateBlock(blk); selectedBlock = null }) { Text("Copy") }
                    if (blk.lengthBars > 1) {
                        TextButton(onClick = { onSplitBlock(blk); selectedBlock = null }) { Text("Split") }
                    }
                    TextButton(onClick = { onDeleteBlock(blk.id); selectedBlock = null }) {
                        Text("Delete", color = PhonkCoral)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBlock = null }) { Text("Band karo") }
            }
        )
    }
}
