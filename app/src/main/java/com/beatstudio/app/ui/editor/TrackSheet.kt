package com.beatstudio.app.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.beatstudio.app.audio.instruments.Presets
import com.beatstudio.app.domain.*
import com.beatstudio.app.ui.components.AppButton
import com.beatstudio.app.ui.components.AppChipButton
import com.beatstudio.app.ui.components.SliderRow
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioAmber
import com.beatstudio.app.ui.theme.StudioObsidian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSheet(
    track: Track,
    isProMode: Boolean,
    isRecordingMic: Boolean,
    onDismiss: () -> Unit,
    onUpdateTrack: ((Track) -> Track) -> Unit,
    onSelectPreset: (SynthPresetId) -> Unit,
    onAddDrumRow: (DrumSoundType) -> Unit,
    onImportSampleUri: (android.net.Uri) -> Unit,
    onImportMidiUri: (android.net.Uri) -> Unit,
    onToggleMicRecord: () -> Unit,
    onOpenEffects: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val trackColor = Color(track.colorHex)

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportSampleUri(it) }
    }

    val midiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportMidiUri(it) }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleMicRecord()
        } else {
            onShowMessage("Mic permission nahi mili! Voice record karne ke liye Microphone permission allow karein.")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = trackColor.copy(alpha = 0.25f),
                        border = BorderStroke(1.5.dp, trackColor),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = trackColor)
                        }
                    }
                    Column {
                        Text(track.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(track.type.labelHinglish, style = MaterialTheme.typography.labelSmall, color = trackColor)
                    }
                }
                OutlinedButton(
                    onClick = onOpenEffects,
                    modifier = Modifier.testTag("open_track_fx_button")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Effects")
                }
            }

            when (track.type) {
                TrackType.INSTRUMENT -> {
                    Text("Sound Chunein", style = MaterialTheme.typography.titleMedium)
                    SynthSoundPickerGrid(
                        selectedPreset = track.synthParams.presetId,
                        onSelectPreset = onSelectPreset
                    )

                    Text("Simple Controls", style = MaterialTheme.typography.titleMedium)
                    val sp = track.synthParams
                    SliderRow(
                        label = "Tone (Warmth / Sub)",
                        value = sp.tone,
                        accentColor = trackColor,
                        onValueChange = { v ->
                            onUpdateTrack { it.copy(synthParams = Presets.applySimpleControls(sp, v, sp.attack, sp.release, sp.brightness)) }
                        }
                    )
                    SliderRow(
                        label = "Attack (Smooth start)",
                        value = sp.attack,
                        accentColor = trackColor,
                        onValueChange = { v ->
                            onUpdateTrack { it.copy(synthParams = Presets.applySimpleControls(sp, sp.tone, v, sp.release, sp.brightness)) }
                        }
                    )
                    SliderRow(
                        label = "Release (Tail length)",
                        value = sp.release,
                        accentColor = trackColor,
                        onValueChange = { v ->
                            onUpdateTrack { it.copy(synthParams = Presets.applySimpleControls(sp, sp.tone, sp.attack, v, sp.brightness)) }
                        }
                    )
                    SliderRow(
                        label = "Brightness (Filter)",
                        value = sp.brightness,
                        accentColor = trackColor,
                        onValueChange = { v ->
                            onUpdateTrack { it.copy(synthParams = Presets.applySimpleControls(sp, sp.tone, sp.attack, sp.release, v)) }
                        }
                    )

                    if (isProMode) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text("PRO Synth Controls", style = MaterialTheme.typography.titleMedium, color = StudioAmber)

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OscWaveform.entries.forEach { wf ->
                                AppChipButton(
                                    text = wf.labelHinglish,
                                    selected = sp.waveform == wf,
                                    accentColor = StudioAmber,
                                    onClick = { onUpdateTrack { it.copy(synthParams = sp.copy(waveform = wf)) } }
                                )
                            }
                        }

                        SliderRow("Decay", sp.decay, accentColor = StudioAmber, onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(decay = v)) } })
                        SliderRow("Sustain", sp.sustain, accentColor = StudioAmber, onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(sustain = v)) } })
                        SliderRow(
                            label = "Unison Voices",
                            value = sp.unisonVoices.toFloat(),
                            valueRange = 1f..5f,
                            accentColor = StudioAmber,
                            valueFormatter = { "${it.toInt()}x" },
                            onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(unisonVoices = v.toInt())) } }
                        )
                        SliderRow("Detune", sp.detune, accentColor = StudioAmber, onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(detune = v)) } })
                        SliderRow("Filter Cutoff", sp.cutoff, accentColor = StudioAmber, onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(cutoff = v)) } })
                        SliderRow("Filter Resonance", sp.resonance, accentColor = StudioAmber, onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(resonance = v)) } })
                        SliderRow("LFO Depth", sp.lfoDepth, accentColor = StudioAmber, onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(lfoDepth = v)) } })
                        SliderRow("LFO Rate", sp.lfoRate, accentColor = StudioAmber, onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(lfoRate = v)) } })
                        SliderRow("808 / Synth Glide", sp.glideTime, accentColor = StudioAmber, onValueChange = { v -> onUpdateTrack { it.copy(synthParams = sp.copy(glideTime = v)) } })
                    }

                    // MIDI Import Button
                    OutlinedButton(
                        onClick = { midiPicker.launch(arrayOf("audio/midi", "audio/x-midi", "*/*")) },
                        modifier = Modifier.fillMaxWidth().testTag("import_midi_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("MIDI (.mid) file import karein")
                    }
                }

                TrackType.DRUMS -> {
                    Text("Naya Drum ya FX Sweep Add Karein", style = MaterialTheme.typography.titleMedium)
                    DrumSoundPickerGrid(
                        onPickDrumSound = { sound ->
                            onAddDrumRow(sound)
                            onShowMessage("${sound.labelHinglish} add ho gaya!")
                        }
                    )
                }

                TrackType.SAMPLER -> {
                    val sm = track.samplerParams
                    Text("Apna Audio Sample Import Karein", style = MaterialTheme.typography.titleMedium)
                    AppButton(
                        text = if (sm.sampleFilePath == null) "Audio File Chunein (WAV/MP3/M4A)" else "Sample: ${sm.sampleName}",
                        icon = Icons.Default.FolderOpen,
                        containerColor = NeonCyan,
                        contentColor = StudioObsidian,
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        tag = "pick_sample_button"
                    )
                    SliderRow(
                        label = "Trim Start",
                        value = sm.trimStart,
                        valueRange = 0f..0.85f,
                        accentColor = trackColor,
                        onValueChange = { v ->
                            onUpdateTrack { it.copy(samplerParams = sm.copy(trimStart = v.coerceAtMost(sm.trimEnd - 0.05f))) }
                        }
                    )
                    SliderRow(
                        label = "Trim End",
                        value = sm.trimEnd,
                        valueRange = 0.1f..1f,
                        accentColor = trackColor,
                        onValueChange = { v ->
                            onUpdateTrack { it.copy(samplerParams = sm.copy(trimEnd = v.coerceAtLeast(sm.trimStart + 0.05f))) }
                        }
                    )
                    SliderRow(
                        label = "Pitch (Semitones)",
                        value = sm.pitchSemitones,
                        valueRange = -12f..12f,
                        accentColor = trackColor,
                        valueFormatter = { "${it.toInt()} st" },
                        onValueChange = { v -> onUpdateTrack { it.copy(samplerParams = sm.copy(pitchSemitones = v)) } }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppChipButton(
                            text = "Reverse",
                            selected = sm.reverse,
                            onClick = { onUpdateTrack { it.copy(samplerParams = sm.copy(reverse = !sm.reverse)) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppChipButton(
                            text = "Loop",
                            selected = sm.loop,
                            onClick = { onUpdateTrack { it.copy(samplerParams = sm.copy(loop = !sm.loop)) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                TrackType.VOCAL -> {
                    Text("Mic Voice Recording", style = MaterialTheme.typography.titleMedium)
                    AppButton(
                        text = if (isRecordingMic) "Recording Roko & Save" else "Mic se Record Karein (Count-in)",
                        icon = if (isRecordingMic) Icons.Default.Stop else Icons.Default.Mic,
                        containerColor = if (isRecordingMic) PhonkCoral else NeonCyan,
                        contentColor = if (isRecordingMic) Color.White else StudioObsidian,
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onToggleMicRecord()
                            } else {
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        tag = "mic_record_sheet_button"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Tune Style Pitch Snap", fontWeight = FontWeight.Bold)
                            Text(
                                "Gaane ki selected Key & Scale mein sur snap karein",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = track.effects.pitchSnapEnabled,
                            onCheckedChange = { en ->
                                onUpdateTrack { it.copy(effects = it.effects.copy(pitchSnapEnabled = en)) }
                            }
                        )
                    }
                    if (track.effects.pitchSnapEnabled) {
                        SliderRow(
                            label = "Pitch Snap Strength",
                            value = track.effects.pitchSnapStrength,
                            accentColor = NeonCyan,
                            onValueChange = { v ->
                                onUpdateTrack { it.copy(effects = it.effects.copy(pitchSnapStrength = v)) }
                            }
                        )
                    }
                }
            }
        }
    }
}
