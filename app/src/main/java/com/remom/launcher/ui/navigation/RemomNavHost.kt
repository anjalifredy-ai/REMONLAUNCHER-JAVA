package com.remom.launcher.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.remom.launcher.ui.screens.AccountsScreen
import com.remom.launcher.ui.screens.HomeScreen
import com.remom.launcher.ui.screens.ModsScreen
import com.remom.launcher.ui.screens.ProfilesScreen
import com.remom.launcher.ui.screens.SettingsScreen

sealed class Screen(val route: String, val label: String) {
    data object Home : Screen("home", "Home")
    data object Profiles : Screen("profiles", "Profiles")
    data object Mods : Screen("mods", "Mods")
    data object Accounts : Screen("accounts", "Accounts")
    data object Settings : Screen("settings", "Settings")
}

@Composable
fun RemomNavHost() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home to Icons.Default.Home,
        Screen.Profiles to Icons.Default.SportsEsports,
        Screen.Mods to Icons.Default.Extension,
        Screen.Accounts to Icons.Default.AccountCircle,
        Screen.Settings to Icons.Default.Settings
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // Horizontal layout: Navigation Rail on the left (perfect for landscape)
        NavigationRail(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            items.forEach { (screen, icon) ->
                NavigationRailItem(
                    icon = { Icon(icon, contentDescription = screen.label) },
                    label = { Text(screen.label) },
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.weight(1f)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Profiles.route) { ProfilesScreen() }
            composable(Screen.Mods.route) { ModsScreen() }
            composable(Screen.Accounts.route) { AccountsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
