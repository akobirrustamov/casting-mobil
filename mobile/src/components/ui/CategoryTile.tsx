import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

/**
 * Плитка направления — 10 категорий из ТЗ.
 * Паттерн «Janrlar» из Yangi.TV: картинка + текст поверх.
 *
 * У направлений кастинга картинки нет — там цветная подложка акцентом.
 * У категорий каталога (они приходят из `/api/v1/app/home`) иконку задаёт
 * админ, тогда она и становится фоном плитки.
 */
export function CategoryTile({
  title,
  accent,
  imageUrl,
  width = 116,
  onPress,
}: {
  title: string;
  accent: string;
  imageUrl?: string;
  width?: number;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={{ width, height: 72 }}
      className="justify-end overflow-hidden rounded-card bg-surface-2 p-3 active:opacity-70"
    >
      {imageUrl ? (
        <Image
          source={{ uri: imageUrl }}
          style={{ position: 'absolute', width: '100%', height: '100%' }}
          contentFit="cover"
          transition={200}
        />
      ) : null}
      <View
        style={{ backgroundColor: accent, opacity: imageUrl ? 0.45 : 0.22 }}
        className="absolute inset-0"
      />
      <View
        style={{ backgroundColor: accent }}
        className="absolute left-0 top-0 h-full w-1"
      />
      <Text numberOfLines={2} className="text-caption font-semibold text-text">
        {title}
      </Text>
    </Pressable>
  );
}
