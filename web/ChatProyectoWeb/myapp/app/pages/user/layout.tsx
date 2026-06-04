'use client';

import { useEffect, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import Sidebar from '@/app/components/SideBar';
import { AuthProvider, useAuth } from '@/app/lib/AuthContext';

function AuthGuard({ children }: { children: ReactNode }) {
  const { firebaseUser, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!firebaseUser) {
      router.push('/pages/login');
      return;
    }
    if (!firebaseUser.emailVerified) {
      router.push('/pages/verify-email');
    }
  }, [firebaseUser, loading, router]);

  if (loading) {
    return (
      <div className="min-h-screen bg-[#1a1a1e] flex items-center justify-center">
        <span className="text-gray-600 text-xs font-black tracking-[0.3em] uppercase animate-pulse">
          Loading...
        </span>
      </div>
    );
  }

  if (!firebaseUser || !firebaseUser.emailVerified) return null;

  return (
    <div className="flex bg-[#1a1a1e]">
      <Sidebar />
      <main className="flex-grow ml-[280px] min-h-screen">{children}</main>
    </div>
  );
}

export default function UserLayout({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <AuthGuard>{children}</AuthGuard>
    </AuthProvider>
  );
}
