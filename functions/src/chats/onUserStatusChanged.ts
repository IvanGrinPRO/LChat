import * as admin from "firebase-admin";
import {onDocumentUpdated} from "firebase-functions/v2/firestore";
import {FieldValue} from "firebase-admin/firestore";

const db = admin.firestore();

export const onUserStatusChanged = onDocumentUpdated(
  "users/{uid}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) return;

    if (before.isOnline === after.isOnline) return;

    if (after.isOnline === false) {
      await db.collection("users").doc(event.params.uid).update({
        lastSeen: FieldValue.serverTimestamp(),
      });
    }
  }
);
