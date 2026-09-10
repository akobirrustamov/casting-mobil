import { useLocalSearchParams } from 'expo-router';

import { CommentsScreen } from '@/features/comments/CommentsScreen';

/**
 * Izohlar контента — `/comments/{contentId}`.
 *
 * Сюда ведут плитка «Izohlar» на странице контента и облачко на кадре
 * плеера. ⚠️ Здесь идентификатор КОНТЕНТА, не комментария.
 */
export default function CommentsRoute() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const parsed = Number(id);
  const contentId = Number.isFinite(parsed) ? parsed : null;

  return <CommentsScreen contentId={contentId} />;
}
