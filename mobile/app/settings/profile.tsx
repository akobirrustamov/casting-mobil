import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, TextInput, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { FormMessage } from '@/components/ui/FormMessage';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { useMe, useUpdateProfile } from '@/features/profile/api';
import { colors } from '@/theme/tokens';

/**
 * Профиль — изменить имя.
 *
 * <h2>⚠️ Что это чинит</h2>
 * Имя записывалось ТОЛЬКО во время входа и после этого не менялось
 * никогда: пункт «Profil» в списке не нажимался. У вошедших через
 * Google имя навсегда оставалось гугловским, а у тех, кто ошибся при
 * регистрации, — с опечаткой.
 *
 * <h2>Границы те же, что на входе</h2>
 * 2–60 символов, правило одно на оба экрана (`PersonName` на бэкенде).
 * Иначе имя, принятое здесь, отвергалось бы при следующем входе.
 *
 * <h2>Чего здесь пока нет</h2>
 * Аватара: чтобы человек мог его загрузить, нужен открытый для
 * пользователей файловый эндпоинт, а старый `/api/v1/file/upload`
 * принадлежит ЗАМОРОЖЕННОМУ модулю. Отдельный шаг.
 *
 * Телефон не редактируется намеренно: он подтверждён SMS и служит
 * логином — смена номера это отдельный поток с подтверждением.
 */
const MIN_LENGTH = 2;
const MAX_LENGTH = 60;

export default function ProfileSettingsScreen() {
  const { t } = useTranslation();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);

  const me = useMe();
  const update = useUpdateProfile();

  // Поле заполняется один раз — тем, что уже пришло. Подтягивать сюда
  // каждый ответ сервера нельзя: он затирал бы то, что человек печатает.
  const [name, setName] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const current = me.data?.user.name ?? '';
  const value = name ?? current;
  const trimmed = value.trim();

  const changed = trimmed !== current.trim();
  const valid = trimmed.length >= MIN_LENGTH && trimmed.length <= MAX_LENGTH;
  const canSave = changed && valid && !update.isPending;

  const onSubmit = async () => {
    setError(null);
    setSaved(false);
    try {
      await update.mutateAsync({ name: trimmed });
      setSaved(true);
    } catch {
      // Сервер проверяет то же самое, но мог отказать по другой причине —
      // например имя стало длиннее после нормализации пробелов.
      setError(t('settings.saveFailed'));
    }
  };

  if (!isAuthorized) {
    return (
      <Screen title={t('profile.editProfile')} onBack={() => router.back()}>
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
      title={t('profile.editProfile')}
      subtitle={t('settings.profileSubtitle')}
      onBack={() => router.back()}
    >
      {me.isLoading ? <ScreenState kind="loading" /> : null}

      {me.isError ? <ScreenState kind="error" onRetry={() => void me.refetch()} /> : null}

      {me.data ? (
        <>
          <View className="gap-2">
            <Text className="text-caption text-text-muted">{t('settings.name')}</Text>
            <View
              className="flex-row items-center gap-3 rounded-card-lg border bg-surface p-2.5"
              style={{ borderColor: valid ? colors.blue : colors.border }}
            >
              <View
                className="items-center justify-center rounded-card"
                style={{ width: 44, height: 44, backgroundColor: `${colors.purple}26` }}
              >
                <Ionicons name="person" size={20} color={colors.magenta} />
              </View>

              <View className="h-7 w-px" style={{ backgroundColor: colors.border }} />

              <TextInput
                value={value}
                onChangeText={(raw) => {
                  setName(raw);
                  setError(null);
                  setSaved(false);
                }}
                placeholder={t('settings.namePlaceholder')}
                placeholderTextColor={colors.textDisabled}
                autoCapitalize="words"
                maxLength={MAX_LENGTH}
                editable={!update.isPending}
                onSubmitEditing={canSave ? onSubmit : undefined}
                className="flex-1 text-body"
                style={{ color: colors.white }}
              />
            </View>
          </View>

          {/* Место под сообщение отведено заранее — поле над ним не
              прыгает, когда приходит ответ. */}
          <FormMessage
            message={error ?? (saved ? t('settings.saved') : null)}
            tone={error ? 'danger' : 'muted'}
            lines={1}
          />

          <Button
            variant="primary"
            shape="card"
            onPress={onSubmit}
            loading={update.isPending}
            disabled={!canSave}
          >
            {t('common.save')}
          </Button>

          {/* Только для чтения: номер подтверждён SMS и служит логином. */}
          <View className="gap-3 rounded-card bg-surface p-4">
            <ReadOnlyRow
              icon="call-outline"
              label={t('settings.phone')}
              value={me.data.user.phone}
            />
            {me.data.user.email ? (
              <ReadOnlyRow
                icon="mail-outline"
                label={t('settings.email')}
                value={me.data.user.email}
              />
            ) : null}
          </View>

          <Text className="text-caption text-text-muted">{t('settings.phoneNote')}</Text>
        </>
      ) : null}
    </Screen>
  );
}

function ReadOnlyRow({
  icon,
  label,
  value,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  value: string | null;
}) {
  return (
    <View className="flex-row items-center gap-3">
      <Ionicons name={icon} size={18} color={colors.textMuted} />
      <Text className="flex-1 text-caption text-text-muted">{label}</Text>
      <Text className="text-body text-text" numberOfLines={1}>
        {value ?? '—'}
      </Text>
    </View>
  );
}
