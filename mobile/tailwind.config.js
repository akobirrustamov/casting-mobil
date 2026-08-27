/** @type {import('tailwindcss').Config} */
// Палитра и UI-правила: ТЗ V2 стр. 18 «PREMIUM DESIGN SYSTEM» плюс референс
// заказчика от 26.08.2026 (синий конец градиента, лаймовый акцент, более
// глубокий фон). Разбор — в src/theme/tokens.ts.
//
// ⚠️ Тот же набор продублирован в src/theme/tokens.ts для мест без className.
// Меняем палитру — правим ОБА файла.
module.exports = {
  content: ['./app/**/*.{js,jsx,ts,tsx}', './src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  // Приложение только тёмное. На web без 'class' NativeWind падает с
  // «Cannot manually set color scheme, as dark mode is type 'media'».
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // --- Фон и поверхности ---
        ink: '#05050A', // почти чёрный: на нём читается свечение
        surface: '#0D0D17', // карточка
        'surface-2': '#15152A', // приподнятая карточка / skeleton
        border: '#23233C',
        // --- Фирменная шкала: синий → фиолетовый → маджента ---
        blue: '#2563EB', // начало градиента (референс заказчика)
        purple: '#7C3AED', // Neon Purple — основной CTA (ТЗ)
        violet: '#A855F7', // светлый конец фиолетового
        magenta: '#EC4899', // Magenta — premium / highlight (ТЗ)
        // --- Акценты ---
        cyan: '#22D3EE', // Electric Cyan — info / secondary (ТЗ)
        gold: '#F5C542', // Gold — premium / verified / VIP (ТЗ)
        lime: '#7DF06B', // акцент-указатель (референс заказчика)
        // --- Текст и статусы ---
        text: '#FFFFFF',
        'text-muted': '#9A9AB8',
        'text-disabled': '#5A5A75',
        success: '#34D399',
        danger: '#F87171',
      },
      borderRadius: {
        // ТЗ: radius карточек 14–22px
        card: '16px',
        'card-lg': '22px',
        pill: '999px',
      },
      spacing: {
        // ТЗ: spacing system 8–16px
        touch: '44px', // минимальный touch target из ТЗ
      },
      fontSize: {
        display: ['32px', { lineHeight: '38px', fontWeight: '800' }],
        h1: ['24px', { lineHeight: '30px', fontWeight: '700' }],
        h2: ['20px', { lineHeight: '26px', fontWeight: '700' }],
        body: ['15px', { lineHeight: '21px' }],
        caption: ['13px', { lineHeight: '18px' }],
        micro: ['11px', { lineHeight: '14px' }],
      },
    },
  },
  plugins: [],
};
