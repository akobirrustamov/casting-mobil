import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, TextInput, View } from 'react-native';

import { FormMessage } from '@/components/ui/FormMessage';
import { PasswordField } from '@/components/ui/PasswordField';
import { registerComplete } from '@/features/auth/api';
import { AuthScaffold } from '@/features/auth/AuthScaffold';
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
 * <h2>Раскладка</h2>
 * Общий каркас `AuthScaffold` — тот же, что на входе и на экране кода:
 * поля начинаются на той же высоте, кнопка стоит на том же месте.
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
    <AuthScaffold
      // Назад — к вводу номера: код уже использован, возвращаться к
      // экрану кода незачем.
      onBack={() => router.replace('/(auth)/sign-in')}
      header={
        <View className="gap-2">
          <Text className="text-center text-h2 text-text">
            {t('auth.setPasswordTitle')}
          </Text>
          <Text
            numberOfLines={2}
            className="text-center text-caption text-text-muted"
          >
            {t('auth.setPasswordSubtitle', { min: MIN_LENGTH })}
          </Text>
        </View>
      }
      message={error}
      action={{
        label: t('auth.finishSignUp'),
        onPress: onSubmit,
        loading: saving,
        disabled: !canSubmit,
      }}
    >
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

      {/* Подсказка тоже стоит в отведённом месте: она загорается прямо
          во время набора, и без брони поля прыгали бы под пальцами на
          каждой шестой букве пароля. */}
      <FormMessage message={hint} tone="muted" lines={1} />
    </AuthScaffold>
  );
}
