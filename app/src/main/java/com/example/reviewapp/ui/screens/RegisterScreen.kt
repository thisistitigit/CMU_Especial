package com.example.reviewapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.AuthViewModel

    /**
     * **Ecrã de registo.**
     *
     * Cria a conta via AuthViewModel.registerWithUsername(), validações locais:
     * username: regex ^[a-z0-9_]{3,20}$.
     * password: mínimo 6 caracteres e confirmação igual.
     *
     * ### Estrutura:
     *  - Logótipo + título.
     *  - Campos username, email, password, confirm password com supporting text.
     *  - Botão “Criar conta” com estado loading.
     *  - Link "Já tenho conta".
     *  - Erros suportados:
     *  - invalid_username, passwords_mismatch, username_taken,
     *  - register_failed, profile_create_failed, not_authenticated_after_register.
     *
     * ### Side effects:
     *  - onRegisterSuccess() quando o registo conclui com sucesso.
     *  - onGoLogin() para navegar para o login.
     *
     *
     * @param vm AuthViewModel (Hilt).
     * @param onRegisterSuccess callback de sucesso.
     * @param onGoLogin callback para navegar para login.
     *
     */

@Composable
fun RegisterScreen(
    vm: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorCode by remember { mutableStateOf<String?>(null) }

    val usernameRegex = Regex("^[a-z0-9_]{3,20}$")
    val passOk = pass.length >= 6 && pass == pass2
    val usernameOk = usernameRegex.matches(username.trim().lowercase())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
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
                text = stringResource(R.string.auth_title_register),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(20.dp))

            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor   = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text(stringResource(R.string.field_username_unique)) },
                        singleLine = true,
                        supportingText = {
                            if (!usernameOk && username.isNotBlank()) {
                                Text(stringResource(R.string.error_username_invalid))
                            }
                        },
                        isError = username.isNotBlank() && !usernameOk,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text(stringResource(R.string.field_email)) },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pass, onValueChange = { pass = it },
                        label = { Text(stringResource(R.string.field_password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pass2, onValueChange = { pass2 = it },
                        label = { Text(stringResource(R.string.field_confirm_password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        supportingText = {
                            if (pass2.isNotBlank() && pass != pass2) {
                                Text(stringResource(R.string.error_passwords_mismatch))
                            }
                        },
                        isError = pass2.isNotBlank() && pass != pass2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorCode != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (errorCode) {
                                "passwords_mismatch" -> stringResource(R.string.error_passwords_mismatch)
                                "invalid_username" -> stringResource(R.string.error_username_invalid)
                                "username_taken" -> stringResource(R.string.error_username_taken)
                                "register_failed" -> stringResource(R.string.error_register_failed)
                                "profile_create_failed" -> stringResource(R.string.error_profile_create_failed)
                                "not_authenticated_after_register" -> stringResource(R.string.error_not_authenticated_after_register)
                                else -> stringResource(R.string.error_generic)
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            errorCode = null; loading = true
                            if (!passOk) {
                                errorCode = "passwords_mismatch"; loading = false; return@Button
                            }
                            if (!usernameOk) {
                                errorCode = "invalid_username"; loading = false; return@Button
                            }
                            vm.registerWithUsername(email, pass, username) { ok, errCode ->
                                loading = false
                                if (ok) onRegisterSuccess() else errorCode = errCode ?: "register_failed"
                            }
                        },
                        enabled = !loading && email.isNotBlank() && usernameOk && passOk,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (loading) stringResource(R.string.status_registering)
                            else stringResource(R.string.action_create_account)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onGoLogin) {
                Text(stringResource(R.string.action_have_account))
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
