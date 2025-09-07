// ProfileScreen.kt
package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.AuthViewModel
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.ui.components.rememberIsOnline
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val user = authViewModel.auth.currentUser
    val uid = user?.uid
    val db = authViewModel.db
    val context = LocalContext.current

    val isOnline by rememberIsOnline()

    var username by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasCache by remember { mutableStateOf<Boolean?>(null) } // null = desconhecido

    LaunchedEffect(uid, isOnline) {
        if (uid == null) {
            loading = false
            error = context.getString(R.string.error_not_authenticated)
            return@LaunchedEffect
        }

        loading = true
        error = null
        hasCache = null

        val docRef = db.collection("users").document(uid)

        try {
            val snap = if (isOnline) {
                // Online: lê do servidor
                docRef.get(Source.SERVER).await()
            } else {
                // Offline: tenta cache (não lançar erro vermelho)
                docRef.get(Source.CACHE).await()
            }

            hasCache = snap.metadata.isFromCache || !isOnline
            if (snap.exists()) {
                username = snap.getString("username") ?: ""
            } else {
                // Sem documento (ex.: sem cache offline)
                username = null
            }
        } catch (e: Exception) {
            // Só mostrar erro se estamos online; offline sem cache não é "erro" para o utilizador
            if (isOnline) error = e.localizedMessage
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // (Opcional) Banner de offline
        OfflineBanner()

        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        when {
            loading -> CircularProgressIndicator()

            else -> {
                // Erros reais (apenas online)
                error?.let {
                    Text(
                        "${stringResource(R.string.label_error)} $it",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Mensagem neutra quando offline e não há cache
                if (!isOnline && (hasCache == false || username == null)) {
                    Text(
                        text = stringResource(R.string.profile_offline_no_cache),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Text("${stringResource(R.string.label_username)} ${username ?: stringResource(R.string.placeholder_na)}")
                Text("${stringResource(R.string.label_email)} ${user?.email ?: stringResource(R.string.placeholder_na)}")
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            authViewModel.signOut()
            onLogout()
        }) { Text(stringResource(R.string.action_logout)) }
    }
}
