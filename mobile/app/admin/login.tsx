import { Ionicons } from '@expo/vector-icons';
import { Redirect, router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, TextInput, View } from 'react-native';

import { FormMessage } from '@/components/ui/FormMessage';
import { AuthScaffold } from '@/features/auth/AuthScaffold';
import { adminLogin, adminLoginErrorKey, toAdminAuthError } from '@/features/castingAdmin/auth';
import { useAdminStore } from '@/features/castingAdmin/store';
import { colors } from '@/theme/tokens';

/**
 * Вход сотрудника: телефон + пароль (`POST /api/v1/app/admin/auth/login`).
 *
 * Каркас — тот же `AuthScaffold`, что у входа пользователя: знакомая
 * раскладка, но другой заголовок, чтобы сотрудник не спутал, в какую
 * сессию входит.
 *
 * <h2>Номер — как есть</h2>
 * Бэкенд ищет сотрудника по точному совпадению строки телефона
 * (`userRepo.findByPhone`), поэтому маски «+998 и 9 цифр», как у входа по
 * SMS, здесь нет: убираем только пробелы, остальное — как ввёл человек.
 */
export default function AdminLoginScreen() {
  const { t } = useTranslation();
  const token = useAdminStore((s) => s.token);
  const expired = useAdminStore((s) => s.expired);
  const signIn = useAdminStore((s) => s.signIn);

  const [phone, setPhone] = useState('+998');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Уже вошёл (например, вернулся назад со списка) — сразу в список.
  if (token) return <Redirect href="/admin" />;

  const cleanPhone = phone.replace(/\s+/g, '');
  const canSubmit = cleanPhone.length >= 9 && password.length > 0 && !busy;

  const onSubmit = async () => {
    setError(null);
    setBusy(true);
    try {
      const session = await adminLogin(cleanPhone, password);
      await signIn(session.token, session.user);
      router.replace('/admin');
    } catch (e) {
      const err = toAdminAuthError(e);
      // При блокировке сервер называет причину и срок — они полезнее
      // нашего общего «попробуйте позже».
      const own = t(adminLoginErrorKey(err));
      setError(
        (err.code === 'ACCESS_DENIED' || err.code === 'ACCOUNT_LOCKED') && err.serverMessage
          ? `${own} ${err.serverMessage}`
          : own,
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthScaffold
      onBack={() => (router.canGoBack() ? router.back() : router.replace('/(auth)/sign-in'))}
      header={
        <View className="gap-2">
          <Text className="text-center text-h2 text-text">{t('castingAdmin.login.title')}</Text>
          <FormMessage
            message={expired && !error ? t('castingAdmin.login.expired') : t('castingAdmin.login.subtitle')}
            tone={expired && !error ? 'danger' : 'muted'}
          />
        </View>
      }
      message={error}
      action={{
        label: t('castingAdmin.login.submit'),
        onPress: onSubmit,
        loading: busy,
        disabled: !canSubmit,
      }}
    >
      <View className="gap-3">
        <InputRow icon="call">
          <TextInput
            value={phone}
            onChangeText={(v) => {
              setPhone(v);
              setError(null);
            }}
            placeholder={t('castingAdmin.login.phone')}
            placeholderTextColor={colors.textDisabled}
            keyboardType="phone-pad"
            autoComplete="tel"
            editable={!busy}
            maxLength={20}
            className="flex-1 text-body"
            style={{ color: colors.white }}
          />
        </InputRow>
        <InputRow icon="lock-closed">
          <TextInput
            value={password}
            onChangeText={(v) => {
              setPassword(v);
              setError(null);
            }}
            placeholder={t('castingAdmin.login.password')}
            placeholderTextColor={colors.textDisabled}
            secureTextEntry
            autoCapitalize="none"
            autoCorrect={false}
            autoComplete="password"
            editable={!busy}
            onSubmitEditing={canSubmit ? onSubmit : undefined}
            className="flex-1 text-body"
            style={{ color: colors.white }}
          />
        </InputRow>
      </View>
    </AuthScaffold>
  );
}

function InputRow({ icon, children }: { icon: keyof typeof Ionicons.glyphMap; children: React.ReactNode }) {
  return (
    <View className="flex-row items-center gap-3 rounded-card-lg border border-border bg-surface p-2.5">
      <View
        className="items-center justify-center rounded-card"
        style={{ width: 40, height: 40, backgroundColor: `${colors.purple}26` }}
      >
        <Ionicons name={icon} size={18} color={colors.magenta} />
      </View>
      {children}
    </View>
  );
}
