import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import * as ImagePicker from 'expo-image-picker';
import { router } from 'expo-router';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { pushOnce } from '@/lib/navigation';
import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { useAuthStore } from '@/features/auth/store';
import {
  castingErrorKey,
  toCastingError,
  uploadCastingPhoto,
  useSubmitApplication,
} from '@/features/casting/api';
import { BirthdayField } from '@/features/casting/BirthdayField';
import { ChoiceField, Field, Section, SelectOrTextField } from '@/features/casting/components';
import { useKeyboardInset } from '@/lib/keyboard';
import {
  EMPTY_FORM,
  FIELD_LIMITS,
  MAX_PHOTOS,
  MAX_PHOTO_BYTES,
  MIN_PHOTOS,
  buildPayload,
  sanitizeField,
  validateApplication,
  type ApplicationForm,
  type FormField,
} from '@/features/casting/form';
import { CASTING_TYPES, CLOTH_SIZES, GENDERS, REGIONS } from '@/features/casting/options';
import { colors } from '@/theme/tokens';

/**
 * Заявка на кастинг — перенос формы сайта (`DataForm.js`) в приложение.
 *
 * <h2>Порядок работы</h2>
 * 1. Фото загружаются СРАЗУ после выбора, по одному — как на сайте.
 *    Так человек видит проблему с конкретным снимком (слишком большой,
 *    оборвалась связь) до того, как заполнит всю анкету.
 * 2. Отправка анкеты идёт только с id уже загруженных фото.
 *
 * <h2>⚠️ Фото без id не уходят</h2>
 * Превью показывается до ответа сервера. Если загрузка упала, карточка
 * остаётся с пометкой и кнопкой «повторить», но в заявку не попадает:
 * на сайте однажды было наоборот, и человек отправлял анкету с фото,
 * которого на сервере не было.
 */
type PhotoItem = {
  key: string;
  uri: string;
  fileName?: string | null;
  mimeType?: string | null;
  status: 'uploading' | 'done' | 'error';
  id?: string;
  /** Уже переведённый текст причины. */
  error?: string;
};

export default function ApplyScreen() {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const keyboard = useKeyboardInset();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const user = useAuthStore((s) => s.user);

  // Имя и телефон уже известны по аккаунту — не заставляем набирать заново.
  const [form, setForm] = useState<ApplicationForm>(() => ({
    ...EMPTY_FORM,
    name: user?.name ?? '',
    phone: user?.phone ?? '',
  }));
  const [errors, setErrors] = useState<Partial<Record<FormField, string>>>({});
  const [photos, setPhotos] = useState<PhotoItem[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [pendingExists, setPendingExists] = useState(false);
  const [done, setDone] = useState(false);

  const submit = useSubmitApplication();
  const keySeq = useRef(0);

  if (!isAuthorized) {
    return (
      <Screen title={t('casting.form.title')} onBack={() => router.back()} underTabBar={false}>
        <ScreenState
          kind="locked"
          body={t('casting.signInToApply')}
          actionLabel={t('profile.signIn')}
          onAction={() => pushOnce('/(auth)/sign-in')}
        />
      </Screen>
    );
  }

  if (done) {
    return (
      <Screen scroll={false} title={t('casting.form.title')} onBack={() => router.back()} underTabBar={false}>
        <View className="flex-1 items-center justify-center gap-4 px-8">
          <Ionicons name="checkmark-circle" size={64} color={colors.success} />
          <Text className="text-center text-h2 text-text">{t('casting.form.success.title')}</Text>
          <Text className="text-center text-body text-text-muted">{t('casting.form.success.body')}</Text>
          <Button variant="primary" onPress={() => router.replace('/casting/my')}>
            {t('casting.form.success.action')}
          </Button>
          <Button variant="secondary" onPress={() => router.back()}>
            {t('casting.form.success.back')}
          </Button>
        </View>
      </Screen>
    );
  }

  const set = <K extends keyof ApplicationForm>(field: K, value: ApplicationForm[K]) => {
    // Фильтр и предел длины — здесь, а не у каждого поля: см. sanitizeField.
    const clean = (typeof value === 'string' ? sanitizeField(field, value) : value) as ApplicationForm[K];
    setForm((prev) => ({ ...prev, [field]: clean }));
    // Поле исправили — старая ошибка под ним больше не про него.
    setErrors((prev) => (prev[field] ? { ...prev, [field]: undefined } : prev));
    setSubmitError(null);
  };

  const patchPhoto = (key: string, patch: Partial<PhotoItem>) =>
    setPhotos((prev) => prev.map((p) => (p.key === key ? { ...p, ...patch } : p)));

  const upload = async (item: PhotoItem) => {
    patchPhoto(item.key, { status: 'uploading', error: undefined });
    try {
      const id = await uploadCastingPhoto(item);
      patchPhoto(item.key, { status: 'done', id });
    } catch (e) {
      const err = toCastingError(e);
      // Сервер про размер говорит конкретно («Rasm juda katta: 25 MB…») —
      // его текст полезнее нашего общего.
      patchPhoto(item.key, {
        status: 'error',
        error: err.code === 'VALIDATION_ERROR' && err.serverMessage ? err.serverMessage : t(castingErrorKey(err)),
      });
    }
  };

  const onPick = async () => {
    const remaining = MAX_PHOTOS - photos.length;
    if (remaining <= 0) return;

    let result: ImagePicker.ImagePickerResult;
    try {
      result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsMultipleSelection: true,
        selectionLimit: remaining,
        // Без обрезки: снимок уходит как есть, в любом формате и пропорциях.
        // Пережатие 0.8 на глаз неотличимо и держит снимок с камеры
        // в пределах 20 МБ сервера.
        quality: 0.8,
      });
    } catch {
      setErrors((prev) => ({ ...prev, photos: t('casting.form.pickerDenied') }));
      return;
    }
    if (result.canceled) return;

    const picked: PhotoItem[] = result.assets.slice(0, remaining).map((a) => {
      keySeq.current += 1;
      const tooLarge = typeof a.fileSize === 'number' && a.fileSize > MAX_PHOTO_BYTES;
      return {
        key: `p${keySeq.current}`,
        uri: a.uri,
        fileName: a.fileName,
        mimeType: a.mimeType,
        status: tooLarge ? 'error' : 'uploading',
        error: tooLarge ? t('casting.form.errors.photoTooLarge') : undefined,
      };
    });

    setPhotos((prev) => [...prev, ...picked]);
    setErrors((prev) => ({ ...prev, photos: undefined }));
    setSubmitError(null);

    // По одному, а не пачкой: на мобильной сети параллельные загрузки
    // мешают друг другу и чаще падают по таймауту все сразу.
    for (const item of picked) {
      if (item.status === 'uploading') await upload(item);
    }
  };

  const removePhoto = (key: string) => {
    setPhotos((prev) => prev.filter((p) => p.key !== key));
    setSubmitError(null);
  };

  const uploaded = photos.filter((p) => p.status === 'done' && p.id).map((p) => p.id as string);
  const isUploading = photos.some((p) => p.status === 'uploading');

  const onSubmit = async () => {
    setSubmitError(null);
    setPendingExists(false);

    if (isUploading) {
      setSubmitError(t('casting.form.errors.photosUploading'));
      return;
    }

    const found = validateApplication(form, uploaded.length);
    if (Object.keys(found).length > 0) {
      const translated: Partial<Record<FormField, string>> = {};
      for (const [field, key] of Object.entries(found)) {
        translated[field as FormField] = t(key as string, { min: MIN_PHOTOS, max: MAX_PHOTOS });
      }
      setErrors(translated);
      setSubmitError(t('casting.form.fixErrors'));
      return;
    }

    try {
      await submit.mutateAsync(buildPayload(form, uploaded));
      setDone(true);
    } catch (e) {
      const err = toCastingError(e);
      if (err.code === 'VALIDATION_ERROR' && Object.keys(err.fieldErrors).length > 0) {
        // Сервер назвал поля — показываем его текст под каждым.
        setErrors((prev) => ({ ...prev, ...(err.fieldErrors as Partial<Record<FormField, string>>) }));
      }
      if (err.code === 'CASTING_APPLICATION_PENDING') setPendingExists(true);
      setSubmitError(t(castingErrorKey(err)));
    }
  };

  const typeOptions = CASTING_TYPES.map((value) => ({ value, label: t(`casting.types.${value}`) }));
  const genderOptions = GENDERS.map((value) => ({ value, label: t(`casting.form.${value}`) }));
  const f = (key: keyof ApplicationForm) => t(`casting.form.fields.${key}`);
  const regionOptions = REGIONS.map((r) => ({ value: r.value as string, label: t(`casting.form.regions.${r.key}`) }));
  const clothOptions = CLOTH_SIZES.map((v) => ({ value: v as string, label: v }));
  const isMale = form.gender === 'male';

  return (
    <Screen
      scroll={false}
      title={t('casting.form.title')}
      subtitle={t('casting.form.subtitle')}
      onBack={() => router.back()}
      underTabBar={false}
      // Отступ снизу отмеряем сами: под клавиатурой он другой.
      padBottom={false}
    >
      {/*
        ⚠️ Отступ под клавиатуру — на обёртке прокрутки, а не в её
        содержимом: так короче становится ВИДИМАЯ часть списка, и поле,
        до которого добрался человек, можно вывести из-под клавиатуры.
        Почему не `KeyboardAvoidingView` — см. `useKeyboardInset`.
      */}
      <View className="flex-1" style={{ paddingBottom: keyboard }}>
        <ScrollView
          className="flex-1"
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          contentContainerClassName="px-4 gap-4"
          contentContainerStyle={{ paddingBottom: insets.bottom + 24 }}
        >
          <Section title={t('casting.form.sections.basic')}>
            <ChoiceField
              label={f('castingType')}
              required
              options={typeOptions}
              value={form.castingType}
              onChange={(v) => set('castingType', v)}
              error={errors.castingType}
            />
            <ChoiceField
              label={f('gender')}
              required
              options={genderOptions}
              value={form.gender}
              onChange={(v) => set('gender', v)}
              error={errors.gender}
            />
            <Field label={f('name')} required value={form.name} onChangeText={(v) => set('name', v)} error={errors.name} autoCapitalize="words" maxLength={FIELD_LIMITS.name} />
            <SelectOrTextField
              label={f('region')}
              required
              options={regionOptions}
              value={form.region}
              onChange={(v) => set('region', v)}
              placeholder={t('casting.form.regionPlaceholder')}
              otherLabel={t('casting.form.other')}
              otherPlaceholder={t('casting.form.regionOtherPlaceholder')}
              error={errors.region}
              maxLength={FIELD_LIMITS.region}
            />
            <Field label={f('nationality')} required value={form.nationality} onChangeText={(v) => set('nationality', v)} error={errors.nationality} maxLength={FIELD_LIMITS.nationality} />
            <BirthdayField
              label={f('birthday')}
              required
              value={form.birthday}
              onChange={(v) => set('birthday', v)}
              placeholder={t('casting.form.birthdayPlaceholder')}
              error={errors.birthday}
            />
          </Section>

          <Section title={t('casting.form.sections.physical')}>
            <Field label={f('height')} required value={form.height} onChangeText={(v) => set('height', v)} keyboardType="number-pad" maxLength={FIELD_LIMITS.height} error={errors.height} />
            <Field label={f('hairColor')} required value={form.hairColor} onChangeText={(v) => set('hairColor', v)} error={errors.hairColor} maxLength={FIELD_LIMITS.hairColor} />
            <Field label={f('eyeColor')} required value={form.eyeColor} onChangeText={(v) => set('eyeColor', v)} error={errors.eyeColor} maxLength={FIELD_LIMITS.eyeColor} />
            <SelectOrTextField
              label={f('clothSize')}
              options={clothOptions}
              value={form.clothSize}
              onChange={(v) => set('clothSize', v)}
              placeholder={t('casting.form.clothSizePlaceholder')}
              otherLabel={t('casting.form.other')}
              otherPlaceholder={t('casting.form.clothSizeOtherPlaceholder')}
              error={errors.clothSize}
              maxLength={FIELD_LIMITS.clothSize}
            />
            <Field label={f('shoeSize')} value={form.shoeSize} onChangeText={(v) => set('shoeSize', v)} keyboardType="number-pad" error={errors.shoeSize} maxLength={FIELD_LIMITS.shoeSize} />
            {/* Как на сайте: грудь и бёдра спрашиваем только не у мужчин. */}
            {!isMale ? (
              <>
                <Field label={f('bust')} value={form.bust} onChangeText={(v) => set('bust', v)} keyboardType="number-pad" error={errors.bust} maxLength={FIELD_LIMITS.bust} />
                <Field label={f('son')} value={form.son} onChangeText={(v) => set('son', v)} keyboardType="number-pad" error={errors.son} maxLength={FIELD_LIMITS.son} />
              </>
            ) : null}
            <Field label={f('waist')} value={form.waist} onChangeText={(v) => set('waist', v)} keyboardType="number-pad" error={errors.waist} maxLength={FIELD_LIMITS.waist} />
          </Section>

          <Section title={t('casting.form.sections.contact')}>
            <Field label={f('email')} required value={form.email} onChangeText={(v) => set('email', v)} keyboardType="email-address" autoCapitalize="none" autoCorrect={false} error={errors.email} maxLength={FIELD_LIMITS.email} />
            <Field label={f('phone')} required value={form.phone} onChangeText={(v) => set('phone', v)} keyboardType="phone-pad" placeholder={t('casting.form.phonePlaceholder')} error={errors.phone} maxLength={FIELD_LIMITS.phone} />
            <Field label={f('telegram')} value={form.telegram} onChangeText={(v) => set('telegram', v)} autoCapitalize="none" autoCorrect={false} error={errors.telegram} maxLength={FIELD_LIMITS.telegram} />
            <Field label={f('facebook')} value={form.facebook} onChangeText={(v) => set('facebook', v)} autoCapitalize="none" autoCorrect={false} error={errors.facebook} maxLength={FIELD_LIMITS.facebook} />
            <Field label={f('instagram')} value={form.instagram} onChangeText={(v) => set('instagram', v)} autoCapitalize="none" autoCorrect={false} error={errors.instagram} maxLength={FIELD_LIMITS.instagram} />
          </Section>

          <Section title={t('casting.form.sections.photos')}>
            <Text className="text-caption text-text-muted">
              {t('casting.form.photosHint', { min: MIN_PHOTOS, max: MAX_PHOTOS })}
            </Text>
            <Text className="text-caption text-text-muted">{t('casting.form.photoSize')}</Text>

            <PhotoGrid
              photos={photos}
              canAdd={photos.length < MAX_PHOTOS}
              onAdd={onPick}
              onRemove={removePhoto}
              onRetry={(item) => void upload(item)}
            />

            <Text className="text-caption text-text-muted">
              {t('casting.form.photosCount', { count: uploaded.length, max: MAX_PHOTOS })}
            </Text>
            {errors.photos ? (
              <Text className="text-micro" style={{ color: colors.danger }}>
                {errors.photos}
              </Text>
            ) : null}
          </Section>

          {submitError ? (
            <View className="gap-2 rounded-card border p-3" style={{ borderColor: colors.danger }}>
              <Text className="text-caption" style={{ color: colors.danger }}>
                {submitError}
              </Text>
              {pendingExists ? (
                <Pressable onPress={() => router.replace('/casting/my')} hitSlop={8}>
                  <Text className="text-caption text-cyan">{t('casting.my.title')}</Text>
                </Pressable>
              ) : null}
            </View>
          ) : null}

          <Button variant="primary" onPress={onSubmit} loading={submit.isPending} disabled={submit.isPending}>
            {t('casting.form.submit')}
          </Button>
        </ScrollView>
      </View>
    </Screen>
  );
}

const TILE = 96;

function PhotoGrid({
  photos,
  canAdd,
  onAdd,
  onRemove,
  onRetry,
}: {
  photos: PhotoItem[];
  canAdd: boolean;
  onAdd: () => void;
  onRemove: (key: string) => void;
  onRetry: (item: PhotoItem) => void;
}) {
  const { t } = useTranslation();

  return (
    <View className="flex-row flex-wrap gap-2">
      {photos.map((p) => (
        <View key={p.key} style={{ width: TILE }} className="gap-1">
          <View style={{ width: TILE, height: TILE }} className="overflow-hidden rounded-card bg-surface-2">
            <Image source={{ uri: p.uri }} style={{ width: '100%', height: '100%' }} contentFit="cover" />

            {p.status === 'uploading' ? (
              <View className="absolute inset-0 items-center justify-center" style={{ backgroundColor: 'rgba(7,7,13,0.55)' }}>
                <ActivityIndicator color={colors.white} />
              </View>
            ) : null}

            {p.status === 'error' ? (
              <Pressable
                onPress={() => onRetry(p)}
                accessibilityRole="button"
                accessibilityLabel={t('casting.form.retry')}
                className="absolute inset-0 items-center justify-center"
                style={{ backgroundColor: 'rgba(7,7,13,0.7)' }}
              >
                <Ionicons name="refresh" size={22} color={colors.danger} />
              </Pressable>
            ) : null}

            <Pressable
              onPress={() => onRemove(p.key)}
              accessibilityRole="button"
              accessibilityLabel={t('casting.form.remove')}
              hitSlop={8}
              className="absolute right-1 top-1 h-7 w-7 items-center justify-center rounded-pill"
              style={{ backgroundColor: 'rgba(7,7,13,0.7)' }}
            >
              <Ionicons name="close" size={16} color={colors.white} />
            </Pressable>
          </View>
          {p.status === 'error' && p.error ? (
            <Text numberOfLines={3} className="text-micro" style={{ color: colors.danger }}>
              {p.error}
            </Text>
          ) : null}
        </View>
      ))}

      {canAdd ? (
        <Pressable
          onPress={onAdd}
          accessibilityRole="button"
          accessibilityLabel={t('casting.form.addPhoto')}
          style={{ width: TILE, height: TILE, borderStyle: 'dashed' }}
          className="items-center justify-center gap-1 rounded-card border border-border active:opacity-70"
        >
          <Ionicons name="add" size={26} color={colors.textMuted} />
          <Text className="text-center text-micro text-text-muted">{t('casting.form.addPhoto')}</Text>
        </Pressable>
      ) : null}
    </View>
  );
}
