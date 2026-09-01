import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Rail } from '@/components/ui/Rail';
import { SkeletonRail } from '@/components/ui/Skeleton';
import { CARD_RATIO, useRailCardWidth } from '@/features/content/railLayout';
import { ContentPoster } from '@/features/home/sections';
import { useIsOffline } from '@/lib/network';

import {
  CatalogUnavailableError,
  ROW_SIZE,
  useCategoryRows,
  type CategoryRow,
} from './contentCategories';

/**
 * Ряды разделов каталога на главной: «Drama», под ним карточки — ровно
 * такой же ряд, как «Podkastlar» или «Mini seriallar».
 *
 * <h2>Что здесь общего с рядами фида</h2>
 * Карточка — тот же `ContentPoster`, рельс — тот же `Rail`. Это не
 * экономия строк: ряд из фида и ряд категории стоят на экране рядом, и
 * любое расхождение (другая пропорция, другой бейдж) читалось бы как
 * поломка одного из них.
 *
 * <h2>Ряды приходят по одному</h2>
 * Каждый ряд — свой запрос (`useCategoryRows`). Поэтому верхний рисуется,
 * не дожидаясь нижних, а упавший ряд не уносит соседей: он просто не
 * появляется, остальные работают.
 *
 * <h2>Три карточки в кадре, десять в ряду</h2>
 * Заказчик: «kamida 3ta card ko'rinishi majburiy, 10ta get bo'ladi ammo
 * 3ta ko'rinadi, scroll qilsa qolganini ko'rsatasan». Ширина и форма
 * карточки — общие для всего приложения (`features/content/railLayout`),
 * поэтому ряд категории неотличим от ряда фида, а «Barchasi ›» открывает
 * такие же карточки, а не другие.
 */
export function CategoryRows({ size = ROW_SIZE }: { size?: number }) {
  const { t } = useTranslation();
  const isOffline = useIsOffline();
  const { list, rows } = useCategoryRows(size);

  const cardWidth = useRailCardWidth();

  if (list.isPending) {
    return (
      <View className="gap-3">
        <View className="-mx-4">
          {/* Заглушки той же ширины, что будущие карточки — иначе ряд
              «прыгает» в момент, когда данные приезжают. */}
          <SkeletonRail count={3} width={cardWidth} height={cardWidth / CARD_RATIO} />
        </View>
      </View>
    );
  }

  if (list.isError) {
    // Старый бэкенд без `/api/v1/app/catalog` — это не сбой, а сервер, на
    // котором раздела просто нет. Главная в этот момент уже показывает
    // своё сообщение про недоступный фид; второй такой же блок был бы
    // шумом, а не информацией.
    if (list.error instanceof CatalogUnavailableError) {
      return null;
    }
    return (
      <View className="h-48">
        <ScreenState
          kind={isOffline ? 'offline' : 'error'}
          title={t('home.categoryRowsError')}
          onRetry={() => list.refetch()}
        />
      </View>
    );
  }

  if (rows.length === 0) {
    return null;
  }

  return (
    <View className="gap-4">
      {rows.map((row) => (
        <CategoryRowView key={row.head.id} row={row} cardWidth={cardWidth} />
      ))}
    </View>
  );
}

/**
 * Один ряд.
 *
 * Пока карточки едут, заголовок уже известен из списка — поэтому вместо
 * безымянного скелетона видно, ЧТО именно грузится.
 */
function CategoryRowView({
  row,
  cardWidth,
}: {
  row: CategoryRow;
  cardWidth: number;
}) {
  const { head, query } = row;
  const title = head.name ?? '';

  if (query.isPending) {
    return (
      <View className="gap-3">
        <Text numberOfLines={1} className="text-h2 text-text">
          {title}
        </Text>
        <View className="-mx-4">
          <SkeletonRail count={3} width={cardWidth} height={cardWidth / CARD_RATIO} />
        </View>
      </View>
    );
  }

  // Один упавший раздел не должен рисовать «ошибку» посреди работающей
  // главной: список категорий цел, соседние ряды пришли. Ряд просто не
  // показывается — придумывать вместо него карточки нельзя.
  if (query.isError || query.data === undefined) {
    return null;
  }

  const cards = query.data.items;
  if (cards.length === 0) {
    return null;
  }

  return (
    <Rail
      title={title}
      // «Barchasi ›» только если за пределами ряда что-то есть. Кнопка,
      // открывающая ровно те же карточки, обманывает ожидание.
      onSeeAll={
        query.data.total > cards.length
          ? () => router.push(`/category/${head.id}`)
          : undefined
      }
    >
      {cards.map((card) => (
        <ContentPoster key={card.id} card={card} width={cardWidth} />
      ))}
    </Rail>
  );
}
