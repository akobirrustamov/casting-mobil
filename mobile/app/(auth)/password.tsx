import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { GlowBackdrop } from '@/components/ui/GlowBackdrop';
import { PasswordField } from '@/components/ui/PasswordField';
import { Wordmark } from '@/components/ui/Wordmark';
import { registerComplete } from '@/features/auth/api';
import { authErrorKey } from '@/features/auth/authErrors';
import { useAuthStore } from '@/features/auth/store';
import { colors } from '@/theme/tokens';

/**
 * Регистрация, шаг 3 из 3: имя, пароль и его повтор
 * (`POST /api/v1/app/auth/register/complete`).
 *
 * <h2>Почему имя спрашиваем здесь</h2>
 * Заказчик (01.09.2026) попросил добавить полное имя в регистрацию. До
 * SMS его класть нельзя: каждое поле перед кодом удлиняет путь к самому
 * коду. А на этом шаге форма уже открыта, и имя — одна строка в ней.
 *
 * Без имени аккаунт оставался безымянным: в профиле и под комментариями
 * зияла пустота, а «имя» подставить неоткуда — телефон именем не бывает.
 *
 * <h2>Почему повтор — обязательное поле, а не «покажи пароль»</h2>
 * Глаз в поле есть, и всё равно повтор нужен: «забыли пароль» пока
 * отключён, а пароль набирают на телефонной клавиатуре. Опечатка здесь
 * стоит аккаунта — второе поле ловит её до того, как она станет
 * настоящим паролем.
 *
 * <h2>Дальше — сразу главная</h2>
 * Ответ содержит готовую сессию: заставлять человека входить только что
 * заданным паролем было бы лишним шагом ради ничего.
 *
 * ⚠️ Номер подтверждён 15 минут (`app.otp.verified-ttl-seconds`). Если
 * человек ушёл надолго, бэкенд ответит `PHONE_NOT_VERIFIED` — экран
 * говорит это словами и возвращает к вводу номера.
 */
const MIN_LENGTH = 6; // как на бэкенде: AppAccountService.MIN_PASSWORD_LENGTH

/** Тоже как на бэкенде: AppAccountService.MIN_NAME_LENGTH. */
const MIN_NAME_LENGTH = 2;

export default function PasswordScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const { phone } = useLocalSearchParams<{ phone?: string }>();
  const signIn = useAuthStore((s) => s.signIn);

  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const trimmedName = name.trim();
  const isNameValid = trimmedName.length >= MIN_NAME_LENGTH;
  const isLongEnough = password.length >= MIN_LENGTH;
  const matches = confirm.length > 0 && confirm === password;
  const canSubmit = Boolean(phone) && isNameValid && isLongEnough && matches && !saving;

  /**
   * Подсказка под полями.
   *
   * Показываем ОДНУ причину, по которой кнопка ещё не активна, и только
   * когда человек уже что-то набрал: список требований на пустом экране
   * читается как выговор авансом.
   */
  const hint = (() => {
    if (name.length > 0 && !isNameValid) return t('auth.nameInvalid');
    if (password.length > 0 && !isLongEnough) return t('auth.passwordTooShort');
    if (confirm.length > 0 && !matches) return t('auth.passwordMismatch');
    return null;
  })();

  const onSubmit = async () => {
    if (!phone) return;
    setError(null);
    setSaving(true);
    try {
      const session = await registerComplete(phone, trimmedName, password, confirm);
      await signIn(session.token, session.user, session.refreshToken);
      router.replace('/(tabs)');
    } catch (e) {
      setError(t(authErrorKey(e)));
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      className="flex-1 bg-ink"
      style={{ paddingTop: insets.top }}
    >
      <GlowBackdrop intensity="hero" decor />

      {/* Назад — к вводу номера: код уже использован, возвращаться к
          экрану кода незачем. */}
      <Pressable
        onPress={() => router.replace('/(auth)/sign-in')}
        hitSlop={12}
        className="ml-6 w-10 py-2"
        accessibilityRole="button"
      >
        <Ionicons name="arrow-back" size={24} color={colors.white} />
      </Pressable>

      {/* Ярусы как на входе и на экране кода: знак вверху, поля в
          середине, кнопка внизу. */}
      <View className="items-center pb-1">
        <Wordmark variant="stacked" markSize={92} showTagline={false} />
      </View>

      <ScrollView
        className="flex-1"
        contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <View className="gap-4 px-6 py-2">
          <View className="gap-2">
            <Text className="text-center text-h2 text-text">
              {t('auth.setPasswordTitle')}
            </Text>
            <Text className="text-center text-caption text-text-muted">
              {t('auth.setPasswordSubtitle', { min: MIN_LENGTH })}
            </Text>
          </View>

          {/* Имя — в той же раскладке, что номер и пароль: квадрат со
              знаком, разделитель, ввод. Экраны входа и регистрации
              читаются как одна форма. */}
          <View
            className="flex-row items-center gap-3 rounded-card-lg border bg-surface p-2.5"
            style={{ borderColor: isNameValid ? colors.blue : colors.border }}
          >
            <View
              className="items-center justify-center rounded-card"
              style={{ width: 44, height: 44, backgroundColor: `${colors.purple}26` }}
            >
              <Ionicons name="person" size={20} color={colors.magenta} />
            </View>

            <View className="h-7 w-px" style={{ backgroundColor: colors.border }} />

            <TextInput
              value={name}
              onChangeText={(value) => {
                setName(value);
                setError(null);
              }}
              placeholder={t('auth.namePlaceholder')}
              placeholderTextColor={colors.textDisabled}
              autoCapitalize="words"
              autoComplete="name"
              textContentType="name"
              maxLength={60}
              editable={!saving}
              autoFocus
              className="flex-1 text-body"
              style={{ color: colors.white }}
            />
          </View>

          <PasswordField
            value={password}
            onChangeText={(value) => {
              setPassword(value);
              setError(null);
            }}
            placeholder={t('auth.passwordPlaceholder')}
            valid={isLongEnough}
            editable={!saving}
            isNew
          />

          <PasswordField
            value={confirm}
            onChangeText={(value) => {
              setConfirm(value);
              setError(null);
            }}
            placeholder={t('auth.passwordConfirmPlaceholder')}
            valid={matches}
            editable={!saving}
            isNew
            onSubmitEditing={canSubmit ? onSubmit : undefined}
          />

          {hint ? (
            <Text className="text-center text-caption" style={{ color: colors.textMuted }}>
              {hint}
            </Text>
          ) : null}
        </View>
      </ScrollView>

      <View className="gap-3 px-6 pt-3" style={{ paddingBottom: insets.bottom + 12 }}>
        {error ? (
          <Text className="text-center text-caption text-danger">{error}</Text>
        ) : null}

        <Button
          variant="primary"
          shape="card"
          loading={saving}
          disabled={!canSubmit}
          onPress={onSubmit}
          className="py-1"
          trailing={
            <Ionicons
              name="arrow-forward"
              size={20}
              color={colors.white}
              style={{ marginLeft: 6 }}
            />
          }
        >
          {t('auth.finishSignUp')}
        </Button>
      </View>
    </KeyboardAvoidingView>
  );
}
