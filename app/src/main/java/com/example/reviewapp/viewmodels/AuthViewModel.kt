// AuthViewModel.kt
package com.example.reviewapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val auth: FirebaseAuth
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

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                val err = task.exception
                if (!task.isSuccessful && err is FirebaseAuthException) {
                    Log.e("Auth", "register failed: ${err.errorCode} - ${err.message}")
                }
                onResult(task.isSuccessful, err?.localizedMessage)
            }
    }

    /** Google Sign-In → Firebase */
    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                val err = task.exception
                if (!task.isSuccessful) {
                    val msg = err?.localizedMessage ?: "Falha no login Google"
                    Log.e("Auth", "google failed: $msg", err)
                }
                onResult(task.isSuccessful, err?.localizedMessage)
            }
    }

    fun signOut() = auth.signOut()
    fun isLoggedIn(): Boolean = auth.currentUser != null
}
