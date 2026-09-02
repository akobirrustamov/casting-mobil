import { READ_ONLY, api } from '@/lib/api';

import type { AuthUser, Role } from './store';

/**
 * Вход в обход Google — **только для локальной разработки**.
 *
 * <h2>Зачем</h2>
 * Google-вход требует сборки: с 02.09.2026 он нативный
 * (`@react-native-google-signin/google-signin`), а нативного модуля в
 * Expo Go нет и добавить его туда нельзя. Вход по номеру упирается в
 * Eskiz, которого на локальном контуре нет. То есть зайти в приложение
 * и посмотреть внутренние экраны было нечем.
 *
 * <h2>Что это НЕ</h2>
 * Не поддельная сессия. Токен получается настоящим запросом
 * `POST /api/v1/auth/login` — тем же, которым логинится админ-панель.
 * Пользователь тоже настоящий, из dev-сеялки. Поэтому в профиле реальные
 * баланс и права, а не подставленные числа: если бэкенд скажет «нет
 * подписки», человек это и увидит.
 *
 * <h2>Три замка, чтобы это не уехало в релиз</h2>
 *   1. `__DEV__` — в production-бандле ветка мертва;
 *   2. `READ_ONLY` — против боевой базы сайта не работает;
 *   3. логин и пароль берутся из `.env`, которого нет в репозитории.
 *      Не задан хотя бы один — обхода не существует.
 *
 * Ни одного значения по умолчанию здесь нет намеренно: пароль, вписанный
 * в код «на всякий случай», однажды сработал бы на настоящем сервере.
 */
const DEV_PHONE = process.env.EXPO_PUBLIC_DEV_LOGIN_PHONE;
const DEV_PASSWORD = process.env.EXPO_PUBLIC_DEV_LOGIN_PASSWORD;

export const isDevLoginEnabled =
  __DEV__ && !READ_ONLY && Boolean(DEV_PHONE) && Boolean(DEV_PASSWORD);

type LoginResponse = {
  access_token: string;
  roles?: { name: string }[];
};

function toRole(roles: { name: string }[] | undefined): Role {
  const names = (roles ?? []).map((r) => r.name);
  if (names.includes('ROLE_ADMIN') || names.includes('ROLE_SUPERADMIN')) return 'admin';
  return 'user';
}

/**
 * `sub` из нашего же JWT — это id пользователя.
 *
 * Подпись здесь не проверяется и не должна: токен только что выдал наш
 * бэкенд, а нужен всего лишь идентификатор для ключей кэша и строки «ID»
 * в профиле. Ответ `/auth/login` объекта пользователя не содержит —
 * в отличие от `/auth/google` и `/otp/verify`.
 */
function subjectOf(jwt: string): string | null {
  const payload = jwt.split('.')[1];
  if (!payload) return null;
  try {
    return (JSON.parse(decodeBase64Url(payload)) as { sub?: string }).sub ?? null;
  } catch {
    return null;
  }
}

/**
 * base64url → строка.
 *
 * Своя реализация, а не `atob`: он есть не во всех рантаймах React Native,
 * и «работает у меня» здесь означало бы падение на чужом телефоне.
 */
const B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

function decodeBase64Url(input: string): string {
  const normalized = input.replace(/-/g, '+').replace(/_/g, '/');
  let bits = 0;
  let acc = 0;
  let out = '';

  for (const ch of normalized) {
    const value = B64.indexOf(ch);
    if (value < 0) continue; // padding и мусор пропускаем
    acc = (acc << 6) | value;
    bits += 6;
    if (bits >= 8) {
      bits -= 8;
      out += String.fromCharCode((acc >> bits) & 0xff);
    }
  }
  // Кириллицы в payload нет (это id, время и тип), поэтому latin1 достаточно.
  return out;
}

export type DevLoginResult = { token: string; user: AuthUser };

export async function devLogin(): Promise<DevLoginResult> {
  if (!isDevLoginEnabled) {
    throw new Error('Dev-вход выключен: нет EXPO_PUBLIC_DEV_LOGIN_* в .env');
  }

  const { data } = await api.post<LoginResponse>('/api/v1/auth/login', {
    phone: DEV_PHONE,
    password: DEV_PASSWORD,
  });

  const id = subjectOf(data.access_token);
  if (!id) throw new Error('В токене нет sub — это ответ не нашего бэкенда');

  return {
    token: data.access_token,
    user: {
      id,
      // Имени эндпоинт не отдаёт. Подставляем номер, а не выдуманное имя:
      // профиль покажет то, чем человек на самом деле вошёл.
      name: null,
      phone: DEV_PHONE ?? null,
      email: null,
      avatarUrl: null,
      role: toRole(data.roles),
    },
  };
}
