import { ago, parseServerTime } from '../time';

/**
 * «Когда написано».
 *
 * ⚠️ Главное здесь — время сервера с МИКРОсекундами. `new Date()` на
 * Hermes такую строку понимать не обязан, и лента показала бы
 * «NaN мин назад» под каждым комментарием.
 */
describe('parseServerTime', () => {
  it('понимает LocalDateTime с микросекундами', () => {
    const d = parseServerTime('2026-09-10T14:25:55.495270');

    expect(d).not.toBeNull();
    expect(d?.getFullYear()).toBe(2026);
    expect(d?.getMonth()).toBe(8);
    expect(d?.getDate()).toBe(10);
    expect(d?.getHours()).toBe(14);
    expect(d?.getMinutes()).toBe(25);
    expect(d?.getSeconds()).toBe(55);
  });

  it('пустое и мусор — null, а не Invalid Date', () => {
    expect(parseServerTime(null)).toBeNull();
    expect(parseServerTime('')).toBeNull();
    expect(parseServerTime('вчера')).toBeNull();
  });
});

describe('ago', () => {
  const at = (iso: string) => new Date(parseServerTime(iso) as Date).getTime();
  const base = '2026-09-10T12:00:00';
  const now = at(base);

  it('меньше минуты — «только что»', () => {
    expect(ago('2026-09-10T11:59:30', now)).toEqual({ kind: 'now' });
  });

  it('часы телефона впереди сервера — тоже «только что», не «из будущего»', () => {
    expect(ago('2026-09-10T12:02:00', now)).toEqual({ kind: 'now' });
  });

  it('минуты, часы, дни', () => {
    expect(ago('2026-09-10T11:55:00', now)).toEqual({ kind: 'minutes', count: 5 });
    expect(ago('2026-09-10T09:00:00', now)).toEqual({ kind: 'hours', count: 3 });
    expect(ago('2026-09-08T12:00:00', now)).toEqual({ kind: 'days', count: 2 });
  });

  it('старше недели — дата, а не «40 дней назад»', () => {
    expect(ago('2026-08-01T10:00:00', now)).toEqual({ kind: 'date', text: '01.08.2026' });
  });

  it('без времени — ничего', () => {
    expect(ago(null, now)).toBeNull();
  });
});
