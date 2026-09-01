import { Ionicons } from '@expo/vector-icons';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';

import { Screen } from '@/components/ui/Screen';
import { isVertical } from '@/features/content/orientation';
import { contentCards, useHomeFeed } from '@/features/home/api';
import { ContentGrid } from '@/features/home/ContentGrid';
import { GenreSheet } from '@/features/home/GenreSheet';
import { TOUCH_TARGET, colors } from '@/theme/tokens';

/**
 * Каталог «Media». По ТЗ (V3, стр. 17 «16. Premyera katalogi»):
 * табы Seriallar/Ko'rsatuvlar/Filmlar · premiere badge · exclusive status.
 *
 * <h2>Откуда что берётся</h2>
 * Карточки — из `GET /api/v1/app/home`, отфильтрованные по табам.
 *
 * ⚠️ Промо-баннеров премьер (`NEW_PREMIERES`) здесь БОЛЬШЕ НЕТ: на макете
 * заказчика (01.09.2026) экран начинается сразу с вкладок, а баннер над
 * строкой поиска закрывал собой то, что человек ищет. Секция не потеряна —
 * её, как и все остальные ряды фида, показывает главная.
 *
 * <h2>Почему табы фильтруют по `contentType`, а не по категории</h2>
 * ТЗ §13: тип, категория и жанр — три разные оси. «Сериалы» и «Фильмы» —
 * это ТИП контента; фильтровать их по категории каталога значило бы смешать
 * несмешиваемое (в «Драме» лежат и сериал, и подкаст, и фильм).
 *
 * <h2>Шапка: поиск и фильтр</h2>
 * Два знака справа — с макета заказчика (01.09.2026). Оба работают ПО УЖЕ
 * ЗАГРУЖЕННОМУ фиду, ничего не спрашивая у сервера:
 *
 *   - лупа раскрывает строку и отбирает карточки по названию;
 *   - ползунки открывают выбор жанра (`GenreSheet`).
 *
 * ⚠️ Лупа НЕ ведёт на `/search`: тот экран ищет анкеты кастинга, а не
 * контент, и человек с «Media» попал бы в чужой список.
 *
 * ⚠️ Выпадашки сортировки («Yangi avval») на экране нет: заказчик вычеркнул
 * её на макете, да и сервер отдаёт один порядок — по дате публикации.
 *
 * ⚠️ Цены на карточках нет намеренно: фид её не отдаёт — она приходит вместе
 * с правом доступа из `/api/v1/app/watch/{episodeId}` (экран 17).
 */
/**
 * Вкладки — с макета заказчика (28.08.2026):
 * Seriallar · Podkastlar · Reels seriallar · Kliplar · Stream.
 *
 * ⚠️ «Shoular» и «Filmlar» добавлены СВЕРХ макета. На макете их нет, но
 * без них выпуски шоу и фильмы не открывались бы отсюда вообще — контент
 * в базе есть, а вкладки под него нет. Ряд прокручивается, поэтому пять
 * с макета стоят первыми и видны сразу.
 *
 * ⚠️ «Reels seriallar» фильтруется по ФОРМАТУ, а не по типу: вертикальным
 * бывает и мини-сериал, и клип (ТЗ §13 — оси независимы). Фильтруй его по
 * `contentType` — и половина рилсов пропала бы.
 *
 * Вкладка выбирается только тапом: параметра `?tab=` в адресе больше нет —
 * «Barchasi ›» с главной с 31.08.2026 открывает свой экран ряда
 * (`app/section/[id].tsx`), а не этот каталог.
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
  const [genre, setGenre] = useState<string | null>(null);
  const [genreOpen, setGenreOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState('');

  const all = useMemo(() => contentCards(feed.data), [feed.data]);

  /** Карточки текущей вкладки — до жанра и поиска. По ним же считаем жанры. */
  const byTab = useMemo(() => {
    const current = TABS[active];
    if ('vertical' in current && current.vertical) {
      return all.filter((c) => isVertical(c.orientation));
    }
    const types = current.types;
    if (!types) return all;
    return all.filter(
      (c) => c.contentType !== null && (types as readonly string[]).includes(c.contentType)
    );
  }, [all, active]);

  const genres = useMemo(() => {
    const found = new Set<string>();
    for (const card of byTab) {
      if (card.genre) found.add(card.genre);
    }
    return [...found].sort((a, b) => a.localeCompare(b));
  }, [byTab]);

  const trimmed = query.trim().toLowerCase();

  const visible = useMemo(
    () =>
      byTab.filter((c) => {
        if (genre && c.genre !== genre) return false;
        if (trimmed && !(c.title ?? '').toLowerCase().includes(trimmed)) return false;
        return true;
      }),
    [byTab, genre, trimmed]
  );

  const narrowed = genre !== null || trimmed.length > 0;

  // Смена вкладки сбрасывает жанр: в «Kliplar» жанра «Drama» может не быть
  // вовсе, и человек попал бы на пустой экран, ничего для этого не сделав.
  const selectTab = (index: number) => {
    setActive(index);
    setGenre(null);
  };

  return (
    <Screen
      title={t('premiere.title')}
      subtitle={t('premiere.subtitle')}
      onRefresh={() => feed.refetch()}
      refreshing={feed.isRefetching}
      headerRight={
        <View className="flex-row items-center gap-2">
          <HeaderButton
            icon="search-outline"
            label={t('common.search')}
            active={searchOpen}
            onPress={() => {
              // Закрывая строку, снимаем и сам запрос: иначе список остался бы
              // отфильтрованным, а поля, которое это объясняет, на экране нет.
              setSearchOpen((v) => !v);
              if (searchOpen) setQuery('');
            }}
          />
          <HeaderButton
            icon="options-outline"
            label={t('catalog.filters')}
            active={genre !== null}
            onPress={() => setGenreOpen(true)}
          />
        </View>
      }
    >
      {searchOpen ? (
        <View className="flex-row items-center gap-2 rounded-card bg-surface px-3">
          <Ionicons name="search-outline" size={18} color={colors.textMuted} />
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder={t('premiere.searchPlaceholder')}
            placeholderTextColor={colors.textDisabled}
            autoFocus
            returnKeyType="search"
            className="flex-1 py-3 text-body"
            style={{ color: colors.white }}
          />
          {query.length > 0 ? (
            <Pressable onPress={() => setQuery('')} accessibilityRole="button" hitSlop={10}>
              <Ionicons name="close-circle" size={18} color={colors.textMuted} />
            </Pressable>
          ) : null}
        </View>
      ) : null}

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
            onPress={() => selectTab(i)}
            accessibilityRole="button"
            accessibilityState={{ selected: i === active }}
            // Неактивная вкладка на макете — контур, а не заливка: залитая
            // тёмным, она сливалась бы с фоном экрана.
            className={`flex-row items-center gap-1.5 rounded-pill border px-4 py-2 ${
              i === active ? 'border-purple bg-purple' : 'border-border bg-surface'
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

      {/* «Barcha seriallar · 128 ta» — как на макете: счётчик стоит рядом с
          названием раздела и подсвечен, а не уехал серым к правому краю. */}
      {visible.length > 0 ? (
        <View className="flex-row items-baseline gap-2">
          <Text className="text-body font-semibold text-text">
            {t('premiere.allOf', {
              name: t(`premiere.${TABS[active].label}`).toLowerCase(),
            })}
          </Text>
          <Text className="text-caption font-semibold text-violet">
            {t('premiere.count', { count: visible.length })}
          </Text>
        </View>
      ) : null}

      <ContentGrid
        feed={feed}
        cards={visible}
        hasAny={all.length > 0}
        // Пустой раздел, пустой фильтр и пустой каталог — разные вещи.
        // Подсказка должна говорить, что именно снять.
        emptyBody={narrowed ? t('premiere.emptyByFilters') : t('premiere.emptyByTab')}
      />

      <GenreSheet
        visible={genreOpen}
        genres={genres}
        selected={genre}
        onSelect={(value) => {
          setGenre(value);
          setGenreOpen(false);
        }}
        onClose={() => setGenreOpen(false)}
      />
    </Screen>
  );
}

/**
 * Знак в шапке: лупа и ползунки (макет 01.09.2026) — контурный квадрат
 * со скруглением, а не круглая заливка.
 *
 * Включённое состояние покрашено фирменным фиолетовым: иначе человек,
 * выбравший жанр и ушедший вниз по списку, не понимал бы, почему карточек
 * стало меньше.
 */
function HeaderButton({
  icon,
  label,
  active,
  onPress,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  active: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ selected: active }}
      style={{ width: TOUCH_TARGET, height: TOUCH_TARGET }}
      className={`items-center justify-center rounded-card border active:opacity-70 ${
        active ? 'border-purple bg-purple/20' : 'border-border bg-surface'
      }`}
    >
      <Ionicons name={icon} size={20} color={active ? colors.violet : colors.white} />
    </Pressable>
  );
}
