package com.lchat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LChatApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val userRepository by lazy { UserRepository() }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.USE_EMULATOR) {
            connectToEmulators()
        }
        createNotificationChannel()
        setupPresence()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            LChatMessagingService.CHANNEL_ID,
            "Mensajes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de nuevos mensajes"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun connectToEmulators() {
        val host = if (isEmulator()) "10.0.2.2" else BuildConfig.EMULATOR_HOST
        Firebase.auth.useEmulator(host, 9099)
        Firebase.firestore.useEmulator(host, 8080)
        Firebase.storage.useEmulator(host, 9199)
        Firebase.functions.useEmulator(host, 5001)
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.contains("generic")
                || Build.FINGERPRINT.contains("emulator")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator"))
    }

    private fun setupPresence() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    appScope.launch { userRepository.setOnline(true) }
                }

                override fun onStop(owner: LifecycleOwner) {
                    appScope.launch { userRepository.setOnline(false) }
                }
            }
        )
    }
}