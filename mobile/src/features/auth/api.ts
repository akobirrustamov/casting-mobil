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
