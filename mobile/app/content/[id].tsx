import { useLocalSearchParams } from 'expo-router';

import { ContentScreen } from '@/features/content/ContentScreen';

/**
 * Экран 17 — карточка контента.
 *
 * <h2>Почему здесь больше нет развилки «фильм или сериал»</h2>
 * Раньше экран сначала спрашивал `/watch`, и если сервер отвечал
 * «контент многосерийный», сразу подменял себя списком серий. То есть у
 * сериала карточки не было вовсе: ни описания, ни актёров, ни донатов.
 *
 * На макете заказчика (08.09.2026) страница у фильма и у сериала ОДНА, а
 * различается только кнопка: у фильма она включает плеер, у сериала ведёт
 * в список серий (`/episodes/{id}`). Развилка переехала внутрь
 * {@link ContentScreen} — туда, где известен `structureType`.
 */
export default function ContentRoute() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const parsed = Number(id);
  const contentId = Number.isFinite(parsed) ? parsed : null;

  return <ContentScreen contentId={contentId} />;
}
