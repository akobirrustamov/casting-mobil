import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { FlatList, RefreshControl, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { pushOnce } from '@/lib/navigation';
import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { useMyApplications } from '@/features/casting/api';
import { ApplicationStatusBlock } from '@/features/casting/components';
import { useIsOffline } from '@/lib/network';
import { colors } from '@/theme/tokens';

/**
 * «Мои заявки» — все заявки кандидата с решением админа.
 *
 * <h2>Оплата — только статусом</h2>
 * У одобренной заявки — цена в сумах и пометка «To'lov — tez orada».
 * Кнопки оплаты нет намеренно: платёжного шага в приложении ещё нет,
 * и кнопка, ведущая в никуда, — ровно то, что уже однажды убирали с
 * этой вкладки (коммит 8fe810f).
 */
export default function MyApplicationsScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const isOffline = useIsOffline();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const userId = useAuthStore((s) => s.user?.id ?? null);

  const query = useMyApplications(isAuthorized ? userId : null);

  const shell = (children: React.ReactNode) => (
    <Screen scroll={false} title={t('casting.my.title')} onBack={() => router.back()} underTabBar={false}>
      {children}
    </Screen>
  );

  if (!isAuthorized) {
    return shell(
      <ScreenState
        kind="locked"
        body={t('casting.my.signIn')}
        actionLabel={t('profile.signIn')}
        onAction={() => pushOnce('/(auth)/sign-in')}
      />,
    );
  }

  if (query.isPending) return shell(<ScreenState kind="loading" />);

  if (query.isError) {
    return shell(<ScreenState kind={isOffline ? 'offline' : 'error'} onRetry={() => query.refetch()} />);
  }

  const apps = [...query.data].sort((a, b) => b.createdAt.localeCompare(a.createdAt));

  if (apps.length === 0) {
    return shell(
      <ScreenState
        kind="empty"
        body={t('casting.my.empty')}
        actionLabel={t('casting.applyCta')}
        onAction={() => router.replace('/casting/apply')}
      />,
    );
  }

  const hasPending = apps.some((a) => a.status === 'PENDING');

  return shell(
    <FlatList
      data={apps}
      keyExtractor={(a) => String(a.id)}
      contentContainerStyle={{ paddingHorizontal: 16, gap: 12, paddingBottom: insets.bottom + 24 }}
      refreshControl={
        <RefreshControl
          refreshing={query.isRefetching}
          onRefresh={() => query.refetch()}
          tintColor={colors.purple}
          colors={[colors.purple]}
          progressBackgroundColor={colors.surface}
        />
      }
      renderItem={({ item }) => <ApplicationStatusBlock app={item} />}
      ListFooterComponent={
        // Новую заявку можно подать, только когда прошлая рассмотрена —
        // иначе сервер ответит 409.
        !hasPending ? (
          <View className="pt-2">
            <Button variant="secondary" onPress={() => pushOnce('/casting/apply')}>
              {t('casting.applyCta')}
            </Button>
          </View>
        ) : null
      }
    />,
  );
}
