import { AuthError } from './api';

/**
 * Коды ошибок с бэкенда (`BusinessException.code`, см. `AppAccountService`
 * и `OtpService`) → ключи перевода. Общее для всех экранов входа.
 *
 * Возвращаем ключ, а не готовый текст: `t` живёт в компоненте, и так помощник
 * не тянет за собой типы i18next.
 */
const MESSAGE_KEY: Record<string, string> = {
  // --- SMS ---
  OTP_COOLDOWN: 'auth.otpCooldown',
  OTP_INVALID: 'auth.otpInvalid',
  OTP_EXPIRED: 'auth.otpExpired',
  OTP_LOCKED: 'auth.otpLocked',
  SMS_NOT_CONFIGURED: 'auth.smsUnavailable',
  SMS_SEND_FAILED: 'auth.otpSendFailed',

  // --- Регистрация ---
  /** Номер занят — экран уводит на вкладку входа, текст только поясняет. */
  PHONE_ALREADY_REGISTERED: 'auth.phoneAlreadyRegistered',
  /** Подтверждение просрочено: пароль придумывали дольше 15 минут. */
  PHONE_NOT_VERIFIED: 'auth.verificationExpired',
  PASSWORD_TOO_SHORT: 'auth.passwordTooShort',
  PASSWORD_TOO_LONG: 'auth.passwordTooLong',
  PASSWORD_MISMATCH: 'auth.passwordMismatch',
  NAME_INVALID: 'auth.nameInvalid',

  // --- Вход ---
  PHONE_NOT_REGISTERED: 'auth.phoneNotRegistered',
  INVALID_CREDENTIALS: 'auth.invalidCredentials',
  /**
   * Аккаунт создан через SMS или Google — пароля у него никогда не было.
   * «Неверный пароль» здесь был бы неправдой: подобрать его нельзя.
   */
  PASSWORD_NOT_SET: 'auth.passwordNotSet',
  ACCOUNT_LOCKED: 'auth.accountLocked',
};

/** Незнакомый код или обрыв связи — общий «не получилось». */
export function authErrorKey(error: unknown): string {
  if (error instanceof AuthError) {
    const key = MESSAGE_KEY[error.code];
    if (key) return key;
  }
  return 'auth.requestFailed';
}
