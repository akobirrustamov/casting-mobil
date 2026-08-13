import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { colors } from '@/theme/tokens';

/**
 * Ввод кода из SMS.
 *
 * ⚠️ Заглушка: эндпоинтов отправки и проверки OTP на бэкенде нет
 * (docs/API.md §5). Экран собран, чтобы флоу был целым и его можно
 * было отдать в Figma, но код никуда не уходит и не проверяется.
 */
const CODE_LENGTH = 6;

export default function OtpScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const { phone } = useLocalSearchParams<{ phone?: string }>();

  const [code, setCode] = useState('');
  const isValid = code.length === CODE_LENGTH;

  return (
    <View
      className="flex-1 bg-ink px-6"
      style={{ paddingTop: insets.top + 8, paddingBottom: insets.bottom + 16 }}
    >
      <Pressable onPress={() => router.back()} hitSlop={12} className="w-10 py-2">
        <Ionicons name="arrow-back" size={24} color={colors.white} />
      </Pressable>

      <View className="flex-1 justify-center gap-8">
        <View className="gap-2">
          <Text className="text-center text-h2 text-text">SMS kod</Text>
          {phone ? (
            <Text className="text-center text-caption text-text-muted">{phone}</Text>
          ) : null}
        </View>

        <TextInput
          value={code}
          onChangeText={(raw) => setCode(raw.replace(/\D/g, '').slice(0, CODE_LENGTH))}
          keyboardType="number-pad"
          inputMode="numeric"
          maxLength={CODE_LENGTH}
          autoFocus
          className="rounded-card bg-surface py-4 text-center text-h1"
          style={{ color: colors.white, letterSpacing: 12 }}
        />
      </View>

      <Button
        variant="primary"
        disabled={!isValid}
        onPress={() => router.replace('/(tabs)')}
      >
        {t('auth.continue')}
      </Button>
    </View>
  );
}
