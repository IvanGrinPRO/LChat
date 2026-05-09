import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {FieldValue} from "firebase-admin/firestore";

const db = admin.firestore();

export const deleteMessage = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "unauthenticated");
  }

  const uid = request.auth.uid;
  const {chatId, messageId, deleteForAll} = request.data as {
    chatId: string;
    messageId: string;
    deleteForAll?: boolean;
  };

  if (!chatId || !messageId) {
    throw new HttpsError("invalid-argument", "invalid arguments");
  }

  const messageRef = db
    .collection("chats")
    .doc(chatId)
    .collection("messages")
    .doc(messageId);

  if (deleteForAll) {
    const messageSnap = await messageRef.get();
    const senderId = messageSnap.data()?.senderId;
    if (senderId !== uid) {
      throw new HttpsError("permission-denied", "No puede borrar para todos");
    }
    await messageRef.delete();
  } else {
    await messageRef.update({
      deletedFor: FieldValue.arrayUnion(uid),
    });
  }

  return {success: true};
});
