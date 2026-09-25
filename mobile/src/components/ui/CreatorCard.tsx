import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { useIsFocused } from 'expo-router';
import { memo, useEffect, useMemo, useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

/** Сколько держится одно фото. */
const SLIDE_MS = 3500;

/**
 * Сдвиг старта для карточки — чтобы вся сетка не меняла фото в один
 * момент (это выглядело бы как мигание экрана).
 */
function startOffset(seed: string): number {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) hash = (hash * 31 + seed.charCodeAt(i)) | 0;
  return Math.abs(hash) % SLIDE_MS;
}

/**
 * Карточка креатора для сетки каталога.
 *
 * Портретная пропорция 3:4 — как на постерах Yangi.TV и на сайте кастинга.
 * Имя и мета лежат поверх фото: так карточка остаётся одной высоты
 * независимо от длины имени, и сетка не «рвётся».
 *
 * <h2>Карусель (25.09.2026)</h2>
 * Заказчик: у анкеты несколько фото — пусть они сменяют друг друга, а
 * затемнение снизу было слишком высоким и закрывало лицо. Теперь фото
 * меняются сами (плавной сменой, без свайпа — свайп в сетке спорил бы с
 * прокруткой и нажатием), а снизу лишь мягкий градиент под текстом:
 * фото видно примерно на 85% карточки. Смена стоит, пока экран не в
 * фокусе, и следующее фото подгружается заранее.
 *
 * Сердечко — сосед основной области нажатия, а не потомок. Вложенные
 * Pressable на нативе перехватывают касания друг у друга, а в вебе дают
 * <button> внутри <button> — невалидный HTML и ошибка гидратации.
 */
export const CreatorCard = memo(function CreatorCard({
  name,
  meta,
  imageUrl,
  imageUrls,
  width,
  onPress,
  isFavorite,
  onToggleFavorite,
}: {
  name: string;
  meta?: string;
  /** Одно фото — если списка нет. */
  imageUrl?: string;
  /** Все фото анкеты — крутятся каруселью. */
  imageUrls?: string[];
  width: number;
  onPress?: () => void;
  isFavorite?: boolean;
  onToggleFavorite?: () => void;
}) {
  const photos = useMemo(
    () => (imageUrls && imageUrls.length > 0 ? imageUrls : imageUrl ? [imageUrl] : []),
    [imageUrls, imageUrl]
  );
  const index = useCarousel(photos);
  const current = photos[index];

  return (
    <View style={{ width }} className="overflow-hidden rounded-card bg-surface-2">
      <Pressable
        onPress={onPress}
        accessibilityRole="button"
        accessibilityLabel={name}
        style={{ width: '100%', aspectRatio: 0.75 }}
        className="active:opacity-80"
      >
        {current ? (
          <Image
            source={{ uri: current }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            // Плавная смена кадра карусели.
            transition={{ duration: 450, effect: 'cross-dissolve' }}
            cachePolicy="memory-disk"
          />
        ) : (
          <View className="flex-1 items-center justify-center">
            <Ionicons name="person-outline" size={36} color={colors.textDisabled} />
          </View>
        )}

        {/*
          Мягкий градиент только под текстом (~15% высоты): белые буквы
          читаются на любом фото, а само фото видно почти целиком.
        */}
        <LinearGradient
          pointerEvents="none"
          colors={['rgba(7,7,13,0)', 'rgba(7,7,13,0.55)', 'rgba(7,7,13,0.85)']}
          locations={[0, 0.55, 1]}
          style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: '24%' }}
        />

        <View className="absolute bottom-0 left-0 right-0 px-2 pb-1.5">
          <Text
            numberOfLines={1}
            className="text-caption font-semibold text-text"
            style={{ textShadowColor: 'rgba(0,0,0,0.6)', textShadowRadius: 4 }}
          >
            {name}
          </Text>
          {meta ? (
            <Text
              numberOfLines={1}
              className="text-micro text-text-muted"
              style={{ textShadowColor: 'rgba(0,0,0,0.6)', textShadowRadius: 4 }}
            >
              {meta}
            </Text>
          ) : null}
        </View>

        {photos.length > 1 ? <Dots count={photos.length} active={index} /> : null}
      </Pressable>

      {onToggleFavorite ? (
        <Pressable
          onPress={onToggleFavorite}
          accessibilityRole="button"
          accessibilityState={{ selected: isFavorite }}
          hitSlop={8}
          className="absolute right-1.5 top-1.5 h-9 w-9 items-center justify-center rounded-pill active:opacity-60"
          style={{ backgroundColor: 'rgba(7,7,13,0.5)' }}
        >
          <Ionicons
            name={isFavorite ? 'heart' : 'heart-outline'}
            size={18}
            color={isFavorite ? colors.magenta : colors.white}
          />
        </Pressable>
      ) : null}
    </View>
  );
});

/**
 * Номер текущего фото. Крутится, только пока экран в фокусе: вкладки
 * остаются смонтированными, и таймеры в скрытой вкладке зря гоняли бы
 * загрузку картинок.
 */
function useCarousel(photos: string[]): number {
  const focused = useIsFocused();
  const [index, setIndex] = useState(0);
  const count = photos.length;
  const seed = photos[0] ?? '';

  // Список фото сменился (другая анкета в той же ячейке) — с начала.
  useEffect(() => setIndex(0), [seed, count]);

  useEffect(() => {
    if (!focused || count < 2) return;

    let interval: ReturnType<typeof setInterval> | undefined;
    const next = () => setIndex((i) => (i + 1) % count);
    const start = setTimeout(() => {
      next();
      interval = setInterval(next, SLIDE_MS);
    }, SLIDE_MS + startOffset(seed));

    return () => {
      clearTimeout(start);
      if (interval) clearInterval(interval);
    };
  }, [focused, count, seed]);

  // Следующее фото — заранее в кэш, чтобы смена не мигала пустотой.
  useEffect(() => {
    if (count < 2) return;
    const upcoming = photos[(index + 1) % count];
    if (upcoming) void Image.prefetch(upcoming).catch(() => {});
  }, [photos, index, count]);

  return count > 0 ? index % count : 0;
}

/** Точки над текстом: сколько фото и какое сейчас. */
function Dots({ count, active }: { count: number; active: number }) {
  // Больше 6 точек на узкой карточке сливаются в полосу.
  const shown = Math.min(count, 6);
  const current = Math.min(active, shown - 1);

  return (
    <View
      pointerEvents="none"
      className="absolute left-0 right-0 top-2 flex-row justify-center"
      style={{ gap: 4 }}
    >
      {Array.from({ length: shown }, (_, i) => (
        <View
          key={i}
          style={{
            width: i === current ? 14 : 5,
            height: 5,
            borderRadius: 3,
            backgroundColor: i === current ? colors.white : 'rgba(255,255,255,0.5)',
          }}
        />
      ))}
    </View>
  );
}
