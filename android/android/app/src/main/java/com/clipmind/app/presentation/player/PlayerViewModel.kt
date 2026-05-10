package com.clipmind.app.presentation.player

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipmind.app.core.domain.onFailure
import com.clipmind.app.core.domain.onSuccess
import com.clipmind.app.core.presentation.UiText
import com.clipmind.app.core.presentation.toUiText
import com.clipmind.app.domain.repository.VideoRepository
import com.clipmind.app.presentation.library.VideoUi
import com.clipmind.app.presentation.library.toVideoUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class PlayerState(
    val video: VideoUi? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
)

sealed interface PlayerAction

sealed interface PlayerEvent

class PlayerViewModel(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private val _events = Channel<PlayerEvent>()
    val events = _events.receiveAsFlow()

    init {
        val videoId = savedStateHandle.get<Long>("videoId") ?: -1L
        if (videoId <= 0) {
            _state.update { it.copy(isLoading = false, error = UiText.DynamicString("Invalid video")) }
        } else {
            viewModelScope.launch {
                videoRepository.getById(videoId)
                    .onSuccess { video ->
                        _state.update { it.copy(video = video.toVideoUi(), isLoading = false) }
                    }
                    .onFailure { error ->
                        _state.update { it.copy(isLoading = false, error = error.toUiText()) }
                    }
            }
        }
    }

    fun onAction(action: PlayerAction) = Unit
}
