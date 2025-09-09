package com.example.reviewapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.example.reviewapp.R
import com.example.reviewapp.ui.theme.AppTheme

/**
 * Modelo de item da **Bottom Navigation**.
 *
 * @property route rota de navegação (deve existir no grafo).
 * @property label rótulo legível apresentado sob o ícone.
 * @property icon ícone representativo do destino.
 */
data class BottomItem(val route: String, val label: String, val icon: ImageVector)

/**
 * Barra de navegação inferior com realce do item ativo.
 *
 * Comportamento:
 * - Usa navegação com `launchSingleTop` e `restoreState` para preservar estado
 *   de ecrãs ao alternar separadores.
 * - Realce visual do item selecionado com *chip* circular e sombra suave.
 *
 * Acessibilidade:
 * - Cada item possui `contentDescription` baseado no `label`.
 *
 * @param nav controlador de navegação.
 * @param currentDest destino atual (obtido via `currentBackStackEntryAsState`).
 */
@Composable
fun BottomBar(nav: NavHostController, currentDest: NavDestination?) {
    val items = listOf(
        BottomItem("home", stringResource(R.string.bottom_home), Icons.Filled.Home),
        BottomItem("search", stringResource(R.string.bottom_search), Icons.Filled.Search),
        BottomItem("leaderboard", stringResource(R.string.bottom_leaderboard), Icons.Filled.Star),
        BottomItem("history", stringResource(R.string.bottom_history), Icons.Filled.History),
        BottomItem("profile", stringResource(R.string.bottom_profile), Icons.Filled.AccountCircle),
    )

    Surface(
        color = AppTheme.colors.navContainer,
        shadowElevation = 12.dp,
        tonalElevation  = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = AppTheme.colors.navContainer,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
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
