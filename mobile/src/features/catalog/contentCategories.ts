import { useInfiniteQuery, useQueries, useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { useMemo } from 'react';

import { feedLocale, mapContent, useFeedLanguage } from '@/features/home/api';
import type { ContentCard } from '@/features/home/types';
import type { Language } from '@/i18n';
import { api } from '@/lib/api';

/**
 * Разделы каталога КОНТЕНТА («Drama», «Komediya», «O'zbek kinosi»).
 *
 * ⚠️ Это НЕ 10 направлений кастинга из `features/catalog/categories.ts`.
 * Там анкеты людей на старом API сайта, здесь фильмы и сериалы новой
 * платформы. Совпадает только слово «категория»; id из одного списка в
 * другом не значит ничего.
 *
 * <h2>Почему не из фида главной</h2>
 * Секция `CATEGORIES` в `/api/v1/app/home` отдаёт только НАЗВАНИЯ — плитку
 * нарисовать можно, а что внутри, неизвестно. Ряды же контента там собраны
 * по ТИПУ (`PODCASTS`, `SHOWS`, `MINI_SERIES`), а не по категории (ТЗ §13 —
 * это разные оси), и поля `categoryId` у карточки нет. То есть ряд «Drama»
 * из фида собрать нечем.
 *
 * <h2>Почему по одному запросу на категорию</h2>
 * Категорий может быть сколько угодно. Класть их все в главную значило бы
 * добавлять к каждому её запросу десяток лишних рядов — в том числе тем,
 * кто до них не долистает. Здесь сначала приходит лёгкий список (названия
 * и счётчики), а карточки запрашиваются по одному ряду: верхний ряд
 * заполняется сразу и не ждёт нижних, а упавший ряд не уносит остальные.
 */

/**
 * Сколько карточек запрашивает ряд на главной.
 *
 * Заказчик: в ряду видно три карточки, остальные — прокруткой вбок.
 * Десять — это то, что реально пролистывают: грузить больше значило бы
 * тянуть постеры, до которых не доводят палец, на КАЖДУЮ категорию сразу.
 */
export const ROW_SIZE = 10;

/**
 * Размер страницы на экране «вся категория» — заказчик: «20ta 20ta».
 * Следующая приезжает при подходе к концу списка.
 */
export const PAGE_SIZE = 20;

/**
 * Эндпоинта нет на этом сервере — причина та же, что у
 * `HomeFeedUnavailableError`: боевой сервер пока на старой сборке и на
 * неизвестный адрес отдаёт `index.html` со статусом 200. Без этой проверки
 * блок молча остался бы пустым вместо честного «ряды не пришли».
 */
export class CatalogUnavailableError extends Error {
  constructor() {
    super('/api/v1/app/catalog недоступен на этом сервере');
    this.name = 'CatalogUnavailableError';
  }
}

/**
 * Ответ, который означает «этого раздела на сервере нет».
 *
 * <h2>Почему 401 и 403 тоже здесь</h2>
 * Эндпоинт открыт для гостя (`permitAll` в `SecurityConfig`). Значит на
 * сервере, где правило есть, отказа по доступу быть не может — а если он
 * пришёл, сервер просто не знает такого адреса и роняет его в общее
 * «всё остальное закрыто». Ровно это и происходит на сборке, поднятой до
 * появления каталога.
 *
 * Показывать в этом случае «Bo'limlar yuklanmadi. Qayta urinib ko'ring» —
 * значит звать человека повторять запрос, который не изменится. Блок
 * просто не рисуется, как и при отсутствующем `/app/home`.
 */
function isMissingEndpoint(error: unknown): boolean {
  if (!axios.isAxiosError(error)) return false;
  const status = error.response?.status;
  return status === 401 || status === 403 || status === 404;
}

/**
 * Раздел каталога.
 *
 * `total` — сколько контента в разделе ВСЕГО, независимо от `limit`. Только
 * по нему видно, есть ли смысл в кнопке «Barchasi ›»: длина `items`
 * ограничена запросом и про остаток ничего не говорит.
 */
export type ContentCategory = {
  id: number;
  slug: string | null;
  name: string | null;
  iconMediaId: number | null;
  total: number;
  /** Номер пришедшей страницы, с нуля. В списке разделов — `null`. */
  page: number | null;
  /** Есть ли ещё страница. Решает сервер — клиент не считает это сам. */
  hasMore: boolean;
  /** Карточки ЭТОЙ страницы. */
  items: ContentCard[];
};

function str(value: unknown): string | null {
  return typeof value === 'string' && value.trim() !== '' ? value : null;
}

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

/** Категория без id ни на что не ведёт — такой ряд открыть нечем. */
function mapCategory(raw: unknown): ContentCategory | null {
  const r = raw as Record<string, unknown>;
  const id = num(r?.id);
  if (id === null) return null;

  const items = Array.isArray(r.items) ? r.items : [];

  return {
    id,
    slug: str(r.slug),
    name: str(r.name),
    iconMediaId: num(r.iconMediaId),
    // Отсутствие счётчика — это «неизвестно», а не «пусто»: подставлять
    // сюда длину `items` значило бы выдать одну страницу за весь раздел.
    total: num(r.total) ?? 0,
    page: num(r.page),
    hasMore: r.hasMore === true,
    items: items
      .map(mapContent)
      .filter((card): card is ContentCard => card !== null),
  };
}

async function fetchCategories(language: Language): Promise<ContentCategory[]> {
  let data: unknown;
  try {
    ({ data } = await api.get<unknown>('/api/v1/app/catalog/categories', {
      params: { locale: feedLocale(language) },
    }));
  } catch (error) {
    if (isMissingEndpoint(error)) throw new CatalogUnavailableError();
    throw error;
  }

  // Старая сборка отдаёт HTML со статусом 200 — для axios это успех.
  if (!Array.isArray(data)) {
    throw new CatalogUnavailableError();
  }

  return data
    .map(mapCategory)
    .filter((c): c is ContentCategory => c !== null);
}

async function fetchCategory(
  language: Language,
  categoryId: number,
  page: number,
  size: number
): Promise<ContentCategory> {
  let data: unknown;
  try {
    ({ data } = await api.get<unknown>(
      `/api/v1/app/catalog/categories/${categoryId}`,
      { params: { locale: feedLocale(language), page, size } }
    ));
  } catch (error) {
    if (isMissingEndpoint(error)) throw new CatalogUnavailableError();
    throw error;
  }

  const mapped =
    data && typeof data === 'object' && !Array.isArray(data)
      ? mapCategory(data)
      : null;

  if (mapped === null) {
    throw new CatalogUnavailableError();
  }
  return mapped;
}

/** Повторять есть смысл при сетевом сбое; отсутствующий адрес не появится. */
function retry(failureCount: number, error: unknown): boolean {
  return !(error instanceof CatalogUnavailableError) && failureCount < 2;
}

/**
 * Список разделов — названия и счётчики, без карточек.
 *
 * Язык в ключе кэша: при переключении приходят другие названия, то есть
 * другой ответ, а не тот же самый.
 */
export function useContentCategories() {
  const language = useFeedLanguage();

  return useQuery({
    queryKey: ['catalog-categories', language],
    queryFn: () => fetchCategories(language),
    retry,
  });
}

/** Один раздел, одна страница. */
export function useContentCategoryPage(
  categoryId: number | null,
  page = 0,
  size = ROW_SIZE
) {
  const language = useFeedLanguage();

  return useQuery({
    queryKey: ['catalog-category', language, categoryId, page, size],
    queryFn: () => fetchCategory(language, categoryId as number, page, size),
    enabled: categoryId !== null,
    retry,
  });
}

/**
 * Весь раздел страницами по двадцать — экран «Barchasi».
 *
 * <h2>Почему не один запрос на всё</h2>
 * У эндпоинта есть потолок (100), но дело не в нём: раздел может быть
 * большим, а человек чаще всего смотрит первый экран. Страницы приезжают
 * по мере прокрутки — то есть загружается ровно то, до чего долистали.
 *
 * `hasMore` приходит с сервера, а не считается из `total`: решение
 * «страница последняя» должно жить в одном месте, иначе клиент однажды
 * попросит страницу, которой нет.
 */
export function useCategoryPages(categoryId: number | null, size = PAGE_SIZE) {
  const language = useFeedLanguage();

  return useInfiniteQuery({
    queryKey: ['catalog-category-pages', language, categoryId, size],
    queryFn: ({ pageParam }) =>
      fetchCategory(language, categoryId as number, pageParam, size),
    initialPageParam: 0,
    getNextPageParam: (last) =>
      last.hasMore ? (last.page ?? 0) + 1 : undefined,
    enabled: categoryId !== null,
    retry,
  });
}

export type CategoryRow = {
  /** Название и счётчик — известны сразу, до прихода карточек. */
  head: ContentCategory;
  query: ReturnType<typeof useContentCategoryPage>;
};

/**
 * Ряды категорий для главной: список плюс по запросу на каждый ряд.
 *
 * <h2>Пустые разделы не запрашиваются</h2>
 * Раздел с `total === 0` отсеивается сразу: запрос вернул бы пустой
 * список, а ряд без карточек всё равно не рисуется. Это экономит ровно
 * столько запросов, сколько в панели заведено ещё не наполненных
 * категорий, — а на свежей базе это они все.
 */
export function useCategoryRows(size = ROW_SIZE): {
  list: ReturnType<typeof useContentCategories>;
  rows: CategoryRow[];
} {
  const language = useFeedLanguage();
  const list = useContentCategories();

  const heads = useMemo(
    () => (list.data ?? []).filter((c) => c.total > 0),
    [list.data]
  );

  const results = useQueries({
    queries: heads.map((c) => ({
      // Тот же ключ, что и у `useContentCategoryPage` — первая страница
      // ряда не запрашивается дважды.
      queryKey: ['catalog-category', language, c.id, 0, size],
      queryFn: () => fetchCategory(language, c.id, 0, size),
      retry,
    })),
  });

  return {
    list,
    rows: heads.map((head, i) => ({
      head,
      query: results[i] as CategoryRow['query'],
    })),
  };
}
