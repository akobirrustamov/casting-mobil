import { useLocalSearchParams } from 'expo-router';

import { DonorsScreen, parseCurrency } from '@/features/content/DonorsScreen';

/**
 * «Top 100 donatchilar» контента — `/donors/{contentId}?currency=…`.
 *
 * Сюда ведут плитки «Yulduzlar» и «Uzcasting» на странице контента и
 * такие же значки на кадре плеера. Валюта — в адресе, чтобы «назад» и
 * повторное открытие попадали в тот же рейтинг, с которого пришли.
 */
export default function DonorsRoute() {
  const { id, currency } = useLocalSearchParams<{ id: string; currency?: string }>();

  const parsed = Number(id);
  const contentId = Number.isFinite(parsed) ? parsed : null;

  return <DonorsScreen contentId={contentId} initialCurrency={parseCurrency(currency)} />;
}
