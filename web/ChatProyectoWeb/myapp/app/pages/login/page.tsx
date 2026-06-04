'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { auth } from '@/app/lib/firebase';
import { AuthProvider, useAuth } from '@/app/lib/AuthContext';

function LoginForm() {
  const router = useRouter();
  const { firebaseUser, loading } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!loading && firebaseUser) {
      router.push(firebaseUser.emailVerified ? '/pages/user/chat/list' : '/pages/verify-email');
    }
  }, [firebaseUser, loading, router]);

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const { user } = await signInWithEmailAndPassword(auth, email, password);
      router.push(user.emailVerified ? '/pages/user/chat/list' : '/pages/verify-email');
    } catch (err: unknown) {
      const code = (err as { code?: string }).code;
      setError(
        code === 'auth/invalid-credential' || code === 'auth/wrong-password' || code === 'auth/user-not-found'
          ? 'Invalid email or password'
          : 'Sign in failed. Please try again.'
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return null;

  return (
    <div
      className="min-h-screen flex items-center justify-center text-white font-sans p-4"
      style={{
        backgroundImage:
          "linear-gradient(rgba(0,0,0,0.5),rgba(0,0,0,0.5)), url('/images/d860a717e06773b024fe904e0ee095b5.jpg')",
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }}
    >
      <div className="absolute top-8 left-8">
        <button
          onClick={() => router.back()}
          className="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition-colors bg-[#1E1E1E] px-3 py-1.5 rounded-md border border-white/10"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="m15 18-6-6 6-6" />
          </svg>
          Back
        </button>
      </div>

      <div className="w-full max-w-[400px] flex flex-col items-center">
        <h1 className="text-3xl font-semibold mb-2">Yooo, welcome back!</h1>
        <p className="text-sm text-gray-400 mb-8">
          First time here?{' '}
          <Link href="/pages/signup" className="text-white hover:underline font-medium">
            Sign up for free
          </Link>
        </p>

        {error && (
          <div className="w-full mb-4 p-3 bg-red-500/20 border border-red-500/30 rounded-lg text-sm text-red-400">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="w-full space-y-4">
          <input
            type="email"
            placeholder="Your email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full bg-[#1E1E1E] border border-white/5 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-white/20 transition-all placeholder:text-gray-600"
          />
          <input
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full bg-[#1E1E1E] border border-white/5 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-white/20 transition-all placeholder:text-gray-600"
          />
          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-white text-black font-semibold py-3 rounded-lg hover:bg-gray-200 transition-colors mt-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {submitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <p className="mt-12 text-[11px] text-center text-gray-500 leading-relaxed max-w-[280px]">
          You acknowledge that you read, and agree, to our{' '}
          <Link href="/terms" className="underline hover:text-gray-300">Terms of Service</Link>{' '}
          and our{' '}
          <Link href="/privacy" className="underline hover:text-gray-300">Privacy Policy</Link>.
        </p>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <AuthProvider>
      <LoginForm />
    </AuthProvider>
  );
}
