import { MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { radius } from '@/theme/tokens';

/**
 * Плитка направления — 10 направлений кастинга и разделы каталога контента.
 *
 * <h2>Почему глиф, а не картинка из админки</h2>
 * В `iconMediaId` лежит не иконка, а изображение размером с постер: в
 * dev-базе это 1280×720 с названием категории и подписью «DEV MA'LUMOTI»
 * прямо на кадре (`DevMediaFactory` — формата иконки там нет вовсе).
 * В плитке 116×76 из такого файла получается либо вторая конкурирующая
 * подпись, либо нечитаемое пятно.
 *
 * Глиф из MaterialCommunityIcons (7448 знаков) решает это насовсем:
 * он векторный, одноцветный и подбирается по смыслу направления.
 * Когда заказчик загрузит настоящие иконки, картинку можно вернуть —
 * но уже зная, что именно лежит в поле.
 *
 * <h2>⚠️ Фон тянется через `absoluteFill`</h2>
 * Было `{position:'absolute', width:'100%', height:'100%'}` — и фон не
 * доставал до краёв. Проценты у абсолютного элемента считаются от
 * КОНТЕНТНОЙ области, то есть от плитки минус `p-3`, а без `top/left`
 * он вдобавок встаёт внутрь отступа. Вокруг оставалась тёмная рамка.
 */
export type CategoryGlyph = keyof typeof MaterialCommunityIcons.glyphMap;

export function CategoryTile({
  title,
  accent,
  icon,
  width = 116,
  onPress,
}: {
  title: string;
  accent: string;
  /** Знак направления. Без него плитка просто цветная. */
  icon?: CategoryGlyph;
  width?: number;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={{
        width,
        height: 76,
        borderRadius: radius.card,
        overflow: 'hidden',
        borderWidth: 1,
        // Тонкий кант тем же цветом: на почти чёрном фоне без него плитка
        // сливается с экраном.
        borderColor: `${accent}59`,
      }}
      className="justify-end bg-surface p-3 active:opacity-70"
    >
      {/* Свечение из нижнего угла — тот же приём, что и на фоне экрана. */}
      <LinearGradient
        colors={[`${accent}80`, `${accent}0D`]}
        start={{ x: 0, y: 1 }}
        end={{ x: 1, y: 0 }}
        style={StyleSheet.absoluteFill}
      />

      {icon ? (
        // Знак крупный и приглушённый: он фон, а не вторая подпись.
        // Уходит за правый край — так плитка выглядит кадром, а не наклейкой.
        <View
          pointerEvents="none"
          style={{ position: 'absolute', right: -10, top: -6, opacity: 0.28 }}
        >
          <MaterialCommunityIcons name={icon} size={62} color="#FFFFFF" />
        </View>
      ) : null}

      <Text numberOfLines={2} className="text-body font-semibold text-text">
        {title}
      </Text>
    </Pressable>
  );
}
