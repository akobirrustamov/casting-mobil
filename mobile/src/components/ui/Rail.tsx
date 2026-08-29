import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';

import { colors } from '@/theme/tokens';

/**
 * Горизонтальный рельс: заголовок + «Barchasini ko'rish ›» + список.
 * Основной строительный блок главной — паттерн из Yangi.TV,
 * состав рельсов задан ТЗ (bugungi premyeralar, mashhur ijodkorlar, casting).
 */
export function Rail({
  title,
  /** Знак перед заголовком — пламя у премьер, как на макете заказчика. */
  icon,
  onSeeAll,
  children,
}: {
  title: string;
  icon?: keyof typeof Ionicons.glyphMap;
  onSeeAll?: () => void;
  children: ReactNode;
}) {
  const { t } = useTranslation();

  return (
    <View className="gap-3">
      <View className="flex-row items-center justify-between">
        <View className="flex-1 flex-row items-center gap-2">
          {icon ? <Ionicons name={icon} size={17} color={colors.magenta} /> : null}
          <Text numberOfLines={1} className="text-h2 text-text">
            {title}
          </Text>
        </View>
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
