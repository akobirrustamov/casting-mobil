import { Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';

import { BrandLoader } from '@/components/ui/BrandLoader';
import { Button } from '@/components/ui/Button';
import { colors } from '@/theme/tokens';

/**
 * ТЗ требует на КАЖДОМ экране 8 состояний:
 * loading • empty • error • success • locked • purchased • disabled • offline
 *
 * Три из них (success, purchased, disabled) — это не отдельные экраны,
 * а состояния конкретных элементов, поэтому здесь только пять «экранных».
 * Остальные живут в Button (disabled/loading) и Badge (purchased/success).
 */
export type ScreenStateKind = 'loading' | 'empty' | 'error' | 'offline' | 'locked';

type Props = {
  kind: ScreenStateKind;
  /** Переопределяет заголовок из переводов. */
  title?: string;
  /** Переопределяет описание из переводов. */
  body?: string;
  onRetry?: () => void;
  /** CTA для locked — например «Открыть за 5 000 сум». */
  actionLabel?: string;
  onAction?: () => void;
};

const ICON: Record<ScreenStateKind, string> = {
  loading: '',
  empty: '○',
  error: '!',
  offline: '⌁',
  locked: '🔒',
};

const ACCENT: Record<ScreenStateKind, string> = {
  loading: colors.purple,
  empty: colors.textMuted,
  error: colors.danger,
  offline: colors.textMuted,
  locked: colors.gold,
};

export function ScreenState({
  kind,
  title,
  body,
  onRetry,
  actionLabel,
  onAction,
}: Props) {
  const { t } = useTranslation();

  if (kind === 'loading') {
    return (
      /**
       * ⚠️ Без подложки вообще — ни сплошной, ни полупрозрачной.
       *
       * Сначала была сплошная `bg-ink` (тёмная заплата поверх светлой
       * шторки, скриншот заказчика от 10.09.2026), потом `bg-ink/75` —
       * затемнение вместо заплаты. Но и оно видно как прямоугольник
       * поверх свечения экрана, и заказчик прислал такой же скриншот
       * 21.09.2026.
       *
       * Подложка не нужна: состояние не НАКЛАДЫВАЕТСЯ на контент, а
       * заменяет его — под ним всегда фон экрана или шторки.
       *
       * Отступ маленький: у загрузчика уже есть собственное поле под ореол.
       */
      <View className="flex-1 items-center justify-center gap-1">
        <BrandLoader />
        <Text className="text-caption text-text-muted">{t('states.loading')}</Text>
      </View>
    );
  }

  const titles: Record<Exclude<ScreenStateKind, 'loading'>, string> = {
    empty: t('states.emptyTitle'),
    error: t('states.errorTitle'),
    offline: t('states.offlineTitle'),
    locked: t('states.lockedTitle'),
  };
  const bodies: Record<Exclude<ScreenStateKind, 'loading'>, string> = {
    empty: t('states.emptyBody'),
    error: t('states.errorBody'),
    offline: t('states.offlineBody'),
    locked: t('states.lockedBody'),
  };

  return (
    /**
     * ⚠️ БЕЗ своей подложки — прозрачно.
     *
     * Была сплошная `bg-ink`. Экран (`Screen`) рисует под собой мягкое
     * свечение (`GlowBackdrop`), и этот прямоугольник вырезал в нём
     * чёрную заплату: на скриншоте «Xabarlar» от 21.09.2026 видно, где
     * кончается пустое состояние и начинается фон.
     *
     * Своя подложка здесь не нужна вовсе: состояние занимает область
     * контента, а её фон уже нарисован экраном — каким бы он ни был.
     */
    <View className="flex-1 items-center justify-center gap-3 px-8">
      <Text style={{ color: ACCENT[kind] }} className="text-display">
        {ICON[kind]}
      </Text>
      <Text className="text-center text-h2 text-text">{title ?? titles[kind]}</Text>
      <Text className="text-center text-body text-text-muted">
        {body ?? bodies[kind]}
      </Text>

      {onAction && actionLabel ? (
        <Button className="mt-2" variant="premium" onPress={onAction}>
          {actionLabel}
        </Button>
      ) : null}

      {onRetry ? (
        <Button className="mt-2" variant="secondary" onPress={onRetry}>
          {t('states.retry')}
        </Button>
      ) : null}
    </View>
  );
}
