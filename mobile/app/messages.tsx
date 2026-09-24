import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Linking, Pressable, Text, View } from 'react-native';

import { pushOnce } from '@/lib/navigation';
import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import {
  internalRoute,
  useNotifications,
  type AppNotification,
} from '@/features/notifications/api';
import { formatDate } from '@/features/profile/api';
import { colors } from '@/theme/tokens';

/**
 * Xabarlar — уведомления, написанные в админке.
 *
 * <h2>Почему это больше не вкладка</h2>
 * На макете заказчика «Landing Page» в нижнем баре её нет — там
 * `Bosh sahifa · Media · Casting · Saqlanganlar · Profil`. Экран
 * открывается колокольчиком из профиля.
 *
 * <h2>Что изменилось</h2>
 * Здесь было пустое состояние с `TODO`: модуль уведомлений на бэкенде
 * существовал целиком, но приложение его не читало. Теперь список
 * настоящий — `GET /api/v1/app/notifications`, на языке интерфейса, с
 * учётом аудитории (для всех / только Premium / только без Premium).
 *
 * <h2>Чего здесь нет</h2>
 * Отметки «прочитано» и счётчика непрочитанного: они требуют отдельной
 * таблицы, а сообщений пока мало.
 *
 * TODO: чаты и системные сообщения (статусы заявок) — отдельный модуль.
 */
export default function MessagesScreen() {
  const { t } = useTranslation();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const notifications = useNotifications();

  if (!isAuthorized) {
    return (
      <Screen title={t('tabs.messages')} scroll={false} underTabBar={false} onBack={() => router.back()}>
        <ScreenState
          kind="locked"
          body={t('notifications.signInRequired')}
          actionLabel={t('profile.signIn')}
          onAction={() => pushOnce('/(auth)/sign-in')}
        />
      </Screen>
    );
  }

  return (
    <Screen
      title={t('tabs.messages')}
      underTabBar={false}
      onBack={() => router.back()}
      onRefresh={() => void notifications.refetch()}
      refreshing={notifications.isRefetching}
    >
      {notifications.isLoading ? <ScreenState kind="loading" /> : null}

      {notifications.isError ? (
        <ScreenState kind="error" onRetry={() => void notifications.refetch()} />
      ) : null}

      {notifications.data?.length === 0 ? (
        <ScreenState kind="empty" body={t('notifications.empty')} />
      ) : null}

      <View className="gap-3">
        {notifications.data?.map((item) => (
          <NotificationCard key={item.id} item={item} />
        ))}
      </View>
    </Screen>
  );
}

/**
 * Одно сообщение.
 *
 * ⚠️ Карточка нажимается ТОЛЬКО когда ссылка ведёт куда-то реальное.
 * Ряд, который выглядит кликабельным и молчит, читается как поломка —
 * то же правило, что у пунктов профиля.
 */
function NotificationCard({ item }: { item: AppNotification }) {
  const { t } = useTranslation();

  const target = internalRoute(item);
  const external = item.linkType === 'EXTERNAL' && item.linkUrl ? item.linkUrl : null;
  const pressable = Boolean(target || external);

  const open = () => {
    if (target) {
      pushOnce(target);
      return;
    }
    if (external) {
      Linking.openURL(external).catch(() => {});
    }
  };

  const body = (
    <View className="gap-3 rounded-card bg-surface p-4">
      {item.imageUrl ? (
        <Image
          source={{ uri: item.imageUrl }}
          style={{ width: '100%', aspectRatio: 16 / 9, borderRadius: 12 }}
          contentFit="cover"
          transition={150}
        />
      ) : null}

      <View className="flex-row items-start gap-3">
        <View className="h-11 w-11 items-center justify-center rounded-pill bg-surface-2">
          <Ionicons name="notifications-outline" size={20} color={colors.textMuted} />
        </View>

        <View className="flex-1 gap-1">
          <Text className="text-body font-semibold text-text">
            {item.title ?? t('notifications.untitled')}
          </Text>
          {item.body ? (
            <Text className="text-caption text-text-muted">{item.body}</Text>
          ) : null}
          <Text className="text-micro text-text-disabled">
            {formatDate(item.sentAt) ?? ''}
          </Text>
        </View>

        {pressable ? (
          <Ionicons name="chevron-forward" size={18} color={colors.textDisabled} />
        ) : null}
      </View>
    </View>
  );

  if (!pressable) return body;

  return (
    <Pressable accessibilityRole="button" onPress={open} className="active:opacity-70">
      {body}
    </Pressable>
  );
}
