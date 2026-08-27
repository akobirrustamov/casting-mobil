import { View } from 'react-native';
import Svg, { Defs, LinearGradient, Stop, Text as SvgText } from 'react-native-svg';

import { colors } from '@/theme/tokens';

import { Logo } from './Logo';

/**
 * Знак + название в одну строку — «◆ UZCASTING» с макета.
 *
 * Заказчик разрешил ставить логотип вместо названия или рядом с ним;
 * на макете он рядом, поэтому связка едет вместе и не расходится по экранам.
 *
 * <h2>Почему название нарисовано в SVG</h2>
 * На референсе заказчика фирменный приём — заголовок с переходом из синего
 * в фиолетовый. В React Native текст градиентом не заливается: нужен либо
 * маска-компонент (в проекте его нет), либо SVG. SVG уже стоит — на нём
 * сам знак.
 *
 * Так красится ТОЛЬКО «UZCASTING»: строка постоянная, её не переводят и не
 * меняют по длине, поэтому ширину бокса можно задать числом. С переводимым
 * текстом этот приём давал бы обрезанные концы на длинных языках.
 */
const SIZES = {
  md: { mark: 22, font: 22, tracking: 1.8, width: 176, height: 28 },
  lg: { mark: 30, font: 30, tracking: 2.5, width: 240, height: 38 },
} as const;

export function Wordmark({
  size = 'md',
  /** Одноцветное название — там, где градиент спорит с фоном. */
  plain = false,
}: {
  size?: 'md' | 'lg';
  plain?: boolean;
}) {
  const s = SIZES[size];

  return (
    <View className="flex-row items-center gap-2">
      <Logo size={s.mark} />

      <View style={{ width: s.width, height: s.height }}>
        <Svg width={s.width} height={s.height}>
          <Defs>
            <LinearGradient id="wordmark" x1="0" y1="0" x2="1" y2="0">
              <Stop offset="0" stopColor={colors.white} />
              <Stop offset="0.55" stopColor={colors.white} />
              <Stop offset="1" stopColor={colors.violet} />
            </LinearGradient>
          </Defs>
          <SvgText
            x={0}
            // Базовая линия: буквы без нижних выносных, поэтому чуть выше низа.
            y={s.font * 0.86}
            fill={plain ? colors.white : 'url(#wordmark)'}
            fontSize={s.font}
            fontWeight="700"
            letterSpacing={s.tracking}
          >
            UZCASTING
          </SvgText>
        </Svg>
      </View>
    </View>
  );
}
