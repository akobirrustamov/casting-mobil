import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import { contentCards, HomeFeedUnavailableError, useHomeFeed } from '@/features/home/api';
import { rowRatio } from '@/features/content/orientation';
import { ContentPoster, HomeSectionView } from '@/features/home/sections';
import type { ContentCard } from '@/features/home/types';
import { useIsOffline } from '@/lib/network';

/**
 * Каталог премьер. По ТЗ (V3, стр. 17 «16. Premyera katalogi»):
 * табы Seriallar/Ko'rsatuvlar/Filmlar · premiere badge · exclusive status.
 *
 * <h2>Откуда что берётся</h2>
 * Сверху — секция `NEW_PREMIERES` из `GET /api/v1/app/home`: это промо-баннеры
 * премьер, которыми управляет админ-панель. Ниже — карточки контента из того же
 * фида, отфильтрованные по табам.
 *
 * <h2>Почему табы фильтруют по `contentType`, а не по категории</h2>
 * ТЗ §13: тип, категория и жанр — три разные оси. «Сериалы» и «Фильмы» —
 * это ТИП контента; фильтровать их по категории каталога значило бы смешать
 * несмешиваемое (в «Драме» лежат и сериал, и подкаст, и фильм).
 *
 * ⚠️ Цены на карточках нет намеренно: фид её не отдаёт — она приходит вместе
 * с правом доступа из `/api/v1/app/watch/{episodeId}` (экран 17).
 */
const TABS = [
  { key: 'tabsAll', types: null },
  { key: 'tabsSeries', types: ['SERIES', 'MINI_SERIES'] },
  { key: 'tabsShows', types: ['SHOW'] },
  { key: 'tabsMovies', types: ['MOVIE', 'SHORT_FILM'] },
] as const;

const CARD_WIDTH = 158;

export default function PremiereScreen() {
  const { t } = useTranslation();
  const [active, setActive] = useState(0);
  const feed = useHomeFeed();
  const isOffline = useIsOffline();

  const all = useMemo(() => contentCards(feed.data), [feed.data]);

  const visible = useMemo(() => {
    const types = TABS[active].types;
    if (!types) return all;
    return all.filter(
      (c) => c.contentType !== null && (types as readonly string[]).includes(c.contentType)
    );
  }, [all, active]);

  const premieres = feed.data?.sections.find((s) => s.type === 'NEW_PREMIERES');

  return (
    <Screen
      title={t('premiere.title')}
      subtitle={t('premiere.subtitle')}
      onRefresh={() => feed.refetch()}
      refreshing={feed.isRefetching}
    >
      {premieres ? <HomeSectionView section={premieres} /> : null}

      <View className="flex-row gap-2">
        {TABS.map((tab, i) => (
          <Pressable
            key={tab.key}
            onPress={() => setActive(i)}
            accessibilityRole="button"
            accessibilityState={{ selected: i === active }}
            className={`rounded-pill px-4 py-2 ${
              i === active ? 'bg-purple' : 'bg-surface'
            }`}
          >
            <Text
              className={`text-caption ${
                i === active ? 'font-semibold text-white' : 'text-text-muted'
              }`}
            >
              {t(`premiere.${tab.key}`)}
            </Text>
          </Pressable>
        ))}
      </View>

      <PremiereGrid
        feed={feed}
        cards={visible}
        hasAny={all.length > 0}
        isOffline={isOffline}
      />
    </Screen>
  );
}

function PremiereGrid({
  feed,
  cards,
  hasAny,
  isOffline,
}: {
  feed: ReturnType<typeof useHomeFeed>;
  cards: ContentCard[];
  /** Есть ли контент вообще — чтобы отличить «пусто в табе» от «пусто везде». */
  hasAny: boolean;
  isOffline: boolean;
}) {
  const { t } = useTranslation();

  if (feed.isPending) {
    return (
      <View className="-mx-4">
        <SkeletonGrid count={6} cardWidth={CARD_WIDTH} />
      </View>
    );
  }

  if (feed.isError) {
    const unavailable = feed.error instanceof HomeFeedUnavailableError;
    return (
      <View className="h-64">
        <ScreenState
          kind={isOffline ? 'offline' : 'error'}
          title={unavailable ? t('home.feedUnavailableTitle') : undefined}
          body={unavailable ? t('home.feedUnavailableBody') : undefined}
          onRetry={() => feed.refetch()}
        />
      </View>
    );
  }

  if (cards.length === 0) {
    return (
      <View className="h-64">
        {/* Пустой таб и пустой каталог — разные вещи: в первом случае человек
            сам сузил выборку и подсказка должна вести обратно к табам. */}
        <ScreenState kind="empty" body={hasAny ? t('premiere.emptyByTab') : undefined} />
      </View>
    );
  }

  // Сетка переносит карточки по строкам, поэтому форма должна быть одна:
  // вертикальная карточка рядом с обычной делает строку разновысокой.
  const ratio = rowRatio(cards.map((c) => c.orientation));

  return (
    <View className="flex-row flex-wrap justify-between gap-y-4">
      {cards.map((card) => (
        <ContentPoster key={card.id} card={card} width={CARD_WIDTH} ratio={ratio} />
      ))}
    </View>
  );
}
