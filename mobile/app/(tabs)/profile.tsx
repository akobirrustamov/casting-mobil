import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import Constants from 'expo-constants';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Linking, Pressable, Text, View } from 'react-native';

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
  icon: keyof typeof Ionicons.glyphMap;
  danger?: boolean;
  creatorOnly?: boolean;
};

/** Ряд соцсетей внизу профиля — как у Yangi.TV. Ссылки уточняются у заказчика. */
const SOCIALS: { key: string; icon: keyof typeof Ionicons.glyphMap; url: string }[] = [
  { key: 'telegram', icon: 'paper-plane-outline', url: 'https://t.me/uzcasting' },
  { key: 'instagram', icon: 'logo-instagram', url: 'https://instagram.com/uzcasting' },
  { key: 'youtube', icon: 'logo-youtube', url: 'https://youtube.com/@uzcasting' },
  { key: 'facebook', icon: 'logo-facebook', url: 'https://facebook.com/uzcasting' },
];

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

  // Иконки — как у Yangi.TV: список из одних строк читается тяжелее,
  // а по значку пункт находится взглядом, без чтения подписи.
  const items: MenuItem[] = [
    { key: 'topUp', label: t('profile.topUp'), icon: 'wallet-outline' },
    { key: 'purchases', label: t('profile.purchases'), icon: 'bag-check-outline' },
    { key: 'paymentHistory', label: t('profile.paymentHistory'), icon: 'receipt-outline' },
    { key: 'applications', label: t('profile.applications'), icon: 'document-text-outline' },
    { key: 'portfolio', label: t('profile.portfolio'), icon: 'images-outline' },
    { key: 'favorites', label: t('profile.favorites'), icon: 'heart-outline' },
    {
      key: 'creatorStudio',
      label: t('profile.creatorStudio'),
      icon: 'sparkles-outline',
      creatorOnly: true,
    },
    { key: 'premium', label: t('profile.premium'), icon: 'diamond-outline' },
    { key: 'devices', label: t('profile.devices'), icon: 'phone-portrait-outline' },
    { key: 'settings', label: t('profile.settings'), icon: 'settings-outline' },
    { key: 'contact', label: t('profile.contact'), icon: 'chatbubble-ellipses-outline' },
    { key: 'logout', label: t('profile.logout'), icon: 'log-out-outline', danger: true },
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
            {isAuthorized && user?.id ? <UserIdRow id={user.id} /> : null}
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
            onPress={
              item.key === 'logout'
                ? signOut
                : item.key === 'favorites'
                  ? () => router.push('/favorites')
                  : undefined
            }
            className={`flex-row items-center justify-between px-4 py-4 active:opacity-70 ${
              i > 0 ? 'border-t border-border' : ''
            }`}
          >
            <View className="flex-row items-center gap-3">
              <Ionicons
                name={item.icon}
                size={20}
                color={item.danger ? colors.danger : colors.textMuted}
              />
              <Text
                className={`text-body ${item.danger ? 'text-danger' : 'text-text'}`}
              >
                {item.label}
              </Text>
            </View>

            <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
          </Pressable>
        ))}
      </View>

      {/* Как у Yangi.TV — соцсети и версия приложения внизу профиля */}
      <View className="flex-row justify-center gap-3">
        {SOCIALS.map((s) => (
          <Pressable
            key={s.key}
            accessibilityRole="link"
            accessibilityLabel={s.key}
            onPress={() => Linking.openURL(s.url).catch(() => {})}
            className="h-11 w-11 items-center justify-center rounded-pill bg-surface active:opacity-70"
          >
            <Ionicons name={s.icon} size={20} color={colors.textMuted} />
          </Pressable>
        ))}
      </View>

      <Text className="text-center text-micro text-text-disabled">
        UzCasting {Constants.expoConfig?.version ?? '1.0.0'}
      </Text>
    </Screen>
  );
}

/**
 * ID пользователя с копированием — у Yangi.TV он на видном месте
 * рядом с балансом: его диктуют в поддержку.
 */
function UserIdRow({ id }: { id: string }) {
  const [copied, setCopied] = useState(false);

  const onCopy = async () => {
    await Clipboard.setStringAsync(id);
    setCopied(true);
    // Возвращаем подпись обратно, иначе «скопировано» висит навсегда
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <Pressable
      onPress={onCopy}
      accessibilityRole="button"
      hitSlop={6}
      className="mt-0.5 flex-row items-center gap-1.5 active:opacity-60"
    >
      <Text numberOfLines={1} className="text-micro text-text-disabled">
        ID: {id}
      </Text>
      <Ionicons
        name={copied ? 'checkmark' : 'copy-outline'}
        size={13}
        color={copied ? colors.success : colors.textDisabled}
      />
    </Pressable>
  );
}
