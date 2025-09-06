package com.example.reviewapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.utils.ReviewRules
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Se criaste isto no repo, importa-o; caso não, o catch abaixo trata com fallback.
// import com.example.reviewapp.data.repository.ReviewDeniedException

@HiltViewModel
class ReviewFormViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val placeId: String = "",
        val userId: String = "",
        val userName: String = "",
        val pastryName: String = "",
        val stars: Int = 0,
        val comment: String = "",
        val photoLocalPath: String? = null,
        val photoCloudUrl: String? = null,

        // Regras
        val userLat: Double? = null,
        val userLng: Double? = null,
        val distanceMeters: Double? = null,
        val lastReviewAt: Long? = null,
        val rulesOk: Boolean = false,
        val ruleMessage: String? = null,

        // UI
        val canSubmit: Boolean = false,
        val isSubmitting: Boolean = false,
        val isLocLoading: Boolean = false,
        val hasLocationPermission: Boolean = false,
        val isLocationEnabled: Boolean = true,

        )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /* -------- setters básicos -------- */

    fun init(placeId: String, userId: String, userName: String) {
        _state.update { it.copy(placeId = placeId, userId = userId, userName = userName) }
        recompute()
    }

    fun onPastryChanged(value: String) {
        _state.update { it.copy(pastryName = value) }; recompute()
    }

    fun onStarsChanged(value: Int) {
        _state.update { it.copy(stars = value.coerceIn(0, 5)) }; recompute()
    }

    fun onCommentChanged(value: String) {
        _state.update { it.copy(comment = value) }; recompute()
    }

    /** Define localização do utilizador (chamado pelo ecrã quando obtém GPS). */
    fun setUserLocation(lat: Double?, lng: Double?) {
        _state.update { it.copy(userLat = lat, userLng = lng) }
        recompute()
    }

    /** Se calculares a distância no ecrã, envia-a para aqui. */
    fun setDistanceMeters(value: Double?) {
        _state.update { it.copy(distanceMeters = value) }
        recompute()
    }

    /** Pre-carrega o lastReviewAt; NÃO passes 0.0 como distância — usa null até teres GPS. */
    fun warmupRules(distanceMeters: Double?) = viewModelScope.launch {
        val uid = _state.value.userId
        val last = if (uid.isNotBlank()) reviewRepo.lastReviewAtByUser(uid) else null
        _state.update { it.copy(distanceMeters = distanceMeters, lastReviewAt = last) }
        recompute()
    }

    fun setLastReviewAt(value: Long?) {
        _state.update { it.copy(lastReviewAt = value) }
        recompute()
    }

    fun setPhotoLocalPath(path: String?) {
        _state.update { it.copy(photoLocalPath = path) }
    }

    /** SUBMIT: devolve true/false e preenche ruleMessage se chumbar. */
    suspend fun submit(): Boolean {
        val s = _state.value
        val uid = s.userId
        val lat = s.userLat
        val lng = s.userLng
        val now = System.currentTimeMillis()

        // Guardas explícitos com mensagens
        if (uid.isBlank()) {
            _state.update { it.copy(ruleMessage = "Tens de iniciar sessão para avaliar.") }
            return false
        }
        if (lat == null || lng == null) {
            _state.update { it.copy(ruleMessage = "Ativa a localização para avaliar no local.") }
            return false
        }
        if (!s.canSubmit) {
            _state.update { it.copy(ruleMessage = "Preenche todos os campos e escolhe as estrelas.") }
            return false
        }
        if (!s.rulesOk) {
            // já temos a ruleMessage calculada no recompute()
            return false
        }

        _state.update { it.copy(isSubmitting = true, ruleMessage = null) }

        val review = Review(
            id = java.util.UUID.randomUUID().toString(),
            placeId = s.placeId,
            userId = uid,                 // reforçado no repo
            userName = s.userName,
            pastryName = s.pastryName,
            stars = s.stars,
            comment = s.comment,
            photoLocalPath = s.photoLocalPath,
            photoCloudUrl = s.photoCloudUrl,
            createdAt = now               // reforçado no repo
        )

        return try {
            reviewRepo.addReview(review, userLat = lat, userLng = lng, now = now)
            _state.update {
                it.copy(
                    pastryName = "", stars = 0, comment = "",
                    photoLocalPath = null, photoCloudUrl = null,
                    isSubmitting = false, ruleMessage = null
                )
            }
            recompute()
            true
        } catch (e: Exception) {
            // Se usares ReviewDeniedException no repo, dá mensagens específicas:
            val msg = when (e::class.simpleName) {
                "ReviewDeniedException" -> {
                    // tenta extrair a razão se tiveres enum Reason (TOO_FAR/TOO_SOON)
                    when (e.message ?: "") {
                        "TOO_FAR" -> "Estás longe do local (limite ~${ReviewRules.MIN_DISTANCE_METERS.toInt()} m)."
                        "TOO_SOON" -> "Ainda não passaram ${ReviewRules.MIN_INTERVAL_MINUTES} minutos desde a última avaliação."
                        else -> "Não podes avaliar agora."
                    }
                }

                else -> "Não foi possível enviar a avaliação. Tenta novamente."
            }
            _state.update { it.copy(isSubmitting = false, ruleMessage = msg, rulesOk = false) }
            false
        }
    }

    fun setLocLoading(loading: Boolean) {
        _state.update { it.copy(isLocLoading = loading) }
        recompute()
    }
    fun setLocationPermission(has: Boolean) {
        _state.update { it.copy(hasLocationPermission = has) }; recompute()
    }
    fun setLocationEnabled(enabled: Boolean) {
        _state.update { it.copy(isLocationEnabled = enabled) }; recompute()
    }
    /* -------- lógica de regras e validação -------- */
    private fun recompute() {
        val s = _state.value
        val now = System.currentTimeMillis()
        val dist = s.distanceMeters

        val (ok, message) = when {
            !s.hasLocationPermission ->
                false to "Concede permissão de localização para avaliar no local."
            !s.isLocationEnabled ->
                false to "Ativa a localização do dispositivo (GPS)."
            s.isLocLoading ->
                false to "A obter localização…"
            dist == null ->
                false to "A obter localização…"
            dist > ReviewRules.MIN_DISTANCE_METERS ->
                false to "Estás longe do local (≈${dist.toInt()} m; limite ${ReviewRules.MIN_DISTANCE_METERS.toInt()} m)."
            (s.lastReviewAt != null) && (now - s.lastReviewAt < ReviewRules.MIN_INTERVAL_MINUTES * 60_000) -> {
                val faltaMs = (ReviewRules.MIN_INTERVAL_MINUTES * 60_000) - (now - s.lastReviewAt)
                val faltaMin = (faltaMs / 60_000).coerceAtLeast(1)
                false to "Aguarda ~${faltaMin} min para nova avaliação."
            }
            else -> true to null
        }


        val canSubmit = s.pastryName.isNotBlank() && s.stars in 1..5 && s.comment.isNotBlank()
        _state.update { it.copy(rulesOk = ok, canSubmit = canSubmit, ruleMessage = message) }
    }
}
