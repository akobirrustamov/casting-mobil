import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { FormMessage } from '@/components/ui/FormMessage';
import { GlowBackdrop } from '@/components/ui/GlowBackdrop';
import { Wordmark } from '@/components/ui/Wordmark';
import { colors } from '@/theme/tokens';

/**
 * Общая раскладка трёх экранов входа: вход/регистрация, SMS-код,
 * имя и пароль.
 *
 * <h2>Зачем один каркас на три экрана</h2>
 * Заказчик (03.09.2026): «sms kod kiritishi, ism familiya kiritish
 * qismini ham bir xil qimirlamaydigan qil». Раньше каждый экран
 * складывал ярусы сам, и колонка полей стояла на разной высоте:
 *
 * • на входе знак начинался сразу под статус-баром, а на коде и пароле
 *   его сдвигала вниз строка со стрелкой «назад» — почти на её высоту;
 * • середина центрировалась (`justifyContent: 'center'`), поэтому
 *   высота полей задавала их же положение: три поля на пароле стояли
 *   выше, чем одно поле кода.
 *
 * Человек шёл «номер → код → имя и пароль», и поле каждый раз оказывалось
 * в новом месте — приходилось искать его глазами и переносить палец.
 *
 * <h2>Как стоит теперь</h2>
 * Ярусы жёсткие и одинаковые на всех трёх экранах:
 *
 * 1. знак — сразу под статус-баром и В ПОТОКЕ прокрутки (уезжает вместе
 *    с формой); стрелка «назад» лежит ПОВЕРХ и ничего не двигает;
 * 2. шапка — в слоте {@link HEADER_HEIGHT} фиксированной высоты, поэтому
 *    первое поле начинается на одной и той же высоте везде: и там, где
 *    шапка это переключатель разделов, и там, где это заголовок с
 *    подписью;
 * 3. поля — сразу под шапкой, ПРИЖАТЫ ВВЕРХ, а не по центру;
 * 4. распорка — единственное, что меняет высоту;
 * 5. нижняя группа (`footer`) — прижата к низу прокрутки;
 * 6. сообщение и главная кнопка — вне прокрутки, всегда на виду, место
 *    под сообщение забронировано (см. `FormMessage`).
 *
 * ⚠️ Вне прокрутки осталась ТОЛЬКО главная кнопка: что нажать — видно
 * всегда, даже с открытой клавиатурой. Знак прокручивается вместе с
 * формой и на низком экране просто уходит наверх, освобождая место
 * полям.
 */

/**
 * Высота слота шапки.
 *
 * Считана по самой высокой из трёх шапок — переключателю разделов на
 * входе: 52 (кнопка 44 + рамка-подложка 2×4) + 16 (`gap-4`) + 36 (две
 * строки `caption` под сообщение/подпись).
 *
 * ⚠️ Меняешь тут — проверь ВСЕ ТРИ экрана: смысл числа в том, что оно
 * одно на всех, а не в самом значении.
 */
export const HEADER_HEIGHT = 104;

export function AuthScaffold({
  onBack,
  header,
  children,
  footer,
  message,
  action,
}: {
  /** Стрелка «назад». Нет обработчика — нет и стрелки (экран входа). */
  onBack?: () => void;
  /** Переключатель разделов или заголовок с подписью — слот одной высоты. */
  header: ReactNode;
  /** Поля формы. Идут сразу под шапкой. */
  children: ReactNode;
  /** Что прижато к низу прокрутки: вход через Google, ссылка «выслать код». */
  footer?: ReactNode;
  /** Ошибка над главной кнопкой. Место под неё занято всегда. */
  message?: string | null;
  /** Единственное главное действие экрана — по ТЗ на экране один CTA. */
  action: {
    label: string;
    onPress: () => void;
    loading?: boolean;
    disabled?: boolean;
  };
}) {
  const insets = useSafeAreaInsets();

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      className="flex-1 bg-ink"
      style={{ paddingTop: insets.top }}
    >
      {/* Свечение с референса заказчика: на пустом экране входа оно и
          делает всю картинку, поэтому здесь оно ярче обычного. */}
      <GlowBackdrop intensity="hero" decor />

      {/* ⚠️ Стрелка вынута из потока намеренно. Пока она стояла строкой
          над знаком, знак и вся колонка под ним ехали вниз на её высоту —
          и экраны кода и пароля стояли ниже экрана входа. */}
      {onBack ? (
        <Pressable
          onPress={onBack}
          hitSlop={16}
          accessibilityRole="button"
          style={{ position: 'absolute', top: insets.top + 10, left: 24, zIndex: 10 }}
        >
          <Ionicons name="arrow-back" size={24} color={colors.white} />
        </Pressable>
      ) : null}

      <ScrollView
        className="flex-1"
        contentContainerStyle={{ flexGrow: 1 }}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* Знак прокручивается ВМЕСТЕ с формой (заказчик, 03.09.2026:
            «login pagelarda header logolarni fixed qilma»).

            Он и правда лучше живёт в потоке: на низком экране с открытой
            клавиатурой знак уезжает вверх и отдаёт свои ~120 пунктов
            полям, вместо того чтобы занимать треть экрана над одной
            строкой ввода. На положение колонки это не влияет — она
            по-прежнему начинается сразу под знаком и одинаково на всех
            экранах входа. */}
        <View className="items-center pb-1 pt-2">
          <Wordmark variant="stacked" markSize={92} showTagline={false} />
        </View>

        <View className="gap-4 px-6 py-2">
          {/* Слот шапки. Содержимое стоит по центру слота, а вот его НИЗ
              всегда на одной высоте — поэтому первое поле на всех трёх
              экранах начинается в одном и том же месте. */}
          <View style={{ height: HEADER_HEIGHT, justifyContent: 'center' }}>
            {header}
          </View>

          {children}
        </View>

        {/* Единственное, что меняет высоту при переходе между экранами и
            при переключении разделов. На низком экране схлопывается
            в ноль, и середина просто прокручивается. */}
        <View className="flex-1" style={{ minHeight: 16 }} />

        {footer ? <View className="gap-4 px-6 py-2">{footer}</View> : null}
      </ScrollView>

      {/* Главное действие — последним и всегда на виду. */}
      <View className="gap-3 px-6 pt-3" style={{ paddingBottom: insets.bottom + 12 }}>
        {/* ⚠️ Место под сообщение занято ВСЕГДА: ошибка приходит в ответ
            на нажатие, и если бы строка раздвигала низ экрана, кнопка
            уходила бы из-под пальца ровно в момент повторного нажатия. */}
        <FormMessage message={message} />

        <Button
          variant="primary"
          shape="card"
          loading={action.loading}
          disabled={action.disabled}
          onPress={action.onPress}
          // Стрелка идёт вплотную к подписи (отступ у кнопки один — `gap`).
          // Своего `marginLeft` тут больше нет: с ним знак отъезжал к краю
          // и висел отдельно от слова, будто это две разные кнопки.
          trailing={<Ionicons name="arrow-forward" size={18} color={colors.white} />}
        >
          {action.label}
        </Button>
      </View>
    </KeyboardAvoidingView>
  );
}
