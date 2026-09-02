import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
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
import { Wordmark } from '@/components/ui/Wordmark';
import { registerConfirm, registerStart } from '@/features/auth/api';
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
 * pagenikidek qil». Те же три яруса: знак ВВЕРХУ, поле в карточке с
 * квадратным знаком слева — в середине, кнопка ВНИЗУ. Шаг регистрации
 * не должен выглядеть экраном из другого приложения.
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
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      className="flex-1 bg-ink"
      style={{ paddingTop: insets.top }}
    >
      <GlowBackdrop intensity="hero" decor />

      {/* Ярусы те же, что на входе: знак вверху, кнопка внизу, между
          ними — единственная прокручиваемая часть. */}
      <Pressable
        onPress={() => router.back()}
        hitSlop={12}
        className="ml-6 w-10 py-2"
        accessibilityRole="button"
      >
        <Ionicons name="arrow-back" size={24} color={colors.white} />
      </Pressable>

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
            <Text className="text-center text-h2 text-text">{t('auth.otpTitle')}</Text>
            {phone ? (
              <Text className="text-center text-caption text-text-muted">
                {t('auth.otpSubtitle', { phone })}
              </Text>
            ) : null}
          </View>

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
      </ScrollView>

      <View className="gap-3 px-6 pt-3" style={{ paddingBottom: insets.bottom + 12 }}>
        {error ? (
          <Text className="text-center text-caption text-danger">{error}</Text>
        ) : null}

        <Button
          variant="primary"
          shape="card"
          loading={verifying}
          disabled={!isValid || verifying}
          onPress={onVerify}
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
          {t('auth.continue')}
        </Button>
      </View>
    </KeyboardAvoidingView>
  );
}
