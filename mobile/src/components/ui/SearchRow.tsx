import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * Строка результата поиска.
 *
 * У Yangi.TV выдача — именно список строк, а не сетка: миниатюра слева,
 * справа название и мета в несколько строк. Так помещается больше подписей,
 * чем на карточке, и читать выдачу удобнее.
 */
export function SearchRow({
  title,
  subtitle,
  meta,
  imageUrl,
  onPress,
}: {
  title: string;
  subtitle?: string | null;
  meta?: string | null;
  imageUrl?: string;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      className="flex-row items-center gap-3 rounded-card bg-surface p-2 active:opacity-70"
    >
      <View
        style={{ width: 56, height: 72, borderRadius: 10 }}
        className="overflow-hidden bg-surface-2"
      >
        {imageUrl ? (
          <Image
            source={{ uri: imageUrl }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            transition={150}
          />
        ) : (
          <View className="flex-1 items-center justify-center">
            <Ionicons name="person-outline" size={22} color={colors.textDisabled} />
          </View>
        )}
      </View>

      <View className="flex-1 gap-0.5">
        <Text numberOfLines={1} className="text-body font-semibold text-text">
          {title}
        </Text>
        {subtitle ? (
          <Text numberOfLines={1} className="text-caption text-purple">
            {subtitle}
          </Text>
        ) : null}
        {meta ? (
          <Text numberOfLines={1} className="text-caption text-text-muted">
            {meta}
          </Text>
        ) : null}
      </View>

      <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
    </Pressable>
  );
}
