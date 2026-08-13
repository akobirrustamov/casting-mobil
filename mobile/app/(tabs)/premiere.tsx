import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { PosterCard } from '@/components/ui/PosterCard';
import { Screen } from '@/components/ui/Screen';
import { EPISODE_PRICE, PREMIERES } from '@/lib/placeholder';

/**
 * Каталог премьер. По ТЗ (V3, стр. 17 «16. Premyera katalogi»):
 * табы Seriallar/Ko'rsatuvlar/Filmlar · premiere badge · price ·
 * exclusive status · trailer · episode list.
 *
 * Сегментированные табы + сетка постеров — паттерн Yangi.TV.
 * ⚠️ Данные временные. Разделение по типам контента заработает,
 * когда появится поле типа в API.
 */
const TAB_KEYS = ['tabsAll', 'tabsSeries', 'tabsShows', 'tabsMovies'] as const;

export default function PremiereScreen() {
  const { t } = useTranslation();
  const [active, setActive] = useState(0);

  const price = t('common.price', { amount: EPISODE_PRICE.toLocaleString('ru-RU') });

  return (
    <Screen title={t('premiere.title')} subtitle={t('premiere.subtitle')}>
      <View className="flex-row gap-2">
        {TAB_KEYS.map((key, i) => (
          <Pressable
            key={key}
            onPress={() => setActive(i)}
            className={`rounded-pill px-4 py-2 ${
              i === active ? 'bg-purple' : 'bg-surface'
            }`}
          >
            <Text
              className={`text-caption ${
                i === active ? 'font-semibold text-white' : 'text-text-muted'
              }`}
            >
              {t(`premiere.${key}`)}
            </Text>
          </Pressable>
        ))}
      </View>

      <View className="flex-row flex-wrap justify-between gap-y-4">
        {PREMIERES.map((p) => (
          <PosterCard
            key={p.id}
            width={158}
            title={p.title}
            subtitle={p.episode}
            badge={p.purchased ? 'purchased' : 'locked'}
            badgeLabel={p.purchased ? t('common.purchased') : price}
          />
        ))}
      </View>
    </Screen>
  );
}
