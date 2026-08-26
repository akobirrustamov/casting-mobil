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

export type OtpVerifyResult = { token: string; user: AuthUser };

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
      '/api/v1/auth/otp/send',
      { phone },
    );
    return data.expiresInSeconds;
  } catch (error) {
    return toOtpError(error);
  }
}

export async function verifyOtp(phone: string, code: string): Promise<OtpVerifyResult> {
  try {
    const { data } = await api.post<OtpVerifyResponse>('/api/v1/auth/otp/verify', {
      phone,
      code,
    });

    return {
      token: data.access_token,
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
