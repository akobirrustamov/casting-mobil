import axios from 'axios';

import { adminApi } from './client';

/**
 * Вход админа — `AdminAuthController` (`/api/v1/app/admin/auth/*`).
 *
 * Отличие от входа пользователя: телефон + ПАРОЛЬ и проверка роли на
 * сервере. Обычный пользователь сюда не войдёт — `403 ACCESS_DENIED`.
 */
export type AdminRole = 'HYPER_ADMIN' | 'SUPER_ADMIN' | 'ADMIN' | 'WORKER';

export type AdminUser = {
  id: string;
  name: string | null;
  phone: string | null;
  role: AdminRole | string;
};

type AdminLoginResponse = {
  accessToken: string;
  /** Всегда `null`: refresh живёт в httpOnly cookie. */
  refreshToken: null;
  user: { id: string; name?: string | null; phone?: string | null; role?: string };
};

function toUser(raw: AdminLoginResponse['user']): AdminUser {
  return {
    id: String(raw.id),
    name: raw.name || null,
    phone: raw.phone || null,
    role: raw.role ?? 'WORKER',
  };
}

export type AdminSession = { token: string; user: AdminUser };

/**
 * ⚠️ Ответ проверяется по форме: старая сборка бэкенда отвечает на
 * незнакомый адрес `index.html` со статусом 200 — без проверки «вход»
 * прошёл бы с пустым токеном, и первый же запрос списка получил бы 401.
 */
function toSession(data: unknown): AdminSession {
  const raw = data as Partial<AdminLoginResponse> | null;
  if (!raw || typeof raw.accessToken !== 'string' || !raw.user) {
    throw new AdminAuthError('UNKNOWN');
  }
  return { token: raw.accessToken, user: toUser(raw.user) };
}

export async function adminLogin(phone: string, password: string): Promise<AdminSession> {
  try {
    const { data } = await adminApi.post<unknown>('/api/v1/app/admin/auth/login', { phone, password });
    return toSession(data);
  } catch (error) {
    throw toAdminAuthError(error);
  }
}

/** Продление по cookie. Тела нет — токен сервер берёт из `uz_refresh`. */
export async function adminRefresh(): Promise<AdminSession> {
  const { data } = await adminApi.post<unknown>('/api/v1/app/admin/auth/refresh');
  return toSession(data);
}

/** Выход: гасим refresh на сервере. Ошибку глотает вызывающий. */
export async function adminLogout(): Promise<void> {
  await adminApi.post('/api/v1/app/admin/auth/logout');
}

// ─── Ошибки входа ────────────────────────────────────────────────────

export type AdminAuthErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'ACCESS_DENIED'
  | 'ACCOUNT_LOCKED'
  | 'RATE_LIMIT_EXCEEDED'
  | 'NETWORK'
  | 'UNKNOWN';

export class AdminAuthError extends Error {
  constructor(
    public readonly code: AdminAuthErrorCode,
    /** Причина блокировки сервер пишет сам — её стоит показать как есть. */
    public readonly serverMessage: string | null = null,
  ) {
    super(code);
    this.name = 'AdminAuthError';
  }
}

const KNOWN: AdminAuthErrorCode[] = [
  'INVALID_CREDENTIALS',
  'ACCESS_DENIED',
  'ACCOUNT_LOCKED',
  'RATE_LIMIT_EXCEEDED',
];

export function toAdminAuthError(error: unknown): AdminAuthError {
  if (error instanceof AdminAuthError) return error;
  if (!axios.isAxiosError(error)) return new AdminAuthError('UNKNOWN');
  if (!error.response) return new AdminAuthError('NETWORK');

  const body = (error.response.data ?? {}) as { code?: string; message?: string };
  const message = body.message ?? null;

  if (body.code && (KNOWN as string[]).includes(body.code)) {
    return new AdminAuthError(body.code as AdminAuthErrorCode, message);
  }
  // Код мог не дойти (прокси, старая сборка) — тогда решает статус.
  switch (error.response.status) {
    case 401:
      return new AdminAuthError('INVALID_CREDENTIALS', message);
    case 403:
      return new AdminAuthError('ACCESS_DENIED', message);
    case 429:
      return new AdminAuthError('ACCOUNT_LOCKED', message);
    default:
      return new AdminAuthError('UNKNOWN', message);
  }
}

const LOGIN_MESSAGE_KEY: Record<AdminAuthErrorCode, string> = {
  INVALID_CREDENTIALS: 'castingAdmin.login.errors.invalid',
  ACCESS_DENIED: 'castingAdmin.login.errors.denied',
  ACCOUNT_LOCKED: 'castingAdmin.login.errors.locked',
  RATE_LIMIT_EXCEEDED: 'castingAdmin.login.errors.locked',
  NETWORK: 'castingAdmin.login.errors.network',
  UNKNOWN: 'castingAdmin.login.errors.unknown',
};

export function adminLoginErrorKey(error: unknown): string {
  return LOGIN_MESSAGE_KEY[toAdminAuthError(error).code];
}
