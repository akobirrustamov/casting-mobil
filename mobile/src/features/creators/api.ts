import { useQuery } from '@tanstack/react-query';

import { api, fileUrl } from '@/lib/api';

import type { CastingType, CastingUserDto, Creator } from './types';

const CASTING_TYPES: CastingType[] = [
  'model',
  'euromodel',
  'bloger',
  'actor',
  'extra',
  'influencer',
];

function toCastingType(raw: string | null): CastingType | null {
  const value = (raw ?? '').toLowerCase() as CastingType;
  return CASTING_TYPES.includes(value) ? value : null;
}

/** Возраст из даты рождения — API его не всегда присылает. */
function calcAge(birthday: string | null): number | null {
  if (!birthday) return null;
  const born = new Date(birthday);
  if (Number.isNaN(born.getTime())) return null;

  const now = new Date();
  let age = now.getFullYear() - born.getFullYear();
  const monthDiff = now.getMonth() - born.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < born.getDate())) {
    age -= 1;
  }
  return age >= 0 && age <= 100 ? age : null;
}

function mapCreator(dto: CastingUserDto): Creator {
  return {
    id: dto.id,
    name: dto.name,
    castingType: toCastingType(dto.castingType),
    gender: dto.gender,
    region: dto.region,
    age: dto.age ?? calcAge(dto.birthday),
    height: dto.height,
    // Сайт показывает только isWebShow-фото. Повторяем то же правило.
    photoUrls: (dto.photos ?? []).filter((p) => p.isWebShow).map((p) => fileUrl(p.id)),
  };
}

async function fetchCreators(): Promise<Creator[]> {
  const { data } = await api.get<CastingUserDto[]>('/api/v1/casting-user/web');
  return (Array.isArray(data) ? data : []).map(mapCreator);
}

export function useCreators() {
  return useQuery({
    queryKey: ['creators'],
    queryFn: fetchCreators,
  });
}

/**
 * Один креатор.
 *
 * Берём из того же списка, а не отдельным запросом. Причины две:
 * эндпоинт `/casting-user/appeal/{id}` отдаёт полную анкету вместе
 * с персональными данными (телефон, email, замеры) — тянуть их в приложение
 * нельзя; и список уже в кэше, так что открытие профиля мгновенное.
 */
export function useCreator(id: number | null) {
  return useQuery({
    queryKey: ['creators'],
    queryFn: fetchCreators,
    enabled: id !== null,
    select: (list) => list.find((c) => c.id === id) ?? null,
  });
}

/** Только те, у кого есть хотя бы одно публичное фото — для витрин. */
export function withPhotos(creators: Creator[] | undefined): Creator[] {
  return (creators ?? []).filter((c) => c.photoUrls.length > 0);
}
