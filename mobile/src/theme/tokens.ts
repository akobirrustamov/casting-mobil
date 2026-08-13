/**
 * Дизайн-токены UzCasting.
 *
 * Источник: UzCasting_Premium_UIUX_Texnik_Topshiriq_V2.pdf, стр. 18
 * «17. PREMIUM DESIGN SYSTEM».
 *
 * Дублирует tailwind.config.js для мест, где className недоступен:
 * навигация, StatusBar, нативные опции экранов, градиенты.
 * При изменении палитры правим оба файла.
 */

export const colors = {
  // --- Утверждённые в ТЗ ---
  ink: '#07070D', // Deep Black — фон
  surface: '#11111F', // Midnight — карточка
  purple: '#7C3AED', // Neon Purple — основной CTA
  magenta: '#EC4899', // Magenta — premium / highlight
  cyan: '#22D3EE', // Electric Cyan — info / secondary
  gold: '#F5C542', // Gold — premium / verified
  white: '#FFFFFF', // основной текст

  // --- Производные, нужны для 8 обязательных состояний ---
  surface2: '#1A1A2E',
  border: '#252540',
  textMuted: '#9A9AB8',
  textDisabled: '#5A5A75',
  success: '#34D399',
  danger: '#F87171',
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
