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
import { groupDigits } from '@/lib/money';
import { colors } from '@/theme/tokens';

import { DonateSheet } from './DonateSheet';
import { useContentDonors, type CastMember, type ContentDetail } from './detail';

/**
 * Нижние блоки страницы контента: актёры, трейлер, кадры и донаты.
 *
 * Вынесены из `ContentScreen` не ради размера файла: каждый из них
 * появляется ТОЛЬКО при своих данных и молча исчезает без них. Держать
 * четыре таких правила в одном экране — значит потерять их из виду.
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

/** Знаки первых трёх мест. Дальше — просто номер. */
const MEDALS: Record<number, string> = {
  1: colors.gold,
  2: '#C0C6D8',
  3: '#CD7F32',
};

/**
 * «Top 10 Donatchilar» — кто больше всех поддержал контент.
 *
 * <h2>⚠️ Блок исчезает, пока донатов нет</h2>
 * Пустая таблица с заголовком «топ 10» читается как поломка, а не как
 * «ещё никто не donat qilmagan». Кнопка «Donat qilish» при этом остаётся:
 * поддержать можно и первым.
 *
 * <h2>Сумма контента и сумма списка — разные числа</h2>
 * В строках только верхушка рейтинга, а `starsReceived` — весь контент.
 * Складывать их нельзя, поэтому общая сумма стоит отдельной строкой над
 * списком.
 */
export function DonorsBoard({
  contentId,
  title,
}: {
  contentId: number | null;
  title: string | null;
}) {
  const { t } = useTranslation();
  const donors = useContentDonors(contentId);
  const [sheet, setSheet] = useState(false);

  const list = donors.data?.donors ?? [];
  const total = donors.data?.starsReceived ?? 0;

  /**
   * ⚠️ Рейтинг РИСУЕТСЯ, только если сервер ответил.
   *
   * На старой сборке бэкенда этого адреса нет, и запрос падает. Показать
   * тогда «ещё никто не поддержал» значило бы соврать: платформа могла
   * собрать миллион звёзд, а человек прочитал бы, что контент никому не
   * нужен. Кнопка при этом остаётся — поддержать можно и молча.
   */
  const answered = donors.data !== undefined;

  return (
    <View className="gap-3">
      <SectionHead title={t('content.topDonors')} />

      {answered ? (
      <View className="gap-3 rounded-card bg-surface p-3">
        {total > 0 ? (
          <View className="flex-row items-center gap-2">
            <Ionicons name="star" size={14} color={colors.gold} />
            <Text className="text-caption text-text-muted">
              {t('content.starsTotal', { amount: groupDigits(total) })}
            </Text>
          </View>
        ) : null}

        {list.length === 0 ? (
          <Text className="text-caption text-text-muted">{t('content.noDonors')}</Text>
        ) : (
          list.map((d) => (
            <View key={d.rank} className="flex-row items-center gap-3">
              <View className="w-6 items-center">
                {MEDALS[d.rank] ? (
                  <Ionicons name="trophy" size={14} color={MEDALS[d.rank]} />
                ) : (
                  <Text className="text-caption text-text-muted">{d.rank}</Text>
                )}
              </View>

              <View className="h-8 w-8 overflow-hidden rounded-pill bg-surface-2">
                {d.avatarUrl ? (
                  <Image
                    source={{ uri: d.avatarUrl }}
                    style={{ width: '100%', height: '100%' }}
                    contentFit="cover"
                  />
                ) : null}
              </View>

              <Text numberOfLines={1} className="flex-1 text-caption text-text">
                {/* Удалённый или безымянный аккаунт остаётся в рейтинге:
                    звёзды он действительно отправил, и выбросить строку
                    значило бы спрятать часть суммы. */}
                {d.name ?? t('content.anonymousDonor')}
              </Text>

              <Text className="text-caption font-semibold text-text">
                {groupDigits(d.stars)}
              </Text>
              <Ionicons name="star" size={13} color={colors.gold} />
            </View>
          ))
        )}
      </View>
      ) : null}

      <Pressable
        onPress={() => setSheet(true)}
        accessibilityRole="button"
        className="flex-row items-center justify-center gap-2 rounded-card bg-purple px-5 py-3.5 active:opacity-80"
      >
        <Ionicons name="heart" size={18} color={colors.white} />
        <Text className="text-body font-semibold text-white">{t('content.donate')}</Text>
      </Pressable>

      <DonateSheet
        open={sheet}
        contentId={contentId}
        title={title}
        onClose={() => setSheet(false)}
      />
    </View>
  );
}
