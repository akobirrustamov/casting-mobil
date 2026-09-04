import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { useAuthStore } from '@/features/auth/store';
import { feedLocale } from '@/features/home/api';
import { DEFAULT_LANGUAGE, isSupportedLanguage, type Language } from '@/i18n';
import { api } from '@/lib/api';

/**
 * Тарифы и подписка — `GET /api/v1/app/tariffs`, `GET /api/v1/app/me/subscription`.
 *
 * <h2>Что здесь чинится</h2>
 * Модуль подписки был готов на бэкенде и в админке — тарифы, цены,
 * история, отчёты. В приложении от него было два поля: `premium{active,
 * until}` в `/app/me`. Человек не видел ни цен, ни того, за что платил.
 *
 * <h2>Язык</h2>
 * Тот же параметр `locale`, что у ленты и каталога (`feedLocale`), и тот
 * же порядок на сервере: запрос → профиль → UZ. Без параметра бэкенд взял
 * бы язык профиля, а не текущий язык интерфейса — и после переключения
 * языка в приложении тарифы остались бы на старом.
 */
export type Tariff = {
  id: number;
  /** Стабильный код: `m1`, `m3`, `y1`. */
  code: string;
  durationMonths: number;
  price: number;
  /** Цена в пересчёте на месяц — считает сервер. */
  monthlyPrice: number | null;
  currency: string;
  /** «ENG FOYDALI TARIF» — карточка выделяется. */
  highlighted: boolean;
  name: string;
  badge: string | null;
  description: string | null;
  /** Что входит — уже разбито на строки сервером. */
  features: string[];
};

export type SubscriptionSource = 'PURCHASE' | 'ADMIN_GIFT';

export type SubscriptionEntry = {
  id: number;
  tariffCode: string | null;
  tariffName: string | null;
  startAt: string;
  endAt: string;
  source: SubscriptionSource;
  /** `null` — подарок: денег не было, и это не «0 сум». */
  paidAmount: number | null;
  currency: string | null;
  revokedAt: string | null;
  /** Действует прямо сейчас. */
  active: boolean;
};

export type MySubscription = {
  active: boolean;
  /** `null` — подписки никогда не было. Истёкшая сохраняет дату. */
  until: string | null;
  history: SubscriptionEntry[];
};

function num(value: unknown): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0;
}

function numOrNull(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function str(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function mapTariff(raw: unknown): Tariff {
  const r = (raw ?? {}) as Record<string, unknown>;
  return {
    id: num(r.id),
    code: str(r.code) ?? '',
    durationMonths: num(r.durationMonths),
    price: num(r.price),
    monthlyPrice: numOrNull(r.monthlyPrice),
    currency: str(r.currency) ?? 'UZS',
    highlighted: r.highlighted === true,
    name: str(r.name) ?? '',
    badge: str(r.badge),
    description: str(r.description),
    features: Array.isArray(r.features) ? r.features.filter((f): f is string => typeof f === 'string') : [],
  };
}

function mapEntry(raw: unknown): SubscriptionEntry {
  const r = (raw ?? {}) as Record<string, unknown>;
  return {
    id: num(r.id),
    tariffCode: str(r.tariffCode),
    tariffName: str(r.tariffName),
    startAt: str(r.startAt) ?? '',
    endAt: str(r.endAt) ?? '',
    source: r.source === 'ADMIN_GIFT' ? 'ADMIN_GIFT' : 'PURCHASE',
    paidAmount: numOrNull(r.paidAmount),
    currency: str(r.currency),
    revokedAt: str(r.revokedAt),
    active: r.active === true,
  };
}

/** Текущий язык интерфейса — тот, что видит человек, а не тот, что в профиле. */
function useLanguage(): Language {
  const { i18n } = useTranslation();
  return isSupportedLanguage(i18n.language) ? i18n.language : DEFAULT_LANGUAGE;
}

export async function fetchTariffs(language: Language): Promise<Tariff[]> {
  const { data } = await api.get<{ tariffs?: unknown[] }>('/api/v1/app/tariffs', {
    params: { locale: feedLocale(language) },
  });
  return (data.tariffs ?? []).map(mapTariff);
}

/**
 * Тарифы доступны и гостю: цена — то, что смотрят ДО входа.
 *
 * Ключ включает язык: переключение языка должно перечитать список, а
 * не показать кэш на прежнем.
 */
export function useTariffs() {
  const language = useLanguage();

  return useQuery({
    queryKey: ['tariffs', language],
    queryFn: () => fetchTariffs(language),
    // Цены меняются из админки редко; минута кэша избавляет от повторного
    // запроса при возврате на экран.
    staleTime: 60_000,
  });
}

export async function fetchMySubscription(language: Language): Promise<MySubscription> {
  const { data } = await api.get<Record<string, unknown>>('/api/v1/app/me/subscription', {
    params: { locale: feedLocale(language) },
  });
  return {
    active: data.active === true,
    until: str(data.until),
    history: Array.isArray(data.history) ? data.history.map(mapEntry) : [],
  };
}

export function useMySubscription() {
  const language = useLanguage();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  return useQuery({
    queryKey: ['subscription', userId, language],
    queryFn: () => fetchMySubscription(language),
    enabled: isAuthorized,
    // Подписка ИСТЕКАЕТ: закэшированное «активна» показывало бы её и
    // после окончания.
    staleTime: 0,
  });
}

// ------------------------------------------------------- история платежей

/**
 * Донаты — `GET /api/v1/app/donations/my`.
 *
 * Здесь только то, что нужно строке истории; полный DTO живёт в
 * `DonationTransactionDto` на бэкенде.
 */
export type DonationEntry = {
  id: number;
  targetName: string | null;
  /** `STARS` или монеты — подпись берётся из `profile.stars` / `profile.coins`. */
  kind: 'STARS' | 'COINS';
  amount: number;
  createdAt: string;
};

function mapDonation(raw: unknown): DonationEntry {
  const r = (raw ?? {}) as Record<string, unknown>;
  return {
    id: num(r.id),
    targetName: str(r.targetName),
    kind: r.kind === 'STARS' ? 'STARS' : 'COINS',
    amount: num(r.amount),
    createdAt: str(r.createdAt) ?? '',
  };
}

export async function fetchMyDonations(): Promise<DonationEntry[]> {
  // Одна страница на 100 записей: это личная история, а не лента, и
  // листать её постранично незачем. `PageResponse` отдаёт `items`.
  const { data } = await api.get<{ items?: unknown[] }>('/api/v1/app/donations/my', {
    params: { page: 0, size: 100 },
  });
  return (data.items ?? []).map(mapDonation);
}

export function useMyDonations() {
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  return useQuery({
    queryKey: ['donations', 'my', userId],
    queryFn: fetchMyDonations,
    enabled: isAuthorized,
  });
}

/**
 * Одна строка общей истории — подписка или донат.
 *
 * <h2>Почему это склеивается на клиенте</h2>
 * На сервере это два разных источника с разной природой: подписка —
 * «за что и до какого числа», донат — «кому и сколько». Общего
 * эндпоинта под них нет, и заводить его ради одного экрана значило бы
 * дублировать обе выборки на бэкенде. Здесь достаточно отсортировать.
 */
export type PaymentEntry =
  | { key: string; kind: 'subscription'; at: string; entry: SubscriptionEntry }
  | { key: string; kind: 'donation'; at: string; entry: DonationEntry };

export function mergeHistory(
  subscriptions: SubscriptionEntry[],
  donations: DonationEntry[],
): PaymentEntry[] {
  const rows: PaymentEntry[] = [
    ...subscriptions.map((entry): PaymentEntry => ({
      key: `s-${entry.id}`,
      kind: 'subscription',
      at: entry.startAt,
      entry,
    })),
    ...donations.map((entry): PaymentEntry => ({
      key: `d-${entry.id}`,
      kind: 'donation',
      at: entry.createdAt,
      entry,
    })),
  ];

  // ISO-строки `LocalDateTime` сравниваются как текст: `2026-09-04T10:00`
  // старше `2026-08-30T...` в любом порядке символов.
  return rows.sort((a, b) => (a.at < b.at ? 1 : a.at > b.at ? -1 : 0));
}
