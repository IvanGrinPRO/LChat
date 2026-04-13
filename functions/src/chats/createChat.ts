import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {FieldValue} from "firebase-admin/firestore";

const db = admin.firestore();

export const createChat = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "unauthenticated");
  }

  const currentUid = request.auth.uid;
  const targetUid = request.data.targetUid as string;

  if (!targetUid || targetUid === currentUid) {
    throw new HttpsError("invalid-argument", "invalid targetUid");
  }

  // buscar chat existente
  const existing = await db.collection("chats")
    .where("type", "==", "private")
    .where("members", "array-contains", currentUid)
    .get();

  for (const doc of existing.docs) {
    const members = doc.data().members as string[];
    if (members.includes(targetUid)) {
      return {chatId: doc.id};
    }
  }

  // crear nuevo chat
  const chatRef = db.collection("chats").doc();
  const now = FieldValue.serverTimestamp();

  await chatRef.set({
    id: chatRef.id,
    type: "private",
    members: [currentUid, targetUid],
    createdAt: now,
    updatedAt: now,
    lastMessageText: null,
    lastMessageType: null,
    lastMessageAt: null,
    lastMessageSenderId: null,
  });

  // crear chat_members para cada usuario
  const batch = db.batch();

  const member1Ref = db
    .collection("chat_members")
    .doc(`${chatRef.id}_${currentUid}`);
  batch.set(member1Ref, {
    chatId: chatRef.id,
    userId: currentUid,
    unreadCount: 0,
    lastReadAt: now,
  });

  const member2Ref = db
    .collection("chat_members")
    .doc(`${chatRef.id}_${targetUid}`);
  batch.set(member2Ref, {
    chatId: chatRef.id,
    userId: targetUid,
    unreadCount: 0,
    lastReadAt: now,
  });

  await batch.commit();

  return {chatId: chatRef.id};
});
