import { LinearGradient } from 'expo-linear-gradient';
import { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, View, type ViewStyle } from 'react-native';
import Svg, { Defs, Path, RadialGradient, Stop } from 'react-native-svg';

import { colors } from '@/theme/tokens';

import { CUTOUT, CUTOUT_VIEWBOX, CUTOUT_VIEW, LOGO_PAD } from './logoCutout';
import { LOGO_GRADIENT, LOGO_PLAY, LOGO_SPROCKETS, LOGO_VIEWBOX } from './logoPaths';

/**
 * Загрузка фирменным знаком: плёнка бежит, кнопка плея дышит, по знаку
 * идёт перелив.
 *
 * <h2>Как это устроено: знак — это ОКНО</h2>
 * Анимировать сам знак нечем: он приходит из SVG готовой картинкой, и
 * подсветить внутри неё одну деталь нельзя. Поэтому всё наоборот — знак
 * не рисуется, а ВЫРЕЗАЕТСЯ. Поверх движущихся слоёв лежит трафарет:
 * прямоугольник фона с контурами знака и правилом `evenodd`, из-за
 * которого сам знак оказывается дыркой. Что бы ни двигалось под
 * трафаретом, видно это только в границах знака.
 *
 * Плата за приём — трафарет НЕПРОЗРАЧЕН и обязан совпадать с фоном под
 * ним (`background`). На картинке или градиенте загрузчик выдаст себя
 * квадратом; для тёмных экранов приложения это ровно `colors.ink`.
 *
 * <h2>Почему не маска SVG</h2>
 * `<Mask>` сняла бы это ограничение, но тогда двигать блик пришлось бы
 * внутри SVG — то есть анимировать его свойства, а это JS-поток. Загрузчик
 * показывают ровно тогда, когда JS занят: он бы дёргался именно в тот
 * момент, ради которого нужен. Здесь всё движение — `transform` и
 * `opacity` обычных `View`, то есть нативный поток.
 *
 * <h2>Что анимировано</h2>
 * Три детали, и все три УЖЕ нарисованы в знаке — ничего не дорисовано:
 *   • перфорация плёнки — бегущий огонёк по ходу ленты;
 *   • треугольник плея — подсветка в такт с лентой;
 *   • весь знак — один блик наискось.
 */

/**
 * Набор «Протяжка» — тот, что выбрали из трёх на разборе.
 *
 * Лента идёт быстро и коротко, плея вспыхивает с ней в такт, блик один и
 * заметно медленнее: он читается как отдельное движение, а не как ещё одно
 * мельтешение поверх ленты.
 */

/** Один кадр протяжки плёнки. */
const FILM_STEP_MS = 70;

/** Пауза между протяжками — без неё огонёк сливается в ровное мерцание. */
const FILM_PAUSE_MS = 260;

const FILM_CYCLE_MS = LOGO_SPROCKETS.length * FILM_STEP_MS + FILM_PAUSE_MS;

/**
 * Полувзмах подсветки плея: ровно половина цикла ленты, поэтому кнопка
 * выходит на пик, когда огонёк добегает до конца плёнки. Считается, а не
 * вписано числом, — иначе правка шага ленты молча развела бы их по фазе.
 */
const PULSE_MS = FILM_CYCLE_MS / 2;

/** Проход блика по знаку. */
const SHEEN_MS = 2_000;

/** Пауза перед следующим проходом: блик приходит гостем, а не метрономом. */
const SHEEN_DELAY_MS = 260;

/**
 * Полудыхание ореола.
 *
 * Не кратно ни ленте, ни блику — и это единственная причина числа. Совпади
 * периоды, знак начал бы мигать целиком вместо перелива; на прежних 1150 мс
 * ореол почти сошёлся с замедленным бликом (2300 против 2260).
 */
const HALO_MS = 1_300;

export function BrandLoader({
  /**
   * Сторона ЗНАКА. Места компонент занимает примерно в 1.35 раза больше —
   * поле вокруг знака отдано ореолу.
   */
  size = 104,
  /** Цвет трафарета. Обязан совпадать с фоном под загрузчиком. */
  background = colors.ink,
}: {
  size?: number;
  background?: string;
}) {
  const box = Math.round((size * CUTOUT_VIEW) / LOGO_VIEWBOX);

  /** Единица viewBox в пикселях. */
  const unit = box / CUTOUT_VIEW;
  /** Координата знака → пиксель внутри холста (начало холста сдвинуто на поле). */
  const at = (v: number) => (v + LOGO_PAD) * unit;

  return (
    <View
      accessibilityRole="progressbar"
      pointerEvents="none"
      // Блик выезжает за пределы холста — без обрезки он лёг бы на соседей.
      style={{ width: box, height: box, overflow: 'hidden' }}
    >
      {/* Заливка знака. Видна только сквозь вырез, но лежит на весь холст:
          так градиент идёт по знаку непрерывно, а не по каждой детали. */}
      <LinearGradient
        colors={LOGO_GRADIENT}
        start={{ x: 0, y: 1 }}
        end={{ x: 1, y: 0 }}
        style={StyleSheet.absoluteFill}
      />

      <PlayPulse
        style={{
          position: 'absolute',
          left: at(LOGO_PLAY.x),
          top: at(LOGO_PLAY.y),
          width: LOGO_PLAY.w * unit,
          height: LOGO_PLAY.h * unit,
        }}
      />

      <Sheen
        box={box}
        color="rgba(255,255,255,0.72)"
        duration={SHEEN_MS}
        delay={SHEEN_DELAY_MS}
      />

      <Svg
        width={box}
        height={box}
        viewBox={CUTOUT_VIEWBOX}
        style={StyleSheet.absoluteFill}
      >
        <Path d={CUTOUT} fill={background} fillRule="evenodd" />
      </Svg>

      <Halo box={box} />

      {/* Огонёк ПОВЕРХ трафарета: перфорация — это фон, а не вырез, и
          подсветить её снизу нечем. */}
      <FilmRun unit={unit} at={at} />
    </View>
  );
}

/**
 * Ореол вокруг знака.
 *
 * Рисуется тем же вырезом, что и трафарет, поэтому ложится ТОЛЬКО на
 * подложку и не мутит сам знак. Свечение, положенное поверх целиком,
 * пришлось бы гасить до незаметности, чтобы оно не съедало краску.
 */
function Halo({ box }: { box: number }) {
  const breath = useRef(new Animated.Value(0.45)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(breath, {
          toValue: 1,
          duration: HALO_MS,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(breath, {
          toValue: 0.45,
          duration: HALO_MS,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [breath]);

  return (
    <Animated.View style={[StyleSheet.absoluteFill, { opacity: breath }]}>
      <Svg width={box} height={box} viewBox={CUTOUT_VIEWBOX}>
        <Defs>
          <RadialGradient id="brand-loader-halo" cx="50%" cy="50%" r="50%">
            <Stop offset="0" stopColor={colors.violet} stopOpacity={0.5} />
            <Stop offset="0.5" stopColor={colors.purple} stopOpacity={0.18} />
            <Stop offset="1" stopColor={colors.purple} stopOpacity={0} />
          </RadialGradient>
        </Defs>
        <Path d={CUTOUT} fill="url(#brand-loader-halo)" fillRule="evenodd" />
      </Svg>
    </Animated.View>
  );
}

/** Подсветка за треугольником плея — «кнопка дышит». */
function PlayPulse({ style }: { style: ViewStyle }) {
  const pulse = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, {
          toValue: 1,
          duration: PULSE_MS,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(pulse, {
          toValue: 0,
          duration: PULSE_MS,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [pulse]);

  return (
    <Animated.View style={[style, { opacity: pulse }]}>
      <LinearGradient
        colors={['#FFFFFF', '#F0ABFC']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={StyleSheet.absoluteFill}
      />
    </Animated.View>
  );
}

/**
 * Бегущий огонёк по перфорации.
 *
 * Одно значение на все отверстия: каждое берёт из него свой отрезок.
 * Одиннадцать отдельных анимаций разъезжались бы по фазе — плёнка бы
 * мерцала вразнобой, а не двигалась.
 */
function FilmRun({ unit, at }: { unit: number; at: (v: number) => number }) {
  const run = useRef(new Animated.Value(0)).current;
  const count = LOGO_SPROCKETS.length;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(run, {
          toValue: count,
          duration: count * FILM_STEP_MS,
          easing: Easing.linear,
          useNativeDriver: true,
        }),
        Animated.delay(FILM_PAUSE_MS),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [count, run]);

  return (
    <>
      {LOGO_SPROCKETS.map((hole, i) => (
        <Animated.View
          key={`${hole.x}-${hole.y}`}
          style={{
            position: 'absolute',
            left: at(hole.x),
            top: at(hole.y),
            width: hole.w * unit,
            height: hole.h * unit,
            borderRadius: Math.max(1, 3 * unit),
            backgroundColor: colors.white,
            // Огонёк проходит отверстие почти за два шага: соседние
            // успевают перекрыться, и по ленте идёт волна, а не отдельные
            // вспышки.
            opacity: run.interpolate({
              inputRange: [i - 0.9, i, i + 0.9],
              outputRange: [0, 1, 0],
              extrapolate: 'clamp',
            }),
          }}
        />
      ))}
    </>
  );
}

/**
 * Световая полоса наискось по знаку.
 *
 * Начинается и заканчивается ЗА краем холста: иначе видно, как она
 * возникает из ниоткуда. Возврат в начало делает сам `Animated.loop` —
 * он сбрасывает значение перед каждым проходом.
 */
function Sheen({
  box,
  color,
  duration,
  delay,
}: {
  box: number;
  color: string;
  duration: number;
  delay: number;
}) {
  const progress = useRef(new Animated.Value(0)).current;
  const band = Math.max(16, Math.round(box * 0.13));

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.delay(delay),
        Animated.timing(progress, {
          toValue: 1,
          duration,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [delay, duration, progress]);

  const translateX = progress.interpolate({
    inputRange: [0, 1],
    outputRange: [-band * 2, box + band * 2],
  });

  return (
    <Animated.View
      style={{
        position: 'absolute',
        left: 0,
        // Полоса наклонена, поэтому она выше холста: без запаса её углы
        // обрезались бы прямо по знаку.
        top: -box,
        width: band,
        height: box * 3,
        transform: [{ translateX }, { rotate: '20deg' }],
      }}
    >
      <LinearGradient
        colors={['transparent', color, 'transparent']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={StyleSheet.absoluteFill}
      />
    </Animated.View>
  );
}
