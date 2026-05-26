import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {FieldValue} from "firebase-admin/firestore";

const db = admin.firestore();

export const createGroupChat = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "unauthenticated");
  }

  const uid = request.auth.uid;
  const {name, memberUids} = request.data as {
    name: string;
    memberUids: string[];
  };

  if (!name || name.trim().length < 1) {
    throw new HttpsError(
      "invalid-argument", "El nombre del grupo es obligatorio");
  }

  if (!memberUids || memberUids.length < 1) {
    throw new HttpsError(
      "invalid-argument", "Se necesitan al menos 2 miembros");
  }

  // incluir al creador en los miembros
  const allMembers = [uid, ...memberUids.filter((m) => m !== uid)];

  const chatRef = db.collection("chats").doc();

  await chatRef.set({
    id: chatRef.id,
    type: "group",
    name: name.trim(),
    members: allMembers,
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
    lastMessageText: null,
    lastMessageType: null,
    lastMessageAt: null,
    lastMessageSenderId: null,
  });

  // crear chat_members para cada participante
  const memberPromises = allMembers.map((memberUid) =>
    db.collection("chat_members").doc(`${chatRef.id}_${memberUid}`).set({
      chatId: chatRef.id,
      userId: memberUid,
      unreadCount: 0,
      lastReadAt: FieldValue.serverTimestamp(),
    })
  );

  await Promise.all(memberPromises);

  return {chatId: chatRef.id};
});
