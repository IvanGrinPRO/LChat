package com.lchat.app.data

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
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

    suspend fun createGroupChat(name: String, memberUids: List<String>): Result<String> {
        return try {
            val result = functions.getHttpsCallable("createGroupChat")
                .call(hashMapOf(
                    "name" to name,
                    "memberUids" to memberUids
                ))
                .await()
            val chatId = (result.data as? Map<*, *>)?.get("chatId") as? String
                ?: return Result.failure(Exception("No chatId"))
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun markAsRead(chatId: String, messageId: String): Result<Unit> {
        return try {
            val data = hashMapOf(
                "chatId" to chatId,
                "messageId" to messageId
            )
            functions.getHttpsCallable("markAsRead")
                .call(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMembersToGroup(chatId: String, newMemberUids: List<String>): Result<Unit> {
        return try {
            functions.getHttpsCallable("addGroupMembers")
                .call(hashMapOf(
                    "chatId" to chatId,
                    "newMemberUids" to newMemberUids
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(chatId: String): Result<Unit> {
        return try {
            firestore.collection("chats").document(chatId)
                .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(currentUid))
                .await()
            firestore.collection("chat_members").document("${chatId}_${currentUid}")
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGroupName(chatId: String, name: String): Result<Unit> {
        return try {
            firestore.collection("chats").document(chatId)
                .update("name", name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadGroupAvatar(chatId: String, imageUri: android.net.Uri): Result<String> {
        return try {
            val storageRef = com.google.firebase.ktx.Firebase.storage.reference
                .child("chats/$chatId/avatar.jpg")
            storageRef.putFile(imageUri).await()
            val url = storageRef.downloadUrl.await().toString()
            firestore.collection("chats").document(chatId)
                .update("avatarUrl", url)
                .await()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatOnce(chatId: String): Chat? {
        return try {
            firestore.collection("chats").document(chatId)
                .get().await()
                .toObject(Chat::class.java)?.copy(id = chatId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String, deleteForAll: Boolean = false): Result<Unit> {
        return try {
            val data = hashMapOf(
                "chatId" to chatId,
                "messageId" to messageId,
                "deleteForAll" to deleteForAll
            )
            functions.getHttpsCallable("deleteMessage")
                .call(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hideChatForMe(chatId: String): Result<Unit> {
        return try {
            firestore.collection("chats").document(chatId)
                .update("hiddenFor", com.google.firebase.firestore.FieldValue.arrayUnion(currentUid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendImageMessage(chatId: String, imageUri: android.net.Uri): Result<Unit> {
        return try {
            val messageId = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document().id

            val storageRef = com.google.firebase.ktx.Firebase
                .storage.reference
                .child("chats/$chatId/images/$messageId.jpg")

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val message = hashMapOf(
                "chatId" to chatId,
                "senderId" to currentUid,
                "type" to "image",
                "text" to null,
                "fileUrl" to downloadUrl,
                "fileName" to null,
                "fileSize" to null,
                "thumbnailUrl" to downloadUrl,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "readBy" to listOf(currentUid),
                "deletedFor" to emptyList<String>()
            )

            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun sendFileMessage(
        chatId: String,
        fileUri: android.net.Uri,
        fileName: String,
        fileSize: Long
    ): Result<Unit> {
        return try {
            val messageId = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document().id

            val storageRef = com.google.firebase.ktx.Firebase
                .storage.reference
                .child("chats/$chatId/files/${messageId}_${fileName}")

            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val message = hashMapOf(
                "chatId" to chatId,
                "senderId" to currentUid,
                "type" to "file",
                "text" to null,
                "fileUrl" to downloadUrl,
                "fileName" to fileName,
                "fileSize" to fileSize,
                "thumbnailUrl" to null,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "readBy" to listOf(currentUid),
                "deletedFor" to emptyList<String>()
            )

            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun getUnreadCount(chatId: String): Flow<Int> = callbackFlow {
        val docId = "${chatId}_${currentUid}"
        val listener = firestore.collection("chat_members").document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                val count = snapshot?.getLong("unreadCount")?.toInt() ?: 0
                trySend(count)
            }
        awaitClose { listener.remove() }
    }
}