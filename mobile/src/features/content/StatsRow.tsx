import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import type { WatchInfo } from '@/features/watch/types';
import { groupDigits } from '@/lib/money';

import coinIcon from '../../../assets/brand/coin.png';
import { colors } from '@/theme/tokens';

import { openComments } from '@/features/comments/CommentsScreen';

import type { ContentDetail } from './detail';
import { openDonors } from './DonorsScreen';
import { useContentLike } from './like';

/**
 * Четыре плитки под кнопкой «Tomosha qilish» (макет заказчика, 08.09.2026).
 *
 * <h2>Что в них стоит и почему именно это</h2>
 * На макете четыре знака: сердце, звезда, стрелка вниз и облачко. Три из
 * них есть в базе — «нравится», подаренные звёзды и комментарии. Четвёртой,
 * «скачиваний», НЕТ: офлайн-загрузок в платформе нет вовсе, и счётчика
 * тоже. Вместо выдуманного числа стоит вторая валюта донатов — монеты
 * UZCASTING, они считаются на самом деле. Просмотры ушли в строку фактов,
 * к году и возрасту: пять плиток в ряд не помещаются.
 *
 * <h2>Все четыре плитки НАЖИМАЮТСЯ</h2>
 * Сердце ставит «нравится». Звезда и монета ведут на страницу «Top 100
 * donatchilar» своей валюты, и кнопка «поддержать» стоит внизу той
 * страницы — требование заказчика от 10.09.2026. Раньше рейтинг и кнопка
 * стояли прямо на странице фильма; см. `DonorsScreen`, почему они ушли.
 * Облачко открывает ленту комментариев (`CommentsScreen`).
 *
 * <h2>Пустое число — «0», а не прочерк (заказчик, 10.09.2026)</h2>
 * Раньше, когда сервер поле не присылал (`null`), плитка писала «—»:
 * мол, сервер ничего не сказал. Заказчик попросил «0» — прочерк на
 * странице читался как поломка.
 *
 * ⚠️ Цена решения: на старой сборке бэкенда, где этих полей нет, «0»
 * стоит и там, где на самом деле что-то есть. Лечится обновлением
 * сервера, а не приложением.
 */
export function StatsRow({
  contentId,
  detail,
  info,
}: {
  contentId: number | null;
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
}) {
  const { t } = useTranslation();

  const like = useContentLike(contentId, detail, info);

  // ⚠️ Запасной источник — `/watch`. У многосерийного контента он молчит
  // (там нужен номер серии), но у фильма отвечает и на старой сборке
  // бэкенда, где карточки `/content/{id}` ещё нет вовсе.
  const stars = detail?.starsReceived ?? info?.starsReceived ?? null;
  const coins = detail?.coinsReceived ?? info?.coinsReceived ?? null;
  const comments = detail?.commentCount ?? info?.commentCount ?? null;

  return (
    <View className="flex-row gap-2">
      <Tile
        icon={like.liked ? 'heart' : 'heart-outline'}
        // Нажатое сердце — фирменной маджентой: серое «нравится»
        // невозможно отличить от ненажатого одним взглядом.
        color={like.liked ? colors.magenta : colors.textMuted}
        value={like.likes}
        label={t('content.likes')}
        onPress={() => void like.toggle()}
        disabled={like.busy || contentId === null}
        selected={like.liked}
      />

      <Tile
        icon="star-outline"
        color={colors.gold}
        value={stars}
        label={t('content.stars')}
        onPress={() => contentId !== null && openDonors(contentId, 'STARS')}
        disabled={contentId === null}
      />
      <Tile
        icon="coin"
        color={colors.textMuted}
        value={coins}
        label={t('content.coins')}
        onPress={() => contentId !== null && openDonors(contentId, 'UZCASTING_COIN')}
        disabled={contentId === null}
      />
      <Tile
        icon="chatbubble-outline"
        color={colors.violet}
        value={comments}
        label={t('content.comments')}
        onPress={() => contentId !== null && openComments(contentId)}
        disabled={contentId === null}
      />
    </View>
  );
}

/**
 * Знак плитки.
 *
 * ⚠️ `'coin'` — не имя из Ionicons, а НАША картинка: у UZCASTING Coin
 * свой фирменный знак, и заменить его значком из набора нельзя.
 */
type Glyph = keyof typeof Ionicons.glyphMap | 'coin';

function Tile({
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
          // ⚠️ Знак залит белым, цвет даёт `tintColor`: тогда он живёт по
          // тем же правилам, что и соседние значки — одна линия, один
          // цвет. Хромированный оригинал рядом с ними выглядел бы
          // наклейкой из чужого приложения, а на 18 пунктах блики
          // превращаются в грязь.
          tintColor={color}
          style={{ width: 18, height: 18 }}
          contentFit="contain"
        />
      ) : (
        <Ionicons name={icon} size={18} color={color} />
      )}
      <Text numberOfLines={1} className="text-caption font-semibold text-text">
        {groupDigits(value ?? 0)}
      </Text>
      <Text numberOfLines={1} className="text-micro text-text-muted">
        {label}
      </Text>
    </>
  );

  if (!onPress) {
    return (
      <View className="flex-1 items-center gap-1 rounded-card bg-surface px-1 py-3">
        {body}
      </View>
    );
  }

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityState={{ selected, busy: disabled }}
      className="flex-1 items-center gap-1 rounded-card bg-surface px-1 py-3 active:opacity-70"
    >
      {body}
    </Pressable>
  );
}
