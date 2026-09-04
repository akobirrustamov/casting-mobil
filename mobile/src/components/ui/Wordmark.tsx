import MaskedView from '@react-native-masked-view/masked-view';
import { LinearGradient as ExpoLinearGradient } from 'expo-linear-gradient';
import { useEffect, useRef, useState, type ReactElement } from 'react';
import {
  Animated,
  Easing,
  Platform,
  StyleSheet,
  Text,
  View,
  type LayoutChangeEvent,
} from 'react-native';
import Svg, {
  Defs,
  Ellipse,
  LinearGradient,
  RadialGradient,
  Stop,
  Text as SvgText,
  TSpan,
} from 'react-native-svg';

import { colors, gradients, radius } from '@/theme/tokens';

import { Logo } from './Logo';

/**
 * Знак и название UzCasting.
 *
 * Три вида:
 *   `compact` — знак и «UzCasting» в строку, слово плотное и белое.
 *               Шапка главной, по макету заказчика.
 *   `inline`  — то же, но название набрано капителью с градиентом.
 *   `stacked` — знак сверху, под ним название, слоган и черта.
 *               Экран входа, по референсу заказчика.
 *
 * <h2>Почему название нарисовано в SVG</h2>
 * В обоих видах оно с градиентом, а в React Native текст градиентом не
 * заливается: нужен либо маска-компонент (в проекте его нет), либо SVG.
 * Размер бокса берётся у настоящего `<Text>` — угадывать ширину строки
 * нельзя, метрики шрифта на iOS и Android разные.
 */
const SIZES = {
  md: { mark: 22, font: 22, tracking: 1.8 },
  lg: { mark: 30, font: 30, tracking: 2.5 },
} as const;

/**
 * Размеры вида `compact` — шапка главной по макету заказчика (01.09.2026).
 *
 * Знак крупный, название мельче него примерно вдвое: на макете вес несёт
 * плитка со знаком, а слово стоит рядом подписью, а не вторым логотипом.
 */
const COMPACT = { mark: 36, font: 22 } as const;

/** Сколько блик идёт от края до края. */
const SHINE_SWEEP_MS = 1_500;

/**
 * Пауза между бликами.
 *
 * Блик — украшение, а не индикатор: если он идёт непрерывно, шапка
 * мерцает под каждым экраном и тянет взгляд на себя. Раз в несколько
 * секунд его замечают, но он не мешает читать.
 */
const SHINE_DELAY_MS = 5_000;

/**
 * Ширина световой полосы: доля ширины локапа и нижняя граница в точках.
 *
 * Узкая полоса на коротком слове успевает мигнуть между букв и читается
 * как артефакт. Примерно треть локапа — блик накрывает сразу несколько
 * букв, и глаз видит именно движение света.
 */
const SHINE_BAND_RATIO = 0.28;
const SHINE_BAND_MIN = 40;

/** Наклон полосы. Вертикальная читается как шов, а не как отблеск. */
const SHINE_TILT_DEG = 18;

/**
 * Цвет полосы.
 *
 * Белое ядро — ради знака: он фиолетовый, и по нему нужен именно белый
 * отблеск. Фиолетовое и маджентовое крылья — ради слова: оно белое, и
 * белым по белому не видно ничего, а фирменная волна по белым буквам
 * читается сразу.
 */
const SHINE_COLORS: readonly [string, string, string, string, string] = [
  'rgba(168,85,247,0)',
  'rgba(168,85,247,0.55)',
  'rgba(255,255,255,0.92)',
  'rgba(236,72,153,0.55)',
  'rgba(236,72,153,0)',
];
const SHINE_LOCATIONS: readonly [number, number, number, number, number] = [
  0, 0.3, 0.5, 0.7, 1,
];

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
const TAGLINE = 'PLAY.   WATCH.   INSPIRE.';

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
  markSize = 150,
  showTagline = true,
  /** Пробегающий блик по знаку. Только `inline`. */
  shine = false,
}: {
  size?: 'md' | 'lg';
  variant?: 'compact' | 'inline' | 'stacked';
  plain?: boolean;
  markSize?: number;
  showTagline?: boolean;
  shine?: boolean;
}) {
  if (variant === 'stacked') {
    return <StackedWordmark markSize={markSize} showTagline={showTagline} />;
  }
  if (variant === 'compact') {
    return <CompactWordmark shine={shine} />;
  }
  return <InlineWordmark size={size} plain={plain} shine={shine} />;
}

/**
 * Знак и «UzCasting» в строку — шапка главной.
 *
 * <h2>Почему обычный `<Text>`, а не SVG с градиентом</h2>
 * На макете слово плотное белое: цвет несёт плитка со знаком, а градиент
 * ещё и на подписи спорил бы с ней. Заодно отпадает измерение строки —
 * SVG нужен был только ради заливки.
 */
function CompactWordmark({ shine }: { shine: boolean }) {
  const [frame, setFrame] = useState<{ width: number; height: number } | null>(null);

  const onFrame = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (frame && frame.width === width && frame.height === height) return;
    setFrame({ width, height });
  };

  const word = {
    fontSize: COMPACT.font,
    fontWeight: '700' as const,
    color: colors.white,
  };

  // Локап целиком. Тот же самый узел уходит в маску блика: две копии
  // обязаны совпасть пиксель в пиксель, иначе свет поедет относительно
  // букв. Поэтому — одна константа, а не два одинаковых куска разметки.
  const lockup = (
    <>
      <Logo size={COMPACT.mark} />
      <Text style={word}>UzCasting</Text>
    </>
  );

  return (
    <View
      // Высота фиксирована и равна `TOUCH_TARGET` (44): справа в шапке
      // стоят «Premium» и колокольчик, а `Screen` выравнивает шапку по
      // верху — на одной линии их держит именно одинаковая высота. Та же
      // цифра делает соседние кнопки нажимаемыми без отдельного `hitSlop`.
      className="h-11 flex-row items-center gap-2"
      onLayout={shine ? onFrame : undefined}
    >
      {lockup}

      {shine && frame ? (
        <ShineSweep
          width={frame.width}
          height={frame.height}
          mask={
            <View
              className="h-11 flex-row items-center gap-2"
              style={{ width: frame.width, height: frame.height }}
            >
              {lockup}
            </View>
          }
        />
      ) : null}
    </View>
  );
}

/**
 * Блик: световая полоса наискось, слева направо — по самим буквам и знаку.
 *
 * <h2>Почему через маску, а не просто полосой сверху</h2>
 * Раньше полоса ехала поверх строки прямоугольником. По знаку её было
 * видно, а по слову — нет: слово белое, и белая полоса на белом не
 * читается вообще. Теперь полоса обрезана по форме локапа (`MaskedView`,
 * маска — та же разметка знака и слова), а её цвет — фирменная шкала:
 * по белому слову проходит фиолетово-маджентовая волна, по знаку — белый
 * отблеск. Видно и там, и там, и свет идёт ровно по контуру знака и по
 * штрихам букв, а не по прямоугольнику вокруг них.
 *
 * <h2>Почему translateX, а не анимация градиента</h2>
 * `Stop` в SVG нельзя двигать через нативный драйвер — анимация шла бы по
 * JS-потоку и дёргалась бы каждый раз, когда экран что-то грузит. Полоса
 * же уезжает целиком в нативном потоке.
 *
 * Размеры приходят снаружи: блик обязан начинаться ЗА левым краем и
 * заканчиваться ЗА правым, иначе видно, как он появляется из ниоткуда.
 */
function ShineSweep({
  width,
  height,
  mask,
}: {
  width: number;
  height: number;
  mask: ReactElement;
}) {
  const progress = useRef(new Animated.Value(0)).current;
  const band = Math.max(SHINE_BAND_MIN, Math.round(width * SHINE_BAND_RATIO));

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        // Пауза первая: на первом кадре экрана блик не отвлекает от того,
        // ради чего человек его открыл.
        Animated.delay(SHINE_DELAY_MS),
        Animated.timing(progress, {
          toValue: 1,
          duration: SHINE_SWEEP_MS,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [progress]);

  const translateX = progress.interpolate({
    inputRange: [0, 1],
    outputRange: [-band, width + band],
  });

  return (
    <MaskedView
      pointerEvents="none"
      // Маска ровно по измеренному локапу — тогда её копия ложится на
      // оригинал без сдвига, и полосе некуда выехать за строку.
      style={{ position: 'absolute', left: 0, top: 0, width, height }}
      maskElement={mask}
    >
      <Animated.View
        style={{
          position: 'absolute',
          left: 0,
          // Полоса наклонена, поэтому она выше строки: без запаса сверху и
          // снизу её углы обрезались бы прямо посреди знака.
          top: -height,
          width: band,
          height: height * 3,
          transform: [{ translateX }, { rotate: `${SHINE_TILT_DEG}deg` }],
        }}
      >
        <ExpoLinearGradient
          colors={SHINE_COLORS}
          locations={SHINE_LOCATIONS}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 0 }}
          style={StyleSheet.absoluteFill}
        />
      </Animated.View>
    </MaskedView>
  );
}

/** Знак сверху, под ним название, слоган и короткая черта — как на референсе. */
function StackedWordmark({
  markSize,
  showTagline,
}: {
  markSize: number;
  showTagline: boolean;
}) {
  const font = Math.round(markSize * 0.31);

  return (
    <View className="items-center">
      {/* Знак стоит в собственном ореоле: на референсе он светится. */}
      <View className="items-center justify-center">
        <MarkHalo size={markSize} />
        <Logo size={markSize} />
      </View>

      <View style={{ marginTop: 10 }}>
        <GradientWord text="UzCasting" font={font} />
      </View>

      {showTagline ? (
        <Text
          style={{
            fontFamily: TAGLINE_FONT,
            fontSize: Math.round(markSize * 0.088),
            letterSpacing: 2.5,
            color: colors.textMuted,
            marginTop: 10,
          }}
        >
          {TAGLINE}
        </Text>
      ) : null}

      {/* Короткая черта под слоганом — акцент с референса. */}
      <ExpoLinearGradient
        colors={gradients.brandWide}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={{
          width: Math.round(markSize * 0.5),
          height: 4,
          borderRadius: radius.pill,
          marginTop: 16,
        }}
      />
    </View>
  );
}

/** Мягкое свечение под знаком. Радиальный градиент — только через SVG. */
function MarkHalo({ size }: { size: number }) {
  const box = Math.round(size * 1.9);

  return (
    <View
      pointerEvents="none"
      style={{
        position: 'absolute',
        width: box,
        height: box,
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Svg width={box} height={box}>
        <Defs>
          <RadialGradient id="mark-halo" cx="50%" cy="50%" r="50%">
            <Stop offset="0" stopColor={colors.violet} stopOpacity={0.34} />
            <Stop offset="0.55" stopColor={colors.purple} stopOpacity={0.14} />
            <Stop offset="1" stopColor={colors.purple} stopOpacity={0} />
          </RadialGradient>
        </Defs>
        <Ellipse cx={box / 2} cy={box / 2} rx={box / 2} ry={box / 2} fill="url(#mark-halo)" />
      </Svg>
    </View>
  );
}

/**
 * «UzCasting»: «Uz» с переходом синий → фиолетовый, «Casting» белым.
 *
 * Размер берём у настоящего `<Text>` с теми же параметрами и рисуем SVG
 * поверх измеренного места. Иначе ширину пришлось бы угадывать, а метрики
 * засечкового шрифта на iOS и Android разные.
 */
function GradientWord({ text, font }: { text: string; font: number }) {
  const [box, setBox] = useState<{ width: number; height: number } | null>(null);

  const onLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (box && box.width === width && box.height === height) return;
    setBox({ width, height });
  };

  const typography = { fontFamily: BRAND_FONT, fontSize: font };

  return (
    <View>
      {/*
        Эталон раскладки. Пока размер не измерен — он же и виден: иначе на
        первом кадре название пропадало бы совсем.
      */}
      <Text
        onLayout={onLayout}
        style={{ ...typography, color: colors.white, opacity: box ? 0 : 1 }}
      >
        {text}
      </Text>

      {box ? (
        <Svg width={box.width} height={box.height} style={StyleSheet.absoluteFill}>
          <Defs>
            <LinearGradient id="uz-part" x1="0" y1="0" x2="1" y2="0">
              <Stop offset="0" stopColor={colors.blue} />
              <Stop offset="1" stopColor={colors.violet} />
            </LinearGradient>
          </Defs>
          <SvgText
            x={0}
            y={(box.height + font * CAP_HEIGHT_RATIO) / 2}
            fontSize={font}
            fontFamily={BRAND_FONT}
          >
            <TSpan fill="url(#uz-part)">Uz</TSpan>
            <TSpan fill={colors.white}>Casting</TSpan>
          </SvgText>
        </Svg>
      ) : null}
    </View>
  );
}

/** Знак и название в строку — шапка главной. */
function InlineWordmark({
  size,
  plain,
  shine,
}: {
  size: 'md' | 'lg';
  plain: boolean;
  shine: boolean;
}) {
  const s = SIZES[size];
  const [box, setBox] = useState<{ width: number; height: number } | null>(null);
  const [frame, setFrame] = useState<{ width: number; height: number } | null>(null);

  const onLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (box && box.width === width && box.height === height) return;
    setBox({ width, height });
  };

  // Блику нужна ширина ВСЕЙ строки — знак плюс слово. Измеряется только
  // когда блик включён: лишний `onLayout` в шапке ни к чему.
  const onFrame = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (frame && frame.width === width && frame.height === height) return;
    setFrame({ width, height });
  };

  const typography = {
    fontSize: s.font,
    fontWeight: '700' as const,
    letterSpacing: s.tracking,
  };

  return (
    <View className="flex-row items-center gap-2" onLayout={shine ? onFrame : undefined}>
      <Logo size={s.mark} />

      <View>
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

      {shine && frame ? (
        <ShineSweep
          width={frame.width}
          height={frame.height}
          // Маска берёт обычный `<Text>`, а не SVG-копию слова: маске нужна
          // только альфа, то есть форма букв, а градиентная заливка на неё
          // не влияет. Геометрия совпадает — SVG и так нарисован поверх
          // этого же измеренного текста.
          mask={
            <View
              className="flex-row items-center gap-2"
              style={{ width: frame.width, height: frame.height }}
            >
              <Logo size={s.mark} />
              <Text style={{ ...typography, color: colors.white }}>UZCASTING</Text>
            </View>
          }
        />
      ) : null}
    </View>
  );
}
