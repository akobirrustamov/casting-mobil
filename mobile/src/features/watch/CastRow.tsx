import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, ScrollView, Text, View } from 'react-native';

import { mediaUrl } from '@/lib/api';

import type { Credit } from './types';

/**
 * «Aktyorlar» — ряд круглых портретов под описанием (референс, 09.09.2026).
 *
 * <h2>⚠️ Эти данные лежали в базе с самого начала</h2>
 * Состав заполняется в админке (`cms_content_credit`, ТЗ §24), но в
 * приложение НИКОГДА не попадал: ни один app-эндпоинт его не отдавал.
 * То есть админ вводил актёров, а увидеть их было негде.
 *
 * <h2>Почему подпись двойная</h2>
 * Сверху человек, снизу роль — как на референсе. Роль важнее, чем
 * кажется: по имени актёра зритель часто не узнаёт, а по имени героя —
 * сразу. Роли может не быть (режиссёр, оператор) — тогда снизу
 * профессия, и строка не пустует.
 */
const AVATAR = 72;

export function CastRow({ credits }: { credits: Credit[] }) {
  const { t } = useTranslation();

  if (credits.length === 0) return null;

  return (
    <View className="gap-3">
      <View className="flex-row items-center justify-between">
        <Text className="text-h2 text-text">{t('content.cast')}</Text>
        {/*
          ⚠️ «Barchasi ›» на референсе есть, но экрана со всем составом
          нет — поэтому и ссылки нет. Кнопка, ведущая в никуда, хуже её
          отсутствия: человек нажимает и решает, что приложение сломано.
        */}
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        // Отрицательный отступ гасит поля экрана: ряд должен уезжать
        // под край, а не обрываться в 16 пунктах от него.
        className="-mx-4"
        contentContainerStyle={{ paddingHorizontal: 16, gap: 12 }}
      >
        {credits.map((credit, index) => (
          <Person key={`${credit.creatorId ?? 'x'}-${index}`} credit={credit} />
        ))}
      </ScrollView>
    </View>
  );
}

function Person({ credit }: { credit: Credit }) {
  const { t } = useTranslation();

  const photo = mediaUrl(credit.photoMediaId);
  const subtitle =
    credit.characterName ??
    (credit.profession
      ? t(`profession.${credit.profession}`, { defaultValue: credit.profession })
      : null);

  const open = credit.creatorId === null
    ? undefined
    : () => router.push(`/creator/${credit.creatorId}`);

  return (
    <Pressable
      onPress={open}
      disabled={open === undefined}
      accessibilityRole={open ? 'button' : undefined}
      className="items-center gap-1.5 active:opacity-70"
      style={{ width: AVATAR + 12 }}
    >
      <View
        className="overflow-hidden bg-surface"
        style={{ width: AVATAR, height: AVATAR, borderRadius: AVATAR / 2 }}
      >
        {photo ? (
          <Image
            source={{ uri: photo }}
            style={{ width: AVATAR, height: AVATAR }}
            contentFit="cover"
            transition={150}
          />
        ) : null}
      </View>

      <Text className="text-center text-caption text-text" numberOfLines={1}>
        {credit.name ?? '—'}
      </Text>
      {subtitle ? (
        <Text className="text-center text-caption text-text-muted" numberOfLines={1}>
          {subtitle}
        </Text>
      ) : null}
    </Pressable>
  );
}
