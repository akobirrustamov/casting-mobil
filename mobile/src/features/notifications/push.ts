import * as Notifications from 'expo-notifications';
import { router, useRootNavigationState } from 'expo-router';
import { useEffect, useRef } from 'react';
import { Linking } from 'react-native';

import { track } from '@/features/analytics/api';
import { useAuthStore } from '@/features/auth/store';
import { useDeviceStore } from '@/features/devices/store';

import { internalRoute } from './api';
import { registerPushToken } from './pushToken';

/**
 * Push-уведомления — Expo Push.
 *
 * <h2>Как устроено</h2>
 * 1. После входа (устройство уже зарегистрировано) спрашиваем разрешение
 *    и берём Expo push-токен.
 * 2. Токен уходит на бэкенд — `PUT /api/v1/app/devices/push-token`, он
 *    привязывается к ТЕКУЩЕМУ устройству (`X-Device-Id`).
 * 3. Админ жмёт «Yuborish» — бэкенд шлёт через Expo на все токены
 *    нужной аудитории, каждому на его языке.
 * 4. Нажатие на уведомление открывает то же, что карточка в «Xabarlar».
 *
 * <h2>⚠️ Android</h2>
 * Без `google-services.json` в сборке Expo-токен не выдаётся (FCM).
 * Здесь это не ошибка: регистрация молча не удаётся, приложение
 * работает, список «Xabarlar» — тоже. См. `docs/PUSH.md`.
 *
 * <h2>Ни одна ошибка не ломает экран</h2>
 * Всё обёрнуто в try: push — удобство, а не условие работы приложения.
 */

/** Данные, которые кладёт бэкенд (`NotificationPushService.payload`). */
type PushData = {
  notificationId?: unknown;
  linkType?: unknown;
  linkUrl?: unknown;
  targetType?: unknown;
  targetId?: unknown;
};

function str(v: unknown): string | null {
  return typeof v === 'string' && v.length > 0 ? v : null;
}

function num(v: unknown): number | null {
  if (typeof v === 'number' && Number.isFinite(v)) return v;
  if (typeof v === 'string' && v.trim() !== '' && Number.isFinite(Number(v))) return Number(v);
  return null;
}

/** Куда вести по нажатию. Ничего подходящего — список «Xabarlar». */
export function openPushTarget(raw: unknown): void {
  const data = (raw ?? {}) as PushData;
  const notificationId = num(data.notificationId);
  if (notificationId != null) {
    track({ type: 'NOTIFICATION_OPEN', targetId: notificationId });
  }

  const linkType = str(data.linkType);
  const route = internalRoute({
    linkType,
    targetType: str(data.targetType),
    targetId: num(data.targetId),
  });
  if (route) {
    if (notificationId != null) track({ type: 'NOTIFICATION_CLICK', targetId: notificationId });
    router.push(route as never);
    return;
  }

  const url = str(data.linkUrl);
  if (linkType === 'EXTERNAL' && url) {
    if (notificationId != null) track({ type: 'NOTIFICATION_CLICK', targetId: notificationId });
    Linking.openURL(url).catch(() => {});
    return;
  }

  router.push('/messages');
}

/**
 * Регистрация токена и обработка нажатий — один раз, в корневом layout.
 *
 * @param ready навигатор смонтирован и splash снят: раньше переход
 *              сбил бы стартовый маршрут
 */
export function usePushNotifications(ready: boolean): void {
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const deviceStatus = useDeviceStore((s) => s.status);

  // Токен — только когда устройство принято (не упёрлось в лимит).
  useEffect(() => {
    if (!isAuthorized || deviceStatus !== 'registered') return;
    void registerPushToken();
  }, [isAuthorized, deviceStatus]);

  // Нажатие: и холодный старт из уведомления, и нажатие при открытом приложении.
  const response = Notifications.useLastNotificationResponse();
  const handled = useRef<string | null>(null);
  const navigationState = useRootNavigationState();
  const navigatorReady = Boolean(navigationState?.key);

  useEffect(() => {
    if (!response || !ready || !navigatorReady || !isAuthorized) return;
    if (response.actionIdentifier !== Notifications.DEFAULT_ACTION_IDENTIFIER) return;

    const id = response.notification.request.identifier;
    if (handled.current === id) return;
    handled.current = id;

    openPushTarget(response.notification.request.content.data);
    try {
      Notifications.clearLastNotificationResponse();
    } catch {
      // Не критично: повтор отсекает `handled`.
    }
  }, [response, ready, navigatorReady, isAuthorized]);
}
