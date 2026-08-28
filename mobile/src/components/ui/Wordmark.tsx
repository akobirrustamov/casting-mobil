import { useState } from 'react';
import { Text, View, type LayoutChangeEvent } from 'react-native';
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
 * На референсе заказчика фирменный приём — переход из белого в фиолетовый.
 * В React Native текст градиентом не заливается: нужен либо маска-компонент
 * (в проекте его нет), либо SVG. SVG уже стоит — на нём сам знак.
 *
 * <h2>Почему размер бокса меряется, а не задан числом</h2>
 * Сначала ширина и высота были вписаны на глаз. Это разъехалось:
 * бокс `lg` был 240×38 при настоящей надписи около 190 в ширину, поэтому
 * между знаком и словом оставался лишний воздух, а буквы стояли в верхней
 * части бокса и не совпадали по центру со знаком.
 *
 * Угадать метрики нельзя в принципе: ширина строки зависит от системного
 * шрифта, а он на iOS и Android разный. Поэтому размер берётся у настоящего
 * `<Text>` с теми же параметрами — он и задаёт раскладку, а SVG рисуется
 * поверх него ровно по измеренному месту.
 */
const SIZES = {
  md: { mark: 22, font: 22, tracking: 1.8 },
  lg: { mark: 30, font: 30, tracking: 2.5 },
} as const;

/**
 * Доля кегля, которую занимает высота прописной буквы.
 *
 * Нужна, чтобы поставить базовую линию: в слове нет ни одной буквы с
 * нижним выносным элементом, поэтому центрировать надо именно капитель,
 * а не всю строку с запасом под «у» и «р».
 */
const CAP_HEIGHT_RATIO = 0.72;

export function Wordmark({
  size = 'md',
  /** Одноцветное название — там, где градиент спорит с фоном. */
  plain = false,
}: {
  size?: 'md' | 'lg';
  plain?: boolean;
}) {
  const s = SIZES[size];
  const [box, setBox] = useState<{ width: number; height: number } | null>(null);

  const onLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (box && box.width === width && box.height === height) return;
    setBox({ width, height });
  };

  const typography = {
    fontSize: s.font,
    fontWeight: '700' as const,
    letterSpacing: s.tracking,
  };

  return (
    <View className="flex-row items-center gap-2">
      <Logo size={s.mark} />

      <View>
        {/*
          Эталон раскладки. Пока размер не измерен — он же и виден: иначе
          на первом кадре название пропадало бы совсем, а это хуже, чем
          мгновение без градиента.
        */}
        <Text
          onLayout={onLayout}
          style={{ ...typography, color: colors.white, opacity: box ? 0 : 1 }}
        >
          UZCASTING
        </Text>

        {box ? (
          <Svg
            width={box.width}
            height={box.height}
            style={{ position: 'absolute', left: 0, top: 0 }}
          >
            <Defs>
              <LinearGradient id="wordmark" x1="0" y1="0" x2="1" y2="0">
                <Stop offset="0" stopColor={colors.white} />
                <Stop offset="0.55" stopColor={colors.white} />
                <Stop offset="1" stopColor={colors.violet} />
              </LinearGradient>
            </Defs>
            <SvgText
              x={0}
              // Капитель по центру измеренной строки — тогда слово стоит на
              // одной оси со знаком, который центрируется через `items-center`.
              y={(box.height + s.font * CAP_HEIGHT_RATIO) / 2}
              fill={plain ? colors.white : 'url(#wordmark)'}
              fontSize={s.font}
              fontWeight="700"
              letterSpacing={s.tracking}
            >
              UZCASTING
            </SvgText>
          </Svg>
        ) : null}
      </View>
    </View>
  );
}
