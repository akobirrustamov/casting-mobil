import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '@/theme/tokens';

/**
 * Дата рождения — выбор в календаре вместо ввода цифрами.
 *
 * <h2>⚠️ Почему свой календарь, а не `@react-native-community/datetimepicker`</h2>
 * Нативный модуль — это новая сборка в сторах; свой календарь уезжает
 * обычным OTA-обновлением. Плюс у системного пикера год рождения
 * приходится листать назад по месяцу — здесь сначала выбирают год.
 *
 * Значение — та же строка `ДД.ММ.ГГГГ`, что и раньше вводилась руками:
 * проверка (`parseBirthday`) и тело запроса от выбора способа не зависят.
 */

/** Сколько лет назад показывать в списке годов. */
const YEARS_BACK = 100;
/** С какого года открывать пустой календарь — возраст типичного кандидата. */
const DEFAULT_AGE = 20;

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

function parse(value: string): { y: number; m: number; d: number } | null {
  const m = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(value);
  if (!m) return null;
  return { d: Number(m[1]), m: Number(m[2]) - 1, y: Number(m[3]) };
}

export function BirthdayField({
  label,
  value,
  onChange,
  placeholder,
  required = false,
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  required?: boolean;
  error?: string | null;
}) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const today = new Date();
  const picked = parse(value);

  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState<'days' | 'years'>('days');
  const [year, setYear] = useState(picked?.y ?? today.getFullYear() - DEFAULT_AGE);
  const [month, setMonth] = useState(picked?.m ?? 0);

  const months = t('casting.form.calendar.months', { returnObjects: true }) as string[];
  const weekdays = t('casting.form.calendar.weekdays', { returnObjects: true }) as string[];

  const show = () => {
    // Открываем там, где стоит выбранная дата, а не где листали в прошлый раз.
    if (picked) {
      setYear(picked.y);
      setMonth(picked.m);
      setMode('days');
    } else {
      // Пустое поле — сразу список годов: год рождения выбирают первым.
      setMode('years');
    }
    setOpen(true);
  };

  const isFuture = (y: number, m: number, d: number) => new Date(y, m, d).getTime() > today.getTime();

  const shiftMonth = (delta: number) => {
    const next = new Date(year, month + delta, 1);
    if (next.getFullYear() < today.getFullYear() - YEARS_BACK) return;
    if (isFuture(next.getFullYear(), next.getMonth(), 1)) return;
    setYear(next.getFullYear());
    setMonth(next.getMonth());
  };

  // Сетка месяца с понедельника: пустые клетки до первого числа.
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const lead = (new Date(year, month, 1).getDay() + 6) % 7;
  const cells: (number | null)[] = [
    ...Array.from({ length: lead }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];
  while (cells.length % 7 !== 0) cells.push(null);

  const years = Array.from({ length: YEARS_BACK + 1 }, (_, i) => today.getFullYear() - i);
  const canNext = !isFuture(new Date(year, month + 1, 1).getFullYear(), new Date(year, month + 1, 1).getMonth(), 1);

  return (
    <View className="gap-1.5">
      <Text className="text-caption text-text-muted">
        {label}
        {required ? <Text style={{ color: colors.magenta }}> *</Text> : null}
      </Text>
      <Pressable
        onPress={show}
        accessibilityRole="button"
        accessibilityLabel={label}
        className="flex-row items-center rounded-card border bg-surface-2 px-3 py-3 active:opacity-70"
        style={{ borderColor: error ? colors.danger : colors.border }}
      >
        <Text className="flex-1 text-body" style={{ color: value ? colors.white : colors.textDisabled }}>
          {value || placeholder}
        </Text>
        <Ionicons name="calendar-outline" size={18} color={colors.textMuted} />
      </Pressable>
      {error ? (
        <Text className="text-micro" style={{ color: colors.danger }}>
          {error}
        </Text>
      ) : null}

      <Modal visible={open} transparent animationType="slide" onRequestClose={() => setOpen(false)} statusBarTranslucent>
        <Pressable className="flex-1 justify-end bg-black/60" onPress={() => setOpen(false)}>
          {/* Нажатие внутри окна не должно его закрывать. */}
          <Pressable
            onPress={(e) => e.stopPropagation()}
            style={{ paddingBottom: insets.bottom + 16 }}
            className="gap-3 rounded-t-card-lg bg-surface px-4 pt-3"
          >
            <View className="h-1 w-10 self-center rounded-pill bg-border" />

            <View className="flex-row items-center justify-between">
              <Pressable
                onPress={() => shiftMonth(-1)}
                disabled={mode === 'years'}
                accessibilityRole="button"
                hitSlop={8}
                className="h-10 w-10 items-center justify-center active:opacity-70"
                style={{ opacity: mode === 'years' ? 0 : 1 }}
              >
                <Ionicons name="chevron-back" size={22} color={colors.white} />
              </Pressable>

              <Pressable
                onPress={() => setMode(mode === 'days' ? 'years' : 'days')}
                accessibilityRole="button"
                className="flex-row items-center gap-1 active:opacity-70"
              >
                <Text className="text-title font-semibold text-text">
                  {months[month]} {year}
                </Text>
                <Ionicons name={mode === 'days' ? 'chevron-down' : 'chevron-up'} size={16} color={colors.textMuted} />
              </Pressable>

              <Pressable
                onPress={() => shiftMonth(1)}
                disabled={mode === 'years' || !canNext}
                accessibilityRole="button"
                hitSlop={8}
                className="h-10 w-10 items-center justify-center active:opacity-70"
                style={{ opacity: mode === 'years' ? 0 : canNext ? 1 : 0.3 }}
              >
                <Ionicons name="chevron-forward" size={22} color={colors.white} />
              </Pressable>
            </View>

            {mode === 'years' ? (
              <ScrollView style={{ maxHeight: 320 }} showsVerticalScrollIndicator={false}>
                <View className="flex-row flex-wrap">
                  {years.map((y) => {
                    const active = y === year;
                    return (
                      <View key={y} style={{ width: '25%' }} className="p-1">
                        <Pressable
                          onPress={() => {
                            setYear(y);
                            // Год текущий, а месяц ещё не наступил — откатываемся на сегодняшний.
                            if (isFuture(y, month, 1)) setMonth(today.getMonth());
                            setMode('days');
                          }}
                          accessibilityRole="button"
                          accessibilityState={{ selected: active }}
                          className={`items-center rounded-card py-3 active:opacity-70 ${active ? 'bg-purple' : 'bg-surface-2'}`}
                        >
                          <Text className="text-body text-text">{y}</Text>
                        </Pressable>
                      </View>
                    );
                  })}
                </View>
              </ScrollView>
            ) : (
              <View>
                <View className="flex-row">
                  {weekdays.map((w) => (
                    <View key={w} style={{ width: `${100 / 7}%` }} className="items-center py-1">
                      <Text className="text-micro text-text-muted">{w}</Text>
                    </View>
                  ))}
                </View>
                <View className="flex-row flex-wrap">
                  {cells.map((d, i) => {
                    if (d === null) return <View key={`e${i}`} style={{ width: `${100 / 7}%`, height: 44 }} />;
                    const active = picked?.y === year && picked?.m === month && picked?.d === d;
                    const disabled = isFuture(year, month, d);
                    return (
                      <View key={d} style={{ width: `${100 / 7}%`, height: 44 }} className="p-0.5">
                        <Pressable
                          onPress={() => {
                            onChange(`${pad(d)}.${pad(month + 1)}.${year}`);
                            setOpen(false);
                          }}
                          disabled={disabled}
                          accessibilityRole="button"
                          accessibilityState={{ selected: active, disabled }}
                          className={`flex-1 items-center justify-center rounded-pill active:opacity-70 ${active ? 'bg-purple' : ''}`}
                          style={{ opacity: disabled ? 0.3 : 1 }}
                        >
                          <Text className="text-body text-text">{d}</Text>
                        </Pressable>
                      </View>
                    );
                  })}
                </View>
              </View>
            )}
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}
