import { useLocalSearchParams } from 'expo-router';

import { ContentIsMultiPartError, useWatchContent } from '@/features/watch/api';
import { useEpisodes } from '@/features/watch/episodes';
import { EpisodeListScreen } from '@/features/watch/EpisodeList';
import { WatchDetail } from '@/features/watch/WatchDetail';

/**
 * Экран 17 — контент.
 *
 * <h2>Почему структура выясняется запросом</h2>
 * По карточке главной фильм от сериала не отличить: `structureType` фид не
 * отдаёт, а `contentType` о структуре не говорит (ТЗ §13, §14 — «шоу» бывает
 * и цельным, и эпизодическим). Поэтому сначала спрашиваем «можно ли смотреть»,
 * и если сервер отвечает «контент многосерийный», показываем список серий.
 *
 * Второй запрос уходит только в этом случае — фильму список серий не нужен.
 */
export default function ContentScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const parsed = Number(id);
  const contentId = Number.isFinite(parsed) ? parsed : null;

  const watch = useWatchContent(contentId);
  const isMultiPart = watch.error instanceof ContentIsMultiPartError;

  const episodes = useEpisodes(isMultiPart ? contentId : null);

  if (isMultiPart) {
    return <EpisodeListScreen contentId={contentId} query={episodes} />;
  }

  return <WatchDetail query={watch} />;
}
