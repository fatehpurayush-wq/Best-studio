package com.beatstudio.app.ui.editor

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatstudio.app.domain.GenreId
import com.beatstudio.app.domain.Project
import com.beatstudio.app.ui.components.AppButton
import com.beatstudio.app.ui.components.AppChipButton
import com.beatstudio.app.ui.components.SliderRow
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioAmber
import com.beatstudio.app.ui.theme.StudioObsidian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicSheet(
    project: Project,
    onDismiss: () -> Unit,
    onApplyGenrePreset: (GenreId) -> Unit,
    onAutoBeat: (variation: Float, shuffle: Boolean) -> Unit,
    onGenerateSong: () -> Unit,
    onToggleSlowedReverb: () -> Unit,
    onTogglePhonkDrive: () -> Unit
) {
    var variation by remember { mutableFloatStateOf(0.35f) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(28.dp))
                Column {
                    Text("Magic Beat Lab", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Rule-based offline beat, bass, chords aur song generator",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Genre Switcher Row
            Text("Genre Preset badlo (BPM, Sounds & Effects)", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenreId.entries.forEach { genre ->
                    AppChipButton(
                        text = genre.labelHinglish,
                        selected = project.genre == genre,
                        accentColor = Color(genre.tagColorHex),
                        onClick = { onApplyGenrePreset(genre) }
                    )
                }
            }

            // Variation Slider
            SliderRow(
                label = "Variation (Alag patterns & fills)",
                value = variation,
                onValueChange = { variation = it },
                accentColor = StudioAmber
            )

            // Big Auto Beat & Shuffle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppButton(
                    text = "Auto beat banao",
                    icon = Icons.Default.AutoAwesome,
                    containerColor = NeonCyan,
                    contentColor = StudioObsidian,
                    onClick = { onAutoBeat(variation, false) },
                    modifier = Modifier.weight(1f),
                    tag = "auto_beat_button"
                )
                AppButton(
                    text = "Dobara banao",
                    icon = Icons.Default.Shuffle,
                    containerColor = StudioAmber,
                    contentColor = StudioObsidian,
                    onClick = { onAutoBeat(variation, true) },
                    modifier = Modifier.weight(1f),
                    tag = "shuffle_beat_button"
                )
            }

            // Song Timeline Generator
            AppButton(
                text = "Song banao (Intro • Verse • Drop • Outro)",
                icon = Icons.Default.ViewTimeline,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    onGenerateSong()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                tag = "song_banao_button"
            )

            // One-tap Master Macros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onToggleSlowedReverb,
                    shape = RoundedCornerShape(14.dp),
                    color = if (project.slowedReverbEnabled) PhonkCoral.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (project.slowedReverbEnabled) PhonkCoral else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f).testTag("slowed_reverb_toggle")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Slowed + Reverb", fontWeight = FontWeight.Bold)
                        Text(
                            if (project.slowedReverbEnabled) "ON (Slowed edit)" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (project.slowedReverbEnabled) PhonkCoral else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onTogglePhonkDrive,
                    shape = RoundedCornerShape(14.dp),
                    color = if (project.phonkDriveEnabled) StudioAmber.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (project.phonkDriveEnabled) StudioAmber else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f).testTag("phonk_drive_toggle")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Phonk Drive", fontWeight = FontWeight.Bold)
                        Text(
                            if (project.phonkDriveEnabled) "ON (Punchy crunch)" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (project.phonkDriveEnabled) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
