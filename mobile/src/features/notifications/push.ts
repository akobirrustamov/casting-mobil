import { router, useRootNavigationState } from 'expo-router';
import { useEffect, useRef } from 'react';
import { Linking } from 'react-native';

import { track } from '@/features/analytics/api';
import { useAuthStore } from '@/features/auth/store';
import { useDeviceStore } from '@/features/devices/store';

import { CASTING_KIND, internalRoute, kindFromParam, markRead, messagesRoute } from './api';
import { Notifications } from './module';
import { registerPushToken } from './pushToken';

/**
 * Хук последнего нажатия. Выбирается ОДИН раз при загрузке модуля, поэтому
 * порядок хуков между рендерами не меняется. В Expo Go — всегда `null`.
 */
const useLastNotificationResponse: () => ReturnType<
  NonNullable<typeof Notifications>['useLastNotificationResponse']
> = Notifications ? Notifications.useLastNotificationResponse : () => null;

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
  /** `APP_NOTIFICATION` / `CASTING_NOTIFICATION`. */
  type?: unknown;
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

/** Куда вести по нажатию. Ничего подходящего — список своего вида. */
export function openPushTarget(raw: unknown): void {
  const data = (raw ?? {}) as PushData;
  const notificationId = num(data.notificationId);
  if (notificationId != null) {
    track({ type: 'NOTIFICATION_OPEN', targetId: notificationId });
    // Сразу по ссылке, мимо списка — иначе знак на колокольчике так и
    // горел бы из-за сообщения, которое человек уже открыл.
    void markRead(notificationId);
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

  // Сначала — вкладка своего раздела, поверх — список этого вида: кастинговое
  // уведомление открывается в «Casting», общее — на главной, и «назад»
  // возвращает именно туда.
  const kind = kindFromParam(data.type);
  router.navigate((kind === CASTING_KIND ? '/(tabs)/casting' : '/(tabs)') as never);
  router.push(messagesRoute(kind) as never);
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
  const response = useLastNotificationResponse();
  const handled = useRef<string | null>(null);
  const navigationState = useRootNavigationState();
  const navigatorReady = Boolean(navigationState?.key);

  useEffect(() => {
    if (!Notifications || !response || !ready || !navigatorReady || !isAuthorized) return;
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
