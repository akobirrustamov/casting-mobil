import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Linking, Pressable, Text, View } from 'react-native';

import { pushOnce } from '@/lib/navigation';
import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import {
  CASTING_KIND,
  internalRoute,
  kindFromParam,
  useMarkAllRead,
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
 * <h2>Прочитано (25.09.2026)</h2>
 * Открыл экран — всё видимое прочитано, знак на колокольчике гаснет.
 * Новые при этом остаются подсвеченными, пока человек на экране: иначе
 * он не увидел бы, что именно пришло.
 *
 * <h2>Два вида (25.09.2026)</h2>
 * `/messages` — общие уведомления (колокольчик на главной),
 * `/messages?type=casting` — кастинговые (колокольчик во вкладке «Casting»).
 *
 * TODO: чаты и системные сообщения (статусы заявок) — отдельный модуль.
 */
export default function MessagesScreen() {
  const { t } = useTranslation();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const params = useLocalSearchParams<{ type?: string }>();
  const kind = kindFromParam(params.type);
  const title = kind === CASTING_KIND ? t('notifications.castingTitle') : t('tabs.messages');
  const notifications = useNotifications(kind);
  const markAllRead = useMarkAllRead(kind);

  const hasUnread = notifications.data?.some((item) => !item.read) ?? false;
  const { mutate: markAll } = markAllRead;
  useEffect(() => {
    if (hasUnread) markAll();
  }, [hasUnread, markAll]);

  if (!isAuthorized) {
    return (
      <Screen title={title} scroll={false} underTabBar={false} onBack={() => router.back()}>
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
      title={title}
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
    <View
      className="gap-3 rounded-card bg-surface p-4"
      // Новое — фиолетовая рамка: видно, что именно пришло.
      style={item.read ? undefined : { borderWidth: 1, borderColor: colors.purple }}
    >
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
          <Ionicons
            name={item.read ? 'notifications-outline' : 'notifications'}
            size={20}
            color={item.read ? colors.textMuted : colors.violet}
          />
          {item.read ? null : (
            <View
              style={{ borderWidth: 2, borderColor: colors.surface2 }}
              className="absolute right-0.5 top-0.5 h-3 w-3 rounded-pill bg-danger"
            />
          )}
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
