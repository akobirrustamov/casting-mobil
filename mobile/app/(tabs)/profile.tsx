import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { LanguageSwitcher } from '@/components/ui/LanguageSwitcher';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { colors } from '@/theme/tokens';

/**
 * Профиль. Пункты по ТЗ (V3, стр. 22 «21. Foydalanuvchi profili»):
 * balance · purchases · projects · favorites · payments · settings,
 * плюс Creator Studio для роли creator.
 *
 * Верстка — по Yangi.TV: карточка профиля сверху (аватар, имя, телефон,
 * баланс, ID) и список пунктов с шевронами. См. docs/STRUCTURE.md §3.6.
 *
 * ⚠️ Авторизации ещё нет — показываем состояние гостя.
 */
type MenuItem = {
  key: string;
  label: string;
  danger?: boolean;
  creatorOnly?: boolean;
};

export default function ProfileScreen() {
  const { t } = useTranslation();

  const user = useAuthStore((s) => s.user);
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const signOut = useAuthStore((s) => s.signOut);

  const isCreator = user?.role === 'creator';

  // Имени может не быть: Google отдаёт его не всегда. Тогда показываем email,
  // иначе карточка выглядит пустой у реально вошедшего человека.
  const displayName = user?.name || user?.email || user?.phone || '—';
  const subtitle = user?.email ?? user?.phone ?? null;

  const items: MenuItem[] = [
    { key: 'topUp', label: t('profile.topUp') },
    { key: 'purchases', label: t('profile.purchases') },
    { key: 'paymentHistory', label: t('profile.paymentHistory') },
    { key: 'applications', label: t('profile.applications') },
    { key: 'portfolio', label: t('profile.portfolio') },
    { key: 'favorites', label: t('profile.favorites') },
    { key: 'creatorStudio', label: t('profile.creatorStudio'), creatorOnly: true },
    { key: 'premium', label: t('profile.premium') },
    { key: 'devices', label: t('profile.devices') },
    { key: 'settings', label: t('profile.settings') },
    { key: 'contact', label: t('profile.contact') },
    { key: 'logout', label: t('profile.logout'), danger: true },
  ];

  const visible = items.filter(
    (i) => (!i.creatorOnly || isCreator) && (i.key !== 'logout' || isAuthorized)
  );

  return (
    <Screen title={t('profile.title')}>
      <View className="gap-3 rounded-card-lg bg-surface p-4">
        <View className="flex-row items-center gap-3">
          {user?.avatarUrl ? (
            <Image
              source={{ uri: user.avatarUrl }}
              style={{ width: 56, height: 56, borderRadius: 999 }}
              contentFit="cover"
              transition={150}
            />
          ) : (
            <View className="h-14 w-14 items-center justify-center rounded-pill bg-surface-2">
              <Ionicons name="person-outline" size={26} color={colors.textMuted} />
            </View>
          )}

          <View className="flex-1">
            <Text className="text-h2 text-text" numberOfLines={1}>
              {isAuthorized ? displayName : t('profile.guest')}
            </Text>
            {isAuthorized && subtitle ? (
              <Text className="text-caption text-text-muted" numberOfLines={1}>
                {subtitle}
              </Text>
            ) : null}
            <Text className="text-caption text-text-muted">
              {t('profile.balance')}: 0 UZS
            </Text>
          </View>
        </View>

        {!isAuthorized ? (
          <Button variant="primary" onPress={() => router.push('/(auth)/sign-in')}>
            {t('profile.signIn')}
          </Button>
        ) : null}
      </View>

      <LanguageSwitcher />

      <View className="overflow-hidden rounded-card bg-surface">
        {visible.map((item, i) => (
          <Pressable
            key={item.key}
            accessibilityRole="button"
            // Остальные пункты пока без экранов — появятся по мере готовности
            onPress={item.key === 'logout' ? signOut : undefined}
            className={`flex-row items-center justify-between px-4 py-4 active:opacity-70 ${
              i > 0 ? 'border-t border-border' : ''
            }`}
          >
            <Text
              className={`text-body ${item.danger ? 'text-danger' : 'text-text'}`}
            >
              {item.label}
            </Text>
            <Text className="text-body text-text-muted">›</Text>
          </Pressable>
        ))}
      </View>

      {/* Как у Yangi.TV — версия приложения внизу профиля */}
      <Text className="text-center text-micro text-text-disabled">
        UzCasting 1.0.0
      </Text>
    </Screen>
  );
}
