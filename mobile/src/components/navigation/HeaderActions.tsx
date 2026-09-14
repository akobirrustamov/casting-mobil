import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * Правая часть шапки главной: «Premium» и колокольчик (макет заказчика
 * 01.09.2026).
 *
 * Шапка `Screen` выравнивает содержимое по ВЕРХУ (`items-start`, иначе на
 * экранах с подзаголовком кнопка уезжала бы на середину двух строк).
 * Значит держать оба знака на одной линии со словом слева может только
 * их высота.
 *
 * ⚠️ Поэтому ОБЩАЯ строка ниже осталась `h-11` (`TOUCH_TARGET` из ТЗ), а
 * уменьшенная по просьбе заказчика плашка «Premium» (14.09.2026, «20%
 * kichraytirish») стоит внутри неё с `items-center`. Задай мы новую
 * высоту самой строке — вниз уехал бы и колокольчик, и знак слева.
 */

/**
 * «Premium».
 *
 * <h2>Куда ведёт</h2>
 * В «Profil», где лежит баннер Premium с описанием. Экрана оплаты (19) нет
 * и решение по платежам через сторы не принято — придумывать его здесь
 * нельзя, а кнопка, которая на вид работает и ничего не делает, хуже
 * отсутствующей. Профиль — настоящий адрес, где про Premium написано.
 */
export function PremiumChip() {
  const { t } = useTranslation();

  return (
    <Pressable
      onPress={() => router.push('/profile')}
      accessibilityRole="button"
      // Нажатие остаётся во всю высоту шапки (`hitSlop` добирает то, что
      // недобрала уменьшенная плашка), поэтому попасть по ней не сложнее,
      // чем было: по ТЗ минимум 44px, у плашки 36.
      hitSlop={{ top: 4, bottom: 4 }}
      className="h-9 flex-row items-center gap-1 rounded-pill bg-surface px-3 active:opacity-70"
    >
      <MaterialCommunityIcons name="crown" size={14} color={colors.gold} />
      <Text className="text-label font-semibold text-text">
        {t('common.premium')}
      </Text>
    </Pressable>
  );
}

/**
 * Колокольчик уведомлений — ведёт на экран сообщений.
 *
 * <h2>Почему точка по умолчанию не горит</h2>
 * На макете у колокольчика красная точка. Источника непрочитанного в
 * приложении пока нет: `app/messages.tsx` — пустой экран с `TODO` про
 * чаты и unread badge, эндпоинта тоже нет. Постоянная точка означала бы
 * «у вас непрочитанное», после нажатия человек видел бы пустоту, а точка
 * осталась бы гореть — это выдуманные данные, а не оформление.
 *
 * Проп готов: когда появится счётчик, точка включается одной строкой.
 */
export function NotificationBell({ dot = false }: { dot?: boolean }) {
  const { t } = useTranslation();

  return (
    <Pressable
      onPress={() => router.push('/messages')}
      accessibilityRole="button"
      accessibilityLabel={t('profile.notifications')}
      className="h-11 w-11 items-center justify-center active:opacity-70"
    >
      <Ionicons name="notifications-outline" size={25} color={colors.white} />
      {dot ? (
        // Обводка цветом фона — иначе точка сливается с дужкой колокольчика.
        <View
          style={{ borderWidth: 2, borderColor: colors.ink }}
          className="absolute right-0.5 top-0.5 h-3 w-3 rounded-pill bg-danger"
        />
      ) : null}
    </Pressable>
  );
}

/** Оба знака вместе — то, что уходит в `headerRight`. */
export function HomeHeaderActions() {
  return (
    <View className="h-11 flex-row items-center gap-2">
      <PremiumChip />
      <NotificationBell />
    </View>
  );
}
