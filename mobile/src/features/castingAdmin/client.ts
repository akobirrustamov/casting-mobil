import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';

import { BASE_URL, getDeviceId, isBlockedByReadOnly } from '@/lib/api';

/**
 * HTTP-клиент админки кастинга — ОТДЕЛЬНЫЙ экземпляр axios.
 *
 * <h2>⚠️ Почему не общий `api`</h2>
 * У общего клиента один токен на всё приложение (`setAuthToken`), и его
 * проставляет стор пользователя. Админ, вошедший в том же приложении,
 * либо затёр бы токен пользователя, либо получил бы его заголовок в
 * своих запросах — и сервер ответил бы 403 на ровном месте. К тому же
 * на 401 общий клиент продлевает ПОЛЬЗОВАТЕЛЬСКУЮ сессию и при неудаче
 * выводит пользователя из аккаунта: админская ошибка разлогинивала бы
 * человека, который к админке отношения не имеет.
 *
 * Поэтому здесь свой токен, своё продление и свой «конец сессии».
 *
 * <h2>Продление — только через cookie</h2>
 * `AdminAuthController` отдаёт refresh-токен НЕ в теле, а в httpOnly
 * cookie `uz_refresh` (Path=/api/v1/app/admin/auth, Secure). Прочитать
 * её из JS нельзя, но сетевая подсистема телефона хранит cookie сама
 * (OkHttp на Android, NSHTTPCookieStorage на iOS) и отправит её на
 * `/refresh`, если запрос идёт с `withCredentials`.
 *
 * ⚠️ Это «лучшее усилие», а не гарантия: cookie с флагом Secure не
 * сохранится на `http://` стенде, а хранилище cookie может очиститься
 * вместе с данными приложения. Поэтому неудачное продление — штатная
 * ветка: сессия закрывается, и админ видит «сессия истекла» на экране
 * входа, а не непонятную ошибку посреди списка.
 */
export const ADMIN_AUTH_PREFIX = '/api/v1/app/admin/auth/';

export const adminApi = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  withCredentials: true,
});

let adminToken: string | null = null;

export function setAdminToken(token: string | null): void {
  adminToken = token;
}

/**
 * Что делать с концом сессии, решает стор (`./store`) — здесь только
 * механика. Та же развязка, что у `lib/api` и `features/auth`.
 */
type SessionHandlers = {
  /** Новый access-токен либо `null`. */
  refresh: () => Promise<string | null>;
  /** Продлить не вышло — закрыть сессию. */
  onExpired: () => void;
};

let handlers: SessionHandlers | null = null;

export function setAdminSessionHandlers(next: SessionHandlers | null): void {
  handlers = next;
}

/**
 * Продление в одном экземпляре.
 *
 * ⚠️ На бэкенде ротация: каждый refresh-токен срабатывает один раз.
 * Список админки открывает несколько запросов сразу; два параллельных
 * продления погасили бы друг друга, и второе закрыло бы сессию.
 */
let inFlight: Promise<string | null> | null = null;

function refreshOnce(): Promise<string | null> {
  if (!inFlight) {
    const run = handlers ? handlers.refresh().catch(() => null) : Promise.resolve(null);
    inFlight = run.finally(() => {
      inFlight = null;
    });
  }
  return inFlight;
}

adminApi.interceptors.request.use((config) => {
  const url = config.url ?? '';

  // ⚠️ Правило записи — ОБЩЕЕ с пользовательским клиентом. Вход и
  // выход админа пропускаем так же, как пропускается вход по SMS:
  // без них нельзя даже посмотреть список.
  if (!url.startsWith(ADMIN_AUTH_PREFIX) && isBlockedByReadOnly(config.method, url)) {
    throw new Error(
      `[READ_ONLY] Запрос ${(config.method ?? 'get').toUpperCase()} ${url} заблокирован. ` +
        'Сборка только для чтения.',
    );
  }

  if (adminToken && !url.startsWith(ADMIN_AUTH_PREFIX)) {
    config.headers.set('Authorization', `Bearer ${adminToken}`);
  }

  const deviceId = getDeviceId();
  if (deviceId) {
    config.headers.set('X-Device-Id', deviceId);
  }

  return config;
});

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

adminApi.interceptors.response.use(undefined, async (error: unknown) => {
  if (!axios.isAxiosError(error) || error.response?.status !== 401) {
    throw error;
  }

  const config = error.config as RetriableConfig | undefined;
  const url = config?.url ?? '';

  // 401 на входе — это «неверный пароль», а не истёкшая сессия.
  if (!config || config._retried || url.startsWith(ADMIN_AUTH_PREFIX)) {
    throw error;
  }
  config._retried = true;

  const token = await refreshOnce();
  if (!token) {
    handlers?.onExpired();
    throw error;
  }

  setAdminToken(token);
  return adminApi.request(config);
});
