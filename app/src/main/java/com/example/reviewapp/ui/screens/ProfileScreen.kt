package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.AuthViewModel
import com.example.reviewapp.ui.components.AppHeader
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.ui.components.rememberIsOnline
import com.example.reviewapp.ui.theme.AppTheme
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
    /***
     *
     * **Ecrã de Perfil do utilizador.**
     *
     * Lê o documento users/{uid} do Firestore (SERVER se online, CACHE se offline) e
     *
     * mostra dados básicos (username, e-mail). Inclui ação de terminar sessão.
     *
     * ### Estrutura:
     * AppBar com ação de logout.
     * OfflineBanner para status de rede.
     * Cartão com “Visão geral”, barra de progresso, mensagens de erro/offline sem cache.
     * Carregamento de dados:
     * Reage a mudanças de uid e conectividade (rememberIsOnline()).
     * Usa Source.SERVER quando online, caso contrário Source.CACHE.
     *
     * ### Acessibilidade/UX:
     * Mensagens claras em caso de erro ou ausência de cache.
     * Tipografia consistente para rótulos/valores (ProfileRow).
     *
     * @param authViewModel AuthViewModel (Hilt) com auth e db.
     * @param onLogout callback executado ao clicar no ícone de terminar sessão.
     *
     */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val user = authViewModel.auth.currentUser
    val uid = user?.uid
    val db = authViewModel.db

    val isOnline by rememberIsOnline()
    val context = LocalContext.current

    var username by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasCache by remember { mutableStateOf<Boolean?>(null) }

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
            val snap = if (isOnline) docRef.get(Source.SERVER).await()
            else           docRef.get(Source.CACHE).await()

            hasCache = snap.metadata.isFromCache || !isOnline
            username = if (snap.exists()) snap.getString("username") ?: "" else null
        } catch (e: Exception) {
            if (isOnline) error = e.localizedMessage ?: context.getString(R.string.profile_error_generic)
        } finally {
            loading = false
        }
    }

        Scaffold(
            topBar = {
                AppHeader(
                    title = stringResource(R.string.profile_title),
                    actions = {
                        IconButton(
                            onClick = {
                                authViewModel.signOut() // termina a sessão no Firebase
                                onLogout()              // callback para navegar/logicar após logout
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(R.string.action_logout),
                                tint = AppTheme.colors.logout
                            )
                        }
                    }
                )
            }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OfflineBanner()
            Spacer(Modifier.height(12.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.profile_overview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    when {
                        loading -> {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        else -> {
                            error?.let {
                                Text(
                                    text = "${stringResource(R.string.label_error)} $it",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(8.dp))
                            } ?: run {
                                if (!isOnline && (hasCache == false || username == null)) {
                                    Text(
                                        text = stringResource(R.string.profile_offline_no_cache),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }

                            ProfileRow(
                                label = stringResource(R.string.label_username),
                                value = username ?: stringResource(R.string.placeholder_na)
                            )
                            Spacer(Modifier.height(6.dp))
                            ProfileRow(
                                label = stringResource(R.string.label_email),
                                value = user?.email ?: stringResource(R.string.placeholder_na)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
