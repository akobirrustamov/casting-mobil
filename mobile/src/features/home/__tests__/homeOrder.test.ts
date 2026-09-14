import fs from 'fs';
import path from 'path';

/**
 * Порядок блоков на главной — решение заказчика, а не наша раскладка.
 *
 * <h2>Почему это тест, а не просто комментарий в коде</h2>
 * «Ko'rishda davom eting» полгода стоял ПЕРВЫМ, и довод был разумный:
 * кто не досмотрел — открывает приложение ради этого. 07.09.2026
 * заказчик прислал скриншот со стрелкой и написал: «buni eng oxiriga
 * qoyish kk categoriyalardan keyin castingdan oldin».
 *
 * ⚠️ Разумный довод и есть опасность: при следующем разборе главной
 * ряд легко «починят» обратно наверх — как более логичный, — и заказчик
 * увидит откат своего решения. Тест ставит его слово выше нашей логики.
 *
 * Проверяется исходник как текст: порядок JSX в этом файле и есть
 * порядок блоков на экране, а поднимать ради него весь экран с
 * навигацией и запросами — несоразмерно.
 */
const SCREEN = fs.readFileSync(
  path.join(__dirname, '../../../../app/(tabs)/index.tsx'),
  'utf8'
);

/**
 * Исходник БЕЗ закомментированных кусков.
 *
 * ⚠️ Иначе тест считает выключенный блок за живой. `at()` ищет по
 * тексту файла, а `{/* <Block /> *\/}` — это по-прежнему текст: порядок
 * «проверялся» бы по строке, которой на экране нет, и тест оставался
 * бы зелёным, ничего не проверяя.
 */
const ACTIVE = SCREEN
  .replace(/\{\/\*[\s\S]*?\*\/\}/g, '')
  .replace(/^\s*\/\/.*$/gm, '');

/** Позиция куска в файле; -1 превращается в понятный провал. */
function at(marker: string): number {
  const index = ACTIVE.indexOf(marker);
  expect(index).toBeGreaterThanOrEqual(0);
  return index;
}

/** Есть ли блок на экране (не закомментирован). */
function isActive(marker: string): boolean {
  return ACTIVE.includes(marker);
}

describe('главная: состав и порядок блоков', () => {
  it('«продолжить просмотр» стоит один раз', () => {
    const all = SCREEN.split('<ContinueRail />').length - 1;
    expect(all).toBe(1);
  });

  it('«продолжить просмотр» — ПОСЛЕ разделов каталога', () => {
    // ⚠️ `<CategoryRows />` временно отключён (09.09.2026). Пока его нет,
    // проверять его позицию нечего — но и молча пропускать нельзя:
    // порядок задал заказчик, и при возврате блока проверку надо вернуть.
    //
    // Поэтому условие перевёрнуто: когда блок снова появится, эта ветка
    // сработает сама. Никто не обязан помнить про этот тест.
    if (isActive('<CategoryRows />')) {
      expect(at('<ContinueRail />')).toBeGreaterThan(at('<CategoryRows />'));
    }

    expect(at('<HomeFeedBlock')).toBeLessThan(at('<ContinueRail />'));
  });

  it('«продолжить просмотр» — последний блок экрана', () => {
    // Дословно «buni eng oxiriga qoyish kk». Соседи, названные в той
    // просьбе (направления и кастинг), с экрана ушли 14.09.2026 — значит
    // «в конце» теперь проверяется буквально: после ряда нет ничего,
    // кроме закрывающего `</Screen>`.
    const after = at('<ContinueRail />') + '<ContinueRail />'.length;
    const tail = ACTIVE.slice(after, ACTIVE.indexOf('</Screen>', after));

    expect(tail.trim()).toBe('');
  });

  it('убранные заказчиком блоки на экран не вернулись', () => {
    // ⚠️ Три блока убраны 14.09.2026: «kerak emas bu yo'nalishlar va
    // casting ijodkorlari va yana home pagedagi eng oxirida premiumga
    // o'tishni olib tashlash».
    //
    // Каждый из них легко «вернуть как было»: направления и анкеты
    // выглядят как потеря функциональности, а Premium — как потеря
    // денег. Поэтому решение заказчика держит тест, а не комментарий.
    expect(isActive("t('home.categories')")).toBe(false);
    expect(isActive("t('home.castingCreators')")).toBe(false);
    expect(isActive("t('home.premiumTitle')")).toBe(false);
  });
});
