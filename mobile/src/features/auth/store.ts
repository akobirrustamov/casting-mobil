import { create } from 'zustand';

import { getItem, removeItem, setItem } from '@/lib/storage';

/**
 * Состояние авторизации.
 *
 * Токен идёт через lib/storage: на телефоне это expo-secure-store
 * (Keystore на Android, Keychain на iOS) — там ничего не изменилось.
 *
 * В браузере SecureStore не существует в принципе, и попытка записи роняла
 * вход с `setValueWithKeyAsync is not a function` уже после успешного ответа
 * Google. Веб у нас — только площадка для просмотра вёрстки, в магазины он
 * не идёт, поэтому там допустим localStorage. На безопасность мобильной
 * сборки это не влияет: ветка с localStorage на устройстве не выполняется.
 */
const TOKEN_KEY = 'uzcasting.access_token';

/**
 * Профиль кладём рядом с токеном.
 *
 * Эндпоинта «дай мне мои данные по токену» на бэкенде нет (docs/API.md §5),
 * поэтому без кэша после перезапуска мы знали бы только «человек вошёл»,
 * без имени и аватара. Когда эндпоинт появится — это станет кэшем на первый
 * кадр, а свежие данные подтянутся запросом.
 */
const USER_KEY = 'uzcasting.user';

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

/** Битый или устаревший JSON не должен ронять запуск — считаем, что профиля нет. */
function parseUser(raw: string | null): AuthUser | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as AuthUser;
    return parsed && typeof parsed.id === 'string' ? parsed : null;
  } catch {
    return null;
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isRestoring: true,
  isAuthorized: false,

  restore: async () => {
    const [token, rawUser] = await Promise.all([
      getItem(TOKEN_KEY),
      getItem(USER_KEY),
    ]);

    // TODO: по токену дёрнуть свежий профиль, когда появится эндпоинт
    set({
      token,
      user: parseUser(rawUser),
      isAuthorized: Boolean(token),
      isRestoring: false,
    });
  },

  signIn: async (token, user) => {
    await Promise.all([
      setItem(TOKEN_KEY, token),
      setItem(USER_KEY, JSON.stringify(user)),
    ]);
    set({ token, user, isAuthorized: true });
  },

  signOut: async () => {
    await Promise.all([removeItem(TOKEN_KEY), removeItem(USER_KEY)]);
    set({ token: null, user: null, isAuthorized: false });
  },
}));
