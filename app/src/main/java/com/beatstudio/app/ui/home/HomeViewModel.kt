package com.beatstudio.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatstudio.app.data.db.AppDatabase
import com.beatstudio.app.data.db.ProjectRepository
import com.beatstudio.app.domain.GenreId
import com.beatstudio.app.domain.Project
import com.beatstudio.app.music.GenreTemplates
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProjectRepository(AppDatabase.getInstance(application).projectDao())

    val projects: StateFlow<List<Project>> = repository.projectsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDemoProjectsSeeded()
            _isLoading.value = false
        }
    }

    fun createNewProject(
        name: String,
        genre: GenreId,
        autoGenerate: Boolean = true,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val cleanName = name.trim().ifEmpty { "${genre.labelHinglish} Beat" }
            val project = GenreTemplates.createNewProject(
                id = id,
                name = cleanName,
                genre = genre,
                autoGenerate = autoGenerate
            )
            repository.saveProject(project)
            onCreated(id)
        }
    }

    fun duplicateProject(project: Project) {
        viewModelScope.launch {
            repository.duplicateProject(project)
        }
    }

    fun renameProject(id: String, newName: String) {
        viewModelScope.launch {
            repository.renameProject(id, newName)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }
}
