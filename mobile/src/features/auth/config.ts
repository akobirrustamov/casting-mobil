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

/** Пока клиенты не заведены, кнопку Google показываем неактивной. */
export const isGoogleConfigured = Boolean(
  GOOGLE_CLIENT_IDS.web || GOOGLE_CLIENT_IDS.android
);
