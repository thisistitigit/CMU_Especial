// LoginScreen.kt
package com.example.reviewapp.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    vm: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onGoRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Launcher para Google Sign-In
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
        try {
            val account = task.result  // lança ApiException se falhar
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                error = "Não foi possível obter o token Google."
                return@rememberLauncherForActivityResult
            }
            loading = true; error = null
            vm.signInWithGoogle(idToken) { ok, err ->
                loading = false
                if (ok) onLoginSuccess() else error = toFriendlyGoogleError(err)
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
        // Usa o client ID do JSON gerado (R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.reviewapp.R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        // opcional: client.signOut() para forçar escolha de conta sempre
        googleLauncher.launch(client.signInIntent)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Entrar", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("Palavra-passe") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                TextButton(onClick = { showPass = !showPass }) {
                    Text(if (showPass) "Ocultar" else "Mostrar")
                }
            },
            singleLine = true,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (!loading) doLogin(vm, email, pass, { loading = it }, { error = it }, onLoginSuccess)
            }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { doLogin(vm, email, pass, { loading = it }, { error = it }, onLoginSuccess) },
            enabled = !loading && email.isNotBlank() && pass.length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "A entrar..." else "Entrar") }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { if (!loading) startGoogle() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Entrar com Google") }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onGoRegister) { Text("Criar conta") }
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

private fun doLogin(
    vm: AuthViewModel,
    email: String,
    pass: String,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onSuccess: () -> Unit
) {
    setLoading(true); setError(null)
    vm.login(email, pass) { ok, err ->
        setLoading(false)
        setError(if (ok) null else err ?: "Falha ao entrar")
        if (ok) onSuccess()
    }
}
