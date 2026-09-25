import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { AppState } from 'react-native';

import { api } from '@/lib/api';

/**
 * Настройки приложения — `GET /api/v1/app/config` (открытый).
 *
 * `paymentsVisible` — показывать ли всё, что связано с оплатой: Premium,
 * тарифы, подписку, промокоды, баланс, донаты, кнопки покупки. Включает
 * SUPER_ADMIN в админке («Sozlamalar»).
 *
 * ⚠️ По умолчанию — СКРЫТО. Пока ответа нет или запрос упал, платёжные
 * блоки не показываются: мигнуть и исчезнуть хуже, чем появиться чуть позже.
 */
export type AppConfig = {
  paymentsVisible: boolean;
};

/**
 * ⚠️ Ошибка НЕ пробрасывается. На сервере без этого эндпоинта (старая
 * сборка) приходит 401/404 — это значит «настройки нет», а не поломка:
 * платежи просто остаются скрытыми. Проброшенная ошибка запускала бы
 * обновление токена и повторы на каждом экране.
 */
export async function fetchAppConfig(): Promise<AppConfig> {
  try {
    const { data } = await api.get<Partial<AppConfig>>('/api/v1/app/config');
    return { paymentsVisible: data?.paymentsVisible === true };
  } catch {
    return { paymentsVisible: false };
  }
}

const CONFIG_KEY = ['app-config'];

/**
 * Чтение настройки — для экранов и карточек.
 *
 * ⚠️ Здесь НЕТ ни опроса, ни перечитывания при монтировании: хук стоит в
 * каждой карточке ленты, и таймер на каждую карточку давал бы десятки
 * запросов при прокрутке. Свежесть держит один {@link useAppConfigSync}
 * в корне — он обновляет кэш, а эти читатели просто перерисовываются.
 */
export function useAppConfig() {
  return useQuery({
    queryKey: CONFIG_KEY,
    queryFn: fetchAppConfig,
    staleTime: Infinity,
    retry: false,
  });
}

/**
 * Единственный источник обновлений — вызывается ОДИН раз в `app/_layout`.
 *
 * Вкладки не размонтируются, а фокус приложения react-query в RN сам не
 * видит — без этого настройку читали бы один раз за запуск, и
 * переключатель в админке доходил бы до телефона только после
 * перезапуска. Поэтому: опрос раз в 30 с и перечитывание при возврате в
 * приложение.
 */
export function useAppConfigSync(): void {
  const { refetch } = useQuery({
    queryKey: CONFIG_KEY,
    queryFn: fetchAppConfig,
    staleTime: 0,
    refetchInterval: 30_000,
    retry: false,
  });

  useEffect(() => {
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') void refetch();
    });
    return () => sub.remove();
  }, [refetch]);
}

/** Видны ли платёжные разделы. Пока неизвестно — `false`. */
export function usePaymentsVisible(): boolean {
  return useAppConfig().data?.paymentsVisible === true;
}
