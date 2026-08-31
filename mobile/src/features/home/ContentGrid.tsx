import { useTranslation } from 'react-i18next';
import { View, useWindowDimensions } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import { rowRatio } from '@/features/content/orientation';
import { useIsOffline } from '@/lib/network';

import { HomeFeedUnavailableError, type useHomeFeed } from './api';
import { ContentPoster } from './sections';
import type { ContentCard } from './types';

/**
 * Сетка карточек контента — экран «Media» и экран одного ряда.
 *
 * <h2>Почему общий компонент</h2>
 * Оба экрана показывают один и тот же список одинаковыми карточками и
 * одинаково ведут себя в загрузке, ошибке и пустоте. Пока это было два
 * места, любая правка (три колонки вместо двух, таймкод, жанр) требовала
 * помнить про второе — а забытое второе выглядело бы как поломка.
 */

/** Три карточки в ряду — заказчик: «3 ta content». */
const COLUMNS = 3;
const GRID_GAP = 8;
const SCREEN_PADDING = 16;

export function ContentGrid({
  feed,
  cards,
  /**
   * Есть ли контент вообще. Пустая вкладка и пустой каталог — разные вещи:
   * в первом случае человек сам сузил выборку.
   */
  hasAny,
  emptyBody,
}: {
  feed: ReturnType<typeof useHomeFeed>;
  cards: ContentCard[];
  hasAny?: boolean;
  emptyBody?: string;
}) {
  const { t } = useTranslation();
  const { width } = useWindowDimensions();
  const isOffline = useIsOffline();

  // Ширина считается от экрана, а не задана числом: с фиксированной
  // шириной на узком телефоне влезали две карточки, на широком три —
  // сетка разъезжалась от устройства к устройству.
  const cardWidth =
    (width - SCREEN_PADDING * 2 - GRID_GAP * (COLUMNS - 1)) / COLUMNS;

  if (feed.isPending) {
    return (
      <View className="-mx-4">
        <SkeletonGrid count={6} cardWidth={cardWidth} />
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
        <ScreenState kind="empty" body={hasAny ? emptyBody : undefined} />
      </View>
    );
  }

  // Одна форма на всю сетку: вертикальная карточка рядом с обычной делает
  // строку разновысокой и подписи разъезжаются по вертикали.
  const ratio = rowRatio(cards.map((c) => c.orientation));

  return (
    <View className="flex-row flex-wrap" style={{ gap: GRID_GAP }}>
      {cards.map((card) => (
        <ContentPoster key={card.id} card={card} width={cardWidth} ratio={ratio} />
      ))}
    </View>
  );
}
