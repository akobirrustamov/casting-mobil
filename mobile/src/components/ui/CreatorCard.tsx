import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * Карточка креатора для сетки каталога.
 *
 * Портретная пропорция 3:4 — как на постерах Yangi.TV и на сайте кастинга.
 * Имя и мета лежат поверх фото на градиентной подложке: так карточка
 * остаётся одной высоты независимо от длины имени, и сетка не «рвётся».
 */
export function CreatorCard({
  name,
  meta,
  imageUrl,
  width,
  onPress,
}: {
  name: string;
  meta?: string;
  imageUrl?: string;
  width: number;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      style={{ width }}
      className="overflow-hidden rounded-card bg-surface-2 active:opacity-80"
    >
      <View style={{ width: '100%', aspectRatio: 0.75 }} className="bg-surface-2">
        {imageUrl ? (
          <Image
            source={{ uri: imageUrl }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            transition={200}
          />
        ) : (
          <View className="flex-1 items-center justify-center">
            <Ionicons name="person-outline" size={36} color={colors.textDisabled} />
          </View>
        )}

        {/* Затемнение снизу, чтобы белый текст читался на любом фото */}
        <View
          pointerEvents="none"
          className="absolute bottom-0 left-0 right-0"
          style={{ height: '45%', backgroundColor: 'rgba(7,7,13,0.72)' }}
        />

        <View className="absolute bottom-0 left-0 right-0 p-2">
          <Text numberOfLines={1} className="text-caption font-semibold text-text">
            {name}
          </Text>
          {meta ? (
            <Text numberOfLines={1} className="text-micro text-text-muted">
              {meta}
            </Text>
          ) : null}
        </View>
      </View>
    </Pressable>
  );
}
