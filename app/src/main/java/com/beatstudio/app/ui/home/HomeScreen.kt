package com.beatstudio.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatstudio.app.R
import com.beatstudio.app.domain.GenreId
import com.beatstudio.app.domain.Project
import com.beatstudio.app.music.Scales
import com.beatstudio.app.ui.components.AppButton
import com.beatstudio.app.ui.components.AppCard
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioObsidian
import com.beatstudio.app.util.TimeFormat

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProject: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showGenreSheet by remember { mutableStateOf(false) }
    var preselectedGenre by remember { mutableStateOf<GenreId?>(null) }
    var renameTarget by remember { mutableStateOf<Project?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Banner Header
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero_1794553115554),
                        contentDescription = "Beat Studio desk",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        StudioObsidian.copy(alpha = 0.35f),
                                        StudioObsidian.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NeonCyan.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "100% OFFLINE STUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Beat Studio",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "Apna music banao, bina internet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppButton(
                                text = "Naya beat",
                                icon = Icons.Default.Add,
                                containerColor = NeonCyan,
                                contentColor = StudioObsidian,
                                onClick = {
                                    preselectedGenre = null
                                    showGenreSheet = true
                                },
                                tag = "new_beat_button"
                            )
                        }
                    }
                }
            }

            // "Genre se shuru karo" quick launcher strip
            item(span = { GridItemSpan(2) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Genre se shuru karo",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(GenreId.entries) { genre ->
                            val accent = Color(genre.tagColorHex)
                            Surface(
                                onClick = {
                                    preselectedGenre = genre
                                    showGenreSheet = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = accent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .widthIn(min = 112.dp)
                                    .height(66.dp)
                                    .testTag("quick_genre_${genre.name}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = genre.labelHinglish,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
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
                }
            }

            // Section Title
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mere Projects (${projects.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Demo beats included",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isLoading) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            } else if (projects.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Abhi koi project nahi hai",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Upar 'Naya beat' dabao ya kisi Genre se shuru karo!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onOpen = { onOpenProject(project.id) },
                        onRename = {
                            renameTarget = project
                            renameText = project.name
                        },
                        onDuplicate = { viewModel.duplicateProject(project) },
                        onDelete = { deleteTarget = project }
                    )
                }
            }
        }
    }

    if (showGenreSheet) {
        GenreStartSheet(
            initialGenre = preselectedGenre,
            onDismiss = { showGenreSheet = false },
            onConfirmCreate = { name, genre, autoBeat ->
                showGenreSheet = false
                viewModel.createNewProject(name, genre, autoBeat) { newId ->
                    onOpenProject(newId)
                }
            }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Project ka naam badlo") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Naya naam") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameProject(target.id, renameText)
                    renameTarget = null
                }) { Text("Save karo") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Raho") }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Project delete karein?") },
            text = { Text("'${target.name}' hamesha ke liye delete ho jayega.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProject(target.id)
                    deleteTarget = null
                }) { Text("Delete karo", color = PhonkCoral) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val genreColor = Color(project.genre.tagColorHex)
    var menuExpanded by remember { mutableStateOf(false) }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .testTag("project_card_${project.id}"),
        borderColor = genreColor.copy(alpha = 0.4f),
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = genreColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (project.isDemo) "DEMO • ${project.genre.labelHinglish}" else project.genre.labelHinglish,
                        style = MaterialTheme.typography.labelSmall,
                        color = genreColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Project options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Naam badlo") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = { menuExpanded = false; onDuplicate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete karo", color = PhonkCoral) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = PhonkCoral) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }

            Column {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${project.bpm} BPM • ${Scales.rootName(project.rootNote)} ${project.scale.labelHinglish}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = TimeFormat.formatRelativeTimeHinglish(project.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
