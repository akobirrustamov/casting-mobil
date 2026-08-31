import { useQuery } from '@tanstack/react-query';

import { ProfileUnavailableError, fetchMe } from '@/features/auth/api';
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

/**
 * Свой профиль и статус подписки — `GET /api/v1/app/me`.
 *
 * <h2>Почему запросом, а не из стора</h2>
 * Стор кладёт профиль в защищённое хранилище и обновляет его один раз при
 * запуске — до того, как смонтирован React. Подписка же ИСТЕКАЕТ: сохранить
 * «Premium активен» на диск значило бы показывать его и после окончания.
 * Поэтому статус живёт в кэше запросов и перечитывается вместе с экраном.
 *
 * ⚠️ Тот же адрес дёргает `auth/store` на старте. Это два обращения за
 * холодный запуск: у них разные фазы жизни (стор работает до React) и
 * общего кэша нет.
 */
export type Premium = { active: boolean; until: string | null };

export function useMe() {
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  return useQuery({
    queryKey: ['me', userId],
    queryFn: fetchMe,
    enabled: isAuthorized,
    // Подписка могла закончиться, пока экран лежал в фоне.
    staleTime: 0,
    retry: (failureCount, error) =>
      !(error instanceof ProfileUnavailableError) && failureCount < 2,
  });
}

/**
 * Три состояния подписки — их различает `/app/me`:
 *   активна          — `active: true`, срок в будущем
 *   истекла          — `active: false`, но срок есть
 *   не было никогда  — `active: false`, срок `null`
 *
 * «Истекла» и «не было» — разные вещи для человека: в первом случае он
 * платил и захочет продлить.
 */
export type PremiumState = 'active' | 'expired' | 'none';

export function premiumState(premium: Premium | undefined): PremiumState {
  if (!premium) return 'none';
  if (premium.active) return 'active';
  return premium.until ? 'expired' : 'none';
}

/**
 * Дата в виде `30.09.2026`.
 *
 * Своё форматирование, а не `toLocaleDateString`: поддержка `Intl` в Hermes
 * зависит от сборки, и «работает у меня» здесь означало бы пустую строку на
 * чужом телефоне. Бэкенд отдаёт `LocalDateTime` без зоны — берём как есть.
 */
export function formatDate(iso: string | null): string | null {
  if (!iso) return null;
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  return m ? `${m[3]}.${m[2]}.${m[1]}` : null;
}
