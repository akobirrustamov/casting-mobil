import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { colors } from '@/theme/tokens';

/**
 * Привязка телефона после входа через Google.
 *
 * Бэкенд отвечает `phone_required: true`, если аккаунт создан через Google
 * и телефона у него ещё нет. По ТЗ телефон — ключ пользователя и к нему
 * привязаны платёжные системы, поэтому спрашиваем сразу после входа.
 *
 * ⚠️ Отправки OTP пока нет — эндпоинта не существует (docs/API.md §5).
 */
const PHONE_DIGITS = 9;

export default function PhoneLinkScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();

  const [phone, setPhone] = useState('');
  const digits = phone.replace(/\D/g, '');

  return (
    <View
      className="flex-1 justify-center gap-8 bg-ink px-6"
      style={{ paddingTop: insets.top, paddingBottom: insets.bottom + 16 }}
    >
      <Text className="text-center text-h2 text-text">{t('auth.phoneTitle')}</Text>

      <View className="flex-row items-center gap-3 rounded-card bg-surface px-4">
        <Ionicons name="call-outline" size={20} color={colors.textMuted} />
        <Text className="text-body text-text">+998</Text>
        <TextInput
          value={phone}
          onChangeText={(raw) => setPhone(raw.replace(/\D/g, '').slice(0, PHONE_DIGITS))}
          placeholder={t('auth.phonePlaceholder')}
          placeholderTextColor={colors.textDisabled}
          keyboardType="phone-pad"
          inputMode="tel"
          autoFocus
          className="flex-1 py-4 text-body"
          style={{ color: colors.white }}
        />
      </View>

      <Button
        variant="primary"
        disabled={digits.length !== PHONE_DIGITS}
        // TODO: отправить OTP и привязать телефон, когда появится эндпоинт
        onPress={() => router.replace('/(tabs)')}
      >
        {t('auth.continue')}
      </Button>
    </View>
  );
}
