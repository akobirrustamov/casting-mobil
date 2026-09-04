import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, TextInput, View } from 'react-native';

import { FormMessage } from '@/components/ui/FormMessage';
import { completeOtp } from '@/features/auth/api';
import { AuthScaffold } from '@/features/auth/AuthScaffold';
import { authErrorKey } from '@/features/auth/authErrors';
import { useAuthStore } from '@/features/auth/store';
import { colors } from '@/theme/tokens';

/**
 * Вход, шаг 3 — ТОЛЬКО для новых: имя
 * (`POST /api/v1/app/auth/otp/complete`).
 *
 * <h2>Кто сюда попадает</h2>
 * Тот, у кого после проверки кода не оказалось аккаунта — или аккаунт
 * есть, но без имени (такие строки остались от старого `otp/verify` и
 * от входа через Google). Человек с именем этот экран не видит вообще:
 * он вошёл ещё на коде.
 *
 * <h2>Почему имя вообще спрашивают</h2>
 * Без него аккаунт оставался безымянным: в профиле и под комментариями
 * зияла пустота, а «имя» подставить неоткуда — телефон именем не бывает.
 * Аккаунт поэтому и создаётся здесь, вместе с именем, а не на шаге кода.
 *
 * <h2>Раскладка</h2>
 * Общий каркас `AuthScaffold` — тот же, что на номере и на коде: знак
 * на месте, поле начинается на той же высоте, кнопка стоит там же.
 * Заказчик (04.09.2026): последовательность экранов не должна ломать
 * дизайн.
 *
 * <h2>Дальше — сразу главная</h2>
 * Ответ содержит готовую сессию: заставлять человека входить второй раз
 * было бы лишним шагом ради ничего.
 *
 * ⚠️ Номер подтверждён 15 минут (`app.otp.verified-ttl-seconds`). Если
 * человек ушёл надолго, бэкенд ответит `PHONE_NOT_VERIFIED` — экран
 * говорит это словами и возвращает к вводу номера.
 */

/** Как на бэкенде: AppAccountService.MIN_NAME_LENGTH. */
const MIN_NAME_LENGTH = 2;

export default function NameScreen() {
  const { t } = useTranslation();
  const { phone } = useLocalSearchParams<{ phone?: string }>();
  const signIn = useAuthStore((s) => s.signIn);

  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const trimmedName = name.trim();
  const isNameValid = trimmedName.length >= MIN_NAME_LENGTH;
  const canSubmit = Boolean(phone) && isNameValid && !saving;

  /**
   * Подсказка под полем.
   *
   * Показываем её только когда человек уже что-то набрал: требование на
   * пустом экране читается как выговор авансом.
   */
  const hint = name.length > 0 && !isNameValid ? t('auth.nameInvalid') : null;

  const onSubmit = async () => {
    if (!phone) return;
    setError(null);
    setSaving(true);
    try {
      const session = await completeOtp(phone, trimmedName);
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
          <Text className="text-center text-h2 text-text">{t('auth.nameTitle')}</Text>
          <Text
            numberOfLines={2}
            className="text-center text-caption text-text-muted"
          >
            {t('auth.nameSubtitle')}
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
      {/* Имя — в той же раскладке, что номер и код: квадрат со знаком,
          разделитель, ввод. Три экрана читаются как одна форма. */}
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
          onSubmitEditing={canSubmit ? onSubmit : undefined}
          className="flex-1 text-body"
          style={{ color: colors.white }}
        />
      </View>

      {/* Подсказка тоже стоит в отведённом месте: она загорается прямо
          во время набора, и без брони поле прыгало бы под пальцами. */}
      <FormMessage message={hint} tone="muted" lines={1} />
    </AuthScaffold>
  );
}
