import { CATEGORIES, EXTRA_API_TYPES } from '@/features/catalog/categories';

import type { Creator } from './types';

/**
 * Поиск по анкетам.
 *
 * У Yangi.TV поиск живой и требует минимум 2 символа — иначе под полем
 * загорается «Minimal qidiruv uzunligi 2 ta harf!». Повторяем то же правило:
 * с одной буквы выдача бессмысленна, а список перерисовывается на каждый ввод.
 */
export const MIN_QUERY_LENGTH = 2;

/**
 * Ищем по имени, городу и названию направления — на обоих языках.
 * Человек может набрать и «model», и «Modellar», и «Модели».
 */
export function searchCreators(creators: Creator[], rawQuery: string): Creator[] {
  const query = normalize(rawQuery);
  if (query.length < MIN_QUERY_LENGTH) return [];

  return creators.filter((c) => {
    const haystack = [
      c.name,
      c.region,
      c.castingType,
      ...typeLabels(c.castingType),
    ]
      .filter(Boolean)
      .map((v) => normalize(String(v)));

    return haystack.some((v) => v.includes(query));
  });
}

/**
 * Апостроф в узбекской латинице пишут по-разному: o'zbek, o‘zbek, oʻzbek.
 * Без приведения к одному виду поиск по «Farg'ona» не найдёт «Farg‘ona».
 */
function normalize(value: string): string {
  return value
    .toLowerCase()
    .replace(/[’‘ʻ`´]/g, "'")
    .trim();
}

function typeLabels(type: string | null): string[] {
  if (!type) return [];

  const category = CATEGORIES.find((c) => c.apiType === type);
  if (category) return [category.titleUz, category.titleRu];

  const extra = EXTRA_API_TYPES[type];
  return extra ? [extra.uz, extra.ru] : [];
}
