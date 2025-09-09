package com.example.reviewapp

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.reviewapp.navigation.AppNavGraph
import com.example.reviewapp.ui.theme.ReviewAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import com.google.android.gms.location.LocationServices
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.example.reviewapp.geofence.LocationUpdateReceiver
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

/**
 * ## [MainActivity]
 *
 * Atividade principal da aplicação, responsável por:
 *
 * 1. **Gestão de _splash screen_:** instala a `SplashScreen` nativa (API 31+) e,
 *    após o _resume_ inicial, aplica de volta o tema normal (`setTheme(...)`) antes
 *    de renderizar a UI Compose para evitar _theme flickering_.
 * 2. **Montagem da UI Compose:** aplica [ReviewAppTheme] e injeta o `NavHost`
 *    ([AppNavGraph]) com um `NavController` próprio.
 * 3. **Semeadura e manutenção de geofences:** invoca [quickSeedGeofences] e
 *    [startLocationUpdatesForGeofences] para registar/atualizar geofences em
 *    _background_ (com Fused Location).
 *
 * Requisitos:
 * - Permissões de localização (_foreground_ e _background_) devem estar concedidas
 *   para as operações de geofencing.
 * - Eventuais exceções de segurança são protegidas com `runCatching`/`try-catch`
 *   para não interromper o _lifecycle_ da atividade.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity() {

    /** Componente responsável por (des)registar geofences com base na posição atual. */
    @Inject lateinit var registrar: com.example.reviewapp.geofence.GeofenceRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) Instalar SplashScreen (Android 12+ gere o resto)
        val splash: SplashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2) Repor o tema normal antes de desenhar Compose (evita flicker após o splash).
        setTheme(R.style.Theme_ReviewApp)

        // 3) Montar UI Compose com navegação.
        setContent { ReviewAppTheme { AppNavGraph(rememberNavController()) } }

        // 4) Geofencing: semente inicial + atualizações periódicas por deslocação.
        quickSeedGeofences()
        startLocationUpdatesForGeofences()
    }

    /**
     * Regista geofences **uma única vez** com a última localização conhecida
     * (seed inicial). Útil para garantir que, mesmo antes do primeiro _fix_ ativo,
     * o conjunto de cercas virtuais já está aproximado à posição do utilizador.
     *
     * Pré-condições:
     * - Permissão de localização (foreground + background) concedida.
     * - Serviços Google Play disponíveis (Fused Location Provider).
     */
    private fun quickSeedGeofences() {
        if (!com.example.reviewapp.utils.PermissionUtils.hasLocationPermission(this)) return
        if (!com.example.reviewapp.utils.PermissionUtils.hasBackgroundLocationPermission(this)) return

        val fused = LocationServices.getFusedLocationProviderClient(this)
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    lifecycleScope.launch {
                        try {
                            registrar.refreshGeofences(
                                this@MainActivity,
                                loc.latitude,
                                loc.longitude
                            )
                        } catch (t: Throwable) {
                            Log.e("Geofence", "Seed geofences falhou: ${t.message}")
                        }
                    }
                }
            }
        } catch (_: SecurityException) { /* Sem permissões no momento da chamada. */ }
    }

    /**
     * Solicita **atualizações de localização de baixo consumo** (_balanced power_) e
     * atualiza o conjunto de geofences quando o utilizador se move ~500 metros.
     *
     * Implementação:
     * - Constrói um [LocationRequest] com prioridade [Priority.PRIORITY_BALANCED_POWER_ACCURACY]
     *   e intervalo de 30 minutos.
     * - Usa `PendingIntent` para enviar _callbacks_ ao [LocationUpdateReceiver] (BroadcastReceiver),
     *   mesmo que a app esteja em _background_.
     *
     * Notas de segurança:
     * - Requer foreground + background location permissions; caso contrário, a chamada é ignorada.
     * - `FLAG_MUTABLE` é necessário em Android 12+ quando o `PendingIntent` é inspecionado pelo
     *   fornecedor de localização.
     */
    private fun startLocationUpdatesForGeofences() {
        if (!com.example.reviewapp.utils.PermissionUtils.hasLocationPermission(this)) return
        if (!com.example.reviewapp.utils.PermissionUtils.hasBackgroundLocationPermission(this)) return

        val fused = LocationServices.getFusedLocationProviderClient(this)
        val req = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30 * 60 * 1000L
        ).setMinUpdateDistanceMeters(500f).build()

        val intent = Intent(this, LocationUpdateReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        try {
            fused.requestLocationUpdates(req, pi)
        } catch (_: SecurityException) { /* Falha controlada se as permissões forem revogadas. */ }
    }
}
