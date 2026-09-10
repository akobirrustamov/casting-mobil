import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import type { WatchInfo } from '@/features/watch/types';
import { groupDigits } from '@/lib/money';

import coinIcon from '../../../assets/brand/coin.png';
import { colors } from '@/theme/tokens';

import type { ContentDetail, DonationCurrency } from './detail';
import { DonorsSheet } from './DonorsSheet';
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
 * <h2>Три из четырёх плиток НАЖИМАЮТСЯ</h2>
 * Сердце ставит «нравится». Звезда и монета открывают рейтинг «Top 10
 * donatchilar» своей валюты, и кнопка «поддержать» стоит внизу этого
 * окна — требование заказчика от 10.09.2026. Раньше рейтинг и кнопка
 * стояли прямо на странице; см. {@link DonorsSheet}, почему они оттуда
 * ушли.
 *
 * <h2>⚠️ Пустая плитка не рисуется</h2>
 * Старая сборка бэкенда части полей не отдаёт (`null`). Нарисовать вместо
 * них «0» значило бы написать на каждом фильме, что его никто не смотрел и
 * никому он не понравился. `0` ОТ СЕРВЕРА — честный факт и показывается.
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
  const [sheet, setSheet] = useState<DonationCurrency | null>(null);

  const like = useContentLike(contentId, detail, info);

  // ⚠️ Запасной источник — `/watch`. У многосерийного контента он молчит
  // (там нужен номер серии), но у фильма отвечает и на старой сборке
  // бэкенда, где карточки `/content/{id}` ещё нет вовсе.
  const stars = detail?.starsReceived ?? info?.starsReceived ?? null;
  const coins = detail?.coinsReceived ?? info?.coinsReceived ?? null;
  const comments = detail?.commentCount ?? info?.commentCount ?? null;

  const title = detail?.title ?? info?.title ?? null;

  return (
    <>
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
          onPress={() => setSheet('STARS')}
          disabled={contentId === null}
        />
        <Tile
          icon="coin"
          color={colors.textMuted}
          value={coins}
          label={t('content.coins')}
          onPress={() => setSheet('UZCASTING_COIN')}
          disabled={contentId === null}
        />
        {/* ⚠️ Облачко НЕ нажимается: списка комментариев в
            `/api/v1/app/**` пока нет вовсе (есть только счётчик), и
            кнопка вела бы в пустоту. */}
        <Tile
          icon="chatbubble-outline"
          color={colors.violet}
          value={comments}
          label={t('content.comments')}
        />
      </View>

      <DonorsSheet
        open={sheet !== null}
        contentId={contentId}
        title={title}
        currency={sheet ?? 'STARS'}
        onClose={() => setSheet(null)}
      />
    </>
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
        {/* Прочерк, а не «0»: сервер про это число ничего не сказал. */}
        {value === null ? '—' : groupDigits(value)}
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
