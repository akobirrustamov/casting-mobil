import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useIsFocused } from 'expo-router';
import { useEffect, useRef } from 'react';
import { Animated, Easing, Pressable, StyleSheet, View } from 'react-native';
import Svg, { Defs, Path, Text as SvgText, TextPath } from 'react-native-svg';

import { colors, gradients } from '@/theme/tokens';

/**
 * Круглая кнопка «Ariza qoldirish» — правый нижний угол каталога.
 *
 * <h2>Почему надпись по кругу, а не под кнопкой</h2>
 * Одна иконка не объясняет, что случится по нажатию, а подпись в
 * строку под кружком заняла бы полэкрана по ширине и упёрлась бы в
 * край. Текст по окружности решает обе задачи сразу — так же сделано
 * на сайте (`frontend/src/pages/home/Home.js`).
 *
 * <h2>Три анимации и зачем каждая</h2>
 * 1. Кольцо с текстом ВРАЩАЕТСЯ — иначе надпись читается только с той
 *    стороны, где она началась.
 * 2. Блик пробегает по кружку раз в несколько секунд — это то самое
 *    «чтобы притягивало взгляд». Реже, чем кажется нужным: анимация,
 *    которая не замолкает, раздражает и её перестают замечать.
 * 3. Ореол пульсирует под кнопкой, отрывая её от постеров каталога.
 *
 * Всё три — на нативном драйвере (`transform`/`opacity`), поэтому
 * список под кнопкой скроллится без рывков.
 */
const SIZE = 104;
/** Внутренний круг с иконкой; между ним и краем идёт текст. */
const CORE = 62;
const RING_ROTATE_MS = 14_000;
const SHINE_SWEEP_MS = 1_200;
const SHINE_PAUSE_MS = 3_200;
const PULSE_MS = 2_400;

export function ApplyFab({
  label,
  onPress,
  bottom,
  right,
}: {
  /** Текст по кругу — слово повторяется дважды, точка между повторами. */
  label: string;
  onPress: () => void;
  bottom: number;
  right: number;
}) {
  const spin = useRef(new Animated.Value(0)).current;
  const shine = useRef(new Animated.Value(0)).current;
  const pulse = useRef(new Animated.Value(0)).current;
  // Вкладки не размонтируются: без этого три бесконечные анимации
  // крутились бы и тогда, когда «Casting» скрыт за другой вкладкой.
  const focused = useIsFocused();

  useEffect(() => {
    if (!focused) return;

    const spinLoop = Animated.loop(
      Animated.timing(spin, {
        toValue: 1,
        duration: RING_ROTATE_MS,
        easing: Easing.linear,
        useNativeDriver: true,
      })
    );

    const shineLoop = Animated.loop(
      Animated.sequence([
        Animated.delay(SHINE_PAUSE_MS),
        Animated.timing(shine, {
          toValue: 1,
          duration: SHINE_SWEEP_MS,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        // ⚠️ Явный возврат: с нативным драйвером сброс цикла до
        // значения не доходит и блик проходит один раз (та же беда
        // была у логотипа в шапке — см. `ui/Wordmark`).
        Animated.timing(shine, { toValue: 0, duration: 0, useNativeDriver: true }),
      ])
    );

    const pulseLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, {
          toValue: 1,
          duration: PULSE_MS,
          easing: Easing.out(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(pulse, { toValue: 0, duration: 0, useNativeDriver: true }),
      ])
    );

    spinLoop.start();
    shineLoop.start();
    pulseLoop.start();
    return () => {
      spinLoop.stop();
      shineLoop.stop();
      pulseLoop.stop();
    };
  }, [focused, spin, shine, pulse]);

  const rotate = spin.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });
  const shineX = shine.interpolate({ inputRange: [0, 1], outputRange: [-CORE, CORE] });
  const pulseScale = pulse.interpolate({ inputRange: [0, 1], outputRange: [1, 1.45] });
  const pulseOpacity = pulse.interpolate({ inputRange: [0, 1], outputRange: [0.45, 0] });

  // Повтор с точкой: на окружности у текста нет начала и конца, и без
  // разделителя два прохода слипаются в одно длинное слово.
  const ringText = `${label} • ${label} • `;

  return (
    <View
      pointerEvents="box-none"
      style={{ position: 'absolute', right, bottom, width: SIZE, height: SIZE }}
    >
      <Animated.View
        pointerEvents="none"
        style={{
          position: 'absolute',
          left: (SIZE - CORE) / 2,
          top: (SIZE - CORE) / 2,
          width: CORE,
          height: CORE,
          borderRadius: CORE / 2,
          backgroundColor: colors.purple,
          opacity: pulseOpacity,
          transform: [{ scale: pulseScale }],
        }}
      />

      {/* Кольцо с надписью. Вращается целиком — текст внутри статичен. */}
      <Animated.View
        pointerEvents="none"
        style={[StyleSheet.absoluteFill, { transform: [{ rotate }] }]}
      >
        <Svg width={SIZE} height={SIZE} viewBox="0 0 100 100">
          <Defs>
            {/* Окружность r=40: текст идёт между краем и внутренним кругом. */}
            <Path id="applyRing" d="M50,50 m-40,0 a40,40 0 1,1 80,0 a40,40 0 1,1 -80,0" />
          </Defs>
          <SvgText
            fill={colors.white}
            fontSize={8}
            fontWeight="600"
            // textLength = длина окружности (2π·40 ≈ 251): надпись
            // ложится ровно на круг и не обрывается на полуслове.
            textLength={251}
            lengthAdjust="spacing"
          >
            <TextPath href="#applyRing" startOffset="0">
              {ringText.toUpperCase()}
            </TextPath>
          </SvgText>
        </Svg>
      </Animated.View>

      <Pressable
        onPress={onPress}
        accessibilityRole="button"
        accessibilityLabel={label}
        style={{
          position: 'absolute',
          left: (SIZE - CORE) / 2,
          top: (SIZE - CORE) / 2,
          width: CORE,
          height: CORE,
          borderRadius: CORE / 2,
          overflow: 'hidden',
          shadowColor: colors.purple,
          shadowOpacity: 0.5,
          shadowRadius: 12,
          shadowOffset: { width: 0, height: 6 },
          elevation: 8,
        }}
        className="items-center justify-center active:opacity-80"
      >
        <LinearGradient
          colors={gradients.premium}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={StyleSheet.absoluteFill}
        />

        {/* Блик: наклонная светлая полоса проходит по кружку. */}
        <Animated.View
          pointerEvents="none"
          style={{
            position: 'absolute',
            top: -CORE / 2,
            width: CORE / 3,
            height: CORE * 2,
            transform: [{ translateX: shineX }, { rotate: '18deg' }],
          }}
        >
          <LinearGradient
            colors={['rgba(255,255,255,0)', 'rgba(255,255,255,0.55)', 'rgba(255,255,255,0)']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 0 }}
            style={StyleSheet.absoluteFill}
          />
        </Animated.View>

        <Ionicons name="person-add" size={24} color={colors.white} />
      </Pressable>
    </View>
  );
}
