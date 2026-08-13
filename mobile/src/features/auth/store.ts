import * as SecureStore from 'expo-secure-store';
import { create } from 'zustand';

/**
 * Состояние авторизации.
 *
 * Токены держим в expo-secure-store (Keystore на Android, Keychain на iOS),
 * а не в обычном хранилище. На сайте это localStorage — для мобилки так нельзя.
 */
const TOKEN_KEY = 'uzcasting.access_token';

export type Role = 'user' | 'creator' | 'admin';

export type AuthUser = {
  id: string;
  name: string | null;
  phone: string | null;
  email: string | null;
  avatarUrl: string | null;
  role: Role;
};

type AuthState = {
  token: string | null;
  user: AuthUser | null;
  /** true, пока не прочитали токен из хранилища при старте. */
  isRestoring: boolean;
  isAuthorized: boolean;

  restore: () => Promise<void>;
  signIn: (token: string, user: AuthUser) => Promise<void>;
  signOut: () => Promise<void>;
};

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isRestoring: true,
  isAuthorized: false,

  restore: async () => {
    try {
      const token = await SecureStore.getItemAsync(TOKEN_KEY);
      // TODO: по токену дёрнуть профиль с бэкенда, когда появится эндпоинт
      set({ token, isAuthorized: Boolean(token), isRestoring: false });
    } catch {
      set({ token: null, isAuthorized: false, isRestoring: false });
    }
  },

  signIn: async (token, user) => {
    await SecureStore.setItemAsync(TOKEN_KEY, token);
    set({ token, user, isAuthorized: true });
  },

  signOut: async () => {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
    set({ token: null, user: null, isAuthorized: false });
  },
}));
