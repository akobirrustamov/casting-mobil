import type { ReactNode } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';

/**
 * Горизонтальный рельс: заголовок + «Barchasini ko'rish ›» + список.
 * Основной строительный блок главной — паттерн из Yangi.TV,
 * состав рельсов задан ТЗ (bugungi premyeralar, mashhur ijodkorlar, casting).
 */
export function Rail({
  title,
  onSeeAll,
  children,
}: {
  title: string;
  onSeeAll?: () => void;
  children: ReactNode;
}) {
  const { t } = useTranslation();

  return (
    <View className="gap-3">
      <View className="flex-row items-center justify-between">
        <Text className="text-h2 text-text">{title}</Text>
        {onSeeAll ? (
          <Pressable onPress={onSeeAll} hitSlop={12}>
            <Text className="text-caption text-cyan">{t('common.seeAll')} ›</Text>
          </Pressable>
        ) : null}
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerClassName="gap-3 pr-4"
      >
        {children}
      </ScrollView>
    </View>
  );
}
