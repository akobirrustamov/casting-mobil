import { useLocalSearchParams } from 'expo-router';

import { useWatchEpisode } from '@/features/watch/api';
import { WatchDetail } from '@/features/watch/WatchDetail';

/**
 * Отдельная серия.
 *
 * Сюда ведут баннеры с `internalTargetType: EPISODE` — списка серий в
 * приложении пока нет, других входов тоже. Экран тот же, что у контента:
 * право доступа и там и там считает `AccessService`.
 */
export default function EpisodeScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const parsed = Number(id);
  const query = useWatchEpisode(Number.isFinite(parsed) ? parsed : null);

  return <WatchDetail query={query} />;
}
