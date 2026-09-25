import {
  EMPTY_CATALOG_FILTERS,
  applyCatalogFilters,
  collectRegionOptions,
  countCatalogFilters,
  matchesQuery,
  parseBound,
  toggleValue,
} from '../catalogFilters';
import type { Creator } from '@/features/creators/types';

function creator(patch: Partial<Creator>): Creator {
  return {
    id: 1,
    name: 'Test',
    castingType: 'model',
    castingTypeRaw: 'model',
    gender: 'female',
    region: 'Toshkent',
    nationality: null,
    age: 22,
    height: 175,
    hairColor: null,
    eyeColor: null,
    photoUrls: [],
    ...patch,
  };
}

const list: Creator[] = [
  creator({ id: 1, castingType: 'model', gender: 'female', region: 'Toshkent', age: 22, height: 175 }),
  creator({ id: 2, castingType: 'bloger', gender: 'male', region: 'Samarqand', age: 31, height: 182 }),
  creator({ id: 3, castingType: 'actor', gender: 'female', region: "Qashqadaryo'", age: 17, height: 160 }),
  creator({ id: 4, castingType: 'model', gender: 'male', region: null, age: null, height: null }),
];

const ids = (items: Creator[]) => items.map((c) => c.id);

describe('applyCatalogFilters', () => {
  it('без фильтров отдаёт всех', () => {
    expect(ids(applyCatalogFilters(list, EMPTY_CATALOG_FILTERS))).toEqual([1, 2, 3, 4]);
  });

  it('несколько направлений — это ИЛИ, а не И', () => {
    const got = applyCatalogFilters(list, { ...EMPTY_CATALOG_FILTERS, types: ['model', 'bloger'] });
    expect(ids(got)).toEqual([1, 2, 4]);
  });

  it('несколько регионов сразу', () => {
    const got = applyCatalogFilters(list, {
      ...EMPTY_CATALOG_FILTERS,
      regions: ['Toshkent', 'Samarqand'],
    });
    expect(ids(got)).toEqual([1, 2]);
  });

  it('разделы складываются через И', () => {
    const got = applyCatalogFilters(list, {
      ...EMPTY_CATALOG_FILTERS,
      types: ['model', 'bloger'],
      genders: ['male'],
    });
    expect(ids(got)).toEqual([2, 4]);
  });

  it('диапазон возраста включает границы', () => {
    const got = applyCatalogFilters(list, { ...EMPTY_CATALOG_FILTERS, ageMin: 22, ageMax: 31 });
    expect(ids(got)).toEqual([1, 2]);
  });

  it('одна граница диапазона тоже работает', () => {
    expect(ids(applyCatalogFilters(list, { ...EMPTY_CATALOG_FILTERS, heightMin: 180 }))).toEqual([2]);
    expect(ids(applyCatalogFilters(list, { ...EMPTY_CATALOG_FILTERS, heightMax: 170 }))).toEqual([3]);
  });

  it('анкеты без возраста и роста в диапазон не попадают', () => {
    const got = applyCatalogFilters(list, { ...EMPTY_CATALOG_FILTERS, ageMin: 1 });
    expect(ids(got)).not.toContain(4);
  });
});

describe('countCatalogFilters', () => {
  it('диапазон считается один раз, даже если заданы обе границы', () => {
    expect(
      countCatalogFilters({ ...EMPTY_CATALOG_FILTERS, ageMin: 18, ageMax: 25 })
    ).toBe(1);
  });

  it('каждое выбранное значение — отдельная единица', () => {
    expect(
      countCatalogFilters({
        ...EMPTY_CATALOG_FILTERS,
        types: ['model', 'actor'],
        genders: ['female'],
        heightMax: 180,
      })
    ).toBe(4);
  });
});

describe('поиск', () => {
  it('не зависит от регистра и апострофов', () => {
    expect(matchesQuery("Qashqadaryo'", 'qashqadaryo')).toBe(true);
    expect(matchesQuery('Qashqadaryoʻ', "qashqadaryo'")).toBe(true);
  });

  it('пустой запрос совпадает со всем', () => {
    expect(matchesQuery('Toshkent', '   ')).toBe(true);
  });

  it('несовпадение остаётся несовпадением', () => {
    expect(matchesQuery('Toshkent', 'samar')).toBe(false);
  });
});

describe('вспомогательное', () => {
  it('toggleValue добавляет и убирает', () => {
    expect(toggleValue(['a'], 'b')).toEqual(['a', 'b']);
    expect(toggleValue(['a', 'b'], 'a')).toEqual(['b']);
  });

  it('collectRegionOptions отдаёт только встречающиеся регионы, без пустых', () => {
    expect(collectRegionOptions(list)).toEqual(["Qashqadaryo'", 'Samarqand', 'Toshkent']);
  });

  it('parseBound берёт только цифры', () => {
    expect(parseBound('18')).toBe(18);
    expect(parseBound('1a8')).toBe(18);
    expect(parseBound('')).toBeNull();
    expect(parseBound('abc')).toBeNull();
  });
});
