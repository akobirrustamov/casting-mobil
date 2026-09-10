import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, ScrollView, Text, View } from 'react-native';

import { StoryCircle } from '@/components/ui/StoryCircle';
import { Player } from '@/features/watch/Player';
import type { VideoSource, WatchInfo } from '@/features/watch/types';
import { mediaUrl } from '@/lib/api';
import { colors } from '@/theme/tokens';

import type { CastMember, ContentDetail } from './detail';

/**
 * Нижние блоки страницы контента: актёры, трейлер и кадры.
 *
 * Вынесены из `ContentScreen` не ради размера файла: каждый из них
 * появляется ТОЛЬКО при своих данных и молча исчезает без них. Держать
 * три таких правила в одном экране — значит потерять их из виду.
 *
 * ⚠️ Рейтинг донатов жил здесь же и 10.09.2026 УЕХАЛ в отдельное окно —
 * см. `DonorsSheet`, там сказано почему.
 */

/** Заголовок блока с необязательной ссылкой «Barchasi ›». */
function SectionHead({ title, onSeeAll }: { title: string; onSeeAll?: () => void }) {
  const { t } = useTranslation();

  return (
    <View className="flex-row items-center justify-between">
      <Text numberOfLines={1} className="flex-1 text-h2 text-text">
        {title}
      </Text>
      {onSeeAll ? (
        <Pressable onPress={onSeeAll} hitSlop={12}>
          <Text className="text-caption text-cyan">{t('common.seeAll')} ›</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

/**
 * «Aktyorlar» — круглые портреты с именем и ролью.
 *
 * <h2>⚠️ Портрет НЕ открывается</h2>
 * Соблазн повесить переход на `/creator/{id}` велик, но это разные
 * сущности: экран креатора показывает анкету КАСТИНГА (старый бэкенд,
 * свои идентификаторы), а здесь — актёр из каталога контента. По
 * совпавшему числу открылась бы чужая анкета, и заметить подмену было бы
 * невозможно.
 */
export function CastRail({ cast }: { cast: CastMember[] }) {
  const { t } = useTranslation();

  if (cast.length === 0) return null;

  return (
    <View className="gap-3">
      <SectionHead title={t('content.cast')} />

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={{ gap: 12, paddingRight: 16 }}
      >
        {cast.map((member, i) => (
          <StoryCircle
            key={member.creatorId ?? `cast-${i}`}
            name={member.name ?? ''}
            // Имя персонажа важнее профессии: на макете под актёром стоит
            // «Sevinch», а не «актёр». Профессия — запасная подпись для
            // режиссёра и оператора, у которых персонажа нет.
            role={
              member.characterName ??
              (member.profession
                ? t(`profession.${member.profession}`, { defaultValue: member.profession })
                : undefined)
            }
            imageUrl={mediaUrl(member.photoMediaId)}
            size={64}
          />
        ))}
      </ScrollView>
    </View>
  );
}

/**
 * Трейлер.
 *
 * <h2>Откуда берётся ролик</h2>
 * Сначала из `/watch` — там он приходит подписанным и с HLS, то есть
 * играет через CDN. Если `/watch` про этот контент ничего не сказал (у
 * многосерийного он отвечает «спрашивай серию»), берём медиа-файл из
 * карточки: он отдаётся напрямую и без транскодирования, но это лучше,
 * чем не показать трейлер сериалу вовсе.
 *
 * <h2>⚠️ Ролик подписан — и это не украшение</h2>
 * Без подписи полторы минуты в рамке плеера читаются как «фильм уже
 * открыт»: человек досматривает ролик, видит конец и уходит, решив, что
 * купил пустоту.
 */
export function TrailerCard({
  detail,
  info,
}: {
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
}) {
  const { t } = useTranslation();
  const [playing, setPlaying] = useState(false);
  const [failed, setFailed] = useState(false);

  const source: VideoSource | null =
    info?.trailer ??
    (detail?.trailerMediaId != null
      ? {
          partNumber: null,
          mediaId: detail.trailerMediaId,
          // Относительный путь: базу подставляет плеер. Абсолютный дал бы
          // «https://uzcasting.sitehttps://…» — тихую поломку.
          url: `/api/v1/app/media/${detail.trailerMediaId}/raw`,
          hlsUrl: null,
          durationSeconds: null,
        }
      : null);

  if (source === null) return null;

  const poster = mediaUrl(detail?.coverMediaId ?? detail?.posterMediaId);

  return (
    <View className="gap-3">
      <SectionHead title={t('content.trailer')} />

      {playing && !failed ? (
        <View className="gap-2">
          {/*
            ⚠️ `contentId` и `episodeId` — `null` НАМЕРЕННО.

            Плеер по ним пишет позицию просмотра и считает запуск
            контента. Ни то, ни другое к ролику не относится: позиция
            трейлера затёрла бы место, на котором человек бросил сам
            фильм, а счётчик просмотров раздулся бы теми, кто ничего не
            купил.
          */}
          <Player
            key={`trailer-${source.mediaId}-${source.hlsUrl ? 'hls' : 'raw'}`}
            source={source}
            orientation={detail?.orientation ?? info?.orientation ?? null}
            contentId={null}
            episodeId={null}
            // Сбой трейлера — не ошибка экрана: ролик необязателен, и
            // кнопка «попробовать ещё раз» здесь была бы враньём.
            // Человеку нужен фильм, а не трейлер.
            onError={() => setFailed(true)}
          />
        </View>
      ) : (
        <Pressable
          onPress={() => {
            setFailed(false);
            setPlaying(true);
          }}
          accessibilityRole="button"
          accessibilityLabel={t('content.trailer')}
          className="overflow-hidden rounded-card bg-surface-2 active:opacity-80"
          style={{ aspectRatio: 16 / 9 }}
        >
          {poster ? (
            <Image
              source={{ uri: poster }}
              style={{ width: '100%', height: '100%' }}
              contentFit="cover"
              transition={200}
            />
          ) : null}

          <View className="absolute inset-0 items-center justify-center">
            <View className="h-14 w-14 items-center justify-center rounded-pill bg-black/55">
              <Ionicons name="play" size={24} color={colors.white} />
            </View>
          </View>

          <View className="absolute bottom-3 left-3">
            <Text numberOfLines={1} className="text-h2 text-white">
              {detail?.title ?? ''}
            </Text>
          </View>
        </Pressable>
      )}
    </View>
  );
}

/**
 * «Qiziq sahnalar» — кадры из галереи контента.
 *
 * ⚠️ Это КАРТИНКИ, а не фрагменты видео. На макете под каждым кадром стоит
 * тайм-код («02:10»), но нарезки сцен в базе нет — есть галерея
 * изображений. Придуманный тайм-код обещал бы переход в место фильма,
 * которого никто не размечал.
 */
export function ScenesRail({ mediaIds }: { mediaIds: number[] }) {
  const { t } = useTranslation();

  if (mediaIds.length === 0) return null;

  return (
    <View className="gap-3">
      <SectionHead title={t('content.scenes')} />

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={{ gap: 8, paddingRight: 16 }}
      >
        {mediaIds.map((id) => (
          <View
            key={id}
            className="overflow-hidden rounded-md bg-surface-2"
            style={{ width: 128, height: 76 }}
          >
            <Image
              source={{ uri: mediaUrl(id) }}
              style={{ width: '100%', height: '100%' }}
              contentFit="cover"
              transition={200}
            />
          </View>
        ))}
      </ScrollView>
    </View>
  );
}
