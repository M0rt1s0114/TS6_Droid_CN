package dev.tsdroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "connection") {
        composable("connection") {
            ConnectionScreen(
                onConnected = { navController.navigate("server") },
                onNavigateToAbout = { navController.navigate("about") }
            )
        }
        composable("server") {
            ServerScreen(
                onDisconnected = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate("about") }
            )
        }
        composable("about") {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
