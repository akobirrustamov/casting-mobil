import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { GlowBackdrop } from '@/components/ui/GlowBackdrop';
import { Wordmark } from '@/components/ui/Wordmark';
import { exchangeGoogleToken, sendOtp } from '@/features/auth/api';
import type { DevLoginResult } from '@/features/auth/devLogin';
import { GoogleSignInButton } from '@/features/auth/GoogleSignInButton';
import { otpErrorKey } from '@/features/auth/otpErrors';
import { useAuthStore } from '@/features/auth/store';
import { colors } from '@/theme/tokens';

/**
 * S03 — вход по телефону. Раскладка с макета заказчика:
 * логотип сверху, заголовок, поле +998 с разделителем, «yoki», Google,
 * согласие ссылками и одна крупная кнопка внизу.
 *
 * Принцип с подписи к макету: один экран — одно действие.
 *
 * Код отправляется через Eskiz SMS (`POST /api/v1/auth/otp/send`,
 * см. docs/API.md §5). Экран otp.tsx проверяет его и делает
 * вход/регистрацию одним запросом.
 */
const PHONE_DIGITS = 9; // после +998

export default function SignInScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const signIn = useAuthStore((s) => s.signIn);

  const [phone, setPhone] = useState('');
  const [googleError, setGoogleError] = useState<string | null>(null);
  const [otpError, setOtpError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  const digits = phone.replace(/\D/g, '');
  const isPhoneValid = digits.length === PHONE_DIGITS;

  const onChangePhone = (raw: string) => {
    setPhone(formatPhone(raw.replace(/\D/g, '').slice(0, PHONE_DIGITS)));
    setOtpError(null);
  };

  const onContinue = async () => {
    const fullPhone = `+998${digits}`;
    setOtpError(null);
    setSending(true);
    try {
      await sendOtp(fullPhone);
      router.push({ pathname: '/(auth)/otp', params: { phone: fullPhone } });
    } catch (e) {
      setOtpError(t(otpErrorKey(e)));
    } finally {
      setSending(false);
    }
  };

  /**
   * Dev-вход: токен и пользователь уже настоящие, менять нечего —
   * просто кладём сессию и уходим на главную.
   */
  const onDevSession = async ({ token, user }: DevLoginResult) => {
    await signIn(token, user);
    router.replace('/(tabs)');
  };

  const onGoogleSuccess = async (idToken: string) => {
    setGoogleError(null);
    try {
      const { token, user } = await exchangeGoogleToken(idToken);
      await signIn(token, user);

      // Телефон после Google не спрашиваем: по ТЗ аккаунт можно создать
      // «telefon/email orqali», а соцвход указан как optional. Номер нужен
      // только для выплат — попросим его в Creator Studio, когда дойдём.
      // Бэкенд по-прежнему шлёт phone_required, но это подсказка, не запрет.
      router.replace('/(tabs)');
    } catch (e) {
      setGoogleError(e instanceof Error ? e.message : 'Не удалось войти через Google');
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      className="flex-1 bg-ink"
      style={{ paddingTop: insets.top }}
    >
      {/* Свечение с референса заказчика: на пустом экране входа оно и
          делает всю картинку, поэтому здесь оно ярче обычного. */}
      <GlowBackdrop intensity="hero" />

      <ScrollView
        contentContainerStyle={{ flexGrow: 1, paddingBottom: insets.bottom + 16 }}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <View className="items-center pb-10 pt-10">
          <Wordmark size="lg" />
        </View>

        <View className="gap-6 px-6">
          <Text className="text-center text-h2 text-text">{t('auth.phoneTitle')}</Text>

          {/* Рамка загорается синим — началом фирменной шкалы, — когда
              номер введён полностью */}
          <View
            className="flex-row items-center gap-3 rounded-card border bg-surface px-4"
            style={{ borderColor: isPhoneValid ? colors.blue : colors.border }}
          >
            <Ionicons name="call-outline" size={18} color={colors.textMuted} />
            <Text className="text-body text-text">+998</Text>
            <View className="h-5 w-px" style={{ backgroundColor: colors.border }} />
            <TextInput
              value={phone}
              onChangeText={onChangePhone}
              placeholder={t('auth.phonePlaceholder')}
              placeholderTextColor={colors.textDisabled}
              keyboardType="phone-pad"
              inputMode="tel"
              maxLength={12}
              className="flex-1 py-4 text-body"
              style={{ color: colors.white }}
            />
          </View>

          <View className="flex-row items-center gap-3">
            <View className="h-px flex-1 bg-border" />
            <Text className="text-caption text-text-muted">— {t('auth.or')} —</Text>
            <View className="h-px flex-1 bg-border" />
          </View>

          <GoogleSignInButton
            onSuccess={onGoogleSuccess}
            onDevSession={onDevSession}
          />

          {googleError ? (
            <Text className="text-center text-caption text-danger">{googleError}</Text>
          ) : null}
        </View>

        {/* Прижимает согласие и кнопку к низу, как на макете */}
        <View className="flex-1" />

        <View className="gap-4 px-6 pt-8">
          <Text className="text-center text-caption text-text-muted">
            <Text className="text-cyan underline">{t('auth.termsLink')}</Text>
            {' ' + t('auth.consentMiddle') + ' '}
            <Text className="text-cyan underline">{t('auth.privacyLink')}</Text>
            {' ' + t('auth.consentTail')}
          </Text>

          {otpError ? (
            <Text className="text-center text-caption text-danger">{otpError}</Text>
          ) : null}

          <Button
            variant="primary"
            shape="card"
            loading={sending}
            disabled={!isPhoneValid || sending}
            onPress={onContinue}
          >
            {t('auth.continue')}
          </Button>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

/** 91 123 45 67 */
function formatPhone(digits: string): string {
  return [
    digits.slice(0, 2),
    digits.slice(2, 5),
    digits.slice(5, 7),
    digits.slice(7, 9),
  ]
    .filter(Boolean)
    .join(' ');
}
