import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { Logo } from '@/components/ui/Logo';
import { exchangeGoogleToken } from '@/features/auth/api';
import { GoogleSignInButton } from '@/features/auth/GoogleSignInButton';
import { useAuthStore } from '@/features/auth/store';
import { colors } from '@/theme/tokens';

/**
 * Вход. Раскладка — как на экране Yangi.TV (docs/STRUCTURE.md §4):
 * один экран — одно действие, поле телефона с префиксом +998,
 * согласие с офертой ссылками и одна крупная кнопка внизу.
 *
 * Отличие от Yangi.TV: добавлен вход через Google — по решению заказчика.
 *
 * ⚠️ Отправки кода пока нет: эндпоинта OTP на бэкенде не существует
 * (docs/API.md §5). Кнопка ведёт на экран OTP-заглушку.
 */
const PHONE_DIGITS = 9; // после +998

export default function SignInScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const signIn = useAuthStore((s) => s.signIn);

  const [phone, setPhone] = useState('');

  const digits = phone.replace(/\D/g, '');
  const isPhoneValid = digits.length === PHONE_DIGITS;

  const onChangePhone = (raw: string) => {
    const next = raw.replace(/\D/g, '').slice(0, PHONE_DIGITS);
    setPhone(formatPhone(next));
  };

  const onContinue = () => {
    // TODO: POST на отправку OTP, когда появится эндпоинт
    router.push({ pathname: '/(auth)/otp', params: { phone: `+998${digits}` } });
  };

  const [googleError, setGoogleError] = useState<string | null>(null);

  const onGoogleSuccess = async (idToken: string) => {
    setGoogleError(null);
    try {
      const { token, user, phoneRequired } = await exchangeGoogleToken(idToken);
      await signIn(token, user);

      // Аккаунт создан через Google — телефона ещё нет.
      // По ТЗ телефон обязателен, поэтому досылаем его следующим шагом.
      if (phoneRequired) {
        router.replace('/(auth)/phone-link');
      } else {
        router.replace('/(tabs)');
      }
    } catch (e) {
      setGoogleError(
        e instanceof Error ? e.message : 'Не удалось войти через Google'
      );
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      className="flex-1 bg-ink"
      style={{ paddingTop: insets.top, paddingBottom: insets.bottom + 16 }}
    >
      <View className="flex-1 justify-center gap-8 px-6">
        <View className="items-center gap-4">
          <Logo size={72} />
          <Text className="text-center text-h2 text-text">
            {t('auth.phoneTitle')}
          </Text>
        </View>

        <View className="flex-row items-center gap-3 rounded-card bg-surface px-4">
          <Ionicons name="call-outline" size={20} color={colors.textMuted} />
          <Text className="text-body text-text">+998</Text>
          <TextInput
            value={phone}
            onChangeText={onChangePhone}
            placeholder={t('auth.phonePlaceholder')}
            placeholderTextColor={colors.textDisabled}
            keyboardType="phone-pad"
            inputMode="tel"
            maxLength={12}
            className="flex-1 py-4 text-body text-text"
            style={{ color: colors.white }}
          />
        </View>

        <View className="gap-4">
          <View className="flex-row items-center gap-3">
            <View className="h-px flex-1 bg-border" />
            <Text className="text-caption text-text-muted">{t('auth.or')}</Text>
            <View className="h-px flex-1 bg-border" />
          </View>

          <GoogleSignInButton onSuccess={onGoogleSuccess} />

          {googleError ? (
            <Text className="text-center text-caption text-danger">{googleError}</Text>
          ) : null}
        </View>
      </View>

      <View className="gap-4 px-6">
        <Text className="text-center text-caption text-text-muted">
          {t('auth.consentPrefix')}{' '}
          <Text className="text-cyan">{t('auth.termsLink')}</Text>{' '}
          {t('auth.consentMiddle')}{' '}
          <Text className="text-cyan">{t('auth.privacyLink')}</Text>{' '}
          {t('auth.consentSuffix')}
        </Text>

        <Button variant="primary" disabled={!isPhoneValid} onPress={onContinue}>
          {t('auth.continue')}
        </Button>

        <Pressable onPress={() => router.replace('/(tabs)')} hitSlop={8}>
          <Text className="text-center text-caption text-text-muted">
            {t('auth.skip')}
          </Text>
        </Pressable>
      </View>
    </KeyboardAvoidingView>
  );
}

/** 91 123 45 67 */
function formatPhone(digits: string): string {
  const parts = [
    digits.slice(0, 2),
    digits.slice(2, 5),
    digits.slice(5, 7),
    digits.slice(7, 9),
  ].filter(Boolean);
  return parts.join(' ');
}
