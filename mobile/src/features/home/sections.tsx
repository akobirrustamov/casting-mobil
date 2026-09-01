import { router } from 'expo-router';
import { useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Linking } from 'react-native';

import { HeroCarousel, type HeroItem } from '@/components/ui/HeroCarousel';
import { PosterCard, type PosterBadge } from '@/components/ui/PosterCard';
import { Rail } from '@/components/ui/Rail';
import { StoryCircle } from '@/components/ui/StoryCircle';
import { trackAdClick, trackAdImpression } from '@/features/analytics/api';
import { formatDuration } from '@/features/content/duration';
import { CARD_RATIO, useRailCardWidth } from '@/features/content/railLayout';
import { mediaUrl } from '@/lib/api';

import type { AccessPolicy, BannerCard, ContentCard, HomeSection } from './types';

/**
 * Отрисовка одной секции главной.
 *
 * <h2>Почему разбор идёт по СОДЕРЖИМОМУ, а не по типу секции</h2>
 * Типы секций задаёт бэкенд и админ может включить новый раньше, чем выйдет
 * новая версия приложения в сторе. Если бы выбор шёл через `switch (type)`,
 * каждая новая секция была бы невидимой до релиза — а ТЗ §31 требует ровно
 * обратного: состав главной меняется без релиза. Поэтому решает форма данных
 * (баннеры / категории / креаторы / контент), а `type` влияет только на подачу.
 */

/**
 * Через сколько карусель показывает следующий баннер.
 *
 * Наше решение: ни ТЗ, ни настройки бэкенда интервала не задают. 6 секунд —
 * достаточно, чтобы прочитать заголовок и нажать, и достаточно мало, чтобы
 * третий баннер в очереди вообще кто-то увидел.
 */
const AD_ROTATION_MS = 6_000;

/**
 * Бейдж на постере — из политики доступа.
 *
 * Цены здесь нет намеренно: фид её не отдаёт, она приходит вместе с
 * entitlement из `/api/v1/app/watch/{episodeId}`. Подставить сюда «3 000 so'm»
 * значило бы показать цену, которой у этого контента может и не быть.
 */
function accessBadge(
  policy: AccessPolicy | null
): { tone: PosterBadge; key: string } | null {
  switch (policy) {
    case 'PREMIUM_ONLY':
      return { tone: 'premiere', key: 'common.premium' };
    case 'PURCHASE_ONLY':
    case 'PREMIUM_OR_PURCHASE':
      return { tone: 'locked', key: 'common.locked' };
    case 'FREE':
      return { tone: 'purchased', key: 'common.free' };
    default:
      return null;
  }
}

/**
 * Карточка контента.
 *
 * Форма кадра одна на всё приложение (`railLayout.CARD_RATIO`): ряд, сетка
 * и экран «Barchasi» показывают карточку одного размера. Раньше форму
 * задавал `orientation`, и вертикальный ряд получался другой высоты — по
 * нажатию «Barchasi ›» карточки менялись на глазах.
 */
export function ContentPoster({
  card,
  width,
  ratio,
  onMenu,
}: {
  card: ContentCard;
  width?: number;
  /** Пропорция кадра. По умолчанию — общая для всего приложения. */
  ratio?: number;
  /** Знак «⋮» на кадре. Задаёт только СЕТКА — в рядах меню нет. */
  onMenu?: () => void;
}) {
  const { t } = useTranslation();
  const badge = accessBadge(card.accessPolicy);

  // У многосерийного контента своей длительности нет — там число серий.
  // Показываем то, что сервер действительно знает, а не среднее по палате.
  const subtitle =
    card.episodeCount !== null
      ? t('content.episodeCount', { count: card.episodeCount })
      : (card.shortDescription ?? undefined);

  return (
    <PosterCard
      width={width}
      ratio={ratio ?? CARD_RATIO}
      title={card.title ?? ''}
      subtitle={subtitle}
      meta={card.genre ?? undefined}
      duration={formatDuration(card.durationSeconds) ?? undefined}
      imageUrl={mediaUrl(card.posterMediaId)}
      badge={badge?.tone ?? null}
      badgeLabel={badge ? t(badge.key) : undefined}
      onMenu={onMenu}
      menuLabel={t('common.more')}
      // Экран 17: право на просмотр и цену спрашивает уже он — в фиде их нет.
      onPress={() => router.push(`/content/${card.id}`)}
    />
  );
}

/**
 * Куда ведёт баннер — или `null`, если вести некуда.
 *
 * `INTERNAL` бэкенд умеет нацеливать на семь сущностей, а экраны в приложении
 * есть только под две. Остальные (категория, креатор, кастинг, премьера)
 * возвращают `null`: кнопка, которая ничего не делает, хуже её отсутствия.
 *
 * Экспортируется: чистая функция, по которой проверяются переходы, и та же
 * таблица понадобится обработчику диплинков.
 */
type BannerTarget = { kind: 'external'; url: string } | { kind: 'route'; route: string };

export function bannerTarget(banner: BannerCard): BannerTarget | null {
  if (banner.linkType === 'EXTERNAL') {
    // Только http(s): схемы вроде `tel:` или `intent:` из панели не ждём,
    // а открывать что попало по ссылке из админки не стоит.
    return banner.linkUrl && /^https?:\/\//i.test(banner.linkUrl)
      ? { kind: 'external', url: banner.linkUrl }
      : null;
  }

  if (banner.linkType !== 'INTERNAL' || banner.internalTargetId === null) return null;

  switch (banner.internalTargetType) {
    case 'CONTENT':
      return { kind: 'route', route: `/content/${banner.internalTargetId}` };
    case 'EPISODE':
      return { kind: 'route', route: `/episode/${banner.internalTargetId}` };
    default:
      return null;
  }
}

/**
 * Что написано на бейдже баннера — или `null`, если бейджа нет.
 *
 * <h2>Три разных случая в одном блоке</h2>
 *   - премьера (`NEW_PREMIERES`) — «PREMYERA»;
 *   - платное размещение (`ADVERTISEMENT`) — «Reklama». Выдавать оплаченный
 *     баннер за собственный анонс нельзя;
 *   - собственный анонс платформы (`ADMIN_ANNOUNCEMENT`) — без бейджа.
 *     Заказчик: «bu premyeralar reklamasi, foydalanuvchi bilmasligi kerak».
 *     Платформа не рекламирует себя третьей стороне, и называть свой же
 *     анонс рекламой значит сбивать человека с толку.
 *
 * Отдельная функция, а не тернарник внутри разметки: это правило заказчика,
 * и оно должно проверяться тестом, а не читаться из JSX.
 */
export function bannerBadgeKey(
  banner: BannerCard,
  sectionType: string
): string | null {
  if (sectionType === 'NEW_PREMIERES') return 'common.premiere';
  if (banner.audience === 'ADVERTISEMENT') return 'common.ad';
  return null;
}

/** Подпись кнопки — только если по ней есть куда пойти. */
function bannerCta(banner: BannerCard, fallback: string): string | undefined {
  if (!banner.buttonEnabled) return undefined;
  if (bannerTarget(banner) === null) return undefined;
  return banner.buttonText ?? fallback;
}

function openBanner(banner: BannerCard) {
  const target = bannerTarget(banner);
  if (target === null) return;

  if (target.kind === 'external') {
    void Linking.openURL(target.url);
  } else {
    router.push(target.route);
  }
}

export function HomeSectionView({
  section,
  active = true,
}: {
  section: HomeSection;
  /** Экран на переднем плане — от этого зависят автолистание и счёт показов. */
  active?: boolean;
}) {
  const title = section.title ?? '';
  // Три карточки в кадре — правило всего экрана, а не одного блока
  // (см. `features/content/railLayout`). Хук вызывается до любых
  // ветвлений: иначе он выпадал бы из вызова на секциях с баннерами.
  const cardWidth = useRailCardWidth();

  // ---- баннеры: реклама и премьеры приходят одной формой,
  //      но показываются по-разному (`type` влияет только на подачу)
  if (section.banners.length > 0) {
    if (section.type === 'NEW_PREMIERES') {
      return <PremiereRail section={section} title={title} cardWidth={cardWidth} />;
    }
    return <BannerSection section={section} title={title} active={active} />;
  }

  // ---- категории каталога: ряд плиток НЕ рисуется
  //
  // Заказчик (01.09.2026): «kategoriyalarni o'chirib ber va har bir
  // kategoriyani alohida get qilib ol». Плитка показывала название и не
  // вела никуда — то есть занимала экран, ничего не давая. Теперь каждая
  // категория приходит своим запросом и разворачивается в полноценный ряд
  // с карточками (`features/catalog/CategoryRows`), как «Podkastlar».
  //
  // Секция гасится ЗДЕСЬ, а не фильтром на экране: её рисует любой, кто
  // отдаёт секции фида в `HomeSectionView`, и фильтр на одном экране
  // оставил бы плитки на другом.
  if (section.categories.length > 0) {
    return null;
  }

  // ---- креаторы каталога контента
  if (section.creators.length > 0) {
    return (
      <Rail title={title}>
        {section.creators.map((c) => (
          <StoryCircle
            key={c.id}
            name={c.displayName ?? ''}
            imageUrl={mediaUrl(c.photoMediaId)}
            // ⚠️ Не анкета кастинга: `/creator/{id}` ждёт id старого бэкенда.
          />
        ))}
      </Rail>
    );
  }

  // ---- ряды контента: тип, «Танланган», «Машҳур», ручной ряд
  if (section.content.length > 0) {
    return (
      <Rail
        title={title}
        // «Barchasi ›» открывает РОВНО этот ряд, а не похожую вкладку.
        // Раньше вело на «Media» с подходящим типом, и набор получался
        // другой: ряд «Mini seriallar» — это `MINI_SERIES`, а вкладка
        // «Seriallar» показывает ещё и `SERIES`.
        onSeeAll={() => router.push(`/section/${section.id}`)}
      >
        {section.content.map((card) => (
          <ContentPoster key={card.id} card={card} width={cardWidth} />
        ))}
      </Rail>
    );
  }

  return null;
}

/**
 * Премьеры — ряд постеров, а не большая карусель.
 *
 * Заказчик прислал макет, где «Yangi premyeralar» стоит рядом с остальными
 * рядами: обложка, пламенный бейдж в левом верхнем углу, название и номер
 * серии. Раньше премьеры занимали второй экранный баннер подряд — после
 * рекламного блока это читалось как ещё одна реклама.
 *
 * ⚠️ Цены на карточке нет намеренно. На макете она есть, но заказчик
 * отдельно сказал «faqat button kk emas»: фид цену и не отдаёт — она
 * приходит вместе с правом доступа из `/watch`, уже на экране просмотра.
 */
function PremiereRail({
  section,
  title,
  cardWidth,
}: {
  section: HomeSection;
  title: string;
  cardWidth: number;
}) {
  const { t } = useTranslation();

  const items = section.banners.filter((b) => b.title || b.imageMediaId);
  if (items.length === 0) return null;

  return (
    <Rail
      title={title}
      icon="flame"
      onSeeAll={() => router.push(`/section/${section.id}`)}
    >
      {items.map((b) => (
        <PosterCard
          key={b.id}
          width={cardWidth}
          ratio={CARD_RATIO}
          title={b.title ?? ''}
          subtitle={b.subtitle ?? b.description ?? undefined}
          imageUrl={mediaUrl(b.imageMediaId)}
          badge="premiere"
          badgeIcon="flame"
          badgeLabel={t('common.premiere')}
          onPress={bannerTarget(b) ? () => openBanner(b) : undefined}
        />
      ))}
    </Rail>
  );
}

/**
 * Карусель баннеров: рекламный блок и «Новые премьеры».
 *
 * <h2>Что помечается словом «Реклама», а что нет</h2>
 * В блоке лежат два разных вида баннеров (`AdAudience` на бэкенде):
 *
 *   - `ADVERTISEMENT` — платное размещение. Оно помечено: выдавать оплаченный
 *     баннер за собственный анонс нельзя, это ровно то, от чего защищает
 *     подпись.
 *   - `ADMIN_ANNOUNCEMENT` — анонс самой платформы, чаще всего премьера.
 *     Метки нет: заказчик про эти баннеры написал «bu premyeralar reklamasi,
 *     foydalanuvchi bilmasligi kerak», и он прав — платформа не рекламирует
 *     себя третьей стороне, а называть свой же анонс рекламой сбивает с толку.
 *
 * Раньше метки не было ни у тех, ни у других, то есть оплаченное размещение
 * выглядело точно так же, как анонс.
 *
 * <h2>Порядок и показы</h2>
 * Последовательность баннеров задаёт админ, бэкенд отдаёт её готовой
 * (`sortOrder`, окно показа, аудитория). Карусель листает её сама, а каждый
 * реально показанный кадр отправляет `AD_IMPRESSION` — иначе отчёты по
 * рекламе в панели остаются пустыми не потому, что рекламу не смотрят, а
 * потому, что о показах никто не сообщает.
 */
function BannerSection({
  section,
  title,
  active,
}: {
  section: HomeSection;
  title: string;
  active: boolean;
}) {
  const { t } = useTranslation();

  const isPremiere = section.type === 'NEW_PREMIERES';
  const isAd = section.type === 'ADVERTISEMENT_CAROUSEL';

  const badgeKeyOf = (b: BannerCard) => {
    const key = bannerBadgeKey(b, section.type);
    return key === null ? null : t(key);
  };

  const byId = useMemo(
    () => new Map(section.banners.map((b) => [String(b.id), b])),
    [section.banners]
  );

  const items: HeroItem[] = section.banners
    // Баннер без заголовка и без картинки нечем показать.
    .filter((b) => b.title || b.imageMediaId)
    .map((b) => ({
      id: String(b.id),
      title: b.title ?? title,
      subtitle: b.subtitle ?? b.description ?? undefined,
      badgeLabel: badgeKeyOf(b) ?? undefined,
      ctaLabel: bannerCta(b, t('common.watch')),
      imageUrl: mediaUrl(b.imageMediaId),
      pressable: bannerTarget(b) !== null,
    }));

  const onPress = useCallback(
    (id: string) => {
      const banner = byId.get(id);
      if (!banner) return;
      if (isAd) trackAdClick(banner.id);
      openBanner(banner);
    },
    [byId, isAd]
  );

  const onVisible = useCallback(
    (id: string) => {
      if (!isAd) return;
      const banner = byId.get(id);
      if (banner) trackAdImpression(banner.id);
    },
    [byId, isAd]
  );

  if (items.length === 0) return null;

  return (
    <HeroCarousel
      items={items}
      badgeTone={isPremiere ? 'premiere' : 'info'}
      active={active}
      // Листается только рекламная очередь: у премьер порядок — это витрина,
      // а не оплаченная последовательность, и уезжающий из-под пальца анонс
      // раздражает больше, чем помогает.
      autoAdvanceMs={isAd ? AD_ROTATION_MS : undefined}
      onItemVisible={onVisible}
      onPressItem={onPress}
    />
  );
}
