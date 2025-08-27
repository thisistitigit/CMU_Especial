package com.example.cmu_especial.ui.screens.review

import android.Manifest
import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Solicita e devolve se a permissão da câmara foi concedida.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissions(ask: Boolean = true): Boolean {
    val camera = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (ask && !camera.status.isGranted) camera.launchPermissionRequest()
    }
    return camera.status.isGranted
}
