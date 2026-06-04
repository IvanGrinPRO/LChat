'use client';

import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { onAuthStateChanged, type User as FirebaseUser } from 'firebase/auth';
import { doc, getDoc, setDoc, updateDoc, serverTimestamp } from 'firebase/firestore';
import { auth, db } from './firebase';
import { userConverter, type User } from './models/UserModel';

interface AuthContextType {
  firebaseUser: FirebaseUser | null;
  user: User | null;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType>({
  firebaseUser: null,
  user: null,
  loading: true,
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [firebaseUser, setFirebaseUser] = useState<FirebaseUser | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    return onAuthStateChanged(auth, async (fbUser) => {
      setFirebaseUser(fbUser);
      if (fbUser) {
        try {
          const userRef = doc(db, 'users', fbUser.uid).withConverter(userConverter);
          let snap = await getDoc(userRef);

          if (!snap.exists()) {
            const fallbackUsername = fbUser.email?.split('@')[0] ?? 'user';
            await setDoc(doc(db, 'users', fbUser.uid), {
              username: fallbackUsername,
              usernameLower: fallbackUsername.toLowerCase(),
              email: fbUser.email ?? '',
              avatarUrl: `https://i.pravatar.cc/150?u=${fbUser.uid}`,
              isOnline: true,
              fcmTokens: [],
              createdAt: serverTimestamp(),
              lastSeen: serverTimestamp(),
            });
            snap = await getDoc(userRef);
          }

          setUser(snap.exists() ? snap.data() : null);
          if (snap.exists() && fbUser.emailVerified) {
            updateDoc(doc(db, 'users', fbUser.uid), { isOnline: true }).catch(() => {});
          }
        } catch (err) {
          console.error('Failed to load user profile:', err);
          setUser(null);
        }
      } else {
        setUser(null);
      }
      setLoading(false);
    });
  }, []);

  // Track online status via tab visibility
  useEffect(() => {
    if (!firebaseUser?.emailVerified) return;

    const userRef = doc(db, 'users', firebaseUser.uid);

    function setOnline() {
      updateDoc(userRef, { isOnline: true }).catch(() => {});
    }

    function setOffline() {
      updateDoc(userRef, { isOnline: false, lastSeen: serverTimestamp() }).catch(() => {});
    }

    function handleVisibilityChange() {
      document.hidden ? setOffline() : setOnline();
    }

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('beforeunload', setOffline);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('beforeunload', setOffline);
    };
  }, [firebaseUser]);

  return (
    <AuthContext.Provider value={{ firebaseUser, user, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
