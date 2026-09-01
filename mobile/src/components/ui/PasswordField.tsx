import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { Pressable, TextInput, View } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * Поле пароля в раскладке поля номера с экрана входа: квадрат со знаком
 * слева, разделитель, ввод. Так вход и регистрация читаются как один
 * экран, а не как две разные формы.
 *
 * <h2>⚠️ Глаз обязателен</h2>
 * Пароль набирают на телефоне, вслепую, а «забыли пароль» пока
 * отключён. Опечатка в скрытом поле на регистрации — это аккаунт,
 * в который человек больше не войдёт. Повтор пароля ловит не всё:
 * одну и ту же опечатку легко сделать дважды.
 */
type Props = {
  value: string;
  onChangeText: (value: string) => void;
  placeholder: string;
  /** Рамка загорается синим — тот же сигнал «поле заполнено», что и у номера. */
  valid?: boolean;
  editable?: boolean;
  autoFocus?: boolean;
  /**
   * Новый пароль или существующий. Меняет подсказку менеджерам паролей:
   * на регистрации они предлагают сгенерировать, на входе — подставить.
   */
  isNew?: boolean;
  onSubmitEditing?: () => void;
};

export function PasswordField({
  value,
  onChangeText,
  placeholder,
  valid = false,
  editable = true,
  autoFocus = false,
  isNew = false,
  onSubmitEditing,
}: Props) {
  const [hidden, setHidden] = useState(true);

  return (
    <View
      className="flex-row items-center gap-3 rounded-card-lg border bg-surface p-2.5"
      style={{ borderColor: valid ? colors.blue : colors.border }}
    >
      <View
        className="items-center justify-center rounded-card"
        style={{ width: 44, height: 44, backgroundColor: `${colors.purple}26` }}
      >
        <Ionicons name="lock-closed" size={20} color={colors.magenta} />
      </View>

      <View className="h-7 w-px" style={{ backgroundColor: colors.border }} />

      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={colors.textDisabled}
        secureTextEntry={hidden}
        autoCapitalize="none"
        autoCorrect={false}
        autoComplete={isNew ? 'new-password' : 'current-password'}
        textContentType={isNew ? 'newPassword' : 'password'}
        editable={editable}
        autoFocus={autoFocus}
        returnKeyType={onSubmitEditing ? 'go' : 'done'}
        onSubmitEditing={onSubmitEditing}
        className="flex-1 text-body"
        style={{ color: colors.white }}
      />

      <Pressable onPress={() => setHidden((h) => !h)} hitSlop={12} className="px-1">
        <Ionicons
          name={hidden ? 'eye-outline' : 'eye-off-outline'}
          size={20}
          color={colors.textMuted}
        />
      </Pressable>
    </View>
  );
}
