import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { formatDate } from '@/features/profile/api';
import {
  mergeHistory,
  useMyDonations,
  useMySubscription,
  type PaymentEntry,
} from '@/features/subscription/api';
import { formatSum } from '@/lib/money';
import { colors } from '@/theme/tokens';

/**
 * История платежей — подписки и донаты одним списком.
 *
 * <h2>Два источника</h2>
 * Подписки приходят из `/app/me/subscription`, донаты — из
 * `/app/donations/my`. Склейка и сортировка — `mergeHistory`: общего
 * эндпоинта на сервере нет, и заводить его ради одного экрана значило бы
 * дублировать обе выборки.
 *
 * <h2>Подарок — не «0 сум»</h2>
 * У подписки от админа `paidAmount` пустой. Показать там ноль значило бы
 * сказать «купил бесплатно»; строка честно говорит «Sovg'a».
 */
export default function PaymentHistoryScreen() {
  const { t } = useTranslation();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);

  const subscription = useMySubscription();
  const donations = useMyDonations();

  const rows = useMemo(
    () => mergeHistory(subscription.data?.history ?? [], donations.data ?? []),
    [subscription.data, donations.data],
  );

  const loading = subscription.isLoading || donations.isLoading;
  const failed = subscription.isError && donations.isError;

  const refetch = () => {
    void subscription.refetch();
    void donations.refetch();
  };

  if (!isAuthorized) {
    return (
      <Screen title={t('subscription.historyTitle')} onBack={() => router.back()}>
        <ScreenState kind="locked" body={t('subscription.signInRequired')} />
      </Screen>
    );
  }

  return (
    <Screen
      title={t('subscription.historyTitle')}
      onBack={() => router.back()}
      onRefresh={refetch}
      refreshing={subscription.isRefetching || donations.isRefetching}
    >
      {loading ? <ScreenState kind="loading" /> : null}

      {/* Ошибка — только когда упали ОБА: один живой источник лучше
          пустого экрана с восклицательным знаком. */}
      {failed ? <ScreenState kind="error" onRetry={refetch} /> : null}

      {!loading && !failed && rows.length === 0 ? (
        <ScreenState kind="empty" body={t('subscription.historyEmpty')} />
      ) : null}

      <View className="gap-3">
        {rows.map((row) => (
          <HistoryRow key={row.key} row={row} />
        ))}
      </View>
    </Screen>
  );
}

function HistoryRow({ row }: { row: PaymentEntry }) {
  const { t } = useTranslation();

  if (row.kind === 'subscription') {
    const s = row.entry;
    const gift = s.source === 'ADMIN_GIFT';

    const title = s.tariffName ?? (gift ? t('subscription.sourceGift') : t('subscription.sourcePurchase'));
    const period = `${formatDate(s.startAt) ?? '—'} – ${formatDate(s.endAt) ?? '—'}`;

    const amount =
      s.paidAmount != null
        ? t('common.price', { amount: formatSum(s.paidAmount) })
        : t('subscription.gift');

    return (
      <Row
        icon={gift ? 'gift-outline' : 'ribbon-outline'}
        title={title}
        subtitle={s.revokedAt ? `${period} · ${t('subscription.revoked')}` : period}
        amount={amount}
        muted={Boolean(s.revokedAt)}
      />
    );
  }

  const d = row.entry;
  const unit = d.kind === 'STARS' ? t('profile.stars') : t('profile.coins');

  return (
    <Row
      icon="heart-outline"
      title={d.targetName ?? t('subscription.donation')}
      subtitle={formatDate(d.createdAt) ?? ''}
      amount={`${formatSum(d.amount)} ${unit}`}
    />
  );
}

function Row({
  icon,
  title,
  subtitle,
  amount,
  muted,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  title: string;
  subtitle: string;
  amount: string;
  muted?: boolean;
}) {
  return (
    <View className="flex-row items-center gap-3 rounded-card bg-surface p-4">
      <View className="h-11 w-11 items-center justify-center rounded-pill bg-surface-2">
        <Ionicons name={icon} size={20} color={colors.textMuted} />
      </View>

      <View className="flex-1 gap-0.5">
        <Text className="text-body text-text" numberOfLines={1}>
          {title}
        </Text>
        <Text className="text-caption text-text-muted" numberOfLines={1}>
          {subtitle}
        </Text>
      </View>

      <Text
        className="text-body font-semibold"
        style={{ color: muted ? colors.textDisabled : colors.white }}
      >
        {amount}
      </Text>
    </View>
  );
}
