package com.lchat.app.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val uid = userRepository.currentUid
    private val firestore = Firebase.firestore

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            userRepository.getUserById(uid).collect { _user.value = it }
        }
    }

    fun updateUsername(newUsername: String) {
        val trimmed = newUsername.trim()
        if (trimmed.length < 3 || trimmed.length > 30) {
            _message.value = "Username: 3-30 caracteres"
            return
        }
        if (!trimmed.matches(Regex("^[a-zA-Z0-9ñÑ_-]+$"))) {
            _message.value = "Username: solo letras, números, ñ, _ y -"
            return
        }
        if (!trimmed.any { it.isLetter() }) {
            _message.value = "Username: mínimo una letra"
            return
        }
        viewModelScope.launch {
            if (userRepository.isUsernameTaken(trimmed, excludeUid = uid)) {
                _message.value = "Username ya está en uso"
                return@launch
            }
            try {
                firestore.collection("users").document(uid)
                    .update(mapOf(
                        "username" to trimmed,
                        "usernameLower" to trimmed.lowercase()
                    )).await()
                _message.value = "Username actualizado"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val ref = Firebase.storage.reference.child("avatars/$uid/avatar.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                firestore.collection("users").document(uid)
                    .update("avatarUrl", url).await()
                _message.value = "Avatar actualizado"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}