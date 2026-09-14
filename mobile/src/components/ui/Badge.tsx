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
    // Размер уменьшен на 20% (заказчик, 14.09.2026: «bularni ham 20%
    // kichraytirish keri» — со скриншотом, где обведены «BEPUL» и «YOPIQ»
    // на обложках). Уменьшены ВСЕ три составляющих разом: буквы 11 → 9,
    // поля 12/4 → 10/2, знак 11 → 9. Сожми мы только текст, бейдж остался
    // бы прежней плашкой с мелкой надписью посередине.
    <View
      className={`flex-row items-center gap-1 self-start rounded-pill px-2.5 py-0.5 ${
        translucent ? TONE_SOFT[tone] : bg
      } ${className}`}
    >
      {icon ? <Ionicons name={icon} size={9} color={iconColor} /> : null}
      {/*
        ⚠️ Прописные делает JS, а НЕ `uppercase` (`textTransform`).

        Заказчик 14.09.2026: «bazi yozuvlar nega bepu yopi bo'lib qolgan».
        На Android при `textTransform` ширину строки успевают померить по
        ИСХОДНОМУ тексту: «Bepul» узнаётся заметно уже, чем «BEPUL», и
        последняя буква не влезала в измеренную ширину. С системным
        шрифтом разница укладывалась в запас, с Manrope — перестала.

        `toUpperCase()` снимает вопрос по построению: меряется и рисуется
        одна и та же строка. `numberOfLines` — чтобы в узком месте слово
        не переносилось на вторую строку, растягивая плашку.
      */}
      <Text numberOfLines={1} className={`text-badge font-bold ${fg}`}>
        {children.toUpperCase()}
      </Text>
    </View>
  );
}
