import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { GlowBackdrop } from '@/components/ui/GlowBackdrop';
import { sendOtp, verifyOtp } from '@/features/auth/api';
import { otpErrorKey } from '@/features/auth/otpErrors';
import { useAuthStore } from '@/features/auth/store';
import { colors } from '@/theme/tokens';

/**
 * Ввод кода из SMS (Eskiz), `POST /api/v1/auth/otp/verify` — см. docs/API.md §5.
 *
 * Проверка и вход — один запрос: бэкенд сам находит хозяина номера или
 * создаёт нового пользователя, поэтому отдельного «регистрации» тут нет.
 */
const CODE_LENGTH = 4;

/**
 * Столько бэкенд не даёт слать код повторно (`app.otp.resend-cooldown-seconds`).
 * Если на сервере значение другое, кнопка просто получит OTP_COOLDOWN и
 * покажет текст ошибки — счётчик здесь только чтобы не жать зря.
 */
const RESEND_COOLDOWN_SECONDS = 120;

export default function OtpScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const { phone } = useLocalSearchParams<{ phone?: string }>();
  const signIn = useAuthStore((s) => s.signIn);

  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(RESEND_COOLDOWN_SECONDS);

  const isValid = code.length === CODE_LENGTH;
  const canResend = secondsLeft === 0 && !resending && !verifying;

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const id = setTimeout(() => setSecondsLeft(secondsLeft - 1), 1000);
    return () => clearTimeout(id);
  }, [secondsLeft]);

  const onVerify = async () => {
    if (!phone) return;
    setError(null);
    setVerifying(true);
    try {
      const { token, user } = await verifyOtp(phone, code);
      await signIn(token, user);
      router.replace('/(tabs)');
    } catch (e) {
      setError(t(otpErrorKey(e)));
      setCode('');
    } finally {
      setVerifying(false);
    }
  };

  const onResend = async () => {
    if (!phone) return;
    setError(null);
    setResending(true);
    try {
      await sendOtp(phone);
      setCode('');
      setSecondsLeft(RESEND_COOLDOWN_SECONDS);
    } catch (e) {
      setError(t(otpErrorKey(e)));
    } finally {
      setResending(false);
    }
  };

  return (
    <View
      className="flex-1 bg-ink px-6"
      style={{ paddingTop: insets.top + 8, paddingBottom: insets.bottom + 16 }}
    >
      <GlowBackdrop intensity="hero" />

      <Pressable onPress={() => router.back()} hitSlop={12} className="w-10 py-2">
        <Ionicons name="arrow-back" size={24} color={colors.white} />
      </Pressable>

      <View className="flex-1 justify-center gap-8">
        <View className="gap-2">
          <Text className="text-center text-h2 text-text">{t('auth.otpTitle')}</Text>
          {phone ? (
            <Text className="text-center text-caption text-text-muted">
              {t('auth.otpSubtitle', { phone })}
            </Text>
          ) : null}
        </View>

        <TextInput
          value={code}
          onChangeText={(raw) => {
            setCode(raw.replace(/\D/g, '').slice(0, CODE_LENGTH));
            setError(null);
          }}
          keyboardType="number-pad"
          inputMode="numeric"
          maxLength={CODE_LENGTH}
          autoFocus
          editable={!verifying}
          className="rounded-card border bg-surface py-4 text-center text-h1"
          style={{
            color: colors.white,
            letterSpacing: 12,
            // Тот же сигнал, что и на экране с номером: поле заполнено.
            borderColor: isValid ? colors.blue : colors.border,
          }}
        />

        {error ? (
          <Text className="text-center text-caption text-danger">{error}</Text>
        ) : null}

        {/* Повторная отправка открывается только после паузы на бэкенде */}
        <Pressable onPress={onResend} disabled={!canResend} hitSlop={8}>
          <Text
            className="text-center text-caption"
            style={{ color: canResend ? colors.cyan : colors.textMuted }}
          >
            {secondsLeft > 0
              ? t('auth.resendIn', { seconds: secondsLeft })
              : t('auth.resend')}
          </Text>
        </Pressable>
      </View>

      <Button
        variant="primary"
        shape="card"
        loading={verifying}
        disabled={!isValid || verifying}
        onPress={onVerify}
      >
        {t('auth.continue')}
      </Button>
    </View>
  );
}
