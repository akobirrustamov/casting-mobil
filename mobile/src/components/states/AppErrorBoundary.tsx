import type { ErrorBoundaryProps } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '@/theme/tokens';

/**
 * Экран вместо чёрного прямоугольника.
 *
 * <h2>⚠️ Что здесь чинится</h2>
 * В release-сборке у ошибки рендера нет красного экрана: expo-router
 * подставляет свой минимальный фолбэк, и человек видит РОВНО ЧЁРНЫЙ
 * экран — без текста, без кнопки, без единой зацепки. Отсюда и жалоба
 * «kirdan song qopqora ekran»: приложение живо, но упавший экран нечем
 * ни прочитать, ни перезапустить.
 *
 * ⚠️ Диагностика важнее вида. Сообщение и первые строки стека
 * показываются ЦЕЛИКОМ и копируются одной кнопкой: без этого поиск
 * причины упирается в `adb logcat`, которого у тестировщика нет.
 *
 * <h2>⚠️ Почему здесь нет ни i18n, ни иконок</h2>
 * Это последний рубеж: он обязан отрисоваться даже тогда, когда
 * сломалось то, на что опирается весь остальной интерфейс. Любая
 * зависимость (переводы, шрифт иконок, тема навигации) — это ещё один
 * способ упасть уже внутри обработчика падения, и тогда экран снова
 * станет чёрным, но теперь необъяснимо.
 *
 * Поэтому текст зашит по-узбекски, а из внешнего — только палитра.
 */

/** Сколько строк стека показываем: дальше идут кадры самого React. */
const STACK_LINES = 12;

export function AppErrorBoundary({ error, retry }: ErrorBoundaryProps) {
  const insets = useSafeAreaInsets();
  const [copied, setCopied] = useState(false);

  const details = describe(error);

  /**
   * ⚠️ Буфер обмена подгружается ЛЕНИВО и под `try`.
   *
   * `expo-clipboard` — нативный модуль. Обычный импорт наверху выполнится
   * при монтировании экрана ошибки, и если в сборке модуля нет, обработчик
   * падения упадёт сам.
   */
  const copy = async () => {
    try {
      // eslint-disable-next-line @typescript-eslint/no-require-imports
      const clipboard = require('expo-clipboard') as typeof import('expo-clipboard');
      await clipboard.setStringAsync(details);
      setCopied(true);
    } catch {
      // Буфера нет — текст всё равно на экране, его можно переснять.
    }
  };

  return (
    <View
      style={{
        flex: 1,
        backgroundColor: colors.ink,
        paddingTop: insets.top + 24,
        paddingBottom: insets.bottom + 24,
        paddingHorizontal: 20,
        gap: 16,
      }}
    >
      <Text style={{ color: colors.white, fontSize: 22, fontWeight: '700' }}>
        Ilovada xatolik
      </Text>
      <Text style={{ color: colors.textMuted, fontSize: 14, lineHeight: 20 }}>
        Ekran ochilmadi. Quyidagi matnni nusxalab dasturchiga yuboring — sabab aynan shu
        yerda yozilgan.
      </Text>

      <ScrollView
        style={{
          flex: 1,
          backgroundColor: colors.surface,
          borderColor: colors.border,
          borderWidth: 1,
          borderRadius: 12,
        }}
        contentContainerStyle={{ padding: 12 }}
      >
        <Text selectable style={{ color: colors.danger, fontSize: 12, lineHeight: 18 }}>
          {details}
        </Text>
      </ScrollView>

      <View style={{ flexDirection: 'row', gap: 12 }}>
        <Pressable
          onPress={() => void retry()}
          accessibilityRole="button"
          style={{
            flex: 1,
            backgroundColor: colors.purple,
            borderRadius: 12,
            paddingVertical: 14,
            alignItems: 'center',
          }}
        >
          <Text style={{ color: colors.white, fontSize: 15, fontWeight: '600' }}>
            Qayta urinish
          </Text>
        </Pressable>

        <Pressable
          onPress={() => void copy()}
          accessibilityRole="button"
          style={{
            flex: 1,
            backgroundColor: colors.surface2,
            borderRadius: 12,
            paddingVertical: 14,
            alignItems: 'center',
          }}
        >
          <Text style={{ color: colors.white, fontSize: 15, fontWeight: '600' }}>
            {copied ? 'Nusxalandi' : 'Xatoni nusxalash'}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

/**
 * Ошибка → текст, который не стыдно переслать.
 *
 * ⚠️ Брошено может быть что угодно, не только `Error`: строка, `undefined`,
 * объект ответа axios. Обращение к `.message` у таких значений — вторая
 * ошибка внутри обработчика первой.
 */
export function describe(error: unknown): string {
  if (error instanceof Error) {
    const stack = (error.stack ?? '').split('\n').slice(0, STACK_LINES).join('\n');

    // ⚠️ `stack` у Hermes уже начинается с имени и сообщения — второй раз
    // их печатать не надо, иначе первые две строки дублируются.
    return stack.includes(error.message)
      ? stack
      : `${error.name}: ${error.message}\n${stack}`;
  }

  try {
    return `Xato (Error emas): ${JSON.stringify(error)}`;
  } catch {
    return `Xato (Error emas): ${String(error)}`;
  }
}
