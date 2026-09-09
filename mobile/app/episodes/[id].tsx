import { useLocalSearchParams } from 'expo-router';

import { useEpisodes } from '@/features/watch/episodes';
import { EpisodeListScreen } from '@/features/watch/EpisodeList';

/**
 * Список серий контента — макет заказчика «3» (08.09.2026).
 *
 * <h2>Почему отдельный маршрут, а не подмена экрана контента</h2>
 * Раньше список подменял собой карточку: сериал открывался сразу списком,
 * и вернуться к описанию было некуда. Теперь это шаг ВПЕРЁД от карточки —
 * значит и в истории переходов он должен быть отдельным шагом, чтобы
 * «назад» возвращало к описанию, а не выбрасывало на главную.
 *
 * ⚠️ Путь `/episodes/{contentId}`, а не `/content/{id}/episodes`: в
 * expo-router второе потребовало бы превратить `content/[id].tsx` в папку
 * с layout'ом — лишний уровень навигации ради одного экрана.
 *
 * ⚠️ Здесь ИДЕНТИФИКАТОР КОНТЕНТА, а не серии. Отдельная серия живёт по
 * `/episode/{episodeId}` — разница в одну букву, поэтому имя параметра
 * читается как contentId в самом коде ниже.
 */
export default function EpisodesRoute() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const parsed = Number(id);
  const contentId = Number.isFinite(parsed) ? parsed : null;

  const query = useEpisodes(contentId);

  return <EpisodeListScreen contentId={contentId} query={query} />;
}
