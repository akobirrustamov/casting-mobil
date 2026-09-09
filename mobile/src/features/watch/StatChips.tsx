import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, Text, View } from 'react-native';

import { useAuthStore } from '@/features/auth/store';
import { groupDigits } from '@/lib/money';
import { colors } from '@/theme/tokens';

import { setLike } from './api';
import type { WatchInfo } from './types';

/**
 * Просмотры и «нравится» под названием (ТЗ §46).
 *
 * Заказчик (07.09.2026): «like bosish va prasmotrlani mobilga qilish
 * kerak».
 *
 * <h2>⚠️ Чего здесь НЕ рисуется</h2>
 * Если сервер не прислал счётчиков — строки нет совсем. Соблазн показать
 * «0 просмотров» велик, но старая сборка бэкенда полей не отдаёт вовсе, и
 * тогда КАЖДЫЙ фильм выглядел бы никем не смотренным. Ноль от сервера —
 * честный факт и показывается; отсутствие поля — не факт.
 *
 * <h2>Гость видит счётчик, но не ставит</h2>
 * Число — часть описания контента, его видят все. Нажатие требует входа:
 * иначе один человек накрутил бы счётчик сколько угодно раз. Гостя ведём
 * на экран входа, а не показываем ошибку.
 */
export function StatChips({ info }: { info: WatchInfo }) {
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

  const views = info.viewCount;
  if (views === null && likes === null) return null;

  return (
    <View className="flex-row items-center gap-4">
      {views !== null ? (
        <View className="flex-row items-center gap-1.5">
          <Ionicons name="eye-outline" size={16} color={colors.textMuted} />
          <Text className="text-caption text-text-muted">{groupDigits(views)}</Text>
        </View>
      ) : null}

      {likes !== null ? (
        <Pressable
          onPress={() => void toggle()}
          disabled={busy || contentId === null}
          accessibilityRole="button"
          accessibilityState={{ selected: liked, busy }}
          accessibilityLabel={t(liked ? 'content.unlike' : 'content.like')}
          hitSlop={10}
          className="flex-row items-center gap-1.5 active:opacity-60"
        >
          <Ionicons
            name={liked ? 'heart' : 'heart-outline'}
            size={16}
            // Нажатое сердце — фирменной маджентой: серое «нравится»
            // невозможно отличить от ненажатого одним взглядом.
            color={liked ? colors.magenta : colors.textMuted}
          />
          <Text className={liked ? 'text-caption text-magenta' : 'text-caption text-text-muted'}>
            {groupDigits(likes)}
          </Text>
        </Pressable>
      ) : null}
    </View>
  );
}
