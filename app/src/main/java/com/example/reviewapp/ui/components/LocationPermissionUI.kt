package com.example.reviewapp.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.reviewapp.R

/**
 * *Gate* de permissões de localização (foreground + background).
 *
 * Estratégia:
 * - Verifica permissões **fine/coarse** (foreground).
 * - Em Android Q (29)+ tenta pedir **ACCESS_BACKGROUND_LOCATION** diretamente.
 * - Em Android 11+ recomenda abrir **App Settings** para "Allow all the time".
 *
 * Mostra um `AlertDialog` explicativo quando em falta, com ações contextuais:
 * pedir permissões ou abrir as definições.
 *
 * @param autoShowIfNeeded se `true`, mostra o diálogo automaticamente quando faltar permissão.
 * @param onAllGranted callback quando foreground + background estiverem garantidas.
 */
@Composable
fun LocationPermissionGate(
    autoShowIfNeeded: Boolean = true,
    onAllGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    fun hasForeground(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
    fun hasBackground(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    var fgGranted by remember { mutableStateOf(hasForeground(context)) }
    var bgGranted by remember { mutableStateOf(hasBackground(context)) }
    var showModal by remember { mutableStateOf(false) }

    val reqForeground = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        fgGranted = (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        bgGranted = hasBackground(context)
        if (fgGranted && bgGranted) onAllGranted() else showModal = true
    }

    val reqBackgroundQ = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        bgGranted = hasBackground(context)
        if (fgGranted && bgGranted) { showModal = false; onAllGranted() } else showModal = true
    }

    fun openAppSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", ctx.packageName, null))
        ctx.startActivity(intent)
    }

    LaunchedEffect(Unit) {
        fgGranted = hasForeground(context)
        bgGranted = hasBackground(context)
        if (fgGranted && bgGranted) onAllGranted() else if (autoShowIfNeeded) showModal = true
    }

    if (showModal) {
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
        } == true

        AlertDialog(
            onDismissRequest = { showModal = false },
            title = { Text(stringResource(R.string.location_modal_title)) },
            text = {
                Column {
                    when {
                        !fgGranted -> Text(stringResource(R.string.location_modal_body_fg))
                        !bgGranted -> {
                            Text(stringResource(R.string.location_modal_body_bg))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.location_modal_hint_path),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!fgGranted) {
                    TextButton(onClick = {
                        reqForeground.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }) {
                        Text(stringResource(R.string.location_modal_action_allow_while_using))
                    }
                } else if (!bgGranted) {
                    TextButton(onClick = {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            reqBackgroundQ.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            openAppSettings(context)
                        }
                    }) {
                        Text(stringResource(R.string.location_modal_action_allow_always_open_settings))
                    }
                }
            },
            dismissButton = {
                Row {
                    if (!fgGranted && !canAskAgain) {
                        TextButton(onClick = { openAppSettings(context) }) {
                            Text(stringResource(R.string.location_modal_open_settings))
                        }
                    }
                    TextButton(onClick = { showModal = false }) {
                        Text(stringResource(R.string.location_modal_not_now))
                    }
                }
            }
        )
    }
}
