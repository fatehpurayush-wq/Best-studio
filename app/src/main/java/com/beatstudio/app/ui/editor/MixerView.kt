package com.beatstudio.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatstudio.app.domain.*
import com.beatstudio.app.ui.components.*
import com.beatstudio.app.ui.theme.*

@Composable
fun MixerView(
    project: Project,
    isProMode: Boolean,
    masterLevel: Pair<Float, Float>,
    trackLevels: Map<String, Float>,
    onUpdateTrack: (String, Boolean, (Track) -> Track) -> Unit,
    onUpdateMasterVolume: (Float) -> Unit,
    onTogglePhonkDrive: () -> Unit,
    onToggleSlowedReverb: () -> Unit,
    onOpenTrackSheet: (Track) -> Unit,
    onOpenTrackEffects: (Track) -> Unit,
    onOpenMasterEffects: () -> Unit
) {
    var automationTrack by remember { mutableStateOf<Track?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Bus Card
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonCyan.copy(alpha = 0.5f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("MASTER BUS (Limiter ON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                            StereoLevelMeter(leftLevel = masterLevel.first, rightLevel = masterLevel.second)
                        }
                        FilledTonalButton(
                            onClick = onOpenMasterEffects,
                            modifier = Modifier.testTag("master_fx_button")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Master FX")
                        }
                    }

                    SliderRow(
                        label = "Master Volume",
                        value = project.masterVolume,
                        valueRange = 0f..1.2f,
                        accentColor = NeonCyan,
                        onValueChange = onUpdateMasterVolume
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppChipButton(
                            text = "Phonk Drive",
                            selected = project.phonkDriveEnabled,
                            accentColor = StudioAmber,
                            icon = Icons.Default.Whatshot,
                            onClick = onTogglePhonkDrive,
                            modifier = Modifier.weight(1f)
                        )
                        AppChipButton(
                            text = "Slowed + Reverb",
                            selected = project.slowedReverbEnabled,
                            accentColor = PhonkCoral,
                            icon = Icons.Default.Nightlight,
                            onClick = onToggleSlowedReverb,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Per-Track Channel Strips Horizontal Rack
        item {
            Text("Track Channels", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(project.tracks, key = { it.id }) { track ->
                    val c = Color(track.colorHex)
                    val peak = trackLevels[track.id] ?: 0f

                    AppCard(
                        modifier = Modifier.width(154.dp),
                        borderColor = c.copy(alpha = 0.55f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Big Colored Track Button
                            Surface(
                                onClick = { onOpenTrackSheet(track) },
                                shape = RoundedCornerShape(12.dp),
                                color = c.copy(alpha = 0.22f),
                                border = BorderStroke(1.5.dp, c),
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("mixer_track_${track.id}")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Text(
                                        text = track.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = c,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VerticalLevelMeter(level = peak)
                                Knob(
                                    label = "Pan",
                                    value = (track.pan + 1f) * 0.5f,
                                    bipolar = true,
                                    accentColor = c,
                                    onValueChange = { norm ->
                                        onUpdateTrack(track.id, false) { it.copy(pan = norm * 2f - 1f) }
                                    }
                                )
                            }

                            SliderRow(
                                label = "Vol",
                                value = track.volume,
                                valueRange = 0f..1.2f,
                                accentColor = c,
                                onValueChange = { v ->
                                    onUpdateTrack(track.id, false) { it.copy(volume = v) }
                                }
                            )

                            // Mute / Solo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AppChipButton(
                                    text = "M",
                                    selected = track.muted,
                                    accentColor = PhonkCoral,
                                    onClick = { onUpdateTrack(track.id, true) { it.copy(muted = !it.muted) } },
                                    modifier = Modifier.weight(1f),
                                    tag = "mute_${track.id}"
                                )
                                AppChipButton(
                                    text = "S",
                                    selected = track.solo,
                                    accentColor = StudioAmber,
                                    onClick = { onUpdateTrack(track.id, true) { it.copy(solo = !it.solo) } },
                                    modifier = Modifier.weight(1f),
                                    tag = "solo_${track.id}"
                                )
                            }

                            // Track FX & Sidechain Quick Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenTrackEffects(track) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text("FX", style = MaterialTheme.typography.labelMedium)
                                }
                                if (track.type != TrackType.DRUMS) {
                                    AppChipButton(
                                        text = "Pump",
                                        selected = track.effects.sidechainEnabled,
                                        accentColor = PhonkCoral,
                                        onClick = {
                                            onUpdateTrack(track.id, true) {
                                                it.copy(effects = it.effects.copy(sidechainEnabled = !it.effects.sidechainEnabled))
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (isProMode) {
                                TextButton(
                                    onClick = { automationTrack = track },
                                    modifier = Modifier.fillMaxWidth().height(34.dp)
                                ) {
                                    Text("Automation", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pro Mode Automation Curve Drawer Dialog
    automationTrack?.let { tr ->
        var target by remember { mutableStateOf(AutomationTarget.FILTER_CUTOFF) }
        val existingLane = tr.automation.firstOrNull { it.target == target }
            ?: AutomationLane(target = target, points = listOf(AutomationPoint(0f, 0.3f), AutomationPoint(8f, 0.9f), AutomationPoint(16f, 0.5f)), enabled = false)
        var points by remember(target, tr.id) { mutableStateOf(existingLane.points.ifEmpty { listOf(AutomationPoint(0f, 0.5f), AutomationPoint(16f, 0.5f)) }) }
        var enabled by remember(target, tr.id) { mutableStateOf(existingLane.enabled) }

        AlertDialog(
            onDismissRequest = { automationTrack = null },
            title = { Text("${tr.name} • Automation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AutomationTarget.entries.forEach { t ->
                            AppChipButton(t.labelHinglish, t == target, { target = t })
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lane Enable", fontWeight = FontWeight.Bold)
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                    Text("Graph par tap karke curve points banayein (Bars 1-16)", style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioObsidian,
                        border = BorderStroke(1.dp, StudioOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    val barPos = ((offset.x / size.width) * 16f).coerceIn(0f, 16f)
                                    val valNorm = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                    val updated = (points.filterNot { kotlin.math.abs(it.barPosition - barPos) < 1f } +
                                        AutomationPoint(barPos, valNorm)).sortedBy { it.barPosition }
                                    points = updated
                                    enabled = true
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            val sorted = points.sortedBy { it.barPosition }
                            if (sorted.size >= 2) {
                                val path = Path()
                                sorted.forEachIndexed { idx, pt ->
                                    val x = (pt.barPosition / 16f) * size.width
                                    val y = (1f - pt.value) * size.height
                                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    drawCircle(NeonCyan, radius = 5.dp.toPx(), center = Offset(x, y))
                                }
                                drawPath(path, color = NeonCyan, style = Stroke(width = 2.5.dp.toPx()))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateTrack(tr.id, true) { currentTrack ->
                        val others = currentTrack.automation.filterNot { it.target == target }
                        currentTrack.copy(automation = others + AutomationLane(target, points, enabled))
                    }
                    automationTrack = null
                }) { Text("Save Curve") }
            },
            dismissButton = {
                TextButton(onClick = { automationTrack = null }) { Text("Cancel") }
            }
        )
    }
}
