import Constants, { ExecutionEnvironment } from 'expo-constants';

/**
 * `expo-notifications` — только там, где он работает.
 *
 * ⚠️ В Expo Go (SDK 53+) на Android сам ИМПОРТ модуля бросает ошибку:
 * удалённые push оттуда убраны. Статический `import` ронял весь
 * `_layout` — приложение в Expo Go не открывалось вовсе. Поэтому модуль
 * грузится через `require` и в Expo Go остаётся `null`: push там всё
 * равно недоступны, остальное приложение работает.
 */
type NotificationsModule = typeof import('expo-notifications');

function load(): NotificationsModule | null {
  if (Constants.executionEnvironment === ExecutionEnvironment.StoreClient) return null;
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('expo-notifications') as NotificationsModule;
  } catch {
    return null;
  }
}

export const Notifications = load();
