import { useLocalSearchParams } from 'expo-router';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import { contentCards, HomeFeedUnavailableError, useHomeFeed } from '@/features/home/api';
import { isVertical, rowRatio } from '@/features/content/orientation';
import { ContentPoster, HomeSectionView } from '@/features/home/sections';
import type { ContentCard } from '@/features/home/types';
import { useIsOffline } from '@/lib/network';

/**
 * Каталог премьер. По ТЗ (V3, стр. 17 «16. Premyera katalogi»):
 * premiere badge · exclusive status.
 *
 * <h2>Откуда что берётся</h2>
 * Сверху — секция `NEW_PREMIERES` из `GET /api/v1/app/home`: это промо-баннеры
 * премьер, которыми управляет админ-панель. Ниже — карточки контента из того же
 * фида.
 *
 * ⚠️ Цены на карточках нет намеренно: фид её не отдаёт — она приходит вместе
 * с правом доступа из `/api/v1/app/watch/{episodeId}` (экран 17).
 */
/**
 * Разделы каталога. Заказчик (31.08.2026) убрал ряд вкладок с экрана: сам
 * список открывается целиком, без фильтра сверху.
 *
 * ⚠️ Разделы при этом остались — по ним приходит переход «Barchasi ›» с
 * главной (`features/home/seeAll`): человек нажал на ряде «Подкасты» и должен
 * увидеть подкасты, а не весь каталог. Видимого переключателя нет, фильтр
 * задаёт только адрес.
 *
 * ⚠️ «Reels seriallar» фильтруется по ФОРМАТУ, а не по типу: вертикальным
 * бывает и мини-сериал, и клип (ТЗ §13 — оси независимы). Фильтруй его по
 * `contentType` — и половина рилсов пропала бы.
 */
const TABS = [
  { key: 'series', types: ['SERIES', 'MINI_SERIES'] },
  { key: 'podcasts', types: ['PODCAST'] },
  { key: 'reels', types: null, vertical: true },
  { key: 'clips', types: ['CLIP'] },
  { key: 'streams', types: ['STREAM'] },
  { key: 'shows', types: ['SHOW'] },
  { key: 'movies', types: ['MOVIE', 'SHORT_FILM'] },
] as const;

const CARD_WIDTH = 158;

export default function PremiereScreen() {
  const { t } = useTranslation();
  const feed = useHomeFeed();
  const isOffline = useIsOffline();

  // Раздел приходит из ряда на главной. Незнакомый ключ — весь каталог:
  // экран не должен падать из-за адреса, набранного руками.
  const { tab } = useLocalSearchParams<{ tab?: string }>();
  const current = useMemo(() => TABS.find((x) => x.key === tab) ?? null, [tab]);

  const all = useMemo(() => contentCards(feed.data), [feed.data]);

  const visible = useMemo(() => {
    if (!current) return all;
    if ('vertical' in current && current.vertical) {
      return all.filter((c) => isVertical(c.orientation));
    }
    const types = current.types;
    if (!types) return all;
    return all.filter(
      (c) => c.contentType !== null && (types as readonly string[]).includes(c.contentType)
    );
  }, [all, current]);

  const premieres = feed.data?.sections.find((s) => s.type === 'NEW_PREMIERES');

  return (
    <Screen
      title={t('premiere.title')}
      subtitle={t('premiere.subtitle')}
      onRefresh={() => feed.refetch()}
      refreshing={feed.isRefetching}
    >
      {premieres ? <HomeSectionView section={premieres} /> : null}

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
  /** Есть ли контент вообще — чтобы отличить «пусто в разделе» от «пусто везде». */
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
        {/* Пустой раздел и пустой каталог — разные вещи: в первом случае
            выборку сузил переход с главной, и подсказка должна отличаться. */}
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
