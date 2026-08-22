import { useTranslation } from 'react-i18next';
import { Linking } from 'react-native';

import { CategoryTile } from '@/components/ui/CategoryTile';
import { HeroCarousel, type HeroItem } from '@/components/ui/HeroCarousel';
import { PosterCard, type PosterBadge } from '@/components/ui/PosterCard';
import { Rail } from '@/components/ui/Rail';
import { StoryCircle } from '@/components/ui/StoryCircle';
import { mediaUrl } from '@/lib/api';
import { colors } from '@/theme/tokens';

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

/** Плитки категорий приходят без цвета — акцент подбираем по позиции. */
const TILE_ACCENTS = [colors.purple, colors.magenta, colors.cyan, colors.gold];

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

export function ContentPoster({
  card,
  width,
}: {
  card: ContentCard;
  width?: number;
}) {
  const { t } = useTranslation();
  const badge = accessBadge(card.accessPolicy);

  return (
    <PosterCard
      width={width}
      title={card.title ?? ''}
      subtitle={card.shortDescription ?? undefined}
      imageUrl={mediaUrl(card.posterMediaId)}
      badge={badge?.tone ?? null}
      badgeLabel={badge ? t(badge.key) : undefined}
      // TODO: экран 17 «Episode detail» (ТЗ V3) — переход появится вместе с ним.
      // До тех пор карточка не кликается: тап, который ничего не делает, хуже,
      // чем его отсутствие.
    />
  );
}

/**
 * Подпись кнопки баннера — только если по ней есть куда пойти.
 *
 * `INTERNAL` ведёт на экраны, которых ещё нет (контент, эпизод, кастинг),
 * поэтому такая кнопка не рисуется вовсе, а не рисуется мёртвой.
 */
function bannerCta(banner: BannerCard, fallback: string): string | undefined {
  if (!banner.buttonEnabled) return undefined;
  if (banner.linkType !== 'EXTERNAL') return undefined;
  if (!banner.linkUrl || !/^https?:\/\//i.test(banner.linkUrl)) return undefined;
  return banner.buttonText ?? fallback;
}

function openBanner(banner: BannerCard) {
  if (banner.linkType === 'EXTERNAL' && banner.linkUrl) {
    void Linking.openURL(banner.linkUrl);
  }
}

export function HomeSectionView({ section }: { section: HomeSection }) {
  const { t } = useTranslation();
  const title = section.title ?? '';

  // ---- баннеры: реклама и премьеры приходят одной формой
  if (section.banners.length > 0) {
    const isPremiere = section.type === 'NEW_PREMIERES';

    const items: HeroItem[] = section.banners
      // Баннер без заголовка и без картинки нечем показать.
      .filter((b) => b.title || b.imageMediaId)
      .map((b) => ({
        id: String(b.id),
        title: b.title ?? title,
        subtitle: b.subtitle ?? b.description ?? undefined,
        badgeLabel: isPremiere ? t('common.premiere') : undefined,
        ctaLabel: bannerCta(b, t('common.watch')),
        imageUrl: mediaUrl(b.imageMediaId),
      }));

    if (items.length === 0) return null;

    const byId = new Map(section.banners.map((b) => [String(b.id), b]));

    return (
      <HeroCarousel
        items={items}
        badgeTone={isPremiere ? 'premiere' : 'info'}
        onPressItem={(item) => {
          const banner = byId.get(item.id);
          if (banner) openBanner(banner);
        }}
      />
    );
  }

  // ---- категории каталога
  if (section.categories.length > 0) {
    return (
      <Rail title={title}>
        {section.categories.map((c, i) => (
          <CategoryTile
            key={c.id}
            title={c.name ?? ''}
            accent={TILE_ACCENTS[i % TILE_ACCENTS.length]}
            imageUrl={mediaUrl(c.iconMediaId)}
            // ⚠️ Это разделы каталога контента («O'zbek kinosi», «Bolalar uchun»),
            // а не 10 направлений кастинга из `features/catalog/categories`.
            // Переход на `/catalog/{id}` открыл бы совсем другую сущность,
            // поэтому его здесь нет — он появится с экраном каталога контента.
          />
        ))}
      </Rail>
    );
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
      <Rail title={title}>
        {section.content.map((card) => (
          <ContentPoster key={card.id} card={card} />
        ))}
      </Rail>
    );
  }

  return null;
}
