import {
  GoogleSignin,
  statusCodes,
  type SignInResponse,
} from '@react-native-google-signin/google-signin';
import { useCallback, useState } from 'react';

import { GOOGLE_CLIENT_IDS, isGoogleConfigured } from './config';

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
 * не доставляется** — нужна новая сборка.
 *
 * <h2>Про client ID</h2>
 * В `configure` уходит ТОЛЬКО веб-клиент: именно его id окажется в `aud`
 * выданного токена, и по нему бэкенд токен проверяет. Android-клиент
 * в коде не упоминается, но в консоли он обязан существовать — Google
 * узнаёт приложение по паре «package + SHA-1» из него. Без совпадения
 * SHA-1 вход не состоится, а по симптомам это неотличимо от поломки.
 */
GoogleSignin.configure({
  webClientId: GOOGLE_CLIENT_IDS.web,
  iosClientId: GOOGLE_CLIENT_IDS.ios,
});

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

  if (code === statusCodes.SIGN_IN_CANCELLED) {
    return { status: 'cancelled' };
  }
  if (code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
    return { status: 'error', messageKey: 'auth.googleNoPlayServices' };
  }
  return { status: 'error', messageKey: 'auth.googleFailed' };
}

export function useGoogleSignIn(onSuccess?: (idToken: string) => void) {
  const [result, setResult] = useState<GoogleSignInResult>({ status: 'idle' });

  const signIn = useCallback(async () => {
    if (!isGoogleConfigured) {
      setResult({ status: 'error', messageKey: 'auth.googleUnavailable' });
      return;
    }

    setResult({ status: 'pending' });

    try {
      // Проверка до показа окна: без сервисов Google Play вход невозможен,
      // и сказать об этом лучше сразу, а не пустым экраном.
      await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });

      const next = toSignInResult(await GoogleSignin.signIn());
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
