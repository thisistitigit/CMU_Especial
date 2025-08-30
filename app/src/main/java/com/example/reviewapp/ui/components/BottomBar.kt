// com/example/reviewapp/ui/components/BottomBar.kt
package com.example.reviewapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.reviewapp.R
import com.example.reviewapp.navigation.Route

data class BottomItem(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomItems = listOf(
    BottomItem(Route.Home.route,         R.string.bottom_home,        Icons.Filled.Home),
    BottomItem(Route.Search.route,       R.string.bottom_search,      Icons.Filled.Search),
    BottomItem(Route.Leaderboard.route,  R.string.bottom_leaderboard, Icons.Filled.Leaderboard),
    BottomItem(Route.History.route,      R.string.bottom_history,     Icons.Filled.History),
    BottomItem(Route.Profile.route,      R.string.bottom_profile,     Icons.Filled.Person)
)

@Composable
fun BottomBar(nav: NavHostController, currentDestination: NavDestination?) {
    NavigationBar {
        bottomItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            val label = stringResource(item.labelRes)

            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(item.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = stringResource(R.string.cd_bottom_item, label)) },
                label = { Text(label) }
            )
        }
    }
}
