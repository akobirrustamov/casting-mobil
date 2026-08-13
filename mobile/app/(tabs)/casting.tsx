import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { CASTINGS } from '@/lib/placeholder';

/**
 * Лента кастингов. Состав по ТЗ (V3, стр. 15 «14. Casting e'lonlari»):
 * search · location/category filters · deadline · paid/unpaid · save · apply.
 *
 * ⚠️ Данные временные — эндпоинта для объявлений пока нет.
 * TODO: фильтры по городу/категории/возрасту после контракта с бэкендом.
 */
export default function CastingScreen() {
  const { t } = useTranslation();

  return (
    <Screen title={t('casting.title')} subtitle={t('casting.subtitle')}>
      <Pressable className="flex-row items-center gap-2 rounded-card bg-surface px-4 py-3 active:opacity-70">
        <Text className="text-body text-text-muted">⌕</Text>
        <Text className="text-body text-text-muted">{t('common.search')}</Text>
      </Pressable>

      {CASTINGS.map((c) => (
        <View key={c.id} className="gap-3 rounded-card bg-surface p-4">
          <View className="flex-row items-start justify-between gap-3">
            <Text className="flex-1 text-body text-text">{c.title}</Text>
            <Badge tone={c.paid ? 'verified' : 'locked'}>
              {c.paid ? t('casting.paid') : t('casting.unpaid')}
            </Badge>
          </View>

          <Text className="text-caption text-text-muted">
            {c.location} • {t('casting.deadline')}: {c.deadline}
          </Text>

          <View className="flex-row gap-2">
            <Button variant="primary" className="flex-1">
              {t('common.apply')}
            </Button>
            <Button variant="secondary">{t('common.save')}</Button>
          </View>
        </View>
      ))}
    </Screen>
  );
}
