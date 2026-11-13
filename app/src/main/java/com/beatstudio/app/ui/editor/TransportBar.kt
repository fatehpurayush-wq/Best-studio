package com.beatstudio.app.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioObsidian

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransportBar(
    activeTab: EditorTab,
    isPlaying: Boolean,
    isRecording: Boolean,
    onSelectTab: (EditorTab) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleRecord: () -> Unit,
    onShowTooltip: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 3 Simple Bottom Tabs: BEAT, MELODY, MIX
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(StudioObsidian, RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabPill(
                    label = "BEAT",
                    icon = Icons.Default.GridView,
                    selected = activeTab == EditorTab.BEAT,
                    onClick = { onSelectTab(EditorTab.BEAT) },
                    onLongClick = { onShowTooltip("BEAT: Drum step grid aur patterns yahan banao") },
                    modifier = Modifier.weight(1f).testTag("tab_beat")
                )
                TabPill(
                    label = "MELODY",
                    icon = Icons.Default.Piano,
                    selected = activeTab == EditorTab.MELODY,
                    onClick = { onSelectTab(EditorTab.MELODY) },
                    onLongClick = { onShowTooltip("MELODY: Piano roll, chords aur live keyboard") },
                    modifier = Modifier.weight(1f).testTag("tab_melody")
                )
                TabPill(
                    label = "MIX",
                    icon = Icons.Default.Tune,
                    selected = activeTab == EditorTab.MIX,
                    onClick = { onSelectTab(EditorTab.MIX) },
                    onLongClick = { onShowTooltip("MIX: Track volume, pan, effects aur master") },
                    modifier = Modifier.weight(1f).testTag("tab_mix")
                )
            }

            Spacer(Modifier.width(10.dp))

            // Floating Play/Stop & Record buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isRecording) PhonkCoral else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .testTag("record_button")
                        .combinedClickable(
                            onClick = onToggleRecord,
                            onLongClick = { onShowTooltip("Record: Mic voice ya live keyboard notes record karein") }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = "Record",
                            tint = if (isRecording) Color.White else PhonkCoral,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) PhonkCoral else NeonCyan,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .testTag("play_stop_button")
                        .combinedClickable(
                            onClick = onTogglePlay,
                            onLongClick = { onShowTooltip("Play / Stop: Beat sunne ke liye dabao") }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = if (isPlaying) Color.White else StudioObsidian,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) NeonCyan.copy(alpha = 0.22f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
