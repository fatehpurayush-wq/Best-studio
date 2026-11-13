package com.beatstudio.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import com.beatstudio.app.audio.effects.SlowedReverb
import com.beatstudio.app.domain.MusicalScale
import com.beatstudio.app.domain.Project
import com.beatstudio.app.music.Scales
import com.beatstudio.app.ui.components.AppChipButton
import com.beatstudio.app.ui.components.SliderRow
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioAmber
import com.beatstudio.app.ui.theme.StudioObsidian

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopBar(
    project: Project,
    canUndo: Boolean,
    canRedo: Boolean,
    isProMode: Boolean,
    onBack: () -> Unit,
    onRenameProject: (String) -> Unit,
    onSetBpm: (Int) -> Unit,
    onTapTempo: () -> Unit,
    onSetTimeSig: (Int) -> Unit,
    onToggleMetronome: () -> Unit,
    onToggleSlowedReverb: () -> Unit,
    onSetKeyScale: (Int, MusicalScale) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleProMode: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenMagic: () -> Unit,
    onOpenTour: () -> Unit,
    onShowTooltip: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showTempoDialog by remember { mutableStateOf(false) }
    var showKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Back, Project Name, Undo/Redo, Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp).testTag("back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wapas jao")
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(
                        onClick = { showRenameDialog = true },
                        onLongClick = { onShowTooltip("Tap karke project ka naam badlein") }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.genre.labelHinglish} • Auto-saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Always visible Undo / Redo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(40.dp).testTag("undo_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(40.dp).testTag("redo_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            Surface(
                onClick = onOpenExport,
                shape = RoundedCornerShape(12.dp),
                color = NeonCyan,
                contentColor = StudioObsidian,
                modifier = Modifier.height(40.dp).testTag("export_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Export", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Row 2: BPM, Key/Scale, Simple/Pro Switch, Magic, Tour Help
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val effBpm = SlowedReverb.displayEffectiveBpm(project)
            AppChipButton(
                text = if (project.slowedReverbEnabled) "$effBpm BPM (Slowed)" else "${project.bpm} BPM",
                selected = project.slowedReverbEnabled,
                accentColor = if (project.slowedReverbEnabled) PhonkCoral else NeonCyan,
                icon = Icons.Default.Speed,
                onClick = { showTempoDialog = true },
                onLongClick = { onShowTooltip("BPM, Tap Tempo, Metronome aur Slowed+Reverb yahan hai") },
                tag = "bpm_chip"
            )

            AppChipButton(
                text = "${Scales.rootName(project.rootNote)} ${project.scale.labelHinglish}",
                selected = false,
                icon = Icons.Default.MusicNote,
                onClick = { showKeyDialog = true },
                onLongClick = { onShowTooltip("Gaane ki Key aur Scale chunein — wrong notes lock ho jayenge") },
                tag = "key_chip"
            )

            AppChipButton(
                text = if (isProMode) "PRO" else "SIMPLE",
                selected = isProMode,
                accentColor = StudioAmber,
                icon = if (isProMode) Icons.Default.Tune else Icons.Default.AutoFixHigh,
                onClick = onToggleProMode,
                onLongClick = { onShowTooltip("SIMPLE: asaan sliders | PRO: full ADSR, 8-band EQ, automation") },
                tag = "simple_pro_switch"
            )

            AppChipButton(
                text = "Magic",
                selected = false,
                accentColor = PhonkCoral,
                icon = Icons.Default.AutoAwesome,
                onClick = onOpenMagic,
                onLongClick = { onShowTooltip("Ek tap mein Auto Beat aur Song banao!") },
                tag = "magic_top_chip"
            )

            IconButton(
                onClick = onOpenTour,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Guide", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showRenameDialog) {
        var text by remember { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Beat ka naam badlo") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Project name") }
                )
            },
            confirmButton = {
                TextButton(onClick = { onRenameProject(text); showRenameDialog = false }) {
                    Text("Save karo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTempoDialog) {
        AlertDialog(
            onDismissRequest = { showTempoDialog = false },
            title = { Text("Tempo, Time Sig & Slowed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SliderRow(
                        label = "BPM (Speed)",
                        value = project.bpm.toFloat(),
                        valueRange = 40f..220f,
                        onValueChange = { onSetBpm(it.toInt()) },
                        valueFormatter = { "${it.toInt()} BPM" }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onTapTempo,
                            modifier = Modifier.weight(1f)
                        ) { Text("Tap Tempo") }
                        OutlinedButton(
                            onClick = onToggleMetronome,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (project.metronomeEnabled) "Click: ON" else "Click: OFF") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Time Signature")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppChipButton("4/4", project.timeSignatureTop == 4, { onSetTimeSig(4) })
                            AppChipButton("3/4", project.timeSignatureTop == 3, { onSetTimeSig(3) })
                        }
                    }
                    Surface(
                        onClick = onToggleSlowedReverb,
                        shape = RoundedCornerShape(14.dp),
                        color = if (project.slowedReverbEnabled) PhonkCoral.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (project.slowedReverbEnabled) PhonkCoral else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Slowed + Reverb", fontWeight = FontWeight.Bold)
                                Text("Speed & pitch kam + big dreamy reverb", style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = project.slowedReverbEnabled, onCheckedChange = { onToggleSlowedReverb() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTempoDialog = false }) { Text("Done") }
            }
        )
    }

    if (showKeyDialog) {
        var tempRoot by remember { mutableIntStateOf(project.rootNote) }
        var tempScale by remember { mutableStateOf(project.scale) }
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Key aur Scale chunein") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Root Note", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Scales.noteNames.forEachIndexed { idx, name ->
                            AppChipButton(
                                text = name,
                                selected = idx == tempRoot,
                                onClick = { tempRoot = idx }
                            )
                        }
                    }
                    Text("Scale", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MusicalScale.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pair.forEach { sc ->
                                    AppChipButton(
                                        text = sc.labelHinglish,
                                        selected = sc == tempScale,
                                        onClick = { tempScale = sc },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetKeyScale(tempRoot, tempScale)
                    showKeyDialog = false
                }) { Text("Apply karo") }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) { Text("Cancel") }
            }
        )
    }
}
