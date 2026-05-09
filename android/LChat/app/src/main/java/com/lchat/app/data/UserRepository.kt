package com.lchat.app.data

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: com.google.firebase.firestore.FirebaseFirestore = Firebase.firestore
) {
    val currentUid: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    fun getAllUsersExceptCurrent(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents
                    ?.mapNotNull { it.toObject(User::class.java) }
                    ?.filter { it.uid != currentUid }
                    ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    fun getUserById(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun getUserOnce(uid: String): User? {
        return try {
            firestore.collection("users").document(uid)
                .get().await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    suspend fun saveFcmToken(token: String) {
        if (currentUid.isEmpty()) return
        try {
            firestore.collection("users").document(currentUid)
                .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
                .await()
        } catch (_: Exception) {}
    }

    suspend fun removeFcmToken(token: String) {
        if (currentUid.isEmpty()) return
        try {
            firestore.collection("users").document(currentUid)
                .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayRemove(token))
                .await()
        } catch (_: Exception) {}
    }

    suspend fun setOnline(isOnline: Boolean) {
        if (currentUid.isEmpty()) return
        try {
            val data = mutableMapOf<String, Any>(
                "isOnline" to isOnline
            )
            if (!isOnline) {
                data["lastSeen"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            }
            firestore.collection("users").document(currentUid)
                .update(data)
                .await()
        } catch (_: Exception) {
            // No bloquear si falla
        }
    }
}