import { Ionicons } from '@expo/vector-icons';
import { useIsFocused } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { pushOnce } from '@/lib/navigation';
import { HomeHeaderActions } from '@/components/navigation/HeaderActions';
import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { Skeleton, SkeletonRail } from '@/components/ui/Skeleton';
import { Wordmark } from '@/components/ui/Wordmark';
// ⚠️ Вместе с блоком ниже: см. «ВРЕМЕННО ОТКЛЮЧЕНО».
// import { CategoryRows } from '@/features/catalog/CategoryRows';
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
 * Ниже фида остался один блок — «Ko'rishda davom eting». Всё остальное
 * заказчик со главной убрал (14.09.2026):
 *
 *   - ряд «Yo'nalishlar» (10 направлений кастинга, вели в каталог анкет);
 *   - ряд «Casting ijodkorlari» (популярные анкеты того же продукта);
 *   - блок «Premiumga o'tish» в самом низу экрана.
 *
 * Дословно: «kerak emas bu yo'nalishlar va casting ijodkorlari va yana
 * home pagedagi eng oxirida premiumga o'tishni olib tashlash».
 *
 * ⚠️ Вместе с двумя первыми с главной ушёл и старый API сайта: запроса
 * анкет (`useCreators`) здесь больше нет. Сам он жив и работает — на него
 * опираются «Saqlanganlar» и экран анкеты.
 *
 * Про Premium это не значит «продавать подписку негде»: плашка в шапке и
 * баннер в «Profil» на месте, оба ведут на тарифы. Ушёл третий призыв,
 * стоявший в конце ленты.
 *
 * <h2>Состав и порядок блоков задан заказчиком</h2>
 * Он не выводится из кода и легко «чинится» обратно, поэтому закреплён
 * тестом — `features/home/__tests__/homeOrder.test.ts`.
 */
export default function HomeScreen() {
  const { t } = useTranslation();
  const feed = useHomeFeed();
  const isOffline = useIsOffline();
  // Вкладка остаётся смонтированной, когда человек ушёл в «Профиль».
  // Без этого рекламная карусель продолжала бы листаться и записывать
  // показы баннерам, которых никто в этот момент не видел.
  const isFocused = useIsFocused();

  return (
    <Screen
      // Шапка по макету заказчика (01.09.2026): слева знак и «UzCasting»,
      // справа «Premium» и колокольчик. Знак больше не по центру — на
      // макете он прижат к левому краю, как в большинстве витрин.
      // Блик только на видимой главной: на Android `MaskedView` с идущей
      // анимацией перерисовывает маску каждый кадр, и в скрытой вкладке
      // это чистая трата.
      titleContent={<Wordmark variant="compact" shine={isFocused} />}
      headerRight={<HomeHeaderActions />}
      onRefresh={() => void feed.refetch()}
      refreshing={feed.isRefetching}
    >
      {/* Поиск: по ТЗ это строка на главной, а не отдельная вкладка */}
      <Pressable
        onPress={() => pushOnce('/search')}
        accessibilityRole="button"
        className="flex-row items-center gap-2 rounded-card bg-surface px-4 py-3 active:opacity-70"
      >
        <Ionicons name="search-outline" size={18} color={colors.textMuted} />
        <Text className="text-body text-text-muted">{t('common.search')}</Text>
      </Pressable>

      <HomeFeedBlock feed={feed} isOffline={isOffline} active={isFocused} />

      {/* ⚠️ ВРЕМЕННО ОТКЛЮЧЕНО (09.09.2026, по просьбе заказчика).
          Возврат — снять комментарий с этой строки и с импорта выше.

          Разделы каталога контента: «Drama», под ним карточки — такой же
          ряд, как «Podkastlar» из фида. Стоят сразу под фидом, потому что
          это продолжение того же списка контента.

          ⚠️ Не путать с рядом «Yo'nalishlar», который стоял здесь до
          14.09.2026: там были 10 направлений КАСТИНГА (анкеты людей),
          здесь — разделы каталога КОНТЕНТА (фильмы).

          Блок сам по себе рабочий: он тянет `/api/v1/app/catalog/categories`
          и по запросу на каждый раздел — карточки. Отключение убирает
          и эти запросы. */}
      {/* <CategoryRows /> */}

      {/* «Ko'rishda davom eting» — В САМОМ НИЗУ.

          Заказчик (07.09.2026, скриншотом со стрелкой): «buni eng
          oxiriga qoyish kk categoriyalardan keyin castingdan oldin». Тех
          двух соседей, между которыми ряд тогда поставили, на экране
          больше нет (14.09.2026) — но смысл просьбы был в другом: верх
          отдан витрине, а незаконченное лежит в конце, куда доходят
          осознанно. Поэтому ряд остался последним.

          ⚠️ Раньше он стоял ПЕРВЫМ, и довод был обратный: кто не
          досмотрел, открывает приложение ради этого. Слово заказчика
          сильнее нашего довода: вернуть наверх — одна строка, но только
          по его просьбе.

          ⚠️ Блока нет совсем, когда продолжать нечего — пустой
          заголовок читался бы как сломанная загрузка. */}
      <ContinueRail />

      {/*
        ⚠️ Здесь стояли ещё три блока, и все три убрал заказчик
        (14.09.2026): ряд анкет «Casting ijodkorlari», ряд направлений
        «Yo'nalishlar» выше и призыв «Premiumga o'tish» в самом конце.

        Ещё раньше отсюда ушёл ряд «Кастинги» — три ВЫДУМАННЫХ объявления
        из `lib/placeholder`, с городами, сроками подачи и кнопкой
        «откликнуться» без обработчика. К 06.09.2026 все три срока
        истекли (25.08, 30.08, 02.09), а сборка лежала у тестировщиков.

        ⚠️ Ряд объявлений и по ТЗ был здесь чужим: §31 требует, чтобы
        состав главной задавал сервер (`GET /api/v1/app/home`), а он был
        единственным захардкоженным блоком на экране. Вернётся, когда
        появится `GET /api/v1/app/castings`: разметка карточек — в
        истории, в коммите с этим сообщением.
      */}
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
