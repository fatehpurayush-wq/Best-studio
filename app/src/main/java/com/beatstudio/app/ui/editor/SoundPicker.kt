package com.beatstudio.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beatstudio.app.audio.instruments.Presets
import com.beatstudio.app.domain.DrumSoundType
import com.beatstudio.app.domain.SynthPresetId
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioAmber
import com.beatstudio.app.ui.theme.StudioViolet

@Composable
fun SynthSoundPickerGrid(
    selectedPreset: SynthPresetId,
    onSelectPreset: (SynthPresetId) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth().heightIn(max = 270.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(Presets.allPresets) { preset ->
            val selected = preset == selectedPreset
            val (accent, icon) = presetVisual(preset)
            Surface(
                onClick = { onSelectPreset(preset) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) accent.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.height(78.dp).testTag("sound_tile_${preset.name}")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.labelHinglish,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = preset.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrumSoundPickerGrid(
    onPickDrumSound: (DrumSoundType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth().heightIn(max = 280.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DrumSoundType.entries) { sound ->
            val accent = when {
                sound.name.startsWith("FX_") -> StudioViolet
                sound == DrumSoundType.PHONK_COWBELL || sound == DrumSoundType.MEMPHIS_HIT -> PhonkCoral
                sound.name.contains("KICK") || sound == DrumSoundType.BASS_808 -> StudioAmber
                else -> NeonCyan
            }
            Surface(
                onClick = { onPickDrumSound(sound) },
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                modifier = Modifier.height(58.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    Text(
                        text = sound.labelHinglish,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun presetVisual(preset: SynthPresetId): Pair<Color, ImageVector> {
    return when (preset.category) {
        "Bass" -> StudioAmber to Icons.Default.GraphicEq
        "Lead" -> NeonCyan to Icons.Default.Bolt
        "Pluck", "Bell" -> StudioViolet to Icons.Default.AutoAwesome
        "Phonk" -> PhonkCoral to Icons.Default.Whatshot
        "Pad" -> Color(0xFF40C4FF) to Icons.Default.Waves
        else -> Color(0xFF00E676) to Icons.Default.Piano
    }
}
