import { Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * Строка сообщения под формой — с МЕСТОМ, забронированным заранее.
 *
 * <h2>⚠️ Зачем фиксированная высота</h2>
 * Ошибка приходит в ответ на нажатие: человек в этот момент смотрит на
 * кнопку и держит палец над ней. Пока строка появлялась в общем потоке,
 * она раздвигала раскладку — кнопка и поля прыгали ровно тогда, когда по
 * ним попадают пальцем. На повторное нажатие это стоило промаха.
 *
 * Поэтому слот стоит всегда, пустой он или нет: текст только зажигается
 * внутри уже отведённой высоты, а соседи не двигаются.
 *
 * Высота считается в строках: `lines` — сколько строк текста слот держит.
 * Двух хватает на самые длинные сообщения (`auth.passwordNotSet`,
 * `auth.phoneAlreadyRegistered`) на узком экране.
 */
type Tone = 'danger' | 'muted';

/** `caption` из tailwind.config.js — держим в одном значении с ним. */
const CAPTION_LINE_HEIGHT = 18;

const TONE_COLOR: Record<Tone, string> = {
  danger: colors.danger,
  muted: colors.textMuted,
};

export function FormMessage({
  message,
  tone = 'danger',
  lines = 2,
}: {
  message?: string | null;
  tone?: Tone;
  /** Сколько строк текста бронирует слот. */
  lines?: number;
}) {
  return (
    <View
      style={{ height: lines * CAPTION_LINE_HEIGHT, justifyContent: 'center' }}
      pointerEvents="none"
    >
      {message ? (
        <Text
          numberOfLines={lines}
          className="text-center text-caption"
          style={{ color: TONE_COLOR[tone] }}
        >
          {message}
        </Text>
      ) : null}
    </View>
  );
}
