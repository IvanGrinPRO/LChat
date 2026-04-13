import * as admin from "firebase-admin";
import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {FieldValue} from "firebase-admin/firestore";

const db = admin.firestore();

export const onMessageCreated = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const message = event.data?.data();
    if (!message) return;

    const chatId = event.params.chatId;

    // actualizar lastMessage en chat
    await db.collection("chats").doc(chatId).update({
      lastMessageText: message.text ?? null,
      lastMessageType: message.type,
      lastMessageAt: message.createdAt,
      lastMessageSenderId: message.senderId,
      updatedAt: FieldValue.serverTimestamp(),
    });

    // incrementar unreadCount para el otro usuario
    const chatSnap = await db.collection("chats").doc(chatId).get();
    const members = chatSnap.data()?.members as string[];
    const otherUid = members.find((uid) => uid !== message.senderId);

    if (!otherUid) return;

    const memberRef = db
      .collection("chat_members")
      .doc(`${chatId}_${otherUid}`);

    await memberRef.update({
      unreadCount: FieldValue.increment(1),
    });
  }
);
