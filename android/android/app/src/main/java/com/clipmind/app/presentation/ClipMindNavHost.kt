package com.clipmind.app.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.clipmind.app.presentation.library.LibraryRoot
import com.clipmind.app.presentation.player.PlayerRoot

@Composable
fun ClipMindNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryRoot(
                onNavigateToPlayer = { videoId -> navController.navigate(PlayerRoute(videoId)) }
            )
        }
        composable<PlayerRoute> {
            PlayerRoot(onBack = { navController.popBackStack() })
        }
    }
}
