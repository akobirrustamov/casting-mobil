import { create } from 'zustand';

import { setAuthToken, setTokenRefresher } from '@/lib/api';
import { getItem, removeItem, setItem } from '@/lib/storage';

import { refreshSession } from './api';

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

/**
 * Refresh-токен — рядом с access-токеном, в том же защищённом хранилище.
 *
 * <h2>⚠️ Что было сломано</h2>
 * Бэкенд возвращал `refresh_token` при каждом входе, а приложение его
 * ВЫБРАСЫВАЛО: сохранялся только access-токен, живущий 15 минут.
 * Человека выкидывало из аккаунта каждые четверть часа — в том числе
 * посреди фильма.
 *
 * Поле в ответе было, читателя у него не было.
 */
const REFRESH_KEY = 'uzcasting.refresh_token';

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
  /** `null` — сессию продлить нечем (например dev-вход). */
  refreshToken: string | null;
  user: AuthUser | null;
  /** true, пока не прочитали токен из хранилища при старте. */
  isRestoring: boolean;
  isAuthorized: boolean;

  restore: () => Promise<void>;
  /** @returns новый access-токен либо `null`, если сессия закончилась. */
  renew: () => Promise<string | null>;
  /**
   * @param refreshToken необязателен: dev-вход выдаёт только access-токен.
   *        Без него сессия живёт 15 минут — как и раньше.
   */
  signIn: (token: string, user: AuthUser, refreshToken?: string | null) => Promise<void>;
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

export const useAuthStore = create<AuthState>((set, get) => ({
  token: null,
  refreshToken: null,
  user: null,
  isRestoring: true,
  isAuthorized: false,

  restore: async () => {
    const [token, refreshToken, rawUser] = await Promise.all([
      getItem(TOKEN_KEY),
      getItem(REFRESH_KEY),
      getItem(USER_KEY),
    ]);

    // TODO: по токену дёрнуть свежий профиль, когда появится эндпоинт
    set({
      token,
      refreshToken,
      user: parseUser(rawUser),
      isAuthorized: Boolean(token),
      isRestoring: false,
    });
  },

  signIn: async (token, user, refreshToken = null) => {
    await Promise.all([
      setItem(TOKEN_KEY, token),
      setItem(USER_KEY, JSON.stringify(user)),
      refreshToken ? setItem(REFRESH_KEY, refreshToken) : removeItem(REFRESH_KEY),
    ]);
    set({ token, refreshToken, user, isAuthorized: true });
  },

  signOut: async () => {
    await Promise.all([
      removeItem(TOKEN_KEY),
      removeItem(REFRESH_KEY),
      removeItem(USER_KEY),
    ]);
    set({ token: null, refreshToken: null, user: null, isAuthorized: false });
  },

  /**
   * Продлить сессию.
   *
   * ⚠️ Вызывается ТОЛЬКО интерцептором в `lib/api` и строго по одному
   * за раз — см. `refreshOnce` там же. Ротация на бэкенде гасит
   * старый токен, поэтому два параллельных обмена закрыли бы все
   * сессии пользователя.
   */
  renew: async () => {
    const current = get().refreshToken;
    if (!current) return null;

    try {
      const { token, refreshToken } = await refreshSession(current);

      // ⚠️ Новый refresh-токен сохраняем ОБЯЗАТЕЛЬНО: старый уже
      // погашен, и попытка использовать его снова будет расценена
      // как кража.
      await Promise.all([
        setItem(TOKEN_KEY, token),
        setItem(REFRESH_KEY, refreshToken),
      ]);
      set({ token, refreshToken });
      return token;
    } catch {
      // Продлить нечем — это нормальный конец сессии, а не сбой.
      // Молча возвращаем человека на экран входа.
      await get().signOut();
      return null;
    }
  },
}));

/**
 * Токен → HTTP-клиент.
 *
 * Подписка, а не вызов в каждом действии: `restore`, `signIn` и `signOut`
 * меняют токен по-разному, и забытый вызов в одном из них дал бы запрос без
 * заголовка — молчаливый «войдите в систему» на экране, где человек уже вошёл.
 */
useAuthStore.subscribe((state) => setAuthToken(state.token));

/**
 * Кто именно продлевает сессию.
 *
 * Регистрируем здесь, а не в `lib/api`: направление зависимостей —
 * `features` знает про `lib`, но не наоборот. `lib` умеет только
 * механику (поймать 401, дождаться одного обновления, повторить
 * запрос), а решение «чем продлевать и когда разлогинить» остаётся
 * в модуле авторизации.
 */
setTokenRefresher(() => useAuthStore.getState().renew());
