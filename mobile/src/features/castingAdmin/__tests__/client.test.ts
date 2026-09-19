/**
 * Клиент админки: своя сессия, продление по cookie, конец сессии.
 *
 * <h2>⚠️ Почему это нужно тестами</h2>
 * Две сессии в одном приложении легко склеить незаметно: админский
 * запрос с токеном пользователя получит 403, а неудачное продление
 * админа, попавшее в общий клиент, выкинет из аккаунта пользователя.
 * Ни то ни другое не видно по коду экрана.
 */
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';

import { setAuthToken } from '@/lib/api';

import { adminApi, setAdminSessionHandlers, setAdminToken } from '../client';

type Seen = { url: string; auth: string | undefined };

function installAdapter(respond: (config: InternalAxiosRequestConfig, seen: Seen[]) => number): Seen[] {
  const seen: Seen[] = [];
  const adapter: AxiosAdapter = async (config) => {
    seen.push({ url: config.url ?? '', auth: config.headers?.get?.('Authorization') as string | undefined });
    const status = respond(config, seen);
    const response = { data: {}, status, statusText: '', headers: {}, config };
    if (status >= 400) {
      const { AxiosError } = jest.requireActual('axios');
      throw new AxiosError('fail', String(status), config, null, response);
    }
    return response;
  };
  adminApi.defaults.adapter = adapter;
  return seen;
}

afterEach(() => {
  setAdminToken(null);
  setAuthToken(null);
  setAdminSessionHandlers(null);
});

it('шлёт токен админа, а не пользователя', async () => {
  setAuthToken('user-token');
  setAdminToken('admin-token');
  const seen = installAdapter(() => 200);

  await adminApi.get('/api/v1/casting-user');

  expect(seen[0].auth).toBe('Bearer admin-token');
});

it('на 401 продлевает сессию один раз и повторяет запрос с новым токеном', async () => {
  setAdminToken('old');
  const refresh = jest.fn().mockResolvedValue('fresh');
  const onExpired = jest.fn();
  setAdminSessionHandlers({ refresh, onExpired });

  const seen = installAdapter((config) =>
    config.headers.get('Authorization') === 'Bearer fresh' ? 200 : 401,
  );

  await adminApi.get('/api/v1/casting-user');

  expect(refresh).toHaveBeenCalledTimes(1);
  expect(onExpired).not.toHaveBeenCalled();
  expect(seen.map((s) => s.auth)).toEqual(['Bearer old', 'Bearer fresh']);
});

it('продлить не удалось — сессия закрывается, ошибка уходит наверх', async () => {
  setAdminToken('old');
  const refresh = jest.fn().mockResolvedValue(null);
  const onExpired = jest.fn();
  setAdminSessionHandlers({ refresh, onExpired });
  installAdapter(() => 401);

  await expect(adminApi.get('/api/v1/casting-user')).rejects.toMatchObject({ response: { status: 401 } });
  expect(onExpired).toHaveBeenCalledTimes(1);
});

/** 401 на входе — неверный пароль. Продлевать там нечего. */
it('401 на входе не запускает продление', async () => {
  const refresh = jest.fn();
  setAdminSessionHandlers({ refresh, onExpired: jest.fn() });
  installAdapter(() => 401);

  await expect(adminApi.post('/api/v1/app/admin/auth/login', {})).rejects.toBeTruthy();
  expect(refresh).not.toHaveBeenCalled();
});
