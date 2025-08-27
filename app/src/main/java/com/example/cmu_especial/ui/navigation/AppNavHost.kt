package com.example.cmu_especial.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cmu_especial.domain.model.GeoPoint
import com.example.cmu_especial.ui.screens.details.DetailsScreen
import com.example.cmu_especial.ui.screens.history.HistoryScreen
import com.example.cmu_especial.ui.screens.home.HomeScreen
import com.example.cmu_especial.ui.screens.leaderboard.LeaderboardScreen
import com.example.cmu_especial.ui.screens.review.ReviewScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    startDestination: String = "home"
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Home
        composable("home") {
            HomeScreen(onOpenDetails = { id ->
                navController.navigate("details/$id")
            })
        }

        // Leaderboard
        composable("leaderboard") {
            LeaderboardScreen(onOpenDetails = { id ->
                navController.navigate("details/$id")
            })
        }

        // History
        composable("history") {
            HistoryScreen(onOpenDetails = { id ->
                navController.navigate("details/$id")
            })
        }

        // Details
        composable(
            route = "details/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")!!
            DetailsScreen(establishmentId = id)
        }

        // Review (submeter avaliação)
        composable(
            route = "review/{establishmentId}",
            arguments = listOf(navArgument("establishmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val estId = backStackEntry.arguments?.getString("establishmentId")!!
            ReviewScreen(
                establishmentId = estId,
                onSubmitted = { navController.popBackStack() }
            )
        }
    }
}
