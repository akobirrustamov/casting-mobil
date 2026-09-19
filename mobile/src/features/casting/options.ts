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
