import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

import { useAuthStore } from '@/features/auth/store';
import { useFeedLanguage } from '@/features/home/api';
import { DEFAULT_LANGUAGE, type Language } from '@/i18n';
import { api } from '@/lib/api';

import type { VideoSource, WatchInfo } from './types';

const LOCALE_PARAM: Record<Language, 'UZ' | 'RU' | 'EN'> = {
  uz: 'UZ',
  ru: 'RU',
  en: 'EN',
};

/** Эндпоинта нет на этом сервере — см. `HomeFeedUnavailableError`, причина та же. */
export class WatchUnavailableError extends Error {
  constructor() {
    super('/api/v1/app/watch недоступен на этом сервере');
    this.name = 'WatchUnavailableError';
  }
}

/**
 * Контент состоит из частей, а `/watch/content/{id}` умеет только цельные.
 *
 * Отдельный тип ошибки, потому что это НЕ поломка: сервер прав, спрашивать
 * надо конкретную серию через `/watch/{episodeId}`. Эндпоинта «дай список
 * серий» в `/api/v1/app/**` пока нет (docs/API.md §5), поэтому экран
 * честно говорит об этом, а не показывает ошибку.
 */
export class ContentIsMultiPartError extends Error {
  constructor() {
    super('Контент многосерийный, нужен /watch/{episodeId}');
    this.name = 'ContentIsMultiPartError';
  }
}

/** Контента с таким id нет — например, его сняли с публикации. */
export class ContentNotFoundError extends Error {
  constructor() {
    super('Контент не найден');
    this.name = 'ContentNotFoundError';
  }
}

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function str(value: unknown): string | null {
  return typeof value === 'string' && value.trim() !== '' ? value : null;
}

function mapSource(raw: unknown): VideoSource | null {
  const r = raw as Record<string, unknown>;
  const url = str(r?.url);
  // Без адреса файл не проиграть — такой источник для плеера не существует.
  if (url === null) return null;
  return {
    partNumber: num(r.partNumber),
    mediaId: num(r.mediaId),
    url,
    // ⚠️ Старая сборка бэкенда этого поля не отдаёт — тогда `null`
    // и плеер идёт по `url`. Приложение работает с обеими версиями.
    hlsUrl: str(r.hlsUrl),
    durationSeconds: num(r.durationSeconds),
  };
}

function mapWatch(raw: unknown): WatchInfo {
  const r = raw as Record<string, unknown> | null;

  // Старая сборка бэкенда отдаёт на этот адрес index.html со статусом 200.
  // Без проверки `allowed` стал бы `undefined` → «доступа нет» без причины.
  if (!r || typeof r !== 'object' || typeof r.allowed !== 'boolean') {
    throw new WatchUnavailableError();
  }

  return {
    episodeId: num(r.episodeId),
    contentId: num(r.contentId),
    episodeNumber: num(r.episodeNumber),
    durationSeconds: num(r.durationSeconds),
    title: str(r.title),
    orientation: str(r.orientation),
    allowed: r.allowed,
    reason: str(r.reason) ?? 'UNKNOWN',
    // Осторожная сторона: неизвестное значение = никакого действия предложить
    // не можем. Лучше не показать кнопку, чем показать неверную.
    requiredAction: str(r.requiredAction) ?? 'NONE',
    episodePrice: num(r.episodePrice),
    premierePrice: num(r.premierePrice),
    showAds: r.showAds === true,
    // ⚠️ Старая сборка бэкенда поля не отдаёт — тогда `null`, и экран
    // остаётся прежним: афиша под замком. Обе версии работают рядом.
    trailer: mapSource(r.trailer),
    sources: Array.isArray(r.sources)
      ? r.sources.map(mapSource).filter((s): s is VideoSource => s !== null)
      : [],
  };
}

/** 422 VALIDATION_ERROR / 404 — не сбои сети, а осмысленные ответы сервера. */
function translateError(error: unknown): never {
  if (axios.isAxiosError(error) && error.response) {
    const status = error.response.status;
    const code = (error.response.data as { code?: string } | undefined)?.code;

    if (status === 422 && code === 'VALIDATION_ERROR') throw new ContentIsMultiPartError();
    if (status === 404) throw new ContentNotFoundError();
  }
  throw error;
}

async function fetchWatchContent(
  contentId: number,
  language: Language
): Promise<WatchInfo> {
  try {
    const { data } = await api.get<unknown>(`/api/v1/app/watch/content/${contentId}`, {
      params: { locale: LOCALE_PARAM[language] },
    });
    return mapWatch(data);
  } catch (error) {
    return translateError(error);
  }
}

async function fetchWatchEpisode(
  episodeId: number,
  language: Language
): Promise<WatchInfo> {
  try {
    const { data } = await api.get<unknown>(`/api/v1/app/watch/${episodeId}`, {
      params: { locale: LOCALE_PARAM[language] },
    });
    return mapWatch(data);
  } catch (error) {
    return translateError(error);
  }
}

/** Повторять есть смысл только при сетевом сбое: остальные ответы не изменятся. */
function retryPolicy(failureCount: number, error: unknown): boolean {
  const settled =
    error instanceof WatchUnavailableError ||
    error instanceof ContentIsMultiPartError ||
    error instanceof ContentNotFoundError;
  return !settled && failureCount < 2;
}

/**
 * Кто спрашивает.
 *
 * Входит в ключ кэша: право на просмотр у гостя и у вошедшего человека
 * РАЗНОЕ. Без этого после входа экран показывал бы закрытый замок из кэша,
 * снятого до авторизации. По той же причине им пользуется список серий.
 */
export function useViewerKey(): string {
  return useAuthStore((s) => s.user?.id ?? (s.token ? 'token' : 'guest'));
}

/** Цельный контент — фильм, короткометражка, клип, выпуск шоу. */
export function useWatchContent(contentId: number | null) {
  const language = useFeedLanguage();
  const viewer = useViewerKey();

  return useQuery({
    queryKey: ['watch', 'content', contentId, language, viewer],
    queryFn: () => fetchWatchContent(contentId as number, language ?? DEFAULT_LANGUAGE),
    enabled: contentId !== null,
    // Право доступа меняется в момент покупки или входа — старый ответ
    // здесь опаснее лишнего запроса.
    staleTime: 0,
    retry: retryPolicy,
  });
}

/** Отдельная серия многосерийного контента. */
export function useWatchEpisode(episodeId: number | null) {
  const language = useFeedLanguage();
  const viewer = useViewerKey();

  return useQuery({
    queryKey: ['watch', 'episode', episodeId, language, viewer],
    queryFn: () => fetchWatchEpisode(episodeId as number, language ?? DEFAULT_LANGUAGE),
    enabled: episodeId !== null,
    staleTime: 0,
    retry: retryPolicy,
  });
}
