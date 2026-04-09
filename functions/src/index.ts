import * as admin from "firebase-admin";
import {beforeUserCreated} from "firebase-functions/v2/identity";
admin.initializeApp();

const db = admin.firestore();

// crear usuario
export const onUserCreated = beforeUserCreated(async (event) => {
  const user = event.data;
  if (!user) return;

  await db.collection("users").doc(user.uid).set({
    uid: user.uid,
    username: user.displayName ?? "",
    email: user.email ?? "",
    avatarUrl: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    lastSeen: admin.firestore.FieldValue.serverTimestamp(),
    isOnline: false,
    fcmTokens: [],
  });
});
