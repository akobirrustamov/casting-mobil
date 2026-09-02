import axios from 'axios';

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

/**
 * Ошибка обмена Google ID-токена → ключ перевода.
 *
 * Здесь коды HTTP, а не `BusinessException.code`: `/api/v1/auth/google`
 * отвечает статусом и телом `{error}` на узбекском (см. бэкенд
 * `AuthServiceImpl.googleLogin`).
 *
 * <h2>Почему 503 отделён от остальных</h2>
 * Это не поломка и не вина человека: `GoogleTokenVerifier` без списка
 * `app.google.client-ids` осознанно не поднимается, чтобы не принимать
 * любой токен молча. Пока строку не добавят на сервер, вход будет падать
 * у ВСЕХ и всегда.
 *
 * ⚠️ Раньше экран показывал `error.message` от axios, то есть человек
 * видел «Request failed with status code 503». По такой строке нельзя
 * понять, что чинить, и тестировщик уходит искать проблему в телефоне.
 */
export function googleErrorKey(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 503) return 'auth.googleServerNotReady';
    if (error.response?.status === 401) return 'auth.googleRejected';
  }
  return 'auth.googleFailed';
}
