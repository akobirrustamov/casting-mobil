import { useEffect, useRef } from 'react';
import { Animated, Easing, View, type ViewStyle } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * Заглушка загрузки в форме будущего контента.
 *
 * Крутилка по центру не говорит, что грузится, и экран «прыгает», когда
 * данные приезжают. Скелетон держит раскладку и показывает, чего ждать.
 * ТЗ требует состояние loading на каждом экране — это оно и есть.
 *
 * Пульсация на `Animated` с `useNativeDriver`: анимируется только opacity,
 * поэтому работает в нативном потоке и не дёргается при загрузке данных.
 */
export function Skeleton({
  width,
  height,
  radius = 12,
  style,
}: {
  width?: number | `${number}%`;
  height: number;
  radius?: number;
  style?: ViewStyle;
}) {
  const pulse = useRef(new Animated.Value(0.4)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, {
          toValue: 1,
          duration: 700,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(pulse, {
          toValue: 0.4,
          duration: 700,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [pulse]);

  return (
    <Animated.View
      style={[
        {
          width,
          height,
          borderRadius: radius,
          backgroundColor: colors.surface2,
          opacity: pulse,
        },
        style,
      ]}
    />
  );
}

/** Ряд карточек-заглушек — под горизонтальные рельсы. */
export function SkeletonRail({
  count = 4,
  width = 120,
  height = 170,
}: {
  count?: number;
  width?: number;
  height?: number;
}) {
  return (
    <View className="flex-row gap-3 px-4">
      {Array.from({ length: count }, (_, i) => (
        <View key={i} className="gap-2">
          <Skeleton width={width} height={height} radius={16} />
          <Skeleton width={width * 0.7} height={10} radius={4} />
        </View>
      ))}
    </View>
  );
}

/**
 * Сетка заглушек — под каталог.
 *
 * Ширина, высота и зазор приходят снаружи и совпадают с настоящей сеткой:
 * заглушка другого размера означала бы, что раскладка прыгает ровно в тот
 * момент, когда приезжают данные, — то есть скелетон делал бы то, ради
 * предотвращения чего он и нужен.
 */
export function SkeletonGrid({
  count = 6,
  cardWidth,
  /** Ширина к высоте кадра — та же, что у карточки. */
  ratio = 2 / 3,
  gap = 12,
}: {
  count?: number;
  cardWidth: number;
  ratio?: number;
  gap?: number;
}) {
  return (
    <View className="flex-row flex-wrap px-4" style={{ gap }}>
      {Array.from({ length: count }, (_, i) => (
        <Skeleton
          key={i}
          width={cardWidth}
          height={Math.round(cardWidth / ratio)}
          radius={16}
        />
      ))}
    </View>
  );
}
