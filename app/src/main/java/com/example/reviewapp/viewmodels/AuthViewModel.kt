package com.example.reviewapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.reviewapp.di.AuthSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Modelo de dados persistido no Firestore para o utilizador da app. */
data class AppUser(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val createdAt: Any? = null
)

/**
 * **VM** de autenticação (FirebaseAuth) + registo de metadados no Firestore.
 *
 * Responsabilidades:
 * - **Login** por email/senha;
 * - **Registo** com `username` único (reservado em `usernames/{usernameLower}`);
 * - Atualiza `displayName` do `FirebaseUser`;
 * - Exposição reativa do `currentUserId` via [AuthSession].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseFirestore,
    session: AuthSession
) : ViewModel() {

    /** UID atual (ou `null`) reativo ao ciclo de autenticação. */
    val currentUserId: StateFlow<String?> = session.uid

    /**
     * Autentica um utilizador existente.
     *
     * @param email credencial.
     * @param password credencial.
     * @param onResult callback com sucesso/erro localizado.
     */
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                val err = task.exception
                if (!task.isSuccessful && err is FirebaseAuthException) {
                    Log.e("Auth", "login failed: ${err.errorCode} - ${err.message}")
                }
                onResult(task.isSuccessful, err?.localizedMessage)
            }
    }

    /**
     * Regista utilizador com email/senha + **username único**.
     *
     * Passos:
     * 1) Cria conta em Auth;
     * 2) Reserva `usernameLower` em `usernames/{usernameLower}` (falha ⇒ ocupado);
     * 3) Cria/atualiza documento `users/{uid}` com metadados;
     * 4) Atualiza `displayName` em Auth.
     *
     * @param email email.
     * @param password password.
     * @param usernameRaw nome de utilizador (validação `^[a-z0-9_]{3,20}$`).
     * @param onResult resultado final (mensagens simbólicas: `username_taken`, `profile_create_failed`, …).
     */
    fun registerWithUsername(
        email: String,
        password: String,
        usernameRaw: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val username = usernameRaw.trim()
        val usernameLower = username.lowercase()

        val re = Regex("^[a-z0-9_]{3,20}$")
        if (!re.matches(usernameLower)) {
            onResult(false, "invalid_username"); return
        }

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { createTask ->
                val err = createTask.exception
                if (!createTask.isSuccessful) {
                    if (err is FirebaseAuthException) {
                        Log.e("Auth", "register failed: ${err.errorCode} - ${err.message}")
                    }
                    onResult(false, "register_failed"); return@addOnCompleteListener
                }

                val uid = auth.currentUser?.uid ?: run {
                    onResult(false, "not_authenticated_after_register"); return@addOnCompleteListener
                }

                // Token force refresh para garantir claims atuais
                auth.currentUser?.getIdToken(true)
                    ?.addOnSuccessListener {
                        val unameRef = db.collection("usernames").document(usernameLower)
                        val userRef  = db.collection("users").document(uid)

                        // Reserva o username (fail ⇒ ocupado / rules)
                        unameRef.set(mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp()))
                            .addOnSuccessListener {
                                // Cria/atualiza o documento do utilizador
                                userRef.get().addOnSuccessListener { snap ->
                                    val write = {
                                        auth.currentUser?.updateProfile(
                                            UserProfileChangeRequest.Builder().setDisplayName(username).build()
                                        )
                                        onResult(true, null)
                                    }
                                    if (snap.exists()) {
                                        userRef.update(
                                            mapOf("username" to username, "usernameLower" to usernameLower)
                                        ).addOnSuccessListener { write() }
                                            .addOnFailureListener { e ->
                                                Log.e("Auth", "users.update failed", e)
                                                onResult(false, "profile_create_failed")
                                            }
                                    } else {
                                        userRef.set(
                                            mapOf(
                                                "uid" to uid, "email" to email.trim(),
                                                "username" to username, "usernameLower" to usernameLower,
                                                "createdAt" to FieldValue.serverTimestamp()
                                            )
                                        ).addOnSuccessListener { write() }
                                            .addOnFailureListener { e ->
                                                Log.e("Auth", "users.set failed", e)
                                                onResult(false, "profile_create_failed")
                                            }
                                    }
                                }.addOnFailureListener {
                                    Log.e("Auth", "users.get failed", it)
                                    onResult(false, "profile_create_failed")
                                }
                            }.addOnFailureListener { t ->
                                Log.e("Auth", "reserve username failed", t)
                                val fse = t as? com.google.firebase.firestore.FirebaseFirestoreException
                                if (fse?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                                    t.message?.contains("PERMISSION_DENIED", true) == true
                                ) onResult(false, "username_taken")
                                else onResult(false, "profile_create_failed")
                            }
                    }
                    ?.addOnFailureListener {
                        Log.e("Auth", "token refresh failed", it)
                        onResult(false, "profile_create_failed")
                    }
            }
    }

    /** Termina sessão do utilizador atual. */
    fun signOut() = auth.signOut()
    /** @return `true` se existe `currentUser`. */
    fun isLoggedIn(): Boolean = auth.currentUser != null
}
