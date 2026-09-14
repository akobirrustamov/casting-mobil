import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Alert, Pressable, Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { FormMessage } from '@/components/ui/FormMessage';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { useDeleteAccount } from '@/features/profile/api';
import { colors } from '@/theme/tokens';

/**
 * Удаление аккаунта.
 *
 * <h2>Зачем экран существует</h2>
 * Требование Google Play: если в приложении можно завести аккаунт, должен
 * быть и способ удалить его — прямо в приложении, а не письмом в
 * поддержку. Ревьюер проверяет это руками, и без экрана приложение в
 * магазин не попадает.
 *
 * <h2>⚠️ Почему нет ни пароля, ни SMS-кода</h2>
 * Подтверждение кодом выглядит ответственнее, но политика требует, чтобы
 * удаление было ПРОСТЫМ: лишние барьеры — повод для отказа. Защита здесь
 * другая: экран открывается только вошедшему, а удаление подтверждается
 * системным диалогом, который случайно не нажать.
 *
 * <h2>Почему написано, что останется</h2>
 * Комментарии и записи о платежах не исчезают, и человек должен узнать об
 * этом ДО нажатия, а не потом увидеть свой комментарий на месте и решить,
 * что его обманули. Тот же текст — на странице `/hisobni-ochirish`.
 */
export default function DeleteAccountScreen() {
  const { t } = useTranslation();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const signOut = useAuthStore((s) => s.signOut);

  const remove = useDeleteAccount();
  const [error, setError] = useState<string | null>(null);

  const onDelete = async () => {
    setError(null);
    try {
      await remove.mutateAsync();

      // ⚠️ Разлогин ПОСЛЕ ответа сервера. Сделай это заранее — и при
      // ошибке сети человек остался бы без сессии с целым аккаунтом,
      // то есть с ощущением, что всё сломалось.
      await signOut();
      router.replace('/(auth)/sign-in');
    } catch {
      setError(t('settings.deleteFailed'));
    }
  };

  const confirm = () => {
    Alert.alert(t('settings.deleteConfirmTitle'), t('settings.deleteConfirmBody'), [
      { text: t('common.cancel'), style: 'cancel' },
      {
        text: t('settings.deleteAction'),
        style: 'destructive',
        onPress: () => void onDelete(),
      },
    ]);
  };

  if (!isAuthorized) {
    return (
      <Screen title={t('settings.deleteTitle')} onBack={() => router.back()}>
        <ScreenState
          kind="locked"
          body={t('settings.signInRequired')}
          actionLabel={t('profile.signIn')}
          onAction={() => router.push('/(auth)/sign-in')}
        />
      </Screen>
    );
  }

  return (
    <Screen
      title={t('settings.deleteTitle')}
      subtitle={t('settings.deleteSubtitle')}
      onBack={() => router.back()}
    >
      <Section
        icon="trash-outline"
        tone={colors.danger}
        title={t('settings.deleteWhatGoes')}
        body={t('settings.deleteWhatGoesBody')}
      />

      <Section
        icon="archive-outline"
        tone={colors.textMuted}
        title={t('settings.deleteWhatStays')}
        body={t('settings.deleteWhatStaysBody')}
      />

      <Text className="text-caption text-text-muted">
        {t('settings.deletePhoneNote')}
      </Text>

      <FormMessage message={error} tone="danger" lines={1} />

      {/*
        ⚠️ Не `Button`: у него нет опасного варианта, а красить главную
        кнопку приложения в красный — значит завести пятый вариант ради
        одного экрана. Здесь достаточно контурной кнопки цвета опасности.
      */}
      <Pressable
        onPress={confirm}
        disabled={remove.isPending}
        accessibilityRole="button"
        accessibilityLabel={t('settings.deleteAction')}
        className="h-12 flex-row items-center justify-center gap-2 rounded-card border active:opacity-70"
        style={{ borderColor: colors.danger, opacity: remove.isPending ? 0.5 : 1 }}
      >
        <Ionicons name="trash-outline" size={18} color={colors.danger} />
        <Text className="text-body font-semibold" style={{ color: colors.danger }}>
          {t('settings.deleteAction')}
        </Text>
      </Pressable>

      {/* Веб-адрес нужен Google и тем, у кого нет доступа к телефону. */}
      <Text className="text-micro text-text-disabled">{t('settings.deleteWebNote')}</Text>
    </Screen>
  );
}

function Section({
  icon,
  tone,
  title,
  body,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  tone: string;
  title: string;
  body: string;
}) {
  return (
    <View className="flex-row gap-3 rounded-card bg-surface p-4">
      <Ionicons name={icon} size={18} color={tone} />
      <View className="flex-1 gap-1">
        <Text className="text-caption font-semibold text-text">{title}</Text>
        <Text className="text-caption text-text-muted">{body}</Text>
      </View>
    </View>
  );
}
