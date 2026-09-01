import { useTranslation } from 'react-i18next';
import { Modal, Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

/**
 * Выбор жанра для сетки «Media» — то, что открывает знак с ползунками
 * в шапке (макет заказчика от 01.09.2026).
 *
 * <h2>Почему именно жанр</h2>
 * Тип контента уже разобран вкладками сверху, а больше фид про карточку
 * ничего не рассказывает: в `ContentCard` из осей ТЗ §13 приходит только
 * `genre`. Фильтр по категории потребовал бы второго запроса и смешал бы
 * оси — в «Драме» лежат и сериал, и подкаст.
 *
 * ⚠️ Список жанров считается ПО ЗАГРУЖЕННЫМ карточкам, а не берётся
 * справочником: справочник отдал бы жанры, под которые в этой вкладке
 * нет ни одного контента, и человек выбирал бы заведомо пустой фильтр.
 */
export function GenreSheet({
  visible,
  genres,
  selected,
  onSelect,
  onClose,
}: {
  visible: boolean;
  genres: string[];
  selected: string | null;
  onSelect: (genre: string | null) => void;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable className="flex-1 justify-end bg-black/60" onPress={onClose}>
        <Pressable
          onPress={() => {}}
          style={{ paddingBottom: Math.max(insets.bottom, 12) + 8 }}
          className="rounded-t-card-lg border-t border-border bg-surface px-4 pt-3"
        >
          <View className="mb-3 h-1 w-10 self-center rounded-pill bg-border" />

          <View className="flex-row items-center justify-between">
            <Text className="text-body font-semibold text-text">{t('premiere.genre')}</Text>
            {selected ? (
              <Pressable
                onPress={() => onSelect(null)}
                accessibilityRole="button"
                hitSlop={8}
                className="active:opacity-60"
              >
                <Text className="text-caption text-violet">{t('catalog.reset')}</Text>
              </Pressable>
            ) : null}
          </View>

          {/* Жанров может быть много — лист не должен вырасти во весь экран. */}
          <ScrollView
            style={{ maxHeight: 320 }}
            showsVerticalScrollIndicator={false}
            contentContainerClassName="flex-row flex-wrap gap-2 py-3"
          >
            <GenreChip
              label={t('catalog.all')}
              active={selected === null}
              onPress={() => onSelect(null)}
            />
            {genres.map((genre) => (
              <GenreChip
                key={genre}
                label={genre}
                active={genre === selected}
                onPress={() => onSelect(genre)}
              />
            ))}
          </ScrollView>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function GenreChip({
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
      className={`rounded-pill border px-4 py-2 active:opacity-70 ${
        active ? 'border-purple bg-purple' : 'border-border bg-surface-2'
      }`}
    >
      <Text
        className={`text-caption ${active ? 'font-semibold text-white' : 'text-text-muted'}`}
      >
        {label}
      </Text>
    </Pressable>
  );
}
