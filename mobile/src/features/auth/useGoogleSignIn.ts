// Именно строчными: публичный вход пакета — providers/google.
// С заглавной резолвится только на Windows и падает в Metro.
import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';
import { useCallback, useEffect, useState } from 'react';

import { GOOGLE_CLIENT_IDS, isGoogleConfigured } from './config';

// Закрывает вкладку авторизации, когда Google вернул нас в приложение.
WebBrowser.maybeCompleteAuthSession();

export type GoogleSignInResult =
  | { status: 'idle' }
  | { status: 'pending' }
  | { status: 'cancelled' }
  | { status: 'error'; message: string }
  | { status: 'success'; idToken: string };

/**
 * Вход через Google.
 *
 * Реализация на expo-auth-session: браузерный OAuth, без нативных модулей.
 * Отдаёт ID-токен, который потом уходит на бэкенд для верификации и обмена
 * на наш токен сессии.
 *
 * ⚠️ В Expo Go не работает — Google не принимает redirect на схему Expo Go.
 * Нужен dev build. Подробности и настройка клиентов — docs/GOOGLE_AUTH.md.
 *
 * TODO: когда появится эндпоинт (см. docs/API.md §5), обменивать idToken
 * на токен сессии и класть в useAuthStore.signIn().
 */
export function useGoogleSignIn(onSuccess?: (idToken: string) => void) {
  const [result, setResult] = useState<GoogleSignInResult>({ status: 'idle' });

  const [request, response, promptAsync] = Google.useIdTokenAuthRequest({
    webClientId: GOOGLE_CLIENT_IDS.web,
    androidClientId: GOOGLE_CLIENT_IDS.android,
    iosClientId: GOOGLE_CLIENT_IDS.ios,
  });

  useEffect(() => {
    if (!response) return;

    if (response.type === 'success') {
      const idToken = response.params?.id_token;
      if (idToken) {
        setResult({ status: 'success', idToken });
        onSuccess?.(idToken);
      } else {
        setResult({ status: 'error', message: 'Google не вернул id_token' });
      }
      return;
    }

    if (response.type === 'dismiss' || response.type === 'cancel') {
      setResult({ status: 'cancelled' });
      return;
    }

    if (response.type === 'error') {
      setResult({
        status: 'error',
        message: response.error?.message ?? 'Ошибка авторизации Google',
      });
    }
  }, [response, onSuccess]);

  const signIn = useCallback(async () => {
    if (!isGoogleConfigured) {
      setResult({
        status: 'error',
        message: 'Google client ID не настроен — см. docs/GOOGLE_AUTH.md',
      });
      return;
    }

    setResult({ status: 'pending' });
    await promptAsync();
  }, [promptAsync]);

  return {
    signIn,
    result,
    /** Запрос ещё не готов — кнопку держим неактивной. */
    isReady: Boolean(request) && isGoogleConfigured,
  };
}
