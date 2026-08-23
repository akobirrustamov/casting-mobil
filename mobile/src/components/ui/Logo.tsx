import { View } from 'react-native';

import LogoMark from '../../../assets/brand/logo.svg';

/**
 * Знак UzCasting. Векторный — берём SVG, а не PNG:
 * масштабируется без потерь и весит 5 КБ вместо двух мегабайт.
 * PNG-версии используются только там, где вектор невозможен:
 * иконка приложения, splash, favicon.
 */
export function Logo({ size = 40 }: { size?: number }) {
  return (
    <View style={{ width: size, height: size }}>
      <LogoMark width={size} height={size} />
    </View>
  );
}
