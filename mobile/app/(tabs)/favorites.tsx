import { router } from 'expo-router';
import { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FlatList, ScrollView, Text, View, useWindowDimensions } from 'react-native';

import { useTabBarHeight } from '@/components/navigation/TabBar';
import { ScreenState } from '@/components/states/ScreenState';
import { CreatorCard } from '@/components/ui/CreatorCard';
import { PosterCard } from '@/components/ui/PosterCard';
import { Screen } from '@/components/ui/Screen';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import { CARD_RATIO, useRailCardWidth } from '@/features/content/railLayout';
import { useCreators } from '@/features/creators/api';
import { useAuthStore } from '@/features/auth/store';
import { useContentFavorites } from '@/features/favorites/content';
import { useFavoritesStore } from '@/features/favorites/store';
import { contentCards, useHomeFeed } from '@/features/home/api';
import { mediaUrl } from '@/lib/api';
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
 *
 * <h2>Сохранённый КОНТЕНТ — отдельным рядом сверху</h2>
 * Закладка на странице фильма появилась вместе с новым экраном контента
 * (макет заказчика, 08.09.2026). Складывать фильмы и анкеты кастинга в
 * одну сетку нельзя: это разные сущности с разной формой карточки и
 * разными переходами, и «Saqlanganlar» превратился бы в кашу.
 *
 * ⚠️ Карточки фильмов берутся из того же кэша главной. Значит контент, не
 * попавший ни в один ряд главной, в списке не покажется — ровно то же
 * ограничение, что у анкет выше, и по той же причине: отдельного запроса
 * «дай карточки по списку id» на сервере нет.
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

  // ⚠️ Список закладок живёт ТОЛЬКО на сервере, локальной копии у него
  // нет (см. `features/favorites/content`). Значит открыть вкладку — это
  // и есть момент, когда его надо спросить.
  const signedIn = useAuthStore((s) => s.isAuthorized);
  const savedIds = useContentFavorites((s) => s.ids);
  const loadSaved = useContentFavorites((s) => s.load);
  const savedLoaded = useContentFavorites((s) => s.isLoaded);

  useEffect(() => {
    if (signedIn && !savedLoaded) void loadSaved();
  }, [signedIn, savedLoaded, loadSaved]);

  const feed = useHomeFeed();
  const railCardWidth = useRailCardWidth();
  const savedContent = useMemo(
    () => contentCards(feed.data).filter((c) => savedIds.has(c.id)),
    [feed.data, savedIds]
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
      {items.length === 0 && savedContent.length === 0 ? (
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
          // ⚠️ Ряд фильмов — ШАПКА этого же списка, а не отдельный
          // ScrollView над ним. Два вложенных вертикальных списка на
          // одном экране прокручиваются рывками и ловят нажатия друг
          // друга.
          ListHeaderComponent={
            <SavedContentRail
              cards={savedContent}
              cardWidth={railCardWidth}
              creatorsBelow={items.length > 0}
            />
          }
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

/**
 * Ряд сохранённых фильмов и сериалов.
 *
 * Горизонтальный, а не сетка: под ним идёт сетка анкет кастинга, и две
 * сетки подряд без единого разделителя читались бы как один список из
 * карточек разной формы.
 */
function SavedContentRail({
  cards,
  cardWidth,
  creatorsBelow,
}: {
  cards: ReturnType<typeof contentCards>;
  cardWidth: number;
  /** Есть ли под рядом сетка анкет — от этого зависит нижний отступ. */
  creatorsBelow: boolean;
}) {
  const { t } = useTranslation();

  if (cards.length === 0) return null;

  return (
    <View style={{ marginBottom: creatorsBelow ? 20 : 0 }} className="gap-3">
      <Text className="text-h2 text-text">{t('favorites.contentTitle')}</Text>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        // Ряд выходит за поля списка: карточка у правого края должна
        // «подглядывать», иначе ряд выглядит законченным и его не листают.
        style={{ marginHorizontal: -PADDING }}
        contentContainerStyle={{ gap: GAP, paddingHorizontal: PADDING }}
      >
        {cards.map((card) => (
          <PosterCard
            key={card.id}
            title={card.title ?? ''}
            meta={card.genre ?? undefined}
            imageUrl={mediaUrl(card.posterMediaId)}
            width={cardWidth}
            ratio={CARD_RATIO}
            onPress={() => router.push(`/content/${card.id}`)}
          />
        ))}
      </ScrollView>

      {creatorsBelow ? (
        <Text className="text-h2 text-text">{t('favorites.creatorsTitle')}</Text>
      ) : null}
    </View>
  );
}
