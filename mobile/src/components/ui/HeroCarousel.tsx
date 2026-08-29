import { Image } from 'expo-image';
import { useEffect, useRef, useState } from 'react';
import {
  Dimensions,
  StyleSheet,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  Pressable,
  ScrollView,
  Text,
  View,
} from 'react-native';

import { Badge, type BadgeTone } from './Badge';
import { Button } from './Button';

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const CARD_WIDTH = SCREEN_WIDTH - 32;
const STRIDE = CARD_WIDTH + 12;

export type HeroItem = {
  id: string;
  title: string;
  /** Всё, кроме заголовка, необязательно: баннеры приходят с сервера
   *  и заполнены по-разному — у рекламы нет подзаголовка, у части
   *  премьер выключена кнопка (`buttonEnabled = false`). */
  subtitle?: string;
  badgeLabel?: string;
  ctaLabel?: string;
  imageUrl?: string;
  /**
   * Открывается ли нажатием по самому кадру.
   *
   * `buttonEnabled` в админке управляет КНОПКОЙ, а не ссылкой. Пока
   * нажималась только кнопка, баннер с выключенной кнопкой и настроенной
   * ссылкой был картинкой в никуда — админ считал, что настроил переход.
   */
  pressable?: boolean;
};

/**
 * Hero-карусель верхнего блока главной: премьеры и рекламные баннеры.
 *
 * <h2>Почему карусель сама листается</h2>
 * Порядок баннеров задаёт админ (`sortOrder` в панели) — это осмысленная
 * последовательность, а не набор. Без автолистания её видел бы только тот,
 * кто догадался свайпнуть: второй и третий баннер не показывались бы почти
 * никому, хотя в отчётах числились бы «размещёнными».
 *
 * Листание останавливается, пока человек трогает карусель, и пока экран не
 * на переднем плане — иначе счётчик показов набивал бы рекламе просмотры,
 * которых не было.
 */
export function HeroCarousel({
  items,
  /** Розовый бейдж «ПРЕМЬЕРА» из ТЗ — только для премьер.
   *  Рекламный баннер тем же тоном выдавал бы себя за премьеру. */
  badgeTone = 'premiere',
  onPressItem,
  autoAdvanceMs,
  onItemVisible,
  active = true,
}: {
  items: HeroItem[];
  badgeTone?: BadgeTone;
  onPressItem?: (itemId: string) => void;
  /** Интервал автолистания. Не задан — карусель стоит. */
  autoAdvanceMs?: number;
  /** Вызывается, когда кадр оказался перед человеком (в том числе первый). */
  onItemVisible?: (itemId: string) => void;
  /** Экран на переднем плане. На фоне не листаем и показы не считаем. */
  active?: boolean;
}) {
  const [index, setIndex] = useState(0);
  const scroller = useRef<ScrollView>(null);

  /** Пока палец на карусели, автолистание молчит. */
  const [dragging, setDragging] = useState(false);

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const next = Math.round(e.nativeEvent.contentOffset.x / STRIDE);
    if (next !== index) setIndex(next);
  };

  // Показ засчитывается за кадр, который реально оказался перед человеком.
  //
  // Наружу отдаётся id, а не объект: массив баннеров пересоздаётся при
  // каждом ререндере главной, и эффект по нему стрелял бы без конца.
  const visibleId = items[index]?.id;
  useEffect(() => {
    if (active && visibleId !== undefined) onItemVisible?.(visibleId);
  }, [visibleId, active, onItemVisible]);

  useEffect(() => {
    if (!autoAdvanceMs || !active || dragging || items.length < 2) return;

    const id = setTimeout(() => {
      // По кругу: дойдя до конца, возвращаемся к первому. Иначе карусель
      // замирала бы на последнем баннере до конца сессии.
      const next = (index + 1) % items.length;
      scroller.current?.scrollTo({ x: next * STRIDE, animated: true });
      setIndex(next);
    }, autoAdvanceMs);

    return () => clearTimeout(id);
  }, [autoAdvanceMs, active, dragging, index, items.length]);

  return (
    <View className="gap-3">
      <ScrollView
        ref={scroller}
        horizontal
        pagingEnabled={false}
        snapToInterval={STRIDE}
        decelerationRate="fast"
        showsHorizontalScrollIndicator={false}
        onScroll={onScroll}
        onScrollBeginDrag={() => setDragging(true)}
        onMomentumScrollEnd={() => setDragging(false)}
        onScrollEndDrag={() => setDragging(false)}
        scrollEventThrottle={32}
        contentContainerClassName="gap-3 pr-4"
      >
        {items.map((item) => (
          <Pressable
            key={item.id}
            style={{ width: CARD_WIDTH, height: 210 }}
            disabled={!item.pressable}
            onPress={() => onPressItem?.(item.id)}
            accessibilityRole={item.pressable ? 'button' : undefined}
            className="justify-end overflow-hidden rounded-card-lg bg-surface-2 p-4 active:opacity-90"
          >
            {item.imageUrl ? (
              // ⚠️ `absoluteFill`, а НЕ `width/height: '100%'`. Проценты у
              // абсолютного элемента считаются от контентной области, то есть
              // от карточки МИНУС `p-4`, и без `top/left` он вдобавок встаёт
              // внутрь отступа. Кадр не доставал до краёв, а вокруг него
              // висела тёмная рамка в 16px.
              <Image
                source={{ uri: item.imageUrl }}
                style={StyleSheet.absoluteFill}
                contentFit="cover"
                transition={200}
              />
            ) : null}
            {/* Затемнение, чтобы текст читался поверх кадра */}
            <View className="absolute inset-0 bg-ink/55" />

            {/* Бейдж — в левом верхнем углу кадра, а не над заголовком.
                Заказчик: вывести в угол и сделать полупрозрачным, чтобы
                сквозь него был виден кадр. Плотная плашка вырезала из
                фотографии прямоугольник. */}
            {item.badgeLabel ? (
              <View className="absolute left-4 top-4">
                <Badge tone={badgeTone} translucent>
                  {item.badgeLabel}
                </Badge>
              </View>
            ) : null}

            <View className="gap-2">
              <Text numberOfLines={2} className="text-h1 text-text">
                {item.title}
              </Text>
              {item.subtitle ? (
                <Text numberOfLines={1} className="text-caption text-text-muted">
                  {item.subtitle}
                </Text>
              ) : null}
              {item.ctaLabel ? (
                <Button
                  variant="premium"
                  className="mt-1 self-start"
                  onPress={() => onPressItem?.(item.id)}
                >
                  {item.ctaLabel}
                </Button>
              ) : null}
            </View>
          </Pressable>
        ))}
      </ScrollView>

      <View className="flex-row justify-center gap-1.5">
        {items.map((item, i) => (
          <View
            key={item.id}
            className={`h-1.5 rounded-pill ${
              i === index ? 'w-5 bg-purple' : 'w-1.5 bg-border'
            }`}
          />
        ))}
      </View>
    </View>
  );
}
