import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import {
  Pressable,
  RefreshControl,
  ScrollView,
  Text,
  View,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useTabBarHeight } from '@/components/navigation/TabBar';
import { TOUCH_TARGET, colors } from '@/theme/tokens';

import { GlowBackdrop } from './GlowBackdrop';

/**
 * Каркас экрана: тёмный фон, safe area сверху, заголовок + подзаголовок
 * как в мокапах V4.
 *
 * Свечение фона лежит здесь, а не на каждом экране: иначе один забытый
 * экран выпадал бы из общего вида, и это заметили бы только на скриншотах.
 *
 * Таб-бар плавающий, поэтому снизу оставляем место под него —
 * иначе последний блок уезжает под капсулу.
 */
export function Screen({
  title,
  /** Заменяет текстовый заголовок — например логотипом. Приоритетнее `title`. */
  titleContent,
  subtitle,
  scroll = true,
  /** Отключить отступ под таб-бар — для экранов вне вкладок. */
  underTabBar = true,
  /** Стрелка назад слева от заголовка. Задаём только на вложенных экранах. */
  onBack,
  /** Действие справа в шапке — например «Фильтры». */
  headerRight,
  /** Потянуть вниз для обновления. Без обработчика жест не включается. */
  onRefresh,
  refreshing = false,
  /** Убрать свечение — для экранов, где фон занят своим изображением. */
  glow = true,
  /**
   * Прокрутка подошла к концу списка — подгрузить следующую страницу.
   *
   * Вызывается на КАЖДОМ подходящем кадре прокрутки, а не один раз:
   * решать, нужен ли ещё запрос, должен тот, кто знает состояние
   * загрузки (у `useInfiniteQuery` это `hasNextPage` и
   * `isFetchingNextPage`). Экран этого знать не может.
   */
  onEndReached,
  children,
}: {
  title?: string;
  titleContent?: ReactNode;
  subtitle?: string;
  scroll?: boolean;
  underTabBar?: boolean;
  onBack?: () => void;
  headerRight?: ReactNode;
  onRefresh?: () => void;
  refreshing?: boolean;
  glow?: boolean;
  onEndReached?: () => void;
  children: ReactNode;
}) {
  const insets = useSafeAreaInsets();
  const tabBarHeight = useTabBarHeight();
  const bottomPad = underTabBar ? tabBarHeight + 8 : insets.bottom + 24;

  // Порог с запасом в экран: страница успевает приехать до того, как
  // человек упрётся в конец списка, и прокрутка не «дёргается» ожиданием.
  const handleScroll = onEndReached
    ? (e: NativeSyntheticEvent<NativeScrollEvent>) => {
        const { layoutMeasurement, contentOffset, contentSize } = e.nativeEvent;
        const left = contentSize.height - contentOffset.y - layoutMeasurement.height;
        if (left < layoutMeasurement.height) {
          onEndReached();
        }
      }
    : undefined;

  if (!scroll) {
    return (
      <View className="flex-1 bg-ink" style={{ paddingTop: insets.top }}>
        {glow ? <GlowBackdrop /> : null}
        <Header
          title={title}
          titleContent={titleContent}
          subtitle={subtitle}
          onBack={onBack}
          headerRight={headerRight}
        />
        <View className="flex-1" style={{ paddingBottom: bottomPad }}>
          {children}
        </View>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-ink" style={{ paddingTop: insets.top }}>
      {glow ? <GlowBackdrop /> : null}
      <Header
        title={title}
        titleContent={titleContent}
        subtitle={subtitle}
        onBack={onBack}
        headerRight={headerRight}
      />
      <ScrollView
        className="flex-1"
        showsVerticalScrollIndicator={false}
        onScroll={handleScroll}
        // Реже, чем каждый кадр: обработчик только сравнивает числа, но
        // 60 вызовов в секунду через мост не нужны ни одному экрану.
        scrollEventThrottle={200}
        contentContainerClassName="px-4 gap-4"
        contentContainerStyle={{ paddingBottom: bottomPad }}
        refreshControl={
          onRefresh ? (
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              // Спиннер по умолчанию серый и на тёмном фоне почти не виден
              tintColor={colors.purple}
              colors={[colors.purple]}
              progressBackgroundColor={colors.surface}
            />
          ) : undefined
        }
      >
        {children}
      </ScrollView>
    </View>
  );
}

function Header({
  title,
  titleContent,
  subtitle,
  onBack,
  headerRight,
}: {
  title?: string;
  titleContent?: ReactNode;
  subtitle?: string;
  onBack?: () => void;
  headerRight?: ReactNode;
}) {
  if (!title && !titleContent) return null;

  return (
    <View className="flex-row items-start gap-2 px-4 pb-3 pt-2">
      {onBack ? (
        <Pressable
          onPress={onBack}
          accessibilityRole="button"
          accessibilityLabel="Orqaga"
          hitSlop={12}
          // ТЗ: минимальный touch target 44px
          style={{ minWidth: TOUCH_TARGET - 12, minHeight: TOUCH_TARGET - 12 }}
          className="-ml-2 justify-center active:opacity-60"
        >
          <Ionicons name="chevron-back" size={26} color={colors.white} />
        </Pressable>
      ) : null}

      <View className="flex-1">
        {titleContent ?? (
          <Text numberOfLines={1} className="text-h1 text-text">
            {title}
          </Text>
        )}
        {subtitle ? (
          <Text className="mt-1 text-caption text-text-muted">{subtitle}</Text>
        ) : null}
      </View>

      {headerRight}
    </View>
  );
}
