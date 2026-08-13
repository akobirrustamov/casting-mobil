import { Text, View } from 'react-native';

/**
 * Бейджи из мокапов ТЗ: розовый «ПРЕМЬЕРА», золотой verified,
 * зелёный «куплено», серый locked.
 */
type Tone = 'premiere' | 'verified' | 'purchased' | 'locked' | 'info';

const TONE: Record<Tone, { bg: string; fg: string }> = {
  premiere: { bg: 'bg-magenta', fg: 'text-white' },
  verified: { bg: 'bg-gold', fg: 'text-ink' },
  purchased: { bg: 'bg-success', fg: 'text-ink' },
  locked: { bg: 'bg-surface-2', fg: 'text-text-muted' },
  info: { bg: 'bg-cyan', fg: 'text-ink' },
};

export function Badge({
  children,
  tone = 'info',
  className = '',
}: {
  children: string;
  tone?: Tone;
  className?: string;
}) {
  const { bg, fg } = TONE[tone];
  return (
    <View className={`self-start rounded-pill px-3 py-1 ${bg} ${className}`}>
      <Text className={`text-micro font-bold uppercase ${fg}`}>{children}</Text>
    </View>
  );
}
