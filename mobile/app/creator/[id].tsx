import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  FlatList,
  Pressable,
  Text,
  View,
  useWindowDimensions,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { CATEGORIES, EXTRA_API_TYPES } from '@/features/catalog/categories';
import { useCreator } from '@/features/creators/api';
import { useFavoritesStore, useIsFavorite } from '@/features/favorites/store';
import { colors } from '@/theme/tokens';

/**
 * Профиль креатора — экран 13 из ТЗ.
 *
 * Показываем только то, что сайт и так публикует: имя, направление,
 * город, возраст, рост и фото с `isWebShow`. Телефон, email, telegram
 * и замеры фигуры API отдаёт, но это персональные данные — в приложение
 * они не идут (docs/API.md).
 */
export default function CreatorScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { t, i18n } = useTranslation();

  const numericId = Number(id);
  const query = useCreator(Number.isFinite(numericId) ? numericId : null);
  const creator = query.data;

  const isFavorite = useIsFavorite(numericId);
  const toggleFavorite = useFavoritesStore((s) => s.toggle);

  if (query.isPending) {
    return (
      <Screen scroll={false} title=" " onBack={() => router.back()} underTabBar={false}>
        <ScreenState kind="loading" />
      </Screen>
    );
  }

  if (query.isError) {
    return (
      <Screen scroll={false} title=" " onBack={() => router.back()} underTabBar={false}>
        <ScreenState kind="error" onRetry={() => query.refetch()} />
      </Screen>
    );
  }

  if (!creator) {
    return (
      <Screen scroll={false} title=" " onBack={() => router.back()} underTabBar={false}>
        <ScreenState kind="empty" body={t('creator.notFound')} />
      </Screen>
    );
  }

  const isRu = i18n.language === 'ru';
  const typeLabel = labelForType(creator.castingType, isRu);

  const facts = [
    creator.age !== null
      ? { key: 'age', label: t('creator.age'), value: String(creator.age) }
      : null,
    creator.height !== null
      ? { key: 'height', label: t('creator.height'), value: `${creator.height} ${t('creator.cm')}` }
      : null,
    creator.region
      ? { key: 'region', label: t('creator.region'), value: creator.region }
      : null,
    {
      key: 'gender',
      label: t('creator.gender'),
      value: creator.gender === 'female' ? t('catalog.female') : t('catalog.male'),
    },
  ].filter((f): f is { key: string; label: string; value: string } => f !== null);

  return (
    <Screen
      title={creator.name}
      subtitle={typeLabel ?? undefined}
      onBack={() => router.back()}
      underTabBar={false}
      headerRight={
        <Pressable
          onPress={() => toggleFavorite(creator.id)}
          accessibilityRole="button"
          accessibilityState={{ selected: isFavorite }}
          hitSlop={10}
          className="h-11 w-11 items-center justify-center active:opacity-60"
        >
          <Ionicons
            name={isFavorite ? 'heart' : 'heart-outline'}
            size={24}
            color={isFavorite ? colors.magenta : colors.white}
          />
        </Pressable>
      }
    >
      <Gallery photos={creator.photoUrls} />

      <View className="gap-2 rounded-card-lg bg-surface p-4">
        {facts.map((f, i) => (
          <View
            key={f.key}
            className={`flex-row items-center justify-between py-2 ${
              i > 0 ? 'border-t border-border' : ''
            }`}
          >
            <Text className="text-body text-text-muted">{f.label}</Text>
            <Text className="text-body text-text">{f.value}</Text>
          </View>
        ))}
      </View>

      {/*
        Кнопки связи здесь намеренно нет: контакты — персональные данные,
        и по ТЗ отклик идёт через заявку на кастинг (экран 15), а не напрямую.
      */}
    </Screen>
  );
}

/** Галерея с пагинацией. Точки считаем по прокрутке — как в онбординге. */
function Gallery({ photos }: { photos: string[] }) {
  const { width } = useWindowDimensions();
  const [index, setIndex] = useState(0);

  // Экран внутри Screen с горизонтальными отступами по 16
  const itemWidth = width - 32;

  if (photos.length === 0) {
    return (
      <View
        style={{ width: itemWidth, aspectRatio: 0.8 }}
        className="items-center justify-center rounded-card-lg bg-surface-2"
      >
        <Ionicons name="image-outline" size={44} color={colors.textDisabled} />
      </View>
    );
  }

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    if (itemWidth <= 0) return;
    const next = Math.round(e.nativeEvent.contentOffset.x / itemWidth);
    if (next !== index && next >= 0 && next < photos.length) setIndex(next);
  };

  return (
    <View className="gap-2">
      <FlatList
        data={photos}
        keyExtractor={(uri) => uri}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onScroll={onScroll}
        scrollEventThrottle={16}
        getItemLayout={(_, i) => ({
          length: itemWidth,
          offset: itemWidth * i,
          index: i,
        })}
        renderItem={({ item }) => (
          <Image
            source={{ uri: item }}
            style={{ width: itemWidth, aspectRatio: 0.8, borderRadius: 22 }}
            contentFit="cover"
            transition={200}
          />
        )}
      />

      {photos.length > 1 ? (
        <View className="flex-row justify-center gap-2">
          {photos.map((uri, i) => (
            <View
              key={uri}
              style={{
                width: 7,
                height: 7,
                borderRadius: 4,
                backgroundColor: i === index ? colors.purple : colors.border,
              }}
            />
          ))}
        </View>
      ) : null}
    </View>
  );
}

/** Подпись направления: сначала 10 направлений ТЗ, затем типы из API вне списка. */
function labelForType(type: string | null, isRu: boolean): string | null {
  if (!type) return null;

  const category = CATEGORIES.find((c) => c.apiType === type);
  if (category) return isRu ? category.titleRu : category.titleUz;

  const extra = EXTRA_API_TYPES[type];
  return extra ? (isRu ? extra.ru : extra.uz) : type;
}
