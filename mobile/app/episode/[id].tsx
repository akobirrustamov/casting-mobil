import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback } from 'react';
import { BackHandler, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { useContentDetail } from '@/features/content/detail';
import { useWatchEpisode } from '@/features/watch/api';
import { FullscreenWatch, useLandscape } from '@/features/watch/FullscreenWatch';
import { WatchDetail } from '@/features/watch/WatchDetail';

/**
 * Отдельная серия.
 *
 * Сюда ведут список серий, баннеры с `internalTargetType: EPISODE` и
 * уведомления. Право доступа считает сервер (`AccessService`).
 *
 * <h2>⚠️ Открытая серия — СРАЗУ плеер во весь экран</h2>
 * Раньше здесь была страница с маленьким плеером, который через секунду
 * сам уходил в системный полный экран: человек нажимал серию, его
 * «перекидывало» на страницу, и уже там видео открывалось само, а
 * «нравится», комментарии и донаты в системном режиме пропадали
 * (18.09.2026). Теперь — тот же полноэкранный просмотр, что у фильма.
 *
 * Закрытая серия, ошибка и «видео ещё нет» по-прежнему идут в
 * {@link WatchDetail}: там цена, замок и повтор.
 */
export default function EpisodeScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const parsed = Number(id);
  const query = useWatchEpisode(Number.isFinite(parsed) ? parsed : null);
  const info = query.data;

  // Карточка родительского контента — название и счётчики для ряда
  // действий на кадре. Без неё ряд работает на цифрах из `/watch`.
  const detail = useContentDetail(info?.contentId ?? null);

  const { landscape, setLandscape } = useLandscape(info);

  // «Назад» на Android: сначала свернуть кадр, потом уйти со страницы.
  useFocusEffect(
    useCallback(() => {
      if (!landscape) return undefined;
      const sub = BackHandler.addEventListener('hardwareBackPress', () => {
        setLandscape(false);
        return true;
      });
      return () => sub.remove();
    }, [landscape, setLandscape])
  );

  if (query.isPending) {
    return (
      <View className="flex-1 justify-center bg-black">
        <ScreenState kind="loading" />
      </View>
    );
  }

  if (!info || !info.allowed || info.sources.length === 0) {
    return <WatchDetail query={query} />;
  }

  return (
    <FullscreenWatch
      contentId={info.contentId}
      detail={detail.data}
      info={info}
      onClose={() => router.back()}
      onRetry={() => query.refetch()}
      landscape={landscape}
      onLandscapeChange={setLandscape}
    />
  );
}
