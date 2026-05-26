import * as admin from "firebase-admin";

admin.initializeApp();

export {onUserCreated} from "./auth/onUserCreated";
export {createChat} from "./chats/createChat";
export {createGroupChat} from "./chats/createGroupChat";
export {addGroupMembers} from "./chats/addGroupMembers";
export {onMessageCreated} from "./chats/onMessageCreated";
export {onUserStatusChanged} from "./chats/onUserStatusChanged";
export {markAsRead} from "./messages/markAsRead";
export {deleteMessage} from "./messages/deleteMessage";
