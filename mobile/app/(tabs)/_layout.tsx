import { Tabs } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Text, type ColorValue } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * 5 вкладок зафиксированы в ТЗ (V2, стр. 19 «NAVIGATION + ROLE SYSTEM»)
 * и подтверждены таб-баром на мокапах V4:
 * Bosh sahifa · Casting · Premyera · Xabarlar · Profil
 *
 * Роль Creator добавляет Creator Studio / Upload / Revenue / Withdraw —
 * это не 6-я вкладка, а раздел внутри Profil.
 *
 * TODO: иконки заменить на набор из Figma после утверждения макетов.
 */
function TabIcon({ glyph, color }: { glyph: string; color: ColorValue }) {
  return <Text style={{ color, fontSize: 20 }}>{glyph}</Text>;
}

export default function TabsLayout() {
  const { t } = useTranslation();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.purple,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
        },
        tabBarLabelStyle: { fontSize: 11 },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: t('tabs.home'),
          tabBarIcon: ({ color }) => <TabIcon glyph="⌂" color={color} />,
        }}
      />
      <Tabs.Screen
        name="casting"
        options={{
          title: t('tabs.casting'),
          tabBarIcon: ({ color }) => <TabIcon glyph="☆" color={color} />,
        }}
      />
      <Tabs.Screen
        name="premiere"
        options={{
          title: t('tabs.premiere'),
          tabBarIcon: ({ color }) => <TabIcon glyph="▶" color={color} />,
        }}
      />
      <Tabs.Screen
        name="messages"
        options={{
          title: t('tabs.messages'),
          tabBarIcon: ({ color }) => <TabIcon glyph="◍" color={color} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: t('tabs.profile'),
          tabBarIcon: ({ color }) => <TabIcon glyph="◯" color={color} />,
        }}
      />
    </Tabs>
  );
}
