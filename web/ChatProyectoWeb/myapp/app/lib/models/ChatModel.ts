import {
  Timestamp,
  type FirestoreDataConverter,
  type QueryDocumentSnapshot,
  type SnapshotOptions,
} from "firebase/firestore";

/* -------------------------------------------------------------------------- */
/*                                   Enums                                     */
/* -------------------------------------------------------------------------- */

export type ChatType = "private" | "group";

export type MessageType = "text" | "image" | "file" | "video" | "audio";

/* -------------------------------------------------------------------------- */
/*                                   Chat                                      */
/* -------------------------------------------------------------------------- */

export interface Chat {
  id: string;
  type: ChatType;
  name: string | null;
  avatarUrl: string | null;
  members: string[];
  hiddenFor: string[];
  lastMessageAt: Timestamp | null;
  lastMessageSenderId: string | null;
  lastMessageText: string | null;
  lastMessageType: MessageType | null;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

/** Поля, які ти задаєш при створенні нового чату (id і таймстемпи ставить Firestore) */
export type NewChat = Omit<Chat, "id" | "createdAt" | "updatedAt">;

/* -------------------------------------------------------------------------- */
/*                                  Message                                    */
/* -------------------------------------------------------------------------- */

export interface Message {
  id: string;
  chatId: string;
  senderId: string;
  type: MessageType;
  text: string;

  /** UID користувачів, які прочитали повідомлення */
  readBy: string[];
  /** UID користувачів, для яких повідомлення видалено */
  deletedFor: string[];

  // Поля для вкладень (null для звичайного тексту)
  fileUrl: string | null;
  fileName: string | null;
  fileSize: number | null;
  thumbnailUrl: string | null;

  createdAt: Timestamp;
}

/** Поля для створення нового повідомлення */
export type NewMessage = Omit<Message, "id" | "createdAt">;

/* -------------------------------------------------------------------------- */
/*                          Firestore converters                              */
/* -------------------------------------------------------------------------- */

export const chatConverter: FirestoreDataConverter<Chat> = {
  toFirestore(chat) {
    // id зберігати в документі не обов'язково (він є в snapshot.id),
    // але лишаю, бо в твоїй структурі поле id присутнє
    const { id, ...data } = chat;
    return data;
  },
  fromFirestore(snapshot: QueryDocumentSnapshot, options: SnapshotOptions): Chat {
    const data = snapshot.data(options);
    return {
      id: snapshot.id,
      type: data.type,
      name: data.name ?? null,
      avatarUrl: data.avatarUrl ?? null,
      members: data.members ?? [],
      hiddenFor: data.hiddenFor ?? [],
      lastMessageAt: data.lastMessageAt ?? null,
      lastMessageSenderId: data.lastMessageSenderId ?? null,
      lastMessageText: data.lastMessageText ?? null,
      lastMessageType: data.lastMessageType ?? null,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
    };
  },
};

export const messageConverter: FirestoreDataConverter<Message> = {
  toFirestore(message) {
    const { id, ...data } = message;
    return data;
  },
  fromFirestore(snapshot: QueryDocumentSnapshot, options: SnapshotOptions): Message {
    const data = snapshot.data(options);
    return {
      id: snapshot.id,
      chatId: data.chatId,
      senderId: data.senderId,
      type: data.type,
      text: data.text ?? "",
      readBy: data.readBy ?? [],
      deletedFor: data.deletedFor ?? [],
      fileUrl: data.fileUrl ?? null,
      fileName: data.fileName ?? null,
      fileSize: data.fileSize ?? null,
      thumbnailUrl: data.thumbnailUrl ?? null,
      createdAt: data.createdAt,
    };
  },
};