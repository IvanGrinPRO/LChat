import * as admin from "firebase-admin";

admin.initializeApp();

export {onUserCreated} from "./auth/onUserCreated";
export {createChat} from "./chats/createChat";
export {onMessageCreated} from "./chats/onMessageCreated";
