import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, TextInput, View } from 'react-native';

import { FormMessage } from '@/components/ui/FormMessage';
import { exchangeGoogleToken, sendOtp } from '@/features/auth/api';
import { AuthScaffold } from '@/features/auth/AuthScaffold';
import { authErrorKey, googleErrorKey } from '@/features/auth/authErrors';
import type { DevLoginResult } from '@/features/auth/devLogin';
import { GoogleSignInButton } from '@/features/auth/GoogleSignInButton';
import { useAuthStore } from '@/features/auth/store';
import { PRIVACY_URL, TERMS_URL, openLegal } from '@/features/legal/links';
import { colors } from '@/theme/tokens';

/**
 * S03 — вход, шаг 1 из 2 (из 3 для новых): номер телефона.
 *
 * <h2>Один поток вместо двух разделов</h2>
 * Заказчик (04.09.2026): номер → SMS-код, а имя — только если человек
 * новый. Переключателя «Kirish / Ro'yxatdan o'tish» больше НЕТ, и
 * пароля тоже: номер всё равно подтверждался кодом, то есть пароль был
 * не вторым замком, а вторым шагом, который забывают.
 *
 * Заодно исчез самый неприятный тупик прежней схемы: человек выбирал
 * «регистрацию», получал «этот номер занят» и должен был сам понять,
 * что ему нужен соседний раздел. Теперь вопрос «ты новый или старый»
 * просто не задаётся — на него отвечает сервер, и только после кода.
 *
 * <h2>Раскладка</h2>
 * Ярусы задаёт общий каркас `AuthScaffold` — он же на экране кода и на
 * экране имени. Знак, высота шапки и место кнопки одинаковы на всех
 * трёх, поэтому поле не приходится искать глазами заново на каждом
 * шаге.
 *
 * <h2>Куда ведёт кнопка</h2>
 * `otp.tsx` (код) → либо сразу `/(tabs)`, либо `name.tsx` (имя) →
 * `/(tabs)`. Сессия выдаётся на последнем пройденном шаге, второй раз
 * входить не нужно.
 */
const PHONE_DIGITS = 9; // после +998

export default function SignInScreen() {
  const { t } = useTranslation();
  const signIn = useAuthStore((s) => s.signIn);

  const [phone, setPhone] = useState('');
  const [googleError, setGoogleError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const digits = phone.replace(/\D/g, '');
  const isPhoneValid = digits.length === PHONE_DIGITS;

  const onChangePhone = (raw: string) => {
    setPhone(formatPhone(raw.replace(/\D/g, '').slice(0, PHONE_DIGITS)));
    setError(null);
  };

  /**
   * Просим код.
   *
   * ⚠️ Ответа «этот номер занят» здесь БОЛЬШЕ НЕТ: занятый номер — это
   * просто входящий человек, и код уходит ему так же.
   */
  const onContinue = async () => {
    const fullPhone = `+998${digits}`;
    setError(null);
    setBusy(true);
    try {
      await sendOtp(fullPhone);
      router.push({ pathname: '/(auth)/otp', params: { phone: fullPhone } });
    } catch (e) {
      setError(t(authErrorKey(e)));
    } finally {
      setBusy(false);
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
      const { token, refreshToken, user } = await exchangeGoogleToken(idToken);
      await signIn(token, user, refreshToken);

      // Телефон после Google не спрашиваем: по ТЗ аккаунт можно создать
      // «telefon/email orqali», а соцвход указан как optional. Номер нужен
      // только для выплат — попросим его в Creator Studio, когда дойдём.
      // Бэкенд по-прежнему шлёт phone_required, но это подсказка, не запрет.
      router.replace('/(tabs)');
    } catch (e) {
      setGoogleError(t(googleErrorKey(e)));
    }
  };

  return (
    <AuthScaffold
      header={
        <View className="gap-2">
          <Text className="text-center text-h2 text-text">{t('auth.phoneTitle')}</Text>
          {/* Место под подпись отведено на две строки: на узком экране
              она переносится, и без брони поле номера уезжало бы вниз. */}
          <FormMessage message={t('auth.phoneSubtitle')} tone="muted" />
        </View>
      }
      message={error}
      action={{
        label: t('auth.continue'),
        onPress: onContinue,
        loading: busy,
        disabled: !isPhoneValid || busy,
      }}
      footer={
        <>
          <View className="flex-row items-center gap-3">
            <View className="h-px flex-1 bg-border" />
            <View className="rounded-pill border border-border px-4 py-1.5">
              <Text className="text-caption text-text-muted">{t('auth.or')}</Text>
            </View>
            <View className="h-px flex-1 bg-border" />
          </View>

          {/* Ошибка обмена токена показывается ВНУТРИ кнопки Google — там
              под сообщение уже отведено место, второе было бы лишней
              пустотой на экране. */}
          <GoogleSignInButton
            onSuccess={onGoogleSuccess}
            onDevSession={onDevSession}
            error={googleError}
          />

          {/*
            ⚠️ Ссылки были подчёркнуты синим, но НЕ нажимались.

            Это хуже, чем обычный текст: подчёркнутое синим человек считает
            ссылкой и жмёт, а экран не отвечает — приложение выглядит
            сломанным ровно в тот момент, когда у него просят согласие.

            Те же два адреса нужны экрану согласия Google, поэтому они
            лежат в одном месте — `features/legal/links`.
          */}
          <Text className="text-center text-caption text-text-muted">
            <Text
              className="text-cyan underline"
              accessibilityRole="link"
              onPress={() => void openLegal(TERMS_URL)}
            >
              {t('auth.termsLink')}
            </Text>
            {' ' + t('auth.consentMiddle') + ' '}
            <Text
              className="text-cyan underline"
              accessibilityRole="link"
              onPress={() => void openLegal(PRIVACY_URL)}
            >
              {t('auth.privacyLink')}
            </Text>
            {' ' + t('auth.consentTail')}
          </Text>
        </>
      }
    >
      {/* Рамка загорается синим — началом фирменной шкалы, — когда
          номер введён полностью.

          ⚠️ Выбора страны нет намеренно: OTP уходит через Eskiz, а он
          шлёт только на узбекские номера. Стрелка-раскрывашка с
          референса здесь была бы обещанием, которого бэкенд не держит. */}
      <View
        className="flex-row items-center gap-3 rounded-card-lg border bg-surface p-2.5"
        style={{ borderColor: isPhoneValid ? colors.blue : colors.border }}
      >
        <View
          className="items-center justify-center rounded-card"
          style={{ width: 44, height: 44, backgroundColor: `${colors.purple}26` }}
        >
          <Ionicons name="call" size={20} color={colors.magenta} />
        </View>

        <Text className="text-h2 text-text">+998</Text>
        <View className="h-7 w-px" style={{ backgroundColor: colors.border }} />

        <TextInput
          value={phone}
          onChangeText={onChangePhone}
          placeholder={t('auth.phonePlaceholder')}
          placeholderTextColor={colors.textDisabled}
          keyboardType="phone-pad"
          inputMode="tel"
          maxLength={12}
          editable={!busy}
          onSubmitEditing={isPhoneValid && !busy ? onContinue : undefined}
          className="flex-1 text-h2"
          style={{ color: colors.white }}
        />
      </View>
    </AuthScaffold>
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
