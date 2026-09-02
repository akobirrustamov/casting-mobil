import { Image } from 'expo-image';
import { useEffect, useRef, useState } from 'react';
import {
  StyleSheet,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  Pressable,
  ScrollView,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';

import { Badge, type BadgeTone } from './Badge';
import { Button } from './Button';

/**
 * Форма баннера — 16:9, ОДНА на все экраны.
 *
 * <h2>Почему пропорция, а не высота в пикселях</h2>
 * Раньше кадр был `ширина экрана − 32` на ЖЁСТКИЕ 210dp. Ширина при этом
 * зависела от телефона, а высота нет — то есть пропорция кадра плавала от
 * 1.56:1 на узком экране до 1.9:1 на широком. Файл в админку загружается
 * ОДИН, и подойти к обеим он не мог: на одном телефоне у баннера срезало
 * верх и низ, на другом — бока, и заранее увидеть это было нельзя.
 *
 * Теперь высота считается от ширины. 16:9 — формат, в котором баннер и
 * просят загружать (`adminpanel/mediaSpecs.banner`), поэтому `cover`
 * ничего не обрезает: загруженный файл виден целиком.
 *
 * Единственное исключение — `MIN_BANNER_HEIGHT` ниже.
 */
const BANNER_RATIO = 16 / 9;

/** Зазор между баннерами — `gap-3` у ленты. */
const BANNER_GAP = 12;

/** Поля экрана — `px-4` у `Screen`. */
const SCREEN_PADDING = 16;

/**
 * Нижняя граница высоты кадра.
 *
 * Внутри баннера лежит реальный текст: заголовок в две строки (60),
 * подзаголовок (18), кнопка (44), зазоры и `p-4` — около 170dp. На экране
 * 320dp пропорция 16:9 дала бы 162dp, и кнопка вышла бы за нижний край.
 *
 * То есть это не «красивое число», а высота содержимого. Срабатывает
 * только на очень узких телефонах (<312dp), где кадр слегка обрезается по
 * бокам — на всех современных ширинах остаётся ровно 16:9.
 */
const MIN_BANNER_HEIGHT = 176;

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

  // ⚠️ Хук, а не `Dimensions.get()` на уровне модуля: то значение
  // замерялось ОДИН раз при загрузке файла и после поворота экрана или на
  // складном телефоне оставалось старым — карусель промахивалась мимо
  // кадра, потому что шаг прокрутки считался по прежней ширине.
  const { width: screenWidth } = useWindowDimensions();
  const cardWidth = screenWidth - SCREEN_PADDING * 2;
  const cardHeight = Math.max(Math.round(cardWidth / BANNER_RATIO), MIN_BANNER_HEIGHT);
  const stride = cardWidth + BANNER_GAP;

  /** Пока палец на карусели, автолистание молчит. */
  const [dragging, setDragging] = useState(false);

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const next = Math.round(e.nativeEvent.contentOffset.x / stride);
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
      scroller.current?.scrollTo({ x: next * stride, animated: true });
      setIndex(next);
    }, autoAdvanceMs);

    return () => clearTimeout(id);
  }, [autoAdvanceMs, active, dragging, index, items.length, stride]);

  return (
    <View className="gap-3">
      <ScrollView
        ref={scroller}
        horizontal
        pagingEnabled={false}
        snapToInterval={stride}
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
            style={{ width: cardWidth, height: cardHeight }}
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
