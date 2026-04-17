package com.lchat.app.data

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val type: String = "text",
    val text: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val thumbnailUrl: String? = null,
    val createdAt: Timestamp? = null,
    val readBy: List<String> = emptyList(),
    val deletedFor: List<String> = emptyList()
)