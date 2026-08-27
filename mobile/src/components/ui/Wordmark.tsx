import { useState } from 'react';
import { Platform, Text, View, type LayoutChangeEvent } from 'react-native';
import Svg, { Defs, LinearGradient, Stop, Text as SvgText } from 'react-native-svg';

import { colors } from '@/theme/tokens';

import { Logo } from './Logo';

/**
 * Знак и название UzCasting.
 *
 * Два вида:
 *   `inline`  — знак и слово в строку. Шапка главной, узкие места.
 *   `stacked` — знак сверху, под ним название и слоган. Экран входа,
 *               по референсу заказчика от 27.08.2026.
 *
 * <h2>Почему в `inline` название нарисовано в SVG</h2>
 * Там оно с градиентом, а в React Native текст градиентом не заливается:
 * нужен либо маска-компонент (в проекте его нет), либо SVG. Размер бокса
 * берётся у настоящего `<Text>` — угадывать ширину строки нельзя, метрики
 * шрифта на iOS и Android разные.
 *
 * В `stacked` градиента нет: на референсе «Uz» синий, «Casting» белый —
 * это две заливки, а не переход. Поэтому обычный текст, без измерений.
 */
const SIZES = {
  md: { mark: 22, font: 22, tracking: 1.8 },
  lg: { mark: 30, font: 30, tracking: 2.5 },
} as const;

/**
 * Доля кегля, которую занимает высота прописной буквы.
 *
 * Нужна, чтобы поставить базовую линию: в слове нет ни одной буквы с
 * нижним выносным элементом, поэтому центрировать надо именно капитель.
 */
const CAP_HEIGHT_RATIO = 0.72;

/**
 * Слоган с референса.
 *
 * Не переводится и не лежит в i18n: это часть знака, как и само слово
 * «UzCasting». Перевод превратил бы его в подпись, которая на трёх языках
 * разной длины и ломала бы композицию.
 */
const TAGLINE = 'PLAY.  WATCH.  INSPIRE.';

/**
 * ⚠️ Системные шрифты, а не тот, что на референсе.
 *
 * В проекте нет ни одного своего шрифта (`expo-font` подключён, но ничего
 * не грузит), а по картинке шрифт не опознать. Взяты ближайшие системные:
 * засечковый для названия и моноширинный для слогана — начертания те же,
 * рисунок букв другой.
 *
 * Когда заказчик пришлёт `.ttf`, подключается через `expo-font` и меняется
 * здесь в двух строках.
 */
const BRAND_FONT = Platform.select({
  ios: 'Georgia',
  android: 'serif',
  default: 'serif',
});

const TAGLINE_FONT = Platform.select({
  ios: 'Courier',
  android: 'monospace',
  default: 'monospace',
});

export function Wordmark({
  size = 'md',
  variant = 'inline',
  /** Одноцветное название — там, где градиент спорит с фоном. */
  plain = false,
  /** Размер знака в `stacked`. По референсу он крупный. */
  markSize = 132,
  showTagline = true,
}: {
  size?: 'md' | 'lg';
  variant?: 'inline' | 'stacked';
  plain?: boolean;
  markSize?: number;
  showTagline?: boolean;
}) {
  if (variant === 'stacked') {
    return <StackedWordmark markSize={markSize} showTagline={showTagline} />;
  }
  return <InlineWordmark size={size} plain={plain} />;
}

/** Знак сверху, название и слоган под ним — композиция с референса. */
function StackedWordmark({
  markSize,
  showTagline,
}: {
  markSize: number;
  showTagline: boolean;
}) {
  return (
    <View className="items-center">
      <Logo size={markSize} />

      <Text
        style={{
          fontFamily: BRAND_FONT,
          fontSize: Math.round(markSize * 0.27),
          color: colors.white,
          marginTop: 14,
        }}
      >
        {/* «Uz» синим — единственный цветной кусок надписи на референсе. */}
        <Text style={{ color: colors.blue }}>Uz</Text>
        Casting
      </Text>

      {showTagline ? (
        <Text
          style={{
            fontFamily: TAGLINE_FONT,
            fontSize: Math.round(markSize * 0.093),
            letterSpacing: 2,
            color: colors.textDisabled,
            marginTop: 8,
          }}
        >
          {TAGLINE}
        </Text>
      ) : null}
    </View>
  );
}

/** Знак и название в строку — шапка главной. */
function InlineWordmark({ size, plain }: { size: 'md' | 'lg'; plain: boolean }) {
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
