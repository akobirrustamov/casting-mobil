import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, TextInput, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { FormMessage } from '@/components/ui/FormMessage';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { formatDate } from '@/features/profile/api';
import {
  promoErrorKey,
  useMyPromocodes,
  useRedeemPromocode,
  type RedeemResult,
} from '@/features/promocode/api';
import { colors } from '@/theme/tokens';

/**
 * Промокод — ввод кода и список уже использованных.
 *
 * <h2>Что даёт код — зависит от кода</h2>
 * Админ выбирает при создании: дни Premium или дни доступа к разделу
 * Casting. Экран показывает РАЗНЫЙ текст: «30 kun Premium qo'shildi» и
 * «7 kun Casting ochildi» — разные обещания, и человек с casting-кодом
 * не должен ждать, что откроются фильмы.
 *
 * <h2>⚠️ Ошибки названы по имени</h2>
 * Пять причин отказа — пять разных текстов. «Siz allaqachon
 * ishlatgansiz» и «Bunday promokod topilmadi» приводят к разным
 * действиям человека; общее «не подошло» заставляло бы его набирать
 * один и тот же код снова.
 *
 * <h2>Ввод — верхним регистром</h2>
 * Бэкенд всё равно приводит к верхнему, но человек должен видеть то же,
 * что видит сервер: код на баннере напечатан заглавными.
 */
const MIN_LENGTH = 3;
const MAX_LENGTH = 32;

export default function PromocodeScreen() {
  const { t } = useTranslation();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);

  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<RedeemResult | null>(null);

  const redeem = useRedeemPromocode();
  const mine = useMyPromocodes(isAuthorized);

  const cleaned = code.trim();
  const canSubmit = cleaned.length >= MIN_LENGTH && !redeem.isPending;

  const onChange = (raw: string) => {
    // Пробелы убираем сразу: их легко поймать при вставке из мессенджера,
    // а сервер такой код не примет.
    setCode(raw.toUpperCase().replace(/\s+/g, '').slice(0, MAX_LENGTH));
    setError(null);
    setDone(null);
  };

  const onSubmit = async () => {
    setError(null);
    try {
      const result = await redeem.mutateAsync(cleaned);
      setDone(result);
      setCode('');
    } catch (e) {
      setError(t(promoErrorKey(e)));
    }
  };

  if (!isAuthorized) {
    return (
      <Screen title={t('promocode.title')} onBack={() => router.back()}>
        <ScreenState
          kind="locked"
          body={t('promocode.signInRequired')}
          actionLabel={t('profile.signIn')}
          onAction={() => router.push('/(auth)/sign-in')}
        />
      </Screen>
    );
  }

  return (
    <Screen
      title={t('promocode.title')}
      subtitle={t('promocode.subtitle')}
      onBack={() => router.back()}
    >
      <View
        className="flex-row items-center gap-3 rounded-card-lg border bg-surface p-2.5"
        style={{ borderColor: canSubmit ? colors.blue : colors.border }}
      >
        <View
          className="items-center justify-center rounded-card"
          style={{ width: 44, height: 44, backgroundColor: `${colors.purple}26` }}
        >
          <Ionicons name="pricetag" size={20} color={colors.magenta} />
        </View>

        <View className="h-7 w-px" style={{ backgroundColor: colors.border }} />

        <TextInput
          value={code}
          onChangeText={onChange}
          placeholder={t('promocode.placeholder')}
          placeholderTextColor={colors.textDisabled}
          autoCapitalize="characters"
          autoCorrect={false}
          maxLength={MAX_LENGTH}
          editable={!redeem.isPending}
          onSubmitEditing={canSubmit ? onSubmit : undefined}
          className="flex-1 text-h2"
          style={{ color: colors.white, letterSpacing: 2 }}
        />
      </View>

      {/* Место под сообщение отведено заранее — поле ввода над ним не
          прыгает, когда приходит ответ. */}
      <FormMessage message={error} tone="danger" lines={2} />

      <Button
        variant="primary"
        shape="card"
        onPress={onSubmit}
        loading={redeem.isPending}
        disabled={!canSubmit}
      >
        {t('promocode.activate')}
      </Button>

      {done ? <SuccessCard result={done} /> : null}

      {mine.data && mine.data.length > 0 ? (
        <View className="gap-3">
          <Text className="text-caption text-text-muted">{t('promocode.myTitle')}</Text>
          {mine.data.map((item) => (
            <View
              key={`${item.code}-${item.redeemedAt}`}
              className="flex-row items-center gap-3 rounded-card bg-surface p-4"
            >
              <View className="h-11 w-11 items-center justify-center rounded-pill bg-surface-2">
                <Ionicons name="pricetag-outline" size={18} color={colors.textMuted} />
              </View>
              <View className="flex-1 gap-0.5">
                <Text className="text-body text-text" style={{ letterSpacing: 1 }}>
                  {item.code}
                </Text>
                <Text className="text-caption text-text-muted">
                  {formatDate(item.redeemedAt) ?? ''}
                </Text>
              </View>
              <View className="items-end gap-1">
                <Text className="text-body font-semibold" style={{ color: colors.success }}>
                  +{t('promocode.days', { count: item.days })}
                </Text>
                <Text className="text-micro text-text-muted">
                  {t(`promocode.grant.${item.grantType}`)}
                </Text>
              </View>
            </View>
          ))}
        </View>
      ) : null}

      <Text className="text-caption text-text-muted">{t('promocode.note')}</Text>
    </Screen>
  );
}

/** Результат активации — что именно получено и до какого числа. */
function SuccessCard({ result }: { result: RedeemResult }) {
  const { t } = useTranslation();
  const until = formatDate(result.until);
  const casting = result.grantType === 'CASTING_DAYS';

  return (
    <View className="flex-row gap-3 rounded-card border border-success/40 bg-success/10 p-4">
      <Ionicons name="checkmark-circle" size={22} color={colors.success} />
      <View className="flex-1 gap-1">
        {/* ⚠️ Заголовок зависит от типа: «Premium qo'shildi» на
            casting-коде был бы обещанием, которого код не даёт. */}
        <Text className="text-body font-semibold text-text">
          {t(
            casting ? 'promocode.successCasting' : 'promocode.successPremium',
            { count: result.days },
          )}
        </Text>
        {until ? (
          <Text className="text-caption text-text-muted">
            {t('promocode.successUntil', { date: until })}
          </Text>
        ) : null}
        {casting ? (
          <Text className="text-caption text-text-muted">
            {t('promocode.castingOnly')}
          </Text>
        ) : null}
      </View>
    </View>
  );
}
