'use client'

import React from 'react';
import Link from 'next/link';
// import { useRouter } from 'next/router';

export default function LoginPage() {
// const router = useRouter();

// const goToPage = () =>
//     router.push("/pages/user/chat/1")

  return (
    <div className="min-h-screen flex items-center justify-center text-white font-sans p-4"
    style={{ backgroundImage: "linear-gradient(rgba(0, 0, 0, 0.5), rgba(0, 0, 0, 0.5)), url('public\d860a717e06773b024fe904e0ee095b5.jpg')" }}
    >
        
      <div className="absolute top-8 left-8">
        <button className="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition-colors bg-[#1E1E1E] px-3 py-1.5 rounded-md border border-white/10">
          <svg 
            width="16" 
            height="16" 
            viewBox="0 0 24 24" 
            fill="none" 
            stroke="currentColor" 
            strokeWidth="2" 
            strokeLinecap="round" 
            strokeLinejoin="round"
          >
            <path d="m15 18-6-6 6-6"/>
          </svg>
          Back
        </button>
      </div>

      <div className="w-full max-w-[400px] flex flex-col items-center">
        <h1 className="text-3xl font-semibold mb-2">Yooo, welcome back!</h1>
        
        <p className="text-sm text-gray-400 mb-8">
          First time here?{' '}
          <Link href="/signup" className="text-white hover:underline font-medium">
            Sign up for free
          </Link>
        </p>

        <form className="w-full space-y-4">
          <div className="space-y-2">
            <input
              type="email"
              placeholder="Your email"
              className="w-full bg-[#1E1E1E] border border-white/5 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-white/20 transition-all placeholder:text-gray-600"
            />
          </div>

          <div className="space-y-2">
            <input
              type="password"
              placeholder="••••••••"
              className="w-full bg-[#1E1E1E] border border-white/5 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-white/20 transition-all placeholder:text-gray-600"
            />
          </div>
        <Link href={"user/chat/1"}>
          <button
            type="submit"
            className="w-full bg-white text-black font-semibold py-3 rounded-lg hover:bg-gray-200 transition-colors mt-2"
          >
            Sign in
          </button>
          </Link>
        </form>

        <div className="w-full mt-6 flex flex-col gap-3">
          <button className="w-full py-3 text-sm font-medium hover:text-gray-300 transition-colors">
            Sign in using magic link
          </button>

          <div className="relative flex items-center py-2">
            <div className="flex-grow border-t border-white/10"></div>
            <span className="flex-shrink mx-4 text-[10px] text-gray-500 uppercase tracking-widest">or</span>
            <div className="flex-grow border-t border-white/10"></div>
          </div>

          <button className="w-full btn-neumorphic px-8 py-4 text-sm font-medium bg-primary-accent">
            Single sign-on (SSO)
          </button>
        </div>

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
