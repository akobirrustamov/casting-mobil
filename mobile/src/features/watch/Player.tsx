import { useEventListener } from 'expo';
import { VideoView, useVideoPlayer } from 'expo-video';
import { useRef, useState } from 'react';
import { View, useWindowDimensions, type LayoutChangeEvent } from 'react-native';

import { trackContentComplete, trackContentPlay } from '@/features/analytics/api';
import { type Orientation, frameRatio, isVertical } from '@/features/content/orientation';
import { BASE_URL, authHeaders } from '@/lib/api';

import type { VideoSource } from './types';

/**
 * Плеер одного видеофрагмента.
 *
 * <h2>Два пути воспроизведения</h2>
 * Если транскодирование закончено и CDN настроен — играем HLS с CDN,
 * и качество переключается само (ABR). Иначе идём по старому пути,
 * через сервер приложения, одним файлом.
 *
 * Выбор — в `playbackSource()`; там же объяснено, почему токен уходит
 * только на второй путь.
 *
 * <h2>ABR не программируется вручную</h2>
 * `expo-video` использует AVPlayer на iOS и ExoPlayer на Android —
 * оба переключают качество сами, по фактической скорости сети. Своя
 * логика замера видит меньше, чем плеер, и решала бы хуже.
 *
 * <h2>Два формата вместо одного</h2>
 * Рамка была жёстко 220 px высотой, то есть всегда горизонтальной.
 * Вертикальный ролик из админки («Формат: Reels») показывался в ней узкой
 * полоской посреди чёрного прямоугольника — бо́льшая часть кадра просто не
 * помещалась. Теперь форму задаёт `orientation` с сервера.
 *
 * Полноэкранный режим тоже разный: рилс разворачивается в портрет, обычное
 * видео — в ландшафт. Без этого на телефоне с включённым замком поворота
 * фильм оставался бы вертикальной ленточкой даже «на весь экран».
 */

/**
 * Что именно открывает плеер.
 *
 * Вынесено отдельно, потому что это РЕШЕНИЕ, а не разметка: какой из
 * двух путей воспроизведения выбран и уходит ли токен. Внутри
 * компонента его нельзя ни прочитать, ни проверить.
 *
 * <pre>
 *   hlsUrl есть  → CDN, HLS, ABR — качество переключается само
 *   hlsUrl null  → сервер приложения, один файл, фиксированное качество
 * </pre>
 *
 * ⚠️ `hlsUrl` уже АБСОЛЮТНЫЙ — `BASE_URL` к нему не добавляется.
 * `url` наоборот относительный, и без `BASE_URL` он не откроется.
 *
 * ⚠️ Токен уходит ТОЛЬКО на второй путь. Там он обязателен: видео
 * проверяет право доступа. На CDN его слать не нужно и вредно —
 * сегментов у одного эпизода сотни, лишний заголовок на каждом
 * запросе мешает кэшированию, а некоторые CDN отклоняют запросы с
 * неожиданной авторизацией.
 */
export function playbackSource(source: VideoSource) {
  if (source.hlsUrl !== null) {
    return { uri: source.hlsUrl };
  }
  return { uri: `${BASE_URL}${source.url}`, headers: authHeaders() };
}

/**
 * Доля высоты окна, которую может занять вертикальный кадр.
 *
 * Полные 9:16 — это почти весь экран: под плеером не осталось бы ни
 * названия, ни кнопки покупки, и человек не понял бы, что там что-то есть.
 * Остаток кадра доступен по кнопке «на весь экран».
 */
const VERTICAL_SCREEN_SHARE = 0.62;

export function Player({
  source,
  orientation,
  contentId,
  episodeId,
}: {
  source: VideoSource;
  orientation: Orientation | null;
  /** Для аналитики: просмотры и досмотры считаются по контенту. */
  contentId: number | null;
  episodeId: number | null;
}) {
  const vertical = isVertical(orientation);
  const { height: windowHeight } = useWindowDimensions();

  const player = useVideoPlayer(playbackSource(source), (p) => {
    p.loop = false;
  });

  /**
   * `CONTENT_PLAY` — один раз на фрагмент.
   *
   * `playingChange` срабатывает и после каждой паузы. Без этого флага одно
   * видео дало бы столько «запусков», сколько раз человек нажал паузу, и
   * счётчик просмотров в панели раздулся бы на ровном месте.
   */
  const played = useRef(false);

  useEventListener(player, 'playingChange', ({ isPlaying }) => {
    if (!isPlaying || played.current || contentId === null) return;
    played.current = true;
    trackContentPlay(contentId, episodeId);
  });

  useEventListener(player, 'playToEnd', () => {
    if (contentId !== null) trackContentComplete(contentId, episodeId);
  });

  // Ширину меряем, а не считаем из размера окна: у экрана свои отступы,
  // и посчитанная «на глаз» ширина разъехалась бы с настоящей.
  const [boxWidth, setBoxWidth] = useState(0);
  const onLayout = (e: LayoutChangeEvent) => setBoxWidth(e.nativeEvent.layout.width);

  const size = frame(boxWidth, windowHeight, vertical);

  return (
    <View onLayout={onLayout} className="items-center">
      <VideoView
        player={player}
        style={{
          width: size.width,
          height: size.height,
          borderRadius: 16,
          backgroundColor: '#000',
        }}
        contentFit="contain"
        nativeControls
        fullscreenOptions={{
          enable: true,
          // Ландшафт для обычного видео, портрет для рилса.
          orientation: vertical ? 'portrait' : 'landscape',
        }}
        allowsPictureInPicture={false}
      />
    </View>
  );
}

/**
 * Размер рамки под кадр.
 *
 * Пока ширина не измерена — нулевая: пустая рамка честнее, чем кадр не того
 * размера, который через мгновение прыгнет.
 */
function frame(
  boxWidth: number,
  windowHeight: number,
  vertical: boolean
): { width: number; height: number } {
  if (boxWidth <= 0) return { width: 0, height: 0 };

  if (!vertical) {
    return { width: boxWidth, height: Math.round(boxWidth / frameRatio('LANDSCAPE')) };
  }

  const full = Math.round(boxWidth / frameRatio('VERTICAL'));
  const height = Math.min(full, Math.round(windowHeight * VERTICAL_SCREEN_SHARE));

  // Ширину пересчитываем от высоты: обрезанный по высоте кадр иначе получил
  // бы чёрные поля по бокам вместо честной пропорции.
  return { width: Math.round(height * frameRatio('VERTICAL')), height };
}
