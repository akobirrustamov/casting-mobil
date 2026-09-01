import type { ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  Text,
  type PressableProps,
} from 'react-native';

import { TOUCH_TARGET, colors, radius } from '@/theme/tokens';

/**
 * ТЗ: на экране только один главный CTA, touch target минимум 44px.
 *
 * <h2>Одна кнопка на всё приложение</h2>
 * Заказчик (01.09.2026) прислал кнопку «◆ 5 000 so'm» — сплошная
 * фиолетовая заливка, белая подпись, скруглённый прямоугольник — со
 * словами «barcha joyda buttonning bg rang va dizayni shunaqa bulsin».
 *
 * До этого заливок было четыре: градиент синий → фиолетовый у главного
 * действия, отдельный сиреневый у покупки, фиолетовый → маджента у промо
 * и золотая у Premium. На одном экране их встречалось по две-три, и
 * одинаковые по смыслу кнопки на разных экранах выглядели по-разному.
 *
 * ⚠️ Имена вариантов ОСТАВЛЕНЫ, хотя все заливные рисуются одинаково.
 * Они говорят, ЧТО это за действие, — а значит, если заказчик захочет
 * вернуть золотую кнопку Premium, это правка одной строки в `FLAT_BG`, а
 * не поиск нужных кнопок по всем экранам заново.
 *
 * `secondary` осталась контурной намеренно: это единственное, что
 * отличает второстепенное действие от главного, когда они стоят рядом
 * (экран кастинга, состояния экрана). Сделай её тоже фиолетовой — и на
 * экране станет два одинаковых главных CTA, чего ТЗ прямо не допускает.
 */
type Variant = 'primary' | 'purchase' | 'premium' | 'gold' | 'secondary';

/**
 * Скруглённый прямоугольник — форма по умолчанию.
 *
 * Заказчик (28.08.2026): «buttonlar shunaqa shaklda bolsin» с картинкой
 * фиолетовой кнопки-прямоугольника. Раньше по умолчанию была пилюля, и
 * форма расходилась от экрана к экрану: вход уже был прямоугольным, а
 * баннеры и покупка оставались пилюлями.
 *
 * `pill` оставлен для мест, где кнопка стоит в ряду с другими пилюлями.
 */
type Shape = 'card' | 'pill';

type Props = Omit<PressableProps, 'children'> & {
  children: string;
  variant?: Variant;
  shape?: Shape;
  loading?: boolean;
  className?: string;
  /**
   * Знак справа от подписи — стрелка «дальше» на главном CTA.
   *
   * Именно `ReactNode`, а не имя иконки: кнопка не должна знать, из какого
   * набора знак пришёл. Сегодня это Ionicons, завтра может быть SVG.
   */
  trailing?: ReactNode;
  /** Знак слева — ромб на кнопке покупки, как на макете заказчика. */
  leading?: ReactNode;
};

/** Заливка. Одна на все заливные варианты — см. шапку файла. */
const FLAT_BG: Record<Variant, string> = {
  primary: 'bg-purple',
  purchase: 'bg-purple',
  premium: 'bg-purple',
  gold: 'bg-purple',
  secondary: 'bg-transparent border border-border',
};

const FG: Record<Variant, string> = {
  primary: 'text-white',
  purchase: 'text-white',
  premium: 'text-white',
  gold: 'text-white',
  secondary: 'text-text',
};

export function Button({
  children,
  variant = 'primary',
  shape = 'card',
  loading = false,
  disabled = false,
  className = '',
  trailing,
  leading,
  ...rest
}: Props) {
  const isInactive = disabled || loading;
  const corner = shape === 'card' ? radius.card : radius.pill;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: isInactive, busy: loading }}
      disabled={isInactive}
      style={{ minHeight: TOUCH_TARGET, borderRadius: corner, overflow: 'hidden' }}
      className={`flex-row items-center justify-center gap-2 px-6 ${FLAT_BG[variant]} ${
        isInactive ? 'opacity-40' : 'active:opacity-80'
      } ${className}`}
      {...rest}
    >
      {loading ? (
        <ActivityIndicator size="small" color={colors.white} />
      ) : (
        leading
      )}
      <Text className={`text-body font-semibold ${FG[variant]}`}>{children}</Text>
      {trailing}
    </Pressable>
  );
}
