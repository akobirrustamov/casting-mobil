import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

import { isGoogleConfigured } from './config';
import { devLogin, devLoginPhone, isDevLoginEnabled, type DevLoginResult } from './devLogin';
import { useGoogleSignIn } from './useGoogleSignIn';

/**
 * Кнопка входа через Google.
 *
 * Хук useIdTokenAuthRequest бросает исключение, если client ID не задан,
 * а условно вызывать хуки нельзя — поэтому вся работа с ним живёт
 * в отдельном компоненте, и он монтируется только когда ключи есть.
 * Иначе показываем неактивную заглушку.
 *
 * <h2>Dev-режим</h2>
 * На локальном контуре настоящий Google недоступен: в Expo Go его redirect
 * в Google Cloud не зарегистрирован. Когда в `.env` задан dev-пользователь,
 * та же кнопка входит настоящим запросом `/auth/login` — и подписывается
 * так, чтобы это нельзя было принять за рабочий Google (см. `devLogin.ts`).
 */
export function GoogleSignInButton({
  onSuccess,
  onDevSession,
}: {
  onSuccess?: (idToken: string) => void;
  /** Вызывается вместо `onSuccess`, когда сработал dev-вход. */
  onDevSession?: (session: DevLoginResult) => void;
}) {
  if (isDevLoginEnabled) return <DevLoginButton onDevSession={onDevSession} />;
  if (!isGoogleConfigured) return <GoogleButtonPlaceholder />;
  return <GoogleButtonLive onSuccess={onSuccess} />;
}

/**
 * Обходная кнопка для локальной разработки.
 *
 * Подпись обязательна: без неё скриншот этого экрана читался бы как
 * «вход через Google работает», хотя он не работает.
 */
function DevLoginButton({
  onDevSession,
}: {
  onDevSession?: (session: DevLoginResult) => void;
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
      <Text className="text-center text-caption text-gold">
        {t('auth.devLogin', { phone: devLoginPhone ?? '' })}
      </Text>
      {error ? (
        <Text className="text-center text-caption text-danger">{error}</Text>
      ) : null}
    </View>
  );
}

function GoogleButtonPlaceholder() {
  const { t } = useTranslation();

  return (
    <View className="gap-2">
      <ButtonShell disabled label={t('auth.google')} />
      <Text className="text-center text-caption text-text-muted">
        {t('auth.googleUnavailable')}
      </Text>
    </View>
  );
}

function GoogleButtonLive({ onSuccess }: { onSuccess?: (idToken: string) => void }) {
  const { t } = useTranslation();
  const { signIn, result, isReady } = useGoogleSignIn(onSuccess);

  const pending = result.status === 'pending';

  return (
    <View className="gap-2">
      <ButtonShell
        label={t('auth.google')}
        disabled={!isReady || pending}
        loading={pending}
        onPress={signIn}
      />

      {result.status === 'error' ? (
        <Text className="text-center text-caption text-danger">{result.message}</Text>
      ) : null}
      {result.status === 'cancelled' ? (
        <Text className="text-center text-caption text-text-muted">
          {t('auth.cancelled')}
        </Text>
      ) : null}
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
      className={`flex-row items-center justify-center gap-3 rounded-card border border-border bg-surface py-4 ${
        disabled ? 'opacity-40' : 'active:opacity-70'
      }`}
    >
      {loading ? (
        <ActivityIndicator size="small" color={colors.white} />
      ) : (
        <Ionicons name="logo-google" size={19} color={colors.white} />
      )}
      <Text className="text-body font-semibold text-text">{label}</Text>
    </Pressable>
  );
}
