import { Tabs } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { TabBar } from '@/components/navigation/TabBar';

/**
 * 5 вкладок зафиксированы в ТЗ (V2, стр. 19 «NAVIGATION + ROLE SYSTEM»)
 * и подтверждены таб-баром на мокапах V4:
 * Bosh sahifa · Casting · Premyera · Xabarlar · Profil
 *
 * Роль Creator добавляет Creator Studio / Upload / Revenue / Withdraw —
 * это не 6-я вкладка, а раздел внутри Profil.
 *
 * Сам бар отрисован своим компонентом — стандартный не даёт
 * плавающую капсулу и анимацию переключения, как в Yangi.TV.
 */
export default function TabsLayout() {
  const { t } = useTranslation();

  return (
    <Tabs
      tabBar={(props) => <TabBar {...props} />}
      screenOptions={{
        headerShown: false,
        // Плавное перетекание вместо резкой подмены экрана
        animation: 'fade',
      }}
    >
      <Tabs.Screen name="index" options={{ title: t('tabs.home') }} />
      <Tabs.Screen name="casting" options={{ title: t('tabs.casting') }} />
      <Tabs.Screen name="premiere" options={{ title: t('tabs.premiere') }} />
      <Tabs.Screen name="messages" options={{ title: t('tabs.messages') }} />
      <Tabs.Screen name="profile" options={{ title: t('tabs.profile') }} />
    </Tabs>
  );
}
