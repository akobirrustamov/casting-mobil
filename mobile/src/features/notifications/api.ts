import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { useAuthStore } from '@/features/auth/store';
import { feedLocale } from '@/features/home/api';
import { DEFAULT_LANGUAGE, isSupportedLanguage, type Language } from '@/i18n';
import { api, mediaUrl } from '@/lib/api';

/**
 * Уведомления — `GET /api/v1/app/notifications`.
 *
 * <h2>Что здесь чинится</h2>
 * Модуль на бэкенде был готов целиком: таблица, переводы на три языка,
 * расписание, страница в админке. В приложении экран «Xabarlar» был
 * пустой заглушкой — то есть написанное админом не видел никто.
 *
 * <h2>Push пока нет</h2>
 * FCM не подключён: сообщение записывается, но не отправляется. Для
 * списка внутри приложения это не помеха — сообщение лежит в базе, и
 * его можно прочитать. Когда push появится, экран менять не придётся.
 *
 * <h2>⚠️ «Прочитано» не отслеживается</h2>
 * Отметка требует отдельной таблицы (кто что прочитал) и записи на
 * каждое открытие. В первой версии её нет намеренно: сообщений мало, а
 * счётчик непрочитанного стоил бы целой таблицы и синхронизации.
 */
export type AppNotification = {
  id: number;
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
  };
}

function useLanguage(): Language {
  const { i18n } = useTranslation();
  return isSupportedLanguage(i18n.language) ? i18n.language : DEFAULT_LANGUAGE;
}

export async function fetchNotifications(language: Language): Promise<AppNotification[]> {
  const { data } = await api.get<unknown[]>('/api/v1/app/notifications', {
    params: { locale: feedLocale(language) },
  });
  return (Array.isArray(data) ? data : []).map(map);
}

export function useNotifications() {
  const language = useLanguage();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  return useQuery({
    queryKey: ['notifications', userId, language],
    queryFn: () => fetchNotifications(language),
    enabled: isAuthorized,
  });
}
