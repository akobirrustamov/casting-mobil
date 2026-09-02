import { AxiosError, AxiosHeaders } from 'axios';

import { googleErrorKey } from '../otpErrors';

/**
 * Ответ сервера на обмен Google ID-токена → что видит человек.
 *
 * <h2>Почему это стоит теста</h2>
 * До этого экран показывал `error.message` от axios, то есть буквально
 * «Request failed with status code 503». По такой строке нельзя понять,
 * что чинить: тестировщик идёт проверять телефон и интернет, хотя дело
 * в одной строке конфига НА СЕРВЕРЕ.
 *
 * Различать `503` и `401` важно именно потому, что чинят их разные люди:
 * первое — тот, у кого доступ к серверу (`app.google.client-ids`,
 * docs/GOOGLE_AUTH.md §6), второе — вопрос к самому аккаунту и SHA-1.
 */
function axiosErrorWithStatus(status: number): AxiosError {
  const config = { headers: new AxiosHeaders() };
  return new AxiosError('Request failed with status code ' + status, 'ERR_BAD_RESPONSE', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data: {},
  });
}

describe('googleErrorKey', () => {
  it('503 — сервер не настроен, и это не вина человека', () => {
    expect(googleErrorKey(axiosErrorWithStatus(503))).toBe('auth.googleServerNotReady');
  });

  it('401 — токен отклонён', () => {
    expect(googleErrorKey(axiosErrorWithStatus(401))).toBe('auth.googleRejected');
  });

  it('прочие коды и обрыв связи — общий текст', () => {
    expect(googleErrorKey(axiosErrorWithStatus(500))).toBe('auth.googleFailed');
    expect(googleErrorKey(new Error('Network Error'))).toBe('auth.googleFailed');
    expect(googleErrorKey(undefined)).toBe('auth.googleFailed');
  });
});
