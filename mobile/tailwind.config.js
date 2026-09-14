/** @type {import('tailwindcss').Config} */
// Палитра и UI-правила: ТЗ V2 стр. 18 «PREMIUM DESIGN SYSTEM» плюс референс
// заказчика от 26.08.2026 (синий конец градиента, лаймовый акцент, более
// глубокий фон). Разбор — в src/theme/tokens.ts.
//
// ⚠️ Тот же набор продублирован в src/theme/tokens.ts для мест без className.
// Меняем палитру — правим ОБА файла.
const plugin = require('tailwindcss/plugin');

/**
 * Начертания Manrope. Имена обязаны совпадать с `src/theme/typography.ts` —
 * там они регистрируются в `useFonts`, здесь раздаются классам.
 */
const FONT = {
  regular: 'Manrope-Regular',
  medium: 'Manrope-Medium',
  semibold: 'Manrope-SemiBold',
  bold: 'Manrope-Bold',
  extrabold: 'Manrope-ExtraBold',
};

/**
 * Шрифт на размерных классах: `text-body`, `text-h1` и т.д.
 *
 * Так фирменный шрифт достаётся ВСЕМ надписям разом, без правки двух сотен
 * `<Text>` по экранам. Крупным заголовкам сразу даётся жирное начертание —
 * раньше его давал `fontWeight` внутри `fontSize`, а с нестандартным
 * шрифтом вес выбирается только именем файла (см. `theme/typography`).
 */
const fontBySize = plugin(({ addUtilities }) => {
  addUtilities({
    '.text-display': { 'font-family': FONT.extrabold },
    '.text-h1': { 'font-family': FONT.bold },
    '.text-h2': { 'font-family': FONT.bold },
    '.text-body': { 'font-family': FONT.regular },
    '.text-caption': { 'font-family': FONT.regular },
    '.text-micro': { 'font-family': FONT.regular },
    '.text-label': { 'font-family': FONT.regular },
    '.text-badge': { 'font-family': FONT.regular },
  });
});

/**
 * Классы насыщенности — вместо стандартных `font-*` из Tailwind.
 *
 * ⚠️ Они НЕ ставят `font-weight`, а меняют семейство. Стандартные утилиты
 * отключены ниже (`theme.fontWeight = {}`): `fontWeight: '600'` поверх
 * имени `Manrope-SemiBold` — это не «сделать жирнее», а сломать подбор
 * начертания (разбор — в `theme/typography`).
 *
 * Плагин идёт ВТОРЫМ в списке: CSS-правила складываются по порядку, и при
 * `class="text-h2 font-medium"` побеждает то, что ближе к концу, — то есть
 * насыщенность, заданная вручную, а не подразумеваемая размером.
 */
const fontByWeight = plugin(({ addUtilities }) => {
  addUtilities({
    '.font-normal': { 'font-family': FONT.regular },
    '.font-medium': { 'font-family': FONT.medium },
    '.font-semibold': { 'font-family': FONT.semibold },
    '.font-bold': { 'font-family': FONT.bold },
    '.font-extrabold': { 'font-family': FONT.extrabold },
  });
});

module.exports = {
  content: ['./app/**/*.{js,jsx,ts,tsx}', './src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  // Приложение только тёмное. На web без 'class' NativeWind падает с
  // «Cannot manually set color scheme, as dark mode is type 'media'».
  darkMode: 'class',
  theme: {
    // ⚠️ Пусто намеренно: стандартные `font-thin … font-black` выключены.
    // Насыщенность у нестандартного шрифта выбирается ИМЕНЕМ начертания, а
    // не числом, и свои `font-*` даёт плагин `fontByWeight` выше.
    fontWeight: {},
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
        // ⚠️ Без `fontWeight`: жирность заголовков теперь приходит
        // начертанием шрифта (плагин `fontBySize`), а не числом.
        display: ['32px', { lineHeight: '38px' }],
        h1: ['24px', { lineHeight: '30px' }],
        h2: ['20px', { lineHeight: '26px' }],
        body: ['15px', { lineHeight: '21px' }],
        caption: ['13px', { lineHeight: '18px' }],
        micro: ['11px', { lineHeight: '14px' }],
        /**
         * Мелкая метка: бейдж на обложке и «Premium» в шапке.
         *
         * Заказчик (14.09.2026) про оба: «20% kichraytirish kerak».
         * 11 → 9 и 15 → 12 — это и есть те самые 20%, но числа должны
         * лежать в одном месте, иначе следующая правка размера разведёт
         * бейдж и шапку в разные стороны.
         */
        badge: ['9px', { lineHeight: '12px' }],
        label: ['12px', { lineHeight: '16px' }],
      },
    },
  },
  // ⚠️ Порядок важен: `fontByWeight` должен идти ПОСЛЕ `fontBySize`.
  plugins: [fontBySize, fontByWeight],
};
