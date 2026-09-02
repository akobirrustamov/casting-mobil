import Constants, { AppOwnership } from 'expo-constants';

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
 * Заведены ли client ID.
 *
 * ⚠️ Проверяется именно `web`, хотя приложение android. Нативная библиотека
 * получает ID-токен с `aud` веб-клиента — по нему бэкенд его и проверяет.
 * Android-клиент в коде не участвует: Google узнаёт приложение по паре
 * «package + SHA-1», заведённой в консоли.
 */
const hasClientIds = Boolean(GOOGLE_CLIENT_IDS.web);

/**
 * Запущены ли мы в Expo Go.
 *
 * ⚠️ `Constants.executionEnvironment` здесь НЕ ГОДИТСЯ: и Expo Go, и
 * dev build отдают одно и то же `storeClient`, а различие тут
 * принципиальное — в dev build Google как раз работает.
 *
 * `appOwnership` помечен deprecated, но это единственный способ
 * отличить их без новой зависимости. Если поле уберут, значение станет
 * `null` — и мы посчитаем, что это не Expo Go: кнопка снова станет живой,
 * то есть вернётся сегодняшнее поведение, а не сломается что-то новое.
 */
export const isExpoGo = Constants.appOwnership === AppOwnership.Expo;

export type GoogleUnavailableReason = 'notConfigured' | 'expoGo';

/**
 * Почему кнопка Google неактивна, либо `null` — если активна.
 *
 * <h2>⚠️ В Expo Go входа через Google нет и быть не может</h2>
 * Причина сменилась вместе с реализацией, а вывод остался прежним.
 * Раньше это был браузерный `expo-auth-session`: в Expo Go он строил
 * redirect на схему самого Expo Go, а Google такие адреса отклонял.
 * Теперь вход нативный (`@react-native-google-signin/google-signin`), и
 * в Expo Go его просто НЕТ — там нельзя добавить нативный модуль.
 *
 * Поэтому в Expo Go кнопка ЗАГЛУШЕНА. Живая кнопка, которая всегда
 * заканчивается ошибкой, выглядит как поломка приложения — и человек
 * винит нас, а не среду запуска. Нужна сборка: `eas build`.
 */
export const googleUnavailableReason: GoogleUnavailableReason | null = !hasClientIds
  ? 'notConfigured'
  : isExpoGo
    ? 'expoGo'
    : null;

/** Кнопку Google показываем живой только когда она действительно сработает. */
export const isGoogleConfigured = googleUnavailableReason === null;
