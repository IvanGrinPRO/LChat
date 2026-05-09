package com.lchat.app.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lchat.app.data.ChatRepository
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface NewChatUiState {
    data object Idle : NewChatUiState
    data object Creating : NewChatUiState
    data class Created(val chatId: String, val otherUid: String) : NewChatUiState
    data class Error(val message: String) : NewChatUiState
}

class NewChatViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _filteredUsers.asStateFlow()

    private val _uiState = MutableStateFlow<NewChatUiState>(NewChatUiState.Idle)
    val uiState: StateFlow<NewChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getAllUsersExceptCurrent().collect { userList ->
                _allUsers.value = userList
            }
        }
        viewModelScope.launch {
            combine(_allUsers, _searchQuery) { users, query ->
                if (query.isBlank()) emptyList()
                else users.filter {
                    it.username.contains(query.trim(), ignoreCase = true)
                }
            }.collect { _filteredUsers.value = it }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun createChat(otherUid: String) {
        viewModelScope.launch {
            _uiState.value = NewChatUiState.Creating
            chatRepository.createChat(otherUid)
                .onSuccess { chatId -> _uiState.value = NewChatUiState.Created(chatId, otherUid) }
                .onFailure { _uiState.value = NewChatUiState.Error(it.message ?: "Error") }
        }
    }
}