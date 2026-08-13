import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { useEffect, useRef } from 'react';
import { Animated, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { TOUCH_TARGET, colors, radius } from '@/theme/tokens';

/**
 * Нижняя навигация.
 *
 * Форма и раскладка — как в Yangi.TV (docs/STRUCTURE.md §2):
 * плавающая капсула с отступами от краёв, полупрозрачный тёмный фон,
 * тонкая светлая рамка, иконка сверху и подпись под ней.
 *
 * Цвета активного состояния — из ТЗ, а не из Yangi.TV: у них активный
 * просто белый, у нас Neon Purple #7C3AED, как на мокапах V4.
 *
 * Переключение: цвет и подпись плавно перетекают, активная иконка
 * приподнимается и получает мягкую подсветку. ТЗ: «glow эффекты в меру».
 */

const ICONS: Record<string, { active: keyof typeof Ionicons.glyphMap; inactive: keyof typeof Ionicons.glyphMap }> = {
  index: { active: 'home', inactive: 'home-outline' },
  casting: { active: 'star', inactive: 'star-outline' },
  premiere: { active: 'play-circle', inactive: 'play-circle-outline' },
  messages: { active: 'chatbubble-ellipses', inactive: 'chatbubble-ellipses-outline' },
  profile: { active: 'person', inactive: 'person-outline' },
};

const BAR_HEIGHT = 62;

/** Высота, которую бар занимает снизу — экраны используют её как отступ. */
export function useTabBarHeight() {
  const insets = useSafeAreaInsets();
  return BAR_HEIGHT + Math.max(insets.bottom, 12) + 12;
}

type TabItemProps = {
  routeName: string;
  label: string;
  focused: boolean;
  onPress: () => void;
};

function TabItem({ routeName, label, focused, onPress }: TabItemProps) {
  // 0 — неактивная вкладка, 1 — активная. Всё остальное считается отсюда.
  const progress = useRef(new Animated.Value(focused ? 1 : 0)).current;

  useEffect(() => {
    Animated.timing(progress, {
      toValue: focused ? 1 : 0,
      duration: 180,
      useNativeDriver: true,
    }).start();
  }, [focused, progress]);

  const icon = ICONS[routeName] ?? ICONS.index;

  const lift = progress.interpolate({
    inputRange: [0, 1],
    outputRange: [0, -2],
  });

  const glowStyle = {
    opacity: progress.interpolate({ inputRange: [0, 1], outputRange: [0, 0.18] }),
    transform: [
      { scale: progress.interpolate({ inputRange: [0, 1], outputRange: [0.6, 1] }) },
    ],
  };

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="tab"
      accessibilityState={{ selected: focused }}
      accessibilityLabel={label}
      style={styles.item}
      android_ripple={{ color: 'transparent' }}
    >
      <Animated.View style={{ transform: [{ translateY: lift }] }}>
        <View style={styles.iconBox}>
          {/* Подсветка под активной иконкой */}
          <Animated.View style={[styles.glow, glowStyle]} />
          <Ionicons
            name={focused ? icon.active : icon.inactive}
            size={23}
            color={focused ? colors.purple : colors.textMuted}
          />
        </View>

        <Text
          numberOfLines={1}
          style={[
            styles.label,
            {
              color: focused ? colors.purple : colors.textMuted,
              fontWeight: focused ? '600' : '400',
            },
          ]}
        >
          {label}
        </Text>
      </Animated.View>
    </Pressable>
  );
}

type TabBarProps = {
  state: {
    index: number;
    routes: { key: string; name: string }[];
  };
  descriptors: Record<string, { options: { title?: string; tabBarLabel?: unknown } }>;
  navigation: {
    emit: (event: {
      type: 'tabPress';
      target: string;
      canPreventDefault: true;
    }) => { defaultPrevented: boolean };
    navigate: (name: string) => void;
  };
};

export function TabBar({ state, descriptors, navigation }: TabBarProps) {
  const insets = useSafeAreaInsets();

  return (
    <View
      style={[styles.wrapper, { paddingBottom: Math.max(insets.bottom, 12) }]}
      pointerEvents="box-none"
    >
      <BlurView intensity={40} tint="dark" style={styles.bar}>
        {state.routes.map((route, index) => {
          const focused = state.index === index;
          const options = descriptors[route.key]?.options;
          const label = options?.title ?? route.name;

          const onPress = () => {
            const event = navigation.emit({
              type: 'tabPress',
              target: route.key,
              canPreventDefault: true,
            });
            if (!focused && !event.defaultPrevented) {
              navigation.navigate(route.name);
            }
          };

          return (
            <TabItem
              key={route.key}
              routeName={route.name}
              label={label}
              focused={focused}
              onPress={onPress}
            />
          );
        })}
      </BlurView>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    paddingHorizontal: 16,
  },
  bar: {
    flexDirection: 'row',
    height: BAR_HEIGHT,
    borderRadius: radius.pill,
    overflow: 'hidden',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    // BlurView на Android слабее, поэтому под ним лежит почти непрозрачная база
    backgroundColor: 'rgba(17, 17, 31, 0.86)',
  },
  item: {
    flex: 1,
    minHeight: TOUCH_TARGET,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconBox: {
    alignItems: 'center',
    justifyContent: 'center',
    height: 26,
  },
  glow: {
    position: 'absolute',
    width: 40,
    height: 26,
    borderRadius: radius.pill,
    backgroundColor: colors.purple,
  },
  label: {
    marginTop: 3,
    fontSize: 10.5,
    textAlign: 'center',
  },
});
