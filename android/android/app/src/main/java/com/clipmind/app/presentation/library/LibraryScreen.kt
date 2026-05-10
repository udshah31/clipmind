package com.clipmind.app.presentation.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.clipmind.app.core.presentation.ObserveAsEvents
import com.clipmind.app.domain.model.UploadStatus
import com.clipmind.app.presentation.theme.ClipMindTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryRoot(
    onNavigateToPlayer: (Long) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pickVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onAction(LibraryAction.ImportVideo(it)) }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LibraryEvent.NavigateToPlayer -> onNavigateToPlayer(event.videoId)
            is LibraryEvent.ShowError -> { /* TODO: show snackbar */ }
        }
    }

    LibraryScreen(
        state = state,
        onAction = { action ->
            when (action) {
                LibraryAction.OpenVideoPicker -> pickVideo.launch(arrayOf("video/*"))
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryState,
    onAction: (LibraryAction) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "ClipMind", fontWeight = FontWeight.SemiBold) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(LibraryAction.OpenVideoPicker) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add video") },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.videos.isEmpty() -> EmptyState(Modifier.padding(padding))
            else -> VideoList(
                videos = state.videos,
                onAction = onAction,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun VideoList(
    videos: List<VideoUi>,
    onAction: (LibraryAction) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = videos, key = { it.id }) { video ->
            VideoCard(video = video, onClick = { onAction(LibraryAction.OnVideoClick(video.id)) })
        }
    }
}

@Composable
private fun VideoCard(video: VideoUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(video.uri))
                            .videoFrameMillis(1000)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${video.formattedDuration} · ${video.formattedSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                UploadBadge(status = video.uploadStatus, modifier = Modifier.padding(start = 8.dp))
            }

            if (video.uploadStatus == UploadStatus.UPLOADING) {
                LinearProgressIndicator(
                    progress = { video.uploadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UploadBadge(status: UploadStatus, modifier: Modifier = Modifier) {
    when (status) {
        UploadStatus.PENDING, UploadStatus.UPLOADING -> Unit
        UploadStatus.UPLOADED -> Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Uploaded",
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(20.dp),
        )
        UploadStatus.FAILED -> Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = "Upload failed",
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.MovieCreation,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))
            Text("No videos yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap “Add video” to import from your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Preview
@Composable
private fun LibraryScreenPreview() {
    ClipMindTheme {
        LibraryScreen(
            state = LibraryState(
                videos = listOf(
                    VideoUi(1L, "Sample Video.mp4", "", "1:23", "45.2 MB", UploadStatus.UPLOADING, 0.6f),
                    VideoUi(2L, "Another Clip.mp4", "", "5:00", "120.0 MB", UploadStatus.UPLOADED, 1f),
                    VideoUi(3L, "Failed.mp4", "", "0:30", "10.0 MB", UploadStatus.FAILED, 0f),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun LibraryScreenEmptyPreview() {
    ClipMindTheme {
        LibraryScreen(state = LibraryState(isLoading = false), onAction = {})
    }
}
