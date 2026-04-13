package com.lchat.app

import android.app.Application
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class LChatApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            connectToEmulators()
        }
    }

    private fun connectToEmulators() {
        val host = "10.0.2.2"
        Firebase.auth.useEmulator(host, 9099)
        Firebase.firestore.useEmulator(host, 8080)
        Firebase.storage.useEmulator(host, 9199)
        Firebase.functions.useEmulator(host, 5001)
    }
}