import { Ionicons } from '@expo/vector-icons';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FlatList, Pressable, RefreshControl, Text, View, useWindowDimensions } from 'react-native';

import { pushOnce } from '@/lib/navigation';
import { NotificationBell } from '@/components/navigation/HeaderActions';
import { useTabBarHeight } from '@/components/navigation/TabBar';
import { ScreenState } from '@/components/states/ScreenState';
import { CreatorCard } from '@/components/ui/CreatorCard';
import { Screen } from '@/components/ui/Screen';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import { useAuthStore } from '@/features/auth/store';
import { useMyApplications } from '@/features/casting/api';
import {
  EMPTY_CATALOG_FILTERS,
  applyCatalogFilters,
  collectRegionOptions,
  countCatalogFilters,
  type CatalogFilters,
} from '@/features/casting/catalogFilters';
import { ApplyFab } from '@/features/casting/ApplyFab';
import { ApplicationStatusBlock } from '@/features/casting/components';
import { FilterSheet } from '@/features/casting/FilterSheet';
import { pickHeadline } from '@/features/casting/status';
import { useCreators } from '@/features/creators/api';
import { CASTING_KIND } from '@/features/notifications/api';
import { useFavoritesStore } from '@/features/favorites/store';
import { useIsOffline } from '@/lib/network';
import { colors } from '@/theme/tokens';

/**
 * Вкладка «Casting»: каталог кандидатов и вход в заявку.
 *
 * <h2>Что здесь было раньше</h2>
 * До 06.09.2026 — три выдуманных «объявления о кастинге» с кнопкой,
 * которая ничего не делала; потом — честный пустой экран (коммит
 * 8fe810f). Объявлений на бэкенде по-прежнему нет, а вот анкеты
 * кандидатов и заявка на участие — есть. Поэтому вкладка теперь про
 * них: тот же каталог, что на сайте (`frontend/src/pages/models/Models.js`),
 * и путь «подать анкету → ждать решения админа».
 *
 * <h2>Каталог</h2>
 * Данные — тот же `useCreators`, что у `app/catalog/[category]`: один
 * запрос и один кэш на оба экрана. Фильтры — как на сайте: направление
 * и пол. Видимость анкеты в каталоге решает админ (`isWebShow`), здесь
 * ничего не прячется и не добавляется.
 *
 * <h2>Доступ</h2>
 * Каталог открыт всем, как и раньше: ни экран направления, ни профиль
 * креатора (`app/creator/[id]`) Premium/«casting access» не проверяют,
 * а на бэкенде `AccessService.canAccessCasting` нигде не вызывается.
 * Новой проверки здесь не добавлено — иначе вкладка вела бы себя иначе,
 * чем каталог с главной.
 *
 * <h2>Заявка</h2>
 * Требует входа: новый эндпоинт привязывает заявку к аккаунту. Гость
 * видит ту же кнопку, но она ведёт на вход.
 *
 * <h2>Колокольчик (25.09.2026)</h2>
 * Свой список: уведомления, которые админ отправил с типом
 * «Casting» (`CASTING_NOTIFICATION`). Общие — на главной.
 */
const GAP = 12;
const PADDING = 16;
const COLUMNS = 2;

export default function CastingScreen() {
  const { t } = useTranslation();
  const { width } = useWindowDimensions();
  const tabBarHeight = useTabBarHeight();
  const isOffline = useIsOffline();

  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  const creators = useCreators();
  const mine = useMyApplications(isAuthorized ? userId : null);

  const favoriteIds = useFavoritesStore((s) => s.ids);
  const toggleFavorite = useFavoritesStore((s) => s.toggle);

  const [filters, setFilters] = useState<CatalogFilters>(EMPTY_CATALOG_FILTERS);
  const [sheetOpen, setSheetOpen] = useState(false);

  const all = creators.data ?? [];
  const regions = useMemo(() => collectRegionOptions(all), [all]);
  const visible = useMemo(() => applyCatalogFilters(all, filters), [all, filters]);
  const activeCount = countCatalogFilters(filters);

  // Лист показывает, сколько анкет останется, ещё до «Применить».
  const countFor = (next: CatalogFilters) => applyCatalogFilters(all, next).length;

  const headline = pickHeadline(mine.data ?? []);
  const hasPending = headline?.status === 'PENDING';
  const cardWidth = (width - PADDING * 2 - GAP * (COLUMNS - 1)) / COLUMNS;

  const onApply = () => {
    pushOnce(isAuthorized ? '/casting/apply' : '/(auth)/sign-in');
  };

  const header = (
    <View className="gap-4 pb-4">
      <View className="gap-3 px-4">
        {/*
          Открытая заявка — вместо кнопки: вторую сервер всё равно не
          примет (409), и кнопка, которая заведомо закончится отказом,
          хуже её отсутствия. После решения админа кнопка возвращается.
        */}
        {headline ? (
          <ApplicationStatusBlock app={headline} compact onPress={() => pushOnce('/casting/my')} />
        ) : null}

      </View>

      <View className="flex-row items-center gap-2 px-4">
        <Pressable
          onPress={() => setSheetOpen(true)}
          accessibilityRole="button"
          className="flex-row items-center gap-2 rounded-pill border px-3 py-2 active:opacity-70"
          style={{ borderColor: activeCount > 0 ? colors.purple : colors.border }}
        >
          <Ionicons name="options-outline" size={16} color={activeCount > 0 ? colors.purple : colors.textMuted} />
          <Text className={`text-caption ${activeCount > 0 ? 'text-text' : 'text-text-muted'}`}>
            {t('casting.filters.title')}
            {activeCount > 0 ? ` · ${activeCount}` : ''}
          </Text>
        </Pressable>

        {activeCount > 0 ? (
          <Pressable
            onPress={() => setFilters(EMPTY_CATALOG_FILTERS)}
            accessibilityRole="button"
            hitSlop={8}
            className="active:opacity-60"
          >
            <Text className="text-caption text-violet">{t('catalog.reset')}</Text>
          </Pressable>
        ) : null}
      </View>
    </View>
  );

  const renderEmpty = () => {
    if (creators.isPending) {
      // Пропорция та же, что у `CreatorCard` (3:4), — сетка не прыгает
      // в момент, когда приезжают данные.
      return <SkeletonGrid cardWidth={cardWidth} ratio={0.75} gap={GAP} />;
    }
    if (creators.isError) {
      return (
        <View style={{ minHeight: 320 }}>
          <ScreenState kind={isOffline ? 'offline' : 'error'} onRetry={() => creators.refetch()} />
        </View>
      );
    }
    return (
      <View style={{ minHeight: 240 }}>
        <ScreenState
          kind="empty"
          body={activeCount > 0 ? t('casting.emptyByFilters') : t('casting.empty')}
        />
      </View>
    );
  };

  return (
    <Screen
      scroll={false}
      title={t('casting.title')}
      subtitle={creators.data ? t('casting.found', { count: visible.length }) : t('casting.subtitle')}
      headerRight={<NotificationBell kind={CASTING_KIND} />}
    >
      <FlatList
        data={creators.isPending || creators.isError ? [] : visible}
        keyExtractor={(item) => String(item.id)}
        numColumns={COLUMNS}
        ListHeaderComponent={header}
        ListEmptyComponent={renderEmpty}
        columnWrapperStyle={{ gap: GAP, paddingHorizontal: PADDING }}
        contentContainerStyle={{ paddingBottom: tabBarHeight + 16, gap: GAP }}
        showsVerticalScrollIndicator={false}
        // Сетка может быть длинной — рисуем порциями, а не всё сразу.
        initialNumToRender={8}
        windowSize={7}
        removeClippedSubviews
        refreshControl={
          <RefreshControl
            refreshing={creators.isRefetching || mine.isRefetching}
            onRefresh={() => {
              void creators.refetch();
              if (isAuthorized) void mine.refetch();
            }}
            tintColor={colors.purple}
            colors={[colors.purple]}
            progressBackgroundColor={colors.surface}
          />
        }
        renderItem={({ item }) => (
          <CreatorCard
            name={item.name}
            meta={[
              item.castingType ? t(`casting.types.${item.castingType}`) : null,
              item.age ? t('common.years', { count: item.age }) : null,
            ]
              .filter(Boolean)
              .join(' • ')}
            imageUrls={item.photoUrls}
            width={cardWidth}
            onPress={() => pushOnce(`/creator/${item.id}`)}
            isFavorite={favoriteIds.has(item.id)}
            onToggleFavorite={() => toggleFavorite(item.id)}
          />
        )}
      />

      {/*
        «Ariza qoldirish» — круглой кнопкой в правом нижнем углу
        (макет заказчика от 21.09.2026, как на сайте). Она всегда на
        виду: каталог длинный, и карточка с призывом уезжала вверх
        после первого же пролистывания.

        Открытая заявка кнопку ПРЯЧЕТ: вторую сервер не примет (409),
        и кнопка, заведомо ведущая к отказу, хуже её отсутствия.
      */}
      {!hasPending ? (
        <ApplyFab
          label={t('casting.applyCta')}
          onPress={onApply}
          right={PADDING}
          bottom={tabBarHeight + 16}
        />
      ) : null}

      <FilterSheet
        visible={sheetOpen}
        value={filters}
        regions={regions}
        countFor={countFor}
        onApply={(next) => {
          setFilters(next);
          setSheetOpen(false);
        }}
        onClose={() => setSheetOpen(false)}
      />
    </Screen>
  );
}
