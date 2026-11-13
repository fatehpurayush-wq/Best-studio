package com.beatstudio.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val genreName: String,
    val bpm: Int,
    val isDemo: Boolean,
    val projectJson: String,
    val createdAt: Long,
    val updatedAt: Long
)
