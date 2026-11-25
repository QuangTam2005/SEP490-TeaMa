import { create } from 'zustand';
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from '@/lib/auth-token';
import type { AuthResponse, UserResponse } from '@/types';

interface AuthState {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isHydrated: boolean;
  setSession: (auth: AuthResponse) => void;
  syncUser: (user: UserResponse | null) => void;
  markHydrated: () => void;
  clearSession: () => void;
  hasStoredToken: () => boolean;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isHydrated: false,

  setSession: (auth: AuthResponse) => {
    setAccessToken(auth.accessToken);
    set({ user: auth.user, isAuthenticated: true, isHydrated: true });
  },

  syncUser: (user: UserResponse | null) => {
    set({ user, isAuthenticated: user !== null, isHydrated: true });
  },

  markHydrated: () => {
    set({ isHydrated: true });
  },

  clearSession: () => {
    clearAccessToken();
    set({ user: null, isAuthenticated: false, isHydrated: true });
  },

  hasStoredToken: () => Boolean(getAccessToken()),
}));
