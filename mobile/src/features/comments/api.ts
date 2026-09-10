import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

import { useViewerKey } from '@/features/watch/api';
import { api } from '@/lib/api';

/**
 * Izohlar — `/api/v1/app/content/{id}/comments` (10.09.2026).
 *
 * Зеркало `AppCommentController` на бэкенде. При изменении DTO править
 * здесь же.
 *
 * <h2>Кто что может</h2>
 * <pre>
 *   читать    — все, и гость тоже (как счётчик на плитке)
 *   писать    — только вошедший; заблокированный получает 403;
 *               ОДИН комментарий на человека на фильм/сериал (409)
 *   удалять   — только своё; удаление мягкое, для модерации запись остаётся
 * </pre>
 */

/** Столько же, сколько колонка `cms_comment.text` на бэкенде. */
export const COMMENT_MAX_LENGTH = 2000;

/** Сколько комментариев в одной порции ленты. */
const PAGE_SIZE = 20;

export type AppComment = {
  id: number;
  text: string;
  /** Время сервера без пояса («2026-09-10T14:25:55.495») — см. `./time`. */
  createdAt: string | null;
  authorName: string | null;
  /** Готовый адрес картинки (Google), а не id медиа — как у донатчиков. */
  authorAvatarUrl: string | null;
  /** Свой комментарий: только у него есть «удалить». */
  mine: boolean;
  /**
   * Скрыт модератором. Такой приходит ТОЛЬКО автору — чтобы тот видел, что
   * комментарий скрыт, а не пропал, и не писал его заново.
   */
  hidden: boolean;
};

export type CommentPage = {
  items: AppComment[];
  page: number;
  totalItems: number;
  hasMore: boolean;
  /**
   * У зрителя уже есть комментарий к этому контенту — второй не пустят
   * (заказчик, 10.09.2026: «один человек — один комментарий»).
   *
   * ⚠️ Считает сервер, а не лента: своя запись может лежать на пятой
   * странице, которую человек ещё не листал.
   */
  alreadyCommented: boolean;
};

/**
 * Комментариев на этом сервере нет.
 *
 * ⚠️ 401 на чтение — тоже сюда. Список открыт всем (`permitAll`); 401
 * отвечает только сборка бэкенда без этого адреса: незнакомый путь
 * попадает под общее правило `/api/**`. Так вёл себя боевой сервер
 * 10.09.2026 с рейтингом донатов — гостю показалось бы «войдите», хотя
 * вход ничего бы не изменил.
 */
export class CommentsUnavailableError extends Error {
  constructor() {
    super('Комментарии недоступны на этом сервере');
    this.name = 'CommentsUnavailableError';
  }
}

/** Отказ сервера при отправке — с его собственным текстом. */
export class CommentRejectedError extends Error {
  constructor(
    readonly reason: 'blocked' | 'invalid' | 'signIn' | 'duplicate',
    message: string
  ) {
    super(message);
    this.name = 'CommentRejectedError';
  }
}

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function str(value: unknown): string | null {
  return typeof value === 'string' && value.trim() !== '' ? value : null;
}

/** ⚠️ Экспортируется ради теста. Без id и текста комментарий не нарисовать. */
export function mapComment(raw: unknown): AppComment | null {
  const r = raw as Record<string, unknown> | null;
  const id = num(r?.id);
  const text = str(r?.text);
  if (!r || id === null || text === null) return null;

  return {
    id,
    text,
    createdAt: str(r.createdAt),
    authorName: str(r.authorName),
    authorAvatarUrl: str(r.authorAvatarUrl),
    mine: r.mine === true,
    hidden: r.hidden === true,
  };
}

/** ⚠️ Экспортируется ради теста — см. `mapComment`. */
export function mapCommentPage(raw: unknown): CommentPage {
  const r = raw as Record<string, unknown> | null;

  // Старая сборка отдаёт на незнакомый адрес index.html со статусом 200 —
  // без этой проверки экран молча показал бы «комментариев нет».
  if (!r || typeof r !== 'object' || !Array.isArray(r.items)) {
    throw new CommentsUnavailableError();
  }

  return {
    items: r.items.map(mapComment).filter((c): c is AppComment => c !== null),
    page: num(r.page) ?? 0,
    totalItems: num(r.totalItems) ?? 0,
    hasMore: r.hasMore === true,
    alreadyCommented: r.alreadyCommented === true,
  };
}

async function fetchComments(contentId: number, page: number): Promise<CommentPage> {
  try {
    const { data } = await api.get<unknown>(`/api/v1/app/content/${contentId}/comments`, {
      params: { page, size: PAGE_SIZE },
    });
    return mapCommentPage(data);
  } catch (error) {
    if (
      axios.isAxiosError(error) &&
      (error.response?.status === 404 || error.response?.status === 401)
    ) {
      throw new CommentsUnavailableError();
    }
    throw error;
  }
}

/** Текст сервера из ответа с ошибкой, если он есть. */
function serverMessage(error: unknown): string | null {
  if (!axios.isAxiosError(error)) return null;
  const data = error.response?.data as { message?: unknown } | undefined;
  return str(data?.message);
}

export async function postComment(contentId: number, text: string): Promise<AppComment> {
  try {
    const { data } = await api.post<unknown>(
      `/api/v1/app/content/${contentId}/comments`,
      {
        text,
      }
    );
    const comment = mapComment(data);
    if (!comment) throw new CommentsUnavailableError();
    return comment;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const status = error.response.status;
      const message = serverMessage(error) ?? '';
      if (status === 401) throw new CommentRejectedError('signIn', message);
      if (status === 403) throw new CommentRejectedError('blocked', message);
      if (status === 409) throw new CommentRejectedError('duplicate', message);
      if (status === 400 || status === 422)
        throw new CommentRejectedError('invalid', message);
      // ⚠️ 405 — тоже «адреса нет». Старая сборка отдаёт на незнакомые GET
      // страницу сайта, а на POST — «метод не поддерживается». Без этой
      // строки человек видел бы «не отправилось, попробуйте ещё раз» и
      // пробовал бы до бесконечности.
      if (status === 404 || status === 405) throw new CommentsUnavailableError();
    }
    throw error;
  }
}

export async function deleteComment(commentId: number): Promise<void> {
  await api.delete(`/api/v1/app/comments/${commentId}`);
}

/**
 * Лента комментариев, порциями по мере прокрутки.
 *
 * <h2>Почему зритель в ключе кэша</h2>
 * В ответе есть `mine` и скрытые модератором СВОИ комментарии — это про
 * конкретного человека. Без зрителя в ключе после входа кнопки «удалить»
 * у своих комментариев не появлялось бы: показывался бы ответ гостя.
 */
export function useComments(contentId: number | null) {
  const viewer = useViewerKey();

  return useInfiniteQuery({
    queryKey: ['comments', contentId, viewer],
    queryFn: ({ pageParam }) => fetchComments(contentId as number, pageParam),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasMore ? last.page + 1 : undefined),
    enabled: contentId !== null,
    retry: (failureCount, error) =>
      !(error instanceof CommentsUnavailableError) && failureCount < 2,
  });
}

/**
 * После записи и удаления обновляются ДВА места: сама лента и карточка
 * контента — на плитке «Izohlar» стоит счётчик, и он не должен отставать
 * от ленты, которую человек только что видел.
 */
function useRefreshAfterWrite(contentId: number | null) {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['comments', contentId] });
    void queryClient.invalidateQueries({ queryKey: ['content-detail', contentId] });
  };
}

export function usePostComment(contentId: number | null) {
  const refresh = useRefreshAfterWrite(contentId);
  return useMutation({
    mutationFn: (text: string) => postComment(contentId as number, text),
    onSuccess: refresh,
  });
}

export function useDeleteComment(contentId: number | null) {
  const refresh = useRefreshAfterWrite(contentId);
  return useMutation({
    mutationFn: (commentId: number) => deleteComment(commentId),
    onSuccess: refresh,
  });
}
