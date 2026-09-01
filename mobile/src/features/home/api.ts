import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';

import { DEFAULT_LANGUAGE, isSupportedLanguage, type Language } from '@/i18n';
import { api } from '@/lib/api';

import type {
  BannerCard,
  CategoryCard,
  ContentCard,
  FeedCreatorCard,
  HomeFeed,
  HomeSection,
} from './types';

/** Язык интерфейса → параметр `locale` бэкенда. */
const LOCALE_PARAM: Record<Language, 'UZ' | 'RU' | 'EN'> = {
  uz: 'UZ',
  ru: 'RU',
  en: 'EN',
};

/**
 * Значение `locale` для `/api/v1/app/**`.
 *
 * Экспортируется, потому что тот же параметр нужен каталогу категорий
 * (`features/catalog/contentCategories`): таблица соответствия должна быть
 * одна — с двумя копиями достаточно поправить одну, чтобы один экран
 * молча остался на другом языке.
 */
export function feedLocale(language: Language): 'UZ' | 'RU' | 'EN' {
  return LOCALE_PARAM[language];
}

/**
 * Эндпоинта нет на этом сервере.
 *
 * Отдельный тип ошибки нужен, чтобы не долбить ретраями: `/api/v1/app/**`
 * появился в новой сборке бэкенда, а на боевом сервере пока старая. Там
 * запрос отдаёт не 404, а `index.html` сайта со статусом 200 — axios такой
 * ответ ошибкой не считает, и без проверки ниже экран молча остался бы пустым.
 */
export class HomeFeedUnavailableError extends Error {
  constructor() {
    super('/api/v1/app/home недоступен на этом сервере');
    this.name = 'HomeFeedUnavailableError';
  }
}

function str(value: unknown): string | null {
  return typeof value === 'string' && value.trim() !== '' ? value : null;
}

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function list(value: unknown): unknown[] {
  return Array.isArray(value) ? value : [];
}

/**
 * Карточки без id пропускаются: id — ключ списка и будущая точка перехода,
 * без него карточка ни на что не ведёт.
 *
 * Экспортируется: `/api/v1/app/catalog/categories/{id}` отдаёт РОВНО такую
 * же карточку (на бэкенде это один и тот же `HomeFeedDto.ContentCard`), и
 * второй разбор того же формата разъехался бы с этим при первом же новом поле.
 */
export function mapContent(raw: unknown): ContentCard | null {
  const r = raw as Record<string, unknown>;
  const id = num(r?.id);
  if (id === null) return null;
  return {
    id,
    slug: str(r.slug),
    title: str(r.title),
    shortDescription: str(r.shortDescription),
    contentType: str(r.contentType),
    orientation: str(r.orientation),
    accessPolicy: str(r.accessPolicy),
    ageRating: str(r.ageRating),
    posterMediaId: num(r.posterMediaId),
    durationSeconds: num(r.durationSeconds),
    episodeCount: num(r.episodeCount),
    genre: str(r.genre),
  };
}

function mapBanner(raw: unknown): BannerCard | null {
  const r = raw as Record<string, unknown>;
  const id = num(r?.id);
  if (id === null) return null;
  return {
    id,
    audience: str(r.audience),
    title: str(r.title),
    subtitle: str(r.subtitle),
    description: str(r.description),
    buttonText: str(r.buttonText),
    // На бэкенде поле nullable; отсутствие трактуем как «кнопки нет».
    buttonEnabled: r.buttonEnabled === true,
    imageMediaId: num(r.imageMediaId),
    videoMediaId: num(r.videoMediaId),
    linkType: str(r.linkType),
    linkUrl: str(r.linkUrl),
    internalTargetType: str(r.internalTargetType),
    internalTargetId: num(r.internalTargetId),
  };
}

function mapCategory(raw: unknown): CategoryCard | null {
  const r = raw as Record<string, unknown>;
  const id = num(r?.id);
  if (id === null) return null;
  return { id, slug: str(r.slug), name: str(r.name), iconMediaId: num(r.iconMediaId) };
}

function mapCreator(raw: unknown): FeedCreatorCard | null {
  const r = raw as Record<string, unknown>;
  const id = num(r?.id);
  if (id === null) return null;
  return {
    id,
    slug: str(r.slug),
    displayName: str(r.displayName),
    photoMediaId: num(r.photoMediaId),
  };
}

function nonNull<T>(items: (T | null)[]): T[] {
  return items.filter((item): item is T => item !== null);
}

/**
 * Секция без элементов не рисуется.
 *
 * Бэкенд такие секции и так не отдаёт, но правило дублируется на клиенте:
 * пустой заголовок без содержимого выглядит как сломанный экран, а
 * придумывать элементы вместо отсутствующих данных нельзя.
 */
function mapSection(raw: unknown): HomeSection | null {
  const r = raw as Record<string, unknown>;
  const id = num(r?.id);
  const type = str(r?.type);
  if (id === null || type === null) return null;

  const section: HomeSection = {
    id,
    type,
    title: str(r.title),
    sortOrder: num(r.sortOrder),
    content: nonNull(list(r.content).map(mapContent)),
    banners: nonNull(list(r.banners).map(mapBanner)),
    categories: nonNull(list(r.categories).map(mapCategory)),
    creators: nonNull(list(r.creators).map(mapCreator)),
  };

  const total =
    section.content.length +
    section.banners.length +
    section.categories.length +
    section.creators.length;

  return total > 0 ? section : null;
}

async function fetchHomeFeed(language: Language): Promise<HomeFeed> {
  const { data } = await api.get<unknown>('/api/v1/app/home', {
    params: { locale: LOCALE_PARAM[language] },
  });

  const raw = data as Record<string, unknown> | null;
  if (!raw || typeof raw !== 'object' || !Array.isArray(raw.sections)) {
    throw new HomeFeedUnavailableError();
  }

  return {
    locale: str(raw.locale) ?? LOCALE_PARAM[language],
    // Осторожная сторона: пока бэкенд не сказал обратного, реклама показывается.
    showAds: raw.showAds !== false,
    sections: nonNull(raw.sections.map(mapSection)),
  };
}

/** Язык интерфейса, приведённый к поддерживаемому. */
export function useFeedLanguage(): Language {
  const { i18n } = useTranslation();
  return isSupportedLanguage(i18n.language) ? i18n.language : DEFAULT_LANGUAGE;
}

/**
 * Главная целиком, одним запросом.
 *
 * Язык входит в ключ кэша: при переключении языка приходят другие заголовки
 * секций, названия и афиши — это другой ответ, а не тот же самый.
 */
export function useHomeFeed() {
  const language = useFeedLanguage();

  return useQuery({
    queryKey: ['home-feed', language],
    queryFn: () => fetchHomeFeed(language),
    // Ретраи имеют смысл при сетевом сбое. Если эндпоинта на сервере нет,
    // повторять бессмысленно — ответ не изменится, а экран будет ждать втрое дольше.
    retry: (failureCount, error) =>
      !(error instanceof HomeFeedUnavailableError) && failureCount < 2,
  });
}

/** Секции с контентом — для вкладки «Premyera», где карточки фильтруются по типу. */
export function contentCards(feed: HomeFeed | undefined): ContentCard[] {
  const seen = new Set<number>();
  const result: ContentCard[] = [];

  for (const section of feed?.sections ?? []) {
    for (const card of section.content) {
      // Один и тот же фильм может стоять в нескольких рядах сразу.
      if (seen.has(card.id)) continue;
      seen.add(card.id);
      result.push(card);
    }
  }
  return result;
}

/**
 * Карточка контента по id — из кэша главной.
 *
 * Обогащение, а не источник правды: эндпоинта «дай карточку по id» в
 * `/api/v1/app/**` нет, а при переходе по прямой ссылке кэш может быть
 * пустым. Экран обязан работать и без неё.
 */
export function useContentCard(contentId: number | null): ContentCard | undefined {
  const feed = useHomeFeed();
  return useMemo(
    () =>
      contentId === null
        ? undefined
        : contentCards(feed.data).find((c) => c.id === contentId),
    [feed.data, contentId]
  );
}

/** Баннеры премьер — их отдаёт секция `NEW_PREMIERES`. */
export function premiereBanners(feed: HomeFeed | undefined): BannerCard[] {
  return (feed?.sections ?? [])
    .filter((s) => s.type === 'NEW_PREMIERES')
    .flatMap((s) => s.banners);
}
