package com.lchat.app.data

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: com.google.firebase.firestore.FirebaseFirestore = Firebase.firestore,
    private val functions: com.google.firebase.functions.FirebaseFunctions = Firebase.functions
) {
    private val currentUid: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    fun getMyChats(): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("members", currentUid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)?.copy(id = doc.id)
                    }
                    ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createChat(otherUid: String): Result<String> {
        return try {
            val data = hashMapOf("targetUid" to otherUid)
            val result = functions
                .getHttpsCallable("createChat")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val chatId = response["chatId"] as String
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                    // Filtrar mensajes borrados para el usuario actual
                    ?.filter { currentUid !in it.deletedFor }
                    ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendTextMessage(chatId: String, text: String): Result<Unit> {
        return try {
            val message = hashMapOf(
                "chatId" to chatId,
                "senderId" to currentUid,
                "type" to "text",
                "text" to text,
                "fileUrl" to null,
                "fileName" to null,
                "fileSize" to null,
                "thumbnailUrl" to null,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "readBy" to listOf(currentUid),
                "deletedFor" to emptyList<String>()
            )
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(chatId: String): Result<Unit> {
        return try {
            val data = hashMapOf("chatId" to chatId)
            functions.getHttpsCallable("markAsRead")
                .call(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}