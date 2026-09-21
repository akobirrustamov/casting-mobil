import { Ionicons } from '@expo/vector-icons';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { CASTING_TYPES, GENDERS } from '@/features/casting/options';
import {
  EMPTY_CATALOG_FILTERS,
  matchesQuery,
  parseBound,
  toggleValue,
  type CatalogFilters,
} from '@/features/casting/catalogFilters';
import type { Gender } from '@/features/creators/types';
import { colors } from '@/theme/tokens';

/**
 * Фильтры каталога кандидатов: множественный выбор с поиском.
 *
 * <h2>Почему лист, а не ряды «чипов» на экране</h2>
 * Выбирать можно несколько значений сразу, а регионов в анкетах
 * десятки — рядом «чипов» это горизонтальная лента, где выбранное
 * уезжает за край и человек не видит, что у него включено.
 *
 * <h2>Один поиск на весь лист</h2>
 * Строка поиска сужает ВСЕ разделы сразу, и раздел без совпадений
 * просто исчезает. Отдельное поле поиска в каждом разделе выглядело бы
 * аккуратнее на макете, но на телефоне это три почти одинаковых поля,
 * и человек ищет «samarqand» в поле у направлений.
 *
 * <h2>Применяется по кнопке, а не сразу</h2>
 * Пока лист открыт, правки живут в его собственном состоянии. Иначе
 * каталог под листом пересчитывался бы на каждое нажатие, а «Тозалаш»
 * было бы нечем отменить.
 */
export function FilterSheet({
  visible,
  value,
  regions,
  countFor,
  onApply,
  onClose,
}: {
  visible: boolean;
  value: CatalogFilters;
  regions: string[];
  /** Сколько анкет останется при переданном выборе — считает экран. */
  countFor: (filters: CatalogFilters) => number;
  onApply: (next: CatalogFilters) => void;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();

  const [draft, setDraft] = useState<CatalogFilters>(value);
  const [query, setQuery] = useState('');

  // Лист открыли заново — показываем то, что реально применено.
  useEffect(() => {
    if (visible) {
      setDraft(value);
      setQuery('');
    }
  }, [visible, value]);

  const typeOptions = useMemo(
    () =>
      CASTING_TYPES.map((v) => ({ value: v as string, label: t(`casting.types.${v}`) })).filter((o) =>
        matchesQuery(o.label, query),
      ),
    [query, t],
  );

  const genderOptions = useMemo(
    () =>
      GENDERS.map((v) => ({
        value: v as Gender,
        label: v === 'female' ? t('catalog.female') : t('catalog.male'),
      })).filter((o) => matchesQuery(o.label, query)),
    [query, t],
  );

  const regionOptions = useMemo(
    () => regions.filter((r) => matchesQuery(r, query)),
    [regions, query],
  );

  const rangesVisible = query.trim() === '';
  const nothingFound =
    typeOptions.length === 0 && genderOptions.length === 0 && regionOptions.length === 0;

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable className="flex-1 justify-end bg-black/60" onPress={onClose}>
        <Pressable
          onPress={() => {}}
          style={{ paddingBottom: Math.max(insets.bottom, 12) + 8, maxHeight: '88%' }}
          className="rounded-t-card-lg border-t border-border bg-surface px-4 pt-3"
        >
          <View className="mb-3 h-1 w-10 self-center rounded-pill bg-border" />

          <View className="flex-row items-center justify-between pb-3">
            <Text className="text-body font-semibold text-text">{t('casting.filters.title')}</Text>
            <Pressable
              onPress={() => setDraft(EMPTY_CATALOG_FILTERS)}
              accessibilityRole="button"
              hitSlop={8}
              className="active:opacity-60"
            >
              <Text className="text-caption text-violet">{t('catalog.reset')}</Text>
            </Pressable>
          </View>

          <View className="flex-row items-center gap-2 rounded-card border border-border px-3">
            <Ionicons name="search" size={16} color={colors.textMuted} />
            <TextInput
              value={query}
              onChangeText={setQuery}
              placeholder={t('casting.filters.searchPlaceholder')}
              placeholderTextColor={colors.textMuted}
              autoCapitalize="none"
              autoCorrect={false}
              className="flex-1 py-3 text-body text-text"
              accessibilityLabel={t('casting.filters.searchPlaceholder')}
            />
            {query !== '' ? (
              <Pressable onPress={() => setQuery('')} hitSlop={8} accessibilityRole="button">
                <Ionicons name="close-circle" size={16} color={colors.textMuted} />
              </Pressable>
            ) : null}
          </View>

          <ScrollView
            showsVerticalScrollIndicator={false}
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={{ gap: 16, paddingVertical: 16 }}
          >
            {nothingFound ? (
              <Text className="py-6 text-center text-caption text-text-muted">
                {t('casting.filters.noMatches')}
              </Text>
            ) : null}

            {typeOptions.length > 0 ? (
              <FilterGroup title={t('casting.filters.type')}>
                {typeOptions.map((o) => (
                  <SelectChip
                    key={o.value}
                    label={o.label}
                    selected={draft.types.includes(o.value)}
                    onPress={() => setDraft((d) => ({ ...d, types: toggleValue(d.types, o.value) }))}
                  />
                ))}
              </FilterGroup>
            ) : null}

            {genderOptions.length > 0 ? (
              <FilterGroup title={t('casting.filters.gender')}>
                {genderOptions.map((o) => (
                  <SelectChip
                    key={o.value}
                    label={o.label}
                    selected={draft.genders.includes(o.value)}
                    onPress={() =>
                      setDraft((d) => ({ ...d, genders: toggleValue(d.genders, o.value) }))
                    }
                  />
                ))}
              </FilterGroup>
            ) : null}

            {regionOptions.length > 0 ? (
              <FilterGroup title={t('casting.filters.region')}>
                {regionOptions.map((r) => (
                  <SelectChip
                    key={r}
                    label={r}
                    selected={draft.regions.includes(r)}
                    onPress={() => setDraft((d) => ({ ...d, regions: toggleValue(d.regions, r) }))}
                  />
                ))}
              </FilterGroup>
            ) : null}

            {/* Диапазоны не участвуют в поиске: искать «18» среди
                названий бессмысленно, а прятать их от человека,
                который что-то набрал, — тем более. */}
            {rangesVisible ? (
              <>
                <RangeRow
                  title={t('casting.filters.age')}
                  min={draft.ageMin}
                  max={draft.ageMax}
                  onMin={(n) => setDraft((d) => ({ ...d, ageMin: n }))}
                  onMax={(n) => setDraft((d) => ({ ...d, ageMax: n }))}
                />
                <RangeRow
                  title={t('casting.filters.height')}
                  min={draft.heightMin}
                  max={draft.heightMax}
                  onMin={(n) => setDraft((d) => ({ ...d, heightMin: n }))}
                  onMax={(n) => setDraft((d) => ({ ...d, heightMax: n }))}
                />
              </>
            ) : null}
          </ScrollView>

          <Button variant="primary" onPress={() => onApply(draft)}>
            {t('casting.filters.apply', { count: countFor(draft) })}
          </Button>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function FilterGroup({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View className="gap-2">
      <Text className="text-caption text-text-muted">{title}</Text>
      <View className="flex-row flex-wrap gap-2">{children}</View>
    </View>
  );
}

/** «Чип» с галочкой: видно, что выбранных может быть несколько. */
function SelectChip({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="checkbox"
      accessibilityState={{ checked: selected }}
      className={`flex-row items-center gap-1.5 rounded-pill border px-3 py-2 active:opacity-70 ${
        selected ? 'bg-purple' : ''
      }`}
      style={{ borderColor: selected ? colors.purple : colors.border }}
    >
      {selected ? <Ionicons name="checkmark" size={14} color={colors.white} /> : null}
      <Text className={`text-caption ${selected ? 'text-white' : 'text-text-muted'}`}>{label}</Text>
    </Pressable>
  );
}

function RangeRow({
  title,
  min,
  max,
  onMin,
  onMax,
}: {
  title: string;
  min: number | null;
  max: number | null;
  onMin: (value: number | null) => void;
  onMax: (value: number | null) => void;
}) {
  const { t } = useTranslation();

  return (
    <View className="gap-2">
      <Text className="text-caption text-text-muted">{title}</Text>
      <View className="flex-row items-center gap-2">
        <BoundInput value={min} placeholder={t('casting.filters.from')} onChange={onMin} />
        <Text className="text-body text-text-muted">—</Text>
        <BoundInput value={max} placeholder={t('casting.filters.to')} onChange={onMax} />
      </View>
    </View>
  );
}

function BoundInput({
  value,
  placeholder,
  onChange,
}: {
  value: number | null;
  placeholder: string;
  onChange: (value: number | null) => void;
}) {
  return (
    <TextInput
      value={value === null ? '' : String(value)}
      onChangeText={(raw) => onChange(parseBound(raw))}
      placeholder={placeholder}
      placeholderTextColor={colors.textMuted}
      keyboardType="number-pad"
      maxLength={3}
      accessibilityLabel={placeholder}
      className="flex-1 rounded-card border border-border px-3 py-3 text-body text-text"
    />
  );
}
