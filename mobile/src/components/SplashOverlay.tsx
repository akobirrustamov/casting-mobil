import { useEffect, useRef } from 'react';
import { Animated, Easing, Text, View } from 'react-native';

import { Logo } from '@/components/ui/Logo';
import { colors } from '@/theme/tokens';

/**
 * S01 — экран загрузки.
 *
 * Лежит поверх навигатора, а не отдельным маршрутом: иначе «/» конфликтует
 * с (tabs)/index, и переход обратно оставлял бы splash в истории.
 *
 * По макету: логотип в рамке с неоновым свечением, подпись под ним,
 * три пульсирующие точки и слово loading внизу. Держим 1–2 секунды.
 */
const DOTS = [0, 1, 2];

/**
 * Слои ореола. Пятью слоями получались видимые концентрические кольца,
 * поэтому берём много слоёв с очень низкой прозрачностью — накладываясь,
 * они дают плавное затухание вместо ступенек.
 */
const GLOW_LAYERS = 14;
const GLOW_SPREAD = 92; // насколько далеко свечение уходит от рамки

const GLOW = Array.from({ length: GLOW_LAYERS }, (_, i) => {
  const step = (i + 1) / GLOW_LAYERS;
  return {
    inset: Math.round(GLOW_SPREAD * step),
    // Квадратичное затухание — ближе к рамке ярче, дальше почти прозрачно
    opacity: 0.05 * (1 - step) ** 2 + 0.008,
  };
});

export function SplashOverlay({ subtitle }: { subtitle: string }) {
  // Появление знака: чуть увеличиваем и проявляем — «оживает», а не мигает
  const appear = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(appear, {
      toValue: 1,
      duration: 520,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
    }).start();
  }, [appear]);

  return (
    <View className="absolute inset-0 items-center justify-center bg-ink">
      {/*
        Выравнивание через style, а не className: NativeWind не оборачивает
        Animated.View, и класс просто игнорируется — подпись уезжала влево.
      */}
      <Animated.View
        style={{
          alignItems: 'center',
          opacity: appear,
          transform: [
            { scale: appear.interpolate({ inputRange: [0, 1], outputRange: [0.92, 1] }) },
          ],
        }}
      >
        <View className="items-center justify-center">
          {/*
            Неон собран из слоёв: shadowColor даёт цветную тень только на iOS
            и в вебе, на Android она игнорируется, а elevation бесцветна.
            Поэтому ореол рисуем подложками — одинаково выглядит везде.

            Слоёв много, а прозрачность низкая: с двумя плотными слоями
            вместо свечения получались видимые прямоугольники с чёткими краями.
            Размер задаём отрицательным inset, чтобы halo сам centrировался
            по рамке и не разъезжался при смене размера логотипа.
          */}
          {GLOW.map((layer) => (
            <View
              key={layer.inset}
              pointerEvents="none"
              style={{
                position: 'absolute',
                top: -layer.inset,
                bottom: -layer.inset,
                left: -layer.inset,
                right: -layer.inset,
                borderRadius: 22 + layer.inset,
                backgroundColor: colors.purple,
                opacity: layer.opacity,
              }}
            />
          ))}

          <View
            className="items-center justify-center rounded-card-lg border px-10 py-9"
            style={{
              borderColor: colors.purple,
              backgroundColor: colors.ink,
              shadowColor: colors.purple,
              shadowOpacity: 0.9,
              shadowRadius: 24,
              shadowOffset: { width: 0, height: 0 },
            }}
          >
            <View className="flex-row items-center gap-2">
              <Logo size={26} />
              <Text
                className="text-h1 text-text"
                style={{ letterSpacing: 2.5, fontWeight: '800' }}
              >
                UZCASTING
              </Text>
            </View>
          </View>
        </View>

        {/* Отступ больше самого дальнего слоя ореола, иначе подпись лежит на свечении */}
        <Text className="text-caption text-text-muted" style={{ marginTop: GLOW_SPREAD + 24 }}>
          {subtitle}
        </Text>
      </Animated.View>

      <View className="absolute bottom-24 flex-row items-center gap-2">
        {DOTS.map((i) => (
          <PulsingDot key={i} delay={i * 180} />
        ))}
        <Text className="ml-2 text-caption" style={{ color: colors.purple }}>
          loading
        </Text>
      </View>
    </View>
  );
}

/** Точка «дышит» — бесконечный цикл со сдвигом, чтобы шли волной. */
function PulsingDot({ delay }: { delay: number }) {
  const value = useRef(new Animated.Value(0.35)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.delay(delay),
        Animated.timing(value, {
          toValue: 1,
          duration: 420,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(value, {
          toValue: 0.35,
          duration: 420,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [delay, value]);

  return (
    <Animated.View
      style={{
        width: 8,
        height: 8,
        borderRadius: 4,
        backgroundColor: colors.purple,
        opacity: value,
        transform: [
          { scale: value.interpolate({ inputRange: [0.35, 1], outputRange: [0.85, 1] }) },
        ],
      }}
    />
  );
}
