import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import * as Linking from 'expo-linking';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, ScrollView, Text, View } from 'react-native';

import { trackAdClick, trackAdImpression } from '@/features/analytics/api';
import { bannerTarget } from '@/features/home/sections';
import type { BannerCard } from '@/features/home/types';
import { mediaUrl } from '@/lib/api';
import { colors } from '@/theme/tokens';

import { useInterstitialAd } from './api';

/**
 * Баннер поверх экрана — «Majburiy reklama» (макет заказчика, 09.09.2026).
 *
 * <h2>Кому он показывается — решает сервер</h2>
 * Коммерческая реклама уходит только тем, у кого нет активного тарифа;
 * объявления админа — всем. Здесь этой развилки НЕТ намеренно: продублируй
 * её клиент — и однажды подписчик увидел бы рекламу, за отсутствие которой
 * заплатил, причём на весь экран.
 *
 * <h2>⚠️ Один раз за запуск приложения</h2>
 * Флаг живёт в модуле, а не в состоянии экрана: вкладка «Media»
 * размонтируется при переходе на другую и смонтируется обратно. Держи
 * счётчик внутри — и человек получал бы баннер при каждом возвращении,
 * то есть по десятку раз за сеанс.
 *
 * ⚠️ Именно «за запуск», а не «за сутки»: чтобы помнить сутки, нужно
 * хранилище, а хранилище — это ещё и «а что показывать после
 * переустановки». Пока заказчик не сказал иначе, одного показа хватает.
 */
let shownThisSession = false;

/** Только для тестов: сбросить «уже показывали». */
export function resetInterstitialForTests(): void {
  shownThisSession = false;
}

export function InterstitialAd() {
  const { t } = useTranslation();
  const { data } = useInterstitialAd();

  const [open, setOpen] = useState(false);

  const banner = data ?? null;

  useEffect(() => {
    if (banner === null || shownThisSession) return;

    shownThisSession = true;
    setOpen(true);
    // Показ считается один раз — тогда же, когда он и происходит.
    trackAdImpression(banner.id);
  }, [banner]);

  if (banner === null || !open) return null;

  const image = mediaUrl(banner.imageMediaId);
  const target = bannerTarget(banner);
  const cta = banner.buttonEnabled && target !== null ? banner.buttonText : null;

  const go = () => {
    if (target === null) return;

    trackAdClick(banner.id);
    setOpen(false);

    if (target.kind === 'external') {
      void Linking.openURL(target.url);
    } else {
      router.push(target.route);
    }
  };

  return (
    <Modal visible transparent animationType="fade" onRequestClose={() => setOpen(false)}>
      <View className="flex-1 items-center justify-center bg-ink/90 px-6">
        {/*
        ⚠️ Высота ограничена, содержимое прокручивается.

        Картинка идёт в пропорции 3:4 от ШИРИНЫ экрана, а под ней ещё
        бейдж, заголовок, подзаголовок и кнопка. На узком телефоне и на
        длинном заголовке карточка перерастала экран, а так как она
        отцентрована — срезало её С ОБЕИХ сторон: сверху уезжал крестик,
        снизу кнопка. Реклама, которую нечем ни закрыть, ни открыть,
        читается как зависшее приложение.
      */}
        <View
          className="w-full overflow-hidden rounded-card-lg bg-surface"
          style={{ maxHeight: '88%' }}
        >
          {/*
            ⚠️ Крестик — первым в разметке и всегда на месте.

            Реклама, которую нечем закрыть, читается как зависшее
            приложение: человек жмёт «назад» и выходит совсем.
          */}
          <Pressable
            onPress={() => setOpen(false)}
            accessibilityRole="button"
            accessibilityLabel={t('common.close')}
            hitSlop={12}
            className="absolute right-3 top-3 z-10 h-9 w-9 items-center justify-center rounded-full bg-ink/70 active:opacity-70"
          >
            <Ionicons name="close" size={20} color={colors.white} />
          </Pressable>

          {/* ⚠️ Крестик ОСТАЁТСЯ снаружи прокрутки — он всегда на месте,
              куда бы человек ни отлистал содержимое. */}
          <ScrollView
            contentContainerStyle={{ flexGrow: 1 }}
            showsVerticalScrollIndicator={false}
            bounces={false}
          >
            {image ? (
              <Image
                source={{ uri: image }}
                style={{ width: '100%', aspectRatio: 3 / 4 }}
                contentFit="cover"
                transition={150}
              />
            ) : null}

            <View className="gap-2 p-4">
              {/*
              Бейдж «Reklama» — только у платного размещения. Собственный
              анонс платформы им не помечается: называть свой же анонс
              рекламой значит сбивать человека с толку (то же правило,
              что и в ленте — `bannerBadgeKey`).
            */}
              {banner.audience === 'ADVERTISEMENT' ? (
                <View className="self-start rounded-pill bg-surface-2 px-2.5 py-1">
                  <Text className="text-micro uppercase text-text-muted">
                    {t('common.ad')}
                  </Text>
                </View>
              ) : null}

              {banner.title ? (
                <Text className="text-h2 text-text">{banner.title}</Text>
              ) : null}
              {banner.subtitle ? (
                <Text className="text-body text-text-muted">{banner.subtitle}</Text>
              ) : null}

              {cta ? (
                <Pressable
                  onPress={go}
                  accessibilityRole="button"
                  accessibilityLabel={cta}
                  className="mt-1 items-center rounded-card bg-purple px-4 py-3 active:opacity-80"
                >
                  <Text className="text-body font-semibold text-white">{cta}</Text>
                </Pressable>
              ) : null}
            </View>
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}
