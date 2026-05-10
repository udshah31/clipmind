package com.clipmind.app.presentation.library

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.clipmind.app.core.domain.Result
import com.clipmind.app.core.domain.onFailure
import com.clipmind.app.core.domain.onSuccess
import com.clipmind.app.core.presentation.UiText
import com.clipmind.app.core.presentation.toUiText
import com.clipmind.app.data.worker.UploadWorker
import com.clipmind.app.domain.model.Video
import com.clipmind.app.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@Stable
data class LibraryState(
    val videos: List<VideoUi> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface LibraryAction {
    data object OpenVideoPicker : LibraryAction
    data class ImportVideo(val uri: Uri) : LibraryAction
    data class DeleteVideo(val id: Long) : LibraryAction
    data class OnVideoClick(val videoId: Long) : LibraryAction
}

sealed interface LibraryEvent {
    data class NavigateToPlayer(val videoId: Long) : LibraryEvent
    data class ShowError(val message: UiText) : LibraryEvent
}

class LibraryViewModel(
    private val videoRepository: VideoRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    private val _events = Channel<LibraryEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            videoRepository.observeAll().collect { videos ->
                _state.update { it.copy(videos = videos.map { v -> v.toVideoUi() }, isLoading = false) }
            }
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.ImportVideo -> importVideo(action.uri)
            is LibraryAction.DeleteVideo -> deleteVideo(action.id)
            is LibraryAction.OnVideoClick -> viewModelScope.launch {
                _events.send(LibraryEvent.NavigateToPlayer(action.videoId))
            }
            LibraryAction.OpenVideoPicker -> Unit
        }
    }

    private fun importVideo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val (title, sizeBytes) = queryNameAndSize(appContext.contentResolver, uri)
            val durationMs = withContext(Dispatchers.IO) { readDuration(appContext, uri) }

            when (val result = videoRepository.add(
                Video(
                    id = 0,
                    title = title,
                    uri = uri.toString(),
                    durationMs = durationMs,
                    sizeBytes = sizeBytes,
                    addedAt = System.currentTimeMillis(),
                )
            )) {
                is Result.Success -> enqueueUpload(result.data)
                is Result.Error -> _events.send(LibraryEvent.ShowError(result.error.toUiText()))
            }
        }
    }

    private fun enqueueUpload(videoId: Long) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_VIDEO_ID to videoId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("upload_$videoId")
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "upload_$videoId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun deleteVideo(id: Long) {
        viewModelScope.launch {
            videoRepository.delete(id)
                .onFailure { error -> _events.send(LibraryEvent.ShowError(error.toUiText())) }
        }
    }

    private fun queryNameAndSize(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        val cursor = resolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
            if (it.moveToFirst()) {
                val name = if (nameIdx >= 0) it.getString(nameIdx) else uri.lastPathSegment.orEmpty()
                val size = if (sizeIdx >= 0) it.getLong(sizeIdx) else 0L
                return name to size
            }
        }
        return (uri.lastPathSegment ?: "Untitled") to 0L
    }

    private fun readDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
}
