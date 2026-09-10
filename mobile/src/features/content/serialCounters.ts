import { useWatchEpisode } from '@/features/watch/api';
import { useEpisodes } from '@/features/watch/episodes';
import type { WatchInfo } from '@/features/watch/types';

/**
 * Счётчики сериала там, где карточки `/content/{id}` на сервере ещё нет.
 *
 * <h2>⚠️ Что чинилось (10.09.2026)</h2>
 * «Нравится» у сериала не было видно, пока его не нажмёшь: число
 * бралось из карточки `/content/{id}`, а у фильма ещё и из `/watch`. На
 * боевом сервере стояла сборка, где карточки нет вовсе, а `/watch` на
 * сериал отвечает «спрашивай серию». Источника числа не оставалось —
 * оно появлялось только из ответа на само нажатие и пропадало, стоило
 * уйти со страницы.
 *
 * <h2>Откуда число</h2>
 * `/watch/{episodeId}` той же сборки отдаёт «нравится» САМОГО контента
 * (`WatchController`: `likes(episode.getContent())`) — для любой серии
 * одно и то же. Берётся первая серия из списка.
 *
 * <h2>⚠️ Только счётчики</h2>
 * Ответ серии говорит и о доступе к ЭТОЙ серии («закрыто, купите»). Для
 * страницы сериала это неправда: первая серия может быть платной, а
 * остальные — нет. Поэтому результат идёт только в плитки, а не в `info`
 * страницы, от которого зависят замок и кнопка «Tomosha qilish».
 *
 * <h2>Когда включается</h2>
 * Только если карточка ответила «такого адреса нет» — на новой сборке два
 * лишних запроса не уходят вовсе. Уйдёт старая сборка — эта обходная
 * дорога просто перестанет включаться.
 */
export function useSerialCountersFallback(
  contentId: number | null,
  enabled: boolean
): WatchInfo | undefined {
  const episodes = useEpisodes(enabled ? contentId : null);
  const firstEpisodeId = enabled ? (episodes.data?.episodes[0]?.id ?? null) : null;
  const watch = useWatchEpisode(firstEpisodeId);

  return enabled ? watch.data : undefined;
}
