import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View, useWindowDimensions } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Badge, type BadgeTone } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { trackContentView } from '@/features/analytics/api';
import { CARD_RATIO } from '@/features/content/railLayout';
import { contentCards, useHomeFeed } from '@/features/home/api';
import type { ContentCard } from '@/features/home/types';
import { mediaUrl } from '@/lib/api';
import { colors } from '@/theme/tokens';
import { useIsOffline } from '@/lib/network';
import { formatSum } from '@/lib/money';

import {
  ContentNotFoundError,
  WatchUnavailableError,
  type useWatchContent,
} from './api';
import { Player, playbackSource } from './Player';
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
      <Stage info={info} card={card} onRetry={() => query.refetch()} />
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
function Stage({
  info,
  card,
  onRetry,
}: {
  info: WatchInfo;
  card: ContentCard | undefined;
  /** Перезапрашивает `/watch` — оттуда приходит свежий адрес видео. */
  onRetry: () => Promise<unknown>;
}) {
  const { t } = useTranslation();
  const [part, setPart] = useState(0);

  const source = info.sources[part];

  /**
   * Видео не открылось.
   *
   * <h2>Почему это лечится перезапросом `/watch`</h2>
   * Адрес видео подписан и живёт ограниченное время: билет плейлиста —
   * 6 часов, подпись сегмента в хранилище — около трёх. Открытый вечером и
   * продолженный утром фильм упирается именно в это, и починка одна —
   * спросить адрес заново.
   *
   * ⚠️ Повтор РУЧНОЙ. Автоматический бился бы в ту же стену на каждом кадре:
   * причина может быть и в сети, и в снятой подписке — тогда сервер честно
   * ответит отказом, и молотить его незачем.
   *
   * Место просмотра при этом не теряется: позиция пишется на телефон каждые
   * пять секунд (`useWatchProgress`), а новый плеер читает её при создании.
   */
  const [failed, setFailed] = useState(false);
  const [retrying, setRetrying] = useState(false);

  /**
   * ⚠️ Ошибку снимает ОТВЕТ сервера, а не сам факт нажатия.
   *
   * Пока `/watch` не ответил, в руках старая — уже просроченная — ссылка.
   * Показать по ней плеер значило бы вернуть ту же ошибку через секунду, и
   * кнопка выглядела бы неработающей.
   */
  const retry = useCallback(() => {
    setRetrying(true);
    void onRetry().finally(() => {
      setRetrying(false);
      setFailed(false);
    });
  }, [onRetry]);

  // Адрес сменился (повтор или «потянули вниз») — прошлый сбой к нему
  // отношения не имеет.
  const uri = source ? playbackSource(source).uri : null;
  useEffect(() => setFailed(false), [uri]);

  if (info.allowed && info.sources.length === 0) {
    return (
      <View className="h-56 justify-center rounded-card bg-surface">
        <ScreenState kind="empty" body={t('content.noVideo')} />
      </View>
    );
  }

  if (info.allowed && source && (failed || retrying)) {
    return (
      <View className="h-56 justify-center rounded-card bg-surface">
        {retrying ? (
          <ScreenState kind="loading" />
        ) : (
          <ScreenState
            kind="error"
            title={t('content.playbackFailedTitle')}
            body={t('content.playbackFailedBody')}
            onRetry={retry}
          />
        )}
      </View>
    );
  }

  if (info.allowed && source) {
    return (
      <View className="gap-3">
        {/* key: смена части пересоздаёт плеер — надёжнее ручной подмены
            источника у живого плеера и не тащит позицию из прошлой части.

            ⚠️ В ключе есть и признак HLS. Без него так: пользователь
            открыл эпизод, пока он ещё обрабатывался (играет через
            сервер), потом потянул экран вниз — `/watch` уже отдаёт
            `hlsUrl`, но плеер остаётся со старым адресом, потому что
            ключ не изменился. Видео продолжает идти мимо CDN. */}
        <Player
          key={`${source.mediaId ?? part}-${source.hlsUrl ? 'hls' : 'raw'}`}
          source={source}
          orientation={info.orientation}
          contentId={info.contentId}
          episodeId={info.episodeId}
          onError={() => setFailed(true)}
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

  return <LockedStage info={info} card={card} />;
}

/**
 * Верх закрытого экрана.
 *
 * <h2>Зачем здесь вообще видео</h2>
 * Раньше человек до покупки не мог посмотреть НИЧЕГО: афиша и замок. При
 * этом главный сценарий ТЗ — «первая серия бесплатно, продолжение за
 * 5 000» — упирается ровно в этот экран. Трейлер и есть то единственное,
 * что можно показать, не отдав фильм.
 *
 * <h2>⚠️ Ролик подписан бейджем — и это не украшение</h2>
 * Без подписи 90 секунд в рамке плеера читаются как «фильм уже открыт»:
 * человек досматривает ролик, видит конец и уходит, решив, что купил
 * пустоту. Бейдж — единственное, что отличает одно от другого.
 *
 * <h2>Сбой трейлера — не ошибка экрана</h2>
 * Ролик необязателен, поэтому на сбой возвращаемся к афише молча. Кнопка
 * «попробовать ещё раз» здесь была бы враньём: человеку нужен не трейлер,
 * а фильм, и повтор ничего для него не изменит.
 */
function LockedStage({ info, card }: { info: WatchInfo; card: ContentCard | undefined }) {
  const { t } = useTranslation();
  const [failed, setFailed] = useState(false);

  const trailer = info.trailer;
  if (trailer === null || failed) {
    return <LockedPoster card={card} />;
  }

  return (
    <View className="gap-2">
      {/*
        ⚠️ `contentId` и `episodeId` — `null` НАМЕРЕННО.

        Плеер по ним делает две вещи: пишет позицию просмотра и считает
        запуск контента. Ни то, ни другое к ролику не относится: позиция
        трейлера затёрла бы место, на котором человек бросил сам фильм, а
        счётчик просмотров раздулся бы теми, кто ничего не купил.
      */}
      <Player
        key={`trailer-${trailer.mediaId}-${trailer.hlsUrl ? 'hls' : 'raw'}`}
        source={trailer}
        orientation={info.orientation}
        contentId={null}
        episodeId={null}
        onError={() => setFailed(true)}
      />

      <View className="flex-row justify-center">
        <Badge tone="info">{t('content.trailer')}</Badge>
      </View>
    </View>
  );
}

/**
 * Обложка закрытого контента.
 *
 * <h2>Почему 2:3, а не формат видео</h2>
 * Раньше рамка бралась из `frameRatio(orientation)` — 16:9 у обычного
 * контента. Довод был такой: пусть закрытый экран выглядит как плеер,
 * тогда после покупки раскладка не прыгнет.
 *
 * Но в админку загружается ОДНА афиша, и загружается она вертикальной
 * (2:3 — `adminpanel/mediaSpecs.poster`), потому что во всём остальном
 * приложении карточка именно такая (`railLayout.CARD_RATIO`). В рамке
 * 16:9 от такой афиши оставалась горизонтальная полоса посередине: у
 * постера срезало примерно две трети высоты вместе с названием сверху.
 *
 * То есть выбор был не «прыгнет или нет», а «показать афишу целиком или
 * её середину». Скачок раскладки случается ОДИН раз и только после
 * покупки; обрезанная афиша — на каждом открытии закрытого контента.
 *
 * Форма кадра у ПЛЕЕРА не изменилась: он по-прежнему рисует 16:9 или 9:16
 * по `orientation`, то есть рилс открывается вертикальным.
 */
function LockedPoster({
  card,
}: {
  card: ContentCard | undefined;
}) {
  const { height: windowHeight } = useWindowDimensions();
  const poster = mediaUrl(card?.posterMediaId);

  const [boxWidth, setBoxWidth] = useState(0);

  // Одна форма афиши на всё приложение — ряд, сетка, «Barchasi» и этот
  // экран. Загруженный файл 2:3 нигде не обрезается.
  const ratio = CARD_RATIO;

  // Афиша во всю ширину вытолкнула бы цену за нижний край — а именно её
  // человек и должен увидеть на этом экране. Поэтому высота ограничена, а
  // ширина считается обратно от неё: кадр сужается, но не обрезается.
  const full = boxWidth > 0 ? Math.round(boxWidth / ratio) : 224;
  const height = Math.min(full, Math.round(windowHeight * 0.45));
  const width = Math.round(height * ratio);

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

  /**
   * Показывать ли «оплата скоро».
   *
   * Раньше кнопка стояла выключенной, а подпись висела всегда. Выключенная
   * кнопка на 40% прозрачности выглядит блёклой — на макете заказчика она
   * в полном цвете, и на скриншоте разница бросается в глаза.
   *
   * Теперь кнопка обычная, а на нажатие честно отвечает, что оплаты пока
   * нет. Это не «кнопка в никуда»: на касание она реагирует и объясняет.
   */
  const [paymentNote, setPaymentNote] = useState(false);

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
        // Ромб и цена — форма кнопки покупки с макета заказчика.
        // Слово «купить» на ней лишнее: цена и знак говорят то же самое.
        <Button
          variant="purchase"
          onPress={() => setPaymentNote(true)}
          leading={<Ionicons name="diamond" size={16} color={colors.white} />}
        >
          {price === null
            ? t('common.buy')
            : t('common.price', { amount: formatSum(price) })}
        </Button>
      ) : null}

      {needsSubscription ? (
        <Button variant="gold" onPress={() => setPaymentNote(true)}>
          {t('content.subscribe')}
        </Button>
      ) : null}

      {paymentNote ? (
        <Text className="text-micro text-text-muted">{t('content.paymentSoon')}</Text>
      ) : null}
    </View>
  );
}
