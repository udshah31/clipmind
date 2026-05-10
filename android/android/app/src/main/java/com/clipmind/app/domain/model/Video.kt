package com.clipmind.app.domain.model

data class Video(
    val id: Long,
    val title: String,
    val uri: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val addedAt: Long,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val uploadProgress: Float = 0f,
    val remoteId: String? = null,
)
