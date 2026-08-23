import { useLocalSearchParams } from 'expo-router';

import { useWatchContent } from '@/features/watch/api';
import { WatchDetail } from '@/features/watch/WatchDetail';

/**
 * Экран 17 — цельный контент: фильм, короткометражка, клип, выпуск шоу.
 *
 * Многосерийный контент сюда тоже попадает — с главной по типу не отличить,
 * `structureType` фид не отдаёт. Сервер в этом случае отвечает «спрашивай
 * серию», и экран честно об этом говорит (см. `ContentIsMultiPartError`).
 */
export default function ContentScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const parsed = Number(id);
  const query = useWatchContent(Number.isFinite(parsed) ? parsed : null);

  return <WatchDetail query={query} />;
}
