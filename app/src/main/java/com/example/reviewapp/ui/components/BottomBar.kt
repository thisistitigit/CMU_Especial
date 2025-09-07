package com.example.reviewapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.reviewapp.ui.theme.AppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy

data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BottomBar(nav: NavHostController, currentDest: NavDestination?) {
    val items = listOf(
        BottomItem("home",        "Início",   Icons.Filled.Home),
        BottomItem("search",      "Explorar", Icons.Filled.Search),
        BottomItem("leaderboard", "Top",      Icons.Filled.Star),
        BottomItem("history",     "Histórico",Icons.Filled.History),
        BottomItem("profile",     "Perfil",   Icons.Filled.AccountCircle),
    )

    // container com sombra lilás
    Surface(
        color = AppTheme.colors.navContainer,
        shadowElevation = 12.dp,                 // sombra “física” (visível claro)
        tonalElevation  = 0.dp,                  // mantem cor fiel
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = AppTheme.colors.navContainer,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)             // pequena separação da sombra
        ) {
            items.forEach { item ->
                val selected = currentDest?.route == item.route ||
                        currentDest?.hierarchy?.any { it.route == item.route } == true

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        nav.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                        }
                    },
                    icon = {
                        if (selected) {
                            // “bolha” lilás clara com sombra roxa
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .shadow(8.dp, CircleShape, clip = false)
                                    .background(AppTheme.colors.navSelectedBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = AppTheme.colors.navSelectedIcon
                                )
                            }
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        }
                    },
                    label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = AppTheme.colors.navSelectedIcon,
                        selectedTextColor   = MaterialTheme.colorScheme.onSurface,
                        indicatorColor      = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
