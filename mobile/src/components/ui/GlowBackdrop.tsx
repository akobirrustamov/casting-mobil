import { StyleSheet, View, useWindowDimensions } from 'react-native';
import Svg, { Defs, Ellipse, Path, RadialGradient, Stop } from 'react-native-svg';

import { colors, glow } from '@/theme/tokens';

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
  /**
   * Тонкие дуги в левом верхнем углу — деталь с референса заказчика.
   *
   * Только для пустых экранов (вход, онбординг): поверх ленты контента
   * они превратились бы в шум за карточками.
   */
  decor = false,
}: {
  intensity?: 'normal' | 'hero';
  decor?: boolean;
}) {
  const { width, height } = useWindowDimensions();

  const peak = intensity === 'hero' ? 0.42 : 0.28;

  /**
   * Веер дуг из-за левого верхнего угла.
   *
   * Считается от размеров экрана, а не рисуется по точкам: на узком
   * телефоне жёстко заданные координаты уехали бы за край.
   */
  const arcs = Array.from({ length: 9 }, (_, i) => {
    const shift = i * (height * 0.018);
    return {
      key: i,
      d:
        `M ${-width * 0.1} ${height * 0.05 + shift}` +
        ` Q ${width * 0.22} ${-height * 0.02 + shift * 0.55}` +
        ` ${width * 0.66} ${-height * 0.06 + shift * 0.3}`,
      opacity: 0.3 - i * 0.028,
    };
  });

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

        {decor
          ? arcs.map((a) => (
              <Path
                key={a.key}
                d={a.d}
                stroke={colors.violet}
                strokeWidth={1}
                strokeOpacity={a.opacity}
                fill="none"
              />
            ))
          : null}
      </Svg>
    </View>
  );
}
