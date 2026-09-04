/**
 * Распознавание отказа «мест нет».
 *
 * <h2>⚠️ Почему именно это проверяется</h2>
 * От одного ответа сервера зависит, что увидит человек: экран со списком
 * устройств, где есть что сделать, — или общее «Что-то пошло не так»,
 * после которого он остаётся заперт снаружи собственного аккаунта.
 *
 * Ошибиться легко в обе стороны. Считать лимитом любой `409` нельзя:
 * этот код когда-нибудь вернёт другой эндпоинт. Сверять текст сообщения
 * тоже нельзя — оно на трёх языках и меняется в админке. Остаётся код
 * ошибки, и этот тест держит договор именно на нём.
 */

jest.mock('@/lib/api', () => ({ api: {} }));

import { AxiosError, AxiosHeaders } from 'axios';

import { isDeviceLimit, DeviceLimitError } from '../api';

/** Ответ бэкенда: `GlobalExceptionHandler` отдаёт `{code, message}`. */
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

describe('isDeviceLimit', () => {
  it('узнаёт 409 DEVICE_LIMIT_REACHED', () => {
    expect(isDeviceLimit(httpError(409, 'DEVICE_LIMIT_REACHED'))).toBe(true);
  });

  it('узнаёт собственный DeviceLimitError', () => {
    expect(isDeviceLimit(new DeviceLimitError())).toBe(true);
  });

  /** Другой конфликт — не про устройства, и экран нужен другой. */
  it('не путает с чужим 409', () => {
    expect(isDeviceLimit(httpError(409, 'DUPLICATE_PHONE'))).toBe(false);
  });

  it('не считает лимитом 409 без кода', () => {
    expect(isDeviceLimit(httpError(409))).toBe(false);
  });

  it('не считает лимитом 401', () => {
    expect(isDeviceLimit(httpError(401, 'DEVICE_REVOKED'))).toBe(false);
  });

  /** Обрыв сети — ответа нет вовсе. */
  it('не считает лимитом обычную ошибку', () => {
    expect(isDeviceLimit(new Error('Network Error'))).toBe(false);
  });
});
