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

sealed interface CreateGroupUiState {
    data object Idle : CreateGroupUiState
    data object Creating : CreateGroupUiState
    data class Created(val chatId: String) : CreateGroupUiState
    data class Error(val message: String) : CreateGroupUiState
}

class CreateGroupViewModel(
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

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _uiState = MutableStateFlow<CreateGroupUiState>(CreateGroupUiState.Idle)
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getAllUsersExceptCurrent().collect { _allUsers.value = it }
        }
        viewModelScope.launch {
            combine(_allUsers, _searchQuery) { users, query ->
                if (query.isBlank()) emptyList()
                else users.filter { it.username.contains(query.trim(), ignoreCase = true) }
            }.collect { _filteredUsers.value = it }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onGroupNameChange(name: String) { _groupName.value = name }

    fun toggleUser(uid: String) {
        _selectedUids.value = _selectedUids.value.toMutableSet().also {
            if (uid in it) it.remove(uid) else it.add(uid)
        }
    }

    fun createGroup() {
        val name = _groupName.value.trim()
        val selected = _selectedUids.value.toList()

        if (name.isEmpty()) {
            _uiState.value = CreateGroupUiState.Error("Escribe un nombre para el grupo")
            return
        }
        if (selected.isEmpty()) {
            _uiState.value = CreateGroupUiState.Error("Selecciona al menos un miembro")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateGroupUiState.Creating
            chatRepository.createGroupChat(name, selected)
                .onSuccess { chatId -> _uiState.value = CreateGroupUiState.Created(chatId) }
                .onFailure { _uiState.value = CreateGroupUiState.Error(it.message ?: "Error") }
        }
    }
}