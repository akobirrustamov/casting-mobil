import type { CategoryGlyph } from '@/components/ui/CategoryTile';

/**
 * Знак раздела каталога контента — по `slug` категории.
 *
 * <h2>Почему по slug, а не из админки</h2>
 * В админке у категории есть только изображение (`iconMediaId`), и это
 * не иконка, а картинка размером с постер — в плитке она нечитаема
 * (разбор — в `components/ui/CategoryTile`). Поля «выбери знак» там нет.
 *
 * `slug` для этого подходит: он задаётся один раз при создании категории
 * и не переводится, в отличие от названия. По названию сопоставлять
 * нельзя — на трёх языках это три разные строки.
 *
 * <h2>Что будет с новой категорией</h2>
 * Админ заведёт раздел, которого здесь нет, — плитка получит запасной
 * знак и останется читаемой. Ничего не сломается и релиз не потребуется:
 * подобрать точный знак можно потом.
 */
const BY_SLUG: Record<string, CategoryGlyph> = {
  drama: 'drama-masks',
  comedy: 'emoticon-excited-outline',
  uzbek: 'flag-variant-outline',
  foreign: 'earth',
  kids: 'teddy-bear',
  documentary: 'book-open-page-variant-outline',
  romance: 'heart-outline',
};

/** Запасной знак — нейтральная «кинематографическая» метка. */
const FALLBACK: CategoryGlyph = 'movie-open-outline';

export function categoryIcon(slug: string | null | undefined): CategoryGlyph {
  if (!slug) return FALLBACK;
  return BY_SLUG[slug] ?? FALLBACK;
}
