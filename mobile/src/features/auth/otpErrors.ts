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
