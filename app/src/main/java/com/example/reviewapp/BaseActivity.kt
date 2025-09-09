package com.example.reviewapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * ## [BaseActivity]
 *
 * Atividade base anotada com [AndroidEntryPoint] (Hilt) que centraliza
 * lógica transversal de arranque da aplicação ao nível da _Activity_,
 * nomeadamente a **verificação/solicitação de permissões de notificações**
 * em Android 13+ (Tiramisu, API 33).
 *
 * Esta classe destina-se a ser herdada por atividades concretas (ex.: [MainActivity]),
 * garantindo um _baseline_ consistente de permissões logo no `onCreate`.
 *
 * ### Racional
 * - O pedido de `POST_NOTIFICATIONS` deve ocorrer antes de funcionalidades que
 *   apresentem _toasts_ persistentes/notifications (geofences, _workers_).
 * - Usa a API moderna de resultados (`registerForActivityResult`) para cumprir
 *   _lifecycle awareness_ e evitar _leaks_.
 */
@AndroidEntryPoint
open class BaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermissionIfNeeded()
    }

    /**
     * _Launcher_ do Activity Result API para o pedido de `POST_NOTIFICATIONS`.
     * Não necessita de _callback_ de sucesso explícito no fluxo atual.
     */
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    /**
     * Verifica, em Android 13+, se a permissão de notificações foi concedida
     * e, caso não, dispara o pedido assíncrono.
     *
     * - Em Android ≤ 12, a permissão é implicitamente concedida pelo sistema
     *   (não aplicável), logo o método termina sem efeitos.
     */
    private fun checkNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val has = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!has) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
