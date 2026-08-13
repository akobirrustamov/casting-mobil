/** @type {import('tailwindcss').Config} */
// Палитра и UI-правила зафиксированы в ТЗ:
// UzCasting_Premium_UIUX_Texnik_Topshiriq_V2.pdf, стр. 18 «PREMIUM DESIGN SYSTEM».
// Значения из ТЗ менять нельзя — это утверждённые токены.
module.exports = {
  content: ['./app/**/*.{js,jsx,ts,tsx}', './src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  // Приложение только тёмное. На web без 'class' NativeWind падает с
  // «Cannot manually set color scheme, as dark mode is type 'media'».
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // --- Из ТЗ ---
        ink: '#07070D', // Deep Black — фон
        surface: '#11111F', // Midnight — карточка
        purple: '#7C3AED', // Neon Purple — основной CTA
        magenta: '#EC4899', // Magenta — premium / highlight
        cyan: '#22D3EE', // Electric Cyan — info / secondary
        gold: '#F5C542', // Gold — premium / verified
        // --- Производные, для 8 обязательных состояний ---
        'surface-2': '#1A1A2E', // приподнятая карточка / skeleton
        border: '#252540',
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
