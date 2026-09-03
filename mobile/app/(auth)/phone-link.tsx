import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, TextInput, View } from 'react-native';

import { AuthScaffold } from '@/features/auth/AuthScaffold';
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

  const [phone, setPhone] = useState('');
  const digits = phone.replace(/\D/g, '');
  const isPhoneValid = digits.length === PHONE_DIGITS;

  return (
    <AuthScaffold
      onBack={() => router.replace('/(tabs)')}
      header={
        <Text className="text-center text-h2 text-text">{t('auth.phoneTitle')}</Text>
      }
      action={{
        // TODO: отправить OTP и привязать телефон, когда появится эндпоинт
        label: t('auth.continue'),
        onPress: () => router.replace('/(tabs)'),
        disabled: !isPhoneValid,
      }}
      footer={
        // Экран не должен быть тупиком: уйти можно без номера
        <Pressable onPress={() => router.replace('/(tabs)')} hitSlop={8}>
          <Text className="text-center text-caption text-text-muted">
            {t('auth.skip')}
          </Text>
        </Pressable>
      }
    >
      {/* Поле — ровно то же, что на входе и на экране кода: квадрат со
          знаком, разделитель, ввод. Свой стиль здесь делал экран чужим
          в собственном же разделе. */}
      <View
        className="flex-row items-center gap-3 rounded-card-lg border bg-surface p-2.5"
        style={{ borderColor: isPhoneValid ? colors.blue : colors.border }}
      >
        <View
          className="items-center justify-center rounded-card"
          style={{ width: 44, height: 44, backgroundColor: `${colors.purple}26` }}
        >
          <Ionicons name="call" size={20} color={colors.magenta} />
        </View>

        <Text className="text-h2 text-text">+998</Text>
        <View className="h-7 w-px" style={{ backgroundColor: colors.border }} />

        <TextInput
          value={phone}
          onChangeText={(raw) => setPhone(raw.replace(/\D/g, '').slice(0, PHONE_DIGITS))}
          placeholder={t('auth.phonePlaceholder')}
          placeholderTextColor={colors.textDisabled}
          keyboardType="phone-pad"
          inputMode="tel"
          autoFocus
          className="flex-1 text-h2"
          style={{ color: colors.white }}
        />
      </View>
    </AuthScaffold>
  );
}
