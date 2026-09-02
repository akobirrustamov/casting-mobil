/**
 * OAuth-клиенты Google. Как их создать и что вписывать —
 * docs/GOOGLE_AUTH.md.
 *
 * Значения кладём в .env (см. .env.example), в репозиторий не коммитим.
 * Client ID не секрет сам по себе, но плодить их копии по коду не стоит.
 */
export const GOOGLE_CLIENT_IDS = {
  web: process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID,
  android: process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID,
  ios: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
} as const;

/**
 * Пока веб-клиент не задан, кнопку Google показываем неактивной.
 *
 * ⚠️ Проверяется именно `web`, хотя приложение android. Нативная библиотека
 * получает ID-токен с `aud` веб-клиента — по нему бэкенд его и проверяет.
 * Android-клиент в коде не участвует: Google узнаёт приложение по паре
 * «package + SHA-1», заведённой в консоли.
 */
export const isGoogleConfigured = Boolean(GOOGLE_CLIENT_IDS.web);
