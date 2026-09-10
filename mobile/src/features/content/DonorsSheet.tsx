import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenState } from '@/components/states/ScreenState';
import { groupDigits } from '@/lib/money';
import { TOUCH_TARGET, colors } from '@/theme/tokens';

import coinIcon from '../../../assets/brand/coin.png';

import { DonateSheet } from './DonateSheet';
import { useContentDonors, type DonationCurrency } from './detail';

/**
 * «Top 10 donatchilar» — окно, которое открывает плитка «Yulduzlar» или
 * «Uzcasting» (требование заказчика от 10.09.2026).
 *
 * <h2>Почему рейтинг переехал со страницы в окно</h2>
 * Раньше он стоял внизу страницы контента вместе с кнопкой «Donat
 * qilish». Валюты у платформы две, и в столбик они не складываются —
 * значит на странице пришлось бы держать ДВА одинаковых блока по десять
 * строк, а под ними ещё кнопку. Страница о фильме превращалась бы в
 * страницу о донатах.
 *
 * Теперь у каждой валюты своя плитка с суммой, а список открывается
 * нажатием — по одному окну на валюту, и кнопка «поддержать» стоит там
 * же, где человек только что увидел, кто уже поддержал.
 *
 * <h2>⚠️ Запрос уходит только с ОТКРЫТЫМ окном</h2>
 * Рейтинг — это группировка по всем донатам контента. Спрашивать оба
 * списка при каждом открытии любого фильма значило бы платить за окно,
 * которое чаще всего не откроют (см. `useContentDonors`).
 */
export function DonorsSheet({
  open,
  contentId,
  title,
  currency,
  onClose,
}: {
  open: boolean;
  contentId: number | null;
  /** Название контента — подпись под заголовком окна. */
  title: string | null;
  currency: DonationCurrency;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const [donate, setDonate] = useState(false);

  const donors = useContentDonors(contentId, currency, open);

  const stars = currency === 'STARS';
  const list = donors.data?.donors ?? [];

  /**
   * ⚠️ Список РИСУЕТСЯ, только если сервер ответил.
   *
   * На старой сборке бэкенда этого адреса нет, и запрос падает. Показать
   * тогда «ещё никто не поддержал» значило бы соврать: контент мог
   * собрать миллион звёзд, а человек прочитал бы, что он никому не
   * нужен. Кнопка при этом остаётся — поддержать можно и молча.
   */
  const answered = donors.data !== undefined;

  return (
    <Modal
      visible={open}
      transparent
      animationType="slide"
      onRequestClose={onClose}
      statusBarTranslucent
    >
      <Pressable className="flex-1 justify-end bg-black/60" onPress={onClose}>
        {/* Нажатие внутри окна не должно его закрывать. */}
        <Pressable
          onPress={(e) => e.stopPropagation()}
          style={{ paddingBottom: insets.bottom + 16, maxHeight: '85%' }}
          className="gap-4 rounded-t-card-lg bg-surface px-4 pt-3"
        >
          <View className="h-1 w-10 self-center rounded-pill bg-border" />

          <View className="flex-row items-start gap-2">
            <View className="flex-1">
              <Text className="text-h2 text-text">{t('content.topDonors')}</Text>
              <Text numberOfLines={1} className="mt-1 text-caption text-text-muted">
                {title ?? (stars ? t('content.stars') : t('content.coins'))}
              </Text>
            </View>

            <Pressable
              onPress={onClose}
              accessibilityRole="button"
              hitSlop={12}
              style={{ width: TOUCH_TARGET - 12, height: TOUCH_TARGET - 12 }}
              className="items-center justify-center active:opacity-60"
            >
              <Ionicons name="close" size={22} color={colors.textMuted} />
            </Pressable>
          </View>

          {donors.isPending && open ? (
            <View className="py-8">
              <ScreenState kind="loading" />
            </View>
          ) : null}

          {answered ? (
            <ScrollView
              showsVerticalScrollIndicator={false}
              contentContainerStyle={{ gap: 12 }}
            >
              <View className="flex-row items-center gap-2">
                <Amount stars={stars} />
                <Text className="text-caption text-text-muted">
                  {t(stars ? 'content.starsTotal' : 'content.coinsTotal', {
                    amount: groupDigits(donors.data?.total ?? 0),
                  })}
                </Text>
              </View>

              {list.length === 0 ? (
                <Text className="text-caption text-text-muted">
                  {t('content.noDonors')}
                </Text>
              ) : (
                list.map((d) => (
                  <View key={d.rank} className="flex-row items-center gap-3">
                    <View className="w-6 items-center">
                      {MEDALS[d.rank] ? (
                        <Ionicons name="trophy" size={14} color={MEDALS[d.rank]} />
                      ) : (
                        <Text className="text-caption text-text-muted">{d.rank}</Text>
                      )}
                    </View>

                    <View className="h-8 w-8 overflow-hidden rounded-pill bg-surface-2">
                      {d.avatarUrl ? (
                        <Image
                          source={{ uri: d.avatarUrl }}
                          style={{ width: '100%', height: '100%' }}
                          contentFit="cover"
                        />
                      ) : null}
                    </View>

                    <Text numberOfLines={1} className="flex-1 text-caption text-text">
                      {/* Удалённый или безымянный аккаунт остаётся в
                          рейтинге: он действительно отправил свои
                          звёзды, и выбросить строку значило бы спрятать
                          часть суммы. */}
                      {d.name ?? t('content.anonymousDonor')}
                    </Text>

                    <Text className="text-caption font-semibold text-text">
                      {groupDigits(d.stars)}
                    </Text>
                    <Amount stars={stars} />
                  </View>
                ))
              )}
            </ScrollView>
          ) : null}

          <Pressable
            onPress={() => setDonate(true)}
            accessibilityRole="button"
            className="flex-row items-center justify-center gap-2 rounded-card bg-purple px-5 py-3.5 active:opacity-80"
          >
            <Ionicons name="heart" size={18} color={colors.white} />
            <Text className="text-body font-semibold text-white">
              {t('content.donate')}
            </Text>
          </Pressable>

          <DonateSheet
            open={donate}
            contentId={contentId}
            title={title}
            currency={currency}
            onClose={() => setDonate(false)}
          />
        </Pressable>
      </Pressable>
    </Modal>
  );
}

/** Знак валюты. Монета — наша картинка, у звезды знак есть в наборе. */
function Amount({ stars }: { stars: boolean }) {
  return stars ? (
    <Ionicons name="star" size={13} color={colors.gold} />
  ) : (
    <Image
      source={coinIcon}
      tintColor={colors.textMuted}
      style={{ width: 13, height: 13 }}
      contentFit="contain"
    />
  );
}

/** Цвета первых трёх мест. Остальные — просто номер. */
const MEDALS: Record<number, string> = {
  1: colors.gold,
  2: '#C0C6D8',
  3: '#CD7F32',
};
