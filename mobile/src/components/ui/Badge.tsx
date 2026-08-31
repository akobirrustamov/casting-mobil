import { Ionicons } from '@expo/vector-icons';
import { Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

/**
 * Бейджи из мокапов ТЗ: розовый «ПРЕМЬЕРА», золотой verified,
 * зелёный «куплено», серый locked.
 */
export type BadgeTone = 'premiere' | 'verified' | 'purchased' | 'locked' | 'info';
type Tone = BadgeTone;

const TONE: Record<Tone, { bg: string; fg: string; icon: string }> = {
  premiere: { bg: 'bg-magenta', fg: 'text-white', icon: colors.white },
  verified: { bg: 'bg-gold', fg: 'text-ink', icon: colors.ink },
  purchased: { bg: 'bg-success', fg: 'text-ink', icon: colors.ink },
  locked: { bg: 'bg-surface-2', fg: 'text-text-muted', icon: colors.textMuted },
  info: { bg: 'bg-cyan', fg: 'text-ink', icon: colors.ink },
};

/**
 * Полупрозрачные версии тех же тонов.
 *
 * Заказчик про бейдж «REKLAMA» на баннере: «текст сделать полупрозрачным,
 * чтобы немного был виден фон». Плотная заливка вырезала из кадра
 * прямоугольник; сквозь такую подложку кадр читается насквозь, а надпись
 * остаётся разборчивой.
 *
 * Класс, а не `opacity` на всём бейдже: прозрачность нужна ПОДЛОЖКЕ, а
 * текст поверх неё должен остаться плотным.
 */
const TONE_SOFT: Record<Tone, string> = {
  premiere: 'bg-magenta/60',
  verified: 'bg-gold/60',
  purchased: 'bg-success/60',
  locked: 'bg-surface-2/60',
  info: 'bg-cyan/60',
};

export function Badge({
  children,
  tone = 'info',
  /** Знак перед подписью — пламя на «премьере», как на макете. */
  icon,
  /** Подложка сквозная: под бейджем виден кадр. */
  translucent = false,
  className = '',
}: {
  children: string;
  tone?: Tone;
  icon?: keyof typeof Ionicons.glyphMap;
  translucent?: boolean;
  className?: string;
}) {
  const { bg, fg, icon: iconColor } = TONE[tone];

  return (
    <View
      className={`flex-row items-center gap-1 self-start rounded-pill px-3 py-1 ${
        translucent ? TONE_SOFT[tone] : bg
      } ${className}`}
    >
      {icon ? <Ionicons name={icon} size={11} color={iconColor} /> : null}
      <Text className={`text-micro font-bold uppercase ${fg}`}>{children}</Text>
    </View>
  );
}
