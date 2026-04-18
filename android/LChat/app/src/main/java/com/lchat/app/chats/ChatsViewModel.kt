package com.lchat.app.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lchat.app.data.Chat
import com.lchat.app.data.ChatRepository
import com.lchat.app.data.User
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.Job
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

    private var currentChats = listOf<Chat>()
    private val usersMap = mutableMapOf<String, User>()
    private val userJobs = mutableMapOf<String, Job>()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            chatRepository.getMyChats().collect { chatList ->
                currentChats = chatList
                chatList.forEach { chat ->
                    val otherUid = chat.members.firstOrNull { it != userRepository.currentUid }
                        ?: return@forEach
                    if (otherUid !in userJobs) {
                        userJobs[otherUid] = listenToUser(otherUid)
                    }
                }
                rebuildPreviews()
                _isLoading.value = false
            }
        }
    }

    private fun listenToUser(uid: String): Job {
        return viewModelScope.launch {
            userRepository.getUserById(uid).collect { user ->
                if (user != null) {
                    usersMap[uid] = user
                    rebuildPreviews()
                }
            }
        }
    }

    private fun rebuildPreviews() {
        _chats.value = currentChats.mapNotNull { chat ->
            val otherUid = chat.members.firstOrNull { it != userRepository.currentUid }
                ?: return@mapNotNull null
            val otherUser = usersMap[otherUid] ?: return@mapNotNull null
            ChatPreview(chat = chat, otherUser = otherUser)
        }
    }
}