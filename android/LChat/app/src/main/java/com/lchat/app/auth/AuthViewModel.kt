package com.lchat.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean
        get() = repository.currentUser != null

    fun signIn(email: String, password: String) {
        if (!validateLogin(email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.signIn(email.trim(), password)
                .onSuccess {
                    guardarTokenFcm()
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Error desconocido") }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        if (!validateRegister(username, email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            if (userRepository.isUsernameTaken(username.trim())) {
                _uiState.value = AuthUiState.Error("Username вже зайнятий")
                return@launch
            }

            repository.signUp(email.trim(), password)
                .onSuccess { user ->
                    try {
                        repository.updateUsername(user.uid, username.trim())
                    } catch (_: Exception) {
                    }
                    guardarTokenFcm()
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Error desconocido") }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun guardarTokenFcm() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                userRepository.saveFcmToken(token)
            } catch (_: Exception) {}
        }
    }

    private fun validateLogin(email: String, password: String): Boolean {
        if (email.isBlank() || !email.contains("@")) {
            _uiState.value = AuthUiState.Error("Email no valido")
            return false
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Min. 6 caracteres")
            return false
        }
        return true
    }

    private fun validateRegister(username: String, email: String, password: String): Boolean {
        if (username.isBlank() || username.length < 3 || username.length > 30) {
            _uiState.value = AuthUiState.Error("Username: 3-30 caracteres")
            return false
        }
        if (!username.matches(Regex("^[a-zA-Z0-9ñÑ_-]+$"))) {
            _uiState.value = AuthUiState.Error("Username: solo letras, números, ñ, _ y -")
            return false
        }
        if (!username.any { it.isLetter() }) {
            _uiState.value = AuthUiState.Error("Username: mínimo una letra")
            return false
        }
        return validateLogin(email, password)
    }
}