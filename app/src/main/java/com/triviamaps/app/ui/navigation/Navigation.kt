package com.triviamaps.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.LatLng
import com.triviamaps.app.ui.screens.SplashScreen
import com.triviamaps.app.ui.screens.auth.LoginScreen
import com.triviamaps.app.ui.screens.auth.RegisterScreen
import com.triviamaps.app.ui.screens.leaderboard.LeaderboardScreen
import com.triviamaps.app.ui.screens.map.MapScreen
import com.triviamaps.app.ui.screens.map.MarkersListScreen
import com.triviamaps.app.ui.screens.profile.ProfileScreen
import com.triviamaps.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Map : Screen("map")
    object Leaderboard : Screen("leaderboard")
    object Profile : Screen("profile")
    object MarkersList : Screen("markers_list")
    object Settings : Screen("settings")
}

@Composable
fun TriviaMapNavigation() {
    val navController = rememberNavController()
    var focusLocation by remember { mutableStateOf<LatLng?>(null) }
    var sharedUserLocation by remember { mutableStateOf<LatLng?>(null) }
    var showWelcome by remember { mutableStateOf(0) }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = { isLoggedIn ->
                    val destination = if (isLoggedIn)
                        Screen.Map.route
                    else
                        Screen.Login.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    showWelcome = 2
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    showWelcome = 1
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToMarkersList = { navController.navigate(Screen.MarkersList.route) },
                focusLocation = focusLocation,
                onFocusConsumed = { focusLocation = null },
                onUserLocationUpdated = { sharedUserLocation = it },
                showWelcome = showWelcome,
                onWelcomeDismissed = { showWelcome = 0 }
            )
        }
        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onViewOnMap = { latLng ->
                    focusLocation = latLng
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.MarkersList.route) {
            MarkersListScreen(
                onBack = { navController.popBackStack() },
                onViewOnMap = { latLng ->
                    focusLocation = latLng
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Map.route) { inclusive = false }
                    }
                },
                userLocation = sharedUserLocation
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}