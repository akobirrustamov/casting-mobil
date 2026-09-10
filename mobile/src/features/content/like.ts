import { useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import { useCallback, useState } from 'react';

import { useAuthStore } from '@/features/auth/store';
import { setLike } from '@/features/watch/api';
import type { WatchInfo } from '@/features/watch/types';

import type { ContentDetail } from './detail';

/**
 * «Нравится» на контенте — состояние и нажатие.
 *
 * <h2>Почему это отдельный хук, а не код внутри плитки</h2>
 * Сердце теперь стоит в двух местах: в ряду плиток под кнопкой
 * «Tomosha qilish» и на самом кадре, в панели плеера (макет от
 * 10.09.2026). Две копии этой логики разъехались бы при первой же
 * правке — например, одна ставила бы «нравится» гостю, а вторая нет.
 *
 * <h2>⚠️ Ответ сервера кладётся В КЭШ ЗАПРОСОВ</h2>
 * Раньше он жил только внутри плитки. Человек ставил «нравится», уходил
 * назад, возвращался — и видел серое сердце: экран перерисовывался из
 * кэшированного ответа `/api/v1/app/content/{id}`, снятого ДО нажатия.
 * Со стороны это выглядело как «лайк не сохраняется», хотя на сервере
 * он лежал.
 *
 * Ключ кэша содержит ещё язык и зрителя, поэтому правится он по
 * ПРЕФИКСУ (`setQueriesData`): перечислять остальные части ключа здесь
 * значило бы повторить их устройство в третьем месте.
 */
export function useContentLike(
  contentId: number | null,
  detail: ContentDetail | undefined,
  info: WatchInfo | undefined
) {
  const signedIn = useAuthStore((s) => s.token !== null);
  const queryClient = useQueryClient();

  /**
   * Наше нажатие поверх серверных данных.
   *
   * ⚠️ Живёт до ухода с экрана и НЕ сбрасывается при повторном запросе:
   * иначе сердце мигало бы обратно на каждом `refetch`, пока сервер не
   * пересчитает. Ответ сервера кладём сюда же — он и есть истина.
   */
  const [own, setOwn] = useState<{ liked: boolean; likeCount: number } | null>(null);
  const [busy, setBusy] = useState(false);

  const liked = own?.liked ?? detail?.liked ?? info?.liked ?? false;
  const likes = own?.likeCount ?? detail?.likeCount ?? info?.likeCount ?? null;

  const publish = useCallback(
    (state: { liked: boolean; likeCount: number }) => {
      queryClient.setQueriesData<ContentDetail>(
        { queryKey: ['content-detail', contentId] },
        (old) => (old ? { ...old, liked: state.liked, likeCount: state.likeCount } : old)
      );
      queryClient.setQueriesData<WatchInfo>(
        { queryKey: ['watch', 'content', contentId] },
        (old) => (old ? { ...old, liked: state.liked, likeCount: state.likeCount } : old)
      );
    },
    [contentId, queryClient]
  );

  const toggle = useCallback(async () => {
    if (contentId === null || busy) return;

    // Счётчик — часть описания контента, его видят все. Нажатие требует
    // входа: иначе один человек накрутил бы его сколько угодно раз.
    // Гостя ведём на экран входа, а не показываем ошибку.
    if (!signedIn) {
      router.push('/(auth)/sign-in');
      return;
    }

    const next = !liked;
    const base = likes ?? 0;
    const guess = { liked: next, likeCount: Math.max(0, base + (next ? 1 : -1)) };

    // Сердце откликается сразу: ждать ответа сети — значит показать
    // человеку, что кнопка «не нажалась».
    setOwn(guess);
    setBusy(true);
    try {
      const state = await setLike(contentId, next);
      setOwn(state);
      publish(state);
    } catch {
      // Не получилось — возвращаем как было. Ошибку не показываем:
      // «нравится» не то действие, ради которого стоит закрывать экран
      // сообщением.
      setOwn(null);
    } finally {
      setBusy(false);
    }
  }, [busy, contentId, liked, likes, publish, signedIn]);

  return { liked, likes, busy, toggle };
}
