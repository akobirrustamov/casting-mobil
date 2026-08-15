import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { CategoryTile } from '@/components/ui/CategoryTile';
import { HeroCarousel, type HeroItem } from '@/components/ui/HeroCarousel';
import { PosterCard } from '@/components/ui/PosterCard';
import { Rail } from '@/components/ui/Rail';
import { Screen } from '@/components/ui/Screen';
import { StoryCircle } from '@/components/ui/StoryCircle';
import { CATEGORIES } from '@/features/catalog/categories';
import { useCreators, withPhotos } from '@/features/creators/api';
import { CASTINGS, EPISODE_PRICE, PREMIERES } from '@/lib/placeholder';

/**
 * Главная. Состав блоков — строго по ТЗ (V2 стр. 4 «HOME — БОШ САҲИФА»):
 * hero premiere carousel · bugungi premyeralar · mashhur ijodkorlar ·
 * casting e'lonlari · premium CTA · search.
 *
 * Устройство блоков (рельсы с «Barchasini ko'rish», круглые аватары,
 * плитки направлений) — по Yangi.TV, см. docs/STRUCTURE.md.
 *
 * «Mashhur ijodkorlar» работает на боевом API сайта.
 * Премьеры и кастинги — временные данные, эндпоинтов пока нет.
 */
export default function HomeScreen() {
  const { t, i18n } = useTranslation();
  const creators = useCreators();

  const isRu = i18n.language === 'ru';
  const price = t('common.price', { amount: EPISODE_PRICE.toLocaleString('ru-RU') });

  const heroItems: HeroItem[] = PREMIERES.slice(0, 3).map((p) => ({
    id: p.id,
    title: p.title,
    subtitle: `${p.episode} • ${t('home.heroSubtitle')}`,
    badgeLabel: t('common.premiere'),
    ctaLabel: p.purchased ? t('common.watch') : price,
  }));

  const popular = withPhotos(creators.data).slice(0, 12);

  return (
    <Screen title="UzCasting">
      {/* Поиск: по ТЗ это строка на главной, а не отдельная вкладка */}
      <Pressable className="flex-row items-center gap-2 rounded-card bg-surface px-4 py-3 active:opacity-70">
        <Text className="text-body text-text-muted">⌕</Text>
        <Text className="text-body text-text-muted">{t('common.search')}</Text>
      </Pressable>

      <HeroCarousel items={heroItems} />

      <Rail title={t('home.todayPremieres')} onSeeAll={() => {}}>
        {PREMIERES.map((p) => (
          <PosterCard
            key={p.id}
            title={p.title}
            subtitle={p.episode}
            badge={p.purchased ? 'purchased' : 'locked'}
            badgeLabel={p.purchased ? t('common.purchased') : price}
          />
        ))}
      </Rail>

      <Rail title={t('home.categories')} onSeeAll={() => router.push('/catalog/all')}>
        {CATEGORIES.map((c) => (
          <CategoryTile
            key={c.id}
            title={isRu ? c.titleRu : c.titleUz}
            accent={c.accent}
            onPress={() => router.push(`/catalog/${c.id}`)}
          />
        ))}
      </Rail>

      {/* Единственный блок на боевых данных */}
      <View className="gap-3">
        {creators.isPending ? (
          <View className="h-28 items-center justify-center">
            <ScreenState kind="loading" />
          </View>
        ) : creators.isError ? (
          <View className="h-40">
            <ScreenState kind="error" onRetry={() => creators.refetch()} />
          </View>
        ) : (
          <Rail
            title={t('home.popularCreators')}
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

      <Rail title={t('home.castings')} onSeeAll={() => {}}>
        {CASTINGS.map((c) => (
          <View
            key={c.id}
            style={{ width: 240 }}
            className="gap-2 rounded-card bg-surface p-3"
          >
            <Text numberOfLines={2} className="text-body text-text">
              {c.title}
            </Text>
            <Text className="text-caption text-text-muted">
              {c.location} • {t('casting.deadline')}: {c.deadline}
            </Text>
            <Button variant="primary" className="mt-1">
              {t('common.apply')}
            </Button>
          </View>
        ))}
      </Rail>

      {/* Premium CTA — обязательный блок по ТЗ */}
      <View className="gap-2 rounded-card-lg bg-surface p-4">
        <Text className="text-h2 text-gold">{t('home.premiumTitle')}</Text>
        <Text className="text-caption text-text-muted">{t('home.premiumBody')}</Text>
        <Button variant="gold" className="mt-2 self-start">
          {t('home.premiumCta')}
        </Button>
      </View>
    </Screen>
  );
}
