import type { ReactNode } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

/**
 * Каркас экрана: тёмный фон из ТЗ, safe area сверху,
 * заголовок + подзаголовок как в мокапах V4.
 */
export function Screen({
  title,
  subtitle,
  scroll = true,
  children,
}: {
  title?: string;
  subtitle?: string;
  scroll?: boolean;
  children: ReactNode;
}) {
  const insets = useSafeAreaInsets();
  const Container = scroll ? ScrollView : View;

  return (
    <View className="flex-1 bg-ink" style={{ paddingTop: insets.top }}>
      {title ? (
        <View className="px-4 pb-3 pt-2">
          <Text className="text-h1 text-text">{title}</Text>
          {subtitle ? (
            <Text className="mt-1 text-caption text-text-muted">{subtitle}</Text>
          ) : null}
        </View>
      ) : null}

      <Container
        className="flex-1"
        {...(scroll
          ? { contentContainerClassName: 'px-4 pb-8 gap-4', showsVerticalScrollIndicator: false }
          : {})}
      >
        {children}
      </Container>
    </View>
  );
}
