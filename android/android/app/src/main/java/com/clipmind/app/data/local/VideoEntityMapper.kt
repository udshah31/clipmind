package com.clipmind.app.data.local

import com.clipmind.app.domain.model.UploadStatus
import com.clipmind.app.domain.model.Video

fun VideoEntity.toVideo(): Video = Video(
    id = id,
    title = title,
    uri = uri,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    addedAt = addedAt,
    uploadStatus = runCatching { UploadStatus.valueOf(uploadStatus) }.getOrDefault(UploadStatus.PENDING),
    uploadProgress = uploadProgress,
    remoteId = remoteId,
)

fun Video.toVideoEntity(): VideoEntity = VideoEntity(
    id = id,
    title = title,
    uri = uri,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    addedAt = addedAt,
    uploadStatus = uploadStatus.name,
    uploadProgress = uploadProgress,
    remoteId = remoteId,
)
