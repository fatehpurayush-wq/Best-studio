package com.beatstudio.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatstudio.app.domain.*
import com.beatstudio.app.music.ChordPreset
import com.beatstudio.app.music.Chords
import com.beatstudio.app.music.Scales
import com.beatstudio.app.ui.components.AppChipButton
import com.beatstudio.app.ui.components.PianoKeyboard
import com.beatstudio.app.ui.components.SliderRow
import com.beatstudio.app.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PianoRoll(
    project: Project,
    tracks: List<Track>,
    selectedTrack: Track,
    scaleLockEnabled: Boolean,
    isProMode: Boolean,
    playheadStep: Int,
    isPlaying: Boolean,
    onSelectTrack: (String) -> Unit,
    onOpenTrackSheet: (Track) -> Unit,
    onSelectPattern: (String) -> Unit,
    onToggleScaleLock: () -> Unit,
    onQuantize: () -> Unit,
    onApplyChord: (ChordPreset) -> Unit,
    onToggleOrAddNote: (midi: Int, startStep: Float) -> Unit,
    onUpdateNote: (Note) -> Unit,
    onDeleteNote: (noteId: String) -> Unit,
    onLiveKeyDown: (Int) -> Unit,
    onLiveKeyUp: (Int) -> Unit,
    onShowTooltip: (String) -> Unit
) {
    val melodyTracks = tracks.filter { it.type != TrackType.DRUMS }
    val pattern = selectedTrack.patterns[selectedTrack.activePatternId] ?: Pattern(id = selectedTrack.activePatternId)
    val trackColor = Color(selectedTrack.colorHex)

    val pitchList = remember(project.rootNote, project.scale, scaleLockEnabled) {
        val list = if (scaleLockEnabled && project.scale != MusicalScale.CHROMATIC) {
            Scales.getScaleMidiNotes(project.rootNote, project.scale, lowMidi = 36, highMidi = 84)
        } else {
            (36..84).toList()
        }
        list.reversed()
    }

    val chordPresets = remember(project.genre) { Chords.presetsForGenre(project.genre) }
    var showChordMenu by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    val horizontalScroll = rememberScrollState()
    val stepWidthDp = 36.dp
    val rowHeightDp = 34.dp
    val density = LocalDensity.current
    val stepWidthPx = with(density) { stepWidthDp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Big Colored Track Selector Buttons (tap again to open TrackSheet!)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            melodyTracks.forEach { t ->
                val c = Color(t.colorHex)
                val isSel = t.id == selectedTrack.id
                Surface(
                    onClick = {
                        if (isSel) onOpenTrackSheet(t) else onSelectTrack(t.id)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSel) c.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) c else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.height(46.dp).testTag("melody_track_btn_${t.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(c, RoundedCornerShape(5.dp)))
                        Text(
                            text = t.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) c else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSel) {
                            Icon(Icons.Default.Tune, contentDescription = "Sound", tint = c, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Toolbar: Scale Lock, Chord Helper, Quantize, Pattern A..H
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppChipButton(
                text = if (scaleLockEnabled) "Scale Lock: ON" else "All Notes",
                selected = scaleLockEnabled,
                icon = Icons.Default.Lock,
                accentColor = StudioEmerald,
                onClick = onToggleScaleLock,
                onLongClick = { onShowTooltip("Scale Lock ON hone par sirf sahi sur dikhte hain") },
                tag = "scale_lock_button"
            )

            Box {
                AppChipButton(
                    text = "Chord Helper",
                    selected = false,
                    icon = Icons.Default.LibraryMusic,
                    accentColor = StudioAmber,
                    onClick = { showChordMenu = true },
                    onLongClick = { onShowTooltip("Ek tap mein genre ke hit chords add karein") },
                    tag = "chord_helper_button"
                )
                DropdownMenu(expanded = showChordMenu, onDismissRequest = { showChordMenu = false }) {
                    chordPresets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(preset.titleHinglish, fontWeight = FontWeight.Bold)
                                    Text(preset.subtitle, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            onClick = {
                                showChordMenu = false
                                onApplyChord(preset)
                            }
                        )
                    }
                }
            }

            AppChipButton(
                text = "Quantize",
                selected = false,
                icon = Icons.Default.GridOn,
                onClick = onQuantize
            )

            listOf("A", "B", "C", "D").forEach { pid ->
                AppChipButton(
                    text = pid,
                    selected = selectedTrack.activePatternId == pid,
                    accentColor = trackColor,
                    onClick = { onSelectPattern(pid) }
                )
            }
        }

        // Piano Roll Note Grid
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(StudioObsidian, RoundedCornerShape(14.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(pitchList, key = { it }) { midi ->
                val isRoot = ((midi - project.rootNote) % 12 + 12) % 12 == 0
                val rowNotes = pattern.notes.filter { it.pitch == midi }

                Row(
                    modifier = Modifier.fillMaxWidth().height(rowHeightDp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pitch Key Label
                    Surface(
                        color = if (isRoot) trackColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.width(48.dp).fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = Scales.midiToName(midi),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRoot) trackColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Horizontal Step Canvas & Draggable Notes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScroll)
                    ) {
                        val totalWidth = stepWidthDp * pattern.stepCount
                        Row(modifier = Modifier.width(totalWidth).fillMaxHeight()) {
                            for (s in 0 until pattern.stepCount) {
                                val isCur = isPlaying && (playheadStep % pattern.stepCount == s)
                                val cellBg = when {
                                    isCur -> trackColor.copy(alpha = 0.2f)
                                    s % 4 == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    else -> StudioSurface.copy(alpha = 0.7f)
                                }
                                Surface(
                                    onClick = { onToggleOrAddNote(midi, s.toFloat()) },
                                    color = cellBg,
                                    border = BorderStroke(0.5.dp, StudioOutline.copy(alpha = 0.45f)),
                                    modifier = Modifier.width(stepWidthDp).fillMaxHeight()
                                ) {}
                            }
                        }

                        // Overlay Notes (drag to move or resize in Pro/Simple)
                        rowNotes.forEach { note ->
                            val offsetX = stepWidthDp * note.startStep
                            val widthDp = (stepWidthDp * note.durationSteps).coerceAtLeast(28.dp)
                            var dragDeltaX by remember(note.id) { mutableFloatStateOf(0f) }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = trackColor.copy(alpha = (0.45f + note.velocity * 0.55f).coerceIn(0.4f, 1f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .offset(x = offsetX)
                                    .width(widthDp)
                                    .fillMaxHeight()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = { onDeleteNote(note.id) },
                                        onLongClick = { editingNote = note }
                                    )
                                    .pointerInput(note.id) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                val stepShift = (dragDeltaX / stepWidthPx).roundToInt().toFloat()
                                                dragDeltaX = 0f
                                                if (stepShift != 0f) {
                                                    val nextStart = (note.startStep + stepShift).coerceIn(0f, (pattern.stepCount - 1).toFloat())
                                                    onUpdateNote(note.copy(startStep = nextStart))
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragDeltaX += dragAmount.x
                                            }
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = Scales.midiToName(note.pitch),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioObsidian,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Piano Keyboard
        PianoKeyboard(
            rootNote = project.rootNote,
            scale = project.scale,
            scaleLockEnabled = scaleLockEnabled,
            accentColor = trackColor,
            onNoteDown = onLiveKeyDown,
            onNoteUp = onLiveKeyUp
        )
    }

    // Per-Note Velocity & Duration Editor Dialog
    editingNote?.let { note ->
        var vel by remember { mutableFloatStateOf(note.velocity) }
        var dur by remember { mutableFloatStateOf(note.durationSteps) }
        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = { Text("Note Edit (${Scales.midiToName(note.pitch)})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SliderRow("Velocity", vel, accentColor = trackColor, onValueChange = { vel = it })
                    SliderRow(
                        label = "Length (Steps)",
                        value = dur,
                        valueRange = 0.5f..8f,
                        valueFormatter = { "${((it * 2f).roundToInt() / 2f)} steps" },
                        accentColor = trackColor,
                        onValueChange = { dur = (it * 2f).roundToInt() / 2f }
                    )
                    if (isProMode) {
                        Text("Tip: Note ko drag karke move bhi kar sakte hain.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateNote(note.copy(velocity = vel, durationSteps = dur))
                    editingNote = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDeleteNote(note.id)
                    editingNote = null
                }) { Text("Delete", color = PhonkCoral) }
            }
        )
    }
}
