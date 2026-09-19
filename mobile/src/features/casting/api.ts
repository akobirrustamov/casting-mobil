import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

import { api } from '@/lib/api';

import type { ApplicationPayload } from './form';
import { normalizeStatus, type MyApplication } from './status';

/**
 * Заявка кандидата на кастинг — новое пространство `/api/v1/app/casting`.
 *
 * <h2>Почему не старый `POST /api/v1/casting-user`</h2>
 * Тот эндпоинт — анонимный вход Telegram-бота: анкету может прислать
 * кто угодно, от чьего угодно имени, и связать её с аккаунтом нельзя.
 * Новый требует токен пользователя, поэтому «мои заявки» — это заявки
 * именно этого человека, а не всех, кто знает его telegramId.
 *
 * <h2>⚠️ READ_ONLY</h2>
 * Эти адреса НЕ добавлены в `WRITE_ALLOWLIST` (`lib/api`): заявка — это
 * запись в живую базу кастинга. В сборках с `EXPO_PUBLIC_READ_ONLY=false`
 * (все профили `eas.json` и `.env` стенда) отправка работает; в сборке
 * без этой переменной экран честно скажет, что отправка отключена.
 */

/** Коды отказа, которые экран различает. */
export type CastingErrorCode =
  | 'CASTING_APPLICATION_PENDING'
  | 'CASTING_PHOTO_IN_USE'
  | 'CASTING_PHOTO_NOT_FOUND'
  | 'VALIDATION_ERROR'
  | 'UNAUTHORIZED'
  | 'NETWORK'
  | 'READ_ONLY'
  | 'UNKNOWN';

export class CastingError extends Error {
  constructor(
    public readonly code: CastingErrorCode,
    /** Текст сервера — у валидации он конкретнее нашего общего. */
    public readonly serverMessage: string | null = null,
    /** Ошибки по полям из `VALIDATION_ERROR.errors[]`. */
    public readonly fieldErrors: Record<string, string> = {},
  ) {
    super(code);
    this.name = 'CastingError';
  }
}

const KNOWN_CODES: CastingErrorCode[] = [
  'CASTING_APPLICATION_PENDING',
  'CASTING_PHOTO_IN_USE',
  'CASTING_PHOTO_NOT_FOUND',
  'VALIDATION_ERROR',
];

type ApiErrorBody = {
  code?: string;
  message?: string;
  errors?: { field?: string; message?: string }[];
};

/**
 * Любая ошибка запроса → `CastingError`.
 *
 * ⚠️ Ветвимся по `code`, а не по статусу: валидация приходит как 422,
 * «уже есть заявка» — 409, но 409 бывает и у «фото уже занято». Статус
 * один, причины и действия человека — разные.
 */
export function toCastingError(error: unknown): CastingError {
  if (error instanceof CastingError) return error;

  // Интерцептор `READ_ONLY` роняет запрос ДО отправки обычной ошибкой.
  if (error instanceof Error && error.message.startsWith('[READ_ONLY]')) {
    return new CastingError('READ_ONLY');
  }

  if (axios.isAxiosError(error)) {
    if (!error.response) return new CastingError('NETWORK');

    const body = (error.response.data ?? {}) as ApiErrorBody;
    const fieldErrors: Record<string, string> = {};
    for (const e of body.errors ?? []) {
      if (e?.field && e.message) fieldErrors[e.field] = e.message;
    }

    if (body.code && (KNOWN_CODES as string[]).includes(body.code)) {
      return new CastingError(body.code as CastingErrorCode, body.message ?? null, fieldErrors);
    }
    if (error.response.status === 401) return new CastingError('UNAUTHORIZED');

    return new CastingError('UNKNOWN', body.message ?? null, fieldErrors);
  }

  return new CastingError('UNKNOWN');
}

const MESSAGE_KEY: Record<CastingErrorCode, string> = {
  CASTING_APPLICATION_PENDING: 'casting.errors.pending',
  CASTING_PHOTO_IN_USE: 'casting.errors.photoInUse',
  CASTING_PHOTO_NOT_FOUND: 'casting.errors.photoNotFound',
  VALIDATION_ERROR: 'casting.errors.validation',
  UNAUTHORIZED: 'casting.errors.unauthorized',
  NETWORK: 'casting.errors.network',
  READ_ONLY: 'casting.errors.readOnly',
  UNKNOWN: 'casting.errors.unknown',
};

export function castingErrorKey(error: unknown): string {
  return MESSAGE_KEY[toCastingError(error).code];
}

// ─── Фото ────────────────────────────────────────────────────────────

export type PickedPhoto = {
  uri: string;
  fileName?: string | null;
  mimeType?: string | null;
};

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Загрузить одно фото — `POST /api/v1/file/upload` (открытый эндпоинт).
 *
 * Ответ — голый UUID строкой JSON (`"3f2c…"`), без обёртки.
 *
 * ⚠️ Форма ответа проверяется: старая сборка бэкенда на незнакомый
 * адрес отвечает `index.html` со статусом 200, и без проверки в заявку
 * ушёл бы кусок HTML вместо id.
 *
 * ⚠️ `transformRequest` возвращает тело как есть: иначе axios попытается
 * сериализовать `FormData` сам, а на React Native это ломает multipart —
 * границу частей должна проставить сетевая подсистема телефона.
 */
export async function uploadCastingPhoto(photo: PickedPhoto): Promise<string> {
  const mimeType = photo.mimeType || 'image/jpeg';
  const ext = mimeType.split('/')[1] || 'jpg';
  const name = photo.fileName || `photo_${Date.now()}.${ext}`;

  const form = new FormData();
  // RN принимает в FormData объект {uri, name, type} — это и есть файл.
  form.append('photo', { uri: photo.uri, name, type: mimeType } as unknown as Blob);
  form.append('prefix', '/casting-app');

  try {
    const { data } = await api.post<unknown>('/api/v1/file/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      transformRequest: (body) => body,
      // Фото с камеры — несколько мегабайт; общий таймаут 15 с на
      // мобильной сети обрывал бы загрузку на середине.
      timeout: 60_000,
    });

    const id = typeof data === 'string' ? data.trim() : '';
    if (!UUID_RE.test(id)) throw new CastingError('UNKNOWN');
    return id;
  } catch (error) {
    throw toCastingError(error);
  }
}

// ─── Заявки ──────────────────────────────────────────────────────────

const MY_KEY = ['casting', 'my'] as const;

function mapApplication(raw: Partial<MyApplication> & { id: number }): MyApplication {
  return {
    id: raw.id,
    castingType: String(raw.castingType ?? ''),
    name: String(raw.name ?? ''),
    status: normalizeStatus(raw.status),
    price: typeof raw.price === 'number' ? raw.price : null,
    paid: raw.paid === true,
    isWebShow: raw.isWebShow === true,
    createdAt: String(raw.createdAt ?? ''),
    photos: Array.isArray(raw.photos) ? raw.photos.filter((p): p is string => typeof p === 'string') : [],
  };
}

export async function fetchMyApplications(): Promise<MyApplication[]> {
  const { data } = await api.get<unknown>('/api/v1/app/casting/applications/my');
  if (!Array.isArray(data)) return [];
  return data
    .filter((a): a is MyApplication => a !== null && typeof a === 'object' && typeof a.id === 'number')
    .map(mapApplication);
}

/**
 * @param userId `null` — не вошёл: без токена ответ — 401, и интерцептор
 *        зря пытался бы продлить несуществующую сессию.
 *
 * ⚠️ id в ключе кэша — не украшение: иначе после смены аккаунта на этом
 * телефоне следующий человек на секунду увидел бы чужие заявки.
 */
export function useMyApplications(userId: string | null) {
  return useQuery({
    queryKey: [...MY_KEY, userId],
    queryFn: fetchMyApplications,
    enabled: userId !== null,
  });
}

export async function submitApplication(payload: ApplicationPayload): Promise<MyApplication> {
  try {
    const { data } = await api.post<MyApplication>('/api/v1/app/casting/applications', payload);
    return mapApplication(data);
  } catch (error) {
    throw toCastingError(error);
  }
}

export function useSubmitApplication() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: submitApplication,
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: MY_KEY });
    },
    // 409 «уже есть заявка» тоже значит, что список на экране устарел.
    onError: () => {
      void client.invalidateQueries({ queryKey: MY_KEY });
    },
  });
}
