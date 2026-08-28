import { LinearGradient } from 'expo-linear-gradient';
import type { ReactNode } from 'react';
import { View } from 'react-native';

import { gradients, radius } from '@/theme/tokens';

/**
 * Карточка с фирменной кромкой сверху.
 *
 * Приём прямо с референса заказчика: тёмная карточка, по верхнему краю —
 * тонкая линия синий → фиолетовый → маджента. Она задаёт карточке верх и
 * отделяет её от чёрного фона там, где обычной рамки не хватает: на почти
 * чёрном фоне граница в один пиксель просто не видна.
 *
 * Кромка декоративная, поэтому это не `border`: рамка со всех сторон
 * превратила бы карточку в подсвеченный прямоугольник, а на референсе
 * светится ровно один край.
 */
export function GlowCard({
  children,
  className = '',
  edge = true,
}: {
  children: ReactNode;
  className?: string;
  /** Выключить кромку — для второстепенных карточек в том же списке. */
  edge?: boolean;
}) {
  return (
    <View
      style={{ borderRadius: radius.cardLg, overflow: 'hidden' }}
      className={`bg-surface ${className}`}
    >
      {edge ? (
        <LinearGradient
          colors={gradients.brandWide}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 0 }}
          style={{ height: 2, width: '100%' }}
        />
      ) : null}
      {children}
    </View>
  );
}
