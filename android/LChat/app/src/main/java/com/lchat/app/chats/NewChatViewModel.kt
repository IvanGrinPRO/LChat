package com.lchat.app.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lchat.app.data.ChatRepository
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NewChatUiState {
    data object Idle : NewChatUiState
    data object Creating : NewChatUiState
    data class Created(val chatId: String) : NewChatUiState
    data class Error(val message: String) : NewChatUiState
}

class NewChatViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _uiState = MutableStateFlow<NewChatUiState>(NewChatUiState.Idle)
    val uiState: StateFlow<NewChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getAllUsersExceptCurrent().collect { userList ->
                _users.value = userList
            }
        }
    }

    fun createChat(otherUid: String) {
        viewModelScope.launch {
            _uiState.value = NewChatUiState.Creating
            chatRepository.createChat(otherUid)
                .onSuccess { chatId -> _uiState.value = NewChatUiState.Created(chatId) }
                .onFailure { _uiState.value = NewChatUiState.Error(it.message ?: "Error") }
        }
    }
}