package com.beatstudio.app.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.beatstudio.app.R
import com.beatstudio.app.domain.Project
import com.beatstudio.app.util.FileUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class ExportUiState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val completedFiles: List<ExportedResultFile> = emptyList(),
    val errorMessage: String? = null
)

data class ExportedResultFile(
    val displayName: String,
    val localCacheFile: File,
    val musicStoreUri: Uri?,
    val mimeType: String
)

class ExportService : Service() {

    companion object {
        private const val CHANNEL_ID = "beat_studio_export"
        private const val NOTIF_ID = 4401
        private const val ACTION_START = "com.beatstudio.app.action.EXPORT_START"
        private const val ACTION_CANCEL = "com.beatstudio.app.action.EXPORT_CANCEL"
        private const val EXTRA_PROJECT_JSON = "extra_project_json"
        private const val EXTRA_FORMAT = "extra_format"
        private const val EXTRA_STEMS = "extra_stems"
        private const val EXTRA_PRO_MODE = "extra_pro_mode"

        private val _exportState = MutableStateFlow(ExportUiState())
        val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

        private val cancelFlag = AtomicBoolean(false)

        fun startExport(
            context: Context,
            project: Project,
            format: ExportFormat,
            exportStems: Boolean,
            isProMode: Boolean
        ) {
            cancelFlag.set(false)
            _exportState.value = ExportUiState(
                isExporting = true,
                progress = 0f,
                statusText = "Export shuru ho raha hai..."
            )
            val json = Json { encodeDefaults = true }.encodeToString(Project.serializer(), project)
            val intent = Intent(context, ExportService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROJECT_JSON, json)
                putExtra(EXTRA_FORMAT, format.name)
                putExtra(EXTRA_STEMS, exportStems)
                putExtra(EXTRA_PRO_MODE, isProMode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelExport(context: Context) {
            cancelFlag.set(true)
            val intent = Intent(context, ExportService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }

        fun clearResult() {
            _exportState.value = ExportUiState()
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelFlag.set(true)
                activeJob?.cancel()
                _exportState.value = ExportUiState(errorMessage = "Export cancel kar diya gaya.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START -> {
                val projectJson = intent.getStringExtra(EXTRA_PROJECT_JSON) ?: run {
                    stopSelf(); return START_NOT_STICKY
                }
                val formatName = intent.getStringExtra(EXTRA_FORMAT) ?: ExportFormat.WAV_16.name
                val format = runCatching { ExportFormat.valueOf(formatName) }.getOrDefault(ExportFormat.WAV_16)
                val exportStems = intent.getBooleanExtra(EXTRA_STEMS, false)
                val isProMode = intent.getBooleanExtra(EXTRA_PRO_MODE, false)

                startForeground(NOTIF_ID, buildNotification("Beat export ho rahi hai...", 0))

                activeJob?.cancel()
                activeJob = scope.launch {
                    runExportJob(projectJson, format, exportStems, isProMode)
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun runExportJob(
        projectJson: String,
        format: ExportFormat,
        exportStems: Boolean,
        isProMode: Boolean
    ) {
        try {
            if (!FileUtils.hasEnoughStorage(this, 15L * 1024L * 1024L)) {
                _exportState.value = ExportUiState(errorMessage = "Storage kam hai! Export ke liye thodi jagah khaali karein.")
                stopSelfCleanly()
                return
            }

            val project = Json { ignoreUnknownKeys = true }.decodeFromString(Project.serializer(), projectJson)
            val cacheDir = FileUtils.getExportsCacheDir(this)
            val baseName = FileUtils.sanitizeFileName(project.name)
            val results = mutableListOf<ExportedResultFile>()

            if (exportStems && format != ExportFormat.MIDI) {
                val tracks = project.tracks
                tracks.forEachIndexed { idx, track ->
                    if (cancelFlag.get()) return
                    val stemName = "${baseName}_${FileUtils.sanitizeFileName(track.name)}.${format.extension}"
                    val outFile = File(cacheDir, stemName)
                    val ok = OfflineRenderer.renderToFile(
                        project = project,
                        outputFile = outFile,
                        format = format,
                        isProMode = isProMode,
                        soloTrackId = track.id,
                        isCancelled = { cancelFlag.get() },
                        onProgress = { p ->
                            val overall = ((idx + p) / tracks.size.toFloat()).coerceIn(0f, 1f)
                            updateProgress(overall, "Stem ${idx + 1}/${tracks.size}: ${track.name}")
                        }
                    )
                    if (!ok) {
                        outFile.delete()
                        _exportState.value = ExportUiState(errorMessage = "Export cancel ho gaya.")
                        stopSelfCleanly()
                        return
                    }
                    val savedUri = FileUtils.saveExportToMusicFolder(this, outFile, stemName, format.mimeType)
                    results.add(ExportedResultFile(stemName, outFile, savedUri, format.mimeType))
                }
            } else {
                val fileName = "${baseName}_${System.currentTimeMillis() % 10000}.${format.extension}"
                val outFile = File(cacheDir, fileName)
                val ok = OfflineRenderer.renderToFile(
                    project = project,
                    outputFile = outFile,
                    format = format,
                    isProMode = isProMode,
                    soloTrackId = null,
                    isCancelled = { cancelFlag.get() },
                    onProgress = { p ->
                        updateProgress(p, "Render ho raha hai (${(p * 100).toInt()}%)...")
                    }
                )
                if (!ok) {
                    outFile.delete()
                    _exportState.value = ExportUiState(errorMessage = "Export cancel ho gaya.")
                    stopSelfCleanly()
                    return
                }
                val savedUri = if (format != ExportFormat.MIDI) {
                    FileUtils.saveExportToMusicFolder(this, outFile, fileName, format.mimeType)
                } else null
                results.add(ExportedResultFile(fileName, outFile, savedUri, format.mimeType))
            }

            _exportState.value = ExportUiState(
                isExporting = false,
                progress = 1f,
                statusText = "Export complete! Music/BeatStudio folder mein save ho gaya.",
                completedFiles = results
            )
        } catch (_: Exception) {
            _exportState.value = ExportUiState(
                isExporting = false,
                errorMessage = "Export fail ho gaya! Dobara koshish karein."
            )
        } finally {
            stopSelfCleanly()
        }
    }

    private fun updateProgress(progress: Float, label: String) {
        _exportState.value = _exportState.value.copy(
            isExporting = true,
            progress = progress,
            statusText = label
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(label, (progress * 100).toInt()))
    }

    private fun stopSelfCleanly() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.export_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.export_channel_desc)
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String, progressPct: Int): Notification {
        val cancelIntent = Intent(this, ExportService::class.java).apply { action = ACTION_CANCEL }
        val cancelPending = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Beat Studio Export")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progressPct.coerceIn(0, 100), false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
