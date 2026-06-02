import {User} from "@/app/lib/models/UserModel"
import { Message } from "./MessageModel"

export interface Chat{
    chat_id: number 
    chat_members : User[]
    Messages : Message[]
}