package com.clipmind.app.core.presentation

import com.clipmind.app.core.domain.DataError

fun DataError.Local.toUiText(): UiText = when (this) {
    DataError.Local.NOT_FOUND -> UiText.DynamicString("Video not found")
    DataError.Local.DISK_FULL -> UiText.DynamicString("Storage is full")
    DataError.Local.UNKNOWN -> UiText.DynamicString("An error occurred")
}