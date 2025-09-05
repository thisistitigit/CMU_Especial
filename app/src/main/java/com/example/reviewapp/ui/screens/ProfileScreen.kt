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

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val user = authViewModel.auth.currentUser
    val uid = user?.uid
    val db = authViewModel.db
    var context = LocalContext.current

    var username by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        if (uid == null) {
            loading = false
            error = context.getString(R.string.error_not_authenticated)
            return@LaunchedEffect
        }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                username = snap.getString("username") ?: ""
                loading = false
            }
            .addOnFailureListener { e ->
                error = e.localizedMessage
                loading = false
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            error?.let {
                Text("${stringResource(R.string.label_error)} $it", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            Text("${stringResource(R.string.label_username)} ${username ?: stringResource(R.string.placeholder_na)}")
            Text("${stringResource(R.string.label_email)} ${user?.email ?: stringResource(R.string.placeholder_na)}")
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            authViewModel.signOut()
            onLogout()
        }) { Text(stringResource(R.string.action_logout)) }
    }
}
