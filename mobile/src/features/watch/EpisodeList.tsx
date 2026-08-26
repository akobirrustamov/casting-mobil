import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Badge, type BadgeTone } from '@/components/ui/Badge';
import { Screen } from '@/components/ui/Screen';
import { isVertical } from '@/features/content/orientation';
import { useContentCard } from '@/features/home/api';
import { mediaUrl } from '@/lib/api';
import { useIsOffline } from '@/lib/network';

import { ContentNotFoundError, WatchUnavailableError } from './api';
import { episodesOfSeason, type EpisodeCard, type useEpisodes } from './episodes';

/**
 * Серии контента — сериал, мини-сериал, подкаст.
 *
 * <h2>Зачем отдельный экран</h2>
 * `/watch/content/{id}` открывает только цельный контент; у многосерийного он
 * отвечает «спрашивай серию». До появления `/content/{id}/episodes` спросить
 * было нечего, и половина каталога в приложении была тупиком.
 *
 * <h2>Замок на серии считает сервер</h2>
 * `allowed` и `requiredAction` приходят из того же `AccessService`, что и на
 * экране просмотра (ТЗ §37). Клиент не решает по `accessPolicy` сам: политика
 * не знает ни о подписке, ни о купленной серии — купленная серия выглядела бы
 * закрытой, и человек заплатил бы второй раз.
 */
type EpisodesQuery = ReturnType<typeof useEpisodes>;

export function EpisodeListScreen({
  contentId,
  query,
}: {
  contentId: number | null;
  query: EpisodesQuery;
}) {
  const { t } = useTranslation();
  const isOffline = useIsOffline();
  const card = useContentCard(contentId);

  const [season, setSeason] = useState<number | null>(null);

  const title = card?.title ?? t('content.episodes');

  if (query.isPending) {
    return (
      <Screen scroll={false} title={title} underTabBar={false} onBack={() => router.back()}>
        <ScreenState kind="loading" />
      </Screen>
    );
  }

  if (query.isError) {
    const unavailable =
      query.error instanceof WatchUnavailableError ||
      query.error instanceof ContentNotFoundError;

    return (
      <Screen scroll={false} title={title} underTabBar={false} onBack={() => router.back()}>
        {unavailable ? (
          <ScreenState
            kind="empty"
            title={t('content.multiPartTitle')}
            body={t('content.multiPartBody')}
          />
        ) : (
          <ScreenState
            kind={isOffline ? 'offline' : 'error'}
            onRetry={() => query.refetch()}
          />
        )}
      </Screen>
    );
  }

  const list = query.data;
  const seasons = list.seasons;
  // Пока сезон не выбран — показываем первый, а не пустоту.
  const activeSeason = season ?? (seasons.length > 0 ? seasons[0].id : null);
  const visible = episodesOfSeason(list, seasons.length > 0 ? activeSeason : null);

  // Формат общий у всего контента — кадры серий рилс-сериала вертикальные.
  const vertical = isVertical(list.orientation);

  return (
    <Screen
      title={title}
      subtitle={t('content.episodes')}
      onBack={() => router.back()}
      underTabBar={false}
      onRefresh={() => query.refetch()}
      refreshing={query.isRefetching}
    >
      {seasons.length > 1 ? (
        <View className="flex-row flex-wrap gap-2">
          {seasons.map((s) => (
            <Pressable
              key={s.id}
              onPress={() => setSeason(s.id)}
              accessibilityRole="button"
              accessibilityState={{ selected: s.id === activeSeason }}
              className={`rounded-pill px-4 py-2 ${
                s.id === activeSeason ? 'bg-purple' : 'bg-surface'
              }`}
            >
              <Text
                className={`text-caption ${
                  s.id === activeSeason ? 'font-semibold text-white' : 'text-text-muted'
                }`}
              >
                {s.title ?? t('content.season', { number: s.seasonNumber ?? '' })}
              </Text>
            </Pressable>
          ))}
        </View>
      ) : null}

      {visible.length === 0 ? (
        <View className="h-64">
          <ScreenState kind="empty" body={t('content.episodesEmpty')} />
        </View>
      ) : (
        <View className="gap-3">
          {visible.map((episode) => (
            <EpisodeRow key={episode.id} episode={episode} vertical={vertical} />
          ))}
        </View>
      )}
    </Screen>
  );
}

/** Бейдж на строке серии — по решению сервера, а не по политике доступа. */
function stateBadge(episode: EpisodeCard): { tone: BadgeTone; key: string } | null {
  if (episode.allowed) {
    return episode.reason === 'FREE'
      ? { tone: 'purchased', key: 'common.free' }
      : { tone: 'purchased', key: 'common.purchased' };
  }
  return { tone: 'locked', key: 'common.locked' };
}

/** Разряды пробелами: 5000 → «5 000». */
function groupDigits(amount: number): string {
  return String(Math.round(amount)).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

function EpisodeRow({
  episode,
  vertical,
}: {
  episode: EpisodeCard;
  vertical: boolean;
}) {
  const { t } = useTranslation();
  const badge = stateBadge(episode);

  const minutes =
    episode.durationSeconds !== null && episode.durationSeconds > 0
      ? Math.max(1, Math.round(episode.durationSeconds / 60))
      : null;

  const thumbnail = mediaUrl(episode.thumbnailMediaId);

  return (
    <Pressable
      // Закрытая серия тоже открывается: цена и кнопка живут на экране
      // просмотра, и там же сервер ещё раз подтверждает решение.
      onPress={() => router.push(`/episode/${episode.id}`)}
      accessibilityRole="button"
      className="flex-row items-center gap-3 rounded-card bg-surface p-3 active:opacity-70"
    >
      {/* Кадр серии в пропорции формата: обрезанный до 3:2 вертикальный
          кадр показывал бы середину головы вместо кадра. */}
      <View
        className={`overflow-hidden rounded-md bg-surface-2 ${
          vertical ? 'h-24 w-[54px]' : 'h-16 w-24'
        }`}
      >
        {thumbnail ? (
          <Image
            source={{ uri: thumbnail }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
          />
        ) : null}
      </View>

      <View className="flex-1 gap-1">
        <Text numberOfLines={2} className="text-body text-text">
          {episode.episodeNumber !== null
            ? `${t('content.part', { number: episode.episodeNumber })}${
                episode.title ? ` · ${episode.title}` : ''
              }`
            : (episode.title ?? '')}
        </Text>

        <View className="flex-row items-center gap-2">
          {badge ? <Badge tone={badge.tone}>{t(badge.key)}</Badge> : null}
          {minutes !== null ? (
            <Text className="text-micro text-text-muted">
              {t('content.minutes', { count: minutes })}
            </Text>
          ) : null}
          {!episode.allowed && episode.episodePrice !== null ? (
            <Text className="text-micro text-text-muted">
              {t('common.price', { amount: groupDigits(episode.episodePrice) })}
            </Text>
          ) : null}
        </View>
      </View>
    </Pressable>
  );
}
