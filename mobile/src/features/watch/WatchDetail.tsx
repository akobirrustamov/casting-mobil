import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View, useWindowDimensions } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Badge, type BadgeTone } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { trackContentView } from '@/features/analytics/api';
import { formatDuration } from '@/features/content/duration';
import { CARD_RATIO } from '@/features/content/railLayout';
import { contentCards, useHomeFeed } from '@/features/home/api';
import type { ContentCard } from '@/features/home/types';
import { mediaUrl } from '@/lib/api';
import { colors, gradients, radius } from '@/theme/tokens';
import { LockedPanel } from '@/features/content/LockedPanel';
import { colors } from '@/theme/tokens';
import { useIsOffline } from '@/lib/network';
import { formatSum } from '@/lib/money';

import {
  ContentNotFoundError,
  WatchUnavailableError,
  type useWatchContent,
} from './api';
import { CastRow } from './CastRow';
import { Player, playbackSource } from './Player';
import { StatTiles } from './StatTiles';
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

  /**
   * Плеер поднимается только по кнопке.
   *
   * ⚠️ Сбрасывается при смене контента: экран переиспользуется при
   * переходе «серия → серия», и без сброса следующая начинала бы
   * играть сама, без нажатия.
   */
  const [playing, setPlaying] = useState(false);
  useEffect(() => setPlaying(false), [contentId, episodeId]);

  return (
    <Screen
      // ⚠️ Заголовка в шапке НЕТ намеренно: по референсу название стоит
      // крупно под афишей. Оставь его и здесь — человек прочитает одно
      // и то же дважды, а место под афишу украдёт строка, которую он
      // уже прочёл.
      title=" "
      onBack={() => router.back()}
      underTabBar={false}
      onRefresh={() => query.refetch()}
      refreshing={query.isRefetching}
    >
      <Stage
        info={info}
        card={card}
        playing={playing}
        onRetry={() => query.refetch()}
      />

      <Head info={info} card={card} />

      {info.allowed && !playing ? <WatchCta onPress={() => setPlaying(true)} /> : null}

      <StatTiles info={info} />

      {card?.shortDescription ? (
        <Text className="text-body text-text-muted">{card.shortDescription}</Text>
      ) : null}

      <GenreChips card={card} />
      <CastRow credits={info.credits} />
      <TrailerSection info={info} card={card} />

      {info.allowed ? null : <LockedPanel info={info} />}
    </Screen>
  );
}

/**
 * Шапка под афишей: бейдж, название, строка фактов.
 *
 * Порядок с референса и он не случайный: сначала ЧТО это (сериал,
 * фильм), потом название, потом мелочи. Человек, пришедший из ряда на
 * главной, узнаёт контент по названию — оно и должно быть самым
 * крупным на экране.
 */
function Head({ info, card }: { info: WatchInfo; card: ContentCard | undefined }) {
  const { t } = useTranslation();

  const kind = card?.contentType
    ? t(`contentType.${card.contentType}`, { defaultValue: card.contentType })
    : null;

  return (
    <View className="gap-2">
      {kind ? (
        <View className="flex-row">
          <Badge tone="premiere">{kind.toUpperCase()}</Badge>
        </View>
      ) : null}

      <Text className="text-h1 text-text">{info.title ?? card?.title ?? ''}</Text>

      <Facts info={info} card={card} />
    </View>
  );
}

/**
 * Главная кнопка — «Tomosha qilish».
 *
 * <h2>⚠️ Видео открывается ТОЛЬКО отсюда</h2>
 * Заказчик (09.09.2026): «Только нажав на кнопку можно получить доступ
 * к видео». До этого плеер стоял сразу и начинал грузиться сам — то
 * есть трафик тратился у каждого, кто просто заглянул на карточку.
 *
 * <h2>⚠️ Заливка здесь ГРАДИЕНТНАЯ, и это исключение</h2>
 * 01.09.2026 заказчик просил одну заливку на все кнопки — сплошной
 * фиолетовый (`components/ui/Button`). 09.09.2026 он же прислал
 * референс, где главная кнопка контента — градиент, и сказал повторить
 * «в точности до цвета кнопок». Более позднее указание сильнее.
 *
 * Исключение ОДНО и живёт здесь, а не в `Button`: вернись градиент в
 * общий компонент — и он расползётся по всем экранам, откуда его
 * убирали.
 */
function WatchCta({ onPress }: { onPress: () => void }) {
  const { t } = useTranslation();

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={t('content.watch')}
      className="overflow-hidden active:opacity-80"
      style={{ borderRadius: radius.card, minHeight: 52 }}
    >
      <LinearGradient
        colors={gradients.premium}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={{
          flex: 1,
          minHeight: 52,
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
        }}
      >
        <Ionicons name="play" size={18} color={colors.white} />
        <Text className="text-body font-semibold text-white">{t('content.watch')}</Text>
      </LinearGradient>
    </Pressable>
  );
}

/**
 * Жанры отдельными плашками — как на референсе.
 *
 * ⚠️ Фид отдаёт ОДИН жанр (`ContentCard.genre`), поэтому плашка обычно
 * одна. Полного списка жанров у карточки в API пока нет; выдумывать
 * второй жанр ради симметрии с макетом нельзя.
 */
function GenreChips({ card }: { card: ContentCard | undefined }) {
  const genres = [card?.genre].filter((g): g is string => Boolean(g));
  if (genres.length === 0) return null;

  return (
    <View className="flex-row flex-wrap gap-2">
      {genres.map((genre) => (
        <View key={genre} className="rounded-pill bg-surface px-4 py-2">
          <Text className="text-caption text-text">{genre}</Text>
        </View>
      ))}
    </View>
  );
}

/**
 * «Treyler» — отдельным блоком под составом, как на референсе.
 *
 * <h2>⚠️ Раньше ролик стоял НАВЕРХУ закрытого экрана</h2>
 * Это работало, но спорило с референсом: там сверху всегда афиша, а
 * трейлер — именованный блок ниже. Заодно исчезла путаница «что сейчас
 * играет»: наверху фильм, здесь ролик, и подписано.
 */
function TrailerSection({
  info,
  card,
}: {
  info: WatchInfo;
  card: ContentCard | undefined;
}) {
  const { t } = useTranslation();
  const trailer = info.trailer;

  /**
   * ⚠️ Ролик тоже поднимается ТОЛЬКО по нажатию.
   *
   * Пока здесь стоял плеер, он начинал грузить трейлер при каждом
   * открытии экрана — ровно то, от чего мы избавились у основного
   * видео. На карточке два видео сразу — это двойной трафик у
   * человека, который ещё ничего не выбрал.
   */
  const [playing, setPlaying] = useState(false);
  const [failed, setFailed] = useState(false);

  if (trailer === null || failed) return null;

  const poster = mediaUrl(card?.posterMediaId);
  const length = formatDuration(trailer.durationSeconds);

  return (
    <View className="gap-3">
      <Text className="text-h2 text-text">{t('content.trailer')}</Text>

      {playing ? (
        <Player
          key={`trailer-${trailer.mediaId}-${trailer.hlsUrl ? 'hls' : 'raw'}`}
          source={trailer}
          orientation={info.orientation}
          // ⚠️ Ни контента, ни серии: ролик не должен ни писать позицию
          // просмотра, ни считаться просмотром фильма.
          contentId={null}
          episodeId={null}
          onError={() => setFailed(true)}
        />
      ) : (
        <Pressable
          onPress={() => setPlaying(true)}
          accessibilityRole="button"
          accessibilityLabel={t('content.trailer')}
          className="h-44 overflow-hidden rounded-card bg-surface-2 active:opacity-80"
        >
          {poster ? (
            <Image
              source={{ uri: poster }}
              style={{ width: '100%', height: '100%' }}
              contentFit="cover"
              transition={150}
            />
          ) : null}

          {/* Кружок с треугольником по центру — как на референсе. */}
          <View className="absolute inset-0 items-center justify-center">
            <View
              className="items-center justify-center rounded-full"
              style={{
                width: 56,
                height: 56,
                backgroundColor: 'rgba(0,0,0,0.45)',
                borderWidth: 1,
                borderColor: 'rgba(255,255,255,0.5)',
              }}
            >
              <Ionicons name="play" size={24} color={colors.white} />
            </View>
          </View>

          {length ? (
            <View className="absolute bottom-2 left-2 rounded-pill bg-ink/80 px-2.5 py-1">
              <Text className="text-caption text-text">{length}</Text>
            </View>
          ) : null}
        </Pressable>
      )}
    </View>
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
  playing,
  onRetry,
}: {
  info: WatchInfo;
  card: ContentCard | undefined;
  /** Человек нажал «Tomosha qilish». До этого наверху стоит афиша. */
  playing: boolean;
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

  /**
   * ⚠️ Пока кнопку не нажали — только афиша, и это не украшение.
   *
   * Плеер, поставленный сразу, начинает грузить видео у каждого, кто
   * просто заглянул на карточку: трафик человека и наш CDN тратятся на
   * тех, кто смотреть не собирался. Заказчик (09.09.2026) закрепил это
   * прямо: «только нажав на кнопку можно получить доступ к видео».
   */
  if (!playing) {
    return <Poster card={card} />;
  }

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

  return <Poster card={card} />;
}

/**
 * Афиша наверху экрана.
 *
 * ⚠️ Стоит ВСЕГДА, пока не нажата кнопка «Tomosha qilish» — и на
 * закрытом контенте, и на открытом. Раньше она была только у закрытого,
 * а у открытого сразу поднимался плеер; теперь верх экрана одинаковый,
 * как на референсе заказчика (09.09.2026).
 *
 * ⚠️ Трейлер отсюда УБРАН: он теперь именованный блок ниже
 * (`TrailerSection`). Пока ролик стоял наверху без подписи, 90 секунд в
 * рамке плеера читались как «фильм уже открыт».
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
function Poster({
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

  const views = info.viewCount;

  const facts = [
    info.episodeNumber !== null ? t('content.part', { number: info.episodeNumber }) : null,
    // ⚠️ Типа контента здесь БОЛЬШЕ НЕТ: он стоит бейджем над названием
    // (`Head`), как на референсе. Пока он был и там, и тут, «Mini-serial»
    // читался дважды подряд.
    card?.ageRating,
    minutes !== null ? t('content.minutes', { count: minutes }) : null,
    // Просмотры ушли сюда, в мелкую строку: на референсе их нет среди
    // плиток, но цифра полезная, а места в строке фактов достаточно.
    views !== null ? t('content.views', { count: views }) : null,
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
