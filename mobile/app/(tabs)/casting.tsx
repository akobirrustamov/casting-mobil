import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FlatList, RefreshControl, Text, View, useWindowDimensions } from 'react-native';

import { useTabBarHeight } from '@/components/navigation/TabBar';
import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { CreatorCard } from '@/components/ui/CreatorCard';
import { Screen } from '@/components/ui/Screen';
import { SkeletonGrid } from '@/components/ui/Skeleton';
import { useAuthStore } from '@/features/auth/store';
import { useMyApplications } from '@/features/casting/api';
import { ApplicationStatusBlock, Chip, ChipRow } from '@/features/casting/components';
import { CASTING_TYPES, GENDERS } from '@/features/casting/options';
import { pickHeadline } from '@/features/casting/status';
import { useCreators } from '@/features/creators/api';
import { EMPTY_FILTERS, applyFilters } from '@/features/creators/filters';
import type { CastingType, Gender } from '@/features/creators/types';
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

  const [type, setType] = useState<CastingType | null>(null);
  const [gender, setGender] = useState<Gender | null>(null);

  const visible = useMemo(
    () =>
      applyFilters(creators.data ?? [], {
        ...EMPTY_FILTERS,
        apiTypes: type ? [type] : [],
        gender,
      }),
    [creators.data, type, gender],
  );

  const headline = pickHeadline(mine.data ?? []);
  const hasPending = headline?.status === 'PENDING';
  const cardWidth = (width - PADDING * 2 - GAP * (COLUMNS - 1)) / COLUMNS;

  const onApply = () => {
    router.push(isAuthorized ? '/casting/apply' : '/(auth)/sign-in');
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
          <ApplicationStatusBlock app={headline} compact onPress={() => router.push('/casting/my')} />
        ) : null}

        {!hasPending ? (
          <View className="gap-3 rounded-card-lg border border-border bg-surface p-4">
            <View className="flex-row items-center gap-3">
              <View
                className="items-center justify-center rounded-card"
                style={{ width: 44, height: 44, backgroundColor: `${colors.purple}26` }}
              >
                <Ionicons name="sparkles" size={20} color={colors.magenta} />
              </View>
              <View className="flex-1">
                <Text className="text-body font-semibold text-text">{t('casting.applyTitle')}</Text>
                <Text className="text-caption text-text-muted">
                  {isAuthorized ? t('casting.applyBody') : t('casting.signInToApply')}
                </Text>
              </View>
            </View>
            <Button variant="primary" onPress={onApply}>
              {t('casting.applyCta')}
            </Button>
          </View>
        ) : null}
      </View>

      <ChipRow>
        <Chip label={t('casting.all')} active={type === null} onPress={() => setType(null)} />
        {CASTING_TYPES.map((value) => (
          <Chip
            key={value}
            label={t(`casting.types.${value}`)}
            active={type === value}
            onPress={() => setType(type === value ? null : value)}
          />
        ))}
      </ChipRow>

      <ChipRow>
        <Chip label={t('casting.all')} active={gender === null} onPress={() => setGender(null)} />
        {GENDERS.map((value) => (
          <Chip
            key={value}
            label={value === 'female' ? t('catalog.female') : t('catalog.male')}
            active={gender === value}
            onPress={() => setGender(gender === value ? null : value)}
          />
        ))}
      </ChipRow>
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
          body={type || gender ? t('casting.emptyByFilters') : t('casting.empty')}
        />
      </View>
    );
  };

  return (
    <Screen
      scroll={false}
      title={t('casting.title')}
      subtitle={creators.data ? t('casting.found', { count: visible.length }) : t('casting.subtitle')}
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
            imageUrl={item.photoUrls[0]}
            width={cardWidth}
            onPress={() => router.push(`/creator/${item.id}`)}
            isFavorite={favoriteIds.has(item.id)}
            onToggleFavorite={() => toggleFavorite(item.id)}
          />
        )}
      />
    </Screen>
  );
}
