package com.lchat.app.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lchat.app.data.ChatRepository
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface AddMembersUiState {
    data object Idle : AddMembersUiState
    data object Loading : AddMembersUiState
    data object Done : AddMembersUiState
    data class Error(val message: String) : AddMembersUiState
}

class AddMembersViewModel(
    private val chatId: String,
    private val existingMemberUids: List<String>,
    private val userRepository: UserRepository = UserRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _filteredUsers.asStateFlow()

    private val _selectedUids = MutableStateFlow<Set<String>>(emptySet())
    val selectedUids: StateFlow<Set<String>> = _selectedUids.asStateFlow()

    private val _uiState = MutableStateFlow<AddMembersUiState>(AddMembersUiState.Idle)
    val uiState: StateFlow<AddMembersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // solo mostrar usuarios que NO son ya miembros
            userRepository.getAllUsersExceptCurrent().collect { users ->
                _allUsers.value = users.filter { it.uid !in existingMemberUids }
            }
        }
        viewModelScope.launch {
            combine(_allUsers, _searchQuery) { users, query ->
                if (query.isBlank()) emptyList()
                else users.filter { it.username.contains(query.trim(), ignoreCase = true) }
            }.collect { _filteredUsers.value = it }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun toggleUser(uid: String) {
        _selectedUids.value = _selectedUids.value.toMutableSet().also {
            if (uid in it) it.remove(uid) else it.add(uid)
        }
    }

    fun addMembers() {
        val selected = _selectedUids.value.toList()
        if (selected.isEmpty()) {
            _uiState.value = AddMembersUiState.Error("Selecciona al menos un usuario")
            return
        }
        viewModelScope.launch {
            _uiState.value = AddMembersUiState.Loading
            chatRepository.addMembersToGroup(chatId, selected)
                .onSuccess { _uiState.value = AddMembersUiState.Done }
                .onFailure { _uiState.value = AddMembersUiState.Error(it.message ?: "Error") }
        }
    }

    class Factory(
        private val chatId: String,
        private val existingMemberUids: List<String>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddMembersViewModel(chatId, existingMemberUids) as T
    }
}