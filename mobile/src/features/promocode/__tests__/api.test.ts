/**
 * Промокод — распознавание причины отказа.
 *
 * <h2>⚠️ Почему это нужно тестами</h2>
 * Промокод даёт бесплатный Premium, и человек ошибается при вводе чаще,
 * чем где-либо ещё: код приходит из мессенджера, с баннера, со слуха.
 * От того, какую причину покажет экран, зависит, что он сделает дальше:
 * наберёт заново, посмотрит срок акции или перестанет пытаться, потому
 * что код уже активирован.
 *
 * Свести пять причин к одному «не подошло» — самая дешёвая и самая
 * дорогая ошибка на этом экране: она ничего не ломает и заставляет
 * человека повторять бесполезное действие.
 */

jest.mock('@/lib/api', () => ({ api: {} }));

import { AxiosError, AxiosHeaders } from 'axios';

import { PromoError, promoErrorKey } from '../api';

function httpError(status: number, code?: string): AxiosError {
  const error = new AxiosError('failed');
  error.response = {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: code ? { code, message: 'xabar' } : {},
  };
  return error;
}

describe('promoErrorKey', () => {
  it.each([
    ['PROMO_NOT_FOUND', 'promocode.errorNotFound'],
    ['PROMO_EXPIRED', 'promocode.errorExpired'],
    ['PROMO_ALREADY_USED', 'promocode.errorAlreadyUsed'],
    ['PROMO_EXHAUSTED', 'promocode.errorExhausted'],
    ['PROMO_INACTIVE', 'promocode.errorInactive'],
    ['VALIDATION_ERROR', 'promocode.errorMalformed'],
  ] as const)('%s → %s', (code, key) => {
    expect(promoErrorKey(new PromoError(code))).toBe(key);
  });

  /** Незнакомый код — общий текст. Врать про причину нельзя. */
  it('незнакомый код даёт общий текст', () => {
    expect(promoErrorKey(new PromoError('UNKNOWN'))).toBe('promocode.errorUnknown');
    expect(promoErrorKey(new Error('Network Error'))).toBe('promocode.errorUnknown');
    expect(promoErrorKey(httpError(500, 'BOOM'))).toBe('promocode.errorUnknown');
  });

  /**
   * ⚠️ «Уже использован» и «не найден» — разные сообщения, и их нельзя
   * перепутать: первый говорит «всё в порядке, Premium у вас уже есть»,
   * второй — «наберите заново».
   */
  it('«использован» и «не найден» не совпадают', () => {
    expect(promoErrorKey(new PromoError('PROMO_ALREADY_USED')))
      .not.toBe(promoErrorKey(new PromoError('PROMO_NOT_FOUND')));
  });
});
