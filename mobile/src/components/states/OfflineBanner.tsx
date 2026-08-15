import { Ionicons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useIsOffline } from '@/lib/network';
import { colors } from '@/theme/tokens';

/**
 * Полоса «нет интернета» поверх всего приложения.
 *
 * Именно баннер, а не подмена экрана: приложение при пропаже сети остаётся
 * рабочим — уже загруженные списки лежат в кэше TanStack Query и продолжают
 * листаться. Подменять их заглушкой было бы шагом назад.
 */
export function OfflineBanner() {
  const isOffline = useIsOffline();
  const insets = useSafeAreaInsets();
  const { t } = useTranslation();

  if (!isOffline) return null;

  return (
    <View
      pointerEvents="none"
      className="absolute left-0 right-0 top-0 z-50 flex-row items-center justify-center gap-2 px-4 pb-2"
      style={{ paddingTop: insets.top + 4, backgroundColor: colors.surface2 }}
    >
      <Ionicons name="cloud-offline-outline" size={16} color={colors.danger} />
      <Text className="text-caption text-text">{t('states.offlineTitle')}</Text>
    </View>
  );
}
