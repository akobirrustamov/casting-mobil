import { Ionicons } from '@expo/vector-icons';
import { useIsFocused } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenState } from '@/components/states/ScreenState';
import type { ContentDetail } from '@/features/content/detail';
import { isVertical } from '@/features/content/orientation';
import { PlayerActions } from '@/features/content/PlayerActions';
import { TOUCH_TARGET, colors } from '@/theme/tokens';

import { Player, playbackSource } from './Player';
import type { VideoSource, WatchInfo } from './types';

/**
 * Полноэкранный просмотр — общий для фильма (окно поверх карточки) и для
 * серии (отдельный маршрут `/episode/{id}`).
 *
 * <h2>⚠️ Что здесь чинилось (18.09.2026)</h2>
 * 1. После «Tomosha qilish» плеер сам уходил в СИСТЕМНЫЙ полный экран. Там
 *    платформа рисует только свои кнопки — «нравится», комментарии и
 *    донаты пропадали ровно тогда, когда их ищут. Теперь полный экран
 *    наш (`Player.landscape`), и ряд действий на кадре остаётся.
 * 2. Серия открывалась страницей с маленьким плеером, который через
 *    секунду сам разворачивался, — выглядело как «перекинуло куда-то, и
 *    там видео открылось само». Теперь нажатие на серию сразу даёт этот
 *    экран.
 *
 * Обычное (горизонтальное) видео стартует развёрнутым: человек уже сказал
 * «смотреть», второе нажатие ради полного экрана ему не нужно. Рилс
 * разворачивать некуда — он и так во весь портретный экран.
 */
export function FullscreenWatch({
  contentId,
  detail,
  info,
  onClose,
  onRetry,
  landscape,
  onLandscapeChange,
}: {
  contentId: number | null;
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
  onClose: () => void;
  /** Перезапрашивает `/watch` — оттуда приходит свежий адрес видео. */
  onRetry: () => Promise<unknown>;
  /**
   * Развёрнут ли кадр — из {@link useLandscape}. Состояние у ВЫЗЫВАЮЩЕГО:
   * «назад» на Android сначала сворачивает кадр, а ловит его экран
   * (у окна — `onRequestClose`, у маршрута — `BackHandler`).
   */
  landscape: boolean;
  onLandscapeChange: (next: boolean) => void;
}) {
  const insets = useSafeAreaInsets();

  // Ушли с экрана (комментарии, донаты, вход) — видео не должно играть
  // под чужой страницей.
  const focused = useIsFocused();

  return (
    <View className="flex-1 bg-black" style={{ paddingTop: landscape ? 0 : insets.top }}>
      {/* Только пока экран в фокусе — иначе строка состояния осталась бы
          скрытой и на комментариях, открытых поверх. */}
      <StatusBar hidden={landscape && focused} style="light" />

      <View style={landscape ? StyleSheet.absoluteFill : { flex: 1 }}>
        <Stage
          contentId={contentId}
          detail={detail}
          info={info}
          onRetry={onRetry}
          paused={!focused}
          landscape={landscape}
          onLandscapeChange={onLandscapeChange}
          onBack={onClose}
        />
      </View>
    </View>
  );
}

/** Развёрнут ли кадр. Горизонтальное видео — сразу да, рилс — никогда. */
/**
 * Стрелка «назад» отдельной строкой — только там, где кадра нет (ошибка,
 * пустота, загрузка).
 *
 * ⚠️ Над живым кадром её больше нет (заказчик, 25.09.2026: «yuqoridan
 * ko'p joy tashlangan»): строка съедала ~60 пунктов над вертикальным
 * видео. Там стрелка стоит В КАДРЕ, в одной строке с названием — её
 * рисует сам `Player` (`onBack`).
 */
function BackRow({ onBack }: { onBack: () => void }) {
  return (
    <View className="flex-row px-2 py-2">
      <Pressable
        onPress={onBack}
        accessibilityRole="button"
        accessibilityLabel="Orqaga"
        hitSlop={12}
        style={{ width: TOUCH_TARGET, height: TOUCH_TARGET }}
        className="items-center justify-center rounded-pill active:opacity-60"
      >
        <Ionicons name="chevron-back" size={24} color={colors.white} />
      </Pressable>
    </View>
  );
}

export function useLandscape(info: WatchInfo | undefined) {
  const vertical = isVertical(info?.orientation ?? null);
  const [landscape, setLandscape] = useState(!vertical);

  // Формат приходит с ответом `/watch` — он может прийти позже монтирования.
  useEffect(() => {
    if (vertical) setLandscape(false);
  }, [vertical]);

  return { landscape: landscape && !vertical, setLandscape };
}

/**
 * Кадр с видео.
 *
 * Адрес видео подписан и живёт ограниченное время (билет плейлиста —
 * 6 часов, подпись сегмента — около трёх), поэтому «попробовать ещё раз»
 * здесь означает перезапросить `/watch`, а не перезапустить проигрывание.
 *
 * ⚠️ Повтор РУЧНОЙ. Автоматический бился бы в ту же стену на каждом кадре:
 * причина может быть и в сети, и в снятой подписке.
 */
function Stage({
  contentId,
  detail,
  info,
  onRetry,
  paused,
  landscape,
  onLandscapeChange,
  onBack,
}: {
  contentId: number | null;
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
  onRetry: () => Promise<unknown>;
  paused: boolean;
  landscape: boolean;
  onLandscapeChange: (next: boolean) => void;
  onBack: () => void;
}) {
  const { t } = useTranslation();
  const [part, setPart] = useState(0);
  const [failed, setFailed] = useState(false);
  const [retrying, setRetrying] = useState(false);

  const source: VideoSource | undefined = info?.sources[part];

  const retry = useCallback(() => {
    setRetrying(true);
    void onRetry().finally(() => {
      setRetrying(false);
      setFailed(false);
    });
  }, [onRetry]);

  // Адрес сменился (повтор или «потянули вниз») — прошлый сбой к нему
  // отношения не имеет.
  const uri = source ? playbackSource(source).uri : null;
  useEffect(() => setFailed(false), [uri]);

  // Ошибку и пустоту показываем в портрете: повёрнутый текст ошибки
  // человеку пришлось бы читать боком.
  useEffect(() => {
    if (failed || retrying || !source) onLandscapeChange(false);
  }, [failed, retrying, source, onLandscapeChange]);

  if (!info || info.sources.length === 0) {
    return (
      <View className="flex-1">
        <BackRow onBack={onBack} />
        <View className="flex-1 justify-center">
          <ScreenState kind="empty" body={t('content.noVideo')} />
        </View>
      </View>
    );
  }

  if (source && (failed || retrying)) {
    return (
      <View className="flex-1">
        <BackRow onBack={onBack} />
        <View className="flex-1 justify-center">
          {retrying ? (
            <ScreenState kind="loading" />
          ) : (
            <ScreenState
              kind="error"
              title={t('content.playbackFailedTitle')}
              body={t('content.playbackFailedBody')}
              onRetry={retry}
            />
          )}
        </View>
      </View>
    );
  }

  if (!source) return <BackRow onBack={onBack} />;

  return (
    <View className={landscape ? 'flex-1' : 'flex-1 gap-3 pb-3'}>
      {/* key: смена части пересоздаёт плеер. Признак HLS в ключе —
          чтобы после «потянули вниз» плеер перешёл на появившийся
          `hlsUrl`, а не остался со старым адресом мимо CDN. */}
      <Player
        key={`${source.mediaId ?? part}-${source.hlsUrl ? 'hls' : 'raw'}`}
        source={source}
        orientation={info.orientation}
        contentId={info.contentId}
        episodeId={info.episodeId}
        onError={() => setFailed(true)}
        controls="custom"
        fill
        autoPlay
        paused={paused}
        landscape={landscape}
        onLandscapeChange={onLandscapeChange}
        onBack={onBack}
        title={detail?.title ?? info.title}
        actions={
          <PlayerActions
            contentId={contentId ?? info.contentId}
            detail={detail}
            info={info}
          />
        }
      />

      {!landscape && info.sources.length > 1 ? (
        <View className="flex-row flex-wrap justify-center gap-2 px-4">
          {info.sources.map((s, i) => (
            <Pressable
              key={s.mediaId ?? i}
              onPress={() => setPart(i)}
              accessibilityRole="button"
              accessibilityState={{ selected: i === part }}
              className={`rounded-pill px-4 py-2 ${i === part ? 'bg-purple' : 'bg-surface'}`}
            >
              <Text
                className={`text-caption ${
                  i === part ? 'font-semibold text-white' : 'text-text-muted'
                }`}
              >
                {t('content.part', { number: s.partNumber ?? i + 1 })}
              </Text>
            </Pressable>
          ))}
        </View>
      ) : null}
    </View>
  );
}
