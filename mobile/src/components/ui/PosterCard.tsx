import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

import { Badge } from './Badge';

/**
 * Вертикальный постер 2:3 с бейджем в углу — основная карточка контента.
 * Паттерн из Yangi.TV, бейджи по ТЗ (locked / purchased вместо PREMIUM / Bepul).
 */
export type PosterBadge = 'premiere' | 'locked' | 'purchased' | null;

export function PosterCard({
  title,
  subtitle,
  imageUrl,
  badge = null,
  badgeLabel,
  width = 132,
  onPress,
}: {
  title: string;
  subtitle?: string;
  imageUrl?: string;
  badge?: PosterBadge;
  badgeLabel?: string;
  width?: number;
  onPress?: () => void;
}) {
  return (
    <Pressable style={{ width }} onPress={onPress} className="gap-2 active:opacity-70">
      <View
        style={{ width, height: width * 1.5 }}
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
