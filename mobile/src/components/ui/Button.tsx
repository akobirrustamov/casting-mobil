import { LinearGradient } from 'expo-linear-gradient';
import type { ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  type PressableProps,
} from 'react-native';

import { TOUCH_TARGET, colors, gradients, radius } from '@/theme/tokens';

/**
 * ТЗ: на экране только один главный CTA, touch target минимум 44px.
 *
 * primary   — фирменный градиент синий → фиолетовый, основное действие
 * premium   — фиолетовый → маджента, покупка / подписка / VIP
 * gold      — Gold, вывод денег и «Premium'ga o'tish» по макету Screen 4
 * secondary — контурная, второстепенное действие
 *
 * <h2>Почему главная кнопка градиентная</h2>
 * На референсе заказчика фирменный цвет — это ПЕРЕХОД от синего к
 * фиолетовому, а не одна заливка. Плоский `bg-purple` рядом со свечением
 * фона выглядел выцветшим: тот же тон, но без глубины.
 *
 * <h2>⚠️ Градиент лежит ПОД содержимым, а не оборачивает его</h2>
 * Сначала он был обёрткой с `flex: 1` — и кнопка растянулась на всю
 * доступную высоту. На баннере премьеры это дало магентовый купол в пол-карточки,
 * который вытолкнул заголовок за края.
 *
 * Раскладку задаёт сам `Pressable`, ровно как до появления градиента:
 * строка, отступы, минимальная высота. Градиент — фон в `absoluteFill`,
 * на размеры он не влияет вообще.
 */
type Variant = 'primary' | 'premium' | 'gold' | 'secondary';

/**
 * На макетах входа и презентации кнопки — скруглённые прямоугольники,
 * а не пилюли. Форма вынесена в параметр, чтобы не переверстывать экраны,
 * которых в макете нет.
 */
type Shape = 'pill' | 'card';

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
};

/** Градиентные варианты. Остальные — плоская заливка классом. */
const GRADIENT: Partial<Record<Variant, [string, string]>> = {
  primary: gradients.brand,
  premium: gradients.premium,
};

const FLAT_BG: Record<Variant, string> = {
  primary: '',
  premium: '',
  gold: 'bg-gold',
  secondary: 'bg-transparent border border-border',
};

const FG: Record<Variant, string> = {
  primary: 'text-white',
  premium: 'text-white',
  gold: 'text-ink',
  secondary: 'text-text',
};

export function Button({
  children,
  variant = 'primary',
  shape = 'pill',
  loading = false,
  disabled = false,
  className = '',
  trailing,
  ...rest
}: Props) {
  const isInactive = disabled || loading;
  const corner = shape === 'card' ? radius.card : radius.pill;
  const stripe = GRADIENT[variant];

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
      {stripe ? (
        <LinearGradient
          colors={stripe}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={StyleSheet.absoluteFill}
        />
      ) : null}

      {loading ? (
        <ActivityIndicator
          size="small"
          color={variant === 'gold' ? colors.ink : colors.white}
        />
      ) : null}
      <Text className={`text-body font-semibold ${FG[variant]}`}>{children}</Text>
      {trailing}
    </Pressable>
  );
}
