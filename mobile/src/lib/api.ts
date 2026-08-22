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

/**
 * Исключения из read-only: авторизация обязана писать (создание аккаунта,
 * выдача токена). Всё остальное — каталоги, заявки, покупки — под запретом,
 * пока не появится тестовый контур.
 */
const WRITE_ALLOWLIST = ['/api/v1/auth/'];

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
});

api.interceptors.request.use((config) => {
  const method = (config.method ?? 'get').toLowerCase();

  const url = config.url ?? '';
  const isAllowed = WRITE_ALLOWLIST.some((prefix) => url.startsWith(prefix));

  if (READ_ONLY && !SAFE_METHODS.includes(method) && !isAllowed) {
    throw new Error(
      `[READ_ONLY] Запрос ${method.toUpperCase()} ${url} заблокирован. ` +
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

/**
 * Медиа новой платформы — другой namespace, чем `fileUrl`.
 *
 * `fileUrl` — вложения старого кастингового модуля (`Attachment`, UUID),
 * здесь — `MediaAsset` нового бэкенда (числовой id). Это разные хранилища,
 * перепутать нельзя: id из фида в `fileUrl` даст 404.
 *
 * Картинки отдаются без токена, видео проверяет entitlement и на отказ
 * возвращает 404 — поэтому в постерах достаточно обычного URL.
 */
export function mediaUrl(id: number | null | undefined): string | undefined {
  return id == null ? undefined : `${BASE_URL}/api/v1/app/media/${id}/raw`;
}
