import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import axios from 'axios';

import { adminApi } from './client';

/**
 * Анкеты кастинга для админа — старые эндпоинты `/api/v1/casting-user/*`,
 * теперь закрытые для всех, кроме сотрудников.
 *
 * Поведение повторяет старую админку сайта
 * (`frontend/src/bot-admin/admin/CastingUserDetail.js`), включая порядок
 * запросов при одобрении — см. `approveApplication`.
 */

/** Как лежит в базе: 0 — новая, 1 — принята, 2 — отклонена. */
export type AdminStatus = 'new' | 'accepted' | 'rejected';

export type AdminPhoto = { id: string; isWebShow: boolean };

/** Анкета целиком — с персональными данными. Только для админки. */
export type AdminCastingUser = {
  id: number;
  castingType: string;
  gender: string | null;
  name: string;
  region: string | null;
  nationality: string | null;
  birthday: string | null;
  age: number | null;
  height: number | null;
  hairColor: string | null;
  eyeColor: string | null;
  clothSize: string | null;
  shoeSize: string | null;
  bust: string | null;
  waist: string | null;
  son: string | null;
  email: string | null;
  phone: string | null;
  telegram: string | null;
  facebook: string | null;
  instagram: string | null;
  price: number | null;
  createdAt: string | null;
  status: AdminStatus;
  /** Отметка «оплатил» из старой админки (`secondChan === 1`). */
  paid: boolean;
  isWebShow: boolean;
  photos: AdminPhoto[];
};

export function toAdminStatus(raw: unknown): AdminStatus {
  if (raw === 1 || raw === '1') return 'accepted';
  if (raw === 2 || raw === '2') return 'rejected';
  // `null` у старых анкет бота — это «новая», как и 0.
  return 'new';
}

function str(v: unknown): string | null {
  return typeof v === 'string' && v.trim() !== '' ? v : null;
}

function num(v: unknown): number | null {
  return typeof v === 'number' && Number.isFinite(v) ? v : null;
}

export function mapAdminUser(raw: Record<string, unknown>): AdminCastingUser {
  const photos = Array.isArray(raw.photos) ? raw.photos : [];
  return {
    id: Number(raw.id),
    castingType: String(raw.castingType ?? '').toLowerCase(),
    gender: str(raw.gender),
    name: String(raw.name ?? ''),
    region: str(raw.region),
    nationality: str(raw.nationality),
    birthday: str(raw.birthday),
    age: num(raw.age),
    height: num(raw.height),
    hairColor: str(raw.hairColor),
    eyeColor: str(raw.eyeColor),
    clothSize: str(raw.clothSize),
    shoeSize: str(raw.shoeSize),
    bust: str(raw.bust),
    waist: str(raw.waist),
    son: str(raw.son),
    email: str(raw.email),
    phone: str(raw.phone),
    telegram: str(raw.telegram),
    facebook: str(raw.facebook),
    instagram: str(raw.instagram),
    price: num(raw.price),
    createdAt: str(raw.createdAt),
    status: toAdminStatus(raw.status),
    paid: raw.secondChan === 1,
    isWebShow: raw.isWebShow === true,
    photos: photos
      .filter((p): p is { id: string; isWebShow?: boolean } => !!p && typeof (p as { id?: unknown }).id === 'string')
      .map((p) => ({ id: p.id, isWebShow: p.isWebShow === true })),
  };
}

// ─── Список и фильтр ─────────────────────────────────────────────────

/**
 * Поиск по имени и телефону.
 *
 * ⚠️ Телефон сравниваем ПО ЦИФРАМ: в базе он записан как придётся
 * («+998 90 123-45-67», «901234567»), а админ набирает кусок номера
 * без пробелов. Сравнение строк как есть не нашло бы половину анкет.
 */
export function filterAdminList(list: AdminCastingUser[], tab: AdminStatus, query: string): AdminCastingUser[] {
  const q = query.trim().toLowerCase();
  const qDigits = q.replace(/\D/g, '');

  return list.filter((u) => {
    if (u.status !== tab) return false;
    if (!q) return true;
    if (u.name.toLowerCase().includes(q)) return true;
    // Минимум три цифры: по одной-двум совпадёт половина базы.
    return qDigits.length >= 3 && (u.phone ?? '').replace(/\D/g, '').includes(qDigits);
  });
}

const ROOT_KEY = ['castingAdmin'] as const;
const LIST_KEY = [...ROOT_KEY, 'list'] as const;
const detailKey = (id: number) => [...ROOT_KEY, 'detail', id] as const;

export async function fetchAdminList(): Promise<AdminCastingUser[]> {
  const { data } = await adminApi.get<unknown>('/api/v1/casting-user');
  if (!Array.isArray(data)) return [];
  return data
    .filter((r): r is Record<string, unknown> => !!r && typeof r === 'object')
    .map(mapAdminUser);
}

/** @returns `null` — анкеты нет (404 приходит с пустым телом). */
export async function fetchAdminDetail(id: number): Promise<AdminCastingUser | null> {
  try {
    const { data } = await adminApi.get<unknown>(`/api/v1/casting-user/appeal/${id}`);
    if (!data || typeof data !== 'object') return null;
    return mapAdminUser(data as Record<string, unknown>);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) return null;
    throw error;
  }
}

/**
 * @param enabled только с живой сессией админа — иначе 401 и лишняя
 *        попытка продления
 */
export function useAdminList(enabled: boolean) {
  return useQuery({ queryKey: LIST_KEY, queryFn: fetchAdminList, enabled });
}

export function useAdminDetail(id: number | null, enabled: boolean) {
  return useQuery({
    queryKey: detailKey(id ?? -1),
    queryFn: () => fetchAdminDetail(id as number),
    enabled: enabled && id !== null,
  });
}

// ─── Действия ────────────────────────────────────────────────────────

/**
 * Цена в сумах из поля ввода.
 *
 * Только целое положительное число: тийины в ценах платформы не
 * используются (`lib/money`), а «0» или пустое поле — это не цена, а
 * забытый ввод. Старая админка ловила это `alert("Iltimos, narx kiriting!")`.
 *
 * Пробелы допускаем: «150 000» админ наберёт именно так.
 *
 * @returns `null` — ввод не цена
 */
export const MAX_PRICE = 1_000_000_000;

export function parsePrice(text: string): number | null {
  const cleaned = text.replace(/[\s ]/g, '');
  if (!/^\d+$/.test(cleaned)) return null;
  const value = Number(cleaned);
  if (!Number.isSafeInteger(value) || value <= 0 || value > MAX_PRICE) return null;
  return value;
}

/**
 * Одобрить с ценой.
 *
 * ⚠️ Порядок — СНАЧАЛА цена, ПОТОМ статус, как в старой админке.
 * Статус создаёт сообщение для бота (`Message`) и в тот же момент
 * делает заявку «одобренной» у кандидата. Если поставить статус
 * первым, а цена не дойдёт (обрыв связи между запросами), кандидат
 * увидит «принято» без суммы. В обратном порядке обрыв оставляет
 * заявку новой — админ просто нажмёт ещё раз.
 */
export async function approveApplication(id: number, price: number): Promise<void> {
  await adminApi.put(`/api/v1/casting-user/price/${id}/${price}`);
  await adminApi.put(`/api/v1/casting-user/status/${id}/1/${price}`);
}

/** Отклонить. Цена `0` — так же, как в старой админке. */
export async function rejectApplication(id: number): Promise<void> {
  await adminApi.put(`/api/v1/casting-user/status/${id}/2/0`);
}

/** Показать / скрыть анкету в публичном каталоге (переключатель на сервере). */
export async function toggleCatalog(id: number): Promise<void> {
  await adminApi.put(`/api/v1/casting-user/web-show/${id}`);
}

/**
 * Показать / скрыть одно фото в каталоге.
 *
 * Каталог (`/casting-user/web`) отдаёт только фото с `isWebShow`, поэтому
 * без этого переключателя анкета в каталоге могла бы быть совсем без фото.
 */
export async function togglePhoto(attachmentId: string): Promise<void> {
  await adminApi.put(`/api/v1/file/${attachmentId}`);
}

/**
 * После любого действия устаревает не только админка.
 *
 * `creators` — публичный каталог: видимость анкеты и её фото меняется
 * тут же. `casting/my` — заявки кандидата: если админ проверяет с того
 * же телефона, где подана заявка, статус должен обновиться без
 * перезапуска.
 */
function invalidateAll(client: QueryClient): void {
  void client.invalidateQueries({ queryKey: ROOT_KEY });
  void client.invalidateQueries({ queryKey: ['creators'] });
  void client.invalidateQueries({ queryKey: ['casting', 'my'] });
}

export function useApprove() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, price }: { id: number; price: number }) => approveApplication(id, price),
    onSettled: () => invalidateAll(client),
  });
}

export function useReject() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => rejectApplication(id),
    onSettled: () => invalidateAll(client),
  });
}

export function useToggleCatalog() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => toggleCatalog(id),
    onSettled: () => invalidateAll(client),
  });
}

export function useTogglePhoto() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (attachmentId: string) => togglePhoto(attachmentId),
    onSettled: () => invalidateAll(client),
  });
}

/** Ошибка действия → ключ перевода. */
export function adminActionErrorKey(error: unknown): string {
  if (error instanceof Error && error.message.startsWith('[READ_ONLY]')) {
    return 'castingAdmin.errors.readOnly';
  }
  if (axios.isAxiosError(error)) {
    if (!error.response) return 'castingAdmin.errors.network';
    if (error.response.status === 401) return 'castingAdmin.errors.expired';
    if (error.response.status === 403) return 'castingAdmin.errors.forbidden';
    if (error.response.status === 404) return 'castingAdmin.errors.notFound';
  }
  return 'castingAdmin.errors.unknown';
}
