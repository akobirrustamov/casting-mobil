import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';

/**
 * Сообщения.
 *
 * <h2>Почему это больше не вкладка</h2>
 * На макете заказчика «Landing Page» в нижнем баре её нет — там
 * `Bosh sahifa · Media · Casting · Saqlanganlar · Profil`. Экран не удалён:
 * он открывается колокольчиком из профиля и остаётся на месте, когда
 * заказчик решит вернуть его в бар.
 *
 * Прятать его через `href: null` было нельзя: expo-router в этом режиме
 * лишь ставит кнопке `display: none`, а таб-бар у нас свой и рисует всё,
 * что придёт в `state.routes`.
 *
 * TODO: чаты, unread badge, системные сообщения, статусы заявок,
 * deep-link из пуша.
 */
export default function MessagesScreen() {
  const { t } = useTranslation();

  return (
    <Screen
      title={t('tabs.messages')}
      scroll={false}
      underTabBar={false}
      onBack={() => router.back()}
    >
      <ScreenState kind="empty" />
    </Screen>
  );
}
