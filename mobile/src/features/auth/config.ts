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

const hasClientIds = Boolean(GOOGLE_CLIENT_IDS.web || GOOGLE_CLIENT_IDS.android);

/**
 * Запущены ли мы в Expo Go.
 *
 * ⚠️ `Constants.executionEnvironment` здесь НЕ ГОДИТСЯ: и Expo Go, и
 * dev build отдают одно и то же `storeClient`, а различие тут
 * принципиальное — в dev build Google как раз работает.
 *
 * `appOwnership` помечен deprecated, но это единственный способ
 * отличить их без новой зависимости (`expo-application` в проекте нет).
 * Если поле уберут, значение станет `null` — и мы посчитаем, что это
 * не Expo Go: кнопка снова станет живой, то есть вернётся сегодняшнее
 * поведение, а не сломается что-то новое.
 */
export const isExpoGo = Constants.appOwnership === AppOwnership.Expo;

export type GoogleUnavailableReason = 'notConfigured' | 'expoGo';

/**
 * Почему кнопка Google неактивна, либо `null` — если активна.
 *
 * <h2>⚠️ Expo Go: Google отвечает «Доступ заблокирован»</h2>
 * `expo-auth-session` в Expo Go строит redirect на схему самого Expo Go,
 * а Google такие адреса отклоняет: `Ошибка 400: invalid_request`,
 * «приложение не соответствует политике OAuth 2.0». Никакая настройка
 * client ID это не лечит — нужен dev build
 * (`eas build --profile development`), где redirect строится из
 * package name: `uz.uzcasting.app:/oauthredirect`.
 *
 * Поэтому в Expo Go кнопка ЗАГЛУШЕНА. Живая кнопка, которая всегда
 * ведёт на страницу ошибки Google, выглядит как поломка приложения —
 * и человек винит нас, а не среду запуска.
 */
export const googleUnavailableReason: GoogleUnavailableReason | null = !hasClientIds
  ? 'notConfigured'
  : isExpoGo
    ? 'expoGo'
    : null;

/** Кнопку Google показываем живой только когда она действительно сработает. */
export const isGoogleConfigured = googleUnavailableReason === null;
