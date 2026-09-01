import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

import { Badge } from './Badge';

/**
 * Карточка контента: кадр с бейджем в углу и подпись под ним.
 *
 * По умолчанию постер 2:3 — паттерн из Yangi.TV, бейджи по ТЗ
 * (locked / purchased вместо PREMIUM / Bepul).
 *
 * Пропорция вынесена наружу, но во всём приложении она одна
 * (`features/content/railLayout.CARD_RATIO`): ряд, сетка и экран
 * «Barchasi» показывают карточку одного размера — заказчик 01.09.2026.
 */
export type PosterBadge = 'premiere' | 'locked' | 'purchased' | null;

export function PosterCard({
  title,
  subtitle,
  imageUrl,
  badge = null,
  badgeLabel,
  /** Знак в бейдже — пламя на «премьере». */
  badgeIcon,
  /** Таймкод в углу обложки. Уже отформатирован — карточка не считает. */
  duration,
  /** Третья строка: жанр. */
  meta,
  /**
   * Меню карточки — знак «⋮» в правом верхнем углу кадра (макет «Media»).
   *
   * Необязательный: в РЯДАХ на главной меню нет. Там карточка узкая и
   * стоит вплотную к соседней, и знак поверх кадра ловил бы нажатия,
   * которыми люди листают ряд вбок.
   */
  onMenu,
  /** Подпись знака «⋮» для озвучки. Переводит вызывающий — здесь нет i18n. */
  menuLabel,
  width = 132,
  /** Ширина к высоте кадра. См. `features/content/railLayout.CARD_RATIO`. */
  ratio = 2 / 3,
  onPress,
}: {
  title: string;
  subtitle?: string;
  imageUrl?: string;
  badge?: PosterBadge;
  badgeLabel?: string;
  badgeIcon?: keyof typeof Ionicons.glyphMap;
  duration?: string;
  meta?: string;
  onMenu?: () => void;
  menuLabel?: string;
  width?: number;
  ratio?: number;
  onPress?: () => void;
}) {
  return (
    <Pressable style={{ width }} onPress={onPress} className="gap-2 active:opacity-70">
      <View
        style={{ width, height: Math.round(width / ratio) }}
        className="overflow-hidden rounded-card bg-surface-2"
      >
        {imageUrl ? (
          <Image
            source={{ uri: imageUrl }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            transition={200}
          />
        ) : null}

        {/* Таймкод — правый НИЖНИЙ угол обложки, как на макете «Media».
            Сверху там метка доступа, а на макете ещё и меню карточки;
            внизу таймкод никому не мешает и читается привычно, как в
            видеосервисах. Подложка сквозная: под ней виден кадр. */}
        {duration ? (
          <View className="absolute bottom-2 right-2 rounded-pill bg-ink/70 px-2 py-0.5">
            <Text className="text-micro font-semibold text-text">{duration}</Text>
          </View>
        ) : null}

        {onMenu ? (
          // Знак без подложки, как на макете. Тень под ним — чтобы белое
          // не пропадало на светлом кадре: подложка в этом углу спорила бы
          // с меткой доступа слева.
          <Pressable
            onPress={onMenu}
            accessibilityRole="button"
            accessibilityLabel={menuLabel}
            hitSlop={10}
            className="absolute right-1 top-1 h-7 w-7 items-center justify-center active:opacity-60"
          >
            <Ionicons
              name="ellipsis-vertical"
              size={16}
              color={colors.white}
              style={{ textShadowColor: 'rgba(0,0,0,0.65)', textShadowRadius: 6 }}
            />
          </Pressable>
        ) : null}

        {badge && badgeLabel ? (
          // Левый верхний угол — как на макете заказчика. Справа он налезал
          // на лица: у постеров герой обычно смещён вправо.
          <View className="absolute left-2 top-2">
            <Badge tone={badge} icon={badgeIcon} translucent>
              {badgeLabel}
            </Badge>
          </View>
        ) : null}
      </View>

      <View>
        <Text numberOfLines={2} className="text-caption text-text">
          {title}
        </Text>
        {subtitle ? (
          <Text numberOfLines={1} className="text-micro text-text-muted">
            {subtitle}
          </Text>
        ) : null}
        {/* Жанр отдельной строкой и фирменным фиолетовым — на макете
            «Media» это акцент под подписью, а не третий серый уровень. */}
        {meta ? (
          <Text numberOfLines={1} className="text-micro text-violet">
            {meta}
          </Text>
        ) : null}
      </View>
    </Pressable>
  );
}
