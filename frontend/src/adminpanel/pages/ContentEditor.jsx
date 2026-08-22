import { useEffect, useState } from 'react';
import { useFieldErrors } from '../api/useFieldErrors';
import AccessTab from './editor/AccessTab';
import BasicTab from './editor/BasicTab';
import CreditsTab from './editor/CreditsTab';
import MediaTab from './editor/MediaTab';
import PublishTab from './editor/PublishTab';
import TextTab from './editor/TextTab';
import { adminApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import ConfirmDialog, { useConfirm } from '../components/ConfirmDialog';
import Modal from '../components/Modal';
import { usePanelI18n } from '../i18n';
import EpisodesTab from './EpisodesTab';


const emptyForm = () => ({
  slug: '',
  contentType: 'MOVIE',
  structureType: 'SINGLE',
  orientation: 'LANDSCAPE',
  status: 'DRAFT',
  accessPolicy: 'FREE',
  premierePrice: '',
  categoryId: '',
  genreIds: [],
  ageRating: '',
  durationMinutes: '',
  premiereDate: '',
  publicationDate: '',
  featured: false,
  popular: false,
  translations: { UZ: {}, RU: {}, EN: {} },
  posterDefault: null,
  posterByLocale: {},
  cover: null,
  gallery: [],
  credits: [],
  version: null,
});

/**
 * Kontent muharriri.
 *
 * Bitta ulkan forma emas - alti bo'limga ajratilgan (§22, §53).
 * Uch til bir vaqtda tahrirlanadi; to'ldirilmagan til tab'da belgilanadi.
 */
export default function ContentEditor({ open, contentId, onClose, onSaved }) {
  const { t } = usePanelI18n();
  const { can } = useAuth();

  const [tab, setTab] = useState('basic');
  const [locale, setLocale] = useState('UZ');
  // Tasdiqlash oqimi — saqlanmagan o'zgarish uchun (§51).
  const confirmer = useConfirm();

  // Backend maydon xatolari (ТЗ §52).
  const fields = useFieldErrors();

  const [form, setForm] = useState(emptyForm);
  const [categories, setCategories] = useState([]);
  const [genres, setGenres] = useState([]);
  const [creators, setCreators] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [dirty, setDirty] = useState(false);

  const isEdit = Boolean(contentId);

  const set = (patch) => {
    setForm((prev) => ({ ...prev, ...patch }));
    setDirty(true);
  };

  const setTranslation = (field, value) => {
    setForm((prev) => ({
      ...prev,
      translations: {
        ...prev.translations,
        [locale]: { ...(prev.translations[locale] || {}), [field]: value },
      },
    }));
    setDirty(true);
  };

  // Ma'lumotnomalar - modal ochilganda bir marta
  useEffect(() => {
    if (!open) return;
    setTab('basic');
    setLocale('UZ');
    setError(null);
    setDirty(false);
    // ⚠️ Bu yerda TO'LIQ ro'yxat kerak: ular ochiluvchi ro'yxatlarga
    // to'ldiriladi, sahifalanmaydi. Shuning uchun katta `size` so'raladi
    // va `items` ochib olinadi.
    const all = { size: 200 };
    adminApi.categories(all).then((r) => setCategories(r.items || []))
      .catch(() => setCategories([]));
    adminApi.genres(all).then((r) => setGenres(r.items || []))
      .catch(() => setGenres([]));
    adminApi.creators(all).then((r) => setCreators(r.items || []))
      .catch(() => setCreators([]));
  }, [open]);

  // Tahrirlashda mavjud qiymatlarni yuklaymiz
  useEffect(() => {
    if (!open) return;
    if (!contentId) {
      setForm(emptyForm());
      return;
    }
    adminApi
      .contentById(contentId)
      .then((c) => {
        setForm({
          ...emptyForm(),
          // ⚠️ §60: versiyasiz saqlash boshqa adminning ishini indamay
          // bosib ketardi — backend tekshiruvi `null` da o'tkazib
          // yuborilardi. Endi u majburiy.
          version: c.version ?? null,
          slug: c.slug || '',
          contentType: c.contentType,
          structureType: c.structureType,
          orientation: c.orientation,
          status: c.status,
          accessPolicy: c.accessPolicy,
          premierePrice: c.premierePrice ?? '',
          // ⚠️ B17: bu uch maydon YUKLANMASA, saqlashda o'chib ketadi -
          // backend media ro'yxatini butunlay almashtiradi.
          categoryId: c.categoryId ?? '',
          ageRating: c.ageRating || '',
          premiereDate: c.premiereDate ? c.premiereDate.slice(0, 16) : '',
          publicationDate: c.publicationDate ? c.publicationDate.slice(0, 16) : '',
          featured: Boolean(c.featured),
          popular: Boolean(c.popular),
          translations: { UZ: {}, RU: {}, EN: {}, ...(c.translations || {}) },
          posterDefault: c.posterMediaId || null,
          posterByLocale: c.localePosters || {},
          cover: c.coverMediaId || null,
          gallery: Array.isArray(c.gallery) ? c.gallery : [],
        });
        setDirty(false);
      })
      .catch(setError);
  }, [open, contentId]);

  const uzTitleMissing = !form.translations?.UZ?.title?.trim();

  // Backend ichma-ich maydonni `translations[UZ].title` deb yuboradi.
  const titleError = fields.errorOf(`translations[${locale}].title`)
    || fields.errorOf('translations');

  async function handleSave() {
    if (uzTitleMissing) {
      setTab('text');
      setLocale('UZ');
      setError({ code: 'VALIDATION_ERROR', message: t('editor.uzRequired') });
      return;
    }
    setSaving(true);
    setError(null);

    // Media ro'yxatini yig'amiz: umumiy afisha + tilga xoslari + muqova + galereya
    const media = [];
    if (form.posterDefault) media.push({ role: 'POSTER', mediaId: form.posterDefault, sortOrder: 0 });
    Object.entries(form.posterByLocale).forEach(([loc, id]) => {
      if (id) media.push({ role: 'POSTER', locale: loc, mediaId: id, sortOrder: 0 });
    });
    if (form.cover) media.push({ role: 'COVER', mediaId: form.cover, sortOrder: 0 });
    form.gallery.forEach((id, i) => media.push({ role: 'GALLERY', mediaId: id, sortOrder: i }));

    const payload = {
      slug: form.slug || undefined,
      contentType: form.contentType,
      structureType: form.structureType,
      orientation: form.orientation,
      status: form.status,
      accessPolicy: form.accessPolicy,
      premierePrice: form.premierePrice === '' ? null : Number(form.premierePrice),
      categoryId: form.categoryId === '' ? null : Number(form.categoryId),
      genreIds: form.genreIds,
      ageRating: form.ageRating || null,
      durationMinutes: form.durationMinutes === '' ? null : Number(form.durationMinutes),
      premiereDate: form.premiereDate ? `${form.premiereDate}:00` : null,
      publicationDate: form.publicationDate ? `${form.publicationDate}:00` : null,
      featured: form.featured,
      popular: form.popular,
      translations: form.translations,
      media,
      credits: form.credits,
      version: form.version,
    };

    try {
      if (isEdit) {
        await adminApi.updateContent(contentId, payload);
      } else {
        await adminApi.createContent(payload);
      }
      setDirty(false);
      onSaved();
      onClose();
    } catch (err) {
      // ⚠️ Kontent muharriri o'nlab maydonli va OLTI bo'limli. Umumiy
      // «Validatsiya xatosi» xabari bilan admin qaysi bo'limdagi qaysi
      // maydonni tuzatishni bilmasdi va ularni birma-bir sinab
      // ko'rishga majbur bo'lardi.
      fields.apply(err);
      setError(err);
    } finally {
      setSaving(false);
    }
  }

  function requestClose() {
    // Saqlanmagan o'zgarish bo'lsa tasdiqlash so'raladi. Bu buzuvchi
    // amal emas — shuning uchun tugma qizil bo'lmaydi (`danger: false`).
    if (!dirty) {
      onClose();
      return;
    }
    confirmer.ask({
      title: t('common.unsaved'),
      message: t('editor.unsavedBody'),
      confirmLabel: t('editor.discard'),
      danger: false,
      run: async () => onClose(),
    });
  }

  // Fasl/qism bo'limi faqat ko'p qismli tuzilishda ma'noga ega
  const hasParts = form.structureType !== 'SINGLE';

  const TABS = [
    ['basic', t('editor.tab.basic')],
    ['text', t('editor.tab.text')],
    ['media', t('editor.tab.media')],
    ['creators', t('editor.tab.creators')],
    ...(hasParts ? [['episodes', t('ep.tab')]] : []),
    ['access', t('editor.tab.access')],
    ['publish', t('editor.tab.publish')],
  ];

  // Tuzilish SINGLE ga o'zgarsa, ochiq turgan bo'lim yo'qoladi
  useEffect(() => {
    if (!hasParts && tab === 'episodes') {
      setTab('basic');
    }
  }, [hasParts, tab]);

  return (
    <Modal
      open={open}
      title={isEdit ? t('editor.edit') : t('editor.new')}
      onClose={requestClose}
      width={820}
      footer={
        <>
          {error && tab !== 'episodes' && (
            <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
              {error.message}
            </span>
          )}
          <button type="button" className="uz-btn uz-btn-ghost" onClick={requestClose}>
            {tab === 'episodes' ? t('common.close') : t('common.cancel')}
          </button>
          {/* Fasl va qismlar o'z saqlash tugmasiga ega - ikkita Saqlash chalkashtiradi */}
          {tab !== 'episodes' && (
            <button type="button" className="uz-btn uz-btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? t('common.saving') : t('common.save')}
            </button>
          )}
        </>
      }
    >
      <div className="uz-tabs" role="tablist">
        {TABS.map(([key, label]) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={tab === key}
            className={`uz-tab ${tab === key ? 'active' : ''}`}
            onClick={() => setTab(key)}
          >
            {label}
          </button>
        ))}
      </div>

      {/* ---------------------------------------------------------- ASOSIY */}
      {tab === 'basic' && (
        <BasicTab form={form} set={set} t={t} locale={locale}
                  categories={categories} genres={genres}
                  categoryError={fields.errorOf('categoryId')} />
      )}

      {tab === 'text' && (
        <TextTab form={form} t={t} locale={locale} setLocale={setLocale}
                 setTranslation={setTranslation}
                 uzTitleMissing={uzTitleMissing} titleError={titleError} />
      )}

      {tab === 'media' && (
        <MediaTab form={form} set={set} t={t} locale={locale} />
      )}

      {tab === 'creators' && (
        <CreditsTab form={form} set={set} t={t} locale={locale} creators={creators}
                    onCreatorCreated={(c) => setCreators((prev) => [c, ...prev])} />
      )}

      {tab === 'episodes' && (
        <EpisodesTab
          contentId={contentId}
          structureType={form.structureType}
          contentAccessPolicy={form.accessPolicy}
        />
      )}

      {/* ---------------------------------------------------- MONETIZATSIYA */}

      {tab === 'access' && (
        <AccessTab form={form} set={set} t={t} />
      )}

      {tab === 'publish' && (
        <PublishTab form={form} set={set} t={t} can={can} />
      )}

      <ConfirmDialog {...confirmer.props} />
    </Modal>
  );
}
