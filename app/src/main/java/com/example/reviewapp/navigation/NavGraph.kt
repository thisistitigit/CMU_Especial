package com.example.reviewapp.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.BottomBar
import com.example.reviewapp.ui.screens.*
import com.example.reviewapp.viewmodels.AuthViewModel
import com.example.reviewapp.viewmodels.SearchViewModel

/**
 * Definição das rotas da app (Compose Navigation) e do grafo principal.
 *
 * Sealed class [Route] centraliza os paths e `builders` de argumentos.
 * O `Scaffold` ativa a `BottomBar` nas rotas principais.
 *
 * @since 1.0
 */
sealed class Route(val route: String) {
    data object AuthGate   : Route("authGate")
    data object Login      : Route("login")
    data object Register   : Route("register")
    data object Main       : Route("main")

    data object Home       : Route("home")
    data object Search     : Route("search")
    data object Leaderboard: Route("leaderboard")
    data object History    : Route("history")
    data object Profile    : Route("profile")

    /** Ecrã de detalhes de um local. */
    data object Details : Route("details/{placeId}") {
        /** Constrói a rota com `placeId` devidamente `Uri.encode`. */
        fun build(id: String) = "details/${Uri.encode(id)}"
    }

    /** Formulário de review; aceita `lat`/`lng` opcionais como hints. */
    data object ReviewForm : Route("review/{placeId}?lat={lat}&lng={lng}") {
        fun build(id: String) = "review/${Uri.encode(id)}"
        fun build(id: String, lat: Double?, lng: Double?): String {
            val base = "review/${Uri.encode(id)}"
            return if (lat != null && lng != null) "$base?lat=$lat&lng=$lng" else base
        }
    }

    /** Ecrã de detalhes de uma review. */
    data object ReviewDetails : Route("reviewDetails/{reviewId}") {
        fun build(id: String) = "reviewDetails/$id"
    }

    /** Lista de todas as reviews de um local. */
    data object ReviewsAll : Route("reviewsAll/{placeId}") {
        fun build(id: String) = "reviewsAll/$id"
    }
}

/** Conjunto de rotas que exibem a `BottomBar`. */
private val bottomRoutes = setOf(
    Route.Home.route,
    Route.Search.route,
    Route.Leaderboard.route,
    Route.History.route,
    Route.Profile.route
)

/**
 * Grafo de navegação principal da aplicação.
 *
 * Gere **AuthGate → (Login|Main)**, e o subgrafo `Main` com as rotas
 * `Home/Search/Leaderboard/History/Profile` e ecrãs auxiliares.
 */
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
        ) {
            composable(Route.AuthGate.route) {
                val vm: AuthViewModel = hiltViewModel()
                LaunchedEffect(Unit) {
                    val target = if (vm.isLoggedIn()) Route.Main.route else Route.Login.route
                    nav.navigate(target) {
                        popUpTo(Route.AuthGate.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                PlaceholderScreen(stringResource(R.string.authgate_preparing))
            }

            composable(Route.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        nav.navigate(Route.Main.route) {
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
                        nav.navigate(Route.Main.route) {
                            popUpTo(Route.Register.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoLogin = { nav.popBackStack() }
                )
            }

            navigation(
                startDestination = Route.Home.route,
                route = Route.Main.route
            ) {
                composable(Route.Home.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) { nav.getBackStackEntry(Route.Main.route) }
                    val sharedVm: SearchViewModel = hiltViewModel(parentEntry)
                    HomeScreen(
                        vm = sharedVm,
                        onOpenDetails = { id -> nav.navigate(Route.Details.build(id)) },
                    )
                }
                composable(Route.Search.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) { nav.getBackStackEntry(Route.Main.route) }
                    val sharedVm: SearchViewModel = hiltViewModel(parentEntry)
                    SearchScreen(
                        vm = sharedVm,
                        onOpenDetails = { id -> nav.navigate(Route.Details.build(id)) },
                        onOpenProfile = { nav.navigate(Route.Profile.route) }
                    )
                }
                composable(Route.Leaderboard.route) {
                    LeaderboardScreen(
                        onPlaceClick = { id -> nav.navigate(Route.Details.build(id)) }
                    )
                }
                composable(Route.History.route) {
                    PlaceholderScreen(stringResource(R.string.placeholder_history))
                }
                composable(Route.Profile.route) {
                    ProfileScreen(
                        onLogout = {
                            nav.navigate(Route.Login.route) {
                                popUpTo(Route.Main.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = Route.Details.route,
                    arguments = listOf(navArgument("placeId") { type = NavType.StringType })
                ) { back ->
                    val raw = back.arguments?.getString("placeId")
                    val placeId = Uri.decode(raw ?: "").trim()
                    DetailsScreen(
                        placeId = placeId,
                        onBack = { nav.popBackStack() },
                        onReview = { id, lat, lng ->
                            nav.navigate(Route.ReviewForm.build(id, lat, lng))},
                        onOpenReviewDetails = { reviewId -> nav.navigate(Route.ReviewDetails.build(reviewId)) },
                        onOpenAllReviews = { id -> nav.navigate(Route.ReviewsAll.build(id)) }
                    )
                }
                composable(
                    route = Route.ReviewForm.route,
                    arguments = listOf(
                        navArgument("placeId") { type = NavType.StringType },
                        navArgument("lat")     { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("lng")     { type = NavType.StringType; nullable = true; defaultValue = null },
                    )
                ) { back ->
                    val placeId = back.arguments?.getString("placeId").orEmpty()
                    val navLat  = back.arguments?.getString("lat")?.toDoubleOrNull()
                    val navLng  = back.arguments?.getString("lng")?.toDoubleOrNull()

                    ReviewFormScreen(
                        placeId = placeId,
                        navPlaceLat = navLat,
                        navPlaceLng = navLng,
                        onDone = { nav.popBackStack() },
                        onCancel = { nav.popBackStack() }
                    )
                }
                composable(
                    route = Route.ReviewDetails.route,
                    arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
                ) { back ->
                    val reviewId = back.arguments?.getString("reviewId").orEmpty()
                    ReviewDetailScreen(
                        reviewId = reviewId,
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(
                    route = Route.ReviewsAll.route,
                    arguments = listOf(navArgument("placeId") { type = NavType.StringType })
                ) { back ->
                    val placeId = back.arguments?.getString("placeId").orEmpty()
                    AllReviewsScreen(
                        placeId = placeId,
                        onBack = { nav.popBackStack() },
                        onOpenReviewDetails = { reviewId -> nav.navigate(Route.ReviewDetails.build(reviewId)) }
                    )
                }
                composable(Route.History.route) {
                    HistoryScreen(
                        onOpenPlaceDetails = { id -> nav.navigate(Route.Details.build(id)) }
                    )
                }
            }
        }
    }
}

/** Placeholder minimalista para ecrãs em construção. */
@Composable
private fun PlaceholderScreen(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
