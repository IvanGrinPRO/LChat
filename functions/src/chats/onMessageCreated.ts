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

    const senderSnap = await db.collection("users").doc(message.senderId).get();
    const senderUsername = senderSnap.data()?.username ?? "LChat";

    let messageBody: string;
    switch (message.type) {
    case "image": messageBody = "Foto"; break;
    case "file": messageBody = message.fileName ?? "Archivo"; break;
    default: messageBody = message.text ?? "";
    }

    const recipientSnap = await db.collection("users").doc(otherUid).get();
    const fcmTokens = (recipientSnap.data()?.fcmTokens ?? []) as string[];

    if (fcmTokens.length === 0) return;

    const sendPromises = fcmTokens.map((token) =>
      admin.messaging().send({
        token,
        notification: {title: senderUsername, body: messageBody},
        data: {chatId, otherUid: message.senderId},
        android: {notification: {channelId: "lchat_messages"}},
      }).catch(() => null)
    );

    await Promise.all(sendPromises);
  }
);
