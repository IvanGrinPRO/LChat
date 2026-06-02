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

    // actualizar lastMessage y quitar al remitente de hiddenFor
    await db.collection("chats").doc(chatId).update({
      lastMessageText: message.text ?? null,
      lastMessageType: message.type,
      lastMessageAt: message.createdAt,
      lastMessageSenderId: message.senderId,
      updatedAt: FieldValue.serverTimestamp(),
      hiddenFor: FieldValue.arrayRemove(message.senderId),
    });

    const chatSnap = await db.collection("chats").doc(chatId).get();
    const chatData = chatSnap.data();
    const members = chatData?.members as string[];
    const isGroup = chatData?.type === "group";
    const recipients = members.filter((uid) => uid !== message.senderId);

    if (recipients.length === 0) return;

    // incrementar unreadCount para todos los destinatarios
    await Promise.all(
      recipients.map((uid) =>
        db.collection("chat_members")
          .doc(`${chatId}_${uid}`)
          .update({unreadCount: FieldValue.increment(1)})
          .catch(() => null)
      )
    );

    // preparar notificación
    const senderSnap = await db.collection("users").doc(message.senderId).get();
    const senderUsername = senderSnap.data()?.username ?? "LChat";

    let messageBody: string;
    switch (message.type) {
    case "image": messageBody = "Foto"; break;
    case "file": messageBody = message.fileName ?? "Archivo"; break;
    default: messageBody = message.text ?? "";
    }

    const notifTitle = isGroup ? (chatData?.name ?? "Grupo") : senderUsername;
    const notifBody = isGroup ?
      `${senderUsername}: ${messageBody}` : messageBody;

    // enviar FCM a cada destinatario
    await Promise.all(
      recipients.map(async (recipientUid) => {
        const recipientSnap = await db
          .collection("users").doc(recipientUid).get();
        const fcmTokens = (recipientSnap.data()?.fcmTokens ?? []) as string[];
        return Promise.all(
          fcmTokens.map((token) =>
            admin.messaging().send({
              token,
              notification: {title: notifTitle, body: notifBody},
              data: {chatId, otherUid: message.senderId},
              android: {notification: {channelId: "lchat_messages"}},
            }).catch(() => null)
          )
        );
      })
    );
  }
);
