import { useQuery } from '@tanstack/react-query';

import { useAuthStore } from '@/features/auth/store';
import { api } from '@/lib/api';

/**
 * Баланс во внутренних валютах — `GET /api/v1/app/donations/balance`.
 *
 * <h2>Три величины с макета Screen 4</h2>
 * `moneyBalance` (Balance, сумы), `starsBalance` (Yulduzlar) и
 * `coinBalance` (Uzcasting).
 *
 * ⚠️ Сумовый баланс в `UserBalance` был всегда, но DTO его не отдавал —
 * поэтому сначала на экране стоял прочерк. Поле добавлено в ответ, и
 * теперь все три числа приходят с сервера. Придумывать их нельзя:
 * человек решит, что у него есть деньги.
 *
 * <h2>Почему только для вошедших</h2>
 * Эндпоинт берёт пользователя из токена — id в параметрах нет намеренно,
 * иначе чужой баланс читался бы подменой числа в URL.
 */
export type Balance = {
  /** Сумы. Дробная часть у сумм не используется, но приходит как число. */
  money: number;
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
  const money = count(r?.moneyBalance);
  const stars = count(r?.starsBalance);
  const coins = count(r?.coinBalance);

  // Ноль здесь — настоящий ноль (бэкенд отдаёт его для нового счёта),
  // а вот отсутствие чисел означает, что ответил не тот сервер.
  if (money === null || stars === null || coins === null) {
    throw new BalanceUnavailableError();
  }

  return { money, stars, coins };
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
