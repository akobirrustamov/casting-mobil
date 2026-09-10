import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import type { TFunction } from 'i18next';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import { useContentCard } from '@/features/home/api';
import { pushOnce } from '@/lib/navigation';
import { useIsOffline } from '@/lib/network';
import { TOUCH_TARGET, colors, radius } from '@/theme/tokens';

import {
  COMMENT_MAX_LENGTH,
  CommentRejectedError,
  CommentsUnavailableError,
  useComments,
  useDeleteComment,
  usePostComment,
  type AppComment,
} from './api';
import { ago } from './time';

/**
 * Открыть комментарии контента.
 *
 * Одна точка входа для плитки «Izohlar» под кнопкой «Tomosha qilish» и
 * облачка на кадре плеера: адрес собирается здесь, чтобы у двух кнопок
 * он не разъехался.
 */
export function openComments(contentId: number) {
  // `pushOnce`: десять быстрых нажатий — одна страница, а не десять.
  pushOnce(`/comments/${contentId}`);
}

/**
 * «Izohlar» — лента комментариев и поле ввода (10.09.2026).
 *
 * <h2>Почему отдельная страница</h2>
 * Как и рейтинг донатов: в шторку поверх страницы не помещается ни
 * длинная лента, ни клавиатура — поле ввода оказалось бы под ней.
 *
 * <h2>Новые — сверху, поле — снизу</h2>
 * Комментарий, отправленный только что, появляется первым: человек видит
 * его сразу, не листая сотню чужих. Поле ввода прибито к низу и уезжает
 * вверх вместе с клавиатурой.
 *
 * <h2>Гость читает, но не пишет</h2>
 * Вместо поля — кнопка входа. Писать без аккаунта нельзя: комментарий
 * подписан именем, и модерации не с кого было бы спросить.
 */
export function CommentsScreen({ contentId }: { contentId: number | null }) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const isOffline = useIsOffline();
  const card = useContentCard(contentId);
  const signedIn = useAuthStore((s) => s.token !== null);

  const comments = useComments(contentId);
  const remove = useDeleteComment(contentId);

  const items = comments.data?.pages.flatMap((p) => p.items) ?? [];
  const total = comments.data?.pages[0]?.totalItems ?? null;

  /**
   * Сервер комментариев не знает (старая сборка бэкенда).
   *
   * ⚠️ Тогда поля ввода нет вовсе. Раньше оно оставалось: человек набирал
   * абзац, жал «отправить» и получал отказ — «почему я не могу оставить
   * комментарий?» (10.09.2026). Причина уже написана в самой ленте.
   */
  const unavailable = comments.error instanceof CommentsUnavailableError;

  /**
   * Уже писал сюда — второй комментарий не пустят (один человек — один
   * комментарий, заказчик 10.09.2026). Вместо поля ввода — объяснение и
   * путь: удалить свой и написать заново.
   */
  const alreadyCommented = comments.data?.pages[0]?.alreadyCommented ?? false;

  const confirmDelete = (comment: AppComment) => {
    Alert.alert(t('comments.deleteConfirmTitle'), comment.text.slice(0, 120), [
      { text: t('common.cancel'), style: 'cancel' },
      {
        text: t('comments.delete'),
        style: 'destructive',
        onPress: () => remove.mutate(comment.id),
      },
    ]);
  };

  const body = (() => {
    if (comments.isPending) return <ScreenState kind="loading" />;

    if (comments.isError) {
      // Старая сборка бэкенда — повтор ответит тем же, «повторить» не
      // предлагаем.
      if (comments.error instanceof CommentsUnavailableError) {
        return <ScreenState kind="empty" body={t('comments.unavailable')} />;
      }
      return (
        <ScreenState
          kind={isOffline ? 'offline' : 'error'}
          onRetry={() => void comments.refetch()}
        />
      );
    }

    return (
      <FlatList
        data={items}
        keyExtractor={(c) => String(c.id)}
        contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 16, gap: 12 }}
        keyboardShouldPersistTaps="handled"
        onRefresh={() => void comments.refetch()}
        refreshing={comments.isRefetching && !comments.isFetchingNextPage}
        onEndReachedThreshold={0.4}
        onEndReached={() => {
          if (comments.hasNextPage && !comments.isFetchingNextPage) {
            void comments.fetchNextPage();
          }
        }}
        ListEmptyComponent={
          <View className="rounded-card bg-surface px-4 py-6">
            <Text className="text-center text-caption text-text-muted">
              {t('comments.empty')}
            </Text>
          </View>
        }
        ListFooterComponent={
          comments.isFetchingNextPage ? (
            <ActivityIndicator color={colors.purple} style={{ marginVertical: 12 }} />
          ) : null
        }
        renderItem={({ item }) => (
          <CommentRow comment={item} onDelete={() => confirmDelete(item)} />
        )}
      />
    );
  })();

  return (
    <Screen
      scroll={false}
      title={total === null ? t('comments.title') : `${t('comments.title')} · ${total}`}
      subtitle={card?.title ?? undefined}
      underTabBar={false}
      onBack={() => router.back()}
    >
      <KeyboardAvoidingView
        className="flex-1"
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={insets.top}
      >
        <View className="flex-1">{body}</View>

        {unavailable ? null : (
          <View
            className="border-t border-border px-4 pt-3"
            style={{ paddingBottom: Math.max(insets.bottom, 12) }}
          >
            {signedIn && alreadyCommented ? (
              <View className="flex-row items-center gap-2 rounded-card bg-surface px-4 py-3">
                <Ionicons
                  name="checkmark-circle-outline"
                  size={18}
                  color={colors.textMuted}
                />
                <Text className="flex-1 text-caption text-text-muted">
                  {t('comments.alreadyCommented')}
                </Text>
              </View>
            ) : signedIn ? (
              <Composer contentId={contentId} />
            ) : (
              <Pressable
                onPress={() => router.push('/(auth)/sign-in')}
                accessibilityRole="button"
                style={{ minHeight: TOUCH_TARGET, borderRadius: radius.card }}
                className="flex-row items-center justify-center gap-2 bg-purple px-5 active:opacity-80"
              >
                <Ionicons name="log-in-outline" size={18} color={colors.white} />
                <Text className="text-body font-semibold text-white">
                  {t('comments.signInToComment')}
                </Text>
              </Pressable>
            )}
          </View>
        )}
      </KeyboardAvoidingView>
    </Screen>
  );
}

/**
 * Поле ввода.
 *
 * ⚠️ Текст стирается ТОЛЬКО после ответа сервера. Сотрёшь сразу — и при
 * обрыве сети человек потеряет абзац, который набирал минуту.
 */
function Composer({ contentId }: { contentId: number | null }) {
  const { t } = useTranslation();
  const [text, setText] = useState('');
  const send = usePostComment(contentId);

  const clean = text.trim();
  const canSend = clean.length > 0 && !send.isPending && contentId !== null;

  const submit = () => {
    if (!canSend) return;
    send.mutate(clean, { onSuccess: () => setText('') });
  };

  const error = send.error ? errorText(send.error, t) : null;

  return (
    <View className="gap-2">
      <View className="flex-row items-end gap-2">
        <TextInput
          value={text}
          onChangeText={(v) => {
            setText(v);
            if (send.isError) send.reset();
          }}
          placeholder={t('comments.placeholder')}
          placeholderTextColor={colors.textDisabled}
          multiline
          maxLength={COMMENT_MAX_LENGTH}
          className="flex-1 rounded-card bg-surface px-4 py-3 text-body"
          style={{ color: colors.white, maxHeight: 120 }}
        />
        <Pressable
          onPress={submit}
          disabled={!canSend}
          accessibilityRole="button"
          accessibilityLabel={t('comments.send')}
          style={{ width: TOUCH_TARGET, height: TOUCH_TARGET }}
          className={`items-center justify-center rounded-pill ${
            canSend ? 'bg-purple' : 'bg-surface'
          }`}
        >
          {send.isPending ? (
            <ActivityIndicator color={colors.white} />
          ) : (
            <Ionicons
              name="send"
              size={18}
              color={canSend ? colors.white : colors.textDisabled}
            />
          )}
        </Pressable>
      </View>

      {error ? <Text className="text-micro text-danger">{error}</Text> : null}
    </View>
  );
}

/** Отказ сервера → текст под полем. Свой текст сервера важнее общего. */
function errorText(error: unknown, t: TFunction): string {
  if (error instanceof CommentRejectedError) {
    if (error.reason === 'blocked') return t('comments.blocked');
    if (error.reason === 'duplicate') return t('comments.alreadyCommented');
    if (error.reason === 'signIn') return t('comments.signInToComment');
    return error.message || t('comments.sendFailed');
  }
  if (error instanceof CommentsUnavailableError) return t('comments.unavailable');
  return t('comments.sendFailed');
}

function CommentRow({
  comment,
  onDelete,
}: {
  comment: AppComment;
  onDelete: () => void;
}) {
  const { t } = useTranslation();
  const when = ago(comment.createdAt);

  const name = comment.authorName ?? t('comments.anonymous');

  return (
    <View className={`flex-row gap-3 ${comment.hidden ? 'opacity-60' : ''}`}>
      <View className="h-9 w-9 items-center justify-center overflow-hidden rounded-pill bg-surface-2">
        {comment.authorAvatarUrl ? (
          <Image
            source={{ uri: comment.authorAvatarUrl }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
          />
        ) : (
          // Без фото — первая буква имени: пустой кружок у половины ленты
          // выглядел бы как недогруженные картинки.
          <Text className="text-caption font-semibold text-text-muted">
            {name.charAt(0).toUpperCase()}
          </Text>
        )}
      </View>

      <View className="flex-1 gap-1">
        <View className="flex-row items-center gap-2">
          <Text numberOfLines={1} className="shrink text-caption font-semibold text-text">
            {name}
          </Text>
          {when ? (
            <Text className="text-micro text-text-disabled">
              {when.kind === 'date'
                ? when.text
                : when.kind === 'now'
                  ? t('comments.justNow')
                  : t(`comments.${when.kind}Ago`, { count: when.count })}
            </Text>
          ) : null}
        </View>

        <Text className="text-body text-text">{comment.text}</Text>

        {comment.hidden ? (
          <Text className="text-micro text-text-muted">{t('comments.hidden')}</Text>
        ) : null}
      </View>

      {comment.mine ? (
        <Pressable
          onPress={onDelete}
          accessibilityRole="button"
          accessibilityLabel={t('comments.delete')}
          hitSlop={10}
          className="pt-0.5 active:opacity-60"
        >
          <Ionicons name="trash-outline" size={16} color={colors.textMuted} />
        </Pressable>
      ) : null}
    </View>
  );
}
