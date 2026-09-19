import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Redirect, router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  Switch,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenState } from '@/components/states/ScreenState';
import { Badge, type BadgeTone } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { Field, InfoRow, Section } from '@/features/casting/components';
import { formatDate } from '@/features/casting/status';
import {
  adminActionErrorKey,
  parsePrice,
  useAdminDetail,
  useApprove,
  useReject,
  useToggleCatalog,
  useTogglePhoto,
  type AdminCastingUser,
  type AdminStatus,
} from '@/features/castingAdmin/api';
import { useAdminStore } from '@/features/castingAdmin/store';
import { fileUrl } from '@/lib/api';
import { formatSum } from '@/lib/money';
import { useIsOffline } from '@/lib/network';
import { colors } from '@/theme/tokens';

/**
 * Анкета для админа — перенос `CastingUserDetail.js` старой админки.
 *
 * <h2>Действия</h2>
 * - «Qabul qilish» — только с ценой в сумах (обязательна, целое > 0).
 *   Запросы — цена, затем статус, как в старой админке (порядок разобран
 *   в `approveApplication`).
 * - «Rad etish» — с подтверждением: кандидат сразу видит отказ.
 * - «Katalogda ko'rsatish» — переключатель видимости анкеты.
 * - Фото — нажатием включается/выключается в каталоге. Каталог
 *   показывает только включённые, поэтому анкета без них в каталоге
 *   выглядела бы пустой.
 *
 * Как и в старой админке, решение принимается по новой анкете: у принятых
 * и отклонённых кнопок решения нет.
 */
const STATUS_TONE: Record<AdminStatus, BadgeTone> = {
  new: 'info',
  accepted: 'purchased',
  rejected: 'locked',
};

const STATUS_LABEL: Record<AdminStatus, string> = {
  new: 'castingAdmin.list.tabs.new',
  accepted: 'castingAdmin.list.tabs.accepted',
  rejected: 'castingAdmin.list.tabs.rejected',
};

export default function AdminDetailScreen() {
  const { t } = useTranslation();
  const { id } = useLocalSearchParams<{ id: string }>();
  const numericId = Number(id);
  const validId = Number.isInteger(numericId) ? numericId : null;

  const token = useAdminStore((s) => s.token);
  const isOffline = useIsOffline();
  const query = useAdminDetail(validId, token !== null);

  if (!token) return <Redirect href="/admin/login" />;

  const shell = (children: React.ReactNode) => (
    <Screen scroll={false} title={t('castingAdmin.detail.title')} onBack={() => router.back()} underTabBar={false}>
      {children}
    </Screen>
  );

  if (query.isPending) return shell(<ScreenState kind="loading" />);
  if (query.isError) {
    return shell(<ScreenState kind={isOffline ? 'offline' : 'error'} onRetry={() => query.refetch()} />);
  }
  if (!query.data) return shell(<ScreenState kind="empty" body={t('castingAdmin.detail.notFound')} />);

  return <Detail user={query.data} />;
}

function Detail({ user }: { user: AdminCastingUser }) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();

  const approve = useApprove();
  const reject = useReject();
  const toggleCatalog = useToggleCatalog();
  const togglePhoto = useTogglePhoto();

  const [price, setPrice] = useState(user.price ? String(Math.round(user.price)) : '');
  const [priceError, setPriceError] = useState<string | null>(null);
  const [message, setMessage] = useState<{ text: string; tone: 'ok' | 'error' } | null>(null);
  const [busyPhoto, setBusyPhoto] = useState<string | null>(null);

  const busy = approve.isPending || reject.isPending;
  const fail = (e: unknown) => setMessage({ text: t(adminActionErrorKey(e)), tone: 'error' });

  const onApprove = async () => {
    setMessage(null);
    const value = parsePrice(price);
    if (value === null) {
      setPriceError(t('castingAdmin.detail.priceInvalid'));
      return;
    }
    try {
      await approve.mutateAsync({ id: user.id, price: value });
      setMessage({ text: t('castingAdmin.detail.approved'), tone: 'ok' });
    } catch (e) {
      fail(e);
    }
  };

  const onReject = () => {
    setMessage(null);
    Alert.alert(t('castingAdmin.detail.rejectConfirmTitle'), t('castingAdmin.detail.rejectConfirmBody'), [
      { text: t('common.cancel'), style: 'cancel' },
      {
        text: t('castingAdmin.detail.reject'),
        style: 'destructive',
        onPress: async () => {
          try {
            await reject.mutateAsync(user.id);
            setMessage({ text: t('castingAdmin.detail.rejected'), tone: 'ok' });
          } catch (e) {
            fail(e);
          }
        },
      },
    ]);
  };

  const onToggleCatalog = async () => {
    setMessage(null);
    try {
      await toggleCatalog.mutateAsync(user.id);
    } catch (e) {
      fail(e);
    }
  };

  const onTogglePhoto = async (photoId: string) => {
    setMessage(null);
    setBusyPhoto(photoId);
    try {
      await togglePhoto.mutateAsync(photoId);
    } catch (e) {
      fail(e);
    } finally {
      setBusyPhoto(null);
    }
  };

  const typeLabel = t(`casting.types.${user.castingType}`, { defaultValue: user.castingType });
  const gender =
    user.gender === 'female' ? t('casting.form.female') : user.gender === 'male' ? t('casting.form.male') : user.gender;
  const f = (key: string) => t(`casting.form.fields.${key}`);

  return (
    <KeyboardAvoidingView className="flex-1 bg-ink" behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <Screen
        scroll={false}
        title={user.name || t('castingAdmin.detail.title')}
        subtitle={typeLabel}
        onBack={() => router.back()}
        underTabBar={false}
      >
        <ScrollView
          className="flex-1"
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          contentContainerClassName="px-4 gap-4"
          contentContainerStyle={{ paddingBottom: insets.bottom + 24 }}
        >
          <View className="flex-row flex-wrap items-center gap-2">
            <Badge tone={STATUS_TONE[user.status]}>{t(STATUS_LABEL[user.status])}</Badge>
            {user.status === 'accepted' && user.price ? (
              <Badge tone="purchased">{t('common.price', { amount: formatSum(user.price) })}</Badge>
            ) : null}
            {user.paid ? <Badge tone="verified">{t('castingAdmin.detail.paid')}</Badge> : null}
          </View>

          {/* Каталог — переключатель на сервере, текущее состояние берём из анкеты. */}
          <View className="flex-row items-center gap-3 rounded-card-lg border border-border bg-surface p-4">
            <View className="flex-1 gap-1">
              <Text className="text-body font-semibold text-text">{t('castingAdmin.detail.catalog')}</Text>
              <Text className="text-caption text-text-muted">{t('castingAdmin.detail.catalogHint')}</Text>
            </View>
            {toggleCatalog.isPending ? (
              <ActivityIndicator color={colors.purple} />
            ) : (
              <Switch
                value={user.isWebShow}
                onValueChange={() => void onToggleCatalog()}
                trackColor={{ true: colors.purple, false: colors.border }}
                thumbColor={colors.white}
              />
            )}
          </View>

          {user.status === 'new' ? (
            <Section title={t('castingAdmin.detail.approve')}>
              <Field
                label={t('castingAdmin.detail.priceLabel')}
                required
                value={price}
                onChangeText={(v) => {
                  setPrice(v.replace(/[^\d\s]/g, ''));
                  setPriceError(null);
                }}
                placeholder={t('castingAdmin.detail.pricePlaceholder')}
                keyboardType="number-pad"
                maxLength={13}
                error={priceError}
              />
              <View className="flex-row gap-3">
                <Button className="flex-1" variant="primary" onPress={onApprove} loading={approve.isPending} disabled={busy}>
                  {t('castingAdmin.detail.approve')}
                </Button>
                <Button className="flex-1" variant="secondary" onPress={onReject} loading={reject.isPending} disabled={busy}>
                  {t('castingAdmin.detail.reject')}
                </Button>
              </View>
            </Section>
          ) : null}

          {message ? (
            <Text className="text-center text-caption" style={{ color: message.tone === 'ok' ? colors.success : colors.danger }}>
              {message.text}
            </Text>
          ) : null}

          <Section title={t('castingAdmin.detail.sections.photos')}>
            <Text className="text-caption text-text-muted">{t('castingAdmin.detail.photosHint')}</Text>
            <View className="flex-row flex-wrap gap-2">
              {user.photos.map((p) => (
                <Pressable
                  key={p.id}
                  onPress={() => void onTogglePhoto(p.id)}
                  disabled={busyPhoto !== null}
                  accessibilityRole="switch"
                  accessibilityState={{ checked: p.isWebShow }}
                  style={{ width: 100 }}
                  className="gap-1 active:opacity-80"
                >
                  <View
                    style={{ width: 100, height: 133, borderColor: p.isWebShow ? colors.success : colors.border }}
                    className="overflow-hidden rounded-card border-2 bg-surface-2"
                  >
                    <Image
                      source={{ uri: fileUrl(p.id) }}
                      style={{ width: '100%', height: '100%', opacity: p.isWebShow ? 1 : 0.45 }}
                      contentFit="cover"
                      cachePolicy="memory-disk"
                    />
                    {busyPhoto === p.id ? (
                      <View className="absolute inset-0 items-center justify-center" style={{ backgroundColor: 'rgba(7,7,13,0.5)' }}>
                        <ActivityIndicator color={colors.white} />
                      </View>
                    ) : null}
                  </View>
                  <View className="flex-row items-center gap-1">
                    <Ionicons
                      name={p.isWebShow ? 'eye-outline' : 'eye-off-outline'}
                      size={12}
                      color={p.isWebShow ? colors.success : colors.textMuted}
                    />
                    <Text className="text-micro" style={{ color: p.isWebShow ? colors.success : colors.textMuted }}>
                      {p.isWebShow ? t('castingAdmin.detail.photoPublic') : t('castingAdmin.detail.photoHidden')}
                    </Text>
                  </View>
                </Pressable>
              ))}
            </View>
          </Section>

          <Section title={t('castingAdmin.detail.sections.basic')}>
            <InfoRow label={f('castingType')} value={typeLabel} />
            <InfoRow label={f('gender')} value={gender} />
            <InfoRow label={f('name')} value={user.name} />
            <InfoRow label={f('region')} value={user.region} />
            <InfoRow label={f('nationality')} value={user.nationality} />
            <InfoRow label={f('birthday')} value={formatDate(user.birthday)} />
            <InfoRow label={t('castingAdmin.detail.age')} value={user.age} />
            <InfoRow label={t('castingAdmin.detail.createdAt')} value={formatDate(user.createdAt)} />
          </Section>

          <Section title={t('castingAdmin.detail.sections.physical')}>
            <InfoRow label={f('height')} value={user.height} />
            <InfoRow label={f('hairColor')} value={user.hairColor} />
            <InfoRow label={f('eyeColor')} value={user.eyeColor} />
            <InfoRow label={f('clothSize')} value={user.clothSize} />
            <InfoRow label={f('shoeSize')} value={user.shoeSize} />
            <InfoRow label={f('bust')} value={user.bust} />
            <InfoRow label={f('waist')} value={user.waist} />
            <InfoRow label={f('son')} value={user.son} />
          </Section>

          <Section title={t('castingAdmin.detail.sections.contact')}>
            <InfoRow label={f('phone')} value={user.phone} />
            <InfoRow label={f('email')} value={user.email} />
            <InfoRow label={f('telegram')} value={user.telegram} />
            <InfoRow label={f('facebook')} value={user.facebook} />
            <InfoRow label={f('instagram')} value={user.instagram} />
          </Section>
        </ScrollView>
      </Screen>
    </KeyboardAvoidingView>
  );
}
