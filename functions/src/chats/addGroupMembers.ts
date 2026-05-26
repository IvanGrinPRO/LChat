import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {FieldValue} from "firebase-admin/firestore";

const db = admin.firestore();

export const addGroupMembers = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "unauthenticated");
  }

  const uid = request.auth.uid;
  const {chatId, newMemberUids} = request.data as {
    chatId: string;
    newMemberUids: string[];
  };

  // verificar que el solicitante es miembro del grupo
  const chatSnap = await db.collection("chats").doc(chatId).get();
  const members = chatSnap.data()?.members as string[];
  if (!members.includes(uid)) {
    throw new HttpsError("permission-denied", "No eres miembro del grupo");
  }

  // agregar nuevos miembros al array
  await db.collection("chats").doc(chatId).update({
    members: FieldValue.arrayUnion(...newMemberUids),
  });

  // crear chat_members para cada nuevo miembro
  await Promise.all(
    newMemberUids.map((memberUid) =>
      db.collection("chat_members").doc(`${chatId}_${memberUid}`).set({
        chatId,
        userId: memberUid,
        unreadCount: 0,
        lastReadAt: FieldValue.serverTimestamp(),
      })
    )
  );

  return {success: true};
});
