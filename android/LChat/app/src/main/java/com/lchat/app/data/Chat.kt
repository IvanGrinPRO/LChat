package com.lchat.app.data

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val type: String = "private",
    val name: String = "",
    val avatarUrl: String? = null,
    val members: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val lastMessageText: String? = null,
    val lastMessageType: String? = null,
    val lastMessageAt: Timestamp? = null,
    val lastMessageSenderId: String? = null
)