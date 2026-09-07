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

/** Позиция куска в файле; -1 превращается в понятный провал. */
function at(marker: string): number {
  const index = SCREEN.indexOf(marker);
  expect(index).toBeGreaterThanOrEqual(0);
  return index;
}

describe('главная: порядок блоков', () => {
  it('«продолжить просмотр» стоит один раз', () => {
    const all = SCREEN.split('<ContinueRail />').length - 1;
    expect(all).toBe(1);
  });

  it('«продолжить просмотр» — ПОСЛЕ разделов каталога', () => {
    expect(at('<ContinueRail />')).toBeGreaterThan(at('<CategoryRows />'));
    expect(at('<ContinueRail />')).toBeGreaterThan(at("t('home.categories')"));
  });

  it('«продолжить просмотр» — ПЕРЕД кастингом', () => {
    // Дословно «castingdan oldin». Кастинг на главной остался один —
    // ряд анкет; выдуманные объявления убраны 06.09.2026.
    expect(at('<ContinueRail />')).toBeLessThan(at("t('home.castingCreators')"));
    expect(at('<ContinueRail />')).toBeLessThan(at("t('home.premiumTitle')"));
  });

  it('фид сервера идёт выше — витрину заказчик оставил наверху', () => {
    expect(at('<HomeFeedBlock')).toBeLessThan(at('<ContinueRail />'));
  });
});
