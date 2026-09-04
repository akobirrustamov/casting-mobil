import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Alert, Pressable, Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { useDevices, useRevokeDevice, type Device } from '@/features/devices/api';
import { useDeviceStore } from '@/features/devices/store';
import { colors } from '@/theme/tokens';

/**
 * Faol qurilmalar — экран 1 в списке профиля и он же экран «мест нет».
 *
 * <h2>Почему один экран, а не два</h2>
 * В плане это были два шага: «список устройств» и «что показать при
 * лимите». Разводить их по разным маршрутам незачем — человеку в обоих
 * случаях нужно одно и то же: увидеть свои устройства и выгнать лишнее.
 * Разница только в шапке, и она объясняет, ЗАЧЕМ он сюда попал.
 *
 * Два экрана означали бы две копии списка, которые разъедутся при первой
 * же правке.
 *
 * <h2>Что чинится</h2>
 * Требование «не больше двух устройств» существовало только на бумаге:
 * приложение никогда не сообщало о себе бэкенду. А выгнать чужой телефон
 * мог лишь администратор — обычному человеку не было доступно ничего.
 */
export default function DevicesScreen() {
  const { t } = useTranslation();

  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const status = useDeviceStore((s) => s.status);
  const limitMessage = useDeviceStore((s) => s.limitMessage);
  const ensureRegistered = useDeviceStore((s) => s.ensureRegistered);

  const devices = useDevices(isAuthorized);
  const revoke = useRevokeDevice();

  /** Человек попал сюда потому, что его не пустили. */
  const blocked = status === 'limit';

  /**
   * Выгнать устройство.
   *
   * ⚠️ Подтверждение обязательно: действие мгновенно закрывает сессию на
   * том телефоне, и отменить его нельзя — там придётся входить заново.
   */
  const onRevoke = (device: Device) => {
    Alert.alert(
      t('devices.revokeTitle'),
      t('devices.revokeBody', { name: device.name ?? t('devices.unnamed') }),
      [
        { text: t('common.cancel'), style: 'cancel' },
        {
          text: t('devices.revoke'),
          style: 'destructive',
          onPress: async () => {
            try {
              await revoke.mutateAsync(device.id);
            } catch {
              Alert.alert(t('states.errorTitle'), t('devices.revokeFailed'));
              return;
            }

            if (device.current) {
              // Выгнали сами себя — это осознанный выход, а не сбой.
              await useAuthStore.getState().signOut();
              router.replace('/(auth)/sign-in');
              return;
            }

            if (blocked) {
              // Место освободилось — пробуем зарегистрироваться снова.
              // Именно ради этого шага человек сюда и пришёл.
              const next = await ensureRegistered();
              if (next === 'registered') {
                router.replace('/(tabs)');
              }
            }
          },
        },
      ]
    );
  };

  if (!isAuthorized) {
    return (
      <Screen title={t('profile.devices')} onBack={() => router.back()}>
        <ScreenState kind="locked" body={t('devices.signInRequired')} />
      </Screen>
    );
  }

  return (
    <Screen
      title={t('profile.devices')}
      subtitle={
        devices.data
          ? t('devices.counter', {
              used: devices.data.devices.length,
              limit: devices.data.limit,
            })
          : undefined
      }
      // ⚠️ Назад нет, пока человек не пущен: кнопка вернула бы его в
      // приложение, которым он ещё не может пользоваться.
      onBack={blocked ? undefined : () => router.back()}
      onRefresh={() => void devices.refetch()}
      refreshing={devices.isRefetching}
    >
      {blocked ? (
        <View className="flex-row gap-3 rounded-card border border-gold/40 bg-gold/10 p-4">
          <Ionicons name="alert-circle-outline" size={20} color={colors.gold} />
          <View className="flex-1 gap-1">
            <Text className="text-body font-semibold text-text">
              {t('devices.limitTitle')}
            </Text>
            <Text className="text-caption text-text-muted">
              {limitMessage ?? t('devices.limitBody')}
            </Text>
          </View>
        </View>
      ) : null}

      {devices.isLoading ? <ScreenState kind="loading" /> : null}

      {devices.isError ? (
        <ScreenState kind="error" onRetry={() => void devices.refetch()} />
      ) : null}

      {devices.data?.devices.length === 0 ? (
        <ScreenState kind="empty" body={t('devices.empty')} />
      ) : null}

      <View className="gap-3">
        {devices.data?.devices.map((device) => (
          <DeviceRow
            key={device.id}
            device={device}
            busy={revoke.isPending}
            onRevoke={() => onRevoke(device)}
          />
        ))}
      </View>

      <Text className="text-caption text-text-muted">{t('devices.note')}</Text>
    </Screen>
  );
}

/** Одно устройство: имя, платформа, когда было активно, кнопка выхода. */
function DeviceRow({
  device,
  busy,
  onRevoke,
}: {
  device: Device;
  busy: boolean;
  onRevoke: () => void;
}) {
  const { t } = useTranslation();

  return (
    <View className="flex-row items-center gap-3 rounded-card bg-surface p-4">
      <View className="h-11 w-11 items-center justify-center rounded-pill bg-surface-2">
        <Ionicons name={iconFor(device.platform)} size={20} color={colors.textMuted} />
      </View>

      <View className="flex-1 gap-1">
        <View className="flex-row items-center gap-2">
          <Text className="text-body text-text" numberOfLines={1}>
            {device.name ?? t('devices.unnamed')}
          </Text>
          {device.current ? (
            <View className="rounded-pill bg-purple px-2 py-0.5">
              <Text className="text-micro font-semibold text-white">
                {t('devices.current')}
              </Text>
            </View>
          ) : null}
        </View>
        <Text className="text-caption text-text-muted">
          {lastSeen(device.lastActiveAt, t)}
        </Text>
      </View>

      <Pressable
        accessibilityRole="button"
        accessibilityLabel={t('devices.revoke')}
        disabled={busy}
        onPress={onRevoke}
        hitSlop={8}
        className="h-11 w-11 items-center justify-center rounded-pill bg-surface-2 active:opacity-70"
      >
        <Ionicons name="log-out-outline" size={18} color={colors.danger} />
      </Pressable>
    </View>
  );
}

/** `useTranslation` qaytaradigan funksiyaning turi — o'z qo'lda yozganim mos kelmaydi. */
type Translate = ReturnType<typeof useTranslation>['t'];

function iconFor(platform: string | null): keyof typeof Ionicons.glyphMap {
  switch (platform) {
    case 'ios':
      return 'phone-portrait-outline';
    case 'android':
      return 'phone-portrait-outline';
    case 'web':
      return 'desktop-outline';
    default:
      return 'hardware-chip-outline';
  }
}

/**
 * «Bugun» / «Kecha» / «12.08.2026».
 *
 * ⚠️ Своё форматирование, а не `toLocaleDateString`: поддержка `Intl` в
 * Hermes зависит от сборки, и «работает у меня» здесь означало бы пустую
 * строку на чужом телефоне. Ровно та же причина записана в
 * `features/profile/api`.
 *
 * Бэкенд отдаёт `LocalDateTime` без зоны и обновляет это поле раз в сутки —
 * поэтому часы и минуты не показываются: они всё равно неточны.
 */
function lastSeen(iso: string | null, t: Translate): string {
  if (!iso) return t('devices.lastSeenUnknown');

  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!m) return t('devices.lastSeenUnknown');

  const [, year, month, day] = m;
  const date = new Date(Number(year), Number(month) - 1, Number(day));
  const today = new Date();
  const days = Math.round((startOfDay(today) - startOfDay(date)) / 86_400_000);

  if (days <= 0) return t('devices.lastSeenToday');
  if (days === 1) return t('devices.lastSeenYesterday');

  return t('devices.lastSeenOn', { date: `${day}.${month}.${year}` });
}

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}
