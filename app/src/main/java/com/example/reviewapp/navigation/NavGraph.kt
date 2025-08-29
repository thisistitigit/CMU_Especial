// AppNavGraph.kt
package com.example.reviewapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.reviewapp.ui.screens.DetailsScreen
import com.example.reviewapp.ui.screens.LoginScreen
import com.example.reviewapp.ui.screens.ProfileScreen
import com.example.reviewapp.ui.screens.RegisterScreen
import com.example.reviewapp.ui.screens.ReviewFormScreen
import com.example.reviewapp.ui.screens.SearchScreen
import com.example.reviewapp.viewmodels.AuthViewModel

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

@Composable
fun AppNavGraph(nav: NavHostController) {
    NavHost(
        navController = nav,
        startDestination = Route.AuthGate.route
    ) {
        // Decide para onde ir consoante o estado de auth
        composable(Route.AuthGate.route) {
            val vm: AuthViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                val target = if (vm.isLoggedIn()) Route.Search.route else Route.Login.route
                nav.navigate(target) {
                    // limpa tudo até ao startDestination (authGate) e remove-o também
                    popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            // (opcional) pequeno placeholder visual enquanto navega
        }

        composable(Route.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    nav.navigate(Route.Search.route) {
                        popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
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
                        popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoLogin = {
                    // voltar ao ecrã anterior sem duplicar rotas
                    nav.popBackStack()
                }
            )
        }

        // HOME / SEARCH (existe agora no grafo)
        composable(Route.Search.route) {
            // Adapta as lambdas aos parâmetros reais do teu SearchScreen
            SearchScreen(
                onOpenDetails = { placeId -> nav.navigate(Route.Details.build(placeId)) },
                onOpenReview  = { placeId -> nav.navigate(Route.ReviewForm.build(placeId)) },
                onOpenProfile = { nav.navigate(Route.Profile.route) }
            )
        }

        // DETAILS com argumento obrigatório placeId
        composable(
            route = Route.Details.route,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId").orEmpty()
            DetailsScreen(
                placeId = placeId,
                onBack  = { nav.popBackStack() },
                onReview= { nav.navigate(Route.ReviewForm.build(placeId)) }
            )
        }

        // REVIEW FORM com argumento obrigatório placeId
        composable(
            route = Route.ReviewForm.route,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId").orEmpty()
            ReviewFormScreen(
                placeId = placeId,
                onDone  = { nav.popBackStack() },   // volta ao detalhe ou search
                onCancel= { nav.popBackStack() }
            )
        }

        // PROFILE com logout a limpar o back stack
        composable(Route.Profile.route) {
            ProfileScreen(
                onLogout = {
                    nav.navigate(Route.Login.route) {
                        popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Se precisares, ativa quando tiveres os ecrãs
        // composable(Route.Leaderboard.route) { LeaderboardScreen() }
        // composable(Route.History.route)    { HistoryScreen() }
    }
}
