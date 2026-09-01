import axios from 'axios';

import { api } from '@/lib/api';

import type { AuthUser, Role } from './store';

/** Ответ POST /api/v1/auth/google — см. backend AuthServiceImpl.googleLogin. */
type GoogleLoginResponse = {
  access_token: string;
  refresh_token: string;
  roles: { name: string }[];
  /** true — аккаунт создан через Google, телефона ещё нет. */
  phone_required: boolean;
  user: {
    id: string;
    name: string;
    email: string;
    avatarUrl: string;
  };
};

function toRole(roles: { name: string }[] | undefined): Role {
  const names = (roles ?? []).map((r) => r.name);
  if (names.includes('ROLE_ADMIN') || names.includes('ROLE_SUPERADMIN')) return 'admin';
  return 'user';
}

export type GoogleLoginResult = {
  token: string;
  /** ⚠️ Обязателен: без него человека выкинет через 15 минут. */
  refreshToken: string;
  user: AuthUser;
  phoneRequired: boolean;
};

/** Меняем Google ID-токен на наш JWT. Проверка подписи — на бэкенде. */
export async function exchangeGoogleToken(idToken: string): Promise<GoogleLoginResult> {
  const { data } = await api.post<GoogleLoginResponse>('/api/v1/auth/google', {
    idToken,
  });

  return {
    token: data.access_token,
    refreshToken: data.refresh_token,
    phoneRequired: data.phone_required,
    user: {
      id: data.user.id,
      name: data.user.name || null,
      email: data.user.email || null,
      phone: null,
      avatarUrl: data.user.avatarUrl || null,
      role: toRole(data.roles),
    },
  };
}

/**
 * Телефон + OTP (Eskiz SMS) — регистрация и вход одним потоком.
 *
 * Бэкенд сам решает, новый это пользователь или уже существующий:
 * `POST /otp/verify` находит хозяина номера или создаёт нового — так же,
 * как `googleLogin` делает это для Google-аккаунтов.
 */
type OtpVerifyResponse = {
  access_token: string;
  refresh_token: string;
  roles: { name: string }[];
  user: { id: string; name: string; phone: string };
};

export type OtpVerifyResult = {
  token: string;
  /** ⚠️ Обязателен: без него человека выкинет через 15 минут. */
  refreshToken: string;
  user: AuthUser;
};

/**
 * Код ошибки из тела ответа бэкенда (`ApiError.code`, см. GlobalExceptionHandler).
 *
 * Текст самой ошибки на сервере — на узбекском и не переведён на UZ/RU/EN,
 * поэтому экран показывает свой локализованный текст по коду, а не
 * `error.response.data.message` напрямую.
 */
export class OtpError extends Error {
  constructor(public readonly code: string) {
    super(code);
  }
}

function toOtpError(error: unknown): never {
  if (axios.isAxiosError(error) && error.response) {
    const code = (error.response.data as { code?: string } | undefined)?.code;
    if (code) throw new OtpError(code);
  }
  throw error;
}

/** @returns через сколько секунд можно запросить код повторно (сейчас — 2 минуты, см. бэкенд) */
export async function sendOtp(phone: string): Promise<number> {
  try {
    const { data } = await api.post<{ sent: boolean; expiresInSeconds: number }>(
      '/api/v1/app/auth/otp/send',
      { phone },
    );
    return data.expiresInSeconds;
  } catch (error) {
    return toOtpError(error);
  }
}

export async function verifyOtp(phone: string, code: string): Promise<OtpVerifyResult> {
  try {
    const { data } = await api.post<OtpVerifyResponse>('/api/v1/app/auth/otp/verify', {
      phone,
      code,
    });

    return {
      token: data.access_token,
      refreshToken: data.refresh_token,
      user: {
        id: data.user.id,
        name: data.user.name || null,
        email: null,
        phone: data.user.phone || null,
        avatarUrl: null,
        role: toRole(data.roles),
      },
    };
  } catch (error) {
    return toOtpError(error);
  }
}

/**
 * Обмен refresh-токена на новую пару.
 *
 * <h2>⚠️ Почему не старый `/api/v1/auth/refresh`</h2>
 * Тот эндпоинт принимает токен в СТРОКЕ ЗАПРОСА — он попадает в логи
 * сервера, прокси и CDN. Он не делает ротацию и заморожен: им
 * пользуются Telegram-бот и старая админка.
 *
 * <h2>⚠️ Новый refresh-токен ОБЯЗАТЕЛЬНО сохранить</h2>
 * На бэкенде ротация: старый токен гасится в момент обмена. Если
 * клиент оставит у себя прежний, следующее обновление будет отказано
 * как «повторное использование» — и закроет все сессии. То есть
 * человека выкинет ровно так же, как до этой починки.
 */
type RefreshResponse = {
  access_token: string;
  refresh_token: string;
};

export type RefreshResult = { token: string; refreshToken: string };

export async function refreshSession(refreshToken: string): Promise<RefreshResult> {
  const { data } = await api.post<RefreshResponse>('/api/v1/app/auth/refresh', {
    refresh_token: refreshToken,
  });

  return { token: data.access_token, refreshToken: data.refresh_token };
}

/**
 * Свой профиль — `GET /api/v1/app/me`.
 *
 * <h2>⚠️ Зачем, если профиль уже пришёл при входе</h2>
 * Тот профиль сохранялся на телефон и больше НИКОГДА не обновлялся.
 * Имя поменяли в панели, выдали Premium, заблокировали аккаунт —
 * приложение об этом не узнавало.
 *
 * Починка сессии сделала это заметнее: раньше токен жил 15 минут и
 * человек перелогинивался по нескольку раз в день, обновляя профиль
 * заодно. Теперь сессия живёт сутками — и устаревший профиль вместе
 * с ней.
 *
 * <h2>⚠️ Старая сборка бэкенда отвечает `index.html` со статусом 200</h2>
 * Как и на `/donations/balance`. Поэтому ответ проверяется по форме,
 * а не по коду: без этого в профиль попал бы кусок HTML, и экран
 * показал бы пустые поля вместо человека.
 */
export class ProfileUnavailableError extends Error {
  constructor() {
    super('/api/v1/app/me недоступен на этом сервере');
    this.name = 'ProfileUnavailableError';
  }
}

type MeResponse = {
  id: string;
  name: string | null;
  phone: string | null;
  email: string | null;
  avatarUrl: string | null;
  roles: { name: string }[];
  premium: { active: boolean; until: string | null };
};

export type Me = {
  user: AuthUser;
  premium: { active: boolean; until: string | null };
};

export async function fetchMe(): Promise<Me> {
  const { data } = await api.get<unknown>('/api/v1/app/me');
  const raw = data as Partial<MeResponse> | null;

  // `id` есть у любого настоящего ответа и не бывает у HTML-заглушки.
  if (!raw || typeof raw.id !== 'string') {
    throw new ProfileUnavailableError();
  }

  return {
    user: {
      id: raw.id,
      name: raw.name || null,
      email: raw.email || null,
      phone: raw.phone || null,
      avatarUrl: raw.avatarUrl || null,
      // ⚠️ Та же форма `[{name}]`, что и в ответе входа, — поэтому
      // здесь работает уже написанный `toRole`.
      role: toRole(raw.roles),
    },
    premium: {
      active: raw.premium?.active === true,
      until: raw.premium?.until ?? null,
    },
  };
}
