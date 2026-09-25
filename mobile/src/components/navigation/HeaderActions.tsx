import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import {
  APP_KIND,
  messagesRoute,
  useUnreadCount,
  type NotificationKind,
} from '@/features/notifications/api';
import { pushOnce } from '@/lib/navigation';
import { colors } from '@/theme/tokens';
import { fonts } from '@/theme/typography';
import { usePaymentsVisible } from '@/features/config/api';

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
      onPress={() => pushOnce('/profile')}
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
 * <h2>Красный знак</h2>
 * Число непрочитанных с бэкенда (`useUnreadCount`). Раньше знака не было
 * вовсе: источника «прочитано» не существовало, а постоянная точка
 * означала бы выдуманное «у вас новое». Теперь источник есть — заказчик
 * 25.09.2026: «push keladi, lekin qizil belgi turmaydi».
 *
 * `kind` — какой список: общий (главная) или кастинг (вкладка «Casting»).
 */
export function NotificationBell({ kind = APP_KIND }: { kind?: NotificationKind }) {
  const { t } = useTranslation();
  const unread = useUnreadCount(kind);

  return (
    <Pressable
      onPress={() => pushOnce(messagesRoute(kind))}
      accessibilityRole="button"
      accessibilityLabel={t('profile.notifications')}
      className="h-11 w-11 items-center justify-center active:opacity-70"
    >
      <Ionicons name="notifications-outline" size={25} color={colors.white} />
      <UnreadBadge count={unread} />
    </Pressable>
  );
}

/**
 * Красный кружок с числом в правом верхнем углу кнопки.
 *
 * Родитель — квадратная кнопка с `items-center`; кружок лежит поверх
 * (`absolute`) и её размер не меняет.
 */
export function UnreadBadge({ count }: { count: number }) {
  if (count <= 0) return null;

  return (
    <View
      pointerEvents="none"
      // Обводка цветом фона — иначе кружок сливается с дужкой колокольчика.
      style={{
        position: 'absolute',
        top: 2,
        right: 0,
        minWidth: 18,
        height: 18,
        paddingHorizontal: 4,
        borderRadius: 9,
        borderWidth: 2,
        borderColor: colors.ink,
        backgroundColor: colors.danger,
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Text
        style={{ color: colors.white, fontSize: 10, lineHeight: 12, fontFamily: fonts.bold }}
        allowFontScaling={false}
      >
        {count > 9 ? '9+' : count}
      </Text>
    </View>
  );
}

/** Оба знака вместе — то, что уходит в `headerRight`. */
export function HomeHeaderActions() {
  const paymentsVisible = usePaymentsVisible();

  return (
    <View className="h-11 flex-row items-center gap-2">
      {paymentsVisible ? <PremiumChip /> : null}
      <NotificationBell />
    </View>
  );
}
