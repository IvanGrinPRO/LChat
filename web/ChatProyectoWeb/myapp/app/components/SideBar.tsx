'use client';

import React from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { signOut } from 'firebase/auth';
import { auth } from '@/app/lib/firebase';
import { useAuth } from '@/app/lib/AuthContext';
import AvatarImage from '@/app/components/AvatarImage';

const Sidebar = () => {
  const router = useRouter();
  const { user, firebaseUser } = useAuth();

  const menuItems = [
    { name: 'Chats', icon: '💬', href: '/pages/user/chat/list' },
    { name: 'Settings', icon: '⚙️', href: `/pages/user/settings/${firebaseUser?.uid ?? ''}` },
  ];

  async function handleLogout() {
    await signOut(auth);
    router.push('/pages/login');
  }

  const displayName = user?.username ?? firebaseUser?.email ?? 'User';
  const avatarUrl = user?.avatarUrl ?? `https://i.pravatar.cc/150?u=${firebaseUser?.uid ?? '1'}`;
  const isOnline = user?.isOnline ?? false;

  return (
    <aside className="fixed left-0 top-0 h-screen w-[280px] p-6 flex flex-col gap-8 bg-[#1a1a1e] z-50">
      <div className="p-6 rounded-[2.5rem] shadow-soft-out flex flex-col items-center">
        <div className="w-20 h-20 rounded-full shadow-soft-out border-[4px] border-[#1e1e22] overflow-hidden mb-3">
          <AvatarImage src={avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
        </div>
        <h2 className="text-sm font-bold tracking-tight text-white">{displayName}</h2>
        <div className="flex items-center gap-1 mt-1">
          <div
            className={`w-2 h-2 rounded-full ${
              isOnline ? 'bg-green-500 shadow-[0_0_8px_#22c55e]' : 'bg-gray-600'
            }`}
          />
          <span className="text-[10px] text-gray-500 font-bold uppercase tracking-widest">
            {isOnline ? 'Online' : 'Offline'}
          </span>
        </div>
      </div>

      <nav className="flex-grow p-2 rounded-[2rem] shadow-soft-in flex flex-col gap-2">
        {menuItems.map((item) => (
          <Link
            key={item.name}
            href={item.href}
            className="group flex items-center gap-4 px-6 py-4 rounded-2xl transition-all hover:text-primary-accent"
          >
            <span className="text-xl group-hover:scale-110 transition-transform">{item.icon}</span>
            <span className="text-sm font-medium text-gray-500 group-hover:text-white">
              {item.name}
            </span>
          </Link>
        ))}
      </nav>

      <button
        onClick={handleLogout}
        className="w-full py-4 rounded-2xl shadow-soft-out text-gray-500 text-xs font-bold hover:text-primary-accent hover:shadow-soft-in transition-all mt-auto uppercase tracking-widest"
      >
        Logout
      </button>
    </aside>
  );
};

export default Sidebar;
