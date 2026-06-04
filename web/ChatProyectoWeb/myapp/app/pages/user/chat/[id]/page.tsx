'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams, useRouter } from 'next/navigation';
import {
  collection, query, where, orderBy, onSnapshot,
  doc, getDoc, getDocs, addDoc, writeBatch, serverTimestamp,
} from 'firebase/firestore';
import { ref, uploadBytesResumable, getDownloadURL } from 'firebase/storage';
import { db, storage } from '@/app/lib/firebase';
import { chatConverter, messageConverter, type Chat, type Message } from '@/app/lib/models/ChatModel';
import { userConverter, type User } from '@/app/lib/models/UserModel';
import { useAuth } from '@/app/lib/AuthContext';
import { SendHorizonal, Paperclip, X, FileIcon, Users, MessageCircle } from 'lucide-react';
import AvatarImage from '@/app/components/AvatarImage';

const MAX_FILE_SIZE = 20 * 1024 * 1024;

function formatFileSize(bytes: number | null): string {
  if (!bytes) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function ChatPage() {
  const params = useParams();
  const chatId = params.id as string;
  const router = useRouter();
  const { firebaseUser } = useAuth();

  const [chats, setChats] = useState<Chat[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageText, setMessageText] = useState('');
  const [userCache, setUserCache] = useState<Record<string, User>>({});

  // New private chat
  const [showNewChat, setShowNewChat] = useState(false);
  const [newChatUsername, setNewChatUsername] = useState('');
  const [newChatError, setNewChatError] = useState('');
  const [newChatLoading, setNewChatLoading] = useState(false);

  // New group chat
  const [showNewGroup, setShowNewGroup] = useState(false);
  const [groupName, setGroupName] = useState('');
  const [groupSearch, setGroupSearch] = useState('');
  const [groupSearchError, setGroupSearchError] = useState('');
  const [groupMembers, setGroupMembers] = useState<User[]>([]);
  const [groupLoading, setGroupLoading] = useState(false);

  // File upload
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadPreview, setUploadPreview] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState('');

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const loadedUids = useRef(new Set<string>());
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isRealChat = chatId !== 'list';
  const activeChat = isRealChat ? chats.find((c) => c.id === chatId) : null;

  async function loadUserProfiles(uids: string[]) {
    const toLoad = uids.filter((uid) => !loadedUids.current.has(uid));
    if (!toLoad.length) return;
    for (const uid of toLoad) {
      loadedUids.current.add(uid);
      const snap = await getDoc(doc(db, 'users', uid).withConverter(userConverter));
      if (snap.exists()) setUserCache((prev) => ({ ...prev, [uid]: snap.data() }));
    }
  }

  useEffect(() => {
    if (!firebaseUser) return;
    const q = query(
      collection(db, 'chats').withConverter(chatConverter),
      where('members', 'array-contains', firebaseUser.uid)
    );
    return onSnapshot(q, (snap) => {
      const list = snap.docs.map((d) => d.data()).sort((a, b) => {
        if (!a.lastMessageAt) return 1;
        if (!b.lastMessageAt) return -1;
        return b.lastMessageAt.toMillis() - a.lastMessageAt.toMillis();
      });
      setChats(list);
      const allUids = list.flatMap((c) => c.members).filter((uid) => uid !== firebaseUser.uid);
      loadUserProfiles([...new Set(allUids)]);
    });
  }, [firebaseUser]);

  useEffect(() => {
    if (!firebaseUser || !isRealChat) { setMessages([]); return; }
    const q = query(
      collection(db, 'chats', chatId, 'messages').withConverter(messageConverter),
      orderBy('createdAt', 'asc')
    );
    return onSnapshot(q, (snap) => setMessages(snap.docs.map((d) => d.data())));
  }, [firebaseUser, chatId, isRealChat]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // ── Send text ──
  async function handleSend() {
    if (!messageText.trim() || !firebaseUser || !isRealChat) return;
    const text = messageText.trim();
    setMessageText('');
    const batch = writeBatch(db);
    batch.set(doc(collection(db, 'chats', chatId, 'messages')), {
      chatId, senderId: firebaseUser.uid, type: 'text', text,
      readBy: [firebaseUser.uid], deletedFor: [],
      fileUrl: null, fileName: null, fileSize: null, thumbnailUrl: null,
      createdAt: serverTimestamp(),
    });
    batch.update(doc(db, 'chats', chatId), {
      lastMessageAt: serverTimestamp(), lastMessageSenderId: firebaseUser.uid,
      lastMessageText: text, lastMessageType: 'text', updatedAt: serverTimestamp(),
    });
    await batch.commit();
  }

  // ── File upload ──
  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';
    if (file.size > MAX_FILE_SIZE) { setUploadError('File exceeds 20 MB limit'); return; }
    setUploadError('');
    setUploadFile(file);
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (ev) => setUploadPreview(ev.target?.result as string);
      reader.readAsDataURL(file);
    } else {
      setUploadPreview(null);
    }
  }

  function cancelUpload() {
    setUploadFile(null); setUploadPreview(null);
    setUploadProgress(null); setUploadError('');
  }

  async function handleSendFile() {
    if (!uploadFile || !firebaseUser || !isRealChat) return;
    const isImage = uploadFile.type.startsWith('image/');
    const type = isImage ? 'image' : 'file';
    const task = uploadBytesResumable(
      ref(storage, `chats/${chatId}/${Date.now()}_${uploadFile.name}`),
      uploadFile
    );
    setUploadProgress(0);
    task.on('state_changed',
      (snap) => setUploadProgress(Math.round((snap.bytesTransferred / snap.totalBytes) * 100)),
      () => { setUploadError('Upload failed'); setUploadProgress(null); },
      async () => {
        const fileUrl = await getDownloadURL(task.snapshot.ref);
        const batch = writeBatch(db);
        batch.set(doc(collection(db, 'chats', chatId, 'messages')), {
          chatId, senderId: firebaseUser.uid, type, text: '',
          readBy: [firebaseUser.uid], deletedFor: [],
          fileUrl, fileName: uploadFile.name, fileSize: uploadFile.size, thumbnailUrl: null,
          createdAt: serverTimestamp(),
        });
        batch.update(doc(db, 'chats', chatId), {
          lastMessageAt: serverTimestamp(), lastMessageSenderId: firebaseUser.uid,
          lastMessageText: isImage ? '📷 Photo' : `📎 ${uploadFile.name}`,
          lastMessageType: type, updatedAt: serverTimestamp(),
        });
        await batch.commit();
        cancelUpload();
      }
    );
  }

  // ── New private chat ──
  async function handleNewChat() {
    if (!newChatUsername.trim() || !firebaseUser) return;
    setNewChatError('');
    setNewChatLoading(true);
    try {
      const snap = await getDocs(query(
        collection(db, 'users').withConverter(userConverter),
        where('username', '==', newChatUsername.trim())
      ));
      if (snap.empty) { setNewChatError('No user found with that username'); return; }
      const target = snap.docs[0].data();
      if (target.uid === firebaseUser.uid) { setNewChatError('Cannot start a chat with yourself'); return; }
      const existing = chats.find((c) => c.type === 'private' && c.members.includes(target.uid));
      if (existing) { router.push(`/pages/user/chat/${existing.id}`); setShowNewChat(false); return; }
      const ref2 = await addDoc(collection(db, 'chats'), {
        type: 'private', name: null, avatarUrl: null,
        members: [firebaseUser.uid, target.uid], hiddenFor: [],
        lastMessageAt: null, lastMessageSenderId: null,
        lastMessageText: null, lastMessageType: null,
        createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      });
      router.push(`/pages/user/chat/${ref2.id}`);
      setShowNewChat(false); setNewChatUsername('');
    } catch (err) {
      console.error('handleNewChat error:', err);
      setNewChatError('Failed to create chat');
    } finally {
      setNewChatLoading(false);
    }
  }

  // ── Group: add member ──
  async function handleAddGroupMember() {
    if (!groupSearch.trim() || !firebaseUser) return;
    setGroupSearchError('');
    const snap = await getDocs(query(
      collection(db, 'users').withConverter(userConverter),
      where('username', '==', groupSearch.trim())
    ));
    if (snap.empty) { setGroupSearchError('User not found'); return; }
    const target = snap.docs[0].data();
    if (target.uid === firebaseUser.uid) { setGroupSearchError('You are already in the group'); return; }
    if (groupMembers.find((m) => m.uid === target.uid)) { setGroupSearchError('Already added'); return; }
    setGroupMembers((prev) => [...prev, target]);
    setGroupSearch('');
  }

  // ── Group: create ──
  async function handleCreateGroup() {
    if (!groupName.trim() || !firebaseUser) return;
    if (groupMembers.length < 1) { setGroupSearchError('Add at least one member'); return; }
    setGroupLoading(true);
    try {
      const memberUids = [firebaseUser.uid, ...groupMembers.map((m) => m.uid)];
      const ref2 = await addDoc(collection(db, 'chats'), {
        type: 'group', name: groupName.trim(), avatarUrl: null,
        members: memberUids, hiddenFor: [],
        lastMessageAt: null, lastMessageSenderId: null,
        lastMessageText: null, lastMessageType: null,
        createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      });
      router.push(`/pages/user/chat/${ref2.id}`);
      setShowNewGroup(false); setGroupName(''); setGroupMembers([]); setGroupSearch('');
    } catch (err) {
      console.error('handleCreateGroup error:', err);
      setGroupSearchError('Failed to create group');
    } finally {
      setGroupLoading(false);
    }
  }

  // ── Display helpers ──
  function getChatPartner(chat: Chat): User | undefined {
    const otherId = chat.members.find((m) => m !== firebaseUser?.uid);
    return otherId ? userCache[otherId] : undefined;
  }

  function getChatName(chat: Chat): string {
    if (chat.type === 'group') return chat.name ?? 'Group';
    return getChatPartner(chat)?.username ?? '...';
  }

  function getChatAvatar(chat: Chat): string {
    if (chat.type === 'group') {
      return chat.avatarUrl ?? `https://ui-avatars.com/api/?name=${encodeURIComponent(chat.name ?? 'G')}&background=6366f1&color=fff&size=150`;
    }
    const otherId = chat.members.find((m) => m !== firebaseUser?.uid);
    return getChatPartner(chat)?.avatarUrl ?? `https://i.pravatar.cc/150?u=${otherId}`;
  }

  function getGroupMemberNames(chat: Chat): string {
    const names = chat.members
      .filter((uid) => uid !== firebaseUser?.uid)
      .map((uid) => userCache[uid]?.username ?? '...')
      .slice(0, 3);
    const total = chat.members.length;
    return `${names.join(', ')}${total > 4 ? ` +${total - 4}` : ''} · ${total} members`;
  }

  function formatTime(ts: { toDate?: () => Date } | null): string {
    if (!ts?.toDate) return '';
    const date = ts.toDate();
    const now = new Date();
    if (date.toDateString() === now.toDateString())
      return date.toLocaleTimeString('en', { hour: '2-digit', minute: '2-digit', hour12: false });
    return date.toLocaleDateString('en', { month: 'short', day: 'numeric' });
  }

  const activeChatPartner = activeChat?.type === 'private' ? getChatPartner(activeChat) : null;

  function closeAllPanels() {
    setShowNewChat(false); setShowNewGroup(false);
    setNewChatUsername(''); setNewChatError('');
    setGroupName(''); setGroupSearch(''); setGroupSearchError(''); setGroupMembers([]);
  }

  return (
    <div className="h-screen bg-[#1a1a1e] p-6 flex gap-8 text-white overflow-hidden">

      {/* ── Chat List ── */}
      <aside className="w-[380px] flex flex-col gap-4 min-w-0">
        <header className="flex justify-between items-center px-4">
          <h1 className="text-3xl font-black tracking-tighter italic">CHATS</h1>
          <button
            onClick={() => { closeAllPanels(); setShowNewChat(true); }}
            className="w-12 h-12 rounded-full shadow-soft-out flex items-center justify-center text-primary-accent text-xl font-bold hover:shadow-soft-in transition-all"
          >+</button>
        </header>

        {/* ── New Private Chat ── */}
        {showNewChat && (
          <div className="px-2">
            <div className="p-4 rounded-2xl shadow-soft-in bg-[#1e1e22] space-y-3">
              <div className="flex items-center justify-between">
                <p className="text-[10px] text-gray-500 font-black tracking-[0.2em] uppercase flex items-center gap-2">
                  <MessageCircle size={12} /> New private chat
                </p>
                <button
                  onClick={() => { setShowNewGroup(true); setShowNewChat(false); setNewChatError(''); }}
                  className="text-[10px] text-primary-accent font-black tracking-widest hover:brightness-110 flex items-center gap-1"
                >
                  <Users size={11} /> Group
                </button>
              </div>
              <div className="p-1 rounded-xl shadow-soft-in">
                <input
                  type="text" placeholder="username" value={newChatUsername}
                  onChange={(e) => setNewChatUsername(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleNewChat()}
                  autoFocus
                  className="w-full bg-transparent p-3 text-sm outline-none placeholder:text-gray-600"
                />
              </div>
              {newChatError && <p className="text-xs text-red-400 ml-2">{newChatError}</p>}
              <div className="flex gap-2">
                <button onClick={handleNewChat} disabled={newChatLoading}
                  className="flex-1 py-2 rounded-xl bg-primary-accent text-white text-xs font-bold hover:brightness-110 disabled:opacity-50">
                  {newChatLoading ? '...' : 'Start Chat'}
                </button>
                <button onClick={closeAllPanels}
                  className="flex-1 py-2 rounded-xl shadow-soft-out text-gray-500 text-xs font-bold hover:text-white transition-all">
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ── New Group Chat ── */}
        {showNewGroup && (
          <div className="px-2">
            <div className="p-4 rounded-2xl shadow-soft-in bg-[#1e1e22] space-y-3">
              <div className="flex items-center justify-between">
                <p className="text-[10px] text-gray-500 font-black tracking-[0.2em] uppercase flex items-center gap-2">
                  <Users size={12} /> New group
                </p>
                <button
                  onClick={() => { setShowNewChat(true); setShowNewGroup(false); setGroupSearchError(''); }}
                  className="text-[10px] text-primary-accent font-black tracking-widest hover:brightness-110 flex items-center gap-1"
                >
                  <MessageCircle size={11} /> Private
                </button>
              </div>

              {/* Group name */}
              <div className="p-1 rounded-xl shadow-soft-in">
                <input
                  type="text" placeholder="Group name" value={groupName}
                  onChange={(e) => setGroupName(e.target.value)}
                  autoFocus
                  className="w-full bg-transparent p-3 text-sm outline-none placeholder:text-gray-600"
                />
              </div>

              {/* Add member search */}
              <div className="flex gap-2">
                <div className="flex-1 p-1 rounded-xl shadow-soft-in">
                  <input
                    type="text" placeholder="Add by username" value={groupSearch}
                    onChange={(e) => setGroupSearch(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleAddGroupMember()}
                    className="w-full bg-transparent px-3 py-2 text-sm outline-none placeholder:text-gray-600"
                  />
                </div>
                <button onClick={handleAddGroupMember}
                  className="w-9 h-9 rounded-xl bg-primary-accent flex items-center justify-center text-white text-sm font-bold hover:brightness-110 shrink-0">
                  +
                </button>
              </div>

              {groupSearchError && <p className="text-xs text-red-400 ml-2">{groupSearchError}</p>}

              {/* Members list */}
              {groupMembers.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {groupMembers.map((m) => (
                    <div key={m.uid} className="flex items-center gap-1.5 bg-white/5 rounded-full px-3 py-1">
                      <AvatarImage src={m.avatarUrl} alt="" className="w-5 h-5 rounded-full object-cover" />
                      <span className="text-xs font-bold">{m.username}</span>
                      <button onClick={() => setGroupMembers((prev) => prev.filter((x) => x.uid !== m.uid))}
                        className="text-gray-500 hover:text-red-400 transition-colors ml-0.5">
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <div className="flex gap-2">
                <button
                  onClick={handleCreateGroup}
                  disabled={groupLoading || !groupName.trim() || groupMembers.length < 1}
                  className="flex-1 py-2 rounded-xl bg-primary-accent text-white text-xs font-bold hover:brightness-110 disabled:opacity-50">
                  {groupLoading ? '...' : `Create Group (${groupMembers.length + 1})`}
                </button>
                <button onClick={closeAllPanels}
                  className="flex-1 py-2 rounded-xl shadow-soft-out text-gray-500 text-xs font-bold hover:text-white transition-all">
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="px-2">
          <div className="p-1 rounded-2xl shadow-soft-in">
            <input type="text" placeholder="Search" className="w-full bg-transparent p-4 text-sm outline-none placeholder:text-gray-600" />
          </div>
        </div>

        <div className="flex-grow overflow-y-auto px-2 space-y-4 custom-scrollbar">
          {chats.length === 0 && (
            <p className="text-center py-12 text-gray-600 text-xs font-black uppercase tracking-[0.2em]">No chats yet. Hit + to start one.</p>
          )}
          {chats.map((chat) => (
            <div key={chat.id} onClick={() => router.push(`/pages/user/chat/${chat.id}`)}
              className={`p-4 rounded-[2rem] cursor-pointer transition-all flex items-center gap-4 ${
                chat.id === chatId ? 'shadow-soft-in bg-[#1c1c20]/50' : 'shadow-soft-out hover:bg-[#1e1e22]'
              }`}>
              <div className="relative shrink-0">
                <div className="w-14 h-14 rounded-full shadow-soft-out border-2 border-[#1e1e22] overflow-hidden">
                  <AvatarImage src={getChatAvatar(chat)} alt="avatar" className="w-full h-full object-cover" />
                </div>
                {chat.type === 'private' && getChatPartner(chat)?.isOnline && (
                  <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-[#1a1a1e] shadow-[0_0_8px_#22c55e]" />
                )}
                {chat.type === 'group' && (
                  <div className="absolute bottom-0 right-0 w-5 h-5 bg-[#1e1e22] rounded-full border border-white/10 flex items-center justify-center">
                    <Users size={10} className="text-gray-400" />
                  </div>
                )}
              </div>
              <div className="flex-grow min-w-0">
                <div className="flex justify-between items-center mb-1">
                  <h3 className="font-bold text-sm truncate">{getChatName(chat)}</h3>
                  <span className="text-[10px] text-gray-500 font-bold shrink-0 ml-2">{formatTime(chat.lastMessageAt)}</span>
                </div>
                <p className="text-xs truncate text-gray-500">{chat.lastMessageText ?? 'No messages yet'}</p>
              </div>
            </div>
          ))}
        </div>
      </aside>

      {/* ── Main Chat Area ── */}
      <main className="flex-grow flex flex-col rounded-[3.5rem] shadow-soft-out bg-[#1e1e22] overflow-hidden border border-white/[0.02]">
        {isRealChat && activeChat ? (
          <>
            {/* Header */}
            <header className="p-8 flex items-center gap-4 border-b border-white/[0.03]">
              <button onClick={() => router.push('/pages/user/chat/list')}
                className="w-10 h-10 rounded-full shadow-soft-out flex items-center justify-center text-gray-500 hover:text-white mr-2">←</button>
              <div className="w-12 h-12 rounded-full shadow-soft-out border-2 border-[#1e1e22] overflow-hidden shrink-0">
                <AvatarImage src={getChatAvatar(activeChat)} alt="avatar" className="w-full h-full object-cover" />
              </div>
              <div className="min-w-0">
                <h2 className="font-bold text-sm truncate">{getChatName(activeChat)}</h2>
                {activeChat.type === 'private' && activeChatPartner?.isOnline && (
                  <p className="text-[10px] text-green-400 font-black tracking-widest uppercase">Online</p>
                )}
                {activeChat.type === 'group' && (
                  <p className="text-[10px] text-gray-500 font-bold truncate">{getGroupMemberNames(activeChat)}</p>
                )}
              </div>
            </header>

            {/* Messages */}
            <div className="flex-grow p-10 overflow-y-auto space-y-4 custom-scrollbar bg-[#1c1c20]/30">
              {messages.length === 0 && (
                <p className="text-center py-12 text-gray-600 text-xs font-black uppercase tracking-[0.2em]">Say hello! 👋</p>
              )}
              {messages.map((msg) => {
                const isOwn = msg.senderId === firebaseUser?.uid;
                const senderName = activeChat.type === 'group' && !isOwn
                  ? (userCache[msg.senderId]?.username ?? '...')
                  : null;
                return (
                  <div key={msg.id} className={`flex flex-col gap-1 max-w-[70%] ${isOwn ? 'items-end ml-auto' : 'items-start'}`}>
                    {senderName && (
                      <span className="text-[10px] text-primary-accent font-bold ml-4">{senderName}</span>
                    )}
                    {msg.type === 'text' && (
                      <div className={`p-5 rounded-[2rem] text-sm leading-relaxed ${
                        isOwn
                          ? 'rounded-tr-none bg-primary-accent text-white shadow-lg shadow-primary-accent/20 border border-white/10'
                          : 'rounded-tl-none shadow-soft-out bg-[#1e1e22] border border-white/5'
                      }`}>{msg.text}</div>
                    )}
                    {msg.type === 'image' && msg.fileUrl && (
                      <div className={`rounded-[2rem] overflow-hidden shadow-soft-out border ${isOwn ? 'rounded-tr-none border-white/10' : 'rounded-tl-none border-white/5'}`}>
                        <a href={msg.fileUrl} target="_blank" rel="noreferrer">
                          <img src={msg.fileUrl} alt={msg.fileName ?? 'image'}
                            className="max-w-[320px] max-h-[400px] object-cover block hover:opacity-90 transition-opacity" />
                        </a>
                      </div>
                    )}
                    {(msg.type === 'file' || msg.type === 'video' || msg.type === 'audio') && msg.fileUrl && (
                      <a href={msg.fileUrl} target="_blank" rel="noreferrer" download={msg.fileName ?? undefined}
                        className={`flex items-center gap-4 p-5 rounded-[2rem] hover:opacity-80 transition-opacity ${
                          isOwn
                            ? 'rounded-tr-none bg-primary-accent text-white shadow-lg shadow-primary-accent/20 border border-white/10'
                            : 'rounded-tl-none shadow-soft-out bg-[#1e1e22] border border-white/5'
                        }`}>
                        <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${isOwn ? 'bg-white/20' : 'bg-white/5'}`}>
                          <FileIcon size={18} />
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-bold truncate max-w-[200px]">{msg.fileName}</p>
                          <p className={`text-[10px] mt-0.5 ${isOwn ? 'text-white/60' : 'text-gray-500'}`}>{formatFileSize(msg.fileSize)}</p>
                        </div>
                      </a>
                    )}
                    <span className={`text-[9px] font-bold uppercase ${isOwn ? 'text-primary-accent mr-2' : 'text-gray-600 ml-2'}`}>
                      {formatTime(msg.createdAt)}
                    </span>
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>

            {/* File preview */}
            {uploadFile && (
              <div className="mx-8 mb-2 p-4 rounded-2xl bg-[#1c1c20] border border-white/5 flex items-center gap-4">
                {uploadPreview
                  ? <img src={uploadPreview} alt="preview" className="w-14 h-14 rounded-xl object-cover shrink-0" />
                  : <div className="w-14 h-14 rounded-xl bg-white/5 flex items-center justify-center shrink-0"><FileIcon size={22} className="text-gray-400" /></div>
                }
                <div className="flex-grow min-w-0">
                  <p className="text-sm font-bold truncate">{uploadFile.name}</p>
                  <p className="text-[10px] text-gray-500 mt-0.5">{formatFileSize(uploadFile.size)}</p>
                  {uploadProgress !== null && (
                    <div className="mt-2 h-1 rounded-full bg-white/10 overflow-hidden">
                      <div className="h-full bg-primary-accent transition-all duration-300" style={{ width: `${uploadProgress}%` }} />
                    </div>
                  )}
                  {uploadError && <p className="text-[10px] text-red-400 mt-1">{uploadError}</p>}
                </div>
                {uploadProgress === null && (
                  <button onClick={cancelUpload} className="text-gray-500 hover:text-white transition-colors shrink-0"><X size={18} /></button>
                )}
              </div>
            )}

            {/* Input */}
            <footer className="p-8 pt-2">
              <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileSelect} />
              {uploadError && !uploadFile && <p className="text-[10px] text-red-400 font-bold mb-2 ml-4">{uploadError}</p>}
              <div className="p-2 rounded-[2.5rem] shadow-soft-in flex items-center gap-4 bg-[#1a1a1e]/50">
                <button onClick={() => { setUploadError(''); fileInputRef.current?.click(); }}
                  className="w-12 h-12 rounded-[1.2rem] shadow-soft-out flex items-center justify-center text-gray-500 hover:text-primary-accent transition-colors ml-2 shrink-0">
                  <Paperclip size={18} />
                </button>
                <input
                  type="text"
                  placeholder={uploadFile ? 'Press send to upload file…' : 'Type a message…'}
                  value={messageText}
                  onChange={(e) => setMessageText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      uploadFile ? handleSendFile() : handleSend();
                    }
                  }}
                  disabled={!!uploadFile}
                  className="flex-grow bg-transparent text-sm outline-none py-4 px-2 disabled:opacity-40"
                />
                <button
                  onClick={uploadFile ? handleSendFile : handleSend}
                  disabled={uploadFile ? uploadProgress !== null : !messageText.trim()}
                  className="w-14 h-14 rounded-[1.5rem] bg-primary-accent shadow-lg shadow-primary-accent/20 flex items-center justify-center hover:brightness-110 active:scale-90 transition-all disabled:opacity-40 shrink-0">
                  <SendHorizonal className="text-white" size={20} />
                </button>
              </div>
            </footer>
          </>
        ) : (
          <div className="flex-grow flex items-center justify-center">
            <div className="text-center space-y-4">
              <div className="text-6xl">💬</div>
              <h2 className="text-2xl font-black tracking-tighter italic text-gray-600">Select a chat</h2>
              <p className="text-xs text-gray-600 font-black uppercase tracking-[0.2em]">Or start a new one with +</p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
