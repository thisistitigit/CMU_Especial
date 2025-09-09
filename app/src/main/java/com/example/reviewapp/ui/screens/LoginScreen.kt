package com.example.reviewapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.AuthViewModel
/**
 *
 * **Ecrã de Início de Sessão.**
 *
 * Autentica um utilizador via AuthViewModel.login(email, password). Apresenta feedback
 * de erro localizável e bloqueia ações enquanto o pedido está em curso.
 *
 * ### Estrutura:
 *  - Logótipo + título.
 *  - OutlinedTextField para e-mail e palavra-passe (com alternância de visibilidade).
 *  - Botão “Iniciar sessão” com estado loading.
 *  - Link para “Criar conta”.
 *  - Interações/Side effects:
 *  - onLoginSuccess() é chamado quando o login conclui com sucesso.
 *  - onGoRegister() navega para o ecrã de registo.
 *  - Validações:
 *  - Ativa o botão apenas se e-  - mail não estiver vazio e palavra-passe tiver ≥ 6 chars.
 *
 * @param vm AuthViewModel para operações de autenticação (Hilt).
 * @param onLoginSuccess callback após sucesso.
 * @param onGoRegister callback para navegar para registo.
 *
 * **/
@Composable
fun LoginScreen(
    vm: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onGoRegister: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun doLogin() {
        if (loading) return
        loading = true; errorMsg = null
        vm.login(email, pass) { ok, err ->
            loading = false
            if (ok) onLoginSuccess() else {
                errorMsg = err ?: context.getString(R.string.error_login_failed)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.auth_title_login),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(20.dp))

                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            label = { Text(stringResource(R.string.field_email)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pass, onValueChange = { pass = it },
                            label = { Text(stringResource(R.string.field_password)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPass = !showPass }) {
                                    Icon(
                                        if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { doLogin() }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMsg != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                errorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { doLogin() },
                            enabled = !loading && email.isNotBlank() && pass.length >= 6,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (loading) stringResource(R.string.status_signing_in)
                                else stringResource(R.string.action_sign_in)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onGoRegister) {
                    Text(stringResource(R.string.action_create_account))
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
