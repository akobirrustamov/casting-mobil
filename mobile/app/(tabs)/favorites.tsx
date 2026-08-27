import { router } from 'expo-router';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FlatList, useWindowDimensions } from 'react-native';

import { useTabBarHeight } from '@/components/navigation/TabBar';
import { ScreenState } from '@/components/states/ScreenState';
import { CreatorCard } from '@/components/ui/CreatorCard';
import { Screen } from '@/components/ui/Screen';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import { useCreators } from '@/features/creators/api';
import { useFavoritesStore } from '@/features/favorites/store';
import { useIsOffline } from '@/lib/network';

/**
 * Сохранённое («Saqlanganlar») — вкладка таб-бара.
 *
 * Была отдельным экраном за пределами вкладок; на макете заказчика
 * (Landing Page) она стоит четвёртой в нижнем баре, поэтому переехала
 * в `(tabs)`. Путь не изменился: группа `(tabs)` в адрес не входит, и
 * переход из профиля по `/favorites` работает как раньше.
 *
 * Список id лежит локально, а сами анкеты берём из общего кэша — отдельного
 * запроса не делаем. Побочный эффект: если анкету убрали с витрины, из
 * избранного она пропадёт молча. Это честнее, чем показывать карточку,
 * которая никуда не открывается.
 */
const GAP = 12;
const PADDING = 16;
const COLUMNS = 2;

export default function FavoritesScreen() {
  const { t } = useTranslation();
  const { width } = useWindowDimensions();
  // Таб-бар плавающий: без этого отступа последний ряд карточек уезжает
  // под капсулу.
  const tabBarHeight = useTabBarHeight();

  const creators = useCreators();
  const isOffline = useIsOffline();
  const favoriteIds = useFavoritesStore((s) => s.ids);
  const toggleFavorite = useFavoritesStore((s) => s.toggle);

  const cardWidth = (width - PADDING * 2 - GAP * (COLUMNS - 1)) / COLUMNS;

  const items = useMemo(
    () => (creators.data ?? []).filter((c) => favoriteIds.has(c.id)),
    [creators.data, favoriteIds]
  );

  const title = t('profile.favorites');

  if (creators.isPending) {
    return (
      <Screen title={title} scroll={false}>
        <SkeletonGrid cardWidth={cardWidth} />
      </Screen>
    );
  }

  if (creators.isError) {
    return (
      <Screen title={title} scroll={false}>
        <ScreenState
          kind={isOffline ? 'offline' : 'error'}
          onRetry={() => creators.refetch()}
        />
      </Screen>
    );
  }

  return (
    <Screen
      title={title}
      subtitle={items.length > 0 ? t('catalog.found', { count: items.length }) : undefined}
      scroll={false}
    >
      {items.length === 0 ? (
        <ScreenState kind="empty" body={t('favorites.empty')} />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => String(item.id)}
          numColumns={COLUMNS}
          columnWrapperStyle={{ gap: GAP }}
          contentContainerStyle={{
            paddingHorizontal: PADDING,
            paddingBottom: tabBarHeight + 8,
            gap: GAP,
          }}
          showsVerticalScrollIndicator={false}
          renderItem={({ item }) => (
            <CreatorCard
              name={item.name}
              meta={[
                item.age ? t('common.years', { count: item.age }) : null,
                item.region,
              ]
                .filter(Boolean)
                .join(' • ')}
              imageUrl={item.photoUrls[0]}
              width={cardWidth}
              onPress={() => router.push(`/creator/${item.id}`)}
              isFavorite
              onToggleFavorite={() => toggleFavorite(item.id)}
            />
          )}
        />
      )}
    </Screen>
  );
}
