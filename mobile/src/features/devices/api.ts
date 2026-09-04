import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

import { api } from '@/lib/api';

import { deviceLabel, devicePlatform, installationId } from './installationId';

/**
 * Устройства аккаунта — `GET/POST/DELETE /api/v1/app/devices`.
 *
 * <h2>Что здесь чинится</h2>
 * Требование «не больше двух устройств на аккаунт» было записано в базе,
 * в настройке и в админке — и не работало, потому что приложение НИКОГДА
 * не сообщало о себе. Регистрация устройства и есть недостающее звено.
 *
 * <h2>Каждый ответ — ПОЛНЫЙ список</h2>
 * Регистрация, чтение и выход возвращают одно и то же: лимит и текущие
 * устройства. Клиенту не нужно пересчитывать список самому, и одна
 * потерянная сеткой ответная пачка не разведёт две стороны.
 */
export type Device = {
  id: number;
  name: string | null;
  platform: string | null;
  lastActiveAt: string | null;
  createdAt: string | null;
  /** Это устройство. Решает сервер — см. `lib/api` про `X-Device-Id`. */
  current: boolean;
};

export type DevicesResponse = {
  /** Сколько устройств разрешено. Приходит с сервера: настройка меняется в админке. */
  limit: number;
  devices: Device[];
};

const URL = '/api/v1/app/devices';

/**
 * Лимит исчерпан.
 *
 * ⚠️ Отдельный тип, а не «просто ошибка»: это единственный отказ, после
 * которого человеку есть что делать — открыть список и освободить место.
 * Показать здесь общее «Что-то пошло не так» значило бы оставить его
 * запертым снаружи собственного аккаунта без единой подсказки.
 */
export class DeviceLimitError extends Error {
  constructor(message?: string) {
    super(message ?? 'Qurilmalar chegarasi to‘ldi');
    this.name = 'DeviceLimitError';
  }
}

function mapDevice(raw: unknown): Device {
  const r = (raw ?? {}) as Record<string, unknown>;
  return {
    id: Number(r.id),
    name: typeof r.name === 'string' ? r.name : null,
    platform: typeof r.platform === 'string' ? r.platform : null,
    lastActiveAt: typeof r.lastActiveAt === 'string' ? r.lastActiveAt : null,
    createdAt: typeof r.createdAt === 'string' ? r.createdAt : null,
    current: r.current === true,
  };
}

function mapResponse(raw: unknown): DevicesResponse {
  const r = (raw ?? {}) as Record<string, unknown>;
  const list = Array.isArray(r.devices) ? r.devices : [];
  return {
    // Запасное значение — то же, что у бэкенда по умолчанию. Оно нужно
    // только на случай старой сборки сервера без этого поля.
    limit: typeof r.limit === 'number' ? r.limit : 2,
    devices: list.map(mapDevice),
  };
}

/** Отличает «мест нет» от любой другой неудачи. */
export function isDeviceLimit(error: unknown): boolean {
  if (error instanceof DeviceLimitError) return true;
  if (!axios.isAxiosError(error) || error.response?.status !== 409) return false;

  const code = (error.response.data as { code?: unknown } | undefined)?.code;
  return code === 'DEVICE_LIMIT_REACHED';
}

/**
 * Сообщить о себе.
 *
 * Вызывается после входа И при каждом запуске: первое применяет лимит,
 * второе обновляет «последнюю активность». Повтор безопасен — знакомое
 * устройство места не занимает.
 *
 * @throws DeviceLimitError мест нет — показать выбор устройства
 */
export async function registerDevice(): Promise<DevicesResponse> {
  try {
    const { data } = await api.post<unknown>(`${URL}/register`, {
      deviceId: await installationId(),
      name: deviceLabel(),
      platform: devicePlatform(),
    });
    return mapResponse(data);
  } catch (error) {
    if (isDeviceLimit(error)) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data as { message?: string } | undefined)?.message
        : undefined;
      throw new DeviceLimitError(message);
    }
    throw error;
  }
}

export async function fetchDevices(): Promise<DevicesResponse> {
  const { data } = await api.get<unknown>(URL);
  return mapResponse(data);
}

export async function revokeDevice(id: number): Promise<DevicesResponse> {
  const { data } = await api.delete<unknown>(`${URL}/${id}`);
  return mapResponse(data);
}

const KEY = ['devices'];

/**
 * ⚠️ `enabled` приходит снаружи, а не читается из `auth/store`.
 *
 * Иначе получалось кольцо импортов: `auth/store` регистрирует устройство
 * через `devices/store`, тот берёт запросы отсюда, а отсюда шла ссылка
 * обратно в `auth/store`. В Metro такое кольцо разрешается тем, что один
 * из модулей на миг оказывается пустым объектом — и падает не там, где
 * ошибка.
 */
export function useDevices(enabled = true) {
  return useQuery({
    queryKey: KEY,
    queryFn: fetchDevices,
    enabled,
    // Список — про безопасность: устаревший показывал бы уже выгнанное
    // устройство как активное.
    staleTime: 0,
  });
}

export function useRevokeDevice() {
  const client = useQueryClient();

  return useMutation({
    mutationFn: revokeDevice,
    // Ответ УЖЕ содержит новый список — второй запрос за ним не нужен.
    onSuccess: (data) => client.setQueryData(KEY, data),
  });
}
