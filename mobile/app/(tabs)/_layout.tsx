import { Tabs } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { TabBar } from '@/components/navigation/TabBar';

/**
 * Состав и порядок вкладок — с макета заказчика «Landing Page» (27.08.2026):
 * Bosh sahifa · Media · Casting · Saqlanganlar · Profil
 *
 * ⚠️ Это НЕ то, что записано в ТЗ (V2 стр. 19): там
 * `Bosh sahifa · Casting · Premyera · Xabarlar · Profil`. Заказчик прислал
 * макет и попросил порядок именно оттуда, поэтому здесь макет, а не ТЗ.
 *
 * Отличия от ТЗ, которые стоит держать в голове:
 *   - «Premyera» в баре называется «Media» — экран тот же, каталог контента;
 *   - «Saqlanganlar» (сохранённое) заняла место «Xabarlar»;
 *   - экран сообщений НЕ удалён, он уехал из `(tabs)` в `app/messages.tsx`
 *     и открывается колокольчиком из профиля. `href: null` тут не подходит:
 *     expo-router в этом режиме лишь ставит кнопке `display: none`, а бар
 *     у нас свой и рисует всё, что придёт в `state.routes`.
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
      <Tabs.Screen name="premiere" options={{ title: t('tabs.media') }} />
      <Tabs.Screen name="casting" options={{ title: t('tabs.casting') }} />
      <Tabs.Screen name="favorites" options={{ title: t('tabs.saved') }} />
      <Tabs.Screen name="profile" options={{ title: t('tabs.profile') }} />
    </Tabs>
  );
}
