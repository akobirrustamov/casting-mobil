import type { ReactNode } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useTabBarHeight } from '@/components/navigation/TabBar';

/**
 * Каркас экрана: тёмный фон из ТЗ, safe area сверху,
 * заголовок + подзаголовок как в мокапах V4.
 *
 * Таб-бар плавающий, поэтому снизу оставляем место под него —
 * иначе последний блок уезжает под капсулу.
 */
export function Screen({
  title,
  subtitle,
  scroll = true,
  /** Отключить отступ под таб-бар — для экранов вне вкладок. */
  underTabBar = true,
  children,
}: {
  title?: string;
  subtitle?: string;
  scroll?: boolean;
  underTabBar?: boolean;
  children: ReactNode;
}) {
  const insets = useSafeAreaInsets();
  const tabBarHeight = useTabBarHeight();
  const bottomPad = underTabBar ? tabBarHeight + 8 : insets.bottom + 24;

  if (!scroll) {
    return (
      <View className="flex-1 bg-ink" style={{ paddingTop: insets.top }}>
        <Header title={title} subtitle={subtitle} />
        <View className="flex-1" style={{ paddingBottom: bottomPad }}>
          {children}
        </View>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-ink" style={{ paddingTop: insets.top }}>
      <Header title={title} subtitle={subtitle} />
      <ScrollView
        className="flex-1"
        showsVerticalScrollIndicator={false}
        contentContainerClassName="px-4 gap-4"
        contentContainerStyle={{ paddingBottom: bottomPad }}
      >
        {children}
      </ScrollView>
    </View>
  );
}

function Header({ title, subtitle }: { title?: string; subtitle?: string }) {
  if (!title) return null;

  return (
    <View className="px-4 pb-3 pt-2">
      <Text className="text-h1 text-text">{title}</Text>
      {subtitle ? (
        <Text className="mt-1 text-caption text-text-muted">{subtitle}</Text>
      ) : null}
    </View>
  );
}
