// RegisterScreen.kt
package com.example.reviewapp.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun RegisterScreen(
    vm: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
        try {
            val account = task.result
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                error = "Não foi possível obter o token Google."
                return@rememberLauncherForActivityResult
            }
            loading = true; error = null
            vm.signInWithGoogle(idToken) { ok, err ->
                loading = false
                if (ok) onRegisterSuccess() else error = toFriendlyGoogleError(err)
            }
        } catch (e: ApiException) {
            Log.e("Auth", "Google sign-in failed: ${e.statusCode}", e)
            error = when (e.statusCode) {
                10 -> "Configuração inválida (SHA-1/JSON)."
                12501 -> "Login cancelado."
                12500 -> "Falha no login Google. Tenta novamente."
                else -> e.localizedMessage ?: "Falha no login Google."
            }
        } catch (t: Throwable) {
            error = t.localizedMessage ?: "Falha inesperada no Google Sign-In."
        }
    }

    fun startGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.reviewapp.R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleLauncher.launch(client.signInIntent)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Criar conta", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("Palavra-passe") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                TextButton(onClick = { showPass = !showPass }) {
                    Text(if (showPass) "Ocultar" else "Mostrar")
                }
            },
            singleLine = true,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pass2, onValueChange = { pass2 = it },
            label = { Text("Confirmar palavra-passe") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            singleLine = true,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        val passOk = pass.length >= 6 && pass == pass2
        Button(
            onClick = {
                error = null; loading = true
                if (!passOk) { error = "As palavras-passe não coincidem"; loading = false; return@Button }
                vm.register(email, pass) { ok, err ->
                    loading = false
                    if (ok) onRegisterSuccess() else {
                        error = when {
                            (err?.contains("CONFIGURATION_NOT_FOUND", true) == true) ->
                                "Configuração do Firebase inválida. Verifica o google-services.json e o método Email/Password no Console."
                            (err?.contains("OPERATION_NOT_ALLOWED", true) == true) ->
                                "Registo por email está desativado no Firebase. Ativa em Authentication → Sign-in method."
                            else -> err ?: "Falha no registo. Tenta novamente."
                        }
                    }
                }
            },
            enabled = !loading && email.isNotBlank() && passOk,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "A registar..." else "Criar conta") }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { if (!loading) startGoogle() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continuar com Google") }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onGoLogin) { Text("Já tenho conta") }
    }
}

private fun toFriendlyGoogleError(raw: String?): String {
    val s = raw?.lowercase() ?: return "Falha no login Google."
    return when {
        "config" in s || "client" in s || "developer error" in s -> "Configuração inválida (SHA-1/JSON)."
        "network" in s -> "Sem ligação. Tenta novamente."
        else -> raw ?: "Falha no login Google."
    }
}
