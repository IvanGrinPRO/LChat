package com.lchat.app.data

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: com.google.firebase.Timestamp? = null
)