import { router, useLocalSearchParams } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { Screen } from '@/components/ui/Screen';
import { ContentGrid } from '@/features/home/ContentGrid';
import { useHomeFeed } from '@/features/home/api';

/**
 * Один ряд главной целиком — сюда ведёт «Barchasi ›».
 *
 * <h2>Почему отдельный экран, а не вкладка «Media»</h2>
 * Сначала «Barchasi» открывала «Media» с подходящей вкладкой. Набор там
 * получался ПОХОЖИЙ, но другой: ряд «Mini seriallar» собран по типу
 * `MINI_SERIES`, а вкладка «Seriallar» показывает ещё и `SERIES`. Человек
 * нажимал «все» и видел не то, что было в ряду.
 *
 * Заказчик сказал прямо: кнопка открывает страницу именно с этими
 * карточками. Поэтому экран берёт РОВНО содержимое своей секции.
 *
 * <h2>Откуда данные</h2>
 * Из того же `GET /api/v1/app/home`, что и главная — запрос уже в кэше,
 * повторный не уходит. Значит и лимит тот же: сервер отдаёт по десять
 * карточек на ряд (настройка `itemLimit` в админке). Когда появится
 * `/api/v1/app/catalog` с постраничной выдачей, экран возьмёт данные
 * оттуда — раскладка не изменится.
 */
export default function SectionScreen() {
  const { t } = useTranslation();
  const { id } = useLocalSearchParams<{ id: string }>();

  const feed = useHomeFeed();

  const parsed = Number(id);
  const sectionId = Number.isFinite(parsed) ? parsed : null;
  const section = feed.data?.sections.find((s) => s.id === sectionId);

  const cards = section?.content ?? [];

  return (
    <Screen
      title={section?.title ?? t('common.seeAll')}
      subtitle={
        cards.length > 0 ? t('premiere.count', { count: cards.length }) : undefined
      }
      onBack={() => router.back()}
      underTabBar={false}
      onRefresh={() => feed.refetch()}
      refreshing={feed.isRefetching}
    >
      <ContentGrid feed={feed} cards={cards} />
    </Screen>
  );
}
