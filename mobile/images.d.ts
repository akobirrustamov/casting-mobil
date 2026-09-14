/**
 * Картинки как модули.
 *
 * Metro превращает `import icon from './icon.png'` в число — ссылку на
 * ресурс в бандле. TypeScript про это не знает, и без объявления любой
 * импорт картинки — ошибка типов.
 *
 * ⚠️ Тип `number`, а не `any`: именно его ждут `Image` из `expo-image` и
 * `require()`-совместимые пропсы. С `any` ошибка в пропсе прошла бы молча.
 */
declare module '*.png' {
  const source: number;
  export default source;
}

declare module '*.jpg' {
  const source: number;
  export default source;
}

/**
 * Шрифты — тоже ресурсы Metro (`assetExts` содержит `ttf`).
 *
 * `require`/`import` файла шрифта возвращает такое же число-ссылку, как у
 * картинок, — именно его ждёт `useFonts` из `expo-font`.
 */
declare module '*.ttf' {
  const source: number;
  export default source;
}
