import { useQuery } from '@tanstack/react-query';

import { useViewerKey } from '@/features/watch/api';
import type { BannerCard } from '@/features/home/types';
import { useFeedLanguage } from '@/features/home/api';
import { api } from '@/lib/api';

/**
 * Баннер поверх экрана — «Majburiy reklama» (макет заказчика, 09.09.2026).
 *
 * <h2>Кто решает, показывать ли его</h2>
 * Только сервер. Коммерческая реклама уходит лишь тем, у кого НЕТ
 * активного тарифа, объявления админа — всем; полноэкранным баннер
 * становится, когда админ выбрал это в панели. Клиент ничего не
 * досчитывает: продублируй он правило у себя — и Premium однажды
 * увидел бы рекламу, за отсутствие которой заплатил.
 *
 * <h2>⚠️ Пустой ответ — это 204, а не `null` в теле</h2>
 * «Показывать нечего» и «ответ пришёл, но пустой» для клиента должны
 * выглядеть одинаково спокойно, поэтому 204 превращается в `null` здесь,
 * а не в компоненте.
 */
const LOCALE_PARAM = { uz: 'UZ', ru: 'RU', en: 'EN' } as const;

async function fetchInterstitial(locale: 'UZ' | 'RU' | 'EN'): Promise<BannerCard | null> {
  const response = await api.get<BannerCard | null>('/api/v1/app/ads/interstitial', {
    params: { locale },
    // 204 — обычный ответ, а не сбой.
    validateStatus: (status) => status === 200 || status === 204,
  });

  return response.status === 204 ? null : (response.data ?? null);
}

export function useInterstitialAd() {
  const language = useFeedLanguage();
  const viewer = useViewerKey();

  return useQuery({
    /**
     * ⚠️ `viewer` в ключе обязателен: право на «без рекламы» у гостя и у
     * подписчика разное. Без него человек, купивший Premium, продолжал бы
     * получать баннер из кэша, снятого до входа.
     */
    queryKey: ['interstitial-ad', language, viewer],
    queryFn: () => fetchInterstitial(LOCALE_PARAM[language]),

    // Реклама не та вещь, ради которой стоит долбить сервер: одного
    // запроса на запуск приложения достаточно.
    staleTime: Infinity,
    retry: false,
  });
}
