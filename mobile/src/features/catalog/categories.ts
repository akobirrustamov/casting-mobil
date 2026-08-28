import type { CategoryGlyph } from '@/components/ui/CategoryTile';
import { colors } from '@/theme/tokens';

/**
 * 10 направлений из ТЗ (V2, стр. 2 «10 асосий йўналиш»).
 * Порядок и состав менять нельзя — согласовано с заказчиком.
 *
 * `apiType` — соответствие боевому API сайта (`castingType`).
 * Совпали только 4 из 10, остальным в базе пока нет типа — см. docs/API.md.
 */
export type Category = {
  id: string;
  titleUz: string;
  titleRu: string;
  accent: string;
  /** Знак на плитке. Подобран по смыслу направления, не по позиции. */
  icon: CategoryGlyph;
  apiType: string | null;
};

export const CATEGORIES: Category[] = [
  { id: 'actors', titleUz: 'Aktyorlar', titleRu: 'Актёры', accent: colors.purple, icon: 'account-star-outline', apiType: 'actor' },
  { id: 'models', titleUz: 'Modellar', titleRu: 'Модели', accent: colors.magenta, icon: 'hanger', apiType: 'model' },
  { id: 'bloggers', titleUz: 'Blogerlar', titleRu: 'Блогеры', accent: colors.cyan, icon: 'video-outline', apiType: 'bloger' },
  { id: 'influencers', titleUz: 'Influencerlar', titleRu: 'Инфлюенсеры', accent: colors.gold, icon: 'bullhorn-variant-outline', apiType: 'influencer' },
  { id: 'musicians', titleUz: 'Musiqachilar', titleRu: 'Музыканты', accent: colors.purple, icon: 'music-clef-treble', apiType: null },
  { id: 'dancers', titleUz: 'Raqqoslar', titleRu: 'Танцоры', accent: colors.magenta, icon: 'dance-ballroom', apiType: null },
  { id: 'photovideo', titleUz: 'Foto/Video ijodkorlar', titleRu: 'Фото/Видео', accent: colors.cyan, icon: 'camera-outline', apiType: null },
  { id: 'styling', titleUz: 'Styling va loyihalar', titleRu: 'Стайлинг', accent: colors.gold, icon: 'palette-outline', apiType: null },
  { id: 'courses', titleUz: 'Kurslar va treninglar', titleRu: 'Курсы', accent: colors.purple, icon: 'school-outline', apiType: null },
  { id: 'casting', titleUz: "Casting e'lonlari", titleRu: 'Кастинги', accent: colors.magenta, icon: 'clipboard-text-outline', apiType: null },
];

/** Подписи типов из API, которых нет среди 10 направлений ТЗ. */
export const EXTRA_API_TYPES: Record<string, { uz: string; ru: string }> = {
  euromodel: { uz: 'Euromodel', ru: 'Евромодель' },
  extra: { uz: 'Massovka', ru: 'Массовка' },
};
