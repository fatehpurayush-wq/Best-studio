package com.beatstudio.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream

object FileUtils {

    fun getSamplesDir(context: Context): File {
        return File(context.filesDir, "samples").apply { mkdirs() }
    }

    fun getVocalsDir(context: Context): File {
        return File(context.filesDir, "vocals").apply { mkdirs() }
    }

    fun getExportsCacheDir(context: Context): File {
        return File(context.cacheDir, "exports").apply { mkdirs() }
    }

    fun hasEnoughStorage(context: Context, requiredBytes: Long = 20L * 1024L * 1024L): Boolean {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBytes >= requiredBytes
        } catch (_: Exception) {
            true
        }
    }

    fun getDisplayName(context: Context, uri: Uri): String {
        var name = "audio_file"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx) ?: name
            }
        }
        return name
    }

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "beat_studio" }
    }

    fun saveExportToMusicFolder(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/BeatStudio")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
