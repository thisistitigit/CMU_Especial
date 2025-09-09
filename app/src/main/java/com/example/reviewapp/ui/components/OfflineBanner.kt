package com.example.reviewapp.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Lê o estado de conectividade e expõe como `State<Boolean>`.
 *
 * Implementação híbrida:
 * - API 23+: usa `ConnectivityManager` + `NetworkCapabilities` validadas.
 * - Pré-23: _fallback_ em `activeNetworkInfo`.
 *
 * @return `State` que emite `true` quando **online** e `false` quando **offline**.
 */
@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    val cm = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun computeOnlineNow(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val active = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(active) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }

    val onlineState = remember { mutableStateOf(computeOnlineNow()) }

    DisposableEffect(cm) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { onlineState.value = computeOnlineNow() }
            override fun onLost(network: Network) { onlineState.value = computeOnlineNow() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                onlineState.value = computeOnlineNow()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(callback)
        } else {
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, callback)
        }

        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }

    return onlineState
}

/**
 * Banner de **offline** para o topo da UI.
 *
 * Visível apenas quando `rememberIsOnline()` indica offline.
 * Marcado com `contentDescription="offline_banner"` para testes UI.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val isOnline by rememberIsOnline()

    AnimatedVisibility(visible = !isOnline) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = "offline_banner" }
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Icon(Icons.Filled.WifiOff, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        com.example.reviewapp.R.string.offline_banner_message
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
