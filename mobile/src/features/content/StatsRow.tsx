import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { useAuthStore } from '@/features/auth/store';
import { setLike } from '@/features/watch/api';
import type { WatchInfo } from '@/features/watch/types';
import { groupDigits } from '@/lib/money';
import { colors } from '@/theme/tokens';

import type { ContentDetail } from './detail';

/**
 * Четыре плитки под кнопкой «Tomosha qilish» (макет заказчика, 08.09.2026).
 *
 * <h2>Что в них стоит и почему именно это</h2>
 * На макете четыре знака: сердце, звезда, стрелка вниз и облачко. Три из
 * них есть в базе — «нравится», подаренные звёзды и комментарии. Четвёртой,
 * «скачиваний», НЕТ: офлайн-загрузок в платформе нет вовсе, и счётчика
 * тоже. Вместо выдуманного числа стоит просмотры — они считаются на самом
 * деле и на макете тоже присутствуют, только выше.
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

  const views = detail?.viewCount ?? info?.viewCount ?? null;
  const stars = detail?.starsReceived ?? null;
  const comments = detail?.commentCount ?? null;

  return (
    <View className="flex-row gap-2">
      <LikeTile contentId={contentId} detail={detail} info={info} />

      <Tile
        icon="star-outline"
        color={colors.gold}
        value={stars}
        label={t('content.stars')}
      />
      <Tile
        icon="eye-outline"
        color={colors.cyan}
        value={views}
        label={t('content.views')}
      />
      <Tile
        icon="chatbubble-outline"
        color={colors.violet}
        value={comments}
        label={t('content.comments')}
      />
    </View>
  );
}

/**
 * «Нравится» — единственная плитка, на которую нажимают.
 *
 * <h2>Гость видит число, но не ставит</h2>
 * Счётчик — часть описания контента, его видят все. Нажатие требует входа:
 * иначе один человек накрутил бы его сколько угодно раз. Гостя ведём на
 * экран входа, а не показываем ошибку.
 */
function LikeTile({
  contentId,
  detail,
  info,
}: {
  contentId: number | null;
  detail: ContentDetail | undefined;
  info: WatchInfo | undefined;
}) {
  const { t } = useTranslation();
  const signedIn = useAuthStore((s) => s.token !== null);

  /**
   * Наше нажатие поверх серверных данных.
   *
   * ⚠️ Живёт до ухода с экрана и НЕ сбрасывается при повторном запросе:
   * иначе сердце мигало бы обратно на каждом `refetch`, пока сервер не
   * пересчитает. Ответ сервера кладём сюда же — он и есть истина.
   */
  const [own, setOwn] = useState<{ liked: boolean; likeCount: number } | null>(null);
  const [busy, setBusy] = useState(false);

  const liked = own?.liked ?? detail?.liked ?? info?.liked ?? false;
  const likes = own?.likeCount ?? detail?.likeCount ?? info?.likeCount ?? null;

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

  return (
    <Tile
      icon={liked ? 'heart' : 'heart-outline'}
      // Нажатое сердце — фирменной маджентой: серое «нравится» невозможно
      // отличить от ненажатого одним взглядом.
      color={liked ? colors.magenta : colors.textMuted}
      value={likes}
      label={t('content.likes')}
      onPress={() => void toggle()}
      disabled={busy || contentId === null}
      selected={liked}
    />
  );
}

function Tile({
  icon,
  color,
  value,
  label,
  onPress,
  disabled = false,
  selected = false,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  color: string;
  value: number | null;
  label: string;
  onPress?: () => void;
  disabled?: boolean;
  selected?: boolean;
}) {
  const body = (
    <>
      <Ionicons name={icon} size={18} color={color} />
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
