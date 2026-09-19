/**
 * Заявка кандидата: статус на экране и разбор ошибок сервера.
 *
 * <h2>⚠️ Почему это нужно тестами</h2>
 * Цена у заявки на рассмотрении или «Katalogda ko'rinmoqda» у отказа —
 * ложь, которую кандидат примет за решение админа. А перепутанный код
 * ошибки (409 бывает двух видов) отправит человека не туда: ждать
 * ответа вместо того, чтобы перезагрузить фото.
 */
jest.mock('@/lib/api', () => ({ api: {} }));

import { AxiosError, AxiosHeaders } from 'axios';

import { castingErrorKey, toCastingError } from '../api';
import { applicationStatusView, formatDate, normalizeStatus, pickHeadline, type MyApplication } from '../status';

function app(patch: Partial<MyApplication>): MyApplication {
  return {
    id: 1,
    castingType: 'model',
    name: 'Madina',
    status: 'PENDING',
    price: null,
    paid: false,
    isWebShow: false,
    createdAt: '2026-09-10T10:00:00',
    photos: [],
    ...patch,
  };
}

describe('applicationStatusView', () => {
  it('на рассмотрении — без цены', () => {
    const view = applicationStatusView(app({ status: 'PENDING', price: 150000 }));
    expect(view.labelKey).toBe('casting.status.pending');
    expect(view.price).toBeNull();
  });

  it('одобрено — цена и «оплата скоро»', () => {
    const view = applicationStatusView(app({ status: 'APPROVED', price: 150000 }));
    expect(view.labelKey).toBe('casting.status.approved');
    expect(view.price).toBe(150000);
    expect(view.noteKey).toBe('casting.status.paymentSoon');
  });

  it('одобрено без цены — цены нет, а не «0 so\'m»', () => {
    expect(applicationStatusView(app({ status: 'APPROVED', price: 0 })).price).toBeNull();
    expect(applicationStatusView(app({ status: 'APPROVED', price: null })).price).toBeNull();
  });

  it('оплачено — своя пометка', () => {
    expect(applicationStatusView(app({ status: 'APPROVED', price: 1, paid: true })).noteKey).toBe(
      'casting.status.paid',
    );
  });

  it('отклонено — ни цены, ни каталога', () => {
    const view = applicationStatusView(app({ status: 'REJECTED', price: 150000, isWebShow: true }));
    expect(view.labelKey).toBe('casting.status.rejected');
    expect(view.price).toBeNull();
    expect(view.showInCatalog).toBe(false);
  });

  it('«в каталоге» — по флагу админа', () => {
    expect(applicationStatusView(app({ status: 'APPROVED', isWebShow: true })).showInCatalog).toBe(true);
    expect(applicationStatusView(app({ status: 'APPROVED', isWebShow: false })).showInCatalog).toBe(false);
  });

  /** Новый статус с сервера не должен ни обрадовать, ни расстроить зря. */
  it('незнакомый статус — «на рассмотрении»', () => {
    expect(normalizeStatus('PAID_LATER')).toBe('PENDING');
    expect(normalizeStatus('approved')).toBe('APPROVED');
  });
});

describe('pickHeadline', () => {
  it('открытая заявка важнее свежего решения', () => {
    const pending = app({ id: 1, status: 'PENDING', createdAt: '2026-01-01T00:00:00' });
    const rejected = app({ id: 2, status: 'REJECTED', createdAt: '2026-09-01T00:00:00' });
    expect(pickHeadline([rejected, pending])?.id).toBe(1);
  });

  it('без открытой — самая свежая', () => {
    const older = app({ id: 1, status: 'REJECTED', createdAt: '2026-01-01T00:00:00' });
    const newer = app({ id: 2, status: 'APPROVED', createdAt: '2026-09-01T00:00:00' });
    expect(pickHeadline([older, newer])?.id).toBe(2);
    expect(pickHeadline([])).toBeNull();
  });
});

describe('formatDate', () => {
  it('LocalDateTime без зоны → ДД.ММ.ГГГГ без сдвига', () => {
    expect(formatDate('2026-09-19T23:59:59')).toBe('19.09.2026');
    expect(formatDate(null)).toBe('');
  });
});

function httpError(status: number, data: unknown): AxiosError {
  const error = new AxiosError('failed');
  error.response = {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data,
  };
  return error;
}

describe('toCastingError', () => {
  it('422 VALIDATION_ERROR — с ошибками по полям', () => {
    const err = toCastingError(
      httpError(422, {
        code: 'VALIDATION_ERROR',
        message: 'Xato',
        errors: [{ field: 'height', message: "Bo'y santimetrda: kamida 50" }],
      }),
    );
    expect(err.code).toBe('VALIDATION_ERROR');
    expect(err.fieldErrors).toEqual({ height: "Bo'y santimetrda: kamida 50" });
  });

  /** Один статус 409 — две разные причины и два разных действия человека. */
  it('409 различается по коду', () => {
    expect(castingErrorKey(httpError(409, { code: 'CASTING_APPLICATION_PENDING' }))).toBe('casting.errors.pending');
    expect(castingErrorKey(httpError(409, { code: 'CASTING_PHOTO_IN_USE' }))).toBe('casting.errors.photoInUse');
    expect(castingErrorKey(httpError(422, { code: 'CASTING_PHOTO_NOT_FOUND' }))).toBe('casting.errors.photoNotFound');
  });

  it('нет ответа — это сеть, а не отказ сервера', () => {
    expect(castingErrorKey(new AxiosError('Network Error'))).toBe('casting.errors.network');
  });

  it('запрет READ_ONLY назван своим именем', () => {
    expect(castingErrorKey(new Error('[READ_ONLY] Запрос POST заблокирован'))).toBe('casting.errors.readOnly');
  });

  it('незнакомое — общий текст', () => {
    expect(castingErrorKey(httpError(500, { code: 'BOOM' }))).toBe('casting.errors.unknown');
  });
});
