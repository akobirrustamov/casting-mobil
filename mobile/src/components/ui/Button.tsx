import { ActivityIndicator, Pressable, Text, type PressableProps } from 'react-native';

import { TOUCH_TARGET, colors } from '@/theme/tokens';

/**
 * ТЗ: на экране только один главный CTA, touch target минимум 44px.
 * primary  — Neon Purple, основное действие
 * premium  — Magenta, покупка / premium
 * gold     — Gold, вывод денег в Creator Studio (по мокапу V4)
 * secondary — контурная, второстепенное действие
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
};

const BG: Record<Variant, string> = {
  primary: 'bg-purple',
  premium: 'bg-magenta',
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
  ...rest
}: Props) {
  const isInactive = disabled || loading;
  const radius = shape === 'card' ? 'rounded-card' : 'rounded-pill';

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: isInactive, busy: loading }}
      disabled={isInactive}
      style={{ minHeight: TOUCH_TARGET }}
      className={`flex-row items-center justify-center gap-2 px-6 ${radius} ${BG[variant]} ${
        isInactive ? 'opacity-40' : 'active:opacity-80'
      } ${className}`}
      {...rest}
    >
      {loading ? (
        <ActivityIndicator
          size="small"
          color={variant === 'gold' ? colors.ink : colors.white}
        />
      ) : null}
      <Text className={`text-body font-semibold ${FG[variant]}`}>{children}</Text>
    </Pressable>
  );
}
