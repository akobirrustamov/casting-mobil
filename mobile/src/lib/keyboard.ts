import { useEffect, useState } from 'react';
import { Keyboard, Platform } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

/**
 * Сколько экрана снизу закрыто клавиатурой (0 — клавиатуры нет).
 *
 * <h2>Зачем свой хук, а не `KeyboardAvoidingView`</h2>
 * С SDK 54 Android рисует приложение «от края до края» (edge-to-edge), и
 * `windowSoftInputMode=adjustResize` больше НЕ сжимает окно: клавиатура
 * просто ложится поверх. `KeyboardAvoidingView` на Android измеряет своё
 * место в окне — а окно не поменялось, и поле ввода остаётся под
 * клавиатурой (жалоба 22.09.2026: «komment yozish bosilganda klaviatura
 * ostiga qolib ketyapti»).
 *
 * Здесь считаем отступ сами — по событиям клавиатуры. Это чистый JS:
 * уезжает по воздуху (`eas update`), нативной библиотеки не требует.
 *
 * <h2>⚠️ Android и iOS считают высоту по-разному</h2>
 * Android (`ReactRootView.checkForKeyboardEvents`) отдаёт
 * `ime.bottom - systemBars.bottom`, то есть БЕЗ полосы навигации. А окно
 * в edge-to-edge занимает весь экран, полоса навигации — часть окна,
 * поэтому её надо вернуть: иначе поле ввода встанет ниже клавиатуры
 * ровно на высоту полосы.
 *
 * iOS отдаёт полную высоту клавиатуры от низа экрана — добавлять нечего.
 */
export function useKeyboardInset(): number {
  const insets = useSafeAreaInsets();
  const [inset, setInset] = useState(0);

  useEffect(() => {
    // iOS присылает `will*` заранее — поле едет вместе с клавиатурой, а не
    // догоняет её. На Android этих событий нет вовсе, только `did*`.
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const hideEvent = Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide';

    const show = Keyboard.addListener(showEvent, (e) => {
      const height = e.endCoordinates?.height ?? 0;
      if (height <= 0) return;
      setInset(Platform.OS === 'android' ? height + insets.bottom : height);
    });
    const hide = Keyboard.addListener(hideEvent, () => setInset(0));

    return () => {
      show.remove();
      hide.remove();
    };
  }, [insets.bottom]);

  return inset;
}
