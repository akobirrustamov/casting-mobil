/**
 * Модель кастинг-пользователя с боевого API (`GET /api/v1/casting-user/web`).
 *
 * ВАЖНО: этот эндпоинт отдаёт ещё и персональные данные — phone, email,
 * telegram, точную дату рождения, замеры фигуры. В мобильное приложение
 * их НЕ тянем: в типе ниже их намеренно нет. Подробнее — docs/API.md.
 */

/** Значения из фильтра на сайте (frontend/src/pages/models/Models.js). */
export type CastingType =
  | 'model'
  | 'euromodel'
  | 'bloger'
  | 'actor'
  | 'extra'
  | 'influencer';

export type Gender = 'male' | 'female';

export type CreatorPhoto = {
  id: string;
  isWebShow: boolean;
};

/** Сырой ответ API — перечислены только используемые поля. */
export type CastingUserDto = {
  id: number;
  name: string;
  castingType: string;
  gender: Gender;
  region: string | null;
  nationality: string | null;
  age: number | null;
  birthday: string | null;
  height: number | null;
  hairColor: string | null;
  eyeColor: string | null;
  isWebShow: boolean;
  photos: CreatorPhoto[];
};

/** Что реально нужно приложению. */
export type Creator = {
  id: number;
  name: string;
  castingType: CastingType | null;
  gender: Gender;
  region: string | null;
  age: number | null;
  height: number | null;
  /** Только фото с isWebShow — остальные показывать нельзя. */
  photoUrls: string[];
};
