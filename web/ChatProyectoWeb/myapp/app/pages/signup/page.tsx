'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { createUserWithEmailAndPassword, sendEmailVerification } from 'firebase/auth';
import { doc, setDoc, serverTimestamp } from 'firebase/firestore';
import { auth, db } from '@/app/lib/firebase';
import { AuthProvider, useAuth } from '@/app/lib/AuthContext';

function SignupForm() {
  const router = useRouter();
  const { firebaseUser, loading } = useAuth();
  const [userName, setUserName] = useState('');
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
      const { user } = await createUserWithEmailAndPassword(auth, email, password);
      await setDoc(doc(db, 'users', user.uid), {
        username: userName,
        usernameLower: userName.toLowerCase(),
        email: user.email ?? email.toLowerCase(),
        bio: null,
        avatarUrl: `https://i.pravatar.cc/150?u=${user.uid}`,
        isOnline: false,
        fcmTokens: [],
        createdAt: serverTimestamp(),
        lastSeen: serverTimestamp(),
      });
      await sendEmailVerification(user);
      router.push('/pages/verify-email');
    } catch (err: unknown) {
      const code = (err as { code?: string }).code;
      setError(
        code === 'auth/email-already-in-use'
          ? 'Email already in use'
          : code === 'auth/weak-password'
          ? 'Password must be at least 6 characters'
          : 'Registration failed. Please try again.'
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
        <h1 className="text-3xl font-semibold mb-2">Create your account</h1>
        <p className="text-sm text-gray-400 mb-8">
          Already have one?{' '}
          <Link href="/pages/login" className="text-white hover:underline font-medium">
            Sign in
          </Link>
        </p>

        {error && (
          <div className="w-full mb-4 p-3 bg-red-500/20 border border-red-500/30 rounded-lg text-sm text-red-400">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="w-full space-y-4">
          <input
            type="text"
            placeholder="Username"
            value={userName}
            onChange={(e) => setUserName(e.target.value)}
            required
            minLength={2}
            className="w-full bg-[#1E1E1E] border border-white/5 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-white/20 transition-all placeholder:text-gray-600"
          />
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
            placeholder="Password (min. 6 chars)"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={6}
            className="w-full bg-[#1E1E1E] border border-white/5 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-white/20 transition-all placeholder:text-gray-600"
          />
          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-white text-black font-semibold py-3 rounded-lg hover:bg-gray-200 transition-colors mt-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {submitting ? 'Creating account...' : 'Sign up'}
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

export default function SignupPage() {
  return (
    <AuthProvider>
      <SignupForm />
    </AuthProvider>
  );
}
