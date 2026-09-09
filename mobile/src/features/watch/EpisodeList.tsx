import { Ionicons } from '@expo/vector-icons';
import { useQuery } from '@tanstack/react-query';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, ScrollView, Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { isVertical } from '@/features/content/orientation';
import { feedLocale, useContentCard, useFeedLanguage } from '@/features/home/api';
import { DEFAULT_LANGUAGE } from '@/i18n';
import { mediaUrl } from '@/lib/api';
import { formatSum, groupDigits } from '@/lib/money';
import { useIsOffline } from '@/lib/network';
import { colors, radius } from '@/theme/tokens';

import { ContentNotFoundError, WatchUnavailableError, useViewerKey } from './api';
import { episodesOfSeason, type EpisodeCard, type useEpisodes } from './episodes';
import { fetchContinueWatching } from './progressApi';

/**
 * Серии контента — макет заказчика «3» (08.09.2026): сезоны вкладками,
 * серии крупными карточками с кадром.
 *
 * <h2>Как сюда попадают</h2>
 * С карточки контента, по кнопке «Tomosha qilish». Раньше многосерийный
 * контент проваливался сюда сразу, минуя карточку, — и описание, актёры и
 * донаты сериалу были недоступны вовсе.
 *
 * <h2>Замок на серии считает сервер</h2>
 * `allowed` и `requiredAction` приходят из того же `AccessService`, что и
 * на экране просмотра (ТЗ §37). Клиент не решает по `accessPolicy` сам:
 * политика не знает ни о подписке, ни о купленной серии — купленная серия
 * выглядела бы закрытой, и человек заплатил бы второй раз.
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
  const current = useCurrentEpisodeNumber(contentId);

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
      onBack={() => router.back()}
      underTabBar={false}
      onRefresh={() => query.refetch()}
      refreshing={query.isRefetching}
    >
      {seasons.length > 1 ? (
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={{ gap: 8, paddingRight: 16 }}
        >
          {seasons.map((s) => (
            <SeasonTab
              key={s.id}
              label={s.title ?? t('content.season', { number: s.seasonNumber ?? '' })}
              selected={s.id === activeSeason}
              onPress={() => setSeason(s.id)}
            />
          ))}
        </ScrollView>
      ) : null}

      {visible.length === 0 ? (
        <View className="h-64">
          <ScreenState kind="empty" body={t('content.episodesEmpty')} />
        </View>
      ) : (
        <View className="gap-3">
          {visible.map((episode) => (
            <EpisodeRow
              key={episode.id}
              episode={episode}
              vertical={vertical}
              // Подсвечивается та серия, на которой человек остановился.
              current={current !== null && episode.episodeNumber === current}
            />
          ))}
        </View>
      )}
    </Screen>
  );
}

/**
 * Вкладка сезона.
 *
 * Выбранная — градиентом, как на макете. Ровно один акцент на экране: у
 * невыбранных заливка обычная, иначе три одинаково ярких вкладки не
 * сказали бы, какая открыта.
 */
function SeasonTab({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  if (!selected) {
    return (
      <Pressable
        onPress={onPress}
        accessibilityRole="button"
        accessibilityState={{ selected: false }}
        className="rounded-pill bg-surface px-5 py-2.5 active:opacity-70"
      >
        <Text className="text-caption text-text-muted">{label}</Text>
      </Pressable>
    );
  }

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected: true }}
      style={{ borderRadius: radius.pill, overflow: 'hidden' }}
    >
      <LinearGradient
        colors={[colors.magenta, colors.purple]}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={{ paddingHorizontal: 20, paddingVertical: 10 }}
      >
        <Text className="text-caption font-semibold text-white">{label}</Text>
      </LinearGradient>
    </Pressable>
  );
}

/**
 * Строка серии: кадр со знаком воспроизведения, номер, длительность и
 * короткое описание.
 *
 * <h2>⚠️ Чего здесь НЕТ и почему</h2>
 * На макете справа стоит стрелка «скачать». Офлайн-загрузок в платформе
 * нет — ни в приложении, ни на сервере. Кнопка, которая ничего не
 * скачивает, хуже её отсутствия, поэтому на её месте стоит замок у
 * закрытых серий: это то, что человеку в этой строке действительно нужно
 * знать.
 */
function EpisodeRow({
  episode,
  vertical,
  current,
}: {
  episode: EpisodeCard;
  vertical: boolean;
  current: boolean;
}) {
  const { t } = useTranslation();

  const thumbnail = mediaUrl(episode.thumbnailMediaId);
  const duration = clock(episode.durationSeconds);

  return (
    <Pressable
      // Закрытая серия тоже открывается: цена и кнопка живут на экране
      // просмотра, и там же сервер ещё раз подтверждает решение.
      onPress={() => router.push(`/episode/${episode.id}`)}
      accessibilityRole="button"
      accessibilityState={{ selected: current }}
      style={{
        borderRadius: radius.card,
        borderWidth: 1,
        // Кромка — единственное, что отличает «эту серию я смотрю» от
        // остальных. Прозрачная у прочих, чтобы карточки не прыгали на
        // пиксель, когда подсветка переезжает.
        borderColor: current ? colors.purple : 'transparent',
      }}
      className="flex-row items-center gap-3 bg-surface p-3 active:opacity-70"
    >
      {/* Кадр в пропорции формата: обрезанный до 3:2 вертикальный кадр
          показывал бы середину головы вместо кадра. */}
      <View
        className={`overflow-hidden rounded-md bg-surface-2 ${
          vertical ? 'h-24 w-[54px]' : 'h-[60px] w-24'
        }`}
      >
        {thumbnail ? (
          <Image
            source={{ uri: thumbnail }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            transition={200}
          />
        ) : null}

        <View className="absolute inset-0 items-center justify-center">
          <View className="h-8 w-8 items-center justify-center rounded-pill bg-black/55">
            <Ionicons name="play" size={15} color={colors.white} />
          </View>
        </View>
      </View>

      <View className="flex-1 gap-0.5">
        <Text numberOfLines={1} className="text-body font-semibold text-text">
          {episode.episodeNumber !== null
            ? t('content.part', { number: episode.episodeNumber })
            : (episode.title ?? '')}
        </Text>

        <View className="flex-row items-center gap-2">
          {duration ? (
            <Text className="text-micro text-text-muted">{duration}</Text>
          ) : null}

          {/* Просмотры ЭТОЙ серии, а не всего сериала.

              ⚠️ Ноль не рисуется — как и на карточках ленты. В списке из
              двадцати серий столбик нулей читался бы как «сериал никто не
              смотрит», хотя серия просто вышла вчера. */}
          {episode.viewCount ? (
            <View className="flex-row items-center gap-1">
              <Ionicons name="eye-outline" size={11} color={colors.textMuted} />
              <Text className="text-micro text-text-muted">
                {groupDigits(episode.viewCount)}
              </Text>
            </View>
          ) : null}

          {!episode.allowed && episode.episodePrice !== null ? (
            <Text className="text-micro text-gold">
              {t('common.price', { amount: formatSum(episode.episodePrice) })}
            </Text>
          ) : null}
        </View>

        {episode.shortDescription ?? episode.title ? (
          <Text numberOfLines={2} className="text-micro text-text-muted">
            {episode.shortDescription ?? episode.title}
          </Text>
        ) : null}
      </View>

      {!episode.allowed ? (
        <Ionicons name="lock-closed" size={16} color={colors.gold} />
      ) : current ? (
        <Ionicons name="play-circle" size={20} color={colors.magenta} />
      ) : null}
    </Pressable>
  );
}

/**
 * Секунды в «45:12».
 *
 * ⚠️ Не «45 daqiqa»: на макете под номером серии стоит именно
 * часы-минуты-секунды, и для серии это точнее — «46 daqiqa» у трёх серий
 * подряд выглядит как одно и то же число.
 */
function clock(seconds: number | null): string | null {
  if (seconds === null || seconds <= 0) return null;

  const total = Math.round(seconds);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;

  const pad = (v: number) => String(v).padStart(2, '0');
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

/**
 * Номер серии, на которой человек остановился.
 *
 * <h2>Почему из «продолжить смотреть», а не по каждой серии</h2>
 * Позиция лежит отдельно для КАЖДОЙ серии, и спросить их все — это
 * двадцать запросов на открытие списка. Список «продолжить» отвечает на
 * тот же вопрос одним запросом и уже прогрет главной: ключ запроса здесь
 * тот же, что у `ContinueRail`.
 *
 * ⚠️ Гостю не запрашиваем: позиция привязана к человеку, и ответом был бы
 * отказ на каждом открытии списка.
 */
function useCurrentEpisodeNumber(contentId: number | null): number | null {
  const language = useFeedLanguage();
  const viewer = useViewerKey();

  const query = useQuery({
    queryKey: ['watch-progress', 'continue', language, viewer],
    queryFn: () => fetchContinueWatching(feedLocale(language ?? DEFAULT_LANGUAGE)),
    enabled: viewer !== 'guest',
    staleTime: 0,
    // Подсветка необязательна: её отсутствие ничего не ломает, и
    // повторять запрос ради неё незачем.
    retry: 1,
  });

  if (contentId === null) return null;

  return (
    query.data?.find((item) => item.content.id === contentId)?.episodeNumber ?? null
  );
}
