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

        {badge && badgeLabel ? (
          <View className="absolute right-2 top-2">
            <Badge tone={badge}>{badgeLabel}</Badge>
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
      </View>
    </Pressable>
  );
}
