import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View, useWindowDimensions } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import {
  CARD_RATIO,
  gridGap,
  useRailCardWidth,
} from '@/features/content/railLayout';
import { useIsOffline } from '@/lib/network';

import { HomeFeedUnavailableError } from './api';
import { CardMenu } from './CardMenu';
import { ContentPoster } from './sections';
import type { ContentCard } from './types';

/**
 * Состояние запроса, из которого сетка рисует loading / error.
 *
 * Структурный тип, а не `ReturnType<typeof useHomeFeed>`: тем же экраном
 * пользуется страница одной категории (`/category/{id}`), где данные едут
 * из `/api/v1/app/catalog`, а не из фида. Форма у обоих запросов одна —
 * это `useQuery`, — и сетке достаточно знать ровно эти четыре поля.
 */
type FeedState = {
  isPending: boolean;
  isError: boolean;
  error: unknown;
  refetch: () => unknown;
};

/**
 * Сетка карточек контента — экран «Media» и экран одного ряда.
 *
 * <h2>Почему общий компонент</h2>
 * Оба экрана показывают один и тот же список одинаковыми карточками и
 * одинаково ведут себя в загрузке, ошибке и пустоте. Пока это было два
 * места, любая правка (три колонки вместо двух, таймкод, жанр) требовала
 * помнить про второе — а забытое второе выглядело бы как поломка.
 *
 * Меню карточки («⋮» с макета «Media») живёт ЗДЕСЬ, а не в карточке: лист
 * открыт один на всю сетку, иначе каждая из десятков карточек держала бы
 * собственное состояние и собственный `Modal`.
 */

export function ContentGrid({
  feed,
  cards,
  /**
   * Есть ли контент вообще. Пустая вкладка и пустой каталог — разные вещи:
   * в первом случае человек сам сузил выборку.
   */
  hasAny,
  emptyBody,
  unavailable,
}: {
  feed: FeedState;
  cards: ContentCard[];
  hasAny?: boolean;
  emptyBody?: string;
  /**
   * «Эндпоинта нет на этом сервере» — если запрос не из фида и определяет
   * это по своему типу ошибки. По умолчанию проверяется ошибка фида.
   */
  unavailable?: boolean;
}) {
  const { t } = useTranslation();
  const { width } = useWindowDimensions();
  const isOffline = useIsOffline();
  const [menuCard, setMenuCard] = useState<ContentCard | null>(null);

  // Та же ширина, что у карточки в ряду (`features/content/railLayout`).
  // Заказчик: «barchasi qilib ichiga kirgandan song ham cardlarni dizayni
  // o'zgarmasin». Раньше сетка считала ширину по своей формуле, и карточка
  // на экране «Barchasi» была шире, чем та, на которую нажали.
  const cardWidth = useRailCardWidth();
  const gap = gridGap(width);

  if (feed.isPending) {
    return (
      <View className="-mx-4">
        <SkeletonGrid count={6} cardWidth={cardWidth} ratio={CARD_RATIO} gap={gap} />
      </View>
    );
  }

  if (feed.isError) {
    const missing = unavailable ?? feed.error instanceof HomeFeedUnavailableError;
    return (
      <View className="h-64">
        <ScreenState
          kind={isOffline ? 'offline' : 'error'}
          title={missing ? t('home.feedUnavailableTitle') : undefined}
          body={missing ? t('home.feedUnavailableBody') : undefined}
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

  return (
    <>
      <View className="flex-row flex-wrap" style={{ gap }}>
        {cards.map((card) => (
          <ContentPoster
            key={card.id}
            card={card}
            width={cardWidth}
            onMenu={() => setMenuCard(card)}
          />
        ))}
      </View>

      <CardMenu card={menuCard} onClose={() => setMenuCard(null)} />
    </>
  );
}
