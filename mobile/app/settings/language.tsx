import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { Screen } from '@/components/ui/Screen';
import {
  LANGUAGE_LABELS,
  SUPPORTED_LANGUAGES,
  isSupportedLanguage,
  type Language,
} from '@/i18n';
import { setLanguage } from '@/i18n/storage';
import { colors } from '@/theme/tokens';

/**
 * Язык интерфейса — отдельный экран.
 *
 * <h2>Почему не всплывашка в «Profil»</h2>
 * Заказчик (14.09.2026): «til almashtirish bosilganda alohida page ochib
 * usha yerga navigate». Раньше ряд «Til» раскрывал под собой
 * сегментированный переключатель — три кнопки в одну строку. Ряд при этом
 * выглядел как все соседние (шеврон «›» справа), а вёл себя иначе: экран
 * не менялся, а под пальцем разворачивалась полоска, которую на длинном
 * списке настроек было легко не заметить.
 *
 * <h2>Список, а не сегменты</h2>
 * Языки здесь строками с галочкой у выбранного — так же, как это сделано
 * в системных настройках телефона. Сегментированный контрол оправдан,
 * когда переключатель стоит В ПОТОКЕ (например, на экране входа, где он
 * лежит во всплывашке под шестерёнкой, — там он и остался). На отдельном
 * экране три кнопки в строку занимали бы верхний угол и оставляли пустой
 * весь остальной экран.
 *
 * <h2>Переключение — и сразу назад</h2>
 * Выбор языка это одно действие, и оставаться на экране после него незачем:
 * человек пришёл сюда ровно за ним. Уходим через `back()`, а не `replace`:
 * возвращаться надо туда, откуда пришли, — в «Profil».
 *
 * ⚠️ Без `await`: запись в хранилище не должна задерживать уход с экрана
 * (см. `i18n/storage.setLanguage` — язык применяется сразу, на диск
 * ложится следом).
 */
export default function LanguageScreen() {
  const { t, i18n } = useTranslation();
  const current: Language = isSupportedLanguage(i18n.language) ? i18n.language : 'uz';

  const onSelect = (lang: Language) => {
    void setLanguage(lang);
    router.back();
  };

  return (
    <Screen
      title={t('profile.language')}
      onBack={() => router.back()}
      underTabBar={false}
    >
      <View className="overflow-hidden rounded-card-lg bg-surface">
        {SUPPORTED_LANGUAGES.map((lang, i) => {
          const active = lang === current;

          return (
            <Pressable
              key={lang}
              accessibilityRole="button"
              accessibilityState={{ selected: active }}
              onPress={() => onSelect(lang)}
              className={`flex-row items-center gap-3 px-4 py-4 active:opacity-70 ${
                i > 0 ? 'border-t border-border' : ''
              }`}
            >
              <Text
                className={`flex-1 text-body text-text ${active ? 'font-semibold' : ''}`}
              >
                {LANGUAGE_LABELS[lang]}
              </Text>

              {/* Галочка только у выбранного: пустое место справа у
                  остальных читается однозначно, а серая «пустая» галочка
                  выглядела бы как недоступный пункт. */}
              {active ? (
                <Ionicons name="checkmark" size={20} color={colors.purple} />
              ) : null}
            </Pressable>
          );
        })}
      </View>
    </Screen>
  );
}
