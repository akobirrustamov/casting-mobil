import { useWindowDimensions } from 'react-native';

/**
 * Ширина карточки в горизонтальном ряду.
 *
 * <h2>Требование</h2>
 * Заказчик (01.09.2026): «kamida 3ta card ko'rinishi majburiy». То есть
 * это правило ВСЕГО экрана, а не одного блока: ряды фида («Yangi
 * premyeralar», «Mini seriallar») и ряды категорий стоят друг под другом,
 * и разная ширина карточки между ними читалась бы как поломка.
 *
 * <h2>Почему не число в `PosterCard`</h2>
 * По умолчанию карточка была 132px. На телефоне шириной 393dp это
 * `132*3 + 24 = 420` при доступных `393 - 32 = 361` — в кадр влезало две с
 * половиной. Фиксированное число не может выполнить условие «ровно три»:
 * оно верно только для одной ширины экрана.
 */

/** Горизонтальные отступы экрана — `px-4` у `Screen`. */
const SCREEN_PADDING = 16;

/** Зазор между карточками — `gap-3` у `Rail`. */
const RAIL_GAP = 12;

/** Сколько карточек обязано помещаться в кадр. */
const VISIBLE_CARDS = 3;

/**
 * Сколько «подглядывает» следующая карточка.
 *
 * Если три карточки заполняют кадр ровно до края, ряд выглядит
 * законченным и вбок его никто не потянет — остальные семь карточек
 * человек просто не увидит. Полоска следующей — единственное, что
 * говорит «здесь есть продолжение».
 */
const PEEK = 18;

/**
 * Пропорция карточки — ОДНА на всё приложение.
 *
 * <h2>Почему не по формату контента</h2>
 * Раньше ряд из вертикального контента становился 9:16 (`rowRatio`), и
 * тогда высота карточки зависела от того, что в ряд попало. На экране это
 * выглядело так: нажимаешь «Barchasi ›» — и внутри карточки другой формы,
 * хотя контент тот же самый.
 *
 * Заказчик (01.09.2026): «barchasi qilib ichiga kirgandan song ham
 * cardlarni dizayni o'zgarmasin, barcha cardlarni image width height ni
 * bir xil qilib ber». Поэтому форма кадра теперь одна: 2:3, как в каталоге
 * и в мокапах.
 *
 * ⚠️ Плата за это: вертикальный ролик на карточке кадрируется до 2:3 —
 * в ряду «Reels seriallar» постер больше не повторяет форму видео. Сам
 * ПЛЕЕР по-прежнему рисует кадр по `orientation` (`frameRatio`), то есть
 * рилс открывается вертикальным.
 */
export const CARD_RATIO = 2 / 3;

/** Чистая функция — её удобно проверить тестом, не поднимая экран. */
export function railCardWidth(screenWidth: number): number {
  const visible = screenWidth - SCREEN_PADDING * 2;
  const gaps = RAIL_GAP * (VISIBLE_CARDS - 1);
  return Math.floor((visible - gaps - PEEK) / VISIBLE_CARDS);
}

/** То же, но от текущего экрана. Ширина меняется при повороте. */
export function useRailCardWidth(): number {
  const { width } = useWindowDimensions();
  return railCardWidth(width);
}

/**
 * Зазор в СЕТКЕ («Barchasi», «Media»).
 *
 * Карточка там той же ширины, что в ряду, — иначе, открыв «Barchasi ›»,
 * человек видел бы другие карточки. Но в сетке нет «подглядывающей»
 * четвёртой, и её место надо куда-то деть: остаток ширины уходит в зазоры,
 * тогда три колонки занимают строку целиком и справа не остаётся дыры.
 */
export function gridGap(screenWidth: number): number {
  const visible = screenWidth - SCREEN_PADDING * 2;
  const left = visible - railCardWidth(screenWidth) * VISIBLE_CARDS;
  return Math.max(0, Math.floor(left / (VISIBLE_CARDS - 1)));
}
