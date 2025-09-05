package com.example.reviewapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.StarSelector
import com.example.reviewapp.viewmodels.AuthViewModel
import com.example.reviewapp.viewmodels.ReviewFormViewModel
import com.example.reviewapp.utils.MediaStoreUtils
import com.example.reviewapp.utils.PermissionUtils
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewFormScreen(
    placeId: String,
    vm: ReviewFormViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),   // <-- usa o AuthViewModel
    onDone: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val uid = authVm.auth.currentUser?.uid
    var username by remember { mutableStateOf<String?>(null) }
    var loadingUser by remember { mutableStateOf(true) }

    // Buscar username real (users/{uid}) e inicializar o VM
    LaunchedEffect(uid, placeId) {
        if (uid == null) {
            loadingUser = false
            return@LaunchedEffect
        }
        try {
            val snap = authVm.db.collection("users").document(uid).get().await()
            val fetched = snap.getString("username")
                ?: authVm.auth.currentUser?.displayName
                ?: "Utilizador"
            username = fetched
            vm.init(placeId = placeId, userId = uid, userName = fetched)
            // enquanto não tens distância real, usa 0.0 para não bloquear as regras locais
            vm.warmupRules(distanceMeters = 0.0)
        } finally {
            loadingUser = false
        }
    }

    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCaptureUri?.let { vm.setPhotoLocalPath(it.toString()) }
        pendingCaptureUri = null
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingCaptureUri?.let { takePicture.launch(it) }
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.setPhotoLocalPath(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            // Estado de autenticação / carregamento de username
            when {
                loadingUser -> {
                    CircularProgressIndicator()
                    return@Column
                }
                uid == null -> {
                    Text(stringResource(R.string.error_not_authenticated), color = MaterialTheme.colorScheme.error)
                    return@Column
                }
                username == null -> {
                    Text(stringResource(R.string.error_generic), color = MaterialTheme.colorScheme.error)
                    return@Column
                }
            }

            // --- PREVIEW DA FOTO ---
            if (!state.photoLocalPath.isNullOrBlank()) {
                AsyncImage(
                    model = state.photoLocalPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.setPhotoLocalPath(null) }) {
                    Text(stringResource(R.string.action_remove_photo))
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.pastryName,
                onValueChange = vm::onPastryChanged,
                label = { Text(stringResource(R.string.field_pastry)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            StarSelector(selected = state.stars, onChange = vm::onStarsChanged)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.comment,
                onValueChange = vm::onCommentChanged,
                label = { Text(stringResource(R.string.field_comment)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row {
                Button(
                    onClick = {
                        val uri = MediaStoreUtils.createImageUri(context)
                        if (uri != null) {
                            pendingCaptureUri = uri
                            if (PermissionUtils.hasCameraPermission(context)) {
                                takePicture.launch(uri)
                            } else {
                                requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                            }
                        }
                    }
                ) { Text(stringResource(R.string.action_take_photo)) }

                Spacer(Modifier.width(12.dp))

                Button(onClick = { pickImage.launch("image/*") }) {
                    Text(stringResource(R.string.action_pick_gallery))
                }
            }

            Spacer(Modifier.height(16.dp))

            val canSubmit = state.canSubmit && state.rulesOk
            Button(
                enabled = canSubmit && !state.isSubmitting,
                onClick = {
                    vm.submit()
                    onDone()
                }
            ) { Text(stringResource(R.string.action_submit)) }

            if (!state.rulesOk) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.rules_not_met),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
