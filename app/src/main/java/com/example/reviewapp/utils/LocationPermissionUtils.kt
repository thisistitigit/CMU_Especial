package com.example.reviewapp.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Stable
data class LocationPermissionState(
    val isGranted: Boolean,
    val showRationale: Boolean,
    val ask: () -> Unit,
    val openSettings: () -> Unit
)

@Composable
fun rememberLocationPermissionState(
    autoRequestOnStart: Boolean = true,
    onGranted: () -> Unit = {}
): LocationPermissionState {
    val context = LocalContext.current
    val activity = context as? Activity

    fun isGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    var granted by rememberSaveable { mutableStateOf(isGranted()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        granted = (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) onGranted()
    }

    val canAskAgain = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
    } ?: false

    LaunchedEffect(Unit) {
        granted = isGranted()
        if (!granted && autoRequestOnStart && canAskAgain) {
            launcher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else if (granted) {
            onGranted()
        }
    }

    val ask: () -> Unit = ask@{
        if (isGranted()) return@ask  // <- early return correto

        if (canAskAgain) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Negado com "Não voltar a perguntar" → abrir definições
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
            context.startActivity(intent)
        }
    }


    val openSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null))
        context.startActivity(intent)
    }

    return remember(granted, canAskAgain) {
        LocationPermissionState(
            isGranted = granted,
            showRationale = !granted && canAskAgain,
            ask = ask,
            openSettings = openSettings
        )
    }
}

