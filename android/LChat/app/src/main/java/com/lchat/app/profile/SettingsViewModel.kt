package com.lchat.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface SettingsUiState {
    data object Idle : SettingsUiState
    data object Loading : SettingsUiState
    data object Deleted : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

class SettingsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun deleteAccount() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                // quitar token FCM
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    firestore.collection("users").document(uid)
                        .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayRemove(token))
                        .await()
                } catch (_: Exception) {}

                // anonimizar el documento de usuario
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "username" to "Deleted Account",
                        "usernameLower" to "deleted account",
                        "email" to "",
                        "avatarUrl" to null,
                        "isOnline" to false,
                        "isDeleted" to true,
                        "fcmTokens" to emptyList<String>()
                    )
                ).await()

                // eliminar usuario de Auth
                auth.currentUser?.delete()?.await()

                _uiState.value = SettingsUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Error al eliminar cuenta")
            }
        }
    }
}