import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, TextInput, View } from 'react-native';

import { registerConfirm, registerStart } from '@/features/auth/api';
import { AuthScaffold } from '@/features/auth/AuthScaffold';
import { authErrorKey } from '@/features/auth/authErrors';
import { colors } from '@/theme/tokens';

/**
 * Регистрация, шаг 2 из 3: код из SMS (Eskiz),
 * `POST /api/v1/app/auth/register/confirm` — см. docs/API.md §5.
 *
 * <h2>⚠️ Здесь НЕ входят</h2>
 * Раньше этот экран звал `otp/verify` и сразу выдавал сессию: код был
 * и подтверждением, и входом одновременно. Теперь SMS подтверждает
 * ТОЛЬКО владение номером — аккаунта ещё нет, пароля тоже. Отсюда
 * дорога одна: `password.tsx`.
 *
 * Вход по паролю сюда вообще не заходит.
 *
 * <h2>Раскладка — как на экране входа</h2>
 * Заказчик (01.09.2026): «sms kod sahifasini ham dizayni login
 * pagenikidek qil», и (03.09.2026) — чтобы поле стояло ТАМ ЖЕ, где поле
 * номера, и не шевелилось. Отсюда общий каркас `AuthScaffold`: он держит
 * знак, шапку в слоте постоянной высоты, поля и кнопку внизу. Шаг
 * регистрации не должен выглядеть экраном из другого приложения — и не
 * должен заставлять искать поле глазами заново.
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
  const { phone } = useLocalSearchParams<{ phone?: string }>();

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
      await registerConfirm(phone, code);
      // Замена, а не push: назад к уже использованному коду возвращаться
      // некуда — он одноразовый.
      router.replace({ pathname: '/(auth)/password', params: { phone } });
    } catch (e) {
      setError(t(authErrorKey(e)));
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
      await registerStart(phone);
      setCode('');
      setSecondsLeft(RESEND_COOLDOWN_SECONDS);
    } catch (e) {
      setError(t(authErrorKey(e)));
    } finally {
      setResending(false);
    }
  };

  return (
    <AuthScaffold
      onBack={() => router.back()}
      header={
        <View className="gap-2">
          <Text className="text-center text-h2 text-text">{t('auth.otpTitle')}</Text>
          {phone ? (
            <Text
              numberOfLines={2}
              className="text-center text-caption text-text-muted"
            >
              {t('auth.otpSubtitle', { phone })}
            </Text>
          ) : null}
        </View>
      }
      message={error}
      action={{
        label: t('auth.continue'),
        onPress: onVerify,
        loading: verifying,
        disabled: !isValid || verifying,
      }}
    >
      {/* Карточка поля — та же, что у номера и пароля на входе:
          квадрат со знаком, разделитель, ввод. Отличается только
          сам ввод: цифры крупные и разрежены, чтобы код читался
          как код, а не как обычная строка. */}
      <View
        className="flex-row items-center gap-3 rounded-card-lg border bg-surface p-2.5"
        style={{ borderColor: isValid ? colors.blue : colors.border }}
      >
        <View
          className="items-center justify-center rounded-card"
          style={{ width: 44, height: 44, backgroundColor: `${colors.purple}26` }}
        >
          <Ionicons name="chatbox-ellipses" size={20} color={colors.magenta} />
        </View>

        <View className="h-7 w-px" style={{ backgroundColor: colors.border }} />

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
          className="flex-1 text-h1"
          style={{ color: colors.white, letterSpacing: 10 }}
        />
      </View>

      {/* Повторная отправка открывается только после паузы на бэкенде.

          ⚠️ Строка одна и та же по высоте и со счётчиком, и без него —
          поле кода над ней не шевелится, пока идёт обратный отсчёт. */}
      <Pressable onPress={onResend} disabled={!canResend} hitSlop={8}>
        <Text
          numberOfLines={1}
          className="text-center text-caption"
          style={{ color: canResend ? colors.cyan : colors.textMuted }}
        >
          {secondsLeft > 0
            ? t('auth.resendIn', { seconds: secondsLeft })
            : t('auth.resend')}
        </Text>
      </Pressable>
    </AuthScaffold>
  );
}
