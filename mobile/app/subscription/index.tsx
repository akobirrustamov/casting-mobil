import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { formatDate, premiumState, type PremiumState } from '@/features/profile/api';
import { useMySubscription } from '@/features/subscription/api';
import { colors, gradients, radius } from '@/theme/tokens';

/**
 * «Mening obunam» — статус и что с ним делать.
 *
 * <h2>Три состояния, три разных экрана</h2>
 * Активна, истекла и «никогда не было» — для человека это разные вещи,
 * и одна карточка «Premium: нет» стирала бы разницу. Истёкшая говорит
 * «до какого числа работало» и предлагает продлить; никогда не бывшая —
 * объясняет, что такое Premium. Различает их `premiumState()` — та же
 * функция, что в профиле, чтобы два экрана не разошлись.
 *
 * <h2>Откуда статус</h2>
 * `GET /api/v1/app/me/subscription` — и статус, и история одним ответом.
 * Флаг «активна» считает `AccessService` на сервере (ТЗ §37); здесь он
 * только читается.
 */
export default function SubscriptionScreen() {
  const { t } = useTranslation();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const subscription = useMySubscription();

  if (!isAuthorized) {
    return (
      <Screen title={t('subscription.title')} onBack={() => router.back()}>
        <ScreenState
          kind="locked"
          body={t('subscription.signInRequired')}
          actionLabel={t('profile.signIn')}
          onAction={() => router.push('/(auth)/sign-in')}
        />
      </Screen>
    );
  }

  const data = subscription.data;
  const state: PremiumState = data
    ? premiumState({ active: data.active, until: data.until })
    : 'none';
  const until = formatDate(data?.until ?? null);

  return (
    <Screen
      title={t('subscription.title')}
      onBack={() => router.back()}
      onRefresh={() => void subscription.refetch()}
      refreshing={subscription.isRefetching}
    >
      {subscription.isLoading ? <ScreenState kind="loading" /> : null}

      {subscription.isError ? (
        <ScreenState kind="error" onRetry={() => void subscription.refetch()} />
      ) : null}

      {data ? <StatusCard state={state} until={until} daysLeft={daysLeft(data.until)} /> : null}

      {data ? (
        <View className="gap-3">
          <Button
            variant="primary"
            shape="card"
            onPress={() => router.push('/subscription/tariffs')}
          >
            {state === 'active' ? t('subscription.viewTariffs') : t('subscription.choosePlan')}
          </Button>

          <Pressable
            onPress={() => router.push('/subscription/history')}
            accessibilityRole="button"
            className="flex-row items-center justify-between rounded-card bg-surface p-4 active:opacity-70"
          >
            <View className="flex-row items-center gap-3">
              <Ionicons name="time-outline" size={20} color={colors.textMuted} />
              <Text className="text-body text-text">{t('subscription.viewHistory')}</Text>
            </View>
            <Ionicons name="chevron-forward" size={18} color={colors.textDisabled} />
          </Pressable>
        </View>
      ) : null}
    </Screen>
  );
}

/** Карточка статуса — на premium-градиенте, когда подписка живая. */
function StatusCard({
  state,
  until,
  daysLeft,
}: {
  state: PremiumState;
  until: string | null;
  daysLeft: number | null;
}) {
  const { t } = useTranslation();

  if (state === 'active') {
    return (
      <View style={{ borderRadius: radius.cardLg, overflow: 'hidden' }}>
        <LinearGradient
          colors={gradients.premium}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={{ padding: 16, gap: 12 }}
        >
          <View className="flex-row items-start gap-3">
            <View className="flex-1 gap-1">
              <Text className="text-h2 text-white">{t('subscription.statusActive')}</Text>
              {until ? (
                <Text className="text-caption text-white/80">
                  {t('subscription.until', { date: until })}
                </Text>
              ) : null}
            </View>
            <Ionicons name="diamond" size={34} color={colors.gold} />
          </View>

          {/* Оставшиеся дни — то, ради чего сюда заходят: «сколько ещё». */}
          {daysLeft != null ? (
            <View className="self-start rounded-pill bg-white/15 px-3 py-1">
              <Text className="text-micro font-semibold text-white">
                {t('subscription.daysLeft', { count: daysLeft })}
              </Text>
            </View>
          ) : null}
        </LinearGradient>
      </View>
    );
  }

  const expired = state === 'expired';

  return (
    <View className="gap-3 rounded-card-lg bg-surface p-4">
      <View className="flex-row items-start gap-3">
        <View className="flex-1 gap-1">
          <Text className="text-h2 text-text">
            {expired ? t('subscription.statusExpired') : t('subscription.statusNone')}
          </Text>
          <Text className="text-caption text-text-muted">
            {expired && until
              ? t('subscription.expiredOn', { date: until })
              : t('subscription.noneBody')}
          </Text>
        </View>
        <Ionicons
          name={expired ? 'time-outline' : 'diamond-outline'}
          size={30}
          color={expired ? colors.danger : colors.textMuted}
        />
      </View>
    </View>
  );
}

/**
 * Сколько дней осталось — по дате, без часов.
 *
 * Бэкенд отдаёт `LocalDateTime` без зоны (Asia/Tashkent). Считаем по
 * календарным дням: «1 kun qoldi» в 23:50 честнее, чем «0».
 */
function daysLeft(iso: string | null): number | null {
  if (!iso) return null;
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!m) return null;

  const end = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])).getTime();
  const today = new Date();
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();

  const days = Math.ceil((end - start) / 86_400_000);
  return days > 0 ? days : null;
}
