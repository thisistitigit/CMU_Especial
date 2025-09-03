package com.example.reviewapp.core.ext

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await


/** Conveniente para guards na UI. */
fun FirebaseAuth.isSignedIn(): Boolean = currentUser != null
fun FirebaseAuth.requireSignedInUid(): String =
    currentUser?.uid
        ?: throw IllegalStateException("É necessário iniciar sessão para publicar reviews.")
