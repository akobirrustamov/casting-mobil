import { Ionicons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

import { isGoogleConfigured } from './config';
import { useGoogleSignIn } from './useGoogleSignIn';

/**
 * Кнопка входа через Google.
 *
 * Хук useIdTokenAuthRequest бросает исключение, если client ID не задан,
 * а условно вызывать хуки нельзя — поэтому вся работа с ним живёт
 * в отдельном компоненте, и он монтируется только когда ключи есть.
 * Иначе показываем неактивную заглушку.
 */
export function GoogleSignInButton({
  onSuccess,
}: {
  onSuccess?: (idToken: string) => void;
}) {
  if (!isGoogleConfigured) return <GoogleButtonPlaceholder />;
  return <GoogleButtonLive onSuccess={onSuccess} />;
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
      className={`flex-row items-center justify-center gap-3 rounded-pill border border-border py-3.5 ${
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
