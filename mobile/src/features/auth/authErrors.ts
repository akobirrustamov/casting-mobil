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

  /**
   * ⚠️ Код от `RateLimitFilter`, а не от `OtpService` — и до сих пор его
   * здесь не было.
   *
   * Бэкенд режет отправку кода на уровне IP (5 запросов в минуту), и без
   * этой строки человек видел общее «Amal bajarilmadi»: неотличимо от
   * обрыва связи, хотя ждать надо всего минуту. Ровно так и читается
   * жалоба «SMS bir marta ishlab keyin ishlamayapti».
   */
  RATE_LIMIT_EXCEEDED: 'auth.tooManyRequests',

  /**
   * ⚠️ Общий код валидации бэкенда — в этом потоке это ВСЕГДА номер.
   *
   * Сервер отвечает внятно («Telefon raqam noto'g'ri: +998XXXXXXXXX
   * kutilmoqda»), а экран показывал общее «Amal bajarilmadi»: человек
   * видел отказ и не знал, что править.
   */
  VALIDATION_ERROR: 'auth.phoneInvalid',

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

  /**
   * ⚠️ «Запрос не дошёл» и «сервер отказал» — РАЗНЫЕ вещи, и раньше они
   * выглядели одинаково.
   *
   * Разбор 10.09.2026 занял полдня ровно из-за этого: приложение
   * стучалось по устаревшему адресу из `.env`, каждый запрос отваливался
   * по таймауту, а на экране стояло вежливое «Amal bajarilmadi, birozdan
   * keyin urinib ko'ring» — то же самое, что при отказе сервера. По нему
   * нельзя было понять ни что искать, ни где.
   *
   * `response === undefined` у axios означает именно это: ответа не было
   * вовсе — нет сети, неверный адрес, таймаут. Человеку это говорит
   * «проверь интернет», а тестировщику — «дело не в сервере».
   */
  if (axios.isAxiosError(error) && !error.response) {
    return 'auth.networkFailed';
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
