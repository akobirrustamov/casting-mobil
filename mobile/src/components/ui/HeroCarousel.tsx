import { Image } from 'expo-image';
import { useState } from 'react';
import {
  Dimensions,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  ScrollView,
  Text,
  View,
} from 'react-native';

import { Badge, type BadgeTone } from './Badge';
import { Button } from './Button';

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const CARD_WIDTH = SCREEN_WIDTH - 32;

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
};

/**
 * Hero-карусель премьер — верхний блок главной по ТЗ:
 * «Hero: PREMIYERA / Yangi serial» + свайп между премьерами.
 */
export function HeroCarousel({
  items,
  /** Розовый бейдж «ПРЕМЬЕРА» из ТЗ — только для премьер.
   *  Рекламный баннер тем же тоном выдавал бы себя за премьеру. */
  badgeTone = 'premiere',
  onPressItem,
}: {
  items: HeroItem[];
  badgeTone?: BadgeTone;
  onPressItem?: (item: HeroItem) => void;
}) {
  const [index, setIndex] = useState(0);

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const next = Math.round(e.nativeEvent.contentOffset.x / (CARD_WIDTH + 12));
    if (next !== index) setIndex(next);
  };

  return (
    <View className="gap-3">
      <ScrollView
        horizontal
        pagingEnabled={false}
        snapToInterval={CARD_WIDTH + 12}
        decelerationRate="fast"
        showsHorizontalScrollIndicator={false}
        onScroll={onScroll}
        scrollEventThrottle={32}
        contentContainerClassName="gap-3 pr-4"
      >
        {items.map((item) => (
          <View
            key={item.id}
            style={{ width: CARD_WIDTH, height: 210 }}
            className="justify-end overflow-hidden rounded-card-lg bg-surface-2 p-4"
          >
            {item.imageUrl ? (
              <Image
                source={{ uri: item.imageUrl }}
                style={{ position: 'absolute', width: '100%', height: '100%' }}
                contentFit="cover"
                transition={200}
              />
            ) : null}
            {/* Затемнение, чтобы текст читался поверх кадра */}
            <View className="absolute inset-0 bg-ink/55" />

            <View className="gap-2">
              {item.badgeLabel ? (
                <Badge tone={badgeTone}>{item.badgeLabel}</Badge>
              ) : null}
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
                  onPress={() => onPressItem?.(item)}
                >
                  {item.ctaLabel}
                </Button>
              ) : null}
            </View>
          </View>
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
