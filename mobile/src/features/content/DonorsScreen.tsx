import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FlatList, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenState } from '@/components/states/ScreenState';
import { Screen } from '@/components/ui/Screen';
import { useContentCard } from '@/features/home/api';
import { groupDigits } from '@/lib/money';
import { pushOnce } from '@/lib/navigation';
import { useIsOffline } from '@/lib/network';
import { TOUCH_TARGET, colors, radius } from '@/theme/tokens';

import coinIcon from '../../../assets/brand/coin.png';

import { DonateSheet } from './DonateSheet';
import {
  ContentDetailUnavailableError,
  useContentDonors,
  type DonationCurrency,
  type Donor,
} from './detail';

/**
 * Открыть рейтинг донатов контента.
 *
 * Одна точка входа на все места, откуда сюда ведут: плитки под кнопкой
 * «Tomosha qilish» и значки на кадре плеера. Адрес собирается здесь,
 * чтобы у двух кнопок не разъехался формат параметра.
 */
export function openDonors(contentId: number, currency: DonationCurrency) {
  // `pushOnce`: десять быстрых нажатий — одна страница, а не десять.
  pushOnce(`/donors/${contentId}?currency=${currency}`);
}

/** Параметр адреса → валюта. Всё незнакомое — звёзды: это прежний рейтинг. */
export function parseCurrency(value: unknown): DonationCurrency {
  return value === 'UZCASTING_COIN' ? 'UZCASTING_COIN' : 'STARS';
}

/**
 * «Top 100 donatchilar» — отдельная страница (заказчик, 10.09.2026).
 *
 * <h2>Почему страница, а не окно снизу</h2>
 * Сначала рейтинг открывался шторкой поверх страницы фильма. Заказчик
 * попросил отдельную страницу и сто строк вместо десяти: в шторку сотня
 * не помещается, а листать список внутри шторки, которая сама
 * закрывается движением вниз, неудобно.
 *
 * <h2>Кнопка «Donat qilish» прибита к низу</h2>
 * Так на макете, и не зря: человек листает рейтинг до сотого места, и
 * кнопка, уехавшая вверх вместе со списком, пропала бы как раз тогда,
 * когда он решил поддержать.
 *
 * <h2>Две валюты — переключатель наверху</h2>
 * Звёзды и монеты UZCASTING в один рейтинг не складываются (курс разный,
 * см. `DonationRepo.topSenders`). На страницу приходят с плитки конкретной
 * валюты, но переключиться можно здесь же — не возвращаясь назад.
 */
export function DonorsScreen({
  contentId,
  initialCurrency,
}: {
  contentId: number | null;
  initialCurrency: DonationCurrency;
}) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const isOffline = useIsOffline();
  const card = useContentCard(contentId);

  const [currency, setCurrency] = useState<DonationCurrency>(initialCurrency);
  const [donate, setDonate] = useState(false);

  const donors = useContentDonors(contentId, currency);
  const stars = currency === 'STARS';

  const body = (() => {
    if (donors.isPending) {
      return <ScreenState kind="loading" />;
    }

    if (donors.isError) {
      // ⚠️ Старая сборка бэкенда: адреса рейтинга на ней нет. Писать
      // «ошибка, повторите» бессмысленно — повтор ответит тем же.
      if (donors.error instanceof ContentDetailUnavailableError) {
        return <ScreenState kind="empty" body={t('content.donorsUnavailable')} />;
      }
      return (
        <ScreenState
          kind={isOffline ? 'offline' : 'error'}
          onRetry={() => void donors.refetch()}
        />
      );
    }

    const list = donors.data.donors;

    return (
      <FlatList
        data={list}
        keyExtractor={(d) => String(d.rank)}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 16 }}
        onRefresh={() => void donors.refetch()}
        refreshing={donors.isRefetching}
        ListHeaderComponent={
          <View className="mb-3 flex-row items-center gap-2">
            <Mark stars={stars} size={14} />
            <Text className="text-caption text-text-muted">
              {t(stars ? 'content.starsTotal' : 'content.coinsTotal', {
                amount: groupDigits(donors.data.total),
              })}
            </Text>
          </View>
        }
        ListEmptyComponent={
          // Пустой рейтинг — не ошибка: кнопка внизу остаётся, и быть
          // первым тоже можно.
          <View className="rounded-card bg-surface px-4 py-6">
            <Text className="text-center text-caption text-text-muted">
              {t('content.noDonors')}
            </Text>
          </View>
        }
        renderItem={({ item, index }) => (
          <Row
            donor={item}
            stars={stars}
            first={index === 0}
            last={index === list.length - 1}
          />
        )}
      />
    );
  })();

  return (
    <Screen
      scroll={false}
      title={t('content.topDonorsTitle')}
      subtitle={card?.title ?? undefined}
      underTabBar={false}
      onBack={() => router.back()}
    >
      <View className="flex-row gap-2 px-4 pb-3">
        <CurrencyTab
          label={t('content.stars')}
          stars
          selected={stars}
          onPress={() => setCurrency('STARS')}
        />
        <CurrencyTab
          label={t('content.coins')}
          stars={false}
          selected={!stars}
          onPress={() => setCurrency('UZCASTING_COIN')}
        />
      </View>

      <View className="flex-1">{body}</View>

      <View className="px-4 pt-2" style={{ paddingBottom: Math.max(insets.bottom, 12) }}>
        <Pressable
          onPress={() => setDonate(true)}
          disabled={contentId === null}
          accessibilityRole="button"
          style={{
            borderRadius: radius.card,
            overflow: 'hidden',
            minHeight: TOUCH_TARGET,
          }}
          className="active:opacity-80"
        >
          <LinearGradient
            colors={[colors.magenta, colors.purple]}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 0 }}
            style={{
              flexDirection: 'row',
              alignItems: 'center',
              gap: 10,
              paddingVertical: 14,
              paddingHorizontal: 18,
            }}
          >
            <Ionicons name="heart" size={18} color={colors.white} />
            <Text className="flex-1 text-body font-semibold text-white">
              {t('content.donate')}
            </Text>
            <Ionicons name="chevron-forward" size={18} color={colors.white} />
          </LinearGradient>
        </Pressable>
      </View>

      <DonateSheet
        open={donate}
        contentId={contentId}
        title={card?.title ?? null}
        currency={currency}
        onClose={() => setDonate(false)}
      />
    </Screen>
  );
}

function CurrencyTab({
  label,
  stars,
  selected,
  onPress,
}: {
  label: string;
  stars: boolean;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="tab"
      accessibilityState={{ selected }}
      className={`flex-1 flex-row items-center justify-center gap-2 rounded-pill py-2.5 ${
        selected ? 'bg-purple' : 'bg-surface'
      }`}
    >
      <Mark stars={stars} size={14} color={selected ? colors.white : undefined} />
      <Text
        className={`text-caption ${selected ? 'font-semibold text-white' : 'text-text-muted'}`}
      >
        {label}
      </Text>
    </Pressable>
  );
}

/**
 * Строка рейтинга — как на макете: место, портрет, имя, сумма.
 *
 * Строки лежат одной карточкой: первая скруглена сверху, последняя
 * снизу, между ними тонкая линия. Сто отдельных карточек с зазорами
 * растянули бы список вдвое.
 */
function Row({
  donor,
  stars,
  first,
  last,
}: {
  donor: Donor;
  stars: boolean;
  first: boolean;
  last: boolean;
}) {
  const { t } = useTranslation();
  const medal = MEDALS[donor.rank];

  return (
    <View
      style={{
        borderTopLeftRadius: first ? radius.card : 0,
        borderTopRightRadius: first ? radius.card : 0,
        borderBottomLeftRadius: last ? radius.card : 0,
        borderBottomRightRadius: last ? radius.card : 0,
        borderBottomWidth: last ? 0 : 1,
        borderBottomColor: colors.border,
      }}
      className="flex-row items-center gap-3 bg-surface px-3 py-3"
    >
      <View className="w-7 items-center">
        {medal ? (
          <View
            style={{ backgroundColor: medal }}
            className="h-6 w-6 items-center justify-center rounded-pill"
          >
            <Ionicons name="trophy" size={12} color={colors.ink} />
          </View>
        ) : (
          <Text className="text-caption text-text-muted">{donor.rank}</Text>
        )}
      </View>

      <View className="h-9 w-9 overflow-hidden rounded-pill bg-surface-2">
        {donor.avatarUrl ? (
          <Image
            source={{ uri: donor.avatarUrl }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
          />
        ) : null}
      </View>

      <Text numberOfLines={1} className="flex-1 text-caption text-text">
        {/* Удалённый или безымянный аккаунт остаётся в рейтинге: он
            действительно отправил свои звёзды, и выбросить строку
            значило бы спрятать часть суммы. */}
        {donor.name ?? t('content.anonymousDonor')}
      </Text>

      <Text className="text-caption font-semibold text-text">
        {groupDigits(donor.stars)}
      </Text>
      <Mark stars={stars} size={14} />
    </View>
  );
}

/**
 * Знак валюты.
 *
 * ⚠️ У UZCASTING Coin СВОЙ фирменный знак — картинка, а не значок из
 * набора; цвет даёт `tintColor`, как у соседней звезды.
 */
function Mark({ stars, size, color }: { stars: boolean; size: number; color?: string }) {
  return stars ? (
    <Ionicons name="star" size={size} color={color ?? colors.gold} />
  ) : (
    <Image
      source={coinIcon}
      tintColor={color ?? colors.violet}
      style={{ width: size, height: size }}
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
