import * as admin from "firebase-admin";
import {auth} from "firebase-functions/v1";
import {FieldValue} from "firebase-admin/firestore";

const db = admin.firestore();

export const onUserCreated = auth.user().onCreate(async (user) => {
  await db.collection("users").doc(user.uid).set({
    uid: user.uid,
    email: user.email ?? "",
    avatarUrl: null,
    createdAt: FieldValue.serverTimestamp(),
    lastSeen: FieldValue.serverTimestamp(),
    isOnline: false,
    fcmTokens: [],
  }, {merge: true});
});
