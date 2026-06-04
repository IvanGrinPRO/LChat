import {
  Timestamp,
  type FirestoreDataConverter,
  type QueryDocumentSnapshot,
  type SnapshotOptions,
} from "firebase/firestore";

export interface User {
  uid: string;
  username: string;
  usernameLower: string;
  email: string;
  avatarUrl: string;
  isOnline: boolean;
  lastSeen: Timestamp;
  createdAt: Timestamp;
  fcmTokens: string[];
}

export type NewUser = {
  username: string;
  usernameLower: string;
  email: string;
  avatarUrl: string;
  isOnline: boolean;
  fcmTokens: string[];
};

export const userConverter: FirestoreDataConverter<User> = {
  toFirestore(user) {
    const { uid, ...data } = user;
    return data;
  },
  fromFirestore(snapshot: QueryDocumentSnapshot, options: SnapshotOptions): User {
    const data = snapshot.data(options);
    return {
      uid: snapshot.id,
      username: data.username ?? "",
      usernameLower: data.usernameLower ?? data.username?.toLowerCase() ?? "",
      email: data.email ?? "",
      avatarUrl: data.avatarUrl ?? `https://i.pravatar.cc/150?u=${snapshot.id}`,
      isOnline: data.isOnline ?? false,
      lastSeen: data.lastSeen,
      createdAt: data.createdAt,
      fcmTokens: data.fcmTokens ?? [],
    };
  },
};
