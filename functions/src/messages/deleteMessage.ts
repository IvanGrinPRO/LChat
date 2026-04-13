import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {FieldValue} from "firebase-admin/firestore";

const db = admin.firestore();

export const deleteMessage = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "unauthenticated");
  }

  const uid = request.auth.uid;
  const {chatId, messageId} = request.data as {
    chatId: string;
    messageId: string;
  };

  if (!chatId || !messageId) {
    throw new HttpsError("invalid-argument", "invalid arguments");
  }

  const messageRef = db
    .collection("chats")
    .doc(chatId)
    .collection("messages")
    .doc(messageId);

  await messageRef.update({
    deletedFor: FieldValue.arrayUnion(uid),
  });

  return {success: true};
});
