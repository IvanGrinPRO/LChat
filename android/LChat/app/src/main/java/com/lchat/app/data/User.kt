package com.lchat.app.data

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,
    val lastSeen: com.google.firebase.Timestamp? = null
)