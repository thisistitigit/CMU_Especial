package com.example.reviewapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.reviewapp.ui.components.BottomBar
import com.example.reviewapp.ui.screens.*
import com.example.reviewapp.viewmodels.AuthViewModel
import androidx.navigation.NavHostController

sealed class Route(val route: String) {
    data object AuthGate   : Route("authGate")
    data object Login      : Route("login")
    data object Register   : Route("register")
    data object Profile    : Route("profile")
    data object Search     : Route("search")
    data object Details    : Route("details/{placeId}") { fun build(id: String) = "details/$id" }
    data object ReviewForm : Route("review/{placeId}") { fun build(id: String) = "review/$id" }
    data object Leaderboard: Route("leaderboard")
    data object History    : Route("history")
}

private val bottomRoutes = setOf(
    Route.Search.route,
    Route.Leaderboard.route,
    Route.History.route,
    Route.Profile.route
)

@Composable
fun AppNavGraph(nav: NavHostController) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val currentRoute = currentDest?.route
    val showBottom = currentRoute in bottomRoutes

    Scaffold(
        bottomBar = { if (showBottom) BottomBar(nav, currentDest) }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Route.AuthGate.route,
            modifier = Modifier.padding(innerPadding)
        )  {
            // Gate decide login ou home
            composable(Route.AuthGate.route) {
                val vm: AuthViewModel = hiltViewModel()
                LaunchedEffect(Unit) {
                    val target = if (vm.isLoggedIn()) Route.Search.route else Route.Login.route
                    nav.navigate(target) {
                        popUpTo(Route.AuthGate.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("A preparar…")
                }
            }

            // Auth
            composable(Route.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        nav.navigate(Route.Search.route) {
                            popUpTo(Route.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoRegister = { nav.navigate(Route.Register.route) }
                )
            }
            composable(Route.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        nav.navigate(Route.Search.route) {
                            popUpTo(Route.Register.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoLogin = { nav.popBackStack() }
                )
            }

            // Tabs (mostram bottom bar)
            composable(Route.Search.route) {
                SearchScreen(
                    onOpenDetails = { placeId -> nav.navigate(Route.Details.build(placeId)) },
                    onOpenReview  = { placeId -> nav.navigate(Route.ReviewForm.build(placeId)) },
                    onOpenProfile = { nav.navigate(Route.Profile.route) }
                )
            }
            composable(Route.Leaderboard.route) {
                // TODO: substitui pelo teu ecrã real
                PlaceholderScreen("Top locais (Leaderboard)")
            }
            composable(Route.History.route) {
                // TODO: substitui pelo teu ecrã real
                PlaceholderScreen("O meu histórico")
            }
            composable(Route.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        nav.navigate(Route.Login.route) {
                            popUpTo(Route.Search.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Sem bottom bar
            composable(
                route = Route.Details.route,
                arguments = listOf(navArgument("placeId") { type = NavType.StringType })
            ) { back ->
                val placeId = back.arguments?.getString("placeId").orEmpty()
                DetailsScreen(
                    placeId = placeId,
                    onBack  = { nav.popBackStack() },
                    onReview= { nav.navigate(Route.ReviewForm.build(placeId)) }
                )
            }
            composable(
                route = Route.ReviewForm.route,
                arguments = listOf(navArgument("placeId") { type = NavType.StringType })
            ) { back ->
                val placeId = back.arguments?.getString("placeId").orEmpty()
                ReviewFormScreen(
                    placeId = placeId,
                    onDone  = { nav.popBackStack() },
                    onCancel= { nav.popBackStack() }
                )
            }
        }
    }
}

/** Placeholder simples para Leaderboard/History enquanto não tens os ecrãs reais */
@Composable
private fun PlaceholderScreen(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
