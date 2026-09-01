import { Ionicons } from '@expo/vector-icons';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { Screen } from '@/components/ui/Screen';
import { isVertical } from '@/features/content/orientation';
import { contentCards, useHomeFeed } from '@/features/home/api';
import { ContentGrid } from '@/features/home/ContentGrid';
import { HomeSectionView } from '@/features/home/sections';
import { colors } from '@/theme/tokens';

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
  { key: 'series', label: 'tabsSeries', icon: 'film-outline', types: ['SERIES', 'MINI_SERIES'] },
  { key: 'podcasts', label: 'tabsPodcasts', icon: 'mic-outline', types: ['PODCAST'] },
  { key: 'reels', label: 'tabsReels', icon: 'play-circle-outline', types: null, vertical: true },
  { key: 'clips', label: 'tabsClips', icon: 'musical-notes-outline', types: ['CLIP'] },
  { key: 'streams', label: 'tabsStreams', icon: 'radio-outline', types: ['STREAM'] },
  { key: 'shows', label: 'tabsShows', icon: 'sparkles-outline', types: ['SHOW'] },
  { key: 'movies', label: 'tabsMovies', icon: 'videocam-outline', types: ['MOVIE', 'SHORT_FILM'] },
] as const;

export default function PremiereScreen() {
  const { t } = useTranslation();
  const feed = useHomeFeed();


  const [active, setActive] = useState(0);

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

      {/* Вкладок семь, в строку они не помещаются — ряд прокручивается.
          Пять с макета стоят первыми и видны без прокрутки. */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerClassName="gap-2 pr-4"
      >
        {TABS.map((item, i) => (
          <Pressable
            key={item.key}
            onPress={() => setActive(i)}
            accessibilityRole="button"
            accessibilityState={{ selected: i === active }}
            className={`flex-row items-center gap-1.5 rounded-pill px-4 py-2 ${
              i === active ? 'bg-purple' : 'bg-surface'
            }`}
          >
            <Ionicons
              name={item.icon}
              size={14}
              color={i === active ? colors.white : colors.textMuted}
            />
            <Text
              className={`text-caption ${
                i === active ? 'font-semibold text-white' : 'text-text-muted'
              }`}
            >
              {t(`premiere.${item.label}`)}
            </Text>
          </Pressable>
        ))}
      </ScrollView>

      {/* Название раздела и счётчик — как на макете заказчика.
          ⚠️ Выпадашки сортировки рядом нет: сервер отдаёт один порядок
          (по дате публикации), и выбор из одного пункта был бы
          притворством. Появится вместе с параметром сортировки в API. */}
      {visible.length > 0 ? (
        <View className="flex-row items-baseline justify-between">
          <Text className="text-body font-semibold text-text">
            {t(`premiere.${TABS[active].label}`)}
          </Text>
          <Text className="text-caption text-text-muted">
            {t('premiere.count', { count: visible.length })}
          </Text>
        </View>
      ) : null}

      <ContentGrid
        feed={feed}
        cards={visible}
        hasAny={all.length > 0}
        emptyBody={t('premiere.emptyByTab')}
      />
    </Screen>
  );
}
