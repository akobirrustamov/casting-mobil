import { Ionicons } from '@expo/vector-icons';
import { router, useIsFocused } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { HomeHeaderActions } from '@/components/navigation/HeaderActions';
import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { CategoryTile } from '@/components/ui/CategoryTile';
import { Rail } from '@/components/ui/Rail';
import { Screen } from '@/components/ui/Screen';
import { Skeleton, SkeletonRail } from '@/components/ui/Skeleton';
import { StoryCircle } from '@/components/ui/StoryCircle';
import { Wordmark } from '@/components/ui/Wordmark';
import { CATEGORIES } from '@/features/catalog/categories';
import { CategoryRows } from '@/features/catalog/CategoryRows';
import { useCreators, withPhotos } from '@/features/creators/api';
import { HomeFeedUnavailableError, useHomeFeed } from '@/features/home/api';
import { HomeSectionView } from '@/features/home/sections';
import { ContinueRail } from '@/features/watch/ContinueRail';
import { useIsOffline } from '@/lib/network';
import { colors } from '@/theme/tokens';

/**
 * Главная.
 *
 * <h2>Состав блоков решает сервер</h2>
 * ТЗ §31: «Mobil app bosh sahifani backenddan oladi, homepage hardcoded
 * bo'lmasin». Верхняя часть экрана — это `GET /api/v1/app/home`: какие ряды
 * есть, в каком порядке и как называются, задаёт админ-панель. Добавить ряд
 * или переставить его местами можно без релиза в стор.
 *
 * <h2>Что НЕ приходит из фида</h2>
 * Ниже идут блоки кастинга — это старый продукт на боевом API сайта:
 * 10 направлений (ведут в каталог анкет) и популярные анкеты. Фид про них
 * не знает, поэтому они собраны здесь.
 *
 * Объявления о кастинге — единственные оставшиеся временные данные:
 * эндпоинта для них нет ни в старом API, ни в новом (`src/lib/placeholder.ts`).
 */
export default function HomeScreen() {
  const { t, i18n } = useTranslation();
  const feed = useHomeFeed();
  const creators = useCreators();
  const isOffline = useIsOffline();
  // Вкладка остаётся смонтированной, когда человек ушёл в «Профиль».
  // Без этого рекламная карусель продолжала бы листаться и записывать
  // показы баннерам, которых никто в этот момент не видел.
  const isFocused = useIsFocused();

  const isRu = i18n.language === 'ru';
  const popular = withPhotos(creators.data).slice(0, 12);

  return (
    <Screen
      // Шапка по макету заказчика (01.09.2026): слева знак и «UzCasting»,
      // справа «Premium» и колокольчик. Знак больше не по центру — на
      // макете он прижат к левому краю, как в большинстве витрин.
      titleContent={<Wordmark variant="compact" shine />}
      headerRight={<HomeHeaderActions />}
      onRefresh={() => {
        void feed.refetch();
        void creators.refetch();
      }}
      refreshing={feed.isRefetching || creators.isRefetching}
    >
      {/* Поиск: по ТЗ это строка на главной, а не отдельная вкладка */}
      <Pressable
        onPress={() => router.push('/search')}
        accessibilityRole="button"
        className="flex-row items-center gap-2 rounded-card bg-surface px-4 py-3 active:opacity-70"
      >
        <Ionicons name="search-outline" size={18} color={colors.textMuted} />
        <Text className="text-body text-text-muted">{t('common.search')}</Text>
      </Pressable>

      {/* «Продолжить просмотр» — ПЕРЕД фидом.
          Человек, у которого есть незаконченное видео, чаще всего
          открывает приложение именно ради него. Ниже фида этот ряд
          пришлось бы искать пролистыванием.

          ⚠️ Блока нет совсем, когда продолжать нечего — у нового
          человека он занимал бы верх экрана и не сообщал ничего. */}
      <ContinueRail />

      <HomeFeedBlock feed={feed} isOffline={isOffline} active={isFocused} />

      {/* Разделы каталога контента: «Drama», под ним карточки — такой же
          ряд, как «Podkastlar» из фида. Стоят сразу под фидом, потому что
          это продолжение того же списка контента; блоки кастинга ниже —
          другой продукт и другой бэкенд.

          ⚠️ Не путать со следующим рельсом: там 10 направлений КАСТИНГА
          (анкеты людей), здесь разделы каталога КОНТЕНТА (фильмы). */}
      <CategoryRows />

      <Rail title={t('home.categories')} onSeeAll={() => router.push('/catalog/all')}>
        {CATEGORIES.map((c) => (
          <CategoryTile
            key={c.id}
            title={isRu ? c.titleRu : c.titleUz}
            accent={c.accent}
            icon={c.icon}
            onPress={() => router.push(`/catalog/${c.id}`)}
          />
        ))}
      </Rail>

      {/* Анкеты кастинга — боевой API сайта */}
      <View className="gap-3">
        {creators.isPending ? (
          <View className="gap-3">
            <Text className="text-h2 text-text">{t('home.castingCreators')}</Text>
            {/* Круглые аватары — поэтому и заглушки круглые */}
            <View className="-mx-4">
              <SkeletonRail count={5} width={64} height={64} />
            </View>
          </View>
        ) : creators.isError ? (
          <View className="h-40">
            <ScreenState
              // При пропавшей сети «ошибка» вводит в заблуждение
              kind={isOffline ? 'offline' : 'error'}
              onRetry={() => creators.refetch()}
            />
          </View>
        ) : (
          <Rail
            title={t('home.castingCreators')}
            onSeeAll={() => router.push('/catalog/all')}
          >
            {popular.map((c) => (
              <StoryCircle
                key={c.id}
                name={c.name}
                role={c.age ? t('common.years', { count: c.age }) : undefined}
                imageUrl={c.photoUrls[0]}
                onPress={() => router.push(`/creator/${c.id}`)}
              />
            ))}
          </Rail>
        )}
      </View>

      {/*
        ⚠️ Здесь был ряд «Кастинги» — три ВЫДУМАННЫХ объявления из
        `lib/placeholder`, с городами, сроками подачи и кнопкой
        «откликнуться», у которой не было обработчика.

        К 06.09.2026 все три срока истекли (25.08, 30.08, 02.09), а
        сборка лежала у тестировщиков. Пустой ряд рисовать нельзя —
        заголовок без карточек читается как сломанная загрузка, —
        поэтому блока нет совсем.

        ⚠️ Он и по ТЗ был здесь чужим: §31 требует, чтобы состав
        главной задавал сервер (`GET /api/v1/app/home`). Этот ряд был
        единственным захардкоженным блоком на экране.

        Вернётся, когда появится `GET /api/v1/app/castings`: разметка
        карточек — в истории, в коммите с этим сообщением.
      */}

      {/* Premium CTA — обязательный блок по ТЗ */}
      <View className="gap-2 rounded-card-lg bg-surface p-4">
        <Text className="text-h2 text-gold">{t('home.premiumTitle')}</Text>
        <Text className="text-caption text-text-muted">{t('home.premiumBody')}</Text>
        {/*
          ⚠️ У кнопки не было `onPress` — она молчала на нажатие. Экран
          тарифов при этом существует и открывается из профиля, то есть
          на главной, где стоит сам призыв купить, дороги к нему не было.
        */}
        <Button
          variant="gold"
          className="mt-2 self-start"
          onPress={() => router.push('/subscription/tariffs')}
        >
          {t('home.premiumCta')}
        </Button>
      </View>
    </Screen>
  );
}

/**
 * Серверная часть главной со всеми состояниями.
 *
 * Ошибка фида не уносит весь экран: шапка, поиск и блок Premium остаются
 * на месте. Придумывать премьеры вместо неприехавших нельзя — вместо них
 * состояние с «повторить».
 *
 * ⚠️ Раньше здесь было написано, что «блоки кастинга работают на другом
 * бэкенде и показывают реальные данные». Это было неправдой: объявления
 * лежали в `lib/placeholder`. Комментарий пережил тот код, ради которого
 * писался, и объяснял поведение, которого не было.
 */
function HomeFeedBlock({
  feed,
  isOffline,
  active,
}: {
  feed: ReturnType<typeof useHomeFeed>;
  isOffline: boolean;
  active: boolean;
}) {
  const { t } = useTranslation();

  if (feed.isPending) {
    return (
      <View className="gap-4">
        {/* Скелетон повторяет раскладку: сверху hero, под ним ряд постеров */}
        <Skeleton height={210} radius={22} />
        <View className="-mx-4">
          <SkeletonRail count={3} />
        </View>
      </View>
    );
  }

  if (feed.isError) {
    // Эндпоинта нет на этом сервере — «проверьте соединение» увело бы не туда.
    const unavailable = feed.error instanceof HomeFeedUnavailableError;
    return (
      <View className="h-64">
        <ScreenState
          kind={isOffline ? 'offline' : 'error'}
          title={unavailable ? t('home.feedUnavailableTitle') : undefined}
          body={unavailable ? t('home.feedUnavailableBody') : undefined}
          onRetry={() => feed.refetch()}
        />
      </View>
    );
  }

  if (feed.data.sections.length === 0) {
    return (
      <View className="h-48">
        <ScreenState kind="empty" />
      </View>
    );
  }

  return (
    <View className="gap-4">
      {feed.data.sections.map((section) => (
        <HomeSectionView key={section.id} section={section} active={active} />
      ))}
    </View>
  );
}
