import * as admin from "firebase-admin";

admin.initializeApp();

export {onUserCreated} from "./auth/onUserCreated";
export {createChat} from "./chats/createChat";
export {onMessageCreated} from "./chats/onMessageCreated";
export {onUserStatusChanged} from "./chats/onUserStatusChanged";
export {markAsRead} from "./messages/markAsRead";
export {deleteMessage} from "./messages/deleteMessage";
