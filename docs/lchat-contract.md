# LChat — Project Contract v3 (FINAL)

> Цей документ є єдиним джерелом правди для всієї команди.
> Будь-яка зміна схеми, структури або правил — тільки після узгодження з усіма.

---

## 1. Стек

| Сервіс | Технологія |
|---|---|
| Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Files | Cloud Storage |
| Server logic | Cloud Functions (TypeScript) |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Local testing | Firebase Emulator Suite |
| Web frontend | React |
| Android | Kotlin + Firebase SDK |

---

## 2. MVP — що входить у першу версію

### ✅ Входить:
- Реєстрація / логін / логаут
- Профіль користувача (ім'я, аватар)
- Список користувачів
- Створення приватного чату (1 на 1)
- Список чатів з останнім повідомленням
- Текстові повідомлення
- Надсилання фото
- Надсилання файлів
- Статуси: sent / delivered / read
- Unread count
- Online / offline / lastSeen
- Push-сповіщення

### ❌ НЕ входить у першу версію:
- Аудіо/відеодзвінки
- "Кружочки" (video notes)
- Групові чати з ролями
- End-to-end шифрування
- Реакції на повідомлення
- Stories / статуси

---

## 3. Правила для дат (ОБОВ'ЯЗКОВО)

> ⚠️ Всі поля часу **завжди** встановлюються через `serverTimestamp()`.
> Ніколи не використовувати `new Date()` або `Date.now()` на клієнті.

```typescript
import { serverTimestamp } from "firebase/firestore";

createdAt: serverTimestamp(),
updatedAt: serverTimestamp(),
lastSeen:  serverTimestamp(),
```

Причина: клієнти (web/Android) можуть мати різний системний час → хаос у сортуванні.

---

## 4. Обмеження та валідація

| Поле | Обмеження |
|---|---|
| `text` повідомлення | ≤ 5000 символів |
| `username` | 3–30 символів, без пробілів |
| Зображення | ≤ 10 MB |
| Відео | ≤ 50 MB |
| Файл | ≤ 20 MB |
| `members` у чаті (MVP) | рівно 2 елементи |

---

## 5. Структура Firestore

### `users/{uid}`
```
{
  uid: string,                  // = document id
  username: string,             // 3–30 символів
  email: string,
  avatarUrl: string | null,
  createdAt: serverTimestamp(),
  lastSeen: serverTimestamp(),
  isOnline: boolean,
  fcmTokens: string[]           // масив токенів (кілька пристроїв)
}
```

### `chats/{chatId}`
```
{
  id: string,                   // = document id
  type: "private",              // MVP: тільки "private"
  members: string[],            // рівно 2 uid для MVP
  createdAt: serverTimestamp(),
  updatedAt: serverTimestamp(),
  lastMessageText: string | null,
  lastMessageType: MessageType | null,
  lastMessageAt: serverTimestamp() | null,
  lastMessageSenderId: string | null
}
```

### `chats/{chatId}/messages/{messageId}`
```
{
  id: string,                   // = document id
  chatId: string,
  senderId: string,             // ЗАВЖДИ = auth.uid відправника
  type: MessageType,
  text: string | null,          // для type: "text"
  fileUrl: string | null,       // public download URL з Firebase Storage
  fileName: string | null,      // для type: "file"
  fileSize: number | null,      // байти
  thumbnailUrl: string | null,  // для type: "image" | "video"
  createdAt: serverTimestamp(),
  readBy: string[],             // завжди включає senderId
  deletedFor: string[]          // uid тих, хто видалив для себе
}
```

### `chat_members/{chatId_uid}`
```
{
  chatId: string,
  userId: string,
  unreadCount: number,
  lastReadAt: serverTimestamp()
}
```

---

## 6. Типи повідомлень (MessageType)

| type | Поля що використовуються |
|---|---|
| `"text"` | `text` |
| `"image"` | `fileUrl`, `thumbnailUrl` |
| `"video"` | `fileUrl`, `thumbnailUrl` |
| `"file"` | `fileUrl`, `fileName`, `fileSize` |
| `"system"` | `text` |

---

## 7. Логіка створення повідомлення

При кожному новому повідомленні клієнт встановлює:

```typescript
{
  createdAt: serverTimestamp(),
  readBy: [senderId],       // відправник одразу в readBy
  deletedFor: [],           // порожній масив
  senderId: auth.uid        // завжди поточний авторизований user
}
```

> ⚠️ `senderId` **завжди** дорівнює `auth.uid`. Відправляти від чужого імені заборонено — це перевіряється в Security Rules.

---

## 8. Логіка isOnline / lastSeen

| Подія | Що робити |
|---|---|
| Користувач підключився | `isOnline = true` |
| Користувач відключився | `isOnline = false`, `lastSeen = serverTimestamp()` |

Реалізується через Firebase Realtime Presence або onDisconnect у Functions.

---

## 9. Логіка unreadCount

| Подія | Що робити |
|---|---|
| Нове повідомлення у чаті | `unreadCount + 1` для **іншого** учасника |
| `markAsRead` викликано | `unreadCount = 0` для поточного user |

---

## 10. Логіка createChat (дублікати)

> ⚠️ Якщо приватний чат між двома users **вже існує** — повернути існуючий `chatId`, не створювати новий.

Перевіряється у Cloud Function `createChat` перед створенням документа.

---

## 11. Сортування повідомлень

> ⚠️ Повідомлення **завжди** сортуються по `createdAt ASC` (від старих до нових).

```typescript
query(messagesRef, orderBy("createdAt", "asc"))
```

---

## 12. fileUrl формат

> `fileUrl` — це завжди **public download URL** з Firebase Storage (отриманий через `getDownloadURL()`).
> Ніколи не зберігати внутрішній Storage path замість URL.

---

## 13. Правила назв

| Правило | Приклад |
|---|---|
| Поля — camelCase | `createdAt`, `lastSeen`, `avatarUrl` |
| Дата створення | завжди `createdAt` |
| Дата оновлення | завжди `updatedAt` |
| ID користувача | завжди `uid` |
| ID чату | завжди `chatId` |
| ID повідомлення | завжди `messageId` |
| Тип повідомлення | завжди `type` |
| FCM токени | завжди `fcmTokens` (масив) |

---

## 14. Структура Cloud Storage

```
avatars/{uid}/avatar.jpg
chats/{chatId}/images/{messageId}.jpg
chats/{chatId}/videos/{messageId}.mp4
chats/{chatId}/files/{messageId}_{fileName}
chats/{chatId}/thumbnails/{messageId}.jpg
```

> ⚠️ Файли у `chats/{chatId}/` доступні **тільки** учасникам цього чату (перевіряється в Storage Rules).

---

## 15. Cloud Functions — список

| Функція | Тригер | Що робить |
|---|---|---|
| `onUserCreated` | Auth `onCreate` | Створює `users/{uid}` документ |
| `onMessageCreated` | Firestore `onCreate` | Оновлює `lastMessage` в чаті, `unreadCount +1`, надсилає FCM |
| `onUserStatusChanged` | Firestore `onUpdate` | Оновлює `isOnline` та `lastSeen` |
| `createChat` | HTTPS callable | Створює або повертає існуючий приватний чат |
| `markAsRead` | HTTPS callable | Додає uid до `readBy`, скидає `unreadCount = 0` |
| `deleteMessage` | HTTPS callable | Додає uid до `deletedFor` |

---

## 16. Security Rules (принципи)

- Читати/писати `users/{uid}` — тільки авторизований `auth.uid == uid`
- Читати `chats/{chatId}` — тільки якщо `auth.uid` в `members`
- Писати `messages` — тільки якщо `senderId == auth.uid` і user є учасником чату
- Читати файли зі Storage `chats/{chatId}/` — тільки учасники чату

---

## 17. Розподіл відповідальності

### Backend:
- Схема Firestore
- Security Rules (Firestore + Storage)
- Cloud Functions (TypeScript)
- Firebase Emulator налаштування
- Документація контракту

### Frontend Web (React):
- UI/UX екрани
- Підключення до Firebase SDK
- Робота **тільки** по погодженій схемі

### Frontend Android (Kotlin):
- Після стабільного web MVP
- Та сама Firebase схема, той самий проєкт

---

## 18. Правила командної роботи

1. **Схему не міняти самостійно** — тільки після узгодження
2. **Інтеграція після кожного спринту**, не в кінці
3. **Один Firebase project** для всіх
4. **Один monorepo** для всього коду
5. **Локальне тестування** через Firebase Emulator Suite
6. **Будь-яка зміна** контракту — оновити файл і зафіксувати в git

---

## 19. Структура монорепо

```
LChat/
├─ web/
├─ android/
├─ functions/
│  ├─ src/
│  │  ├─ index.ts
│  │  ├─ auth/
│  │  │  └─ onUserCreated.ts
│  │  ├─ chats/
│  │  │  ├─ createChat.ts
│  │  │  └─ onMessageCreated.ts
│  │  └─ messages/
│  │     ├─ markAsRead.ts
│  │     └─ deleteMessage.ts
│  ├─ package.json
│  └─ tsconfig.json
├─ docs/
│  └─ lchat-contract.md
├─ firestore.rules
├─ storage.rules
├─ firestore.indexes.json
├─ firebase.json
├─ .firebaserc
├─ .gitignore
└─ README.md
```

---

## 20. Спринти

### Спринт 1 — Основа
- [ ] Створити Firebase project (додати Web app + Android app)
- [ ] Створити GitHub monorepo
- [ ] `firebase init` (Firestore, Functions, Storage, Emulators)
- [ ] Functions → TypeScript
- [ ] Firebase Auth (email/password)
- [ ] `firestore.rules` базові
- [ ] Cloud Function `onUserCreated`
- [ ] Web: login / register
- [ ] Тест через Emulator

### Спринт 2 — Чати та повідомлення
- [ ] Firestore: `chats` + `messages` subcollection
- [ ] Security Rules для chats/messages
- [ ] Cloud Function `createChat` (з перевіркою дублікатів)
- [ ] Cloud Function `onMessageCreated`
- [ ] Cloud Function `onUserStatusChanged`
- [ ] Web: список чатів + екран чату + відправка тексту
- [ ] Real-time оновлення

### Спринт 3 — Медіа та поліровка
- [ ] Storage Rules
- [ ] Upload image / file → `getDownloadURL()`
- [ ] `markAsRead` + `deleteMessage` functions
- [ ] Read status + unread count
- [ ] Online / offline / lastSeen
- [ ] Push-сповіщення (FCM) через `fcmTokens`
- [ ] Cleanup

### Спринт 4 — Android
- [ ] Login / Register
- [ ] Chats list + Chat screen
- [ ] Send text + image
- [ ] Push-сповіщення
