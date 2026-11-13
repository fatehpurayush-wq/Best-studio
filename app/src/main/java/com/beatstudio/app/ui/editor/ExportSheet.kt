package com.beatstudio.app.ui.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.beatstudio.app.export.ExportFormat
import com.beatstudio.app.export.ExportUiState
import com.beatstudio.app.ui.components.AppButton
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioEmerald
import com.beatstudio.app.ui.theme.StudioObsidian
import com.beatstudio.app.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    exportState: ExportUiState,
    onDismiss: () -> Unit,
    onStartExport: (ExportFormat, Boolean) -> Unit,
    onCancelExport: () -> Unit,
    onClearResult: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.WAV_16) }
    var exportStems by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onStartExport(selectedFormat, exportStems)
    }

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
            Text("Audio & MIDI Export", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Offline fast render — Music/BeatStudio folder mein save hoga",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (exportState.isExporting) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, NeonCyan)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(exportState.statusText, style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { exportState.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = NeonCyan
                        )
                        OutlinedButton(
                            onClick = onCancelExport,
                            modifier = Modifier.fillMaxWidth().testTag("cancel_export_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = PhonkCoral)
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel Export", color = PhonkCoral)
                        }
                    }
                }
            } else if (exportState.completedFiles.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StudioEmerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.5.dp, StudioEmerald)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioEmerald)
                            Text("Export Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StudioEmerald)
                        }
                        Text(exportState.statusText, style = MaterialTheme.typography.bodyMedium)
                        exportState.completedFiles.forEach { fileResult ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fileResult.displayName, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                FilledTonalButton(
                                    onClick = {
                                        val shareUri = FileUtils.getShareUri(context, fileResult.localCacheFile)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = fileResult.mimeType
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Beat Share Karein"))
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        }
                        TextButton(onClick = onClearResult, modifier = Modifier.align(Alignment.End)) {
                            Text("Naya Export Karein")
                        }
                    }
                }
            } else {
                exportState.errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PhonkCoral.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, PhonkCoral)
                    ) {
                        Text(
                            text = err,
                            color = PhonkCoral,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Format Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { fmt ->
                                val selected = fmt == selectedFormat
                                Surface(
                                    onClick = { selectedFormat = fmt },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) NeonCyan else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.weight(1f).height(54.dp).testTag("export_fmt_${fmt.name}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = fmt.labelHinglish,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) NeonCyan else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedFormat != ExportFormat.MIDI) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Stems Export (Har Track Alag)", fontWeight = FontWeight.Bold)
                            Text("Drums, Bass, Melody alag files banengi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = exportStems, onCheckedChange = { exportStems = it })
                    }
                }

                AppButton(
                    text = "Export Shuru Karein",
                    icon = Icons.Default.FileUpload,
                    containerColor = NeonCyan,
                    contentColor = StudioObsidian,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@AppButton
                            }
                        }
                        onStartExport(selectedFormat, exportStems)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tag = "start_export_button"
                )
            }
        }
    }
}
