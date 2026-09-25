import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { AppState } from 'react-native';

import { useAuthStore } from '@/features/auth/store';
import { feedLocale } from '@/features/home/api';
import { DEFAULT_LANGUAGE, isSupportedLanguage, type Language } from '@/i18n';
import { api, mediaUrl } from '@/lib/api';

import { Notifications } from './module';

/**
 * Уведомления — `GET /api/v1/app/notifications`.
 *
 * <h2>Что здесь чинится</h2>
 * Модуль на бэкенде был готов целиком: таблица, переводы на три языка,
 * расписание, страница в админке. В приложении экран «Xabarlar» был
 * пустой заглушкой — то есть написанное админом не видел никто.
 *
 * <h2>Push</h2>
 * Тот же отправленный админом текст приходит и push-уведомлением
 * (`./push.ts`); нажатие ведёт туда же, куда карточка в списке —
 * маршрут считает одна функция `internalRoute`.
 *
 * <h2>«Прочитано» (25.09.2026)</h2>
 * Заказчик: push приходит, а на колокольчике нет красного знака «есть
 * новое»; открыл «Xabarlar» — всё должно стать прочитанным. Отметки
 * хранит бэкенд (`cms_notification_read`, V43) — по человеку, а не по
 * телефону: прочитанное на одном устройстве прочитано и на другом.
 *
 * - `useUnreadCount` — число на колокольчике;
 * - `markAllRead` — экран открыт;
 * - `markRead(id)` — нажали push и ушли сразу по ссылке, минуя список.
 *
 * <h2>Два вида (25.09.2026)</h2>
 * Админ выбирает тип: `APP_NOTIFICATION` — колокольчик на главной,
 * `CASTING_NOTIFICATION` — колокольчик во вкладке «Casting». Список,
 * число и «прочитано» считаются отдельно для каждого вида (`?type=`).
 */
export type NotificationKind = 'APP_NOTIFICATION' | 'CASTING_NOTIFICATION';

export const APP_KIND: NotificationKind = 'APP_NOTIFICATION';
export const CASTING_KIND: NotificationKind = 'CASTING_NOTIFICATION';

/** Значение параметра маршрута `/messages?type=…` → вид. */
export function kindFromParam(value: unknown): NotificationKind {
  return value === 'casting' || value === CASTING_KIND ? CASTING_KIND : APP_KIND;
}

/** Экран списка для вида. */
export function messagesRoute(kind: NotificationKind): string {
  return kind === CASTING_KIND ? '/messages?type=casting' : '/messages';
}

export type AppNotification = {
  id: number;
  /** Вид; старый бэкенд не присылает — общий. */
  kind: NotificationKind;
  title: string | null;
  body: string | null;
  /** Готовый адрес картинки или `undefined`. */
  imageUrl: string | undefined;
  sentAt: string | null;
  /** `NONE` / `INTERNAL` / `EXTERNAL`. */
  linkType: string | null;
  linkUrl: string | null;
  targetType: string | null;
  targetId: number | null;
  /** Этот человек уже прочитал. */
  read: boolean;
};

function str(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function map(raw: unknown): AppNotification {
  const r = (raw ?? {}) as Record<string, unknown>;
  return {
    id: num(r.id) ?? 0,
    kind: r.type === CASTING_KIND ? CASTING_KIND : APP_KIND,
    title: str(r.title),
    body: str(r.body),
    // Адрес собирается здесь, а не на экране: правило «id → URL» уже
    // живёт в `lib/api`, и второй его копии быть не должно.
    imageUrl: mediaUrl(num(r.imageId) ?? undefined),
    sentAt: str(r.sentAt),
    linkType: str(r.linkType),
    linkUrl: str(r.linkUrl),
    targetType: str(r.targetType),
    targetId: num(r.targetId),
    // Старый бэкенд поля не присылает — считаем прочитанным, иначе
    // все сообщения разом загорелись бы «новыми».
    read: r.read !== false,
  };
}

function useLanguage(): Language {
  const { i18n } = useTranslation();
  return isSupportedLanguage(i18n.language) ? i18n.language : DEFAULT_LANGUAGE;
}

export async function fetchNotifications(
  language: Language,
  kind: NotificationKind
): Promise<AppNotification[]> {
  const { data } = await api.get<unknown[]>('/api/v1/app/notifications', {
    params: { locale: feedLocale(language), type: kind },
  });
  // Старый бэкенд `type` игнорирует — фильтруем и здесь.
  return (Array.isArray(data) ? data : []).map(map).filter((item) => item.kind === kind);
}

export function useNotifications(kind: NotificationKind = APP_KIND) {
  const language = useLanguage();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  return useQuery({
    queryKey: ['notifications', kind, userId, language],
    queryFn: () => fetchNotifications(language, kind),
    enabled: isAuthorized,
  });
}

const UNREAD_URL = '/api/v1/app/notifications/unread-count';

/**
 * Отметка ушла на сервер — пора перечитать число.
 *
 * ⚠️ Отдельный канал, а не `queryClient`: `markRead` зовёт `push.ts`
 * из обработчика нажатия, а он живёт в корневом layout ВЫШЕ
 * `QueryClientProvider`. Подписчик — `useUnreadCount`.
 */
const readListeners = new Set<() => void>();

function readChanged(): void {
  readListeners.forEach((listener) => listener());
}

/** Сколько непрочитанных — для колокольчика. */
export function useUnreadCount(kind: NotificationKind = APP_KIND) {
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  const query = useQuery({
    queryKey: ['notifications', 'unread', kind, userId],
    queryFn: async () => {
      const { data } = await api.get<{ count?: unknown }>(UNREAD_URL, { params: { type: kind } });
      return num(data?.count) ?? 0;
    },
    enabled: isAuthorized,
  });

  const { refetch } = query;
  useEffect(() => {
    if (!isAuthorized) return;
    const again = () => void refetch();

    // Пришёл push при открытом приложении — знак загорается сразу.
    const received = Notifications?.addNotificationReceivedListener(again);
    // Вернулись в приложение (push пришёл, пока оно было свёрнуто).
    const appState = AppState.addEventListener('change', (state) => {
      if (state === 'active') again();
    });
    readListeners.add(again);

    return () => {
      received?.remove();
      appState.remove();
      readListeners.delete(again);
    };
  }, [isAuthorized, refetch]);

  return isAuthorized ? (query.data ?? 0) : 0;
}

/**
 * Экран «Xabarlar» открыт — всё видимое прочитано.
 *
 * Список при этом НЕ перечитывается: подсветка «новое» остаётся до
 * ухода с экрана, иначе человек не успел бы увидеть, что именно пришло.
 */
export function useMarkAllRead(kind: NotificationKind = APP_KIND) {
  const client = useQueryClient();
  const userId = useAuthStore((s) => s.user?.id ?? null);

  return useMutation({
    mutationFn: async () => {
      await api.post('/api/v1/app/notifications/read', null, { params: { type: kind } });
    },
    onSuccess: () => {
      client.setQueryData(['notifications', 'unread', kind, userId], 0);
      // Из шторки — тоже: прочитанное там больше не нужно.
      Notifications?.dismissAllNotificationsAsync().catch(() => {});
      Notifications?.setBadgeCountAsync(0).catch(() => {});
    },
  });
}

/** Одно сообщение прочитано — нажали push. Ошибка не мешает переходу. */
export async function markRead(notificationId: number): Promise<void> {
  try {
    await api.post(`/api/v1/app/notifications/${notificationId}/read`);
    readChanged();
  } catch {
    // Не страшно: сообщение отметится, когда человек откроет список.
  }
}

/**
 * Внутренняя ссылка → маршрут приложения.
 *
 * ⚠️ Возвращает `null` для всего, чего в приложении ещё нет. Экран не
 * должен уводить в несуществующий маршрут: expo-router на такое
 * отвечает пустым белым экраном без объяснения.
 */
export function internalRoute(
  item: Pick<AppNotification, 'linkType' | 'targetType' | 'targetId'>
): string | null {
  if (item.linkType !== 'INTERNAL' || item.targetId == null) return null;

  switch (item.targetType) {
    case 'CONTENT':
      return `/content/${item.targetId}`;
    case 'EPISODE':
      return `/episode/${item.targetId}`;
    case 'CREATOR':
      return `/creator/${item.targetId}`;
    default:
      return null;
  }
}
