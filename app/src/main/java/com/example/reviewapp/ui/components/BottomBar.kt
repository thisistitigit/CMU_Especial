package com.example.reviewapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.reviewapp.navigation.Route

data class BottomItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomItems = listOf(
    BottomItem(Route.Search.route,      "Explorar", Icons.Filled.Search),
    BottomItem(Route.Leaderboard.route, "Top",      Icons.Filled.Leaderboard),
    BottomItem(Route.History.route,     "Histórico",Icons.Filled.History),
    BottomItem(Route.Profile.route,     "Perfil",   Icons.Filled.Person)
)

@Composable
fun BottomBar(nav: NavHostController, currentDestination: NavDestination?) {
    NavigationBar {
        bottomItems.forEach { item ->
            val selected = currentDestination
                ?.hierarchy
                ?.any { it.route == item.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(item.route) {
                        // volta ao start e preserva estado dos tabs
                        popUpTo(nav.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
