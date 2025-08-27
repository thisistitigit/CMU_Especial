package com.example.cmu_especial.services

import android.content.Context
import jakarta.inject.Inject

// pouca bateria/luz
class SensorsMonitor @Inject constructor(ctx: Context) {
    // observa bateria/luz e emite flags para UI/WorkManager adaptar consumo. (requisito: adaptabilidade a eventos do sistema).
}