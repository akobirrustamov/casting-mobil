import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import {
  LANGUAGE_LABELS,
  SUPPORTED_LANGUAGES,
  isSupportedLanguage,
  type Language,
} from '@/i18n';
import { setLanguage } from '@/i18n/storage';

/**
 * Переключатель языка: узбекский по умолчанию, плюс русский и английский.
 *
 * Сегментированный контрол, а не выпадающий список: языков три, все
 * помещаются в строку и видны сразу — на одно касание меньше.
 */
export function LanguageSwitcher() {
  const { i18n } = useTranslation();
  const current: Language = isSupportedLanguage(i18n.language)
    ? i18n.language
    : 'uz';

  return (
    <View className="flex-row gap-2 rounded-card bg-surface p-1.5">
      {SUPPORTED_LANGUAGES.map((lang) => {
        const active = lang === current;
        return (
          <Pressable
            key={lang}
            accessibilityRole="button"
            accessibilityState={{ selected: active }}
            onPress={() => setLanguage(lang)}
            className={`flex-1 items-center rounded-card py-2.5 ${
              active ? 'bg-purple' : 'active:opacity-70'
            }`}
          >
            <Text
              className={`text-caption ${
                active ? 'font-semibold text-white' : 'text-text-muted'
              }`}
            >
              {LANGUAGE_LABELS[lang]}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}
