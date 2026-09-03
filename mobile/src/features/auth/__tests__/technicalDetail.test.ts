import { AxiosError, AxiosHeaders } from 'axios';

import { technicalDetail } from '../authErrors';

/**
 * Что показать при разборе, когда пользовательский текст ничего не говорит.
 *
 * <h2>Почему это стоит теста</h2>
 * Разбор входа 03.09.2026 встал именно на этом: и «Kirish amalga oshmadi»,
 * и «Amal bajarilmadi» — общие фолбэки, и по ним нельзя отличить ответ
 * сервера от того, что до сервера вообще не дошли. Это разные причины,
 * разные люди и разные действия.
 */
function axiosError(opts: { status?: number; code?: string; data?: unknown; message?: string }) {
  const config = { headers: new AxiosHeaders() };
  const response = opts.status
    ? { status: opts.status, statusText: '', headers: {}, config, data: opts.data }
    : undefined;
  return new AxiosError(opts.message ?? 'failed', opts.code, config, null, response as never);
}

describe('technicalDetail', () => {
  it('ответ сервера — код и бизнес-код из тела', () => {
    expect(technicalDetail(axiosError({ status: 502, data: { code: 'SMS_SEND_FAILED' } }))).toContain(
      '502',
    );
    expect(technicalDetail(axiosError({ status: 502, data: { code: 'SMS_SEND_FAILED' } }))).toContain(
      'SMS_SEND_FAILED',
    );
  });

  /**
   * ⚠️ Главный случай: до сервера НЕ дошли. Ответа нет, есть только код
   * axios — и именно он отделяет «сервер отказал» от «сети не было».
   */
  it('сеть не дошла — виден ERR_NETWORK', () => {
    const d = technicalDetail(axiosError({ code: 'ERR_NETWORK', message: 'Network Error' }));
    expect(d).toContain('ERR_NETWORK');
    expect(d).not.toContain('undefined');
  });

  it('обычная ошибка — её текст', () => {
    expect(technicalDetail(new Error('boom'))).toBe('boom');
  });

  it('не ошибка — ничего', () => {
    expect(technicalDetail(null)).toBeUndefined();
    expect(technicalDetail('строка')).toBeUndefined();
  });
});
