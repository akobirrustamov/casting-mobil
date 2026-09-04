import { statusCodes } from '@react-native-google-signin/google-signin';

import { toSignInError, toSignInResult } from '../useGoogleSignIn';

/**
 * Ответ нативного входа → состояние экрана.
 *
 * <h2>Почему это стоит теста</h2>
 * Отмена и ошибка приходят разными путями — отмена возвращается ответом
 * (`type: 'cancelled'`), а часть проблем прилетает исключением. Если
 * перепутать, человек, закрывший окно выбора аккаунта, увидит красную
 * надпись «не удалось войти», хотя он ничего не ломал.
 *
 * Второй предмет проверки — успех БЕЗ токена. Так бывает, когда в консоли
 * не задан веб-клиент: библиотека отвечает `success`, а `idToken` пустой.
 * Уйти в «успех» тут нельзя — обменивать на сессию нечего, и экран
 * молча остался бы на месте.
 */
describe('toSignInResult', () => {
  it('успех с токеном', () => {
    const r = toSignInResult({
      type: 'success',
      data: { idToken: 'abc', user: {} },
    } as never);
    expect(r).toEqual({ status: 'success', idToken: 'abc' });
  });

  it('отмена — не ошибка', () => {
    expect(toSignInResult({ type: 'cancelled', data: null } as never)).toEqual({
      status: 'cancelled',
    });
  });

  /**
   * ⚠️ Своё сообщение, а не общее: «вошёл, но токена нет» означает, что
   * `webClientId` не тот или не из этого проекта. Свалив это в общий текст,
   * мы бы отправили человека проверять интернет.
   */
  it('успех без токена — отдельная причина', () => {
    const r = toSignInResult({ type: 'success', data: { idToken: null } } as never);
    expect(r).toEqual({ status: 'error', messageKey: 'auth.googleNoToken' });
  });
});

describe('toSignInError', () => {
  it('отмена через исключение тоже не ошибка', () => {
    expect(toSignInError({ code: statusCodes.SIGN_IN_CANCELLED })).toEqual({
      status: 'cancelled',
    });
  });

  /**
   * Отдельный текст: без сервисов Google Play вход невозможен в принципе,
   * и «попробуйте ещё раз» тут вводило бы в заблуждение.
   */
  it('нет сервисов Google Play — свой текст', () => {
    expect(toSignInError({ code: statusCodes.PLAY_SERVICES_NOT_AVAILABLE })).toEqual({
      status: 'error',
      messageKey: 'auth.googleNoPlayServices',
    });
  });

  /**
   * ⚠️ `DEVELOPER_ERROR` (`'10'`) — самая частая и самая обидная ошибка: Google
   * не узнал приложение, потому что расходятся SHA-1, package или client ID.
   * В общем тексте «не удалось войти» она неотличима от обрыва связи, и
   * человек ищет причину в телефоне, хотя чинится это в Google Cloud Console.
   */
  it('DEVELOPER_ERROR — про настройку, а не про связь', () => {
    expect(toSignInError({ code: '10' })).toEqual({
      status: 'error',
      messageKey: 'auth.googleConfigMismatch',
    });
  });

  /**
   * ⚠️ Notanish xato — UMUMIY matn.
   *
   * Ilgari bu yerda `detail` maydoni tekshirilardi: xato kodi ekranga
   * ikkinchi satr bo'lib chiqardi. U 03–04.09.2026 dagi kirish razbori
   * uchun vaqtincha qo'shilgan edi (`BuildMarker`, `NetworkProbe` bilan
   * birga) va razbor tugagach olib tashlandi: `DEVELOPER_ERROR · ...`
   * kabi satr oddiy odamga hech narsa aytmaydi.
   *
   * Qolgani muhim va shuning uchun test saqlanib qoldi: xato qaysi
   * shaklda kelmasin — kod bilan, kodsiz, satr, `null`, bo'sh obyekt —
   * u yiqilmasdan bitta tushunarli matnga aylanishi kerak.
   */
  it('notanish xato umumiy matnga tushadi', () => {
    const cases: unknown[] = [
      { code: 'WHATEVER' },
      new Error('RNGoogleSignin native module is not available'),
      { code: '12500', message: 'Sign in failed' },
      'boom',
      null,
      {},
    ];

    for (const input of cases) {
      expect(toSignInError(input)).toEqual({
        status: 'error',
        messageKey: 'auth.googleFailed',
      });
    }
  });
});
