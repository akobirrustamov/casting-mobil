import { Ionicons } from '@expo/vector-icons';
import * as Linking from 'expo-linking';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { router, useIsFocused } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Modal,
  Pressable,
  RefreshControl,
  ScrollView,
  Share,
  Text,
  View,
  useWindowDimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenState } from '@/components/states/ScreenState';
import { trackContentView } from '@/features/analytics/api';
import { useAuthStore } from '@/features/auth/store';
import { useContentFavorites, useIsContentSaved } from '@/features/favorites/content';
import { contentCards, useContentCard, useHomeFeed } from '@/features/home/api';
import type { ContentCard } from '@/features/home/types';
import {
  ContentIsMultiPartError,
  ContentNotFoundError,
  WatchUnavailableError,
  useWatchContent,
} from '@/features/watch/api';
import { Player, playbackSource } from '@/features/watch/Player';
import type { VideoSource, WatchInfo } from '@/features/watch/types';
import { mediaUrl } from '@/lib/api';
import { pushOnce } from '@/lib/navigation';
import { useIsOffline } from '@/lib/network';
import { TOUCH_TARGET, colors, radius } from '@/theme/tokens';

import { CastRail, ScenesRail, TrailerCard } from './ContentExtras';
import {
  ContentDetailUnavailableError,
  serverLacksContentDetail,
  useContentDetail,
  type ContentDetail,
} from './detail';
import { LockedPanel } from './LockedPanel';
import { PlayerActions } from './PlayerActions';
import { useSerialCountersFallback } from './serialCounters';
import { StatsRow } from './StatsRow';

/**
 * Экран контента — макет заказчика «1. Content ochilish oynasi» (08.09.2026).
 *
 * <h2>Что изменилось против прежнего экрана</h2>
 * Прежний открывался ПЛЕЕРОМ: сверху видео, под ним пара строк из кэша
 * главной. Заказчик прислал другой порядок — сначала афиша во всю ширину,
 * название, кнопка «Tomosha qilish», и только потом всё остальное. То есть
 * страница отвечает на вопрос «стоит ли это смотреть», а плеер включается
 * по нажатию.
 *
 * Данные для этого берутся из `/api/v1/app/content/{id}` (`./detail`), а не
 * из кэша главной: в карточке ряда нет ни года, ни жанров, ни описания, ни
 * актёров. Кэш остался ЗАПАСНЫМ источником — на старой сборке бэкенда
 * нового адреса нет, и экран обязан работать без него.
 *
 * <h2>Многосерийный контент открывается ЗДЕСЬ ЖЕ</h2>
 * Раньше сериал сразу проваливался в список серий, минуя карточку. На
 * макете так же, как у фильма: та же страница, но «Tomosha qilish» ведёт
 * в список серий (макет «3»). Иначе описание, актёры и донаты сериалу были
 * бы недоступны вовсе.
 */
export function ContentScreen({ contentId }: { contentId: number | null }) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const isOffline = useIsOffline();

  const detail = useContentDetail(contentId);
  const watch = useWatchContent(contentId);

  /**
   * Многосерийный ли контент.
   *
   * ⚠️ Два признака, и это не перестраховка. `structureType` — прямой
   * ответ нового эндпоинта, но на старой сборке бэкенда его нет вовсе;
   * там единственный признак — отказ `/watch` со словами «контент
   * многосерийный». Без второго условия сериал на старом сервере
   * показывал бы кнопку «смотреть», ведущую в ошибку.
   */
  const isMultiPart =
    (detail.data?.structureType != null && detail.data.structureType !== 'SINGLE') ||
    watch.error instanceof ContentIsMultiPartError;

  // Счётчики сериала на сервере без карточки — см. `serialCounters`.
  // ⚠️ Только для плиток: в `info` страницы это НЕ идёт.
  //
  // ⚠️ Скорость (10.09.2026): если сервер УЖЕ отказал в карточке раньше,
  // а карточка ряда говорит «это сериал» (у неё есть число серий),
  // обходная дорога стартует сразу — не дожидаясь, пока откажут ещё раз
  // карточка и `/watch`. Минус один поход в сеть на каждом сериале.
  const card = useContentCard(contentId);
  const knownSerial = isMultiPart || (card?.episodeCount ?? null) !== null;
  const serialCounters = useSerialCountersFallback(
    contentId,
    knownSerial &&
      (detail.error instanceof ContentDetailUnavailableError || serverLacksContentDetail())
  );

  // Открытие карточки — отдельное от запуска видео событие: человек может
  // зайти, увидеть цену и уйти, и в отчётах это разные вещи.
  useEffect(() => {
    if (contentId !== null) trackContentView(contentId, null);
  }, [contentId]);

  const notFound =
    watch.error instanceof ContentNotFoundError && detail.error !== null;

  const refresh = useCallback(() => {
    void detail.refetch();
    void watch.refetch();
  }, [detail, watch]);

  if (notFound) {
    return (
      <Plain>
        <ScreenState kind="empty" body={t('content.notFound')} />
      </Plain>
    );
  }

  if (detail.isPending && watch.isPending) {
    return (
      <Plain>
        <ScreenState kind="loading" />
      </Plain>
    );
  }

  /**
   * Совсем нечего показать.
   *
   * ⚠️ Проверяются ОБА источника. Пока новый эндпоинт не разошёлся по
   * серверам, карточка приходит только из `/watch` — и падение одного
   * запроса не повод закрывать экран, на котором есть данные другого.
   */
  if (detail.data === undefined && watch.data === undefined && !isMultiPart) {
    const unavailable = watch.error instanceof WatchUnavailableError;
    return (
      <Plain>
        <ScreenState
          kind={isOffline ? 'offline' : 'error'}
          title={unavailable ? t('home.feedUnavailableTitle') : undefined}
          body={unavailable ? t('home.feedUnavailableBody') : undefined}
          onRetry={refresh}
        />
      </Plain>
    );
  }

  return (
    <Loaded
      contentId={contentId}
      detail={detail.data}
      info={watch.data}
      isMultiPart={isMultiPart}
      counters={serialCounters}
      refreshing={detail.isRefetching || watch.isRefetching}
      onRefresh={refresh}
      onRetryPlayback={() => watch.refetch()}
      bottomInset={insets.bottom}
    />
  );
}

/** Каркас для состояний, когда карточки ещё нет. */
function Plain({ children }: { children: React.ReactNode }) {
  const insets = useSafeAreaInsets();

  return (
    <View className="flex-1 bg-ink" style={{ paddingTop: insets.top }}>
      <View className="flex-row px-2 py-2">
        <BackButton />
      </View>
      <View className="flex-1">{children}</View>
    </View>
  );
}

function BackButton({ floating = false }: { floating?: boolean }) {
  return (
    <Pressable
      onPress={() => router.back()}
      accessibilityRole="button"
      accessibilityLabel="Orqaga"
      hitSlop={12}
      style={{ width: TOUCH_TARGET, height: TOUCH_TARGET }}
      className={`items-center justify-center rounded-pill active:opacity-60 ${
        // На афише кнопка стоит поверх кадра: без подложки чёрная стрелка
        // теряется в тёмном кадре, а белая — в светлом.
        floating ? 'bg-black/45' : ''
      }`}
    >
      <Ionicons name="chevron-back" size={24} color={colors.white} />
    </Pressable>
  );
}

function Loaded({
  contentId,
  detail,
  info,
  isMultiPart,
  counters,
  refreshing,
  onRefresh,
  onRetryPlayback,
  bottomInset,
}: {
  contentId: number | null;
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
  isMultiPart: boolean;
  /** Запасной источник счётчиков сериала — только для плиток. */
  counters: WatchInfo | undefined;
  refreshing: boolean;
  onRefresh: () => void;
  onRetryPlayback: () => Promise<unknown>;
  bottomInset: number;
}) {
  /**
   * Открыт ли полноэкранный плеер.
   *
   * ⚠️ Живёт на уровне страницы: нажимают кнопку в теле страницы, а окно
   * плеера лежит поверх всего экрана. Ни одна из двух частей не знает о
   * другой, кроме как через этот флаг.
   */
  const [playing, setPlaying] = useState(false);

  /**
   * Запасной источник шапки — карточка ряда с главной.
   *
   * ⚠️ Нужен не для красоты. У многосерийного контента `/watch` молчит
   * («спрашивай серию»), и если рядом отказал ещё и `/content/{id}`, то
   * ни названия, ни афиши на экране не оставалось вовсе — открывалась
   * чёрная страница с одним описанием. Название и постер в карточке ряда
   * есть всегда.
   */
  const feed = useHomeFeed();
  const card =
    contentId === null
      ? undefined
      : contentCards(feed.data).find((c) => c.id === contentId);

  return (
    <View className="flex-1 bg-ink">
      <ScrollView
        className="flex-1"
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: bottomInset + 32 }}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.purple}
            colors={[colors.purple]}
            progressBackgroundColor={colors.surface}
          />
        }
      >
        <Hero detail={detail} info={info} card={card} />

        <View className="gap-5 px-4 pt-4">
          {/* Кнопка и закладка стоят в одной строке — как на макете.
              Закладка остаётся и тогда, когда кнопки нет (контент
              закрыт или плеер уже включён): отложить «на потом» можно и
              то, что сейчас не открывается. */}
          <View className="flex-row items-center gap-2">
            <View className="flex-1">
              <WatchCta
                contentId={contentId}
                info={info}
                isMultiPart={isMultiPart}
                episodeCount={detail?.episodeCount ?? null}
                onPlay={() => setPlaying(true)}
              />
            </View>
            <SaveButton contentId={contentId} />
          </View>

          <StatsRow contentId={contentId} detail={detail} info={info ?? counters} />

          <Synopsis detail={detail} contentId={contentId} />

          <GenreTags genres={detail?.genres ?? []} />

          {info && !info.allowed ? <LockedPanel info={info} /> : null}

          <CastRail cast={detail?.cast ?? []} />

          <TrailerCard detail={detail} info={info} />

          <ScenesRail mediaIds={detail?.galleryMediaIds ?? []} />
        </View>
      </ScrollView>

      {/*
        ⚠️ Плеер открывается ОТДЕЛЬНЫМ окном во весь экран, а не подменяет
        собой афишу.

        Так просил заказчик (10.09.2026), и причина не только в размере
        кадра: на странице под плеером оставались описание, актёры и
        кадры, и человек листал их, пока фильм играл где-то вверху.
        Полноэкранное окно снимает этот вопрос — а «назад» возвращает на
        ту же страницу, никуда не уводя из истории переходов.
      */}
      <WatchScreen
        open={playing}
        contentId={contentId}
        detail={detail}
        info={info}
        onClose={() => setPlaying(false)}
        onRetryPlayback={onRetryPlayback}
      />
    </View>
  );
}

/**
 * Полноэкранный просмотр.
 *
 * <h2>Почему окно, а не отдельный маршрут</h2>
 * Плееру нужен ответ `/watch` — он уже получен страницей. Отдельный
 * экран спросил бы его заново, и между нажатием и первым кадром встал бы
 * пустой чёрный экран с крутилкой.
 *
 * <h2>⚠️ Панель управления — НАША</h2>
 * У кнопок платформы нет места под «нравится», комментарии и донат, а на
 * макете они стоят прямо на кадре. Разбор — в `watch/Player`; там же
 * сказано, почему после кнопки «на весь экран» (системный режим) панель
 * всё-таки платформенная.
 */
function WatchScreen({
  open,
  contentId,
  detail,
  info,
  onClose,
  onRetryPlayback,
}: {
  open: boolean;
  contentId: number | null;
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
  onClose: () => void;
  onRetryPlayback: () => Promise<unknown>;
}) {
  const insets = useSafeAreaInsets();

  /**
   * ⚠️ Окно видно, только пока страница в фокусе.
   *
   * `Modal` рисуется поверх ВСЕГО приложения. Значок звезды на кадре ведёт
   * на страницу рейтинга, «нравится» у гостя — на вход; без этого условия
   * новая страница открывалась бы ПОД плеером, и человеку казалось бы, что
   * кнопка не сработала. Вернулся назад — окно снова открыто, просмотр
   * продолжается с сохранённого места.
   */
  const focused = useIsFocused();

  return (
    <Modal
      visible={open && focused}
      animationType="fade"
      onRequestClose={onClose}
      statusBarTranslucent
      // Кадр разворачивают в ландшафт кнопкой «на весь экран» — без
      // этого списка окно осталось бы портретным и плеер поворачивался
      // бы внутри неповёрнутого окна.
      supportedOrientations={[
        'portrait',
        'landscape',
        'landscape-left',
        'landscape-right',
      ]}
    >
      <View className="flex-1 bg-black" style={{ paddingTop: insets.top }}>
        <View className="flex-row px-2 py-2">
          <Pressable
            onPress={onClose}
            accessibilityRole="button"
            accessibilityLabel="Orqaga"
            hitSlop={12}
            style={{ width: TOUCH_TARGET, height: TOUCH_TARGET }}
            className="items-center justify-center rounded-pill active:opacity-60"
          >
            <Ionicons name="chevron-back" size={24} color={colors.white} />
          </Pressable>
        </View>

        <Stage
          contentId={contentId}
          detail={detail}
          info={info}
          onRetry={onRetryPlayback}
        />
      </View>
    </Modal>
  );
}

/**
 * Доля высоты экрана под афишу.
 *
 * На макете кадр занимает примерно половину экрана: ниже сразу видно
 * название и кнопку. Полная пропорция 2:3 у афиши выше — под ней кнопка
 * ушла бы за нижний край, и человек не понял бы, что фильм можно включить.
 */
const HERO_SCREEN_SHARE = 0.52;

/**
 * Шапка: кадр во всю ширину, поверх него — стрелка назад, «поделиться»,
 * бейдж, название и строка фактов.
 *
 * <h2>Почему кадр уходит под статус-бар</h2>
 * Так на макете, и это не украшение: тёмный градиент от кадра к фону —
 * единственное, что связывает картинку с текстом под ней. Обрезанный по
 * safe area кадр выглядел бы наклейкой на чёрном листе.
 */
function Hero({
  detail,
  info,
  card,
}: {
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
  /** Карточка ряда с главной — запасное название и афиша. */
  card: ContentCard | undefined;
}) {
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();

  const heroHeight = Math.round(height * HERO_SCREEN_SHARE);

  // Широкий кадр (COVER) — то, что задумано для шапки. Афиша 2:3 —
  // запасной вариант: она есть всегда, просто обрезается по бокам.
  const image = mediaUrl(
    detail?.coverMediaId ?? detail?.posterMediaId ?? card?.posterMediaId
  );

  return (
    <View style={{ height: heroHeight }} className="bg-surface-2">
      {image ? (
        <Image
          source={{ uri: image }}
          style={{ width: '100%', height: '100%' }}
          contentFit="cover"
          transition={200}
        />
      ) : null}

      {/*
        Градиент от прозрачного к фону экрана.

        ⚠️ Три точки, а не две: при линейном переходе на светлой афише
        середина остаётся мутно-серой и название на ней не читается.
      */}
      <LinearGradient
        colors={['transparent', 'rgba(5,5,10,0.55)', colors.ink]}
        locations={[0, 0.55, 1]}
        style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: '70%' }}
        pointerEvents="none"
      />

      <View
        style={{ paddingTop: insets.top + 4 }}
        className="absolute left-0 right-0 top-0 flex-row items-center justify-between px-2"
      >
        <BackButton floating />
        <ShareButton detail={detail} info={info} />
      </View>

      <View className="absolute bottom-0 left-0 right-0 gap-2 px-4 pb-4">
        <TypeBadge detail={detail} card={card} />

        <Text numberOfLines={2} className="text-display text-text">
          {detail?.title ?? info?.title ?? card?.title ?? ''}
        </Text>

        <FactsRow detail={detail} info={info} />
      </View>
    </View>
  );
}

function ShareButton({
  detail,
  info,
}: {
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
}) {
  const id = detail?.id ?? info?.contentId;
  const title = detail?.title ?? info?.title ?? '';

  if (id == null) return null;

  const share = async () => {
    // Ссылка — диплинк приложения, как в меню карточки на главной:
    // публичной веб-страницы у контента нет.
    const link = Linking.createURL(`/content/${id}`);
    await Share.share({ message: title ? `${title}\n${link}` : link }).catch(() => {});
  };

  return (
    <Pressable
      onPress={() => void share()}
      accessibilityRole="button"
      hitSlop={12}
      style={{ width: TOUCH_TARGET, height: TOUCH_TARGET }}
      className="items-center justify-center rounded-pill bg-black/45 active:opacity-60"
    >
      <Ionicons name="share-outline" size={20} color={colors.white} />
    </Pressable>
  );
}

/** «SERIAL», «FILM» — тип контента маджентой, как на макете. */
function TypeBadge({
  detail,
  card,
}: {
  detail: ContentDetail | undefined;
  card: ContentCard | undefined;
}) {
  const { t } = useTranslation();
  const type = detail?.contentType ?? card?.contentType;
  if (!type) return null;

  return (
    <View className="self-start rounded-pill bg-magenta px-3 py-1">
      <Text className="text-micro font-bold uppercase text-white">
        {t(`contentType.${type}`, { defaultValue: type })}
      </Text>
    </View>
  );
}

/**
 * Год · язык · возраст · длительность.
 *
 * ⚠️ Показывается только то, что реально пришло. На макете в этой строке
 * стоит «O'zbekiston», но СТРАНЫ в базе нет — есть язык оригинала, и
 * выводить страну из языка нельзя: корейский сериал с русской озвучкой
 * остался бы «Россией».
 */
function FactsRow({
  detail,
  info,
}: {
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
}) {
  const { t } = useTranslation();

  const views = detail?.viewCount ?? info?.viewCount ?? null;
  const seconds = detail?.durationSeconds ?? info?.durationSeconds ?? null;
  const minutes = seconds !== null && seconds > 0 ? Math.max(1, Math.round(seconds / 60)) : null;

  const facts = [
    detail?.year !== null && detail?.year !== undefined ? String(detail.year) : null,
    detail?.language
      ? t(`contentLanguage.${detail.language.toLowerCase()}`, {
          defaultValue: detail.language.toUpperCase(),
        })
      : null,
    detail?.ageRating ?? null,
    detail?.episodeCount ? t('content.episodeCount', { count: detail.episodeCount }) : null,
    minutes !== null ? t('content.minutes', { count: minutes }) : null,
    // ⚠️ Просмотры переехали сюда из плиток: на референсе третья плитка —
    // монеты, а пять плиток в ряд не помещаются. Цифра при этом нужна,
    // заказчик просил её отдельно (07.09.2026).
    views !== null ? t('content.views', { count: views }) : null,
  ].filter((f): f is string => Boolean(f));

  if (facts.length === 0) return null;

  return (
    <View className="flex-row flex-wrap items-center gap-2">
      {facts.map((f, i) => (
        <View key={`${f}-${i}`} className="flex-row items-center gap-2">
          {i > 0 ? <Text className="text-caption text-text-disabled">•</Text> : null}
          <Text className="text-caption text-text-muted">{f}</Text>
        </View>
      ))}
    </View>
  );
}

/**
 * Закладка «сохранить на потом».
 *
 * <h2>⚠️ Требует входа — и ведёт на вход, а не показывает ошибку</h2>
 * Список закладок хранится на сервере (см. `features/favorites/content`),
 * потому что иначе он терялся бы при переустановке. Гостю поэтому нечего
 * сохранять: вместо отказа отправляем его на экран входа.
 *
 * <h2>Почему список догружается ЗДЕСЬ</h2>
 * Стор подписан на вход и выход, но подписка начинает работать с момента,
 * когда модуль впервые загрузили. Если человек уже вошёл, а страницу
 * контента открыл только сейчас, перехода «вошёл» больше не случится — и
 * без этой догрузки закладка выглядела бы непоставленной.
 */
function SaveButton({ contentId }: { contentId: number | null }) {
  const { t } = useTranslation();
  const signedIn = useAuthStore((s) => s.isAuthorized);
  const saved = useIsContentSaved(contentId);
  const toggle = useContentFavorites((s) => s.toggle);
  const load = useContentFavorites((s) => s.load);
  const isLoaded = useContentFavorites((s) => s.isLoaded);

  useEffect(() => {
    if (signedIn && !isLoaded) void load();
  }, [signedIn, isLoaded, load]);

  const press = () => {
    if (!signedIn) {
      router.push('/(auth)/sign-in');
      return;
    }
    if (contentId !== null) void toggle(contentId);
  };

  return (
    <Pressable
      onPress={press}
      accessibilityRole="button"
      accessibilityState={{ selected: saved }}
      accessibilityLabel={t(saved ? 'favorites.remove' : 'favorites.add')}
      style={{
        width: 52,
        height: 52,
        borderRadius: radius.card,
        borderWidth: 1,
        borderColor: saved ? colors.purple : colors.border,
      }}
      className="items-center justify-center bg-surface active:opacity-70"
    >
      <Ionicons
        name={saved ? 'bookmark' : 'bookmark-outline'}
        size={20}
        color={saved ? colors.violet : colors.textMuted}
      />
    </Pressable>
  );
}

/**
 * Главное действие страницы.
 *
 * <h2>⚠️ Почему кнопка ГРАДИЕНТНАЯ, хотя все прочие плоские</h2>
 * В `components/ui/Button` записано требование заказчика от 01.09.2026:
 * одна заливка на все кнопки. Макет от 08.09.2026 отменяет его ровно для
 * этой кнопки — на нём она магента → фиолетовый во всю ширину, и это
 * единственный акцент экрана. Общая кнопка при этом НЕ трогается: смена
 * её заливки перекрасила бы всё приложение.
 */
function WatchCta({
  contentId,
  info,
  isMultiPart,
  episodeCount,
  onPlay,
}: {
  contentId: number | null;
  info: WatchInfo | undefined;
  isMultiPart: boolean;
  /**
   * Сколько серий УЖЕ опубликовано. `null` — сервер не сказал (старая
   * сборка без `/content/{id}`), и тогда кнопка ведёт в список как раньше:
   * там свой пустой экран.
   */
  episodeCount: number | null;
  onPlay: () => void;
}) {
  const { t } = useTranslation();

  /**
   * Сериал без единой серии.
   *
   * ⚠️ Раньше кнопка «Tomosha qilish» стояла и здесь — и вела в пустой
   * список. Со стороны это читалось как «видео загружено, но не
   * открывается» (10.09.2026): у сериала видео прикрепляется к СЕРИИ, а
   * не к самому контенту, и пока серий нет, смотреть просто нечего.
   * Честная неактивная плашка вместо кнопки, которая обещает и не делает.
   *
   * Сервер больше не даёт опубликовать сериал без серий
   * (`ContentService.requirePlayableEpisode`), но уже опубликованные
   * такие остались — плашка нужна им.
   */
  if (isMultiPart && episodeCount === 0) {
    return (
      <View
        accessibilityRole="text"
        style={{ borderRadius: radius.card, minHeight: TOUCH_TARGET }}
        className="flex-row items-center justify-center gap-2 border border-border bg-surface px-5 py-3.5"
      >
        <Ionicons name="time-outline" size={18} color={colors.textMuted} />
        <Text className="text-body font-semibold text-text-muted">
          {t('content.episodesSoon')}
        </Text>
      </View>
    );
  }

  // Закрытый контент: что делать, говорит `LockedPanel` — там настоящая
  // цена сервера. Дублировать «смотреть» рядом с замком значило бы обещать
  // то, чего кнопка не сделает.
  if (info && !info.allowed) return null;

  const goToEpisodes = () => {
    // `pushOnce`: быстрые повторные нажатия не открывают список дважды.
    if (contentId !== null) pushOnce(`/episodes/${contentId}`);
  };

  // Подпись одна и та же у фильма и у сериала — так на макете. Разный
  // только знак: у сериала кнопка ведёт в список, а не включает видео.
  const label = t('content.watch');

  return (
    <Pressable
      onPress={isMultiPart ? goToEpisodes : onPlay}
      accessibilityRole="button"
      style={{ borderRadius: radius.card, overflow: 'hidden', minHeight: TOUCH_TARGET }}
      className="active:opacity-80"
    >
      <LinearGradient
        colors={[colors.magenta, colors.purple]}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={{
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
          paddingVertical: 14,
          paddingHorizontal: 20,
        }}
      >
        <Ionicons
          name={isMultiPart ? 'list' : 'play'}
          size={18}
          color={colors.white}
        />
        <Text className="text-body font-semibold text-white">{label}</Text>
      </LinearGradient>
    </Pressable>
  );
}

/**
 * Описание с раскрытием.
 *
 * Полное описание бывает на десять строк и выталкивает актёров и донаты за
 * экран — а на макете они видны почти сразу. Поэтому по умолчанию три
 * строки: остальное по нажатию.
 */
function Synopsis({
  detail,
  contentId,
}: {
  detail: ContentDetail | undefined;
  contentId: number | null;
}) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);

  // Кэш главной — запасной источник: на старой сборке бэкенда описание
  // приходит только оттуда.
  const feed = useHomeFeed();
  const fallback =
    contentId === null
      ? undefined
      : contentCards(feed.data).find((c) => c.id === contentId)?.shortDescription;

  const text = detail?.description ?? detail?.shortDescription ?? fallback ?? null;
  if (!text) return null;

  return (
    <View className="gap-1">
      <Text
        numberOfLines={expanded ? undefined : 3}
        className="text-body text-text-muted"
      >
        {text}
      </Text>

      <Pressable onPress={() => setExpanded((v) => !v)} hitSlop={8} className="self-start">
        <Text className="text-caption text-cyan">
          {expanded ? t('content.less') : t('content.more')}
        </Text>
      </Pressable>
    </View>
  );
}

/** Жанры отдельными плашками — как на макете («Drama», «Melodrama»). */
function GenreTags({ genres }: { genres: string[] }) {
  if (genres.length === 0) return null;

  return (
    <View className="flex-row flex-wrap gap-2">
      {genres.map((g) => (
        <View key={g} className="rounded-pill bg-surface px-3 py-1.5">
          <Text className="text-caption text-text-muted">{g}</Text>
        </View>
      ))}
    </View>
  );
}

/**
 * Кадр с видео.
 *
 * Перенесено из прежнего экрана вместе с разбором сбоев: адрес видео
 * подписан и живёт ограниченное время (билет плейлиста — 6 часов, подпись
 * сегмента — около трёх), поэтому «попробовать ещё раз» здесь означает
 * перезапросить `/watch`, а не перезапустить проигрывание.
 *
 * ⚠️ Повтор РУЧНОЙ. Автоматический бился бы в ту же стену на каждом кадре:
 * причина может быть и в сети, и в снятой подписке.
 */
function Stage({
  contentId,
  detail,
  info,
  onRetry,
}: {
  contentId: number | null;
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
  onRetry: () => Promise<unknown>;
}) {
  const { t } = useTranslation();
  const [part, setPart] = useState(0);
  const [failed, setFailed] = useState(false);
  const [retrying, setRetrying] = useState(false);

  const source: VideoSource | undefined = info?.sources[part];

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

  if (!info || info.sources.length === 0) {
    return (
      <View className="flex-1 justify-center">
        <ScreenState kind="empty" body={t('content.noVideo')} />
      </View>
    );
  }

  if (source && (failed || retrying)) {
    return (
      <View className="flex-1 justify-center">
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

  if (!source) return null;

  return (
    <View className="flex-1 gap-3 pb-3">
      {/* key: смена части пересоздаёт плеер. Признак HLS в ключе —
          чтобы после «потянули вниз» плеер перешёл на появившийся
          `hlsUrl`, а не остался со старым адресом мимо CDN. */}
      <Player
        key={`${source.mediaId ?? part}-${source.hlsUrl ? 'hls' : 'raw'}`}
        source={source}
        orientation={info.orientation}
        contentId={info.contentId}
        episodeId={info.episodeId}
        onError={() => setFailed(true)}
        controls="custom"
        fill
        autoPlay
        title={detail?.title ?? info.title}
        actions={<PlayerActions contentId={contentId} detail={detail} info={info} />}
      />

      {info.sources.length > 1 ? (
        <View className="flex-row flex-wrap justify-center gap-2 px-4">
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
