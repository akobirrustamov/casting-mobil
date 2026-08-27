import { useQuery } from '@tanstack/react-query';

import { useAuthStore } from '@/features/auth/store';
import { api } from '@/lib/api';

/**
 * Баланс во внутренних валютах — `GET /api/v1/app/donations/balance`.
 *
 * <h2>Что здесь есть и чего нет</h2>
 * Бэкенд знает две величины: `starsBalance` (Yulduzlar) и `coinBalance`
 * (UZCASTING coin). Баланса в сумах в модели НЕТ — на макете Screen 4 он
 * есть («Balance 56 000 so'm»), но подставлять туда число неоткуда, и
 * придумать его нельзя: человек решит, что у него есть деньги.
 *
 * <h2>Почему только для вошедших</h2>
 * Эндпоинт берёт пользователя из токена — id в параметрах нет намеренно,
 * иначе чужой баланс читался бы подменой числа в URL.
 */
export type Balance = {
  stars: number;
  coins: number;
};

/** Старая сборка бэкенда отдаёт на этот адрес index.html со статусом 200. */
export class BalanceUnavailableError extends Error {
  constructor() {
    super('/api/v1/app/donations/balance недоступен на этом сервере');
    this.name = 'BalanceUnavailableError';
  }
}

function count(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function mapBalance(raw: unknown): Balance {
  const r = raw as Record<string, unknown> | null;
  const stars = count(r?.starsBalance);
  const coins = count(r?.coinBalance);

  // Ноль здесь — настоящий ноль (бэкенд отдаёт его для нового счёта),
  // а вот отсутствие чисел означает, что ответил не тот сервер.
  if (stars === null || coins === null) throw new BalanceUnavailableError();

  return { stars, coins };
}

export function useBalance() {
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  return useQuery({
    queryKey: ['balance', userId],
    queryFn: async () => {
      const { data } = await api.get<unknown>('/api/v1/app/donations/balance');
      return mapBalance(data);
    },
    enabled: isAuthorized,
    retry: (failureCount, error) =>
      !(error instanceof BalanceUnavailableError) && failureCount < 2,
  });
}
