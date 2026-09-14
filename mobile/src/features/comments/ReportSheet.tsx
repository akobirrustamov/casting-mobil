import { Ionicons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '@/theme/tokens';

import { REPORT_REASONS, type ReportReason } from './api';

/**
 * Шторка «Пожаловаться на комментарий».
 *
 * <h2>⚠️ Почему шторка, а не системный диалог</h2>
 * Причин четыре плюс отмена — пять кнопок. `Alert` на Android
 * поддерживает максимум три: два варианта просто не отрисовались бы, и
 * узнали бы мы об этом от пользователя, а не от кода. Шторка ещё и
 * повторяет вид донатов, то есть выглядит как часть приложения.
 *
 * <h2>Что обещает текст</h2>
 * «Посмотрит модератор», а не «удалим». Жалоба ничего не скрывает —
 * решение принимает человек, и обещать иное нечестно.
 */
const LABEL: Record<ReportReason, string> = {
  SPAM: 'comments.reasonSpam',
  INSULT: 'comments.reasonInsult',
  ADULT: 'comments.reasonAdult',
  OTHER: 'comments.reasonOther',
};

const ICON: Record<ReportReason, keyof typeof Ionicons.glyphMap> = {
  SPAM: 'megaphone-outline',
  INSULT: 'sad-outline',
  ADULT: 'eye-off-outline',
  OTHER: 'ellipsis-horizontal',
};

export function ReportSheet({
  open,
  pending,
  onClose,
  onPick,
}: {
  open: boolean;
  /** Запрос в пути — повторное нажатие отправило бы вторую жалобу. */
  pending: boolean;
  onClose: () => void;
  onPick: (reason: ReportReason) => void;
}) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();

  return (
    <Modal
      visible={open}
      transparent
      animationType="slide"
      onRequestClose={onClose}
      statusBarTranslucent
    >
      <Pressable className="flex-1 justify-end bg-black/60" onPress={onClose}>
        {/* Нажатие внутри окна не должно его закрывать. */}
        <Pressable
          onPress={(e) => e.stopPropagation()}
          style={{ paddingBottom: insets.bottom + 16 }}
          className="gap-3 rounded-t-card-lg bg-surface px-4 pt-3"
        >
          <View className="h-1 w-10 self-center rounded-pill bg-border" />

          <View className="gap-1">
            <Text className="text-title font-semibold text-text">
              {t('comments.reportTitle')}
            </Text>
            <Text className="text-caption text-text-muted">
              {t('comments.reportSubtitle')}
            </Text>
          </View>

          <View className="gap-2">
            {REPORT_REASONS.map((reason) => (
              <Pressable
                key={reason}
                disabled={pending}
                onPress={() => onPick(reason)}
                accessibilityRole="button"
                accessibilityLabel={t(LABEL[reason])}
                style={{ opacity: pending ? 0.5 : 1 }}
                className="flex-row items-center gap-3 rounded-card bg-surface-2 px-4 py-3 active:opacity-70"
              >
                <Ionicons name={ICON[reason]} size={18} color={colors.textMuted} />
                <Text className="flex-1 text-body text-text">{t(LABEL[reason])}</Text>
              </Pressable>
            ))}
          </View>

          <Pressable
            onPress={onClose}
            accessibilityRole="button"
            className="h-12 items-center justify-center rounded-card active:opacity-70"
          >
            <Text className="text-body text-text-muted">{t('common.cancel')}</Text>
          </Pressable>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
