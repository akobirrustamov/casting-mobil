import axios from 'axios';

import { OtpError } from './api';

/**
 * Коды ошибок с бэкенда (`BusinessException.code`, см. backend `OtpService`)
 * → ключи перевода. Общее для экранов sign-in и otp.
 *
 * Возвращаем ключ, а не готовый текст: `t` живёт в компоненте, и так помощник
 * не тянет за собой типы i18next.
 */
const MESSAGE_KEY: Record<string, string> = {
  OTP_COOLDOWN: 'auth.otpCooldown',
  OTP_INVALID: 'auth.otpInvalid',
  OTP_EXPIRED: 'auth.otpExpired',
  OTP_LOCKED: 'auth.otpLocked',
  SMS_NOT_CONFIGURED: 'auth.smsUnavailable',
  SMS_SEND_FAILED: 'auth.otpSendFailed',
};

/** Незнакомый код или обрыв связи — общий «не удалось отправить». */
export function otpErrorKey(error: unknown): string {
  if (error instanceof OtpError) {
    const key = MESSAGE_KEY[error.code];
    if (key) return key;
  }
  return 'auth.otpSendFailed';
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
