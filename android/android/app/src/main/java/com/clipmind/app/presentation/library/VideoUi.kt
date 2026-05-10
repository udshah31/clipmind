package com.clipmind.app.presentation.library

import com.clipmind.app.domain.model.UploadStatus
import com.clipmind.app.domain.model.Video
import java.util.concurrent.TimeUnit

data class VideoUi(
    val id: Long,
    val title: String,
    val uri: String,
    val formattedDuration: String,
    val formattedSize: String,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val uploadProgress: Float = 0f,
)

fun Video.toVideoUi(): VideoUi = VideoUi(
    id = id,
    title = title,
    uri = uri,
    formattedDuration = formatDuration(durationMs),
    formattedSize = formatSize(sizeBytes),
    uploadStatus = uploadStatus,
    uploadProgress = uploadProgress,
)

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "—"
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) "%.1f GB".format(mb / 1024) else "%.1f MB".format(mb)
}
