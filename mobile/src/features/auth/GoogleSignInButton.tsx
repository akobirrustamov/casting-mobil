import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, Pressable, Text, View } from 'react-native';

import { FormMessage } from '@/components/ui/FormMessage';
import { GoogleMark } from '@/components/ui/GoogleMark';
import { colors } from '@/theme/tokens';

import {
  googleUnavailableReason,
  isGoogleConfigured,
  type GoogleUnavailableReason,
} from './config';
import { devLogin, isDevLoginEnabled, type DevLoginResult } from './devLogin';
import { isGoogleNativeAvailable } from './googleModule';
import { useGoogleSignIn } from './useGoogleSignIn';

/**
 * Кнопка входа через Google.
 *
 * Работа с Google вынесена в отдельный компонент: он монтируется, только
 * когда client ID задан, иначе показываем неактивную заглушку.
 *
 * <h2>Dev-режим</h2>
 * На локальном контуре настоящий Google недоступен: в Expo Go его redirect
 * в Google Cloud не зарегистрирован. Когда в `.env` задан dev-пользователь,
 * та же кнопка входит настоящим запросом `/auth/login` под этим
 * пользователем (см. `devLogin.ts`). Сессия при этом настоящая —
 * подделки токена нет.
 */
export function GoogleSignInButton({
  onSuccess,
  onDevSession,
  error,
}: {
  onSuccess?: (idToken: string) => void;
  /** Вызывается вместо `onSuccess`, когда сработал dev-вход. */
  onDevSession?: (session: DevLoginResult) => void;
  /**
   * Ошибка ОБМЕНА токена — она случается уже у экрана, а не здесь.
   *
   * ⚠️ Приходит сюда, чтобы строка под кнопкой была ОДНА. Две строки
   * подряд (своя и экранная) значили бы два забронированных места под
   * сообщения, из которых почти всегда пустуют оба.
   */
  error?: string | null;
}) {
  if (isDevLoginEnabled) {
    return <DevLoginButton onDevSession={onDevSession} error={error} />;
  }

  // ⚠️ Порядок важен: сначала настройки, потом наличие нативной части.
  // Спрашивать `isGoogleNativeAvailable` первым значило бы грузить
  // модуль там, где кнопка всё равно погашена.
  if (!isGoogleConfigured) return <GoogleButtonPlaceholder />;
  if (!isGoogleNativeAvailable()) return <GoogleButtonPlaceholder reason="needsRebuild" />;

  return <GoogleButtonLive onSuccess={onSuccess} error={error} />;
}

/**
 * Обходная кнопка для локальной разработки.
 *
 * Под кнопкой была подпись «Dev-kirish: …» — заказчик попросил убрать:
 * экран идёт на скриншоты, служебная пометка там лишняя. Что это обход,
 * видно по `.env`: без `EXPO_PUBLIC_DEV_LOGIN_*` кнопки просто нет
 * (см. `devLogin.ts`).
 */
function DevLoginButton({
  onDevSession,
  error: externalError,
}: {
  onDevSession?: (session: DevLoginResult) => void;
  error?: string | null;
}) {
  const { t } = useTranslation();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onPress = async () => {
    setError(null);
    setBusy(true);
    try {
      onDevSession?.(await devLogin());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Dev-вход не удался');
    } finally {
      setBusy(false);
    }
  };

  return (
    <View className="gap-2">
      <ButtonShell
        label={t('auth.google')}
        disabled={busy}
        loading={busy}
        onPress={onPress}
      />
      <FormMessage message={error ?? externalError} />
    </View>
  );
}

/** Причина → строка под кнопкой. */
const REASON_KEY: Record<GoogleUnavailableReason, string> = {
  notConfigured: 'auth.googleUnavailable',
  expoGo: 'auth.googleNeedsDevBuild',
  needsRebuild: 'auth.googleNeedsRebuild',
};

/**
 * Неактивная кнопка и ЧЕСТНАЯ причина под ней.
 *
 * Причины три, и чинят их разные люди: ключей нет вовсе — вопрос к
 * `.env` и `eas.json`; Expo Go — нативных модулей туда не добавить; нет
 * нативной части — сборка старше самой зависимости, нужна новая APK.
 * Одинаковая подпись отправляла бы искать ключи там, где они уже
 * прописаны.
 */
function GoogleButtonPlaceholder({ reason }: { reason?: GoogleUnavailableReason }) {
  const { t } = useTranslation();
  const key = REASON_KEY[reason ?? googleUnavailableReason ?? 'notConfigured'];

  return (
    <View className="gap-2">
      <ButtonShell disabled label={t('auth.google')} />
      <FormMessage message={t(key)} tone="muted" />
    </View>
  );
}

function GoogleButtonLive({
  onSuccess,
  error: externalError,
}: {
  onSuccess?: (idToken: string) => void;
  error?: string | null;
}) {
  const { t } = useTranslation();
  const { signIn, result, isReady } = useGoogleSignIn(onSuccess);

  const pending = result.status === 'pending';

  // Своё сообщение важнее чужого: отказ Google объясняет причину точнее,
  // чем общая ошибка обмена токена на экране.
  const status: { message: string; tone: 'danger' | 'muted' } | null =
    result.status === 'error'
      ? { message: t(result.messageKey), tone: 'danger' }
      : result.status === 'cancelled'
        ? { message: t('auth.cancelled'), tone: 'muted' }
        : externalError
          ? { message: externalError, tone: 'danger' }
          : null;

  return (
    <View className="gap-2">
      <ButtonShell
        label={t('auth.google')}
        disabled={!isReady || pending}
        loading={pending}
        onPress={signIn}
      />

      <FormMessage message={status?.message} tone={status?.tone ?? 'danger'} />
    </View>
  );
}

function ButtonShell({
  label,
  disabled,
  loading,
  onPress,
}: {
  label: string;
  disabled?: boolean;
  loading?: boolean;
  onPress?: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled, busy: loading }}
      disabled={disabled}
      onPress={onPress}
      // Форма и фон совпадают с полем телефона — на макете это один ряд
      className={`flex-row items-center justify-center gap-3 rounded-card-lg border border-border bg-surface py-4 ${
        disabled ? 'opacity-40' : 'active:opacity-70'
      }`}
    >
      {/* Знак и кружок ожидания живут в одном месте ФИКСИРОВАННОГО
          размера: без него подпись сдвигалась бы на разницу их ширин в
          момент нажатия. */}
      <View className="items-center justify-center" style={{ width: 20, height: 20 }}>
        {loading ? (
          <ActivityIndicator size="small" color={colors.white} />
        ) : (
          <GoogleMark size={20} />
        )}
      </View>

      <Text className="text-body font-semibold text-text">{label}</Text>
    </Pressable>
  );
}
