import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

import { Badge } from './Badge';

/**
 * Карточка контента: кадр с бейджем в углу и подпись под ним.
 *
 * По умолчанию постер 2:3 — паттерн из Yangi.TV, бейджи по ТЗ
 * (locked / purchased вместо PREMIUM / Bepul).
 *
 * Пропорция вынесена наружу ради вертикального формата: у рилса карточка
 * повторяет форму самого видео (9:16). Обрезать его до 2:3 значило бы
 * показать не тот кадр, который снимали.
 */
export type PosterBadge = 'premiere' | 'locked' | 'purchased' | null;

export function PosterCard({
  title,
  subtitle,
  imageUrl,
  badge = null,
  badgeLabel,
  /** Знак в бейдже — пламя на «премьере». */
  badgeIcon,
  /** Таймкод в углу обложки. Уже отформатирован — карточка не считает. */
  duration,
  /** Третья строка: жанр. */
  meta,
  width = 132,
  /** Ширина к высоте кадра. См. `features/content/orientation.cardRatio`. */
  ratio = 2 / 3,
  onPress,
}: {
  title: string;
  subtitle?: string;
  imageUrl?: string;
  badge?: PosterBadge;
  badgeLabel?: string;
  badgeIcon?: keyof typeof Ionicons.glyphMap;
  duration?: string;
  meta?: string;
  width?: number;
  ratio?: number;
  onPress?: () => void;
}) {
  return (
    <Pressable style={{ width }} onPress={onPress} className="gap-2 active:opacity-70">
      <View
        style={{ width, height: Math.round(width / ratio) }}
        className="overflow-hidden rounded-card bg-surface-2"
      >
        {imageUrl ? (
          <Image
            source={{ uri: imageUrl }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            transition={200}
          />
        ) : null}

        {/* Таймкод в правом верхнем углу — как на макете заказчика.
            Подложка сквозная: под ней виден кадр. */}
        {duration ? (
          <View className="absolute right-2 top-2 rounded-pill bg-ink/70 px-2 py-0.5">
            <Text className="text-micro font-semibold text-text">{duration}</Text>
          </View>
        ) : null}

        {badge && badgeLabel ? (
          // Левый верхний угол — как на макете заказчика. Справа он налезал
          // на лица: у постеров герой обычно смещён вправо.
          <View className="absolute left-2 top-2">
            <Badge tone={badge} icon={badgeIcon} translucent>
              {badgeLabel}
            </Badge>
          </View>
        ) : null}
      </View>

      <View>
        <Text numberOfLines={2} className="text-caption text-text">
          {title}
        </Text>
        {subtitle ? (
          <Text numberOfLines={1} className="text-micro text-text-muted">
            {subtitle}
          </Text>
        ) : null}
        {/* Жанр отдельной строкой и бледнее подписи — на макете это
            третий уровень, а не продолжение второго. */}
        {meta ? (
          <Text numberOfLines={1} className="text-micro text-text-disabled">
            {meta}
          </Text>
        ) : null}
      </View>
    </Pressable>
  );
}
