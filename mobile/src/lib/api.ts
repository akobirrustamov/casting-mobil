import axios from 'axios';

/**
 * Тот же бэкенд, что у сайта (frontend/src/config/index.js).
 * Прод: https://uzcasting.site
 */
export const BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'https://uzcasting.site';

/**
 * Режим только для чтения.
 *
 * Приложение сейчас смотрит в БОЕВУЮ базу сайта. Пока идёт разработка,
 * любая запись туда запрещена: интерцептор ниже роняет запрос до отправки,
 * если метод не GET/HEAD. Это защита от случайного POST при отладке.
 *
 * Снимать флаг только осознанно, когда появится тестовый контур.
 */
export const READ_ONLY = process.env.EXPO_PUBLIC_READ_ONLY !== 'false';

const SAFE_METHODS = ['get', 'head', 'options'];

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
});

api.interceptors.request.use((config) => {
  const method = (config.method ?? 'get').toLowerCase();

  if (READ_ONLY && !SAFE_METHODS.includes(method)) {
    throw new Error(
      `[READ_ONLY] Запрос ${method.toUpperCase()} ${config.url ?? ''} заблокирован. ` +
        'Приложение подключено к боевой базе сайта, запись запрещена.'
    );
  }

  return config;
});

// TODO: подставлять токен из expo-secure-store, когда сделаем авторизацию.
// На сайте это localStorage.getItem('access_token') → заголовок Authorization.

/** Файлы отдаются по id вложения. */
export function fileUrl(id: string): string {
  return `${BASE_URL}/api/v1/file/getFile/${id}`;
}
