import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, Text, View } from 'react-native';

import { radius } from '@/theme/tokens';

/**
 * Плитка направления — 10 категорий кастинга и разделы каталога контента.
 *
 * <h2>Почему картинка больше не фон</h2>
 * Раньше иконка растягивалась на всю плитку, а поверх неё шёл заголовок.
 * На плитке оказывалось ДВЕ подписи: наша и та, что нарисована внутри
 * самой картинки. Это видно на любом изображении с текстом — а именно
 * такие и приходят, пока в базе dev-заглушки.
 *
 * Дело не только в заглушках: `iconMediaId` в админке — это ИКОНКА, и
 * растягивать её на 116×72 неправильно в принципе. Теперь она стоит
 * маленькой меткой в углу и вписывается целиком (`contain`), а подпись
 * на плитке ровно одна.
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
      style={{ width, height: 72, borderRadius: radius.card, overflow: 'hidden' }}
      className="justify-end bg-surface p-3 active:opacity-70"
    >
      {/* Акцент растворяется по диагонали — как свечение на фоне экрана,
          а не как плотная заливка поверх картинки. */}
      <LinearGradient
        colors={[`${accent}66`, `${accent}14`]}
        start={{ x: 0, y: 1 }}
        end={{ x: 1, y: 0 }}
        style={{ position: 'absolute', width: '100%', height: '100%' }}
      />

      <View
        style={{ backgroundColor: accent }}
        className="absolute left-0 top-0 h-full w-1"
      />

      {imageUrl ? (
        <Image
          source={{ uri: imageUrl }}
          style={{ position: 'absolute', top: 8, right: 8, width: 22, height: 22 }}
          // `contain`, а не `cover`: иконку нельзя обрезать по краям.
          contentFit="contain"
          transition={200}
        />
      ) : null}

      <Text numberOfLines={2} className="text-caption font-semibold text-text">
        {title}
      </Text>
    </Pressable>
  );
}
