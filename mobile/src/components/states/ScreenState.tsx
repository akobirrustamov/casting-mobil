import { ActivityIndicator, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';

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
      <View className="flex-1 items-center justify-center gap-4 bg-ink">
        <ActivityIndicator size="large" color={colors.purple} />
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
    <View className="flex-1 items-center justify-center gap-3 bg-ink px-8">
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
