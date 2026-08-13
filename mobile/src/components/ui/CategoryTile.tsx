import { Pressable, Text, View } from 'react-native';

/**
 * Плитка направления — 10 категорий из ТЗ.
 * Паттерн «Janrlar» из Yangi.TV: картинка + текст поверх.
 * Пока картинок нет, поэтому цветная подложка акцентом категории.
 */
export function CategoryTile({
  title,
  accent,
  width = 116,
  onPress,
}: {
  title: string;
  accent: string;
  width?: number;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={{ width, height: 72 }}
      className="justify-end overflow-hidden rounded-card bg-surface-2 p-3 active:opacity-70"
    >
      <View
        style={{ backgroundColor: accent, opacity: 0.22 }}
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
