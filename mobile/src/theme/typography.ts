import ManropeBold from '../../assets/fonts/Manrope-Bold.ttf';
import ManropeExtraBold from '../../assets/fonts/Manrope-ExtraBold.ttf';
import ManropeMedium from '../../assets/fonts/Manrope-Medium.ttf';
import ManropeRegular from '../../assets/fonts/Manrope-Regular.ttf';
import ManropeSemiBold from '../../assets/fonts/Manrope-SemiBold.ttf';

/**
 * Шрифт приложения — Manrope.
 *
 * Заказчик (14.09.2026): «shriftlarni ham boshqa qilib almashtirishimiz
 * kerak». До этого весь интерфейс шёл системным шрифтом: на Android это
 * Roboto, на iOS — SF Pro, то есть приложение выглядело по-разному на двух
 * телефонах и никак — фирменно. Manrope выбран заказчиком из трёх
 * вариантов.
 *
 * <h2>Почему пять файлов, а не один</h2>
 * У Manrope есть variable-версия (один файл на все насыщенности), но
 * React Native её не поддерживает: на Android она грузится как одно
 * начертание, и «жирный» текст становится обычным. Поэтому берутся
 * статические срезы — по файлу на насыщенность.
 *
 * <h2>⚠️ Начертание задаётся СЕМЕЙСТВОМ, а не `fontWeight`</h2>
 * Каждый файл зарегистрирован своим именем (`Manrope-Bold` и т.д.), и
 * выбирать между ними надо именно именем. `fontWeight` рядом с таким
 * именем ломает подбор: iOS начинает искать в семействе ближайший вес и
 * может взять другой файл, Android — дорисовывает поддельную жирность
 * поверх уже жирного начертания.
 *
 * Поэтому:
 *   - `tailwind.config.js` раздаёт `font-family` и НЕ раздаёт `font-weight`
 *     (утилиты `fontWeight` там отключены и заменены своими);
 *   - в `style={{...}}` пишем `fontFamily: fonts.bold`, а не
 *     `fontWeight: '700'`.
 *
 * <h2>Чего в шрифте нет</h2>
 * Знаков `ʻ` (U+02BB) и `ʼ` (U+02BC) — настоящих узбекских апострофов.
 * Их нет и в исходной Manrope от Google, не только в нашей вырезке. В
 * текстах приложения и переводах везде стоит обычный `'` (U+0027), он в
 * шрифте есть; если такой знак придёт из админки, система подставит его
 * из системного шрифта — буква будет видна, просто чуть другой формы.
 *
 * Кириллица (русский интерфейс) в файлы включена — проверено по таблице
 * `cmap` при скачивании.
 */
export const fonts = {
  regular: 'Manrope-Regular',
  medium: 'Manrope-Medium',
  semibold: 'Manrope-SemiBold',
  bold: 'Manrope-Bold',
  extrabold: 'Manrope-ExtraBold',
} as const;

/**
 * Карта для `useFonts` в корневой раскладке.
 *
 * Ключ — это и есть имя семейства в стилях. Названия должны совпадать с
 * тем, что раздаёт `tailwind.config.js`.
 */
export const FONT_ASSETS = {
  [fonts.regular]: ManropeRegular,
  [fonts.medium]: ManropeMedium,
  [fonts.semibold]: ManropeSemiBold,
  [fonts.bold]: ManropeBold,
  [fonts.extrabold]: ManropeExtraBold,
};
