package com.example.reviewapp.di

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * *Session holder* reativo do UID autenticado.
 *
 * Expõe um [StateFlow] com o UID atual ou `null`. Útil para composições
 * de UI que reagem a login/logout sem *polling*.
 *
 * @since 1.0
 */
@Singleton
class AuthSession @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val _uid = MutableStateFlow(auth.currentUser?.uid)
    /** Fluxo somente-leitura do UID atual. */
    val uid: StateFlow<String?> = _uid.asStateFlow()

    private val listener = FirebaseAuth.AuthStateListener { fb ->
        _uid.value = fb.currentUser?.uid
    }

    init {
        auth.addAuthStateListener(listener)
    }
}
