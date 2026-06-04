'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { sendEmailVerification, signOut } from 'firebase/auth';
import { auth } from '@/app/lib/firebase';
import { AuthProvider, useAuth } from '@/app/lib/AuthContext';

function VerifyEmailContent() {
  const router = useRouter();
  const { firebaseUser, loading } = useAuth();
  const [resendCooldown, setResendCooldown] = useState(0);
  const [checking, setChecking] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(false);

  useEffect(() => {
    if (!loading && !firebaseUser) router.push('/pages/login');
    if (!loading && firebaseUser?.emailVerified) router.push('/pages/user/chat/list');
  }, [firebaseUser, loading, router]);

  // Auto-check every 4 seconds
  useEffect(() => {
    if (!firebaseUser || firebaseUser.emailVerified) return;
    const interval = setInterval(async () => {
      await firebaseUser.reload();
      if (auth.currentUser?.emailVerified) router.push('/pages/user/chat/list');
    }, 4000);
    return () => clearInterval(interval);
  }, [firebaseUser, router]);

  // Cooldown timer
  useEffect(() => {
    if (resendCooldown <= 0) return;
    const t = setInterval(() => setResendCooldown((c) => c - 1), 1000);
    return () => clearInterval(t);
  }, [resendCooldown]);

  const handleCheckVerification = useCallback(async () => {
    if (!firebaseUser) return;
    setChecking(true);
    await firebaseUser.reload();
    if (auth.currentUser?.emailVerified) {
      router.push('/pages/user/chat/list');
    }
    setChecking(false);
  }, [firebaseUser, router]);

  const handleResend = useCallback(async () => {
    if (!firebaseUser || resendCooldown > 0) return;
    await sendEmailVerification(firebaseUser);
    setResendSuccess(true);
    setResendCooldown(60);
    setTimeout(() => setResendSuccess(false), 4000);
  }, [firebaseUser, resendCooldown]);

  const handleLogout = useCallback(async () => {
    await signOut(auth);
    router.push('/pages/login');
  }, [router]);

  if (loading || !firebaseUser) return null;

  return (
    <div
      className="min-h-screen flex items-center justify-center text-white font-sans p-4"
      style={{
        backgroundImage:
          "linear-gradient(rgba(0,0,0,0.6),rgba(0,0,0,0.6)), url('/images/d860a717e06773b024fe904e0ee095b5.jpg')",
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }}
    >
      <div className="w-full max-w-[440px] flex flex-col items-center gap-6">

        <div className="w-20 h-20 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-4xl shadow-soft-out">
          ✉️
        </div>

        <div className="text-center space-y-2">
          <h1 className="text-2xl font-black tracking-tighter">Check your inbox</h1>
          <p className="text-sm text-gray-400 leading-relaxed">
            We sent a verification link to{' '}
            <span className="text-white font-semibold">{firebaseUser.email}</span>
          </p>
          <p className="text-xs text-gray-600">Click the link in the email to continue.</p>
        </div>

        <div className="w-full bg-white/5 border border-white/10 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
          <button
            onClick={handleCheckVerification}
            disabled={checking}
            className="w-full bg-white text-black font-semibold py-3 rounded-xl hover:bg-gray-200 transition-colors disabled:opacity-50"
          >
            {checking ? 'Checking...' : "I've verified my email"}
          </button>

          <button
            onClick={handleResend}
            disabled={resendCooldown > 0}
            className="w-full py-3 rounded-xl border border-white/10 text-sm text-gray-400 hover:text-white hover:border-white/30 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {resendCooldown > 0 ? `Resend in ${resendCooldown}s` : 'Resend verification email'}
          </button>

          {resendSuccess && (
            <p className="text-center text-xs text-green-400 font-medium">Email sent! Check your inbox.</p>
          )}
        </div>

        <p className="text-xs text-gray-600">
          Wrong account?{' '}
          <button onClick={handleLogout} className="text-gray-400 hover:text-white underline transition-colors">
            Sign out
          </button>
        </p>

      </div>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <AuthProvider>
      <VerifyEmailContent />
    </AuthProvider>
  );
}
