import { router, useLocalSearchParams } from 'expo-router';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, View } from 'react-native';

import { Screen } from '@/components/ui/Screen';
import {
  CatalogUnavailableError,
  useCategoryPages,
} from '@/features/catalog/contentCategories';
import { ContentGrid } from '@/features/home/ContentGrid';
import { colors } from '@/theme/tokens';

/**
 * Один раздел каталога целиком — сюда ведёт «Barchasi ›» с ряда категории.
 *
 * <h2>Почему не `/section/{id}`</h2>
 * Тот экран берёт РОВНО содержимое своего ряда главной и живёт в кэше
 * `/api/v1/app/home`. Раздел каталога — другая сущность и другой источник
 * (`/api/v1/app/catalog/categories/{id}`), и id у них свои: открыть один
 * по id другого значило бы показать посторонний список.
 *
 * <h2>Страницами по двадцать</h2>
 * Заказчик: «barchasini 20ta 20ta scroll qilganda get qilib bera verasan».
 * Следующая страница запрашивается при подходе к концу списка, а не
 * кнопкой: человек уже листает, и лишнее действие тут ни к чему.
 *
 * Подзаголовок показывает `total` — сколько в разделе ВСЕГО, а не сколько
 * успело загрузиться. Иначе число росло бы на глазах при прокрутке и
 * ничего бы не значило.
 */
export default function CategoryScreen() {
  const { t } = useTranslation();
  const { id } = useLocalSearchParams<{ id: string }>();

  const parsed = Number(id);
  const categoryId = Number.isFinite(parsed) ? parsed : null;

  const pages = useCategoryPages(categoryId);

  const cards = useMemo(
    () => (pages.data?.pages ?? []).flatMap((p) => p.items),
    [pages.data]
  );

  const head = pages.data?.pages[0];

  return (
    <Screen
      title={head?.name ?? t('common.seeAll')}
      subtitle={head ? t('premiere.count', { count: head.total }) : undefined}
      onBack={() => router.back()}
      underTabBar={false}
      onRefresh={() => pages.refetch()}
      refreshing={pages.isRefetching && !pages.isFetchingNextPage}
      // Экран вызывает это на каждом подходящем кадре прокрутки —
      // решение «нужен ли запрос» принимается здесь, где известны обе
      // части состояния. Без второй проверки одна прокрутка запускала бы
      // несколько запросов одной и той же страницы.
      onEndReached={() => {
        if (pages.hasNextPage && !pages.isFetchingNextPage) {
          void pages.fetchNextPage();
        }
      }}
    >
      <ContentGrid
        feed={pages}
        cards={cards}
        unavailable={pages.error instanceof CatalogUnavailableError}
      />

      {/* Индикатор ТОЛЬКО для догрузки: первая страница рисует скелетон
          сетки, и крутилка под ним выглядела бы как вторая загрузка. */}
      {pages.isFetchingNextPage ? (
        <View className="py-4">
          <ActivityIndicator color={colors.purple} />
        </View>
      ) : null}
    </Screen>
  );
}
