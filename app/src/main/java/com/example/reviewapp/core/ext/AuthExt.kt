package com.example.reviewapp.core.ext

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Extensões utilitárias para autenticação com Firebase.
 *
 * Estas helpers são usadas como _guards_ na UI/VM para garantir que
 * operações sensíveis (p.ex. publicar reviews) apenas ocorrem com sessão iniciada.
 *
 * **Thread-safety:** apenas lê estado de [FirebaseAuth.currentUser].
 * Não realiza I/O nem chamadas de rede.
 *
 * @since 1.0
 */

/** @return `true` se existir um utilizador autenticado, `false` caso contrário. */
fun FirebaseAuth.isSignedIn(): Boolean = currentUser != null

/**
 * Obtém o UID do utilizador autenticado ou lança exceção caso não exista sessão.
 *
 * Útil para funções que **exigem** contexto autenticado e não querem lidar
 * com `nullables`.
 *
 * @return UID do utilizador autenticado.
 * @throws IllegalStateException se não existir sessão ativa.
 */
fun FirebaseAuth.requireSignedInUid(): String =
    currentUser?.uid
        ?: throw IllegalStateException("É necessário iniciar sessão para publicar reviews.")
