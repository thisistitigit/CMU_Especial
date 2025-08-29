package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.viewmodels.AuthViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val user = authViewModel.auth.currentUser

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Perfil do Utilizador", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Nome: ${user?.displayName ?: "Desconhecido"}")
        Text("Email: ${user?.email ?: "Não disponível"}")
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            authViewModel.signOut()
            onLogout()
        }) { Text("Logout") }
    }
}
