import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FlatList, Pressable, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenState } from '@/components/states/ScreenState';
import { SearchRow } from '@/components/ui/SearchRow';
import { CATEGORIES, EXTRA_API_TYPES } from '@/features/catalog/categories';
import { useCreators } from '@/features/creators/api';
import { MIN_QUERY_LENGTH, searchCreators } from '@/features/creators/search';
import { colors } from '@/theme/tokens';

/**
 * Поиск.
 *
 * По ТЗ это не отдельная вкладка, а экран за строкой на главной.
 * Устройство — как у Yangi.TV: живая выдача по мере ввода, результаты
 * списком строк, а при слишком коротком запросе под полем красная подсказка.
 */
export default function SearchScreen() {
  const { t, i18n } = useTranslation();
  const insets = useSafeAreaInsets();

  const [query, setQuery] = useState('');
  const creators = useCreators();
  const isRu = i18n.language === 'ru';

  const trimmed = query.trim();
  const tooShort = trimmed.length > 0 && trimmed.length < MIN_QUERY_LENGTH;

  const results = useMemo(
    () => searchCreators(creators.data ?? [], trimmed),
    [creators.data, trimmed]
  );

  return (
    <View className="flex-1 bg-ink" style={{ paddingTop: insets.top }}>
      <View className="flex-row items-center gap-2 px-4 pb-2 pt-2">
        <Pressable
          onPress={() => router.back()}
          accessibilityRole="button"
          accessibilityLabel="Orqaga"
          hitSlop={12}
          className="-ml-2 h-11 w-9 justify-center active:opacity-60"
        >
          <Ionicons name="chevron-back" size={26} color={colors.white} />
        </Pressable>

        <View className="flex-1 flex-row items-center gap-2 rounded-card bg-surface px-3">
          <Ionicons name="search-outline" size={18} color={colors.textMuted} />
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder={t('common.search')}
            placeholderTextColor={colors.textDisabled}
            autoFocus
            returnKeyType="search"
            className="flex-1 py-3 text-body"
            style={{ color: colors.white }}
          />
          {query.length > 0 ? (
            <Pressable onPress={() => setQuery('')} hitSlop={10}>
              <Ionicons name="close-circle" size={18} color={colors.textMuted} />
            </Pressable>
          ) : null}
        </View>
      </View>

      {/* Ровно как у Yangi.TV: подсказка красным прямо под полем */}
      {tooShort ? (
        <Text className="px-4 pb-2 text-caption text-danger">
          {t('search.minLength', { count: MIN_QUERY_LENGTH })}
        </Text>
      ) : null}

      <Body
        state={
          creators.isPending
            ? 'loading'
            : creators.isError
              ? 'error'
              : trimmed.length < MIN_QUERY_LENGTH
                ? 'idle'
                : results.length === 0
                  ? 'nothing'
                  : 'results'
        }
        onRetry={() => creators.refetch()}
      >
        <FlatList
          data={results}
          keyExtractor={(item) => String(item.id)}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={{
            paddingHorizontal: 16,
            paddingBottom: insets.bottom + 24,
            gap: 8,
          }}
          showsVerticalScrollIndicator={false}
          renderItem={({ item }) => (
            <SearchRow
              title={item.name}
              subtitle={labelForType(item.castingType, isRu)}
              meta={[
                item.age ? t('common.years', { count: item.age }) : null,
                item.height ? `${item.height} ${t('creator.cm')}` : null,
                item.region,
              ]
                .filter(Boolean)
                .join(' • ')}
              imageUrl={item.photoUrls[0]}
              onPress={() => router.push(`/creator/${item.id}`)}
            />
          )}
        />
      </Body>
    </View>
  );
}

/** Разводит пять состояний экрана, чтобы разметка выдачи не тонула в тернарниках. */
function Body({
  state,
  onRetry,
  children,
}: {
  state: 'loading' | 'error' | 'idle' | 'nothing' | 'results';
  onRetry: () => void;
  children: React.ReactNode;
}) {
  const { t } = useTranslation();

  if (state === 'loading') return <ScreenState kind="loading" />;
  if (state === 'error') return <ScreenState kind="error" onRetry={onRetry} />;

  if (state === 'idle') {
    return (
      <ScreenState
        kind="empty"
        title={t('search.hintTitle')}
        body={t('search.hintBody')}
      />
    );
  }

  if (state === 'nothing') {
    return (
      <ScreenState
        kind="empty"
        title={t('search.nothingTitle')}
        body={t('search.nothingBody')}
      />
    );
  }

  return <>{children}</>;
}

function labelForType(type: string | null, isRu: boolean): string | null {
  if (!type) return null;

  const category = CATEGORIES.find((c) => c.apiType === type);
  if (category) return isRu ? category.titleRu : category.titleUz;

  const extra = EXTRA_API_TYPES[type];
  return extra ? (isRu ? extra.ru : extra.uz) : type;
}
