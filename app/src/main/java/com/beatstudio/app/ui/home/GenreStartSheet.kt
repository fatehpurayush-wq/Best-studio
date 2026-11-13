package com.beatstudio.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatstudio.app.domain.GenreId
import com.beatstudio.app.ui.components.AppButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreStartSheet(
    initialGenre: GenreId? = null,
    onDismiss: () -> Unit,
    onConfirmCreate: (name: String, genre: GenreId, autoBeat: Boolean) -> Unit
) {
    var selectedGenre by remember { mutableStateOf(initialGenre ?: GenreId.PHONK) }
    var projectName by remember(selectedGenre) { mutableStateOf("${selectedGenre.labelHinglish} Beat") }
    var autoGenerate by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Genre se shuru karo",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Beat ka naam") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_project_name_input")
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 270.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GenreId.entries) { genre ->
                    val isSel = genre == selectedGenre
                    val color = Color(genre.tagColorHex)
                    Surface(
                        onClick = {
                            selectedGenre = genre
                            if (projectName.endsWith(" Beat")) {
                                projectName = "${genre.labelHinglish} Beat"
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) color.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) color else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.height(64.dp).testTag("genre_tile_${genre.name}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = genre.labelHinglish,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) color else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${genre.defaultBpm} BPM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto beat ke saath shuru karein",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Instant drums, bass aur chords ready milenge",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoGenerate,
                    onCheckedChange = { autoGenerate = it },
                    modifier = Modifier.testTag("auto_beat_start_switch")
                )
            }

            AppButton(
                text = if (autoGenerate) "Beat Banao (${selectedGenre.labelHinglish})" else "Khaali Studio Kholo",
                icon = if (autoGenerate) Icons.Default.AutoAwesome else Icons.Default.MusicNote,
                containerColor = Color(selectedGenre.tagColorHex),
                contentColor = Color.Black,
                onClick = { onConfirmCreate(projectName, selectedGenre, autoGenerate) },
                modifier = Modifier.fillMaxWidth(),
                tag = "confirm_create_project_button"
            )
        }
    }
}
