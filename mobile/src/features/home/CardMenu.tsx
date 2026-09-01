import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import * as Linking from 'expo-linking';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, Share, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { TOUCH_TARGET, colors } from '@/theme/tokens';

import type { ContentCard } from './types';

/**
 * Меню карточки — то, что открывается знаком «⋮» на постере (макет «Media»
 * от заказчика, 01.09.2026).
 *
 * <h2>Почему именно эти три действия</h2>
 * Знак «⋮» на макете есть, а списка действий к нему нет. Взято только то,
 * что приложение действительно умеет прямо сейчас и без сервера: открыть,
 * поделиться, скопировать ссылку. «Смотреть позже» или «Скрыть» потребовали
 * бы эндпоинта, которого нет, — а пункт меню, который ничего не меняет,
 * хуже отсутствующего.
 *
 * ⚠️ Ссылка — ДИПЛИНК приложения (`uzcasting://content/{id}`), а не адрес
 * сайта: публичной веб-страницы у контента нет. Значит она открывается
 * только там, где приложение установлено. Появится веб-витрина — менять
 * здесь, в одном месте.
 */

/** Через сколько закрыть меню после копирования — успеть прочитать отметку. */
const CLOSE_AFTER_COPY_MS = 900;

export function CardMenu({ card, onClose }: { card: ContentCard | null; onClose: () => void }) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const [copied, setCopied] = useState(false);

  // Отметка «скопировано» живёт до закрытия: следующее открытие меню
  // должно начинаться с обычной подписи, а не с чужого результата.
  useEffect(() => {
    if (!copied) return;
    const timer = setTimeout(() => {
      setCopied(false);
      onClose();
    }, CLOSE_AFTER_COPY_MS);
    return () => clearTimeout(timer);
  }, [copied, onClose]);

  if (!card) return null;

  const title = card.title ?? '';
  const link = Linking.createURL(`/content/${card.id}`);

  const open = () => {
    onClose();
    router.push(`/content/${card.id}`);
  };

  const share = async () => {
    onClose();
    // Отказ от «Поделиться» — это закрытый системный лист, а не ошибка.
    await Share.share({ message: title ? `${title}\n${link}` : link }).catch(() => {});
  };

  const copy = async () => {
    await Clipboard.setStringAsync(link);
    setCopied(true);
  };

  return (
    <Modal visible transparent animationType="fade" onRequestClose={onClose}>
      {/* Нажатие мимо листа закрывает меню — привычнее, чем искать крестик. */}
      <Pressable className="flex-1 justify-end bg-black/60" onPress={onClose}>
        {/* Своё нажатие лист гасит: иначе тап по пункту закрывал бы меню
            фоном раньше, чем сработает сам пункт. */}
        <Pressable
          onPress={() => {}}
          style={{ paddingBottom: Math.max(insets.bottom, 12) + 8 }}
          className="rounded-t-card-lg border-t border-border bg-surface px-4 pt-3"
        >
          <View className="mb-3 h-1 w-10 self-center rounded-pill bg-border" />

          {title ? (
            <Text numberOfLines={1} className="mb-1 text-body font-semibold text-text">
              {title}
            </Text>
          ) : null}

          <MenuRow icon="play-circle-outline" label={t('common.watch')} onPress={open} />
          <MenuRow icon="share-social-outline" label={t('content.share')} onPress={share} />
          <MenuRow
            icon={copied ? 'checkmark-circle-outline' : 'link-outline'}
            label={copied ? t('content.linkCopied') : t('content.copyLink')}
            tint={copied ? colors.success : undefined}
            onPress={copy}
          />
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function MenuRow({
  icon,
  label,
  tint,
  onPress,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  tint?: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      style={{ minHeight: TOUCH_TARGET }}
      className="flex-row items-center gap-3 active:opacity-60"
    >
      <Ionicons name={icon} size={20} color={tint ?? colors.textMuted} />
      <Text className="text-body" style={{ color: tint ?? colors.white }}>
        {label}
      </Text>
    </Pressable>
  );
}
