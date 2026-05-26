package com.lchat.app.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lchat.app.data.Chat
import com.lchat.app.data.ChatRepository
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GroupProfileUiState {
    data object Idle : GroupProfileUiState
    data object Loading : GroupProfileUiState
    data object Left : GroupProfileUiState
    data class Error(val message: String) : GroupProfileUiState
}

class GroupProfileViewModel(
    private val chatId: String,
    private val chatRepository: ChatRepository = ChatRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _chat = MutableStateFlow<Chat?>(null)
    val chat: StateFlow<Chat?> = _chat.asStateFlow()

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()

    private val _uiState = MutableStateFlow<GroupProfileUiState>(GroupProfileUiState.Idle)
    val uiState: StateFlow<GroupProfileUiState> = _uiState.asStateFlow()

    private val _editingName = MutableStateFlow("")
    val editingName: StateFlow<String> = _editingName.asStateFlow()

    init {
        loadChat()
    }

    private fun loadChat() {
        viewModelScope.launch {
            val chat = chatRepository.getChatOnce(chatId) ?: return@launch
            _chat.value = chat
            _editingName.value = chat.name

            // cargar miembros
            val users = chat.members.mapNotNull { uid ->
                userRepository.getUserOnce(uid)
            }
            _members.value = users
        }
    }

    fun onNameChange(name: String) { _editingName.value = name }

    fun saveName() {
        val name = _editingName.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            chatRepository.updateGroupName(chatId, name)
            _chat.value = _chat.value?.copy(name = name)
        }
    }

    fun uploadAvatar(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.value = GroupProfileUiState.Loading
            chatRepository.uploadGroupAvatar(chatId, uri)
                .onSuccess { url ->
                    _chat.value = _chat.value?.copy(avatarUrl = url)
                    _uiState.value = GroupProfileUiState.Idle
                }
                .onFailure {
                    _uiState.value = GroupProfileUiState.Error(it.message ?: "Error")
                }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _uiState.value = GroupProfileUiState.Loading
            chatRepository.leaveGroup(chatId)
                .onSuccess { _uiState.value = GroupProfileUiState.Left }
                .onFailure { _uiState.value = GroupProfileUiState.Error(it.message ?: "Error") }
        }
    }

    class Factory(private val chatId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GroupProfileViewModel(chatId) as T
    }
}