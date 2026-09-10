/**
 * «Когда написано» для ленты комментариев.
 *
 * Возвращает не строку, а описание: перевод делает экран. Так правило
 * «до минуты — только что, до суток — часы» проверяется тестом без
 * i18n, а тексты живут в словарях.
 */
export type Ago =
  | { kind: 'now' }
  | { kind: 'minutes'; count: number }
  | { kind: 'hours'; count: number }
  | { kind: 'days'; count: number }
  | { kind: 'date'; text: string };

/**
 * Время сервера → локальная дата.
 *
 * ⚠️ Разбор РУЧНОЙ, а не `new Date(iso)`. Сервер присылает
 * `LocalDateTime` без пояса и с микросекундами
 * («2026-09-10T14:25:55.495270»). Шесть цифр дробной части спецификация
 * не обещает понимать, и на Hermes такая строка может стать `Invalid
 * Date` — лента показала бы «NaN мин назад». Время трактуется как
 * местное: сервер и зрители в одном поясе (Ташкент).
 */
export function parseServerTime(iso: string | null): Date | null {
  if (!iso) return null;
  const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/.exec(iso);
  if (!m) return null;

  const [, y, mo, d, h, mi, s] = m;
  return new Date(
    Number(y),
    Number(mo) - 1,
    Number(d),
    Number(h),
    Number(mi),
    Number(s ?? 0)
  );
}

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

export function ago(iso: string | null, now: number = Date.now()): Ago | null {
  const date = parseServerTime(iso);
  if (!date) return null;

  // Часы телефона бывают впереди сервера на минуту-другую — свежий
  // комментарий тогда оказался бы «из будущего». Это тоже «только что».
  const diff = Math.max(0, now - date.getTime());

  if (diff < MINUTE) return { kind: 'now' };
  if (diff < HOUR) return { kind: 'minutes', count: Math.floor(diff / MINUTE) };
  if (diff < DAY) return { kind: 'hours', count: Math.floor(diff / HOUR) };
  if (diff < 7 * DAY) return { kind: 'days', count: Math.floor(diff / DAY) };

  const dd = String(date.getDate()).padStart(2, '0');
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  return { kind: 'date', text: `${dd}.${mm}.${date.getFullYear()}` };
}
