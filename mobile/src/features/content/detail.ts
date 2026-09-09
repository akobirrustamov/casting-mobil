import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

import type { Orientation } from '@/features/content/orientation';
import { useFeedLanguage } from '@/features/home/api';
import { useViewerKey } from '@/features/watch/api';
import type { Language } from '@/i18n';
import { api } from '@/lib/api';

/**
 * Карточка контента — `GET /api/v1/app/content/{id}`.
 *
 * Зеркало `ContentController.detail` и `Cms/Dto/ContentDetailDto` на
 * бэкенде. При изменении DTO править здесь же.
 *
 * <h2>Что этот запрос чинит</h2>
 * Экран контента собирал данные из `/watch/**` и КЭША ГЛАВНОЙ. Оттуда
 * приходили только афиша и короткое описание — больше в карточке ряда
 * ничего и нет. Год, жанры, полное описание и актёры лежат в базе, но до
 * приложения не доезжали вовсе, а по прямой ссылке кэш пуст и экран
 * открывался вообще без афиши.
 *
 * <h2>⚠️ Ссылок на видео здесь НЕТ</h2>
 * Это КАТАЛОЖНЫЕ данные, их видит и гость. Адрес файла выдаёт только
 * `/watch/**` — после подтверждения права.
 */

export type CastMember = {
  creatorId: number | null;
  slug: string | null;
  name: string | null;
  photoMediaId: number | null;
  /** ACTOR, DIRECTOR, PRODUCER … — подпись переводится в приложении. */
  profession: string | null;
  /** Имя персонажа: на макете стоит под именем актёра. */
  characterName: string | null;
};

export type ContentDetail = {
  id: number;
  slug: string | null;

  title: string | null;
  shortDescription: string | null;
  description: string | null;

  contentType: string | null;
  /** SINGLE, EPISODIC, SEASONAL — по нему кнопка ведёт в серии или в плеер. */
  structureType: string | null;
  orientation: Orientation | null;
  accessPolicy: string | null;

  ageRating: string | null;
  /** Год выхода. `null` — даты в базе нет, и придумывать её нельзя. */
  year: number | null;
  /**
   * Язык оригинала («uz», «ru», …).
   *
   * ⚠️ Это НЕ страна. На макете в этой строке стоит «O'zbekiston», но
   * страны в базе нет, и выводить её из языка нельзя: корейский сериал
   * с русской озвучкой остаётся корейским. Показываем название языка.
   */
  language: string | null;

  durationSeconds: number | null;
  episodeCount: number | null;
  seasonCount: number | null;

  posterMediaId: number | null;
  /** Широкий кадр для шапки. `null` — рисуем афишу. */
  coverMediaId: number | null;
  trailerMediaId: number | null;
  /** Кадры галереи — блок «Qiziq sahnalar». */
  galleryMediaIds: number[];

  genres: string[];

  viewCount: number | null;
  likeCount: number | null;
  commentCount: number | null;
  starsReceived: number | null;

  /**
   * UZCASTING Coin — вторая донатная валюта.
   *
   * ⚠️ Со звёздами в одно число не складывается: это разные единицы, и
   * сумма ничего не значит. На бэкенде то же правило охраняет тест
   * отчётов.
   */
  coinsReceived: number | null;

  liked: boolean;

  cast: CastMember[];
};

export type Donor = {
  rank: number;
  name: string | null;
  /** Готовый адрес картинки (Google), а не id медиа. */
  avatarUrl: string | null;
  stars: number;
};

export type DonorList = {
  /**
   * Всего звёзд у контента.
   *
   * ⚠️ Больше суммы десяти строк — это норма, а не ошибка: в списке
   * только верхушка. Складывать одно с другим нельзя.
   */
  starsReceived: number;
  donors: Donor[];
};

/** Старая сборка бэкенда отдаёт на этот адрес index.html со статусом 200. */
export class ContentDetailUnavailableError extends Error {
  constructor() {
    super('/api/v1/app/content/{id} недоступен на этом сервере');
    this.name = 'ContentDetailUnavailableError';
  }
}

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

function ids(value: unknown): number[] {
  return Array.isArray(value)
    ? value.filter((v): v is number => typeof v === 'number' && Number.isFinite(v))
    : [];
}

function strings(value: unknown): string[] {
  return Array.isArray(value)
    ? value.filter((v): v is string => typeof v === 'string' && v.trim() !== '')
    : [];
}

function mapCast(raw: unknown): CastMember {
  const r = raw as Record<string, unknown>;
  return {
    creatorId: num(r?.creatorId),
    slug: str(r?.slug),
    name: str(r?.name),
    photoMediaId: num(r?.photoMediaId),
    profession: str(r?.profession),
    characterName: str(r?.characterName),
  };
}

/** ⚠️ Экспортируется РАДИ ТЕСТА — как `mapContent` в `home/api`. */
export function mapDetail(raw: unknown): ContentDetail {
  const r = raw as Record<string, unknown> | null;

  // ⚠️ Проверяем `id`, а не просто «объект»: index.html разбирается в
  // строку, а пустой ответ — в `null`, и без этой проверки экран получил
  // бы карточку без единого поля и молча показал пустоту.
  const id = num((r as Record<string, unknown>)?.id);
  if (!r || typeof r !== 'object' || id === null) {
    throw new ContentDetailUnavailableError();
  }

  return {
    id,
    slug: str(r.slug),
    title: str(r.title),
    shortDescription: str(r.shortDescription),
    description: str(r.description),
    contentType: str(r.contentType),
    structureType: str(r.structureType),
    orientation: str(r.orientation),
    accessPolicy: str(r.accessPolicy),
    ageRating: str(r.ageRating),
    year: num(r.year),
    language: str(r.language),
    durationSeconds: num(r.durationSeconds),
    episodeCount: num(r.episodeCount),
    seasonCount: num(r.seasonCount),
    posterMediaId: num(r.posterMediaId),
    coverMediaId: num(r.coverMediaId),
    trailerMediaId: num(r.trailerMediaId),
    galleryMediaIds: ids(r.galleryMediaIds),
    genres: strings(r.genres),
    viewCount: num(r.viewCount),
    likeCount: num(r.likeCount),
    commentCount: num(r.commentCount),
    starsReceived: num(r.starsReceived),
    coinsReceived: num(r.coinsReceived),
    liked: r.liked === true,
    cast: Array.isArray(r.cast) ? r.cast.map(mapCast) : [],
  };
}

/** ⚠️ Экспортируется ради теста — см. `mapDetail`. */
export function mapDonors(raw: unknown): DonorList {
  const r = raw as Record<string, unknown> | null;
  const list = r?.donors;

  if (!r || typeof r !== 'object' || !Array.isArray(list)) {
    throw new ContentDetailUnavailableError();
  }

  return {
    starsReceived: num(r.starsReceived) ?? 0,
    donors: list.map((raw, i) => {
      const d = raw as Record<string, unknown>;
      return {
        // Место считает сервер. Пересчитывать его по индексу значило бы
        // получить другой порядок, стоит списку прийти частями.
        rank: num(d?.rank) ?? i + 1,
        name: str(d?.name),
        avatarUrl: str(d?.avatarUrl),
        stars: num(d?.stars) ?? 0,
      };
    }),
  };
}

async function fetchDetail(
  contentId: number,
  language: Language
): Promise<ContentDetail> {
  try {
    const { data } = await api.get<unknown>(`/api/v1/app/content/${contentId}`, {
      params: { locale: LOCALE_PARAM[language] },
    });
    return mapDetail(data);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      throw new ContentDetailUnavailableError();
    }
    throw error;
  }
}

async function fetchDonors(contentId: number, limit: number): Promise<DonorList> {
  const { data } = await api.get<unknown>(`/api/v1/app/content/${contentId}/donors`, {
    params: { limit },
  });
  return mapDonors(data);
}

/**
 * Карточка контента.
 *
 * <h2>Почему зритель в ключе кэша</h2>
 * В ответе есть `liked` — он про КОНКРЕТНОГО человека. Без зрителя в
 * ключе после входа сердце осталось бы серым, потому что показывался бы
 * ответ, снятый гостем.
 *
 * ⚠️ Экран обязан работать и без этого запроса: на старой сборке
 * бэкенда адреса нет вовсе. Поэтому неудача здесь не ошибка экрана —
 * просто полей меньше.
 */
export function useContentDetail(contentId: number | null) {
  const language = useFeedLanguage();
  const viewer = useViewerKey();

  return useQuery({
    queryKey: ['content-detail', contentId, language, viewer],
    queryFn: () => fetchDetail(contentId as number, language),
    enabled: contentId !== null,
    // Каталожные данные меняются редко — в отличие от права доступа.
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, error) =>
      !(error instanceof ContentDetailUnavailableError) && failureCount < 2,
  });
}

/** Сколько строк рейтинга помещается на макете. */
export const TOP_DONORS = 10;

/**
 * Кто больше всех поддержал контент.
 *
 * Отдельный запрос: блок стоит в самом низу страницы, и класть его в
 * карточку значило бы делать группировку по донатам при каждом открытии
 * любого фильма.
 */
export function useContentDonors(contentId: number | null) {
  return useQuery({
    queryKey: ['content-donors', contentId],
    queryFn: () => fetchDonors(contentId as number, TOP_DONORS),
    enabled: contentId !== null,
    staleTime: 60 * 1000,
    retry: (failureCount, error) =>
      !(error instanceof ContentDetailUnavailableError) && failureCount < 2,
  });
}
