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
const WRITE_ALLOWLIST = [
  '/api/v1/auth/',
  // ⚠️ OTP переехал в новое пространство (`AppAuthController`).
  //
  // Без этой строки вход по SMS ломался бы ТИХО: интерцептор
  // `READ_ONLY` роняет запрос ДО отправки, и пользователь видел бы
  // не ответ сервера, а внутреннюю ошибку клиента.
  '/api/v1/app/auth/',
];

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
});

/**
 * Токен для заголовка `Authorization`.
 *
 * Живёт здесь, а не в сторе, по направлению зависимостей: `lib` не знает про
 * `features`. Значение сюда проталкивает `features/auth/store` — одной
 * подпиской, чтобы вход, выход и восстановление из хранилища не требовали
 * каждый своего вызова (пропущенный вызов дал бы молчаливый 401).
 */
let authToken: string | null = null;

export function setAuthToken(token: string | null): void {
  authToken = token;
}

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

  if (authToken) {
    config.headers.set('Authorization', `Bearer ${authToken}`);
  }

  return config;
});

/**
 * Заголовки для плеера и любых запросов мимо axios.
 *
 * Картинки бэкенд отдаёт всем, а видео проверяет право доступа
 * (`AccessService.canReadMedia`) и без токена вернёт отказ — поэтому
 * `expo-video` обязан идти с тем же заголовком, что и остальные запросы.
 */
export function authHeaders(): Record<string, string> {
  return authToken ? { Authorization: `Bearer ${authToken}` } : {};
}

/** Файлы отдаются по id вложения. */
export function fileUrl(id: string): string {
  return `${BASE_URL}/api/v1/file/getFile/${id}`;
}

/**
 * Метка запуска — только для локального бэкенда.
 *
 * <h2>Зачем</h2>
 * `expo-image` кэширует картинку по URL, и это правильно: в проде id медиа
 * постоянен, а его содержимое неизменно — новая загрузка получает новый id.
 *
 * На локальном стенде наоборот: база H2 живёт в памяти и пересоздаётся при
 * каждом рестарте, раздавая те же самые id ДРУГИМ файлам. Адрес
 * `/media/90/raw` не меняется, содержимое меняется — и телефон продолжает
 * показывать вчерашнюю заглушку, сколько ни перезапускай бэкенд.
 *
 * Поэтому в дев-режиме к адресу добавляется метка запуска приложения.
 * В проде она пустая и на кэш никак не влияет.
 */
const MEDIA_CACHE_BUST = READ_ONLY ? '' : `?v=${Date.now()}`;

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
  return id == null
    ? undefined
    : `${BASE_URL}/api/v1/app/media/${id}/raw${MEDIA_CACHE_BUST}`;
}
