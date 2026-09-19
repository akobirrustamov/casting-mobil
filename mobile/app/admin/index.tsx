import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Redirect, router } from 'expo-router';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Alert, FlatList, Pressable, RefreshControl, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenState } from '@/components/states/ScreenState';
import { Badge } from '@/components/ui/Badge';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { Chip, ChipRow } from '@/features/casting/components';
import { formatDate } from '@/features/casting/status';
import {
  filterAdminList,
  useAdminList,
  type AdminCastingUser,
  type AdminStatus,
} from '@/features/castingAdmin/api';
import { useAdminStore } from '@/features/castingAdmin/store';
import { fileUrl } from '@/lib/api';
import { formatSum } from '@/lib/money';
import { useIsOffline } from '@/lib/network';
import { colors } from '@/theme/tokens';

/**
 * Список анкет для админа — перенос `CastingUser.js` / `CastingUserAccepted.js`
 * старой админки сайта в одну вкладку с тремя разделами.
 *
 * Список приходит целиком (`GET /api/v1/casting-user`) — так же, как в
 * старой админке; разделы и поиск считаются на телефоне. Анкет сотни, не
 * десятки тысяч: пока так дешевле, чем городить серверный фильтр.
 */
const TABS: AdminStatus[] = ['new', 'accepted', 'rejected'];

export default function AdminListScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const isOffline = useIsOffline();

  const token = useAdminStore((s) => s.token);
  const admin = useAdminStore((s) => s.user);
  const signOut = useAdminStore((s) => s.signOut);
  const isUserAuthorized = useAuthStore((s) => s.isAuthorized);

  const [tab, setTab] = useState<AdminStatus>('new');
  const [query, setQuery] = useState('');

  const list = useAdminList(token !== null);
  const all = list.data;

  const counts = useMemo(() => {
    const c: Record<AdminStatus, number> = { new: 0, accepted: 0, rejected: 0 };
    for (const u of all ?? []) c[u.status] += 1;
    return c;
  }, [all]);

  const visible = useMemo(() => filterAdminList(all ?? [], tab, query), [all, tab, query]);

  // Сессии нет (не входил или истекла) — на вход. Экран входа сам
  // скажет «сессия истекла», если дело в этом.
  if (!token) return <Redirect href="/admin/login" />;

  const onLogout = () => {
    Alert.alert(t('castingAdmin.list.logoutConfirm'), undefined, [
      { text: t('common.cancel'), style: 'cancel' },
      {
        text: t('castingAdmin.list.logout'),
        style: 'destructive',
        onPress: () => {
          // Возвращаем туда, откуда пришли в админку: пользовательская
          // сессия на этом телефоне живёт своей жизнью.
          //
          // ⚠️ Сначала переход, потом выход. В обратном порядке этот же
          // экран, увидев «токена нет», успел бы сам увести на вход
          // админа (`<Redirect>` ниже), и следом шёл бы второй переход.
          router.replace(isUserAuthorized ? '/(tabs)' : '/(auth)/sign-in');
          void signOut();
        },
      },
    ]);
  };

  const onBack = () => (router.canGoBack() ? router.back() : router.replace(isUserAuthorized ? '/(tabs)' : '/(auth)/sign-in'));

  const header = (
    <View className="gap-3 pb-3">
      <ChipRow>
        {TABS.map((value) => (
          <Chip
            key={value}
            label={`${t(`castingAdmin.list.tabs.${value}`)}${all ? ` · ${counts[value]}` : ''}`}
            active={tab === value}
            onPress={() => setTab(value)}
          />
        ))}
      </ChipRow>
      <View className="mx-4 flex-row items-center gap-2 rounded-card border border-border bg-surface px-3">
        <Ionicons name="search" size={16} color={colors.textMuted} />
        <TextInput
          value={query}
          onChangeText={setQuery}
          placeholder={t('castingAdmin.list.search')}
          placeholderTextColor={colors.textDisabled}
          autoCorrect={false}
          className="flex-1 py-3 text-body"
          style={{ color: colors.white }}
        />
      </View>
    </View>
  );

  const renderEmpty = () => {
    if (list.isPending) return <View style={{ minHeight: 280 }}><ScreenState kind="loading" /></View>;
    if (list.isError) {
      return (
        <View style={{ minHeight: 280 }}>
          <ScreenState kind={isOffline ? 'offline' : 'error'} onRetry={() => list.refetch()} />
        </View>
      );
    }
    return (
      <View style={{ minHeight: 240 }}>
        <ScreenState kind="empty" body={t('castingAdmin.list.empty')} />
      </View>
    );
  };

  return (
    <Screen
      scroll={false}
      title={t('castingAdmin.list.title')}
      subtitle={admin?.name ?? admin?.phone ?? undefined}
      onBack={onBack}
      underTabBar={false}
      headerRight={
        <Pressable
          onPress={onLogout}
          accessibilityRole="button"
          accessibilityLabel={t('castingAdmin.list.logout')}
          hitSlop={10}
          className="h-11 w-11 items-center justify-center active:opacity-60"
        >
          <Ionicons name="log-out-outline" size={24} color={colors.white} />
        </Pressable>
      }
    >
      <FlatList
        data={list.isPending || list.isError ? [] : visible}
        keyExtractor={(u) => String(u.id)}
        ListHeaderComponent={header}
        ListEmptyComponent={renderEmpty}
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={{ gap: 10, paddingBottom: insets.bottom + 24 }}
        initialNumToRender={12}
        refreshControl={
          <RefreshControl
            refreshing={list.isRefetching}
            onRefresh={() => list.refetch()}
            tintColor={colors.purple}
            colors={[colors.purple]}
            progressBackgroundColor={colors.surface}
          />
        }
        renderItem={({ item }) => <Row item={item} />}
      />
    </Screen>
  );
}

function Row({ item }: { item: AdminCastingUser }) {
  const { t } = useTranslation();
  const cover = item.photos[0];

  return (
    <Pressable
      onPress={() => router.push(`/admin/${item.id}`)}
      accessibilityRole="button"
      className="mx-4 flex-row gap-3 rounded-card-lg border border-border bg-surface p-3 active:opacity-80"
    >
      <View style={{ width: 56, height: 74 }} className="overflow-hidden rounded-card bg-surface-2">
        {cover ? (
          <Image
            source={{ uri: fileUrl(cover.id) }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            cachePolicy="memory-disk"
            recyclingKey={cover.id}
          />
        ) : (
          <View className="flex-1 items-center justify-center">
            <Ionicons name="person-outline" size={22} color={colors.textDisabled} />
          </View>
        )}
      </View>

      <View className="flex-1 gap-1">
        <Text numberOfLines={1} className="text-body font-semibold text-text">
          {item.name || '—'}
        </Text>
        <Text numberOfLines={1} className="text-caption text-text-muted">
          {[t(`casting.types.${item.castingType}`, { defaultValue: item.castingType }), item.phone]
            .filter(Boolean)
            .join(' • ')}
        </Text>
        <Text className="text-micro text-text-muted">{formatDate(item.createdAt)}</Text>
        <View className="flex-row flex-wrap gap-1.5">
          {item.status === 'accepted' && item.price ? (
            <Badge tone="purchased">{t('common.price', { amount: formatSum(item.price) })}</Badge>
          ) : null}
          {item.isWebShow ? <Badge tone="info">{t('castingAdmin.list.inCatalog')}</Badge> : null}
        </View>
      </View>

      <Ionicons name="chevron-forward" size={18} color={colors.textMuted} style={{ alignSelf: 'center' }} />
    </Pressable>
  );
}
