package com.lchat.app.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lchat.app.data.ChatRepository
import com.lchat.app.data.Message
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val otherUid: String,
    private val chatRepository: ChatRepository = ChatRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser: StateFlow<User?> = _otherUser.asStateFlow()

    val currentUid: String = userRepository.currentUid

    init {
        loadMessages()
        loadOtherUser()
        markAsRead()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    private fun loadOtherUser() {
        viewModelScope.launch {
            userRepository.getUserById(otherUid).collect { user ->
                _otherUser.value = user
            }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messageList ->
                val lastMessage = messageList.lastOrNull()
                if (lastMessage != null) {
                    chatRepository.markAsRead(chatId, lastMessage.id)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendTextMessage(chatId, text.trim())
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(chatId, messageId)
        }
    }

    fun sendImage(uri: android.net.Uri) {
        viewModelScope.launch {
            chatRepository.sendImageMessage(chatId, uri)
        }
    }

    class Factory(private val chatId: String, private val otherUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(chatId, otherUid) as T
        }
    }
}