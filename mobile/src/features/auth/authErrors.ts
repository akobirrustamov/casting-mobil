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

  // --- Имя (только у новых) ---
  /** Подтверждение просрочено: имя набирали дольше 15 минут. */
  PHONE_NOT_VERIFIED: 'auth.verificationExpired',
  NAME_INVALID: 'auth.nameInvalid',

  // ⚠️ Коды пароля (PASSWORD_TOO_SHORT, PASSWORD_MISMATCH,
  // PASSWORD_NOT_SET, INVALID_CREDENTIALS, ACCOUNT_LOCKED) и
  // PHONE_ALREADY_REGISTERED / PHONE_NOT_REGISTERED убраны 04.09.2026
  // вместе с самим паролем: бэкенд их больше не шлёт. Занятый номер
  // теперь не ошибка, а обычный вход.
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

/**
 * Техническая приписка к сообщению об ошибке — ТОЛЬКО на время разбора.
 *
 * <h2>Зачем</h2>
 * И `authErrorKey`, и `googleErrorKey` при незнакомой ошибке возвращают
 * общий текст. Это правильно для человека, но при разборе беспощадно:
 * «не получилось» одинаково выглядит и когда сервер ответил `500`, и
 * когда до сервера вообще не дошли. Разные причины — разные люди и
 * разные действия, а на экране одна строка.
 *
 * Здесь достаётся то, что отличает эти случаи: код ответа, код axios
 * (`ERR_NETWORK`, `ECONNABORTED`) и текст.
 *
 * ⚠️ Убрать вместе с `BuildMarker`, когда вход заработает.
 */
export function technicalDetail(error: unknown): string | undefined {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const body = error.response?.data as { code?: string; message?: string } | undefined;

    return [status, error.code, body?.code ?? body?.message, error.message]
      .filter(Boolean)
      .join(' · ')
      .slice(0, 160);
  }

  if (error instanceof Error) return error.message.slice(0, 160);

  return undefined;
}
