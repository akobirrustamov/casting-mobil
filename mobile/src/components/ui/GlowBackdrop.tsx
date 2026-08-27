import { StyleSheet, View, useWindowDimensions } from 'react-native';
import Svg, { Defs, Ellipse, RadialGradient, Stop } from 'react-native-svg';

import { glow } from '@/theme/tokens';

/**
 * Свечение под контентом — два размытых пятна на чёрном.
 *
 * <h2>Откуда взялось</h2>
 * С референса заказчика (26.08.2026): фиолетовое пятно сверху, синее снизу.
 * Именно они отличают «дорогой чёрный» от просто тёмно-серого экрана —
 * без них новая палитра выглядит ровно как старая.
 *
 * <h2>Почему SVG, а не `expo-linear-gradient`</h2>
 * Нужен РАДИАЛЬНЫЙ градиент: линейный даёт полосу поперёк экрана, а не
 * пятно. `expo-linear-gradient` радиальный не умеет, `react-native-svg`
 * умеет и уже стоит в проекте (на нём логотип).
 *
 * <h2>Почему не картинка</h2>
 * PNG с размытием весит сотни килобайт и на разных плотностях экрана
 * либо мылится, либо не тянется. Здесь два эллипса и никакого веса.
 *
 * ТЗ: «glow-эффекты в меру, без избыточных градиентов» — поэтому
 * непрозрачность в центре 0.28, а к краям пятно уходит в ноль.
 */
export function GlowBackdrop({
  /** Насколько ярко. `hero` — для входа и онбординга, где экран пустой. */
  intensity = 'normal',
}: {
  intensity?: 'normal' | 'hero';
}) {
  const { width, height } = useWindowDimensions();

  const peak = intensity === 'hero' ? 0.42 : 0.28;

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="none">
      <Svg width={width} height={height}>
        <Defs>
          <RadialGradient id="glow-primary" cx="50%" cy="50%" r="50%">
            <Stop offset="0" stopColor={glow.primary} stopOpacity={peak} />
            <Stop offset="1" stopColor={glow.primary} stopOpacity={0} />
          </RadialGradient>
          <RadialGradient id="glow-secondary" cx="50%" cy="50%" r="50%">
            <Stop offset="0" stopColor={glow.secondary} stopOpacity={peak * 0.8} />
            <Stop offset="1" stopColor={glow.secondary} stopOpacity={0} />
          </RadialGradient>
        </Defs>

        {/* Фиолетовое — сверху справа, как на референсе */}
        <Ellipse
          cx={width * 0.82}
          cy={height * 0.16}
          rx={width * 0.75}
          ry={height * 0.28}
          fill="url(#glow-primary)"
        />
        {/* Синее — снизу слева */}
        <Ellipse
          cx={width * 0.12}
          cy={height * 0.72}
          rx={width * 0.8}
          ry={height * 0.26}
          fill="url(#glow-secondary)"
        />
      </Svg>
    </View>
  );
}
