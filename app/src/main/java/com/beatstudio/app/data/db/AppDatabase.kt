package com.beatstudio.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.beatstudio.app.domain.Project
import com.beatstudio.app.music.GenreTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "beat_studio_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ProjectRepository(private val dao: ProjectDao) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val projectsFlow: Flow<List<Project>> = dao.observeAllProjects().map { list ->
        list.mapNotNull { entity ->
            runCatching { json.decodeFromString<Project>(entity.projectJson) }.getOrNull()
        }
    }

    suspend fun ensureDemoProjectsSeeded() = withContext(Dispatchers.IO) {
        if (dao.getProjectCount() == 0) {
            GenreTemplates.builtInDemoProjects().forEach { demo ->
                saveProject(demo)
            }
        }
    }

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        dao.getProjectById(id)?.let { entity ->
            runCatching { json.decodeFromString<Project>(entity.projectJson) }.getOrNull()
        }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        val payload = json.encodeToString(updated)
        dao.upsertProject(
            ProjectEntity(
                id = updated.id,
                name = updated.name,
                genreName = updated.genre.labelHinglish,
                bpm = updated.bpm,
                isDemo = updated.isDemo,
                projectJson = payload,
                createdAt = updated.createdAt,
                updatedAt = updated.updatedAt
            )
        )
    }

    suspend fun duplicateProject(project: Project): Project = withContext(Dispatchers.IO) {
        val copy = project.copy(
            id = UUID.randomUUID().toString(),
            name = "${project.name} (Copy)",
            isDemo = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveProject(copy)
        copy
    }

    suspend fun renameProject(id: String, newName: String) = withContext(Dispatchers.IO) {
        val existing = getProject(id) ?: return@withContext
        saveProject(existing.copy(name = newName.trim().ifEmpty { existing.name }))
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        dao.deleteProjectById(id)
    }
}
