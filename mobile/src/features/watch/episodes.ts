import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

import { useFeedLanguage } from '@/features/home/api';
import type { Language } from '@/i18n';
import { api } from '@/lib/api';

import { ContentNotFoundError, WatchUnavailableError, useViewerKey } from './api';
import type { RequiredAction, WatchReason } from './types';

/**
 * Контракт `GET /api/v1/app/content/{id}/episodes`.
 *
 * Зеркало `ContentController.EpisodeListResponse`.
 *
 * ⚠️ Ссылок на видео здесь НЕТ и быть не должно: список говорит, что есть и
 * что кому открыто, а адрес файла выдаёт только `/watch/{episodeId}` — после
 * подтверждения права. Иначе списка хватало бы, чтобы забрать платную серию.
 */
export type EpisodeCard = {
  id: number;
  episodeNumber: number | null;
  seasonId: number | null;
  seasonNumber: number | null;
  title: string | null;
  durationSeconds: number | null;
  thumbnailMediaId: number | null;
  accessPolicy: string | null;

  /** Решение сервера — то же самое, что вернёт `/watch` (ТЗ §37). */
  allowed: boolean;
  reason: WatchReason | null;
  requiredAction: RequiredAction | null;
  episodePrice: number | null;
};

export type SeasonCard = {
  id: number;
  seasonNumber: number | null;
  title: string | null;
  posterMediaId: number | null;
};

export type EpisodeList = {
  contentId: number | null;
  structureType: string | null;
  /** Заполняется только у сезонного контента. */
  seasons: SeasonCard[];
  episodes: EpisodeCard[];
};

const LOCALE_PARAM: Record<Language, 'UZ' | 'RU' | 'EN'> = {
  uz: 'UZ',
  ru: 'RU',
  en: 'EN',
};

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function str(value: unknown): string | null {
  return typeof value === 'string' && value.trim() !== '' ? value : null;
}

/** Серия без id не открывается — показывать её незачем. */
function mapEpisode(raw: unknown): EpisodeCard | null {
  const r = raw as Record<string, unknown>;
  const id = num(r?.id);
  if (id === null) return null;

  return {
    id,
    episodeNumber: num(r.episodeNumber),
    seasonId: num(r.seasonId),
    seasonNumber: num(r.seasonNumber),
    title: str(r.title),
    durationSeconds: num(r.durationSeconds),
    thumbnailMediaId: num(r.thumbnailMediaId),
    accessPolicy: str(r.accessPolicy),
    // Осторожная сторона: пока сервер не сказал «можно» — считаем, что нельзя.
    allowed: r.allowed === true,
    reason: str(r.reason),
    requiredAction: str(r.requiredAction),
    episodePrice: num(r.episodePrice),
  };
}

function mapSeason(raw: unknown): SeasonCard | null {
  const r = raw as Record<string, unknown>;
  const id = num(r?.id);
  if (id === null) return null;
  return {
    id,
    seasonNumber: num(r.seasonNumber),
    title: str(r.title),
    posterMediaId: num(r.posterMediaId),
  };
}

function mapList(raw: unknown): EpisodeList {
  const r = raw as Record<string, unknown> | null;

  // Старая сборка бэкенда отдаёт на этот адрес index.html со статусом 200.
  if (!r || typeof r !== 'object' || !Array.isArray(r.episodes)) {
    throw new WatchUnavailableError();
  }

  return {
    contentId: num(r.contentId),
    structureType: str(r.structureType),
    seasons: Array.isArray(r.seasons)
      ? r.seasons.map(mapSeason).filter((s): s is SeasonCard => s !== null)
      : [],
    episodes: r.episodes.map(mapEpisode).filter((e): e is EpisodeCard => e !== null),
  };
}

async function fetchEpisodes(contentId: number, language: Language): Promise<EpisodeList> {
  try {
    const { data } = await api.get<unknown>(`/api/v1/app/content/${contentId}/episodes`, {
      params: { locale: LOCALE_PARAM[language] },
    });
    return mapList(data);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      throw new ContentNotFoundError();
    }
    throw error;
  }
}

/**
 * Серии контента.
 *
 * Зритель входит в ключ кэша: купленная серия показана открытой, а после
 * выхода из аккаунта тот же список выглядит иначе.
 */
export function useEpisodes(contentId: number | null) {
  const language = useFeedLanguage();
  const viewer = useViewerKey();

  return useQuery({
    queryKey: ['episodes', contentId, language, viewer],
    queryFn: () => fetchEpisodes(contentId as number, language),
    enabled: contentId !== null,
    // Право меняется покупкой — старый список здесь опаснее лишнего запроса.
    staleTime: 0,
    retry: (failureCount, error) =>
      !(error instanceof WatchUnavailableError) &&
      !(error instanceof ContentNotFoundError) &&
      failureCount < 2,
  });
}

/** Серии одного сезона — порядок сервер уже задал. */
export function episodesOfSeason(
  list: EpisodeList | undefined,
  seasonId: number | null
): EpisodeCard[] {
  const all = list?.episodes ?? [];
  if (seasonId === null) return all;
  return all.filter((e) => e.seasonId === seasonId);
}
