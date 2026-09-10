import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

import type { WatchInfo } from '@/features/watch/types';
import { groupDigits } from '@/lib/money';
import { colors } from '@/theme/tokens';

import coinIcon from '../../../assets/brand/coin.png';

import { openComments } from '@/features/comments/CommentsScreen';

import type { ContentDetail } from './detail';
import { openDonors } from './DonorsScreen';
import { useContentLike } from './like';

/**
 * Ряд действий НА КАДРЕ — левый нижний угол макета от 10.09.2026.
 *
 * <h2>Зачем те же действия второй раз</h2>
 * Плитки под кнопкой «Tomosha qilish» остаются на странице контента, но
 * страницы во время просмотра не видно: плеер занимает весь экран. А
 * поддержать хочется как раз тогда, когда сцена понравилась, — не после
 * выхода из плеера. Поэтому «нравится», комментарии, звёзды и монеты
 * стоят и здесь.
 *
 * ⚠️ Состояние НЕ дублируется: и плитки, и этот ряд читают один и тот же
 * {@link useContentLike}, а он кладёт ответ сервера в кэш запросов. Иначе
 * сердце на кадре и сердце на странице показывали бы разное.
 */
export function PlayerActions({
  contentId,
  detail,
  info,
}: {
  contentId: number | null;
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
}) {
  const like = useContentLike(contentId, detail, info);

  const stars = detail?.starsReceived ?? info?.starsReceived ?? null;
  const coins = detail?.coinsReceived ?? info?.coinsReceived ?? null;
  const comments = detail?.commentCount ?? info?.commentCount ?? null;

  return (
    <>
      <Action
        icon={like.liked ? 'heart' : 'heart-outline'}
        // На кадре сердце белое, а нажатое — маджентой: серое на тёмном
        // видео не читается вовсе.
        color={like.liked ? colors.magenta : colors.white}
        value={like.likes}
        label="Yoqdi"
        onPress={() => void like.toggle()}
        disabled={like.busy || contentId === null}
        selected={like.liked}
      />

      {/* Облачко ведёт в ленту комментариев. Окно плеера при этом
          прячется само — см. `WatchScreen` в `ContentScreen`. */}
      <Action
        icon="chatbubble-outline"
        color={colors.white}
        value={comments}
        label="Izohlar"
        onPress={() => contentId !== null && openComments(contentId)}
        disabled={contentId === null}
      />

      <Action
        icon="star-outline"
        color={colors.white}
        value={stars}
        label="Yulduzlar"
        onPress={() => contentId !== null && openDonors(contentId, 'STARS')}
        disabled={contentId === null}
      />
      <Action
        icon="coin"
        color={colors.white}
        value={coins}
        label="Uzcasting"
        onPress={() => contentId !== null && openDonors(contentId, 'UZCASTING_COIN')}
        disabled={contentId === null}
      />
    </>
  );
}

type Glyph = keyof typeof Ionicons.glyphMap | 'coin';

function Action({
  icon,
  color,
  value,
  label,
  onPress,
  disabled = false,
  selected = false,
}: {
  icon: Glyph;
  color: string;
  value: number | null;
  label: string;
  onPress?: () => void;
  disabled?: boolean;
  selected?: boolean;
}) {
  const body = (
    <>
      {icon === 'coin' ? (
        <Image
          source={coinIcon}
          tintColor={color}
          style={{ width: 18, height: 18 }}
          contentFit="contain"
        />
      ) : (
        <Ionicons name={icon} size={18} color={color} />
      )}
      {/* Число рядом, а не под знаком: на кадре высоты под вторую строку
          нет — ряд стоит в одной полосе с «на весь экран». Пустое — «0»,
          как и на плитках страницы (заказчик, 10.09.2026). */}
      <Text className="text-micro text-white">{groupDigits(value ?? 0)}</Text>
    </>
  );

  if (!onPress) {
    return <View className="flex-row items-center gap-1">{body}</View>;
  }

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ selected, busy: disabled }}
      hitSlop={10}
      className="flex-row items-center gap-1 active:opacity-60"
    >
      {body}
    </Pressable>
  );
}
