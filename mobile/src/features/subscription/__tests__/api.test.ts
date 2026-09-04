/**
 * История платежей — склейка подписок и донатов.
 *
 * <h2>⚠️ Почему это нужно тестами</h2>
 * Порядок строк здесь — единственное, что делает клиент сам; всё
 * остальное приходит с сервера готовым. Ошибка в сортировке не роняет
 * экран: список просто показывает прошлогодний подарок над вчерашней
 * покупкой, и человек решает, что ему выдали не то.
 */

jest.mock('@/lib/api', () => ({ api: {} }));
jest.mock('@/features/auth/store', () => ({ useAuthStore: jest.fn() }));
jest.mock('@/features/home/api', () => ({ feedLocale: jest.fn(() => 'UZ') }));

import { mergeHistory, type DonationEntry, type SubscriptionEntry } from '../api';

function sub(id: number, startAt: string, paid: number | null): SubscriptionEntry {
  return {
    id,
    tariffCode: 'm1',
    tariffName: '1 oy',
    startAt,
    endAt: '2026-12-31T00:00:00',
    source: paid == null ? 'ADMIN_GIFT' : 'PURCHASE',
    paidAmount: paid,
    currency: 'UZS',
    revokedAt: null,
    active: false,
  };
}

function donation(id: number, createdAt: string): DonationEntry {
  return { id, targetName: 'Ali', kind: 'STARS', amount: 5, createdAt };
}

describe('mergeHistory', () => {
  it('новое сверху, независимо от источника', () => {
    const rows = mergeHistory(
      [sub(1, '2026-08-01T10:00:00', 24000), sub(2, '2026-09-03T10:00:00', 24000)],
      [donation(7, '2026-09-01T12:00:00')],
    );

    expect(rows.map((r) => r.key)).toEqual(['s-2', 'd-7', 's-1']);
  });

  /** Ключи разных источников не пересекаются: подписка №7 и донат №7 — разные строки. */
  it('одинаковые id из разных источников не сливаются', () => {
    const rows = mergeHistory([sub(7, '2026-09-01T00:00:00', 24000)], [donation(7, '2026-09-02T00:00:00')]);

    expect(rows).toHaveLength(2);
    expect(new Set(rows.map((r) => r.key)).size).toBe(2);
  });

  it('подарок сохраняет пустую сумму, а не ноль', () => {
    const [row] = mergeHistory([sub(1, '2026-09-01T00:00:00', null)], []);

    expect(row.kind).toBe('subscription');
    if (row.kind === 'subscription') {
      expect(row.entry.paidAmount).toBeNull();
      expect(row.entry.source).toBe('ADMIN_GIFT');
    }
  });

  it('пустые источники дают пустой список', () => {
    expect(mergeHistory([], [])).toEqual([]);
  });
});
