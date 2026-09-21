import type { Creator, Gender } from '@/features/creators/types';

/**
 * Фильтры каталога кандидатов на вкладке «Casting».
 *
 * <h2>Почему отдельный модуль, а не `features/creators/filters`</h2>
 * Тот фильтр — одиночный выбор (`region: string | null`) и живёт на
 * экране направления (`app/catalog/[category]`). Здесь выбор
 * МНОЖЕСТВЕННЫЙ: «модель ИЛИ блогер», «Тошкент ИЛИ Самарқанд». Сменить
 * тип на месте значило бы переписать и тот экран заодно — а он про
 * другое и менять его никто не просил.
 *
 * <h2>Считается на клиенте</h2>
 * Как и раньше: API отдаёт весь список одним запросом и фильтровать не
 * умеет. Когда анкет станет слишком много, фильтрация уедет на бэкенд —
 * форма этого типа при этом не изменится.
 */
export type CatalogFilters = {
  /** Типы из API (`model`, `bloger`…). Пусто — не фильтруем. */
  types: string[];
  /** Регионы ровно так, как они записаны в анкетах. */
  regions: string[];
  genders: Gender[];
  /** Границы включительно; `null` — граница не задана. */
  ageMin: number | null;
  ageMax: number | null;
  heightMin: number | null;
  heightMax: number | null;
};

export const EMPTY_CATALOG_FILTERS: CatalogFilters = {
  types: [],
  regions: [],
  genders: [],
  ageMin: null,
  ageMax: null,
  heightMin: null,
  heightMax: null,
};

/** Значение есть в списке — убрать, нет — добавить. */
export function toggleValue<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
}

/**
 * Приведение строки к виду, по которому ищем.
 *
 * ⚠️ Апострофы выбрасываем намеренно. В анкетах один и тот же город
 * пишут «Qashqadaryo», «Qashqadaryo'» и «Qashqadaryoʻ»; человек, который
 * набирает запрос на телефоне, ставит обычный `'` или не ставит вовсе.
 * Без этого поиск по «qashqadaryo» не находил бы половину анкет.
 */
export function normalize(value: string): string {
  return value
    .toLowerCase()
    .replace(/[''`ʻʼ‘’´]/g, '')
    .trim();
}

export function matchesQuery(label: string, query: string): boolean {
  const q = normalize(query);
  return q === '' || normalize(label).includes(q);
}

/** Диапазон задан не полностью — считаем только заданную границу. */
function inRange(value: number | null, min: number | null, max: number | null): boolean {
  if (min === null && max === null) return true;
  // Значение неизвестно (в анкете пусто) — в диапазон не попадаем:
  // иначе «рост 170–180» показывал бы анкеты без роста вообще.
  if (value === null) return false;
  if (min !== null && value < min) return false;
  if (max !== null && value > max) return false;
  return true;
}

export function applyCatalogFilters(creators: Creator[], f: CatalogFilters): Creator[] {
  return creators.filter((c) => {
    if (f.types.length > 0 && (!c.castingType || !f.types.includes(c.castingType))) {
      return false;
    }
    if (f.regions.length > 0 && (!c.region || !f.regions.includes(c.region))) {
      return false;
    }
    if (f.genders.length > 0 && !f.genders.includes(c.gender)) {
      return false;
    }
    if (!inRange(c.age, f.ageMin, f.ageMax)) return false;
    if (!inRange(c.height, f.heightMin, f.heightMax)) return false;
    return true;
  });
}

/**
 * Сколько фильтров включено — число на кнопке.
 *
 * Диапазон считается за ОДИН фильтр, даже если заданы обе границы:
 * для человека «возраст 18–25» — это одна строка условия, а не две.
 */
export function countCatalogFilters(f: CatalogFilters): number {
  let n = f.types.length + f.regions.length + f.genders.length;
  if (f.ageMin !== null || f.ageMax !== null) n += 1;
  if (f.heightMin !== null || f.heightMax !== null) n += 1;
  return n;
}

export function hasAnyFilter(f: CatalogFilters): boolean {
  return countCatalogFilters(f) > 0;
}

/**
 * Регионы, которые ВСТРЕЧАЮТСЯ в анкетах.
 *
 * Справочника регионов у нас нет, а если бы и был — он предлагал бы
 * города, где ни одной анкеты нет, и поиск заканчивался бы пустым
 * экраном.
 */
export function collectRegionOptions(creators: Creator[]): string[] {
  const set = new Set<string>();
  for (const c of creators) {
    const region = c.region?.trim();
    if (region) set.add(region);
  }
  return [...set].sort((a, b) => a.localeCompare(b));
}

/** Пустая строка/мусор → `null`; иначе целое число. */
export function parseBound(raw: string): number | null {
  const digits = raw.replace(/\D/g, '');
  if (digits === '') return null;
  const n = Number(digits);
  return Number.isFinite(n) ? n : null;
}
