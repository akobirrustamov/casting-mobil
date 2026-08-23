import { router, useLocalSearchParams } from 'expo-router';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  FlatList,
  Pressable,
  RefreshControl,
  Text,
  View,
  useWindowDimensions,
} from 'react-native';

import { useTabBarHeight } from '@/components/navigation/TabBar';
import { ScreenState } from '@/components/states/ScreenState';
import { CreatorCard } from '@/components/ui/CreatorCard';
import { Screen } from '@/components/ui/Screen';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import { CATEGORIES } from '@/features/catalog/categories';
import { useCreators } from '@/features/creators/api';
import {
  EMPTY_FILTERS,
  applyFilters,
  collectRegions,
  countActive,
  type AgeBucket,
  type CreatorFilters,
} from '@/features/creators/filters';
import type { Gender } from '@/features/creators/types';
import { useFavoritesStore } from '@/features/favorites/store';
import { useIsOffline } from '@/lib/network';
import { colors } from '@/theme/tokens';

/**
 * Каталог креаторов — экраны 06–13 из ТЗ.
 *
 * Один экран на все направления: они отличаются только фильтром по типу,
 * а верстка и фильтры одинаковые. Отдельные файлы на каждое из 10 направлений
 * были бы десятью копиями одного и того же.
 *
 * `all` — «Barchasi», без фильтра по направлению. Через него видны и типы,
 * которых нет среди 10 направлений ТЗ (euromodel, massovka), иначе анкеты
 * с такими типами были бы недоступны вовсе.
 */
const GAP = 12;
const PADDING = 16;
const COLUMNS = 2;

export default function CatalogScreen() {
  const { category } = useLocalSearchParams<{ category: string }>();
  const { t, i18n } = useTranslation();
  const { width } = useWindowDimensions();
  const tabBarHeight = useTabBarHeight();

  const creators = useCreators();
  const isOffline = useIsOffline();
  const favoriteIds = useFavoritesStore((s) => s.ids);
  const toggleFavorite = useFavoritesStore((s) => s.toggle);
  const isRu = i18n.language === 'ru';

  const meta = CATEGORIES.find((c) => c.id === category) ?? null;
  const isAll = category === 'all';

  const [filters, setFilters] = useState<CreatorFilters>(EMPTY_FILTERS);
  const [filtersOpen, setFiltersOpen] = useState(false);

  const all = creators.data ?? [];

  // Направление фиксировано маршрутом, поэтому его в панель фильтров не выносим
  const scoped = useMemo(() => {
    if (isAll) return all;

    // Направления без типа в базе (музыканты, танцоры, стайлинг, курсы)
    // не должны показывать всех подряд — у них просто нет данных.
    // Раньше эта ветка сливалась с «Barchasi» и выдавала весь список.
    if (!meta?.apiType) return [];

    return all.filter((c) => c.castingType === meta.apiType);
  }, [all, isAll, meta?.apiType]);

  const regions = useMemo(() => collectRegions(scoped), [scoped]);
  const visible = useMemo(() => applyFilters(scoped, filters), [scoped, filters]);

  const cardWidth = (width - PADDING * 2 - GAP * (COLUMNS - 1)) / COLUMNS;
  const activeCount = countActive(filters);

  const title = isAll
    ? t('catalog.all')
    : meta
      ? isRu
        ? meta.titleRu
        : meta.titleUz
      : t('catalog.title');

  if (creators.isPending) {
    return (
      <Screen title={title} scroll={false} onBack={() => router.back()}>
        <SkeletonGrid cardWidth={cardWidth} />
      </Screen>
    );
  }

  if (creators.isError) {
    return (
      <Screen title={title} scroll={false} onBack={() => router.back()}>
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
      subtitle={t('catalog.found', { count: visible.length })}
      scroll={false}
      onBack={() => router.back()}
      headerRight={
        <FilterToggle
          active={activeCount}
          open={filtersOpen}
          onPress={() => setFiltersOpen((v) => !v)}
        />
      }
    >
      {filtersOpen ? (
        <FilterPanel
          filters={filters}
          regions={regions}
          onChange={setFilters}
          onReset={() => setFilters(EMPTY_FILTERS)}
        />
      ) : null}

      {visible.length === 0 ? (
        <ScreenState
          kind="empty"
          // Пустота по разным причинам читается по-разному: одно дело
          // «в этом направлении ещё нет анкет», другое — «фильтры слишком узкие»
          body={
            activeCount > 0
              ? t('catalog.emptyByFilters')
              : meta && !meta.apiType && !isAll
                ? t('catalog.noDataYet')
                : undefined
          }
        />
      ) : (
        <FlatList
          data={visible}
          keyExtractor={(item) => String(item.id)}
          numColumns={COLUMNS}
          columnWrapperStyle={{ gap: GAP }}
          contentContainerStyle={{
            paddingHorizontal: PADDING,
            paddingBottom: tabBarHeight + 16,
            gap: GAP,
          }}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={creators.isRefetching}
              onRefresh={() => creators.refetch()}
              tintColor={colors.purple}
              colors={[colors.purple]}
              progressBackgroundColor={colors.surface}
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
              isFavorite={favoriteIds.has(item.id)}
              onToggleFavorite={() => toggleFavorite(item.id)}
            />
          )}
        />
      )}
    </Screen>
  );
}

function FilterToggle({
  active,
  open,
  onPress,
}: {
  active: number;
  open: boolean;
  onPress: () => void;
}) {
  const { t } = useTranslation();

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ expanded: open }}
      hitSlop={8}
      className="flex-row items-center gap-1 rounded-pill border px-3 py-2 active:opacity-70"
      style={{ borderColor: active > 0 ? colors.purple : colors.border }}
    >
      <Text
        className="text-caption"
        style={{ color: active > 0 ? colors.purple : colors.textMuted }}
      >
        {t('catalog.filters')}
        {active > 0 ? ` · ${active}` : ''}
      </Text>
    </Pressable>
  );
}

function FilterPanel({
  filters,
  regions,
  onChange,
  onReset,
}: {
  filters: CreatorFilters;
  regions: string[];
  onChange: (next: CreatorFilters) => void;
  onReset: () => void;
}) {
  const { t } = useTranslation();

  const genders: { value: Gender; label: string }[] = [
    { value: 'female', label: t('catalog.female') },
    { value: 'male', label: t('catalog.male') },
  ];
  const ages: AgeBucket[] = ['18-24', '25-34', '35+'];

  return (
    <View className="gap-3 px-4 pb-3">
      <FilterRow label={t('catalog.gender')}>
        {genders.map((g) => (
          <Chip
            key={g.value}
            label={g.label}
            active={filters.gender === g.value}
            onPress={() =>
              onChange({
                ...filters,
                gender: filters.gender === g.value ? null : g.value,
              })
            }
          />
        ))}
      </FilterRow>

      <FilterRow label={t('catalog.age')}>
        {ages.map((a) => (
          <Chip
            key={a}
            label={a}
            active={filters.age === a}
            onPress={() =>
              onChange({ ...filters, age: filters.age === a ? 'any' : a })
            }
          />
        ))}
      </FilterRow>

      {regions.length > 0 ? (
        <FilterRow label={t('catalog.region')} scrollable>
          {regions.map((r) => (
            <Chip
              key={r}
              label={r}
              active={filters.region === r}
              onPress={() =>
                onChange({ ...filters, region: filters.region === r ? null : r })
              }
            />
          ))}
        </FilterRow>
      ) : null}

      {countActive(filters) > 0 ? (
        <Pressable onPress={onReset} hitSlop={8} className="self-start">
          <Text className="text-caption text-cyan">{t('catalog.reset')}</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

function FilterRow({
  label,
  scrollable = false,
  children,
}: {
  label: string;
  scrollable?: boolean;
  children: React.ReactNode;
}) {
  return (
    <View className="gap-2">
      <Text className="text-micro uppercase text-text-muted">{label}</Text>
      {scrollable ? (
        // Городов может быть много — прокручиваем, а не переносим в несколько рядов
        <FlatList
          data={[0]}
          keyExtractor={() => 'row'}
          horizontal
          showsHorizontalScrollIndicator={false}
          renderItem={() => <View className="flex-row gap-2">{children}</View>}
        />
      ) : (
        <View className="flex-row flex-wrap gap-2">{children}</View>
      )}
    </View>
  );
}

function Chip({
  label,
  active,
  onPress,
}: {
  label: string;
  active: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected: active }}
      className={`rounded-pill border px-3 py-2 active:opacity-70 ${
        active ? 'bg-purple' : ''
      }`}
      style={{ borderColor: active ? colors.purple : colors.border }}
    >
      <Text className={`text-caption ${active ? 'text-white' : 'text-text-muted'}`}>
        {label}
      </Text>
    </Pressable>
  );
}
