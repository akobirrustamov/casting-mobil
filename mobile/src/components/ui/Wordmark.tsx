import { Text, View } from 'react-native';

import { Logo } from './Logo';

/**
 * Знак + название в одну строку — «◆ UZCASTING» с макета.
 *
 * Заказчик разрешил ставить логотип вместо названия или рядом с ним;
 * на макете он рядом, поэтому связка едет вместе и не расходится по экранам.
 */
export function Wordmark({
  size = 'md',
}: {
  size?: 'md' | 'lg';
}) {
  const markSize = size === 'lg' ? 30 : 22;
  const textClass = size === 'lg' ? 'text-h1' : 'text-h2';

  return (
    <View className="flex-row items-center gap-2">
      <Logo size={markSize} />
      <Text
        className={`${textClass} text-text`}
        // Разрядка — на макете буквы стоят заметно свободнее обычного
        style={{ letterSpacing: size === 'lg' ? 2.5 : 1.8, fontWeight: '700' }}
      >
        UZCASTING
      </Text>
    </View>
  );
}
