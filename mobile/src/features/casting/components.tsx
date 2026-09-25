import { Ionicons } from '@expo/vector-icons';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, ScrollView, Text, TextInput, View, type TextInputProps } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Badge } from '@/components/ui/Badge';
import { formatSum } from '@/lib/money';
import { colors } from '@/theme/tokens';

import { applicationStatusView, formatDate, type MyApplication } from './status';
import { usePaymentsVisible } from '@/features/config/api';

/**
 * Мелкие блоки раздела кастинга — общие для кандидата и админки.
 *
 * Своя копия чипа, а не импорт из `app/catalog/[category].tsx`: там он
 * локальный для экрана, и вынос его оттуда задел бы экран, который эта
 * задача не трогает.
 */
export function Chip({
  label,
  active,
  onPress,
}: {
  label: string;
  active: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected: active }}
      className={`rounded-pill border px-3 py-2 active:opacity-70 ${active ? 'bg-purple' : ''}`}
      style={{ borderColor: active ? colors.purple : colors.border }}
    >
      <Text className={`text-caption ${active ? 'text-white' : 'text-text-muted'}`}>{label}</Text>
    </Pressable>
  );
}

/** Ряд чипов с горизонтальной прокруткой — шесть направлений в ширину не влезают. */
export function ChipRow({ children }: { children: ReactNode }) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: 8, paddingHorizontal: 16 }}
    >
      {children}
    </ScrollView>
  );
}

/** Секция формы / карточки: заголовок и содержимое на подложке. */
export function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <View className="gap-3 rounded-card-lg border border-border bg-surface p-4">
      <Text className="text-body font-semibold text-text">{title}</Text>
      {children}
    </View>
  );
}

/**
 * Поле ввода с подписью и ошибкой.
 *
 * Ошибка — под полем, а не общим списком сверху: в длинной анкете
 * человек иначе листал бы туда-сюда, выясняя, какое из девятнадцати
 * полей не понравилось.
 */
export function Field({
  label,
  required = false,
  error,
  ...input
}: TextInputProps & { label: string; required?: boolean; error?: string | null }) {
  return (
    <View className="gap-1.5">
      <Text className="text-caption text-text-muted">
        {label}
        {required ? <Text style={{ color: colors.magenta }}> *</Text> : null}
      </Text>
      <TextInput
        placeholderTextColor={colors.textDisabled}
        className="rounded-card border bg-surface-2 px-3 py-3 text-body"
        style={{ color: colors.white, borderColor: error ? colors.danger : colors.border }}
        {...input}
      />
      {error ? (
        <Text className="text-micro" style={{ color: colors.danger }}>
          {error}
        </Text>
      ) : null}
    </View>
  );
}

/** Выбор одного значения чипами — для типа кастинга и пола. */
export function ChoiceField<T extends string>({
  label,
  options,
  value,
  onChange,
  required = false,
  error,
}: {
  label: string;
  options: { value: T; label: string }[];
  value: T | '';
  onChange: (value: T) => void;
  required?: boolean;
  error?: string | null;
}) {
  return (
    <View className="gap-1.5">
      <Text className="text-caption text-text-muted">
        {label}
        {required ? <Text style={{ color: colors.magenta }}> *</Text> : null}
      </Text>
      <View className="flex-row flex-wrap gap-2">
        {options.map((o) => (
          <Chip key={o.value} label={o.label} active={value === o.value} onPress={() => onChange(o.value)} />
        ))}
      </View>
      {error ? (
        <Text className="text-micro" style={{ color: colors.danger }}>
          {error}
        </Text>
      ) : null}
    </View>
  );
}

/**
 * Выбор одного значения из длинного списка — поле открывает шторку.
 *
 * Чипами, как у `ChoiceField`, четырнадцать регионов заняли бы пол-экрана.
 */
export function SelectField<T extends string>({
  label,
  options,
  value,
  onChange,
  placeholder,
  required = false,
  error,
}: {
  label: string;
  options: { value: T; label: string }[];
  value: T | '';
  onChange: (value: T) => void;
  placeholder?: string;
  required?: boolean;
  error?: string | null;
}) {
  const [open, setOpen] = useState(false);
  const insets = useSafeAreaInsets();
  const selected = options.find((o) => o.value === value);

  return (
    <View className="gap-1.5">
      <Text className="text-caption text-text-muted">
        {label}
        {required ? <Text style={{ color: colors.magenta }}> *</Text> : null}
      </Text>
      <Pressable
        onPress={() => setOpen(true)}
        accessibilityRole="button"
        accessibilityLabel={label}
        className="flex-row items-center rounded-card border bg-surface-2 px-3 py-3 active:opacity-70"
        style={{ borderColor: error ? colors.danger : colors.border }}
      >
        <Text
          numberOfLines={1}
          className="flex-1 text-body"
          style={{ color: selected ? colors.white : colors.textDisabled }}
        >
          {selected ? selected.label : placeholder}
        </Text>
        <Ionicons name="chevron-down" size={18} color={colors.textMuted} />
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
            style={{ paddingBottom: insets.bottom + 16, maxHeight: '80%' }}
            className="gap-3 rounded-t-card-lg bg-surface px-4 pt-3"
          >
            <View className="h-1 w-10 self-center rounded-pill bg-border" />
            <Text className="text-title font-semibold text-text">{label}</Text>
            <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
              {options.map((o) => {
                const active = o.value === value;
                return (
                  <Pressable
                    key={o.value}
                    onPress={() => {
                      onChange(o.value);
                      setOpen(false);
                    }}
                    accessibilityRole="button"
                    accessibilityState={{ selected: active }}
                    className="flex-row items-center gap-3 rounded-card bg-surface-2 px-4 py-3 active:opacity-70"
                  >
                    <Text className="flex-1 text-body text-text">{o.label}</Text>
                    {active ? <Ionicons name="checkmark" size={18} color={colors.purple} /> : null}
                  </Pressable>
                );
              })}
            </ScrollView>
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

/** Значение пункта «Boshqa» в `SelectOrTextField`. */
export const OTHER_OPTION = '__other__';

/**
 * Список + «Boshqa»: не нашёл своего в списке — пишет сам в поле ниже.
 *
 * Для viloyat и размера одежды: стандартные значения выбирают, чтобы у
 * админа не было «XL», «xl» и «Xl» тремя разными размерами; редкое
 * значение всё равно можно ввести.
 *
 * ⚠️ Режим «Boshqa» — своё состояние поля, а не вывод из `value`: пока
 * человек не начал печатать, значение пустое и неотличимо от «ничего не
 * выбрано», и поле ввода исчезало бы сразу после выбора «Boshqa».
 */
export function SelectOrTextField({
  label,
  options,
  value,
  onChange,
  placeholder,
  otherLabel,
  otherPlaceholder,
  required = false,
  error,
  maxLength,
}: {
  label: string;
  options: { value: string; label: string }[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  otherLabel: string;
  otherPlaceholder?: string;
  required?: boolean;
  error?: string | null;
  maxLength?: number;
}) {
  // Значение не из списка (например, восстановленное) — сразу режим «Boshqa».
  const [other, setOther] = useState(() => value !== '' && !options.some((o) => o.value === value));

  return (
    <>
      <SelectField
        label={label}
        required={required}
        options={[...options, { value: OTHER_OPTION, label: otherLabel }]}
        value={other ? OTHER_OPTION : value}
        onChange={(v) => {
          const isOther = v === OTHER_OPTION;
          setOther(isOther);
          onChange(isOther ? '' : v);
        }}
        placeholder={placeholder}
        error={other ? null : error}
      />
      {other ? (
        <Field
          label={otherLabel}
          required={required}
          value={value}
          onChangeText={onChange}
          placeholder={otherPlaceholder}
          error={error}
          maxLength={maxLength}
          autoFocus
        />
      ) : null}
    </>
  );
}

/** Строка «подпись — значение»; пустое значение не рисуем вовсе. */
export function InfoRow({ label, value }: { label: string; value: string | number | null | undefined }) {
  if (value === null || value === undefined || value === '') return null;
  return (
    <View className="flex-row items-start justify-between gap-3 border-t border-border py-2">
      <Text className="text-body text-text-muted">{label}</Text>
      <Text selectable className="flex-1 text-right text-body text-text">
        {String(value)}
      </Text>
    </View>
  );
}

/**
 * Статус заявки кандидата: плашка, цена, пояснение, «в каталоге».
 *
 * Один компонент на карточку во вкладке и на строку в «Мои заявки» —
 * чтобы одна и та же заявка не выглядела на двух экранах по-разному.
 */
export function ApplicationStatusBlock({
  app,
  onPress,
  compact = false,
}: {
  app: MyApplication;
  onPress?: () => void;
  compact?: boolean;
}) {
  const { t } = useTranslation();
  const paymentsVisible = usePaymentsVisible();
  const rawView = applicationStatusView(app);
  // Платежи скрыты — ни строки цены, ни заметки об оплате.
  const paymentNote =
    rawView.noteKey === 'casting.status.paid' || rawView.noteKey === 'casting.status.paymentSoon';
  const view = paymentsVisible
    ? rawView
    : { ...rawView, noteKey: paymentNote ? null : rawView.noteKey };
  const typeLabel = t(`casting.types.${app.castingType}`, { defaultValue: app.castingType });

  const body = (
    <View className="gap-2 rounded-card-lg border border-border bg-surface p-4">
      <View className="flex-row items-center justify-between gap-2">
        <View className="flex-1">
          <Text numberOfLines={1} className="text-body font-semibold text-text">
            {compact ? t('casting.myApplication') : app.name || typeLabel}
          </Text>
          <Text numberOfLines={1} className="text-caption text-text-muted">
            {[typeLabel, app.createdAt ? t('casting.status.submittedAt', { date: formatDate(app.createdAt) }) : null]
              .filter(Boolean)
              .join(' • ')}
          </Text>
        </View>
        <Badge tone={view.tone}>{t(view.labelKey)}</Badge>
      </View>

      {paymentsVisible && app.status === 'APPROVED' ? (
        <View className="flex-row items-center justify-between">
          <Text className="text-body text-text-muted">{t('casting.status.price')}</Text>
          <Text className="text-body font-semibold text-text">
            {view.price !== null
              ? t('common.price', { amount: formatSum(view.price) })
              : t('casting.status.priceNotSet')}
          </Text>
        </View>
      ) : null}

      {view.noteKey ? <Text className="text-caption text-text-muted">{t(view.noteKey)}</Text> : null}

      {view.showInCatalog ? (
        <View className="flex-row items-center gap-1.5">
          <Ionicons name="eye-outline" size={14} color={colors.success} />
          <Text className="text-caption" style={{ color: colors.success }}>
            {t('casting.status.inCatalog')}
          </Text>
        </View>
      ) : null}

      {onPress ? (
        <View className="flex-row items-center justify-end gap-1">
          <Text className="text-caption text-cyan">{t('casting.details')}</Text>
          <Ionicons name="chevron-forward" size={14} color={colors.cyan} />
        </View>
      ) : null}
    </View>
  );

  if (!onPress) return body;

  return (
    <Pressable onPress={onPress} accessibilityRole="button" className="active:opacity-80">
      {body}
    </Pressable>
  );
}
