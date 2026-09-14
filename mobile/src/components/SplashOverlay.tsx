import { View } from 'react-native';

import { Logo } from '@/components/ui/Logo';

/**
 * S01 — экран загрузки: знак UzCasting на чёрном, и больше ничего.
 *
 * Лежит поверх навигатора, а не отдельным маршрутом: иначе «/» конфликтует
 * с (tabs)/index, и переход обратно оставлял бы splash в истории.
 *
 * <h2>Почему он повторяет НАТИВНЫЙ splash один в один</h2>
 * Заказчик (14.09.2026) прислал скриншот заставки со словами «ilovada
 * kirganda har doim boshida shuni chiqadigan qilish kerak». На скриншоте —
 * ровно то, что рисует система по `expo-splash-screen` из `app.json`:
 * `assets/splash-icon.png` шириной 180 на фоне `#05050A`.
 *
 * Система показывает свою заставку, пока грузится JS, и убирает её, как
 * только появляется первый кадр приложения. Дальше кадр держим мы — и
 * если бы здесь было что-то своё, человек видел бы ДВЕ разные заставки
 * подряд. Раньше так и было: системный знак сменялся рамкой со свечением,
 * надписью «UZCASTING», подзаголовком и тремя точками «loading».
 *
 * Поэтому здесь те же три числа, что в `app.json`, и никакой анимации
 * появления: любое проявление или сдвиг выдали бы стык двух заставок,
 * который мы как раз прячем.
 *
 * ⚠️ Меняем заставку — правим ОБА места: `assets/splash-icon.png` с
 * `imageWidth` в `app.json` и этот файл. Разъехавшись, они дадут заметный
 * скачок знака на старте.
 */

/** `imageWidth` из блока `expo-splash-screen` в `app.json`. */
const MARK_WIDTH = 180;

export function SplashOverlay() {
  return (
    <View className="absolute inset-0 items-center justify-center bg-ink">
      <Logo size={MARK_WIDTH} />
    </View>
  );
}
