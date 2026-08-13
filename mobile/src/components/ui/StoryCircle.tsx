import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * Круглый аватар с подписью — ряд «Mashhur ijodkorlar» на главной.
 * Паттерн «историй» из Yangi.TV, содержание по ТЗ: популярные креаторы.
 * Золотое кольцо — verified из ТЗ.
 */
export function StoryCircle({
  name,
  role,
  imageUrl,
  verified = false,
  size = 64,
  onPress,
}: {
  name: string;
  role?: string;
  imageUrl?: string;
  verified?: boolean;
  size?: number;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={{ width: size + 12 }}
      className="items-center gap-1 active:opacity-70"
    >
      <View
        style={{
          width: size,
          height: size,
          borderRadius: size / 2,
          borderWidth: verified ? 2 : 1,
          borderColor: verified ? colors.gold : colors.border,
        }}
        className="overflow-hidden bg-surface-2"
      >
        {imageUrl ? (
          <Image
            source={{ uri: imageUrl }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            transition={200}
          />
        ) : null}
      </View>

      <Text numberOfLines={1} className="text-center text-micro text-text">
        {name}
      </Text>
      {role ? (
        <Text numberOfLines={1} className="text-center text-micro text-text-muted">
          {role}
        </Text>
      ) : null}
    </Pressable>
  );
}
