import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View, useWindowDimensions } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Badge, type BadgeTone } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { trackContentView } from '@/features/analytics/api';
import { frameRatio, isVertical } from '@/features/content/orientation';
import { contentCards, useHomeFeed } from '@/features/home/api';
import type { ContentCard } from '@/features/home/types';
import { mediaUrl } from '@/lib/api';
import { useIsOffline } from '@/lib/network';

import {
  ContentNotFoundError,
  WatchUnavailableError,
  type useWatchContent,
} from './api';
import { Player } from './Player';
import type { RequiredAction, WatchInfo } from './types';

/**
 * Экран просмотра — общий для цельного контента (17) и отдельной серии.
 *
 * <h2>Кто решает, можно ли смотреть</h2>
 * Только сервер. `/api/v1/app/watch/**` возвращает `allowed`, причину,
 * требуемое действие и цену — по ТЗ §37 правила премиума лежат в одном месте
 * (`AccessService`). Экран ничего не досчитывает: складывай он подписку с
 * покупкой сам, правило жило бы в двух местах и разъехалось бы при первом
 * изменении тарифов.
 *
 * <h2>Почему афиша и описание берутся из фида</h2>
 * `/watch` отвечает про ПРОСМОТР, а не отдаёт карточку контента: там только
 * название, длительность и право доступа. Эндпоинта «дай карточку по id» в
 * `/api/v1/app/**` пока нет (docs/API.md §5), поэтому афиша и описание —
 * из кэша главной, и их отсутствие не мешает экрану работать.
 */
type WatchQuery = ReturnType<typeof useWatchContent>;

export function WatchDetail({ query }: { query: WatchQuery }) {
  const isOffline = useIsOffline();

  if (query.isPending) {
    return (
      <Plain>
        <ScreenState kind="loading" />
      </Plain>
    );
  }

  if (query.isError) {
    return (
      <Plain>
        <WatchError
          error={query.error}
          isOffline={isOffline}
          onRetry={() => query.refetch()}
        />
      </Plain>
    );
  }

  return <Loaded info={query.data} query={query} />;
}

function Loaded({ info, query }: { info: WatchInfo; query: WatchQuery }) {
  // Для серии карточки в фиде нет — берём родительский контент, он и даёт афишу.
  const card = useFeedCard(info.contentId);

  // Открытие карточки — отдельное событие от запуска видео: человек может
  // зайти, увидеть цену и уйти, и в отчётах это разные вещи.
  const contentId = info.contentId;
  const episodeId = info.episodeId;
  useEffect(() => {
    if (contentId !== null) trackContentView(contentId, episodeId);
  }, [contentId, episodeId]);

  return (
    <Screen
      title={info.title ?? card?.title ?? ''}
      onBack={() => router.back()}
      underTabBar={false}
      onRefresh={() => query.refetch()}
      refreshing={query.isRefetching}
    >
      <Stage info={info} card={card} />
      <Facts info={info} card={card} />

      {card?.shortDescription ? (
        <Text className="text-body text-text-muted">{card.shortDescription}</Text>
      ) : null}

      {info.allowed ? null : <LockedPanel info={info} />}
    </Screen>
  );
}

/** Каркас для состояний, когда заголовка ещё нет. */
function Plain({ children }: { children: ReactNode }) {
  return (
    <Screen scroll={false} title=" " underTabBar={false} onBack={() => router.back()}>
      {children}
    </Screen>
  );
}

/**
 * Карточка из кэша главной — только афиша и описание.
 *
 * Обогащение, а не источник правды: при переходе по прямой ссылке кэша может
 * не быть, и экран обязан работать без него.
 */
function useFeedCard(contentId: number | null): ContentCard | undefined {
  const feed = useHomeFeed();
  return useMemo(
    () =>
      contentId === null
        ? undefined
        : contentCards(feed.data).find((c) => c.id === contentId),
    [feed.data, contentId]
  );
}

function WatchError({
  error,
  isOffline,
  onRetry,
}: {
  error: unknown;
  isOffline: boolean;
  onRetry: () => void;
}) {
  const { t } = useTranslation();

  if (error instanceof ContentNotFoundError) {
    return <ScreenState kind="empty" body={t('content.notFound')} />;
  }

  if (error instanceof WatchUnavailableError) {
    return (
      <ScreenState
        kind="error"
        title={t('home.feedUnavailableTitle')}
        body={t('home.feedUnavailableBody')}
        onRetry={onRetry}
      />
    );
  }

  return <ScreenState kind={isOffline ? 'offline' : 'error'} onRetry={onRetry} />;
}

/**
 * Верх экрана: плеер, если смотреть можно, иначе афиша под замком.
 *
 * Афиша при отказе — это не «почти доступ»: сам файл сервер не отдаст, а
 * обложку платного фильма видно и на главной. Форма афиши повторяет формат
 * контента: у рилса она вертикальная, иначе под замком человек увидел бы
 * широкий кадр, а после покупки — узкий.
 */
function Stage({ info, card }: { info: WatchInfo; card: ContentCard | undefined }) {
  const { t } = useTranslation();
  const [part, setPart] = useState(0);

  const source = info.sources[part];

  if (info.allowed && info.sources.length === 0) {
    return (
      <View className="h-56 justify-center rounded-card bg-surface">
        <ScreenState kind="empty" body={t('content.noVideo')} />
      </View>
    );
  }

  if (info.allowed && source) {
    return (
      <View className="gap-3">
        {/* key: смена части пересоздаёт плеер — надёжнее ручной подмены
            источника у живого плеера и не тащит позицию из прошлой части. */}
        <Player
          key={source.mediaId ?? part}
          source={source}
          orientation={info.orientation}
          contentId={info.contentId}
          episodeId={info.episodeId}
        />

        {info.sources.length > 1 ? (
          <View className="flex-row flex-wrap gap-2">
            {info.sources.map((s, i) => (
              <Pressable
                key={s.mediaId ?? i}
                onPress={() => setPart(i)}
                accessibilityRole="button"
                accessibilityState={{ selected: i === part }}
                className={`rounded-pill px-4 py-2 ${i === part ? 'bg-purple' : 'bg-surface'}`}
              >
                <Text
                  className={`text-caption ${
                    i === part ? 'font-semibold text-white' : 'text-text-muted'
                  }`}
                >
                  {t('content.part', { number: s.partNumber ?? i + 1 })}
                </Text>
              </Pressable>
            ))}
          </View>
        ) : null}
      </View>
    );
  }

  return <LockedPoster info={info} card={card} />;
}

/** Обложка закрытого контента — в пропорции его формата. */
function LockedPoster({
  info,
  card,
}: {
  info: WatchInfo;
  card: ContentCard | undefined;
}) {
  const { height: windowHeight } = useWindowDimensions();
  const poster = mediaUrl(card?.posterMediaId);

  const vertical = isVertical(info.orientation);
  const [boxWidth, setBoxWidth] = useState(0);

  // Рамка та же, что у плеера: закрытый экран не должен менять раскладку
  // после покупки — иначе кадр прыгнет, как только доступ появится.
  const ratio = frameRatio(info.orientation);

  // Вертикальная афиша во всю высоту вытолкнула бы цену за нижний край —
  // а именно её человек и должен увидеть на этом экране.
  const full = boxWidth > 0 ? Math.round(boxWidth / ratio) : 224;
  const height = vertical ? Math.min(full, Math.round(windowHeight * 0.45)) : full;
  const width = vertical ? Math.round(height * ratio) : boxWidth;

  return (
    <View
      onLayout={(e) => setBoxWidth(e.nativeEvent.layout.width)}
      className="items-center"
    >
      <View
        style={{ width: boxWidth > 0 ? width : '100%', height }}
        className="overflow-hidden rounded-card bg-surface-2"
      >
        {poster ? (
          <Image
            source={{ uri: poster }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
          />
        ) : null}
        <View className="absolute inset-0 items-center justify-center">
          <Text className="text-display">🔒</Text>
        </View>
      </View>
    </View>
  );
}

/** Бейдж доступа — по причине, которую назвал сервер. */
function accessBadge(reason: string): { tone: BadgeTone; key: string } | null {
  switch (reason) {
    case 'FREE':
      return { tone: 'purchased', key: 'common.free' };
    case 'PREMIUM':
      return { tone: 'premiere', key: 'common.premium' };
    case 'EPISODE_PURCHASE':
    case 'PREMIERE_PURCHASE':
      return { tone: 'purchased', key: 'common.purchased' };
    default:
      return null;
  }
}

/** Номер серии, тип, возраст, длительность — только то, что реально пришло. */
function Facts({ info, card }: { info: WatchInfo; card: ContentCard | undefined }) {
  const { t } = useTranslation();

  const minutes =
    info.durationSeconds !== null && info.durationSeconds > 0
      ? Math.max(1, Math.round(info.durationSeconds / 60))
      : null;

  const facts = [
    info.episodeNumber !== null ? t('content.part', { number: info.episodeNumber }) : null,
    card?.contentType
      ? t(`contentType.${card.contentType}`, { defaultValue: card.contentType })
      : null,
    card?.ageRating,
    minutes !== null ? t('content.minutes', { count: minutes }) : null,
  ].filter((f): f is string => Boolean(f));

  const badge = info.allowed ? accessBadge(info.reason) : null;

  if (facts.length === 0 && badge === null) return null;

  return (
    <View className="flex-row flex-wrap items-center gap-2">
      {badge ? <Badge tone={badge.tone}>{t(badge.key)}</Badge> : null}
      {facts.map((f) => (
        <Text key={f} className="text-caption text-text-muted">
          {f}
        </Text>
      ))}
    </View>
  );
}

/** Разряды пробелами: 5000 → «5 000». Цену считает сервер, мы только читаем. */
function groupDigits(amount: number): string {
  return String(Math.round(amount)).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

/** Что делать — по требуемому действию. */
const LOCKED_BODY: Record<string, string> = {
  SIGN_IN: 'content.needSignIn',
  BUY_EPISODE: 'content.needPurchase',
  BUY_PREMIERE: 'content.needPurchase',
  SUBSCRIBE: 'content.needSubscription',
  BUY_OR_SUBSCRIBE: 'content.needPurchaseOrSubscription',
};

/**
 * Отказы, в которых делать нечего: `requiredAction` там `NONE`, и без этой
 * таблицы человек увидел бы общее «закрыто» вместо настоящей причины.
 */
const LOCKED_REASON: Record<string, string> = {
  NOT_PUBLISHED: 'content.notPublished',
  USER_BLOCKED: 'content.userBlocked',
};

/**
 * Почему закрыто и что с этим делать.
 *
 * Кнопка рисуется только там, где ей есть куда вести. Вход есть, экрана
 * оплаты (19) нет — он ждёт решения по оплате через сторы. Поэтому там, где
 * нужна оплата, показывается НАСТОЯЩАЯ цена сервера и неактивная кнопка, а
 * не рабочая кнопка в никуда.
 */
function LockedPanel({ info }: { info: WatchInfo }) {
  const { t } = useTranslation();
  const action: RequiredAction = info.requiredAction;

  const needsSignIn = action === 'SIGN_IN';
  const needsPurchase =
    action === 'BUY_EPISODE' || action === 'BUY_PREMIERE' || action === 'BUY_OR_SUBSCRIBE';
  const needsSubscription = action === 'SUBSCRIBE' || action === 'BUY_OR_SUBSCRIBE';

  const bodyKey = LOCKED_BODY[action] ?? LOCKED_REASON[info.reason] ?? 'states.lockedBody';
  const price = info.episodePrice ?? info.premierePrice;

  return (
    <View className="gap-3 rounded-card bg-surface p-4">
      <Text className="text-h2 text-text">{t('states.lockedTitle')}</Text>
      <Text className="text-body text-text-muted">{t(bodyKey)}</Text>

      {needsSignIn ? (
        <Button onPress={() => router.push('/(auth)/sign-in')}>{t('profile.signIn')}</Button>
      ) : null}

      {needsPurchase ? (
        <Button variant="premium" disabled>
          {price === null
            ? t('common.buy')
            : `${t('common.buy')} · ${t('common.price', { amount: groupDigits(price) })}`}
        </Button>
      ) : null}

      {needsSubscription ? (
        <Button variant="gold" disabled>
          {t('content.subscribe')}
        </Button>
      ) : null}

      {needsPurchase || needsSubscription ? (
        <Text className="text-micro text-text-muted">{t('content.paymentSoon')}</Text>
      ) : null}
    </View>
  );
}
