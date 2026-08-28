/**
 * Дизайн-токены UzCasting.
 *
 * Дублирует tailwind.config.js для мест, где className недоступен:
 * навигация, StatusBar, нативные опции экранов, градиенты, SVG.
 * **При изменении палитры править оба файла.**
 *
 * <h2>Откуда цвета</h2>
 * База — ТЗ V2 стр. 18 «17. PREMIUM DESIGN SYSTEM»: purple, magenta, cyan,
 * gold, тёмный фон.
 *
 * 26.08.2026 заказчик прислал референс (Verifix/EasyFix) со словами
 * «mana shu ranglar kombinatsiyasi chiroyli ko'rinarkan, user tomoni shu
 * uslubda qilish kerak». Из него добавлены три вещи, которых в ТЗ не было:
 *
 *   - `blue` — начало фирменного градиента (в ТЗ градиент не описан вовсе);
 *   - `lime` — акцент-указатель (стрелка на референсе);
 *   - фон ушёл почти в чистый чёрный, чтобы свечение читалось.
 *
 * Утверждённые в ТЗ цвета НЕ заменены — референс их и не отменяет:
 * purple и magenta на нём те же, добавился синий конец шкалы.
 */

export const colors = {
  // --- Фон и поверхности ---
  /** Почти чистый чёрный: на #07070D свечение выглядело грязно-серым пятном. */
  ink: '#05050A',
  surface: '#0D0D17',
  surface2: '#15152A',
  border: '#23233C',

  // --- Фирменная шкала: синий → фиолетовый → маджента ---
  /** Начало градиента. С референса заказчика. */
  blue: '#2563EB',
  /** Neon Purple — основной CTA (ТЗ). */
  purple: '#7C3AED',
  /** Светлый конец фиолетового, середина градиента текста на референсе. */
  violet: '#A855F7',
  /** Magenta — premium / highlight (ТЗ). */
  magenta: '#EC4899',

  // --- Акценты ---
  /** Electric Cyan — info / secondary (ТЗ). */
  cyan: '#22D3EE',
  /** Gold — premium / verified / VIP (ТЗ). */
  gold: '#F5C542',
  /** Акцент-указатель с референса: «смотри сюда». Не статус успеха. */
  lime: '#7DF06B',

  // --- Текст и статусы ---
  white: '#FFFFFF',
  textMuted: '#9A9AB8',
  textDisabled: '#5A5A75',
  success: '#34D399',
  danger: '#F87171',
} as const;

/**
 * Градиенты. Массивы, а не строки — `expo-linear-gradient` берёт `colors`.
 *
 * ⚠️ Направление задаёт вызывающий: у кнопки оно горизонтальное, у баннера
 * диагональное. Здесь только цвета, чтобы шкала была одна на всё приложение.
 */
// Кортежи, а не массивы: `LinearGradient` требует минимум два цвета на
// уровне типа, и обычный `string[]` он не принимает.
export const gradients = {
  /** Основное действие и фирменные подписи: синий → фиолетовый. */
  brand: [colors.blue, colors.purple] as [string, string],
  /** Полная шкала — кромка карточки, крупные плашки. */
  brandWide: [colors.blue, colors.violet, colors.magenta] as [string, string, string],
  /** Premium: покупка, подписка, VIP. */
  premium: [colors.purple, colors.magenta] as [string, string],
};

/**
 * Свечение фона.
 *
 * На референсе это два размытых пятна: фиолетовое сверху и синее снизу.
 * Именно они делают чёрный фон «дорогим» — без них экран выглядит просто
 * тёмно-серым. ТЗ: «glow-эффекты в меру», поэтому непрозрачность низкая.
 */
export const glow = {
  primary: colors.purple,
  secondary: colors.blue,
} as const;

/** ТЗ: spacing system 8–16px. */
export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

/** ТЗ: radius карточек 14–22px. */
export const radius = {
  card: 16,
  cardLg: 22,
  pill: 999,
} as const;

/** ТЗ: минимальный touch target 44px+. */
export const TOUCH_TARGET = 44;

/** ТЗ: dark mode первичен, светлой темы в проекте нет. */
export const navigationTheme = {
  dark: true,
  colors: {
    primary: colors.purple,
    background: colors.ink,
    card: colors.surface,
    text: colors.white,
    border: colors.border,
    notification: colors.magenta,
  },
  fonts: {
    regular: { fontFamily: 'System', fontWeight: '400' as const },
    medium: { fontFamily: 'System', fontWeight: '500' as const },
    bold: { fontFamily: 'System', fontWeight: '700' as const },
    heavy: { fontFamily: 'System', fontWeight: '800' as const },
  },
};
