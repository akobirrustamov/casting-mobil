import type { CastingType, Gender } from '@/features/creators/types';

/**
 * Направления анкеты — ровно те шесть значений, что в форме сайта
 * (`frontend/src/pages/dataForm/DataForm.js`) и в фильтре каталога
 * (`Models.js`). Порядок тоже оттуда: человек, заполнявший анкету на
 * сайте, увидит тот же список в том же порядке.
 *
 * ⚠️ Значения — это контракт бэкенда (`castingType` в анкете), а не
 * подписи. Подписи живут в `casting.types.*` локалей.
 *
 * ⚠️ `extra` на сайте и в сообщениях бэкенда подписан «Aktrisa», а в
 * каталоге приложения (`features/catalog/categories`, `EXTRA_API_TYPES`)
 * — «Massovka». Здесь взята подпись сайта: анкету кандидат заполняет по
 * правилам сайта, и бот присылает ему именно «Aktrisa». Расхождение
 * с каталогом стоит закрыть одним решением заказчика, а не молча.
 */
export const CASTING_TYPES: readonly CastingType[] = [
  'model',
  'euromodel',
  'bloger',
  'actor',
  'extra',
  'influencer',
] as const;

export const GENDERS: readonly Gender[] = ['male', 'female'] as const;

export function isCastingType(value: unknown): value is CastingType {
  return typeof value === 'string' && (CASTING_TYPES as readonly string[]).includes(value);
}

/** Ключ перевода направления; незнакомое значение показываем как есть. */
export function castingTypeKey(value: string | null | undefined): string | null {
  return isCastingType(value) ? `casting.types.${value}` : null;
}

/**
 * Viloyatlar анкеты: ключ — для подписи (`casting.form.regions.*`),
 * `value` — то, что уходит на сервер.
 *
 * ⚠️ На сервер всегда идёт узбекское название, на каком бы языке ни был
 * интерфейс: по этому полю фильтруется каталог (`collectRegionOptions`),
 * и «Samarqand» с «Самарканд» распались бы там на два региона.
 */
export const REGIONS = [
  { key: 'tashkentCity', value: 'Toshkent shahri' },
  { key: 'tashkent', value: 'Toshkent viloyati' },
  { key: 'andijan', value: 'Andijon' },
  { key: 'bukhara', value: 'Buxoro' },
  { key: 'fergana', value: "Farg'ona" },
  { key: 'jizzakh', value: 'Jizzax' },
  { key: 'khorezm', value: 'Xorazm' },
  { key: 'namangan', value: 'Namangan' },
  { key: 'navoi', value: 'Navoiy' },
  { key: 'kashkadarya', value: 'Qashqadaryo' },
  { key: 'karakalpakstan', value: "Qoraqalpog'iston" },
  { key: 'samarkand', value: 'Samarqand' },
  { key: 'sirdarya', value: 'Sirdaryo' },
  { key: 'surkhandarya', value: 'Surxondaryo' },
] as const;

/** Пункт «Boshqa» — регион пишут сами. */
export const REGION_OTHER = 'other';
