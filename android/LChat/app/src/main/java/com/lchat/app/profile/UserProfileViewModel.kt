package com.lchat.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lchat.app.data.ChatRepository
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UserProfileUiState {
    data object Idle : UserProfileUiState
    data object Loading : UserProfileUiState
    data class ChatReady(val chatId: String, val otherUid: String) : UserProfileUiState
    data class Error(val message: String) : UserProfileUiState
}

class UserProfileViewModel(
    private val targetUid: String,
    private val userRepository: UserRepository = UserRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Idle)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getUserById(targetUid).collect { _user.value = it }
        }
    }

    fun startChat() {
        viewModelScope.launch {
            _uiState.value = UserProfileUiState.Loading
            chatRepository.createChat(targetUid)
                .onSuccess { chatId ->
                    _uiState.value = UserProfileUiState.ChatReady(chatId, targetUid)
                }
                .onFailure {
                    _uiState.value = UserProfileUiState.Error(it.message ?: "Error")
                }
        }
    }

    class Factory(private val uid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UserProfileViewModel(uid) as T
    }
}