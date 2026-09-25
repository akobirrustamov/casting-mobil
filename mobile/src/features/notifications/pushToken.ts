import type { AxiosRequestConfig } from 'axios';
import Constants from 'expo-constants';
import * as Device from 'expo-device';
import { Platform } from 'react-native';

import { api } from '@/lib/api';

import { Notifications } from './module';

/**
 * Push-токен этого телефона ↔ бэкенд.
 *
 * Отдельно от `push.ts`: сюда ходит `auth/store` (выход), а `push.ts`
 * сам зависит от стора — вместе был бы цикл импортов.
 *
 * Подробности устройства push — в `push.ts`.
 */

/** Канал Android. Бэкенд шлёт с тем же `channelId`. */
export const ANDROID_CHANNEL_ID = 'default';

const PUSH_TOKEN_URL = '/api/v1/app/devices/push-token';

/** Пришедшее при открытом приложении — тоже показываем баннером. */
Notifications?.setNotificationHandler({
  handleNotification: async () => ({
    shouldPlaySound: true,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

/** Последний отправленный на сервер токен — не шлём одно и то же дважды за запуск. */
let sentToken: string | null = null;

/**
 * Получить токен и отдать его бэкенду.
 *
 * @returns токен или `null` (эмулятор, отказ в разрешении, нет FCM, нет сети)
 */
export async function registerPushToken(): Promise<string | null> {
  try {
    // На эмуляторе Expo-токена нет — не просим разрешение зря.
    // В Expo Go модуля нет вовсе (см. `module.ts`).
    if (!Device.isDevice || !Notifications) return null;

    if (Platform.OS === 'android') {
      // Канал должен существовать ДО запроса разрешения: на Android 13+
      // без канала системный запрос не появляется.
      await Notifications.setNotificationChannelAsync(ANDROID_CHANNEL_ID, {
        name: 'UzCasting',
        importance: Notifications.AndroidImportance.HIGH,
        vibrationPattern: [0, 250, 250, 250],
      });
    }

    const current = await Notifications.getPermissionsAsync();
    let granted = current.granted;
    if (!granted && current.canAskAgain) {
      granted = (await Notifications.requestPermissionsAsync()).granted;
    }
    if (!granted) return null;

    const projectId =
      Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
    const { data: token } = await Notifications.getExpoPushTokenAsync({ projectId });

    if (token && token !== sentToken) {
      await api.put(PUSH_TOKEN_URL, { token });
      sentToken = token;
    }
    return token ?? null;
  } catch {
    return null;
  }
}

/**
 * Выход: этому телефону больше не присылать.
 *
 * ⚠️ Вызывается ДО удаления токена авторизации — запрос идёт от имени
 * уходящего пользователя. Долго не ждём: выход не должен зависать из-за
 * сети.
 */
export async function unregisterPushToken(): Promise<void> {
  if (!sentToken) return;
  sentToken = null;
  try {
    // ⚠️ `_retried`: на 401 НЕ продлевать сессию. `signOut` сам
    // вызывается из продления (`renew`), и интерцептор ждал бы то же
    // продление, которое ждёт этот запрос, — взаимная блокировка.
    await api.delete(PUSH_TOKEN_URL, { timeout: 3000, _retried: true } as AxiosRequestConfig);
  } catch {
    // Не удалось — бэкенд всё равно отвяжет токен, когда на этом
    // телефоне войдёт другой человек.
  }
}

