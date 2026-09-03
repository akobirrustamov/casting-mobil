// ⚠️ ТОЛЬКО тип: `import type` стирается при сборке и нативный модуль
// не трогает. Значения берём через `googleModule.ts` — обычный `import`
// значений отсюда ронял весь экран входа, см. пояснение там.
import type { SignInResponse } from '@react-native-google-signin/google-signin';
import { useCallback, useState } from 'react';

import { isGoogleConfigured } from './config';
import { googleSignin, googleStatusCodes } from './googleModule';

export type GoogleSignInResult =
  | { status: 'idle' }
  | { status: 'pending' }
  | { status: 'cancelled' }
  | { status: 'error'; messageKey: string }
  | { status: 'success'; idToken: string };

/**
 * Вход через Google — нативный, через сервисы Google Play.
 *
 * <h2>Почему не браузерный OAuth (`expo-auth-session`)</h2>
 * На нём это и было написано, и в SDK 57 провайдер `providers/google`
 * помечен **deprecated**: документация Expo ведёт сюда. Практика это
 * подтвердила — 02.09.2026 первая APK упёрлась в браузерный флоу дважды
 * подряд, и оба раза не в наш код:
 *
 * 1. Google вообще не принял запрос — `400 invalid_request`, «Custom URI
 *    scheme is not enabled for your Android client». Снимается тумблером
 *    в консоли, см. docs/GOOGLE_AUTH.md §4.
 * 2. После согласия Google вернулся на `uzcasting://oauthredirect?code=…`,
 *    но это открылось как обычная ссылка в приложении — expo-router показал
 *    «Unmatched Route», а сессия авторизации осталась висеть.
 *
 * Здесь браузера нет вовсе: системное окно выбора аккаунта отдаёт ID-токен
 * напрямую. Нет ни redirect, ни схемы, ни зависимости от того, какой
 * браузер стоит у человека. Заодно это и есть тот системный выбор аккаунта,
 * который нарисован в макетах.
 *
 * ⚠️ Нативный модуль: в Expo Go не работает и **по воздуху (`eas update`)
 * не доставляется** — нужна новая сборка. Сам модуль подгружается
 * лениво (`googleModule.ts`), поэтому его отсутствие гасит одну кнопку,
 * а не весь экран входа.
 */

/**
 * Ответ библиотеки → состояние экрана.
 *
 * Вынесено из хука, чтобы поведение можно было проверить тестом: сам хук
 * тянет за собой нативный модуль и в jest не поднимается.
 */
export function toSignInResult(response: SignInResponse): GoogleSignInResult {
  if (response.type === 'cancelled') {
    return { status: 'cancelled' };
  }

  const idToken = response.data?.idToken;

  // Токена может не быть при успешном входе: так бывает, когда в консоли
  // не задан веб-клиент. Молча уйти в «успех» нельзя — обменивать нечего.
  return idToken
    ? { status: 'success', idToken }
    : { status: 'error', messageKey: 'auth.googleFailed' };
}

/** Брошенное исключение → сообщение. Отмену пользователем за ошибку не считаем. */
export function toSignInError(error: unknown): GoogleSignInResult {
  const code = (error as { code?: string } | null)?.code;
  const statusCodes = googleStatusCodes();

  if (statusCodes) {
    if (code === statusCodes.SIGN_IN_CANCELLED) {
      return { status: 'cancelled' };
    }
    if (code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
      return { status: 'error', messageKey: 'auth.googleNoPlayServices' };
    }
  }

  return { status: 'error', messageKey: 'auth.googleFailed' };
}

export function useGoogleSignIn(onSuccess?: (idToken: string) => void) {
  const [result, setResult] = useState<GoogleSignInResult>({ status: 'idle' });

  const signIn = useCallback(async () => {
    const google = isGoogleConfigured ? googleSignin() : null;

    if (!google) {
      setResult({ status: 'error', messageKey: 'auth.googleUnavailable' });
      return;
    }

    setResult({ status: 'pending' });

    try {
      // Проверка до показа окна: без сервисов Google Play вход невозможен,
      // и сказать об этом лучше сразу, а не пустым экраном.
      await google.hasPlayServices({ showPlayServicesUpdateDialog: true });

      const next = toSignInResult(await google.signIn());
      setResult(next);
      if (next.status === 'success') {
        onSuccess?.(next.idToken);
      }
    } catch (error) {
      setResult(toSignInError(error));
    }
  }, [onSuccess]);

  return {
    signIn,
    result,
    /** Нативный модуль всегда готов — отдельного ожидания запроса больше нет. */
    isReady: isGoogleConfigured,
  };
}
