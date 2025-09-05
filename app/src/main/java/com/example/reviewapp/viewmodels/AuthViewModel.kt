package com.example.reviewapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val createdAt: Any? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseFirestore
) : ViewModel() {

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

    /** Registo com username único + criação no Firestore */
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
            onResult(false, "invalid_username")
            return
        }

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { createTask ->
                val err = createTask.exception
                if (!createTask.isSuccessful) {
                    if (err is FirebaseAuthException) {
                        Log.e("Auth", "register failed: ${err.errorCode} - ${err.message}")
                    }
                    onResult(false, "register_failed")
                    return@addOnCompleteListener
                }

                val uid = auth.currentUser?.uid ?: run {
                    onResult(false, "not_authenticated_after_register")
                    return@addOnCompleteListener
                }

                // Garante token fresco
                auth.currentUser?.getIdToken(true)
                    ?.addOnSuccessListener {
                        val unameRef = db.collection("usernames").document(usernameLower)
                        val userRef  = db.collection("users").document(uid)

                        // 1) Reserva do username (create se não existir; se existir → dá PERMISSION_DENIED por update)
                        unameRef.set(
                            mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp())
                        ).addOnSuccessListener {
                            // 2) Criar ou completar o doc do utilizador
                            userRef.get().addOnSuccessListener { snap ->
                                if (snap.exists()) {
                                    // Primeiro preenchimento (apenas estes 2 campos, como as regras exigem)
                                    userRef.update(
                                        mapOf(
                                            "username" to username,
                                            "usernameLower" to usernameLower
                                        )
                                    ).addOnSuccessListener {
                                        auth.currentUser?.updateProfile(
                                            UserProfileChangeRequest.Builder().setDisplayName(username).build()
                                        )
                                        onResult(true, null)
                                    }.addOnFailureListener { e ->
                                        Log.e("Auth", "users.update failed", e)
                                        onResult(false, "profile_create_failed")
                                    }
                                } else {
                                    userRef.set(
                                        mapOf(
                                            "uid" to uid,
                                            "email" to email.trim(),
                                            "username" to username,
                                            "usernameLower" to usernameLower,
                                            "createdAt" to FieldValue.serverTimestamp()
                                        )
                                    ).addOnSuccessListener {
                                        auth.currentUser?.updateProfile(
                                            UserProfileChangeRequest.Builder().setDisplayName(username).build()
                                        )
                                        onResult(true, null)
                                    }.addOnFailureListener { e ->
                                        Log.e("Auth", "users.set failed", e)
                                        onResult(false, "profile_create_failed")
                                    }
                                }
                            }.addOnFailureListener { e ->
                                Log.e("Auth", "users.get failed", e)
                                onResult(false, "profile_create_failed")
                            }
                        }.addOnFailureListener { t ->
                            // Se o /usernames/{usernameLower} já existir, as regras só permitem CREATE,
                            // por isso o set vira UPDATE -> PERMISSION_DENIED. Tratamos como username_taken.
                            val fse = t as? com.google.firebase.firestore.FirebaseFirestoreException
                            Log.e("Auth", "reserve username failed", t)
                            if (fse?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                                t.message?.contains("PERMISSION_DENIED", true) == true
                            ) {
                                onResult(false, "username_taken")
                            } else {
                                onResult(false, "profile_create_failed")
                            }
                        }
                    }
                    ?.addOnFailureListener { tokErr ->
                        Log.e("Auth", "token refresh failed", tokErr)
                        onResult(false, "profile_create_failed")
                    }
            }
    }


    fun signOut() = auth.signOut()
    fun isLoggedIn(): Boolean = auth.currentUser != null
}
