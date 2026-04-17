package com.lchat.app.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lchat.app.data.Chat
import com.lchat.app.data.ChatRepository
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatPreview(
    val chat: Chat,
    val otherUser: User
)

class ChatsViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _chats = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chats: StateFlow<List<ChatPreview>> = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val userCache = mutableMapOf<String, User>()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            chatRepository.getMyChats().collect { chatList ->
                val previews = chatList.mapNotNull { chat ->
                    val otherUid = chat.members.firstOrNull { it != userRepository.currentUid }
                        ?: return@mapNotNull null

                    val otherUser = userCache[otherUid]
                        ?: userRepository.getUserOnce(otherUid)?.also { userCache[otherUid] = it }
                        ?: return@mapNotNull null

                    ChatPreview(chat = chat, otherUser = otherUser)
                }
                _chats.value = previews
                _isLoading.value = false
            }
        }
    }
}