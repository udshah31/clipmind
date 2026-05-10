package com.clipmind.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uri: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val addedAt: Long,
    val uploadStatus: String = "PENDING",
    val uploadProgress: Float = 0f,
    val remoteId: String? = null,
)
