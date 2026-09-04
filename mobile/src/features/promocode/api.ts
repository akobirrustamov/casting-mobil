import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

import { api } from '@/lib/api';

/**
 * Промокоды — `POST /api/v1/app/promocodes/redeem`, `GET .../my`.
 *
 * <h2>Что даёт код — решает АДМИН</h2>
 * Заказчик (04.09.2026): «промокоды создаются в админке, для чего создан —
 * к тому и должен подключаться». Поэтому у кода есть тип:
 *
 * - `PREMIUM_DAYS` — N дней Premium (он открывает и раздел Casting);
 * - `CASTING_DAYS` — N дней ТОЛЬКО раздела Casting, фильмы остаются закрыты.
 *
 * В обоих случаях дни ПРИБАВЛЯЮТСЯ к текущему сроку. Считает сервер —
 * здесь только показывается результат.
 *
 * ⚠️ Экран обязан различать типы. Человек, активировавший casting-код,
 * не должен ждать, что откроются фильмы.
 *
 * <h2>⚠️ Причина отказа важнее самого отказа</h2>
 * Бэкенд различает пять случаев: кода нет, срок вышел, уже использован,
 * лимит исчерпан, код остановлен. Одно общее «код не подошёл» заставило
 * бы человека набирать его снова и снова — особенно тот, кто УЖЕ его
 * активировал и просто забыл.
 */

/** Что именно даёт код. Совпадает с `PromocodeGrantType` на бэкенде. */
export type GrantType = 'PREMIUM_DAYS' | 'CASTING_DAYS';

export type RedeemResult = {
  code: string;
  grantType: GrantType;
  days: number;
  /** Докуда теперь действует выданное право. */
  until: string | null;
};

export type MyPromocode = {
  code: string;
  grantType: GrantType;
  days: number;
  redeemedAt: string;
  until: string | null;
};

/**
 * Незнакомый тип считаем премиумом.
 *
 * ⚠️ Не бросаем ошибку: если бэкенд однажды добавит третий тип, старое
 * приложение должно показать хоть что-то, а не отказ на уже выданном
 * праве — деньги (пусть и в днях) уже списаны с кода.
 */
function grantType(value: unknown): GrantType {
  return value === 'CASTING_DAYS' ? 'CASTING_DAYS' : 'PREMIUM_DAYS';
}

/** Коды ошибок бэкенда (`BusinessException.code`). */
export type PromoErrorCode =
  | 'PROMO_NOT_FOUND'
  | 'PROMO_EXPIRED'
  | 'PROMO_ALREADY_USED'
  | 'PROMO_EXHAUSTED'
  | 'PROMO_INACTIVE'
  | 'VALIDATION_ERROR'
  | 'UNKNOWN';

export class PromoError extends Error {
  constructor(public readonly code: PromoErrorCode) {
    super(code);
    this.name = 'PromoError';
  }
}

const KNOWN: PromoErrorCode[] = [
  'PROMO_NOT_FOUND',
  'PROMO_EXPIRED',
  'PROMO_ALREADY_USED',
  'PROMO_EXHAUSTED',
  'PROMO_INACTIVE',
  'VALIDATION_ERROR',
];

function toPromoError(error: unknown): never {
  if (axios.isAxiosError(error) && error.response) {
    const code = (error.response.data as { code?: string } | undefined)?.code;
    if (code && (KNOWN as string[]).includes(code)) {
      throw new PromoError(code as PromoErrorCode);
    }
  }
  // Сеть или незнакомый код — общий текст. Врать про причину нельзя.
  throw new PromoError('UNKNOWN');
}

function str(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

export async function redeemPromocode(code: string): Promise<RedeemResult> {
  try {
    const { data } = await api.post<Record<string, unknown>>(
      '/api/v1/app/promocodes/redeem',
      { code },
    );
    return {
      code: str(data.code) ?? code,
      grantType: grantType(data.grantType),
      days: typeof data.days === 'number' ? data.days : 0,
      until: str(data.until),
    };
  } catch (error) {
    return toPromoError(error);
  }
}

export async function fetchMyPromocodes(): Promise<MyPromocode[]> {
  const { data } = await api.get<unknown[]>('/api/v1/app/promocodes/my');
  return (Array.isArray(data) ? data : []).map((raw) => {
    const r = (raw ?? {}) as Record<string, unknown>;
    return {
      code: str(r.code) ?? '',
      grantType: grantType(r.grantType),
      days: typeof r.days === 'number' ? r.days : 0,
      redeemedAt: str(r.redeemedAt) ?? '',
      until: str(r.until),
    };
  });
}

export function useMyPromocodes(enabled = true) {
  return useQuery({
    queryKey: ['promocodes', 'my'],
    queryFn: fetchMyPromocodes,
    enabled,
  });
}

/**
 * Активация.
 *
 * После успеха сбрасываем подписку и профиль: Premium только что
 * изменился, и экраны, показывающие срок, должны перечитать его — иначе
 * человек вернётся в профиль и увидит прежнее «подписки нет».
 */
export function useRedeemPromocode() {
  const client = useQueryClient();

  return useMutation({
    mutationFn: redeemPromocode,
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['promocodes', 'my'] });
      void client.invalidateQueries({ queryKey: ['subscription'] });
      void client.invalidateQueries({ queryKey: ['me'] });
    },
  });
}

/** Код ошибки → ключ перевода. Незнакомый — общий текст. */
export function promoErrorKey(error: unknown): string {
  const code = error instanceof PromoError ? error.code : 'UNKNOWN';
  switch (code) {
    case 'PROMO_NOT_FOUND':
      return 'promocode.errorNotFound';
    case 'PROMO_EXPIRED':
      return 'promocode.errorExpired';
    case 'PROMO_ALREADY_USED':
      return 'promocode.errorAlreadyUsed';
    case 'PROMO_EXHAUSTED':
      return 'promocode.errorExhausted';
    case 'PROMO_INACTIVE':
      return 'promocode.errorInactive';
    case 'VALIDATION_ERROR':
      return 'promocode.errorMalformed';
    default:
      return 'promocode.errorUnknown';
  }
}
