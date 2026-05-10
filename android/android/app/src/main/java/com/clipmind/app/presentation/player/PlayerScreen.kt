package com.clipmind.app.presentation.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.clipmind.app.core.presentation.ObserveAsEvents
import com.clipmind.app.core.presentation.UiText
import com.clipmind.app.presentation.library.VideoUi
import com.clipmind.app.presentation.theme.ClipMindTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerRoot(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { /* handle future events */ }

    PlayerScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerState,
    onAction: (PlayerAction) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.video?.title ?: "Player", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> Unit
                state.error != null -> Text(
                    text = state.error.asString(),
                    color = Color.White,
                )
                state.video != null -> VideoPlayer(uri = Uri.parse(state.video.uri))
            }
        }
    }
}

@Composable
private fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(uri) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
    )
}

@Preview
@Composable
private fun PlayerScreenLoadingPreview() {
    ClipMindTheme {
        PlayerScreen(state = PlayerState(), onAction = {}, onBack = {})
    }
}

@Preview
@Composable
private fun PlayerScreenErrorPreview() {
    ClipMindTheme {
        PlayerScreen(
            state = PlayerState(isLoading = false, error = UiText.DynamicString("Video not found")),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun PlayerScreenReadyPreview() {
    ClipMindTheme {
        PlayerScreen(
            state = PlayerState(
                video = VideoUi(1L, "Sample Video.mp4", "", "1:23", "45.2 MB"),
                isLoading = false,
            ),
            onAction = {},
            onBack = {},
        )
    }
}
