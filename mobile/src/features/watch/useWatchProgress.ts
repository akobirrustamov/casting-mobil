import { useEventListener } from 'expo';
import type { VideoPlayer } from 'expo-video';
import { useEffect, useRef } from 'react';

import { persist, restore, SERVER_SAVE_SECONDS } from './progress';
import type { WatchTarget } from './progressApi';

/**
 * Как часто плеер сообщает время, в секундах.
 *
 * ⚠️ По умолчанию `timeUpdateEventInterval` равен НУЛЮ, и это значит
 * «событие не отправлять вовсе» (docs.expo.dev, SDK 57). То есть без
 * этой строки обработчик `timeUpdate` не вызвался бы НИ РАЗУ, и
 * позиция не сохранялась бы совсем — молча, без единой ошибки.
 *
 * Пять секунд, а не пятнадцать: на телефон позиция пишется на каждом
 * тике, и если приложение убьют, потеряется не больше пяти секунд.
 * На сервер уходит реже — см. `SERVER_SAVE_SECONDS`.
 */
const TICK_SECONDS = 5;

/**
 * Запоминает, где человек остановился, и возвращает его туда же.
 *
 * <h2>Что делает</h2>
 * <pre>
 *   открытие  → читает позицию (телефон + сервер) и перематывает
 *   просмотр  → каждые 5 с пишет на телефон, каждые 15 с — на сервер
 *   пауза     → отправляет сразу
 *   закрытие  → отправляет сразу
 * </pre>
 *
 * <h2>⚠️ Почему перемотка не ждёт сеть</h2>
 * Локальная запись читается мгновенно. Если ждать ответа сервера,
 * человек увидел бы, как видео начинается с нуля и через секунду
 * дёргается на нужное место — это выглядит как сбой.
 *
 * Правило простое: перематываем по САМОЙ СВЕЖЕЙ из двух записей
 * (`progress.newer`), а сервер догоняет позже.
 *
 * @param targetId `null` — сохранять некуда, хук ничего не делает
 * @param quality  что человек выбрал вручную; сейчас на мобильном
 *                 всегда `auto` — `videoTrack` в expo-video 57
 *                 только на чтение (проверено в нативных исходниках)
 */
export function useWatchProgress({
  player,
  type,
  targetId,
  quality = 'auto',
}: {
  player: VideoPlayer;
  type: WatchTarget;
  targetId: number | null;
  quality?: string | null;
}) {
  /**
   * Последнее известное состояние.
   *
   * ⚠️ Именно ref, не состояние: значение читается при размонтировании,
   * а обычная переменная там была бы той, что попала в замыкание при
   * первом рендере — то есть нулевой позицией. Человек досмотрел бы до
   * середины и вернулся в начало.
   */
  const latest = useRef({ position: 0, duration: null as number | null });

  /** Когда последний раз ушло на сервер, в секундах видео. */
  const lastServerSave = useRef(0);

  // ⚠️ Без этого `timeUpdate` не придёт ни разу: значение по
  // умолчанию — 0, то есть «не отправлять».
  useEffect(() => {
    player.timeUpdateEventInterval = TICK_SECONDS;
  }, [player]);

  // ------------------------------------------------------- возобновление
  useEffect(() => {
    if (targetId === null) return;

    let cancelled = false;
    lastServerSave.current = 0;

    void restore(type, targetId).then((position) => {
      // ⚠️ Экран могли закрыть, пока шёл запрос. Перемотка мёртвого
      // плеера в лучшем случае ничего не делает, в худшем — падает.
      if (cancelled || position === null) return;
      player.currentTime = position;
      latest.current = { ...latest.current, position };
    });

    return () => {
      cancelled = true;
    };
  }, [player, type, targetId]);

  // ------------------------------------------------------------- запись
  useEventListener(player, 'timeUpdate', ({ currentTime }) => {
    if (targetId === null) return;

    // `duration` появляется только после загрузки метаданных, до этого
    // это ноль — не длительность, а «ещё не знаю».
    const duration = player.duration > 0 ? player.duration : null;
    latest.current = { position: currentTime, duration };

    // ⚠️ Сравнение по МОДУЛЮ: человек мог перемотать назад, и тогда
    // разница отрицательная. Без модуля после каждой перемотки назад
    // сохранение на сервер вставало до тех пор, пока он не досмотрит
    // до прежнего места.
    const toServer =
      Math.abs(currentTime - lastServerSave.current) >= SERVER_SAVE_SECONDS;
    if (toServer) lastServerSave.current = currentTime;

    void persist(type, targetId, currentTime, duration, quality, toServer);
  });

  /**
   * Пауза — самый вероятный момент ухода с экрана.
   *
   * ⚠️ Отправляем СРАЗУ, не дожидаясь следующего тика: человек может
   * закрыть приложение через секунду, и тик уже не наступит.
   */
  useEventListener(player, 'playingChange', ({ isPlaying }) => {
    if (isPlaying || targetId === null) return;
    const { position, duration } = latest.current;
    if (position <= 0) return;

    lastServerSave.current = position;
    void persist(type, targetId, position, duration, quality, true);
  });

  // --------------------------------------------------------- закрытие
  useEffect(() => {
    return () => {
      if (targetId === null) return;
      const { position, duration } = latest.current;

      // ⚠️ Нулевая позиция НЕ сохраняется: видео могли открыть и сразу
      // закрыть, не начав смотреть. Запись затёрла бы настоящую
      // позицию с прошлого раза нулём — и «продолжить» отправляло бы
      // человека в начало.
      if (position <= 0) return;

      void persist(type, targetId, position, duration, quality, true);
    };
  }, [type, targetId, quality]);
}
