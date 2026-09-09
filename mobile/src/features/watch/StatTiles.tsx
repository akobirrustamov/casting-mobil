import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { useAuthStore } from '@/features/auth/store';
import { groupDigits } from '@/lib/money';
import { colors } from '@/theme/tokens';

import coinIcon from '../../../assets/brand/coin.png';

import { setLike } from './api';
import type { WatchInfo } from './types';

/**
 * Ряд показателей под кнопкой просмотра — по референсу заказчика
 * (09.09.2026): тёмные квадраты со знаком, подписью и числом.
 *
 * <h2>⚠️ Чего здесь НЕ рисуется</h2>
 * Плитка появляется только если сервер прислал число. Соблазн показать
 * «0» велик, но старая сборка бэкенда полей не отдаёт вовсе — и тогда
 * КАЖДЫЙ фильм выглядел бы нулевым. Ноль от сервера — честный факт и
 * показывается; отсутствие поля — не факт.
 *
 * <h2>Гость видит числа, но не ставит</h2>
 * Счётчики — часть описания контента, их видят все. Нажатие требует
 * входа: иначе один человек накрутил бы «нравится» сколько угодно раз.
 * Гостя ведём на экран входа, а не показываем ошибку.
 *
 * <h2>⚠️ Звёзды и монеты — РАЗНЫЕ плитки</h2>
 * Это разные единицы, и складывать их в одно число нельзя: сумма ничего
 * не значит. На референсе они тоже стоят порознь, а на бэкенде то же
 * правило охраняет тест отчётов.
 *
 * ⚠️ У монет, в отличие от звёзд, готового счётчика у контента нет — их
 * сумма собирается по донатам. Поэтому число может прийти `null`, и
 * тогда плитки просто не будет.
 */
export function StatTiles({ info }: { info: WatchInfo }) {
  const { t } = useTranslation();
  const signedIn = useAuthStore((s) => s.token !== null);
  const contentId = info.contentId;

  /**
   * Наше нажатие поверх серверных данных.
   *
   * ⚠️ Живёт до ухода с экрана и НЕ сбрасывается при повторном запросе:
   * иначе сердце мигало бы обратно на каждом `refetch`, пока сервер не
   * пересчитает. Ответ сервера кладём сюда же — он и есть истина.
   */
  const [own, setOwn] = useState<{ liked: boolean; likeCount: number } | null>(null);
  const [busy, setBusy] = useState(false);

  const liked = own?.liked ?? info.liked;
  const likes = own?.likeCount ?? info.likeCount;

  const toggle = useCallback(async () => {
    if (contentId === null || busy) return;

    if (!signedIn) {
      router.push('/(auth)/sign-in');
      return;
    }

    const next = !liked;
    const base = likes ?? 0;

    // Сердце откликается сразу: ждать ответа сети — значит показать
    // человеку, что кнопка «не нажалась».
    setOwn({ liked: next, likeCount: Math.max(0, base + (next ? 1 : -1)) });
    setBusy(true);
    try {
      setOwn(await setLike(contentId, next));
    } catch {
      // Не получилось — возвращаем как было. Ошибку не показываем:
      // «нравится» не то действие, ради которого стоит закрывать экран
      // сообщением.
      setOwn(null);
    } finally {
      setBusy(false);
    }
  }, [busy, contentId, liked, likes, signedIn]);

  const tiles = [
    likes === null
      ? null
      : {
          key: 'like',
          icon: liked ? ('heart' as const) : ('heart-outline' as const),
          tint: liked ? colors.magenta : colors.textMuted,
          label: t('content.likes'),
          value: likes,
          onPress: () => void toggle(),
        },
    info.starsReceived === null
      ? null
      : {
          key: 'stars',
          icon: 'star-outline' as const,
          tint: colors.gold,
          label: t('content.stars'),
          value: info.starsReceived,
          onPress: undefined,
        },
    info.coinsReceived === null
      ? null
      : {
          key: 'coins',
          icon: 'coin' as const,
          tint: colors.textMuted,
          label: t('content.coins'),
          value: info.coinsReceived,
          onPress: undefined,
        },
    info.commentCount === null
      ? null
      : {
          key: 'comments',
          icon: 'chatbubble-outline' as const,
          tint: colors.textMuted,
          label: t('content.comments'),
          value: info.commentCount,
          onPress: undefined,
        },
  ].filter((tile): tile is NonNullable<typeof tile> => tile !== null);

  if (tiles.length === 0) return null;

  return (
    <View className="flex-row gap-2">
      {tiles.map((tile) => (
        <Tile
          key={tile.key}
          icon={tile.icon}
          tint={tile.tint}
          label={tile.label}
          value={tile.value}
          onPress={tile.onPress}
          busy={busy && tile.key === 'like'}
        />
      ))}
    </View>
  );
}

/**
 * Знак плитки.
 *
 * ⚠️ `'coin'` — не имя из Ionicons, а НАША картинка: у UZCASTING Coin
 * свой фирменный знак, и подобрать ему замену из набора нельзя.
 * Отдельным полем это не сделано намеренно — тогда у каждой плитки было
 * бы два взаимоисключающих свойства, и однажды заполнили бы оба.
 */
type Glyph = keyof typeof Ionicons.glyphMap | 'coin';

/**
 * Одна плитка.
 *
 * ⚠️ `flex-1` — плитки делят ширину поровну, как на референсе. Ширина по
 * содержимому развалила бы ряд: «586 ta» шире, чем «0».
 */
function Tile({
  icon,
  tint,
  label,
  value,
  onPress,
  busy,
}: {
  icon: Glyph;
  tint: string;
  label: string;
  value: number;
  onPress?: () => void;
  busy: boolean;
}) {
  const body = (
    <>
      {icon === 'coin' ? (
        <Image
          source={coinIcon}
          // ⚠️ Знак залит белым, цвет даёт `tintColor` — тогда он живёт
          // по тем же правилам, что и соседние значки: одна линия, один
          // цвет. Иначе хромированный оригинал выглядел бы наклейкой из
          // чужого приложения.
          tintColor={tint}
          style={{ width: 20, height: 20 }}
          contentFit="contain"
        />
      ) : (
        <Ionicons name={icon} size={20} color={tint} />
      )}
      <Text className="text-caption text-text-muted" numberOfLines={1}>
        {label}
      </Text>
      <Text className="text-body font-semibold text-text">{groupDigits(value)}</Text>
    </>
  );

  const className = 'flex-1 items-center gap-1 rounded-card bg-surface px-2 py-3';

  // Неинтерактивная плитка не должна отзываться на нажатие: подсветка
  // без действия читается как сломанная кнопка.
  if (!onPress) {
    return <View className={className}>{body}</View>;
  }

  return (
    <Pressable
      onPress={onPress}
      disabled={busy}
      accessibilityRole="button"
      accessibilityLabel={label}
      className={`${className} active:opacity-60`}
    >
      {body}
    </Pressable>
  );
}
