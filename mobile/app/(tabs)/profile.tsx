import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';

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

  // TODO: брать из стора после авторизации
  const isAuthorized = false;
  const isCreator = false;

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
          <View className="h-14 w-14 rounded-pill bg-surface-2" />
          <View className="flex-1">
            <Text className="text-h2 text-text">
              {isAuthorized ? '—' : t('profile.guest')}
            </Text>
            <Text className="text-caption text-text-muted">
              {t('profile.balance')}: 0 UZS
            </Text>
          </View>
        </View>

        {!isAuthorized ? (
          <Button variant="primary">{t('profile.signIn')}</Button>
        ) : null}
      </View>

      <View className="overflow-hidden rounded-card bg-surface">
        {visible.map((item, i) => (
          <Pressable
            key={item.key}
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
