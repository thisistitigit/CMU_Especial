package com.example.cmu_especial.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

// ui/navigation/DrawerScaffold.kt
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DrawerScaffold(content: @Composable () -> Unit) {
    ModalNavigationDrawer(
        drawerContent = { /* itens: Home, Leaderboard, History, Settings */ }
    ) {
        Scaffold(topBar = { /* MD3 TopAppBar */ }) { content() }
    }
}
