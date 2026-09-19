import { create } from 'zustand';

import { getItem, removeItem, setItem } from '@/lib/storage';

import { adminLogout, adminRefresh, type AdminUser } from './auth';
import { setAdminSessionHandlers, setAdminToken } from './client';

/**
 * Сессия админа — ОТДЕЛЬНО от сессии пользователя.
 *
 * <h2>Почему отдельно</h2>
 * Это другой человек с другими правами на том же телефоне: сотрудник
 * может проверить анкеты и выйти, не трогая свой (или чужой) обычный
 * аккаунт. Общий стор значил бы, что выход из админки выкидывает и из
 * приложения, а истёкшая админская сессия — «выход» пользователя.
 *
 * <h2>Хранение</h2>
 * Access-токен — в SecureStore (`lib/storage`), под своими ключами.
 * Refresh-токена у нас нет вовсе: он в httpOnly cookie, и продлевает
 * сессию сетевая подсистема (см. `./client`).
 *
 * ⚠️ Ключи с префиксом `uzcasting.admin.` — пересечение с ключами
 * пользователя (`uzcasting.access_token`) затёрло бы чужой токен.
 */
const TOKEN_KEY = 'uzcasting.admin.access_token';
const USER_KEY = 'uzcasting.admin.user';

type AdminState = {
  token: string | null;
  user: AdminUser | null;
  /** true, пока не прочитали хранилище. */
  isRestoring: boolean;
  /**
   * Сессия закончилась сама (не выход по кнопке). Экран входа
   * показывает «сессия истекла», а не пустую форму без объяснений.
   */
  expired: boolean;

  restore: () => Promise<void>;
  signIn: (token: string, user: AdminUser) => Promise<void>;
  /** Выход по кнопке: гасим refresh и на сервере. */
  signOut: () => Promise<void>;
  /** Продлить не удалось — закрываем молча, без запроса на сервер. */
  expire: () => Promise<void>;
  /** @returns новый access-токен либо `null`. */
  renew: () => Promise<string | null>;
};

function parseUser(raw: string | null): AdminUser | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as AdminUser;
    return parsed && typeof parsed.id === 'string' ? parsed : null;
  } catch {
    return null;
  }
}

let restored = false;

export const useAdminStore = create<AdminState>((set, get) => ({
  token: null,
  user: null,
  isRestoring: true,
  expired: false,

  restore: async () => {
    // Раздел админки открывают редко — читаем хранилище при первом
    // входе в него, а не на старте приложения, и только один раз.
    if (restored) {
      set({ isRestoring: false });
      return;
    }
    const [token, rawUser] = await Promise.all([getItem(TOKEN_KEY), getItem(USER_KEY)]);
    restored = true;
    set({ token, user: token ? parseUser(rawUser) : null, isRestoring: false });
  },

  signIn: async (token, user) => {
    await Promise.all([setItem(TOKEN_KEY, token), setItem(USER_KEY, JSON.stringify(user))]);
    restored = true;
    set({ token, user, expired: false, isRestoring: false });
  },

  signOut: async () => {
    try {
      await adminLogout();
    } catch {
      // Сети нет — сессия на телефоне всё равно закрывается: refresh
      // истечёт на сервере сам, а оставлять админку открытой нельзя.
    }
    await Promise.all([removeItem(TOKEN_KEY), removeItem(USER_KEY)]);
    set({ token: null, user: null, expired: false });
  },

  expire: async () => {
    if (!get().token) return;
    await Promise.all([removeItem(TOKEN_KEY), removeItem(USER_KEY)]);
    set({ token: null, user: null, expired: true });
  },

  renew: async () => {
    try {
      const { token, user } = await adminRefresh();
      await Promise.all([setItem(TOKEN_KEY, token), setItem(USER_KEY, JSON.stringify(user))]);
      set({ token, user });
      return token;
    } catch {
      return null;
    }
  },
}));

/** Токен → клиент админки. Подписка — по той же причине, что в `features/auth/store`. */
useAdminStore.subscribe((state) => setAdminToken(state.token));

setAdminSessionHandlers({
  refresh: () => useAdminStore.getState().renew(),
  onExpired: () => {
    void useAdminStore.getState().expire();
  },
});
