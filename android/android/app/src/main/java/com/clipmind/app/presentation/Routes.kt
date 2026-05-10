package com.clipmind.app.presentation

import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

@Serializable
data class PlayerRoute(val videoId: Long)