package com.example.reviewapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import com.example.reviewapp.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.ui.components.StarSelector
import com.example.reviewapp.viewmodels.ReviewFormViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewFormScreen(
    placeId: String,
    vm: ReviewFormViewModel = hiltViewModel(),
    onDone: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // TODO: substitui por o teu AuthViewModel / estado real de utilizador
    LaunchedEffect(placeId) {
        vm.init(placeId = placeId, userId = "demo-user", userName = "Demo")
        // enquanto não tens localização real, passa 0.0 para desbloquear as regras
        vm.warmupRules(distanceMeters = 0.0)
    }

    var pendingCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }

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
    ) { uri: android.net.Uri? ->
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

            // --- PREVIEW DA FOTO (usa Coil) ---
            if (!state.photoLocalPath.isNullOrBlank()) {
                // add dependency: implementation("io.coil-kt:coil-compose:2.6.0")
                coil.compose.AsyncImage(
                    model = state.photoLocalPath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.setPhotoLocalPath(null) }) {
                    Text("Remover foto")
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
                        val uri = com.example.reviewapp.utils.MediaStoreUtils.createImageUri(context)
                        if (uri != null) {
                            pendingCaptureUri = uri
                            if (com.example.reviewapp.utils.PermissionUtils.hasCameraPermission(context)) {
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
                    onDone() // se preferires, navega só quando isSubmitting voltar a false
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
