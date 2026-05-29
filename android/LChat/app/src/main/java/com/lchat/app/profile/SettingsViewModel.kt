package com.lchat.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.lchat.app.auth.AuthRepository
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

sealed interface ChangePasswordState {
    data object Idle : ChangePasswordState
    data object Loading : ChangePasswordState
    data object Success : ChangePasswordState
    data class Error(val message: String) : ChangePasswordState
}

class SettingsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Idle)
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank()) {
            _changePasswordState.value = ChangePasswordState.Error("Ingresa tu contraseña actual")
            return
        }
        if (newPassword.length < 6) {
            _changePasswordState.value = ChangePasswordState.Error("Mínimo 6 caracteres")
            return
        }
        if (currentPassword == newPassword) {
            _changePasswordState.value = ChangePasswordState.Error("La nueva contraseña debe ser diferente")
            return
        }
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState.Loading
            authRepository.changePassword(currentPassword, newPassword)
                .onSuccess { _changePasswordState.value = ChangePasswordState.Success }
                .onFailure {
                    val msg = when {
                        it.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ||
                                it.message?.contains("wrong-password") == true ||
                                it.message?.contains("WRONG_PASSWORD") == true -> "Contraseña actual incorrecta"
                        it.message?.contains("WEAK_PASSWORD") == true -> "Contraseña muy débil"
                        else -> it.message ?: "Error al cambiar contraseña"
                    }
                    _changePasswordState.value = ChangePasswordState.Error(msg)
                }
        }
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordState.Idle
    }

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
                try {
                    auth.currentUser?.delete()?.await()
                } catch (_: Exception) {
                    auth.signOut()
                }

                _uiState.value = SettingsUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Error al eliminar cuenta")
            }
        }
    }
}