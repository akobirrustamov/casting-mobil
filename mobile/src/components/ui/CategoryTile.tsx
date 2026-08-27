import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, Text } from 'react-native';

import { radius } from '@/theme/tokens';

/**
 * Плитка направления — 10 категорий кастинга и разделы каталога контента.
 *
 * <h2>Почему здесь нет картинки</h2>
 * Сначала иконка из админки растягивалась фоном на всю плитку, потом
 * ужалась до метки в углу — и оба раза выглядело плохо. Причина не в
 * вёрстке: то, что лежит в `iconMediaId`, картинкой размером с плитку
 * не является.
 *
 * В dev-базе это вообще постер 1280×720, на котором нарисованы название
 * категории и подпись «DEV MA'LUMOTI» (`DevMediaFactory` — формата иконки
 * там просто нет). В плитке 116×76 из него получается либо вторая
 * конкурирующая подпись, либо нечитаемое пятно.
 *
 * Поэтому плитка — акцентный градиент и одна подпись. Это не потеря
 * данных: иконка украшение, а не информация. Когда заказчик загрузит
 * настоящие иконки-глифы, их можно вернуть — но уже зная, что именно
 * лежит в поле.
 *
 * Цвет приходит снаружи: у направлений кастинга он закреплён за
 * направлением, у категорий каталога подбирается по позиции — их палитру
 * админка не задаёт.
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
        style={{ position: 'absolute', width: '100%', height: '100%' }}
      />

      <Text numberOfLines={2} className="text-body font-semibold text-text">
        {title}
      </Text>
    </Pressable>
  );
}
