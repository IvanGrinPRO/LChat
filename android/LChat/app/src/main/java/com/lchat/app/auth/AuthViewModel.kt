package com.lchat.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
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
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Error desconocido") }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        if (!validateRegister(username, email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.signUp(email.trim(), password)
                .onSuccess { user ->
                    try {
                        repository.updateUsername(user.uid, username.trim())
                    } catch (_: Exception) {
                    }
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Error desconocido") }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
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
        if (username.contains(" ")) {
            _uiState.value = AuthUiState.Error("Username sin espacios")
            return false
        }
        return validateLogin(email, password)
    }
}