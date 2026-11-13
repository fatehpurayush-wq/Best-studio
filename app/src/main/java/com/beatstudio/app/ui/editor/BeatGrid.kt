package com.beatstudio.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatstudio.app.domain.*
import com.beatstudio.app.ui.components.AppChipButton
import com.beatstudio.app.ui.components.SliderRow
import com.beatstudio.app.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BeatGrid(
    project: Project,
    track: Track,
    playheadStep: Int,
    isPlaying: Boolean,
    hasClipboard: Boolean,
    onSelectPattern: (String) -> Unit,
    onSetStepCount: (Int) -> Unit,
    onToggleTriplet: () -> Unit,
    onSetSwing: (Float) -> Unit,
    onToggleStep: (rowIndex: Int, stepIndex: Int) -> Unit,
    onUpdateStepParams: (rowIndex: Int, stepIndex: Int, vel: Float, prob: Float, roll: Int) -> Unit,
    onPreviewRow: (DrumRowConfig) -> Unit,
    onUpdateRowPitch: (rowIndex: Int, pitch: Float) -> Unit,
    onAddRollFill: () -> Unit,
    onCopyPattern: () -> Unit,
    onPastePattern: () -> Unit,
    onOpenTrackSheet: () -> Unit,
    onSwitchToArrange: () -> Unit,
    onShowTooltip: (String) -> Unit
) {
    val pattern = track.patterns[track.activePatternId] ?: Pattern(id = track.activePatternId)
    val trackColor = Color(track.colorHex)
    val gridScroll = rememberScrollState()

    var editingStep by remember { mutableStateOf<Pair<DrumRowConfig, Int>?>(null) }
    var tuningRow by remember { mutableStateOf<DrumRowConfig?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Track Header + Pattern vs Song Timeline Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenTrackSheet,
                shape = RoundedCornerShape(14.dp),
                color = trackColor.copy(alpha = 0.22f),
                border = BorderStroke(1.5.dp, trackColor),
                modifier = Modifier.height(44.dp).testTag("beat_track_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = trackColor, modifier = Modifier.size(18.dp))
                    Text("${track.name} • Sound Add", style = MaterialTheme.typography.labelLarge, color = trackColor, fontWeight = FontWeight.Bold)
                }
            }

            AppChipButton(
                text = "Song Timeline",
                selected = false,
                icon = Icons.Default.ViewTimeline,
                accentColor = StudioAmber,
                onClick = onSwitchToArrange,
                onLongClick = { onShowTooltip("Song Timeline (Arrange View) kholne ke liye tap karein") },
                tag = "open_arrange_from_beat"
            )
        }

        // Pattern A..H & Grid Options Strip
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("A", "B", "C", "D", "E", "F", "G", "H").forEach { pid ->
                AppChipButton(
                    text = pid,
                    selected = track.activePatternId == pid,
                    accentColor = trackColor,
                    onClick = { onSelectPattern(pid) },
                    tag = "pattern_btn_$pid"
                )
            }
            AppChipButton(
                text = "${pattern.stepCount} Steps",
                selected = pattern.stepCount == 32,
                onClick = { onSetStepCount(if (pattern.stepCount == 16) 32 else 16) }
            )
            AppChipButton(
                text = if (pattern.isTripletGrid) "3x Triplet" else "Normal",
                selected = pattern.isTripletGrid,
                onClick = onToggleTriplet
            )
            AppChipButton(
                text = "Roll Fill",
                selected = false,
                icon = Icons.Default.Bolt,
                accentColor = StudioAmber,
                onClick = onAddRollFill,
                onLongClick = { onShowTooltip("Hi-hat mein trap/phonk roll fills automatically daalein") }
            )
            AppChipButton(
                text = "Copy",
                selected = false,
                icon = Icons.Default.ContentCopy,
                onClick = onCopyPattern
            )
            if (hasClipboard) {
                AppChipButton(
                    text = "Paste",
                    selected = true,
                    icon = Icons.Default.ContentPaste,
                    onClick = onPastePattern
                )
            }
        }

        // Swing slider compact bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Swing", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = project.swing,
                onValueChange = onSetSwing,
                valueRange = 0f..0.6f,
                modifier = Modifier.weight(1f).height(26.dp)
            )
            Text("${(project.swing * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
        }

        // Drum Step Sequencer Rows
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(track.drumRows, key = { it.rowIndex }) { row ->
                val steps = pattern.drumRows[row.rowIndex].orEmpty()
                val rowAccent = when {
                    row.soundType == DrumSoundType.PHONK_COWBELL || row.soundType == DrumSoundType.MEMPHIS_HIT -> PhonkCoral
                    row.soundType.name.contains("KICK") || row.soundType == DrumSoundType.BASS_808 -> StudioAmber
                    row.soundType.name.startsWith("FX_") -> StudioViolet
                    else -> NeonCyan
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Big Colored Row Pad Button (tap to audition, long press to tune pitch)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = rowAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, rowAccent.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .width(104.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { onPreviewRow(row) },
                                onLongClick = { tuningRow = row }
                            )
                            .testTag("drum_pad_${row.rowIndex}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = row.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = rowAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (row.pitchOffset != 0f) {
                                Text(
                                    text = "${if (row.pitchOffset > 0) "+" else ""}${row.pitchOffset.toInt()} st",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Step Grid Cells
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(gridScroll),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        for (s in 0 until pattern.stepCount) {
                            val step = steps.getOrElse(s) { DrumStep() }
                            val isBeatStart = s % 4 == 0
                            val isCurrent = isPlaying && (playheadStep % pattern.stepCount == s)

                            val bg = when {
                                step.active && isCurrent -> Color.White
                                step.active -> rowAccent.copy(alpha = (0.4f + step.velocity * 0.6f).coerceIn(0.35f, 1f))
                                isCurrent -> StudioSurfaceHighlight
                                isBeatStart -> MaterialTheme.colorScheme.surfaceVariant
                                else -> StudioObsidian
                            }
                            val border = when {
                                isCurrent -> Color.White
                                step.active -> rowAccent
                                isBeatStart -> MaterialTheme.colorScheme.outline
                                else -> StudioSurfaceHighlight
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = bg,
                                border = BorderStroke(if (isCurrent) 2.dp else 1.dp, border),
                                modifier = Modifier
                                    .size(width = 38.dp, height = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .combinedClickable(
                                        onClick = { onToggleStep(row.rowIndex, s) },
                                        onLongClick = { editingStep = row to s }
                                    )
                                    .testTag("step_${row.rowIndex}_$s")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (step.rollCount > 1 && step.active) {
                                        Text(
                                            text = "${step.rollCount}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioObsidian
                                        )
                                    } else if (step.probability < 0.99f && step.active) {
                                        Text(
                                            text = "${(step.probability * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioObsidian
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Long-press Step Velocity, Probability & Hi-hat Roll Modal
    editingStep?.let { (row, stepIdx) ->
        val existing = pattern.drumRows[row.rowIndex]?.getOrNull(stepIdx) ?: DrumStep(active = true)
        var vel by remember { mutableFloatStateOf(existing.velocity) }
        var prob by remember { mutableFloatStateOf(existing.probability) }
        var roll by remember { mutableIntStateOf(existing.rollCount) }

        AlertDialog(
            onDismissRequest = { editingStep = null },
            title = { Text("${row.label} • Step ${stepIdx + 1}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SliderRow("Velocity (Loudness)", vel, accentColor = NeonCyan, onValueChange = { vel = it })
                    SliderRow("Probability (Chance)", prob, accentColor = StudioAmber, onValueChange = { prob = it })
                    Text("Roll / Stutter", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "1x", 2 to "2x", 3 to "3x Triplet", 4 to "4x Rapid").forEach { (rCount, lbl) ->
                            AppChipButton(lbl, roll == rCount, { roll = rCount }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateStepParams(row.rowIndex, stepIdx, vel, prob, roll)
                    editingStep = null
                }) { Text("Save karo") }
            },
            dismissButton = {
                TextButton(onClick = { editingStep = null }) { Text("Cancel") }
            }
        )
    }

    // Long-press Row Pitch Tuner (for Phonk Cowbell & 808!)
    tuningRow?.let { row ->
        var pitch by remember { mutableFloatStateOf(row.pitchOffset) }
        AlertDialog(
            onDismissRequest = { tuningRow = null },
            title = { Text("${row.label} Tune Karein") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SliderRow(
                        label = "Pitch Offset",
                        value = pitch,
                        valueRange = -12f..12f,
                        valueFormatter = { "${it.toInt()} st" },
                        accentColor = PhonkCoral,
                        onValueChange = { pitch = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateRowPitch(row.rowIndex, pitch)
                    tuningRow = null
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { tuningRow = null }) { Text("Cancel") }
            }
        )
    }
}
