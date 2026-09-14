import { compactCount, groupDigits } from '../money';

/**
 * Сокращённый счётчик — «1.2k», «1.5m» (заказчик, 14.09.2026).
 *
 * <h2>Что здесь ломается тихо</h2>
 * Округление. `toFixed(1)` вместо `floor` — правка на один символ, после
 * которой 1 999 просмотров превращаются в «2k». На скриншоте это
 * выглядит нормально, и заметить приписку можно только зная точное
 * число. Поэтому граничные значения проверяются парами: «на единицу
 * меньше круглого» и само круглое.
 */
describe('compactCount', () => {
  it('меньше тысячи — цифра как есть', () => {
    expect(compactCount(0)).toBe('0');
    expect(compactCount(35)).toBe('35');
    expect(compactCount(999)).toBe('999');
  });

  it('тысячи — с одним знаком после точки', () => {
    expect(compactCount(1000)).toBe('1k');
    expect(compactCount(1200)).toBe('1.2k');
    expect(compactCount(5606)).toBe('5.6k');
  });

  it('округление ВНИЗ: «2k» на 1 999 — приписка', () => {
    expect(compactCount(1999)).toBe('1.9k');
    expect(compactCount(1099)).toBe('1k');
    expect(compactCount(1_999_999)).toBe('1.9m');
  });

  it('от десяти тысяч дробная часть не нужна — она только шире', () => {
    expect(compactCount(12_345)).toBe('12k');
    expect(compactCount(123_456)).toBe('123k');
    expect(compactCount(999_999)).toBe('999k');
  });

  it('миллионы и миллиарды', () => {
    expect(compactCount(1_000_000)).toBe('1m');
    expect(compactCount(1_500_000)).toBe('1.5m');
    expect(compactCount(1_000_000_000)).toBe('1b');
  });

  it('дробное на входе не ломает вид', () => {
    // Счётчик приходит целым, но `viewCount` — это `number`, и никакой
    // проверки на целое между сервером и экраном нет.
    expect(compactCount(1249.7)).toBe('1.2k');
  });

  /**
   * ⚠️ Точное разбиение по разрядам НИКУДА не делось: оно осталось у
   * денег и донатов. Если однажды `groupDigits` тоже начнут сокращать,
   * «49 999 so'm» превратится в «49.9k so'm» на экране оплаты.
   */
  it('деньги по-прежнему считаются полностью', () => {
    expect(groupDigits(49_999)).toBe('49 999');
    expect(groupDigits(1_500_000)).toBe('1 500 000');
  });
});
