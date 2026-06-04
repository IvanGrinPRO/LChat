'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { updatePassword, EmailAuthProvider, reauthenticateWithCredential, deleteUser } from 'firebase/auth';
import { doc, updateDoc, serverTimestamp } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { db, storage } from '@/app/lib/firebase';
import AvatarImage from '@/app/components/AvatarImage';
import { useAuth } from '@/app/lib/AuthContext';
import { useTheme } from '@/app/lib/ThemeContext';
import { Sun, Moon, Trash2 } from 'lucide-react';

export default function SettingsPage() {
  const router = useRouter();
  const { firebaseUser, user, loading } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const [activeTab, setActiveTab] = useState('General');
  const [username, setUsername] = useState('');
  const [isOnline, setIsOnline] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState('');
  const [avatarUploading, setAvatarUploading] = useState(false);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [showDeleteAccount, setShowDeleteAccount] = useState(false);
  const [deletePassword, setDeletePassword] = useState('');
  const [deleteError, setDeleteError] = useState('');
  const [deleting, setDeleting] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!loading && !firebaseUser) router.push('/pages/login');
  }, [firebaseUser, loading, router]);

  useEffect(() => {
    if (user) {
      setUsername(user.username);
      setIsOnline(user.isOnline);
      setAvatarUrl(user.avatarUrl);
    }
  }, [user]);

  function flash(msg: string, isError = false) {
    if (isError) { setError(msg); setSuccess(''); }
    else { setSuccess(msg); setError(''); }
    setTimeout(() => { setError(''); setSuccess(''); }, 3500);
  }

  async function handleAvatarChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !firebaseUser) return;
    if (file.size > 5 * 1024 * 1024) { flash('Avatar must be under 5 MB', true); return; }
    setAvatarUploading(true);
    try {
      const storageRef = ref(storage, `avatars/${firebaseUser.uid}/avatar`);
      await uploadBytes(storageRef, file);
      const url = await getDownloadURL(storageRef);
      await updateDoc(doc(db, 'users', firebaseUser.uid), { avatarUrl: url });
      setAvatarUrl(url);
      flash('Avatar updated');
    } catch (err) {
      console.error('Avatar upload error:', err);
      flash('Failed to upload avatar', true);
    } finally {
      setAvatarUploading(false);
      e.target.value = '';
    }
  }

  async function handleSaveGeneral() {
    if (!firebaseUser || !username.trim()) return;
    setSaving(true);
    try {
      await updateDoc(doc(db, 'users', firebaseUser.uid), {
        username: username.trim(),
        usernameLower: username.trim().toLowerCase(),
        isOnline,
        updatedAt: serverTimestamp(),
      });
      flash('Changes saved');
    } catch (err) {
      console.error('Save general error:', err);
      flash('Failed to save changes', true);
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteAccount() {
    if (!firebaseUser?.email || !deletePassword) return;
    setDeleteError('');
    setDeleting(true);
    try {
      const credential = EmailAuthProvider.credential(firebaseUser.email, deletePassword);
      await reauthenticateWithCredential(firebaseUser, credential);
      await updateDoc(doc(db, 'users', firebaseUser.uid), {
        username: 'Deleted Account',
        usernameLower: 'deleted account',
        email: '',
        avatarUrl: '',
        isOnline: false,
        isDeleted: true,
        updatedAt: serverTimestamp(),
      });
      await deleteUser(firebaseUser);
      router.push('/pages/login');
    } catch (err: unknown) {
      const code = (err as { code?: string }).code;
      setDeleteError(
        code === 'auth/wrong-password' || code === 'auth/invalid-credential'
          ? 'Incorrect password'
          : 'Failed to delete account'
      );
    } finally {
      setDeleting(false);
    }
  }

  async function handleSaveSecurity() {
    if (!firebaseUser?.email) return;
    if (!currentPassword || !newPassword) { flash('Fill in all fields', true); return; }
    if (newPassword.length < 6) { flash('New password must be at least 6 characters', true); return; }
    if (newPassword !== confirmPassword) { flash('Passwords do not match', true); return; }
    setSaving(true);
    try {
      const credential = EmailAuthProvider.credential(firebaseUser.email, currentPassword);
      await reauthenticateWithCredential(firebaseUser, credential);
      await updatePassword(firebaseUser, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      flash('Password changed successfully');
    } catch (err: unknown) {
      const code = (err as { code?: string }).code;
      flash(
        code === 'auth/wrong-password' || code === 'auth/invalid-credential'
          ? 'Current password is incorrect'
          : 'Failed to change password',
        true
      );
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#1a1a1e] flex items-center justify-center">
        <span className="text-gray-600 text-xs font-black tracking-[0.3em] uppercase animate-pulse">Loading...</span>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-[#1a1a1e] flex flex-col items-center justify-center gap-4">
        <span className="text-gray-500 text-xs font-black tracking-[0.2em] uppercase">Failed to load profile</span>
        <button
          onClick={() => window.location.reload()}
          className="px-6 py-3 rounded-2xl shadow-soft-out text-sm text-gray-400 hover:text-white transition-all"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#1a1a1e] flex items-start justify-center p-4 md:p-10 text-white font-sans">
      <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-[300px_1fr] gap-6 lg:gap-12">

        {/* ── Sidebar ── */}
        <aside className="space-y-4 lg:space-y-8 lg:sticky lg:top-10">
          <div className="p-5 md:p-8 rounded-[2rem] md:rounded-[2.5rem] shadow-soft-out flex flex-row lg:flex-col items-center gap-4 lg:gap-1">
            <div
              className="relative w-16 h-16 lg:w-28 lg:h-28 rounded-full shadow-soft-out border-[4px] lg:border-[6px] border-[#1e1e22] overflow-hidden lg:mb-3 cursor-pointer group shrink-0"
              onClick={() => fileInputRef.current?.click()}
            >
              {avatarUploading ? (
                <div className="w-full h-full bg-[#1e1e22] flex items-center justify-center">
                  <span className="text-[8px] text-gray-500 font-black uppercase tracking-widest animate-pulse">Uploading…</span>
                </div>
              ) : (
                <>
                  <AvatarImage src={avatarUrl} alt="avatar" className="w-full h-full object-cover" />
                  <div className="absolute inset-0 bg-black/60 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <span className="text-[9px] font-black uppercase tracking-[0.2em]">Change</span>
                  </div>
                </>
              )}
            </div>
            <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={handleAvatarChange} />
            <h2 className="text-base font-bold tracking-tight">{user.username}</h2>
            <span className="text-[10px] text-gray-500 font-bold truncate max-w-full">{user.email}</span>
            <div className="flex items-center gap-1.5 mt-1">
              <div className={`w-2 h-2 rounded-full ${user.isOnline ? 'bg-green-500 shadow-[0_0_6px_#22c55e]' : 'bg-gray-600'}`} />
              <span className="text-[10px] text-gray-500 font-bold uppercase tracking-widest">
                {user.isOnline ? 'Online' : 'Offline'}
              </span>
            </div>
          </div>

          <nav className="p-2 md:p-4 rounded-[1.5rem] md:rounded-[2rem] shadow-soft-in flex lg:flex-col gap-1">
            {['General', 'Security'].map((tab) => (
              <button
                key={tab}
                onClick={() => { setActiveTab(tab); setError(''); setSuccess(''); }}
                className={`flex-1 lg:flex-none text-center lg:text-left px-4 lg:px-6 py-3 lg:py-4 rounded-xl text-sm font-bold transition-all ${
                  activeTab === tab
                    ? 'bg-primary-accent text-white shadow-lg shadow-primary-accent/20'
                    : 'text-gray-500 hover:text-white'
                }`}
              >
                {tab}
              </button>
            ))}
          </nav>
        </aside>

        {/* ── Main ── */}
        <div className="space-y-8">
          <div className="px-4">
            <h1 className="text-4xl font-black tracking-tighter uppercase italic">{activeTab}</h1>
            <p className="text-gray-600 text-[10px] font-black tracking-[0.2em] mt-1 uppercase truncate">
              {firebaseUser?.uid}
            </p>
          </div>

          <div className="p-1 md:p-2 rounded-[2rem] md:rounded-[3.5rem] shadow-soft-out bg-[#1e1e22]">
            <div className="p-5 md:p-10 space-y-6 md:space-y-8">

              {/* ── General Tab ── */}
              {activeTab === 'General' && (
                <>
                  <section className="grid grid-cols-1 sm:grid-cols-2 gap-6 md:gap-8">
                    <div className="space-y-3">
                      <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Username</label>
                      <div className="p-1 rounded-2xl shadow-soft-in">
                        <input
                          type="text"
                          value={username}
                          onChange={(e) => setUsername(e.target.value)}
                          className="w-full bg-transparent p-4 text-sm outline-none"
                        />
                      </div>
                    </div>
                    <div className="space-y-3">
                      <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Email</label>
                      <div className="p-1 rounded-2xl shadow-soft-in">
                        <input
                          type="email"
                          value={user.email}
                          readOnly
                          className="w-full bg-transparent p-4 text-sm outline-none text-gray-500 cursor-not-allowed select-none"
                        />
                      </div>
                    </div>
                  </section>

                  <section className="p-8 rounded-3xl shadow-soft-in bg-[#1c1c20]/50 flex items-center justify-between">
                    <div>
                      <h4 className="text-sm font-bold">Show Online Status</h4>
                      <p className="text-[10px] text-gray-500 mt-1">Others will see when you are active</p>
                    </div>
                    <button
                      onClick={() => setIsOnline(!isOnline)}
                      className="w-16 h-8 rounded-full shadow-soft-out p-1 flex items-center bg-[#1a1a1e] transition-all"
                    >
                      <div className={`w-6 h-6 rounded-full transition-all duration-300 ${
                        isOnline
                          ? 'bg-primary-accent shadow-md shadow-primary-accent/40 ml-auto'
                          : 'bg-gray-700 ml-0'
                      }`} />
                    </button>
                  </section>
                </>
              )}

              {/* ── Security Tab ── */}
              {activeTab === 'Security' && (
                <section className="space-y-5">
                  <div className="space-y-3">
                    <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Current Password</label>
                    <div className="p-1 rounded-2xl shadow-soft-in">
                      <input
                        type="password"
                        placeholder="••••••••"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        className="w-full bg-transparent p-4 text-sm outline-none"
                      />
                    </div>
                  </div>
                  <div className="space-y-3">
                    <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">New Password</label>
                    <div className="p-1 rounded-2xl shadow-soft-in">
                      <input
                        type="password"
                        placeholder="min. 6 characters"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        className="w-full bg-transparent p-4 text-sm outline-none"
                      />
                    </div>
                  </div>
                  <div className="space-y-3">
                    <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] ml-4 uppercase">Confirm New Password</label>
                    <div className="p-1 rounded-2xl shadow-soft-in">
                      <input
                        type="password"
                        placeholder="repeat new password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleSaveSecurity()}
                        className="w-full bg-transparent p-4 text-sm outline-none"
                      />
                    </div>
                  </div>
                </section>
              )}

              {/* ── Theme Toggle ── */}
              <section className="p-6 rounded-3xl shadow-soft-in bg-[#1c1c20]/50 flex items-center justify-between">
                <div>
                  <h4 className="text-sm font-bold">Appearance</h4>
                  <p className="text-[10px] text-gray-500 mt-1">{theme === 'dark' ? 'Dark mode' : 'Light mode'}</p>
                </div>
                <button
                  onClick={toggleTheme}
                  className="w-16 h-8 rounded-full shadow-soft-out p-1 flex items-center bg-[#1a1a1e] transition-all"
                >
                  <div className={`w-6 h-6 rounded-full transition-all duration-300 flex items-center justify-center ${
                    theme === 'light'
                      ? 'bg-yellow-400 shadow-md shadow-yellow-400/40 ml-auto'
                      : 'bg-gray-700 ml-0'
                  }`}>
                    {theme === 'light'
                      ? <Sun size={12} className="text-white" />
                      : <Moon size={12} className="text-gray-400" />
                    }
                  </div>
                </button>
              </section>

              {/* ── Feedback ── */}
              {error && <p className="text-xs text-red-400 font-bold ml-2 animate-pulse">{error}</p>}
              {success && <p className="text-xs text-green-400 font-bold ml-2">{success}</p>}

              {/* ── Footer ── */}
              <footer className="pt-2 flex gap-4">
                <button
                  onClick={activeTab === 'General' ? handleSaveGeneral : handleSaveSecurity}
                  disabled={saving}
                  className="flex-1 bg-primary-accent py-5 rounded-[2rem] font-bold text-sm shadow-lg shadow-primary-accent/20 hover:brightness-110 active:scale-[0.98] transition-all tracking-widest uppercase disabled:opacity-50"
                >
                  {saving ? 'Saving…' : `Save ${activeTab}`}
                </button>
              </footer>

              {/* ── Danger Zone ── */}
              <div className="pt-4 border-t border-red-500/20">
                <button
                  onClick={() => { setShowDeleteAccount(true); setDeleteError(''); setDeletePassword(''); }}
                  className="w-full py-4 rounded-2xl border border-red-500/30 text-red-400 text-xs font-bold uppercase tracking-widest hover:bg-red-500/10 transition-all flex items-center justify-center gap-2"
                >
                  <Trash2 size={14} /> Delete Account
                </button>
              </div>

            </div>
          </div>
        </div>
      </div>

      {showDeleteAccount && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4"
          onClick={() => setShowDeleteAccount(false)}>
          <div className="bg-[#1e1e22] rounded-[2rem] shadow-soft-out p-8 max-w-sm w-full space-y-6 border border-white/5"
            onClick={(e) => e.stopPropagation()}>
            <div className="text-center space-y-2">
              <div className="w-14 h-14 rounded-full bg-red-400/10 flex items-center justify-center mx-auto">
                <Trash2 size={22} className="text-red-400" />
              </div>
              <h3 className="font-black text-lg tracking-tight">Delete account?</h3>
              <p className="text-xs text-gray-500 leading-relaxed">
                This action is irreversible. Your profile will be anonymized and your account will be permanently deleted.
              </p>
            </div>
            <div className="space-y-3">
              <label className="text-[10px] text-gray-500 font-black tracking-[0.2em] uppercase">Confirm your password</label>
              <div className="p-1 rounded-2xl shadow-soft-in">
                <input
                  type="password"
                  placeholder="••••••••"
                  value={deletePassword}
                  onChange={(e) => setDeletePassword(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleDeleteAccount()}
                  autoFocus
                  className="w-full bg-transparent p-3 text-sm outline-none"
                />
              </div>
              {deleteError && <p className="text-xs text-red-400 font-bold ml-2">{deleteError}</p>}
            </div>
            <div className="flex gap-3">
              <button onClick={() => setShowDeleteAccount(false)}
                className="flex-1 py-3 rounded-2xl shadow-soft-out text-gray-500 text-sm font-bold hover:text-white transition-all">
                Cancel
              </button>
              <button onClick={handleDeleteAccount} disabled={deleting || !deletePassword}
                className="flex-1 py-3 rounded-2xl bg-red-500 text-white text-sm font-bold hover:brightness-110 disabled:opacity-50 transition-all">
                {deleting ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
