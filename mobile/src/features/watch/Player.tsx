import { Ionicons } from '@expo/vector-icons';
import { useEvent, useEventListener } from 'expo';
import { VideoView, useVideoPlayer } from 'expo-video';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Pressable,
  Text,
  View,
  useWindowDimensions,
  type GestureResponderEvent,
  type LayoutChangeEvent,
} from 'react-native';

import { trackContentComplete, trackContentPlay } from '@/features/analytics/api';
import { type Orientation, frameRatio, isVertical } from '@/features/content/orientation';
import { BASE_URL, authHeaders } from '@/lib/api';
import { getItem, setItem } from '@/lib/storage';
import { colors } from '@/theme/tokens';

import {
  type Quality,
  type Rung,
  effective,
  parseQuality,
  qualityTag,
  rungLabel,
  rungs,
  variantUrl,
} from './quality';
import { useWatchProgress } from './useWatchProgress';

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
 * <h2>ABR не программируется вручную, но его можно отключить</h2>
 * `expo-video` использует AVPlayer на iOS и ExoPlayer на Android — оба
 * переключают качество сами, по фактической скорости сети. Своя логика
 * замера видит меньше, чем плеер, и решала бы хуже.
 *
 * Но плеер меряет скорость, а не цену: на хорошем 4G он честно возьмёт
 * 1080p (≈5,5 Мбит/с). Поэтому под кадром есть выбор ступени — см.
 * `features/watch/quality`, там же объяснено, чем за него платят.
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
 *
 * <h2>Сбой — ожидаемое состояние, а не редкость</h2>
 * Каждая ссылка на этом пути живёт ограниченное время: билет плейлиста —
 * 6 часов, подпись S3 — около трёх, токен CDN — четыре-пять. Фильм,
 * поставленный на паузу вечером и продолженный утром, упирается ровно в
 * это. Поэтому плеер сообщает о сбое наружу — см. `onError`.
 */

/**
 * Что именно открывает плеер.
 *
 * Вынесено отдельно, потому что это РЕШЕНИЕ, а не разметка: какой из
 * двух путей воспроизведения выбран и уходит ли токен. Внутри
 * компонента его нельзя ни прочитать, ни проверить.
 *
 * <pre>
 *   hlsUrl есть  → HLS, ABR — качество переключается само
 *   hlsUrl null  → сервер приложения, один файл, фиксированное качество
 * </pre>
 *
 * <h2>⚠️ `hlsUrl` бывает ДВУХ видов</h2>
 * <pre>
 *   «/api/v1/…»   → защищённый плейлист нашего сервера, нужен BASE_URL
 *   «https://…»   → CDN напрямую, BASE_URL добавлять НЕЛЬЗЯ
 * </pre>
 *
 * Различает первый символ. Если добавить `BASE_URL` ко второму,
 * получится `https://uzcasting.sitehttps://cdn…` — видео молча не
 * откроется, без единой ошибки.
 *
 * <h2>⚠️ Токен уходит ТОЛЬКО на путь `url`</h2>
 * Там он обязателен: сервер проверяет право доступа.
 *
 * На путь HLS его слать НЕЛЬЗЯ, и это не про кэширование. AVPlayer и
 * ExoPlayer задают заголовки на весь поток — они попали бы и в запрос
 * сегмента. Сегменты же идут прямо в хранилище по подписанной ссылке,
 * а S3 не принимает два способа авторизации сразу: запрос и с
 * `Authorization`, и с `X-Amz-Signature` он отклоняет. То есть видео
 * не открылось бы вовсе.
 *
 * Право доступа на этом пути проверяется по билету внутри самой
 * ссылки — его выдаёт сервер вместе с `hlsUrl`.
 */
export function playbackSource(source: VideoSource, variant?: string | null) {
  if (source.hlsUrl !== null) {
    const master = source.hlsUrl.startsWith('/')
      ? `${BASE_URL}${source.hlsUrl}`
      : source.hlsUrl;
    // `contentType` — не украшение: без него iOS не разбирает HLS, если в
    // адресе после «.m3u8» стоит запрос (а у нас там билет), и список
    // ступеней приходит пустым.
    return { uri: variant ?? master, contentType: 'hls' as const };
  }
  return { uri: `${BASE_URL}${source.url}`, headers: authHeaders() };
}

/** Где лежит выбранная ступень. Выбор общий для всех видео, не для одного. */
const QUALITY_KEY = 'watch.quality';

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
  onError,
  controls = 'native',
  title = null,
  actions = null,
  fill = false,
  autoPlay = false,
}: {
  source: VideoSource;
  orientation: Orientation | null;
  /** Для аналитики: просмотры и досмотры считаются по контенту. */
  contentId: number | null;
  episodeId: number | null;

  /**
   * Чьи кнопки под кадром.
   *
   * <pre>
   *   'native' → кнопки платформы (ExoPlayer / AVPlayer)
   *   'custom' → наша панель: макет заказчика от 10.09.2026
   * </pre>
   *
   * ⚠️ По умолчанию НАТИВНЫЕ. Своя панель нужна там, где рядом с
   * перемоткой стоят «нравится», комментарии и донат — то есть на
   * основном видео. У трейлера их нет, и подменять ему привычные
   * кнопки платформы не за чем.
   *
   * ⚠️ В ПОЛНОЭКРАННОМ режиме платформа всегда рисует свои — так
   * устроены и ExoPlayer, и AVPlayer (docs.expo.dev, SDK 57). Поэтому
   * наша панель живёт до нажатия «на весь экран», а не после.
   */
  controls?: 'native' | 'custom';

  /** Название контента — верхняя строка на макете. Только для 'custom'. */
  title?: string | null;

  /**
   * Ряд действий в левом нижнем углу кадра: «нравится», комментарии,
   * звёзды, монеты. Плеер не знает, что они делают, — он только даёт им
   * место на кадре.
   */
  actions?: React.ReactNode;

  /**
   * Кадр занимает ВСЮ рамку, а не считает свой размер по пропорции.
   *
   * Нужно полноэкранной странице просмотра: там рамка уже размером с
   * экран, и второй расчёт пропорции оставил бы чёрные поля внутри
   * чёрных полей.
   */
  fill?: boolean;

  /**
   * Начать воспроизведение сразу.
   *
   * Полноэкранное окно открывается нажатием «Tomosha qilish» — человек
   * уже сказал «смотреть». Остановить его на кадре с кнопкой «play»
   * значило бы заставить нажать то же самое второй раз. Трейлеру на
   * странице этого не нужно: там кадр стоит, пока его не тронут.
   */
  autoPlay?: boolean;

  /**
   * Видео не открылось или оборвалось.
   *
   * ⚠️ Плеер НЕ решает, что показать вместо себя: рамка кадра, афиша и
   * кнопка повтора — забота экрана. Здесь только факт.
   *
   * `message` — текст платформы (ExoPlayer или AVPlayer). Человеку он не
   * показывается: там бывает «Source error» и коды HTTP.
   */
  onError?: (message: string | null) => void;
}) {
  const { t } = useTranslation();
  const vertical = isVertical(orientation);
  const { height: windowHeight } = useWindowDimensions();

  const custom = controls === 'custom';

  /**
   * Лестница качества этого видео.
   *
   * Приходит от плеера — он уже разобрал плейлист и знает, какие кодеки
   * тянет устройство. Разбирать текст плейлиста самим значило бы делать
   * ту же работу хуже и лишним запросом.
   */
  const [ladder, setLadder] = useState<Rung[]>([]);
  const [quality, setQuality] = useState<Quality>('auto');

  // Выбор общий для всех видео: человек один раз сказал «беречь трафик».
  useEffect(() => {
    let cancelled = false;
    void getItem(QUALITY_KEY).then((raw) => {
      if (!cancelled) setQuality(parseQuality(raw));
    });
    return () => {
      cancelled = true;
    };
  }, []);

  // Выбранной ступени у ЭТОГО видео может не быть — тогда играет «Авто»,
  // и кнопка должна быть подсвечена соответственно.
  const chosen = effective(ladder, quality);

  const player = useVideoPlayer(
    playbackSource(source, variantUrl(ladder, chosen)),
    (p) => {
      p.loop = false;
      // Полоса времени обновляется отсюда. Четверть секунды — компромисс:
      // при секунде полоса дёргается заметными скачками, при 0,1 с плеер
      // будит JS десять раз в секунду ради двух цифр.
      p.timeUpdateEventInterval = custom ? 0.25 : 0;
      if (autoPlay) p.play();
    }
  );

  /**
   * Список ступеней.
   *
   * ⚠️ Пустой список НЕ затирает прошлый. После переключения на ступень
   * плеер получает плейлист с ОДНИМ вариантом и честно сообщает о нём —
   * лестница исчезла бы вместе с меню, и вернуться к «Авто» было бы нечем.
   */
  useEventListener(player, 'sourceLoad', ({ availableVideoTracks }) => {
    const list = rungs(availableVideoTracks);
    if (list.length > 0) setLadder(list);
  });

  const chooseQuality = (next: Quality) => {
    setQuality(next);
    void setItem(QUALITY_KEY, qualityTag(next));
  };

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

  /**
   * Сбой воспроизведения.
   *
   * ⚠️ Без этого обработчика кадр просто замирает чёрным прямоугольником:
   * ни ошибки, ни спиннера, ни объяснения. Просроченная подпись и оборванная
   * сеть выглядят одинаково — «приложение сломалось».
   *
   * Проверяется именно `status`, а не наличие `error`: поле необязательное,
   * и на части сбоев платформа его не заполняет — условие по нему пропускало
   * бы их молча.
   */
  useEventListener(player, 'statusChange', ({ status, error }) => {
    if (status === 'error') onError?.(error?.message ?? null);
  });

  /**
   * «Продолжить просмотр»: запоминает секунду, на которой остановились.
   *
   * <h2>⚠️ Серия важнее контента</h2>
   * У многосерийного контента позиция принадлежит КОНКРЕТНОЙ серии.
   * Если писать её на контент, вторая серия перетирала бы первую и
   * человек возвращался бы в середину не той серии.
   *
   * `episodeId` есть только у многосерийного, у фильма он `null` — и
   * тогда единица просмотра действительно контент.
   */
  useWatchProgress({
    player,
    type: episodeId !== null ? 'EPISODE' : 'CONTENT',
    targetId: episodeId ?? contentId,
    // До появления выбора сюда всегда уходило `auto` — выбирать было нечем.
    quality: qualityTag(chosen),
  });

  // Ширину меряем, а не считаем из размера окна: у экрана свои отступы,
  // и посчитанная «на глаз» ширина разъехалась бы с настоящей.
  const [boxWidth, setBoxWidth] = useState(0);
  const [boxHeight, setBoxHeight] = useState(0);
  const onLayout = (e: LayoutChangeEvent) => {
    setBoxWidth(e.nativeEvent.layout.width);
    setBoxHeight(e.nativeEvent.layout.height);
  };

  const computed = frame(boxWidth, windowHeight, vertical);
  const size = fill ? { width: boxWidth, height: boxHeight } : computed;

  const view = useRef<VideoView>(null);

  const qualityMenu =
    ladder.length > 1 ? (
      <>
        <QualityChip
          label={t('content.qualityAuto')}
          selected={chosen === 'auto'}
          onPress={() => chooseQuality('auto')}
        />
        {ladder.map((r) => (
          <QualityChip
            key={r.height}
            label={rungLabel(r.height)}
            selected={chosen === r.height}
            onPress={() => chooseQuality(r.height)}
          />
        ))}
      </>
    ) : null;

  return (
    <View onLayout={onLayout} className={fill ? 'flex-1' : 'items-center'}>
      <View style={{ width: size.width, height: size.height }}>
        <VideoView
          ref={view}
          player={player}
          style={{
            width: size.width,
            height: size.height,
            borderRadius: fill ? 0 : 16,
            backgroundColor: '#000',
          }}
          contentFit="contain"
          nativeControls={!custom}
          fullscreenOptions={{
            enable: true,
            // Ландшафт для обычного видео, портрет для рилса.
            orientation: vertical ? 'portrait' : 'landscape',
          }}
          allowsPictureInPicture={false}
        />

        {custom && size.width > 0 ? (
          <Controls
            player={player}
            title={title}
            actions={actions}
            quality={qualityMenu}
            fallbackDuration={source.durationSeconds}
            onFullscreen={() => void view.current?.enterFullscreen().catch(() => {})}
          />
        ) : null}
      </View>

      {/*
        Меню появляется, только если ступеней действительно несколько.
        «Авто / 480p» при одной ступени обещало бы выбор, которого нет.

        ⚠️ Со своей панелью оно переезжает НА кадр: под кадром там ничего
        нет — страница просмотра занимает весь экран.
      */}
      {!custom && qualityMenu ? (
        <View className="mt-2 flex-row flex-wrap justify-center gap-2">
          {qualityMenu}
        </View>
      ) : null}
    </View>
  );
}

/**
 * Своя панель управления — макет заказчика от 10.09.2026.
 *
 * <h2>Что на ней стоит и почему именно это</h2>
 * Сверху название (в полноэкранном плеере больше неоткуда понять, что
 * играет), по центру перемотка на 10 секунд и пауза, снизу полоса
 * времени, а под ней — «нравится», комментарии, звёзды, монеты и «на
 * весь экран». Кнопки платформы этого ряда не знают вовсе: ни у
 * ExoPlayer, ни у AVPlayer нет места, куда его добавить.
 *
 * <h2>⚠️ Панель ПРЯЧЕТСЯ во время игры</h2>
 * Иначе она закрывала бы нижнюю пятую часть кадра весь просмотр.
 * Возвращается нажатием на кадр — как везде. На паузе не прячется:
 * человек остановил видео как раз ради этих кнопок.
 */
function Controls({
  player,
  title,
  actions,
  quality,
  fallbackDuration,
  onFullscreen,
}: {
  player: ReturnType<typeof useVideoPlayer>;
  title: string | null;
  actions: React.ReactNode;
  quality: React.ReactNode;
  /** Длительность из карточки — пока плеер не разобрал файл. */
  fallbackDuration: number | null;
  onFullscreen: () => void;
}) {
  const [shown, setShown] = useState(true);
  const [barWidth, setBarWidth] = useState(0);

  const { isPlaying } = useEvent(player, 'playingChange', {
    isPlaying: player.playing,
    oldIsPlaying: player.playing,
  });

  const { currentTime } = useEvent(player, 'timeUpdate', {
    currentTime: player.currentTime,
    bufferedPosition: 0,
    currentLiveTimestamp: null,
    currentOffsetFromLive: null,
  });

  /**
   * Длительность.
   *
   * ⚠️ Держим в состоянии, а не читаем `player.duration` при отрисовке:
   * это обращение в нативный модуль, и оно повторялось бы четыре раза в
   * секунду, на каждом обновлении времени. Событие `sourceLoad` приносит
   * её один раз — этого хватает.
   */
  const [duration, setDuration] = useState(0);
  useEventListener(player, 'sourceLoad', (payload) => {
    if (Number.isFinite(payload.duration) && payload.duration > 0) {
      setDuration(payload.duration);
    }
  });

  const total = duration > 0 ? duration : (fallbackDuration ?? 0);

  // Панель уходит через три секунды после того, как видео пошло. На
  // паузе таймера нет вовсе — см. заголовок.
  useEffect(() => {
    if (!shown || !isPlaying) return;
    const timer = setTimeout(() => setShown(false), 3000);
    return () => clearTimeout(timer);
  }, [shown, isPlaying]);

  const seekTo = useCallback(
    (e: GestureResponderEvent) => {
      if (barWidth <= 0 || total <= 0) return;
      const ratio = Math.min(1, Math.max(0, e.nativeEvent.locationX / barWidth));
      player.currentTime = ratio * total;
    },
    [barWidth, player, total]
  );

  const progress = total > 0 ? Math.min(1, Math.max(0, currentTime / total)) : 0;

  const fillAll = { position: 'absolute', left: 0, right: 0, top: 0, bottom: 0 } as const;

  /*
    ⚠️ Панель СКРЫВАЕТСЯ прозрачностью, а не снимается с экрана.

    В её нижнем ряду живут окна донатов (`actions`). Сними панель по
    таймеру — и вместе с ней закрылось бы окно, которое человек открыл
    три секунды назад, пока видео продолжало играть.

    Касания: подложка ловит нажатие по пустому месту (показать/скрыть), а
    слой с кнопками — `box-none`, то есть сам нажатий не забирает и
    пропускает промахи вниз, к подложке. Скрытый слой — `none`: невидимые
    кнопки нажиматься не должны.
  */
  return (
    <View style={fillAll} pointerEvents="box-none">
      <Pressable
        onPress={() => setShown((v) => !v)}
        accessibilityRole="button"
        accessibilityLabel={shown ? 'Boshqaruvni yashirish' : "Boshqaruvni ko'rsatish"}
        style={fillAll}
      />

      <View
        pointerEvents={shown ? 'box-none' : 'none'}
        style={[fillAll, { opacity: shown ? 1 : 0 }]}
        className="justify-between bg-black/35"
      >
        <Text numberOfLines={1} className="px-3 pt-3 text-center text-caption text-white">
          {title ?? ''}
        </Text>

        <View
          pointerEvents="box-none"
          className="flex-row items-center justify-center gap-10"
        >
          <GlyphButton
            icon="play-back"
            label="10 soniya orqaga"
            onPress={() => player.seekBy(-10)}
          />
          <GlyphButton
            icon={isPlaying ? 'pause' : 'play'}
            label={isPlaying ? 'Pauza' : 'Davom ettirish'}
            size={34}
            onPress={() => (isPlaying ? player.pause() : player.play())}
          />
          <GlyphButton
            icon="play-forward"
            label="10 soniya oldinga"
            onPress={() => player.seekBy(10)}
          />
        </View>

        <View pointerEvents="box-none" className="gap-2 px-3 pb-3">
          {quality ? (
            <View
              pointerEvents="box-none"
              className="flex-row flex-wrap justify-end gap-2"
            >
              {quality}
            </View>
          ) : null}

          <View pointerEvents="box-none" className="flex-row items-center gap-2">
            <Text className="text-micro text-white/80">{clock(currentTime)}</Text>

            {/*
              Полоса времени.

              ⚠️ Нажимаемая область в несколько раз выше самой полосы:
              попасть пальцем в три пункта невозможно, а промах по полосе
              перемотки читается как «плеер не слушается».
            */}
            <Pressable
              onPress={seekTo}
              onLayout={(e) => setBarWidth(e.nativeEvent.layout.width)}
              accessibilityRole="adjustable"
              accessibilityLabel="Vaqt chizig'i"
              className="flex-1 justify-center py-2"
            >
              <View className="h-[3px] w-full rounded-pill bg-white/30">
                <View
                  style={{ width: `${progress * 100}%` }}
                  className="h-[3px] rounded-pill bg-white"
                />
              </View>
            </Pressable>

            <Text className="text-micro text-white/80">{clock(total)}</Text>
          </View>

          <View
            pointerEvents="box-none"
            className="flex-row items-center justify-between gap-3"
          >
            <View pointerEvents="box-none" className="flex-1 flex-row items-center gap-4">
              {actions}
            </View>

            <Pressable
              onPress={onFullscreen}
              accessibilityRole="button"
              accessibilityLabel="Butun ekran"
              hitSlop={10}
              className="active:opacity-60"
            >
              <Ionicons name="expand" size={20} color={colors.white} />
            </Pressable>
          </View>
        </View>
      </View>
    </View>
  );
}

function GlyphButton({
  icon,
  label,
  onPress,
  size = 26,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  onPress: () => void;
  size?: number;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      hitSlop={14}
      className="active:opacity-60"
    >
      <Ionicons name={icon} size={size} color={colors.white} />
    </Pressable>
  );
}

/**
 * Секунды → «12:34» или «1:02:03».
 *
 * Час дописывается только тогда, когда он есть: «00:12:34» на
 * двухминутном ролике читается как ошибка вёрстки.
 */
function clock(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00';

  const whole = Math.floor(seconds);
  const s = whole % 60;
  const m = Math.floor(whole / 60) % 60;
  const h = Math.floor(whole / 3600);

  const mm = h > 0 ? String(m).padStart(2, '0') : String(m);
  return `${h > 0 ? `${h}:` : ''}${mm}:${String(s).padStart(2, '0')}`;
}

/** Кнопка одной ступени. Форма та же, что у переключателя частей. */
function QualityChip({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected }}
      className={`rounded-pill px-3 py-1.5 ${selected ? 'bg-purple' : 'bg-surface'}`}
    >
      <Text
        className={`text-micro ${selected ? 'font-semibold text-white' : 'text-text-muted'}`}
      >
        {label}
      </Text>
    </Pressable>
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
