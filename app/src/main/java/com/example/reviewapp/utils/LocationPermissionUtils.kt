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

/**
 * Estado estável para gerir permissões de **localização** (fine/coarse) em Compose.
 *
 * Exposto como um objeto imutável com *callbacks* para pedir permissões e abrir
 * as definições da app, permitindo **desacoplar** UI de lógica de permissões.
 *
 * @property isGranted se a app tem pelo menos uma das permissões `FINE` ou `COARSE`.
 * @property showRationale indica se deve ser apresentado racional ao utilizador.
 * @property ask dispara o pedido de permissões (ou abre settings se não é possível pedir).
 * @property openSettings abre as definições da app para o utilizador ajustar permissões.
 */
@Stable
data class LocationPermissionState(
    val isGranted: Boolean,
    val showRationale: Boolean,
    val ask: () -> Unit,
    val openSettings: () -> Unit
)

/**
 * *Hook* Compose que cria e mantém um [LocationPermissionState].
 *
 * Estratégia:
 * - Verifica `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION`;
 * - Se `autoRequestOnStart` e o sistema permitir, pede logo as permissões;
 * - Invoca [onGranted] assim que uma das permissões é concedida.
 *
 * @param autoRequestOnStart se `true`, tenta pedir permissões no primeiro frame.
 * @param onGranted callback invocado quando a permissão é concedida.
 */
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
        if (isGranted()) return@ask

        if (canAskAgain) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
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
