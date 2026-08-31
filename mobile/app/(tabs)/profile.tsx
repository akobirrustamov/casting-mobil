import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import Constants from 'expo-constants';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Linking, Pressable, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { GlowCard } from '@/components/ui/GlowCard';
import { LanguageSwitcher } from '@/components/ui/LanguageSwitcher';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import {
  formatDate,
  premiumState,
  useBalance,
  useMe,
} from '@/features/profile/api';
import { LANGUAGE_LABELS, isSupportedLanguage, type Language } from '@/i18n';
import { colors, gradients, radius } from '@/theme/tokens';

/**
 * Аккаунт — экран 21, раскладка с макета заказчика «Screen 4».
 *
 * <h2>Откуда три числа</h2>
 * Все три — из `GET /api/v1/app/donations/balance`: сумы, Yulduzlar и
 * Uzcasting. Ни одно не выдумано; пока запрос не ответил, стоит прочерк.
 *
 * ⚠️ Сумовый баланс сначала показывался прочерком: поле в `UserBalance`
 * было, но DTO его не отдавал, и я ошибочно решил, что его нет в модели.
 *
 * <h2>Пункты без экранов не притворяются рабочими</h2>
 * Из списка на макете сегодня существуют «Sevimlilarim», язык и выход.
 * У остальных вместо шеврона стоит метка «Tez orada», и они не нажимаются:
 * ряд, который выглядит кликабельным и молчит, читается как поломка.
 *
 * <h2>Шестерёнки в шапке нет</h2>
 * На макете она рядом с колокольчиком, но ведёт туда же, куда вторая
 * группа списка на этом же экране (профиль, безопасность, уведомления,
 * язык). Появится вместе с отдельным экраном настроек.
 */
type Row = {
  key: string;
  label: string;
  hint?: string;
  icon: keyof typeof Ionicons.glyphMap;
  /** Что происходит по нажатию. Без него ряд помечается «Tez orada». */
  onPress?: () => void;
  /** Значение справа вместо шеврона — язык, версия. */
  value?: string;
  danger?: boolean;
};

/** Ряд соцсетей внизу профиля — как у Yangi.TV. Ссылки уточняются у заказчика. */
const SOCIALS: { key: string; icon: keyof typeof Ionicons.glyphMap; url: string }[] = [
  { key: 'telegram', icon: 'paper-plane-outline', url: 'https://t.me/uzcasting' },
  { key: 'instagram', icon: 'logo-instagram', url: 'https://instagram.com/uzcasting' },
  { key: 'youtube', icon: 'logo-youtube', url: 'https://youtube.com/@uzcasting' },
  { key: 'facebook', icon: 'logo-facebook', url: 'https://facebook.com/uzcasting' },
];

export default function ProfileScreen() {
  const { t, i18n } = useTranslation();

  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const signOut = useAuthStore((s) => s.signOut);

  const [languageOpen, setLanguageOpen] = useState(false);

  const language: Language = isSupportedLanguage(i18n.language) ? i18n.language : 'uz';
  const version = Constants.expoConfig?.version ?? '1.0.0';

  const me = useMe();
  const state = premiumState(me.data?.premium);
  const until = formatDate(me.data?.premium.until ?? null);

  const subscriptionValue =
    state === 'active'
      ? t('profile.subscriptionActive')
      : state === 'expired'
        ? t('profile.subscriptionExpired')
        : t('profile.subscriptionNone');

  // Дата нужна и когда подписка кончилась: «истекла 28.08» объясняет,
  // почему платный контент вдруг закрылся.
  const subscriptionHint =
    until !== null ? t('profile.subscriptionUntil', { date: until }) : undefined;

  const account: Row[] = [
    {
      key: 'mySubscription',
      label: t('profile.mySubscription'),
      hint: subscriptionHint,
      icon: 'ribbon-outline',
      // Ряд информационный: статус и срок — это и есть всё, что можно
      // сказать о подписке, пока нет экрана управления ею.
      value: subscriptionValue,
    },
    { key: 'topUp', label: t('profile.topUp'), hint: t('profile.topUpHint'), icon: 'card-outline' },
    { key: 'promocodes', label: t('profile.promocodes'), hint: t('profile.promocodesHint'), icon: 'pricetag-outline' },
    { key: 'paymentHistory', label: t('profile.paymentHistory'), hint: t('profile.paymentHistoryHint'), icon: 'time-outline' },
    {
      key: 'favorites',
      label: t('profile.favorites'),
      hint: t('profile.favoritesHint'),
      icon: 'heart-outline',
      onPress: () => router.push('/favorites'),
    },
    { key: 'devices', label: t('profile.devices'), hint: t('profile.devicesHint'), icon: 'phone-portrait-outline' },
    { key: 'tariffs', label: t('profile.tariffs'), hint: t('profile.tariffsHint'), icon: 'play-circle-outline' },
  ];

  const settings: Row[] = [
    { key: 'editProfile', label: t('profile.editProfile'), icon: 'person-outline' },
    { key: 'security', label: t('profile.security'), icon: 'shield-checkmark-outline' },
    { key: 'notifications', label: t('profile.notifications'), icon: 'notifications-outline' },
    {
      key: 'language',
      label: t('profile.language'),
      icon: 'globe-outline',
      value: LANGUAGE_LABELS[language],
      onPress: () => setLanguageOpen((open) => !open),
    },
    { key: 'about', label: t('profile.about'), icon: 'information-circle-outline', value: `v ${version}` },
    ...(isAuthorized
      ? [
          {
            key: 'logout',
            label: t('profile.logout'),
            icon: 'log-out-outline' as const,
            onPress: signOut,
            danger: true,
          },
        ]
      : []),
  ];

  return (
    <Screen
      title={t('profile.title')}
      headerRight={
        <Pressable
          onPress={() => router.push('/messages')}
          accessibilityRole="button"
          accessibilityLabel={t('profile.notifications')}
          hitSlop={10}
          className="h-11 w-11 items-center justify-center rounded-pill bg-surface active:opacity-70"
        >
          <Ionicons name="notifications-outline" size={20} color={colors.white} />
        </Pressable>
      }
    >
      <ProfileCard />

      {/* Подписчику незачем предлагать подписку: баннер выглядел бы так,
          будто платёж не прошёл. */}
      {state === 'active' ? null : <PremiumBanner />}

      <RowGroup rows={account} />

      <RowGroup rows={settings} />
      {languageOpen ? <LanguageSwitcher /> : null}

      <View className="flex-row justify-center gap-3">
        {SOCIALS.map((s) => (
          <Pressable
            key={s.key}
            accessibilityRole="link"
            accessibilityLabel={s.key}
            onPress={() => Linking.openURL(s.url).catch(() => {})}
            className="h-11 w-11 items-center justify-center rounded-pill bg-surface active:opacity-70"
          >
            <Ionicons name={s.icon} size={20} color={colors.textMuted} />
          </Pressable>
        ))}
      </View>

      <Text className="text-center text-micro text-text-disabled">
        UzCasting {version}
      </Text>
    </Screen>
  );
}

/** Разряды пробелами: 56000 → «56 000». Считает сервер, мы только читаем. */
function groupDigits(amount: number): string {
  return String(Math.round(amount)).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

/** Шапка аккаунта: аватар, имя, метки, три числа. */
function ProfileCard() {
  const { t } = useTranslation();

  const user = useAuthStore((s) => s.user);
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const balance = useBalance();

  // Тот же ключ запроса, что и на экране выше, — повторного обращения нет.
  const premium = premiumState(useMe().data?.premium);

  // Имени может не быть: Google отдаёт его не всегда. Тогда показываем email,
  // иначе карточка выглядит пустой у реально вошедшего человека.
  const displayName = user?.name || user?.email || user?.phone || '—';
  const subtitle = user?.email ?? user?.phone ?? null;

  return (
    <GlowCard>
      <View className="gap-4 p-4">
        <View className="flex-row items-center gap-3">
          <Avatar url={user?.avatarUrl ?? null} />

          <View className="flex-1 gap-1">
            <Text className="text-h2 text-text" numberOfLines={1}>
              {isAuthorized ? displayName : t('profile.guest')}
            </Text>
            {isAuthorized && subtitle ? (
              <Text className="text-caption text-text-muted" numberOfLines={1}>
                {subtitle}
              </Text>
            ) : (
              <Text className="text-caption text-text-muted">
                {t('profile.guestBody')}
              </Text>
            )}
            {isAuthorized && user?.id ? <UserIdRow id={user.id} /> : null}

            {/* Метка подписчика — как на макете Screen 4. Показывается
                только по ответу сервера: нарисовать её «на всякий случай»
                значило бы пообещать доступ, которого нет. */}
            {premium === 'active' ? (
              <View className="mt-1 flex-row items-center gap-1 self-start rounded-pill bg-purple px-3 py-1">
                <Ionicons name="diamond" size={11} color={colors.white} />
                <Text className="text-micro font-semibold text-white">
                  {t('profile.premiumMember')}
                </Text>
              </View>
            ) : null}
          </View>
        </View>

        {isAuthorized ? (
          <View className="flex-row items-stretch rounded-card bg-surface-2 py-3">
            <Stat
              icon="cash-outline"
              tint={colors.lime}
              label={t('profile.balance')}
              value={balance.data ? groupDigits(balance.data.money) : null}
            />
            <Divider />
            <Stat
              icon="star"
              tint={colors.gold}
              label={t('profile.stars')}
              value={balance.data ? String(balance.data.stars) : null}
            />
            <Divider />
            <Stat
              icon="film-outline"
              tint={colors.cyan}
              label={t('profile.coins')}
              value={balance.data ? String(balance.data.coins) : null}
            />
          </View>
        ) : (
          <Button variant="primary" shape="card" onPress={() => router.push('/(auth)/sign-in')}>
            {t('profile.signIn')}
          </Button>
        )}
      </View>
    </GlowCard>
  );
}

/**
 * Аватар в фирменном кольце.
 *
 * Кольцо — градиент, а не рамка одного цвета: на макете аватар обведён
 * тем же переходом, что и вся палитра, и это единственная деталь, которая
 * связывает карточку с фоном.
 */
function Avatar({ url }: { url: string | null }) {
  return (
    <LinearGradient
      colors={gradients.brandWide}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={{ padding: 2, borderRadius: radius.pill }}
    >
      <View
        className="items-center justify-center bg-surface"
        style={{ width: 60, height: 60, borderRadius: radius.pill, overflow: 'hidden' }}
      >
        {url ? (
          <Image
            source={{ uri: url }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            transition={150}
          />
        ) : (
          <Ionicons name="person-outline" size={26} color={colors.textMuted} />
        )}
      </View>
    </LinearGradient>
  );
}

function Divider() {
  return <View className="w-px self-stretch bg-border" />;
}

/** Одно число из трёх. `null` — данных нет, и это видно. */
function Stat({
  icon,
  tint,
  label,
  value,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  tint: string;
  label: string;
  value: string | null;
}) {
  return (
    <View className="flex-1 items-center gap-1">
      <View className="flex-row items-center gap-1.5">
        <Ionicons name={icon} size={13} color={tint} />
        <Text className="text-micro text-text-muted">{label}</Text>
      </View>
      <Text className={`text-h2 ${value === null ? 'text-text-disabled' : 'text-text'}`}>
        {value ?? '—'}
      </Text>
    </View>
  );
}

/**
 * Баннер Premium.
 *
 * Кнопка ведёт в никуда — экрана оплаты (19) нет, решение по платежам
 * через сторы не принято. Поэтому она неактивна и подписана «Tez orada»:
 * рабочая на вид кнопка, которая ничего не делает, хуже честно выключенной.
 */
function PremiumBanner() {
  const { t } = useTranslation();

  return (
    <View style={{ borderRadius: radius.cardLg, overflow: 'hidden' }}>
      <LinearGradient
        colors={gradients.premium}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={{ padding: 16, gap: 12 }}
      >
        <View className="flex-row items-start gap-3">
          <View className="flex-1 gap-1.5">
            <Text className="text-h2 text-white">{t('profile.premiumBannerTitle')}</Text>
            <Text className="text-caption text-white/80">
              {t('profile.premiumBannerBody')}
            </Text>
          </View>
          <Ionicons name="diamond" size={34} color={colors.gold} />
        </View>

        <View className="flex-row items-center gap-3">
          <Button variant="gold" disabled className="self-start">
            {t('profile.premiumBannerCta')}
          </Button>
          <Text className="text-micro text-white/70">{t('profile.soon')}</Text>
        </View>
      </LinearGradient>
    </View>
  );
}

/** Группа пунктов одним блоком, как на макете. */
function RowGroup({ rows }: { rows: Row[] }) {
  const { t } = useTranslation();

  return (
    <View className="overflow-hidden rounded-card-lg bg-surface">
      {rows.map((row, i) => {
        const interactive = Boolean(row.onPress);

        return (
          <Pressable
            key={row.key}
            accessibilityRole={interactive ? 'button' : undefined}
            accessibilityState={{ disabled: !interactive }}
            disabled={!interactive}
            onPress={row.onPress}
            className={`flex-row items-center gap-3 px-4 py-3.5 ${
              interactive ? 'active:opacity-70' : ''
            } ${i > 0 ? 'border-t border-border' : ''}`}
          >
            <View className="h-9 w-9 items-center justify-center rounded-card bg-surface-2">
              <Ionicons
                name={row.icon}
                size={18}
                color={row.danger ? colors.danger : colors.textMuted}
              />
            </View>

            <View className="flex-1">
              <Text className={`text-body ${row.danger ? 'text-danger' : 'text-text'}`}>
                {row.label}
              </Text>
              {row.hint ? (
                <Text numberOfLines={1} className="text-micro text-text-muted">
                  {row.hint}
                </Text>
              ) : null}
            </View>

            {row.value ? (
              <Text className="text-caption text-text-muted">{row.value}</Text>
            ) : null}

            {interactive ? (
              <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
            ) : row.value ? null : (
              // Ряда без действия быть не должно молча: метка объясняет,
              // почему он не нажимается.
              <Text className="text-micro text-text-disabled">{t('profile.soon')}</Text>
            )}
          </Pressable>
        );
      })}
    </View>
  );
}

/**
 * ID пользователя с копированием — у Yangi.TV он на видном месте
 * рядом с балансом: его диктуют в поддержку.
 */
function UserIdRow({ id }: { id: string }) {
  const [copied, setCopied] = useState(false);

  const onCopy = async () => {
    await Clipboard.setStringAsync(id);
    setCopied(true);
    // Возвращаем подпись обратно, иначе «скопировано» висит навсегда
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <Pressable
      onPress={onCopy}
      accessibilityRole="button"
      hitSlop={6}
      className="mt-0.5 flex-row items-center gap-1.5 active:opacity-60"
    >
      <Text numberOfLines={1} className="text-micro text-text-disabled">
        ID: {id}
      </Text>
      <Ionicons
        name={copied ? 'checkmark' : 'copy-outline'}
        size={13}
        color={copied ? colors.success : colors.textDisabled}
      />
    </Pressable>
  );
}
