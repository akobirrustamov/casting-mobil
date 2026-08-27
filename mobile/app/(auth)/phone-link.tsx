import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { GlowBackdrop } from '@/components/ui/GlowBackdrop';
import { colors } from '@/theme/tokens';

/**
 * Привязка телефона — **добровольная**.
 *
 * Раньше экран показывался принудительно после входа через Google, если
 * бэкенд вернул `phone_required`. Убрано: ТЗ разрешает аккаунт
 * «telefon/email orqali», а соцвход помечен как optional — требования
 * обязательного номера там нет. Гнать человека за номером до того, как он
 * вообще посмотрел приложение, — лишний барьер.
 *
 * Экран остаётся для случаев, когда номер действительно нужен: выплаты
 * креаторам и оплата через узбекские платёжные системы, завязанные на номер.
 * Открывается по требованию, а не на пути входа.
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
      <GlowBackdrop intensity="hero" />

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
        shape="card"
        disabled={digits.length !== PHONE_DIGITS}
        // TODO: отправить OTP и привязать телефон, когда появится эндпоинт
        onPress={() => router.replace('/(tabs)')}
      >
        {t('auth.continue')}
      </Button>

      {/* Экран не должен быть тупиком: уйти можно без номера */}
      <Pressable onPress={() => router.replace('/(tabs)')} hitSlop={8}>
        <Text className="text-center text-caption text-text-muted">
          {t('auth.skip')}
        </Text>
      </Pressable>
    </View>
  );
}
