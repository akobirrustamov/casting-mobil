import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
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
import {
  AuthError,
  exchangeGoogleToken,
  registerStart,
  signInWithPassword,
} from '@/features/auth/api';
import { authErrorKey, googleErrorKey, technicalDetail } from '@/features/auth/authErrors';
import { BuildMarker } from '@/features/auth/BuildMarker';
import type { DevLoginResult } from '@/features/auth/devLogin';
import { GoogleSignInButton } from '@/features/auth/GoogleSignInButton';
import { useAuthStore } from '@/features/auth/store';
import { colors } from '@/theme/tokens';

/**
 * S03 — вход и регистрация. Два раздела на одном экране (заказчик,
 * 01.09.2026): «Kirish» — номер и пароль, «Ro'yxatdan o'tish» — номер,
 * SMS-код, имя и пароль с повтором.
 *
 * <h2>Почему один экран с переключателем, а не два</h2>
 * Поле номера одно и то же, логотип один и тот же. Два отдельных экрана
 * означали бы прыжок с перерисовкой всего ради одного лишнего поля —
 * и человек терял бы уже набранный номер на переходе. Здесь номер
 * ПЕРЕЖИВАЕТ переключение: если регистрация ответила «номер занят»,
 * он же остаётся во вкладке входа.
 *
 * <h2>Раскладка</h2>
 * Знак вверху, форма в середине (только она и прокручивается), главная
 * кнопка внизу — на всех трёх экранах входа одинаково.
 *
 * <h2>Куда ведут кнопки</h2>
 * Вход — сразу `/(tabs)`. Регистрация — `otp.tsx` (код) → `password.tsx`
 * (имя, пароль и повтор) → `/(tabs)`. Сессия выдаётся на последнем шаге,
 * второй раз входить не нужно.
 *
 * ⚠️ «Забыли пароль» намеренно неактивна: эндпоинта восстановления на
 * бэкенде нет. Живая ссылка в никуда хуже честно погашенной.
 */
const PHONE_DIGITS = 9; // после +998

type Mode = 'signIn' | 'signUp';

export default function SignInScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const signIn = useAuthStore((s) => s.signIn);

  const [mode, setMode] = useState<Mode>('signIn');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [googleError, setGoogleError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** Техническая причина — временно, на время разбора входа. */
  const [errorDetail, setErrorDetail] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const digits = phone.replace(/\D/g, '');
  const isPhoneValid = digits.length === PHONE_DIGITS;
  const canSubmit = mode === 'signIn' ? isPhoneValid && password.length > 0 : isPhoneValid;

  const switchMode = (next: Mode) => {
    if (next === mode) return;
    setMode(next);
    setError(null);
  };

  const onChangePhone = (raw: string) => {
    setPhone(formatPhone(raw.replace(/\D/g, '').slice(0, PHONE_DIGITS)));
    setError(null);
  };

  const onChangePassword = (value: string) => {
    setPassword(value);
    setError(null);
  };

  /** Вход: номер и пароль, без SMS. */
  const onSignIn = async () => {
    setError(null);
    setBusy(true);
    try {
      const session = await signInWithPassword(`+998${digits}`, password);
      await signIn(session.token, session.user, session.refreshToken);
      router.replace('/(tabs)');
    } catch (e) {
      setError(t(authErrorKey(e)));
      setErrorDetail(technicalDetail(e) ?? null);
    } finally {
      setBusy(false);
    }
  };

  /**
   * Регистрация: просим код.
   *
   * ⚠️ Занятый номер бэкенд отбивает ДО отправки SMS
   * (`PHONE_ALREADY_REGISTERED`). Тогда экран сам переключается на вход
   * с уже набранным номером — человеку остаётся только пароль.
   */
  const onSignUp = async () => {
    const fullPhone = `+998${digits}`;
    setError(null);
    setBusy(true);
    try {
      await registerStart(fullPhone);
      router.push({ pathname: '/(auth)/otp', params: { phone: fullPhone } });
    } catch (e) {
      const message = t(authErrorKey(e));
      if (e instanceof AuthError && e.code === 'PHONE_ALREADY_REGISTERED') {
        setMode('signIn');
      }
      setError(message);
      setErrorDetail(technicalDetail(e) ?? null);
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
      setErrorDetail(technicalDetail(e) ?? null);
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
      <GlowBackdrop intensity="hero" decor />

      {/*
        Три яруса, и порядок задан заказчиком (01.09.2026): знак ВВЕРХУ,
        кнопка ВСЕГДА ВНИЗУ, форма между ними.

        ⚠️ Знак и кнопка вынесены ИЗ прокрутки. Пока они лежали в общем
        потоке, оба уезжали: на низком экране с открытой клавиатурой
        человек видел середину формы без знака сверху и без кнопки
        снизу — экран выглядел обрезанным с обеих сторон.

        Теперь прокручивается только середина, а края стоят на месте:
        куда смотреть и что нажать — видно всегда.
      */}
      <View className="items-center pb-1 pt-2">
        <Wordmark variant="stacked" markSize={92} showTagline={false} />
      </View>

      <ScrollView
        className="flex-1"
        contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <View className="gap-4 px-6 py-2">
          <ModeSwitch mode={mode} onChange={switchMode} disabled={busy} />

          {/* Заголовок убран намеренно: раздел уже назван в переключателе
              прямо над ним, и второй такой же заголовок только удлинял
              экран. Осталась строка, которая говорит, ЧТО делать. */}
          <Text className="text-center text-caption text-text-muted">
            {mode === 'signIn' ? t('auth.signInSubtitle') : t('auth.signUpSubtitle')}
          </Text>

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
              className="flex-1 text-h2"
              style={{ color: colors.white }}
            />
          </View>

          {/* Пароль — только во входе. На регистрации его задают после
              SMS: до подтверждения номера аккаунта ещё нет. */}
          {mode === 'signIn' ? (
            <View className="gap-2">
              <PasswordField
                value={password}
                onChangeText={onChangePassword}
                placeholder={t('auth.passwordPlaceholder')}
                valid={password.length > 0}
                editable={!busy}
                onSubmitEditing={canSubmit && !busy ? onSignIn : undefined}
              />

              {/* ⚠️ Отключена: восстановления пароля на бэкенде пока нет. */}
              <Pressable disabled hitSlop={8} className="self-end">
                <Text className="text-caption" style={{ color: colors.textDisabled }}>
                  {t('auth.forgotPassword')} · {t('auth.soon')}
                </Text>
              </Pressable>
            </View>
          ) : null}

          <View className="flex-row items-center gap-3">
            <View className="h-px flex-1 bg-border" />
            <View className="rounded-pill border border-border px-4 py-1.5">
              <Text className="text-caption text-text-muted">{t('auth.or')}</Text>
            </View>
            <View className="h-px flex-1 bg-border" />
          </View>

          <GoogleSignInButton onSuccess={onGoogleSuccess} onDevSession={onDevSession} />

          {googleError ? (
            <Text className="text-center text-caption text-danger">{googleError}</Text>
          ) : null}

          <BuildMarker />

          <Text className="text-center text-caption text-text-muted">
            <Text className="text-cyan underline">{t('auth.termsLink')}</Text>
            {' ' + t('auth.consentMiddle') + ' '}
            <Text className="text-cyan underline">{t('auth.privacyLink')}</Text>
            {' ' + t('auth.consentTail')}
          </Text>
        </View>
      </ScrollView>

      {/* Главное действие — последним и всегда на виду. */}
      <View
        className="gap-3 px-6 pt-3"
        style={{ paddingBottom: insets.bottom + 12 }}
      >
        {error ? (
          <Text className="text-center text-caption text-danger">{error}</Text>
        ) : null}
        {errorDetail ? (
          <Text className="text-center text-caption text-text-muted opacity-60">
            {errorDetail}
          </Text>
        ) : null}

        <Button
          variant="primary"
          shape="card"
          loading={busy}
          disabled={!canSubmit || busy}
          onPress={mode === 'signIn' ? onSignIn : onSignUp}
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
          {mode === 'signIn' ? t('auth.signInAction') : t('auth.continue')}
        </Button>
      </View>
    </KeyboardAvoidingView>
  );
}

/**
 * Переключатель разделов.
 *
 * Активный лежит на фирменном градиенте — том же, что у главной кнопки:
 * на экране должно быть видно, ЧТО именно сделает нижняя кнопка.
 */
function ModeSwitch({
  mode,
  onChange,
  disabled,
}: {
  mode: Mode;
  onChange: (mode: Mode) => void;
  disabled: boolean;
}) {
  const { t } = useTranslation();

  const tabs: { key: Mode; label: string }[] = [
    { key: 'signIn', label: t('auth.tabSignIn') },
    { key: 'signUp', label: t('auth.tabSignUp') },
  ];

  return (
    <View
      className="flex-row rounded-card-lg border p-1"
      style={{ borderColor: colors.border, backgroundColor: colors.surface }}
    >
      {tabs.map((tab) => {
        const active = tab.key === mode;
        return (
          <Pressable
            key={tab.key}
            onPress={() => onChange(tab.key)}
            disabled={disabled}
            className="flex-1 overflow-hidden rounded-card"
            style={{ minHeight: 44 }}
          >
            {/* Активная вкладка — сплошной фиолетовый, как у кнопок
                (заказчик 01.09.2026). Раньше здесь был фирменный градиент,
                и переключатель на экране входа оказывался единственным
                местом с ним. */}
            {active ? (
              <View
                style={{
                  position: 'absolute',
                  top: 0,
                  right: 0,
                  bottom: 0,
                  left: 0,
                  backgroundColor: colors.purple,
                }}
              />
            ) : null}

            <Text
              className="py-3 text-center text-caption"
              style={{ color: active ? colors.white : colors.textMuted }}
            >
              {tab.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
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
