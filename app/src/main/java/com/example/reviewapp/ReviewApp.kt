package com.example.reviewapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

@HiltAndroidApp
class ReviewApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.setLoggingEnabled(true)
    }
}
