import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  FlatList,
  Pressable,
  Text,
  View,
  useWindowDimensions,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { GlowBackdrop } from '@/components/ui/GlowBackdrop';
import { markOnboardingSeen } from '@/features/onboarding/store';
import { colors } from '@/theme/tokens';

/**
 * S02 — мини-презентация при первом входе.
 *
 * Состав слайдов задан подписью к макету: casting · premyera · creator daromadi.
 * Четвёртый — Stars, они из письма заказчика (docs/MONETIZATION.md).
 *
 * Показывается один раз: флаг лежит в features/onboarding/store.
 */
type Slide = {
  key: string;
  icon: keyof typeof Ionicons.glyphMap;
  gradient: [string, string];
};

// Первый слайд — фирменная пара синий → фиолетовый с референса заказчика,
// дальше шкала расходится в маджента, золото и циан.
const SLIDES: Slide[] = [
  { key: 'casting', icon: 'sparkles-outline', gradient: [colors.blue, colors.purple] },
  { key: 'premiere', icon: 'play-circle-outline', gradient: [colors.purple, colors.magenta] },
  { key: 'earn', icon: 'trending-up-outline', gradient: [colors.magenta, colors.gold] },
  { key: 'stars', icon: 'star-outline', gradient: [colors.blue, colors.cyan] },
];

export default function OnboardingScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();

  const [index, setIndex] = useState(0);
  const [slideHeight, setSlideHeight] = useState(0);
  const listRef = useRef<FlatList<Slide>>(null);

  const isLast = index === SLIDES.length - 1;

  const finish = async () => {
    await markOnboardingSeen();
    router.replace('/(auth)/sign-in');
  };

  const onPrimary = () => {
    if (isLast) {
      finish();
      return;
    }
    const next = index + 1;
    listRef.current?.scrollToIndex({ index: next, animated: true });
    setIndex(next);
  };

  /*
    Индекс считаем на каждом кадре прокрутки, а не по её остановке.
    onMomentumScrollEnd не годится: в вебе перетаскивание мышью не даёт
    инерции, событие не приходит вовсе — точки и надпись на кнопке
    застревали на первом слайде. Заодно точки теперь следуют за пальцем.
  */
  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    if (width <= 0) return;
    const next = Math.round(e.nativeEvent.contentOffset.x / width);
    if (next !== index && next >= 0 && next < SLIDES.length) {
      setIndex(next);
    }
  };

  return (
    <View className="flex-1 bg-ink" style={{ paddingTop: insets.top }}>
      <GlowBackdrop intensity="hero" />

      <View className="h-11 justify-center px-6">
        <Pressable onPress={finish} hitSlop={12} className="self-end">
          <Text className="text-body text-text-muted">{t('onboarding.skip')}</Text>
        </Pressable>
      </View>

      {/*
        Высоту слайда задаём числом: внутри горизонтального FlatList потомок
        с flex не растягивается на всю высоту — блоки схлопываются по контенту.
      */}
      <View className="flex-1" onLayout={(e) => setSlideHeight(e.nativeEvent.layout.height)}>
        <FlatList
          ref={listRef}
          data={SLIDES}
          keyExtractor={(item) => item.key}
          horizontal
          pagingEnabled
          showsHorizontalScrollIndicator={false}
          onScroll={onScroll}
          scrollEventThrottle={16}
          getItemLayout={(_, i) => ({ length: width, offset: width * i, index: i })}
          extraData={slideHeight}
          renderItem={({ item }) => (
            <SlideView slide={item} width={width} height={slideHeight} />
          )}
        />
      </View>

      <View className="items-center gap-8 px-6" style={{ paddingBottom: insets.bottom + 24 }}>
        <View className="flex-row items-center gap-2">
          {SLIDES.map((slide, i) => (
            <Dot key={slide.key} active={i === index} />
          ))}
        </View>

        <Button variant="primary" shape="card" onPress={onPrimary} className="w-full">
          {isLast ? t('onboarding.start') : t('onboarding.next')}
        </Button>
      </View>
    </View>
  );
}

function SlideView({
  slide,
  width,
  height,
}: {
  slide: Slide;
  width: number;
  height: number;
}) {
  const { t } = useTranslation();

  return (
    <View style={{ width, height }} className="px-6">
      {/*
        Иллюстраций пока нет — на макете здесь placeholder.
        Вместо серого прямоугольника ставим градиент из палитры и иконку темы:
        то же место, но экран не выглядит недоделанным.

        Доля высоты, а не aspectRatio: на узких экранах пропорция раздувала
        картинку и текст уползал вниз.
      */}
      <View className="overflow-hidden rounded-card-lg" style={{ flex: 5 }}>
        <LinearGradient
          colors={slide.gradient}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}
        >
          <View
            className="items-center justify-center rounded-full"
            style={{
              width: 128,
              height: 128,
              backgroundColor: 'rgba(7,7,13,0.35)',
            }}
          >
            <Ionicons name={slide.icon} size={64} color={colors.white} />
          </View>
        </LinearGradient>
      </View>

      <View className="justify-center gap-3" style={{ flex: 2 }}>
        <Text className="text-center text-h1 text-text">
          {t(`onboarding.${slide.key}.title`)}
        </Text>
        <Text className="text-center text-body text-text-muted">
          {t(`onboarding.${slide.key}.body`)}
        </Text>
      </View>
    </View>
  );
}

/** Активная точка залита, остальные — контур. Как на макете. */
function Dot({ active }: { active: boolean }) {
  return (
    <View
      style={{
        width: 8,
        height: 8,
        borderRadius: 4,
        borderWidth: 1,
        borderColor: active ? colors.purple : colors.textDisabled,
        backgroundColor: active ? colors.purple : 'transparent',
      }}
    />
  );
}
