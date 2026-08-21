import { useEffect, useMemo, useState } from 'react';
import { adminApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import LocaleTabs from '../components/LocaleTabs';
import MediaField from '../components/MediaField';
import GalleryField from '../components/GalleryField';
import Modal from '../components/Modal';
import { LOCALES, toBackendLocale, usePanelI18n } from '../i18n';
import EpisodesTab from './EpisodesTab';

const TYPES = ['MOVIE', 'SERIES', 'MINI_SERIES', 'SHORT_FILM', 'PODCAST', 'SHOW', 'INTERVIEW', 'STREAM', 'CLIP', 'OTHER'];
const STRUCTURES = ['SINGLE', 'EPISODIC', 'SEASONAL'];
const ORIENTATIONS = ['LANDSCAPE', 'VERTICAL'];
const STATUSES = ['DRAFT', 'IN_REVIEW', 'SCHEDULED', 'PUBLISHED', 'ARCHIVED'];
const POLICIES = ['FREE', 'PREMIUM_ONLY', 'PURCHASE_ONLY', 'PREMIUM_OR_PURCHASE'];
const PROFESSIONS = ['ACTOR', 'ACTRESS', 'DIRECTOR', 'MODEL', 'PRODUCER', 'SCREENWRITER', 'OPERATOR', 'HOST', 'CREATOR', 'OTHER'];

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
  const [form, setForm] = useState(emptyForm);
  const [categories, setCategories] = useState([]);
  const [genres, setGenres] = useState([]);
  const [creators, setCreators] = useState([]);
  const [creatorQuery, setCreatorQuery] = useState('');
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
    adminApi.categories().then(setCategories).catch(() => setCategories([]));
    adminApi.genres().then(setGenres).catch(() => setGenres([]));
    adminApi.creators({ size: 100 }).then(setCreators).catch(() => setCreators([]));
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

  const filteredCreators = useMemo(() => {
    const q = creatorQuery.trim().toLowerCase();
    if (!q) return creators.slice(0, 12);
    return creators
      .filter((c) =>
        Object.values(c.translations || {}).some((tr) =>
          (tr.displayName || '').toLowerCase().includes(q)
        )
      )
      .slice(0, 12);
  }, [creators, creatorQuery]);

  const creatorName = (c) => {
    const tr = c.translations || {};
    return tr[locale]?.displayName || tr.UZ?.displayName || c.slug;
  };

  const uzTitleMissing = !form.translations?.UZ?.title?.trim();

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
      setError(err);
    } finally {
      setSaving(false);
    }
  }

  function requestClose() {
    if (dirty && !window.confirm(t('common.unsaved'))) return;
    onClose();
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
        <div className="uz-row">
          <div className="uz-col">
            <label className="uz-label" htmlFor="ct">{t('editor.type')}</label>
            <select id="ct" className="uz-select" value={form.contentType}
                    onChange={(e) => set({ contentType: e.target.value })}>
              {TYPES.map((x) => <option key={x} value={x}>{x.replace(/_/g, ' ')}</option>)}
            </select>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="st">{t('editor.structure')}</label>
            <select id="st" className="uz-select" value={form.structureType}
                    onChange={(e) => set({ structureType: e.target.value })}>
              {STRUCTURES.map((x) => <option key={x} value={x}>{x}</option>)}
            </select>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="or">{t('editor.orientation')}</label>
            <select id="or" className="uz-select" value={form.orientation}
                    onChange={(e) => set({ orientation: e.target.value })}>
              {ORIENTATIONS.map((x) => (
                <option key={x} value={x}>
                  {x === 'VERTICAL' ? t('content.vertical') : t('content.landscape')}
                </option>
              ))}
            </select>
          </div>
          <div className="uz-col" style={{ flexBasis: '100%' }}>
            <label className="uz-label" htmlFor="cat">{t('editor.category')}</label>
            <select id="cat" className="uz-select" value={form.categoryId}
                    onChange={(e) => set({ categoryId: e.target.value })}>
              <option value="">{t('common.none')}</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.translations?.[locale]?.title || c.translations?.UZ?.title || c.slug}
                </option>
              ))}
            </select>
          </div>
          <div style={{ flexBasis: '100%' }}>
            <label className="uz-label">{t('editor.genres')}</label>
            <div className="flex gap-2 flex-wrap">
              {genres.map((g) => {
                const on = form.genreIds.includes(g.id);
                return (
                  <button key={g.id} type="button"
                          className={`uz-chip ${on ? 'selected' : ''}`}
                          aria-pressed={on}
                          onClick={() => set({
                            genreIds: on ? form.genreIds.filter((x) => x !== g.id)
                                         : [...form.genreIds, g.id],
                          })}>
                    {g.translations?.[locale]?.title || g.translations?.UZ?.title || g.slug}
                  </button>
                );
              })}
            </div>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="ar">{t('editor.ageRating')}</label>
            <input id="ar" className="uz-input" value={form.ageRating} placeholder="16+"
                   onChange={(e) => set({ ageRating: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="du">{t('editor.duration')}</label>
            <input id="du" className="uz-input" type="number" min="0" value={form.durationMinutes}
                   onChange={(e) => set({ durationMinutes: e.target.value })} />
          </div>
        </div>
      )}

      {/* --------------------------------------------------------- MATNLAR */}
      {tab === 'text' && (
        <>
          <LocaleTabs
            active={locale}
            onChange={setLocale}
            isFilled={(code) => Boolean(form.translations?.[code]?.title?.trim())}
          />
          <div className="mb-4">
            <label className="uz-label" htmlFor="ti">
              {t('editor.title')}
              {locale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
            </label>
            <input id="ti" className="uz-input"
                   value={form.translations?.[locale]?.title || ''}
                   aria-invalid={locale === 'UZ' && uzTitleMissing}
                   onChange={(e) => setTranslation('title', e.target.value)} />
            {locale === 'UZ' && uzTitleMissing && (
              <div className="uz-field-error">{t('editor.uzRequired')}</div>
            )}
          </div>
          <div className="mb-4">
            <label className="uz-label" htmlFor="sd">{t('editor.shortDesc')}</label>
            <input id="sd" className="uz-input"
                   value={form.translations?.[locale]?.shortDescription || ''}
                   onChange={(e) => setTranslation('shortDescription', e.target.value)} />
          </div>
          <div>
            <label className="uz-label" htmlFor="de">{t('editor.desc')}</label>
            <textarea id="de" className="uz-input" rows={5} style={{ resize: 'vertical' }}
                      value={form.translations?.[locale]?.description || ''}
                      onChange={(e) => setTranslation('description', e.target.value)} />
          </div>
        </>
      )}

      {/* ----------------------------------------------------------- MEDIA */}
      {tab === 'media' && (
        <>
          <div className="uz-row mb-5">
            <div className="uz-col">
              <MediaField
                label={`${t('editor.poster')} — ${t('editor.posterDefault')}`}
                value={form.posterDefault}
                onChange={(id) => set({ posterDefault: id })}
              />
            </div>
            <div className="uz-col">
              <MediaField label={t('editor.cover')} value={form.cover}
                          onChange={(id) => set({ cover: id })} />
            </div>
          </div>

          <div className="uz-card p-4 mb-5">
            <GalleryField
              value={form.gallery}
              onChange={(gallery) => set({ gallery })}
            />
          </div>

          <div className="uz-card p-4">
            <div className="uz-h2 mb-1" style={{ fontSize: 15 }}>{t('editor.posterForLocale')}</div>
            <p className="uz-muted mb-4" style={{ fontSize: 12 }}>
              {t('editor.posterDefault')} — {t('common.none').toLowerCase()}
            </p>
            <div className="uz-row">
              {LOCALES.map((l) => {
                const code = toBackendLocale(l);
                return (
                  <div className="uz-col" key={l}>
                    <MediaField
                      label={l.toUpperCase()}
                      value={form.posterByLocale[code] || null}
                      onChange={(id) =>
                        set({ posterByLocale: { ...form.posterByLocale, [code]: id } })}
                    />
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}

      {/* ------------------------------------------------------- IJODKORLAR */}
      {tab === 'creators' && (
        <>
          <input className="uz-input mb-4" placeholder={t('editor.searchCreator')}
                 value={creatorQuery} onChange={(e) => setCreatorQuery(e.target.value)} />

          <div className="flex gap-2 flex-wrap mb-5">
            {filteredCreators.map((c) => (
              <button key={c.id} type="button" className="uz-chip"
                      onClick={() => set({
                        credits: [...form.credits,
                                  { creatorId: c.id, profession: 'ACTOR', characterName: '',
                                    sortOrder: form.credits.length }],
                      })}>
                + {creatorName(c)}
              </button>
            ))}
          </div>

          {form.credits.length === 0 ? (
            <p className="uz-muted" style={{ fontSize: 13 }}>{t('empty.body')}</p>
          ) : (
            form.credits.map((cr, i) => {
              const creator = creators.find((c) => c.id === cr.creatorId);
              return (
                <div key={i} className="uz-row mb-3 items-center">
                  <div className="uz-col" style={{ flex: '0 0 180px', fontWeight: 600, fontSize: 14 }}>
                    {creator ? creatorName(creator) : `#${cr.creatorId}`}
                  </div>
                  <div className="uz-col">
                    <select className="uz-select" value={cr.profession} aria-label={t('editor.profession')}
                            onChange={(e) => {
                              const next = [...form.credits];
                              next[i] = { ...cr, profession: e.target.value };
                              set({ credits: next });
                            }}>
                      {PROFESSIONS.map((p) => <option key={p} value={p}>{p}</option>)}
                    </select>
                  </div>
                  <div className="uz-col">
                    <input className="uz-input" placeholder={t('editor.characterName')}
                           value={cr.characterName || ''}
                           onChange={(e) => {
                             const next = [...form.credits];
                             next[i] = { ...cr, characterName: e.target.value };
                             set({ credits: next });
                           }} />
                  </div>
                  <button type="button" className="uz-btn uz-btn-danger" style={{ minHeight: 40 }}
                          onClick={() => set({ credits: form.credits.filter((_, x) => x !== i) })}>
                    {t('common.remove')}
                  </button>
                </div>
              );
            })
          )}
        </>
      )}

      {/* ------------------------------------------------- FASL VA QISMLAR */}
      {tab === 'episodes' && (
        <EpisodesTab
          contentId={contentId}
          structureType={form.structureType}
          contentAccessPolicy={form.accessPolicy}
        />
      )}

      {/* ---------------------------------------------------- MONETIZATSIYA */}
      {tab === 'access' && (
        <div className="uz-row">
          <div className="uz-col">
            <label className="uz-label" htmlFor="ap">{t('editor.accessPolicy')}</label>
            <select id="ap" className="uz-select" value={form.accessPolicy}
                    onChange={(e) => set({ accessPolicy: e.target.value })}>
              {POLICIES.map((x) => <option key={x} value={x}>{x.replace(/_/g, ' ')}</option>)}
            </select>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="pp">{t('editor.premierePrice')}</label>
            <input id="pp" className="uz-input" type="number" min="0" step="1000"
                   value={form.premierePrice} disabled={form.accessPolicy === 'FREE'}
                   onChange={(e) => set({ premierePrice: e.target.value })} />
          </div>
        </div>
      )}

      {/* ----------------------------------------------------------- NASHR */}
      {tab === 'publish' && (
        <div className="uz-row">
          <div className="uz-col">
            <label className="uz-label" htmlFor="stt">{t('editor.status')}</label>
            <select id="stt" className="uz-select" value={form.status}
                    onChange={(e) => set({ status: e.target.value })}>
              {STATUSES.map((x) => (
                <option key={x} value={x}
                        disabled={x === 'PUBLISHED' && !can('CONTENT_PUBLISH')}>
                  {x.replace(/_/g, ' ')}
                </option>
              ))}
            </select>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="pd">{t('editor.premiereDate')}</label>
            <input id="pd" className="uz-input" type="datetime-local" value={form.premiereDate}
                   onChange={(e) => set({ premiereDate: e.target.value })} />
          </div>
          <div className="uz-col" style={{ flexBasis: '100%' }}>
            <label className="uz-label" htmlFor="sl">{t('editor.slug')}</label>
            <input id="sl" className="uz-input" value={form.slug} placeholder="auto"
                   onChange={(e) => set({ slug: e.target.value })} />
            <p className="uz-muted mt-1" style={{ fontSize: 11 }}>{t('editor.slugHint')}</p>
          </div>
          <div style={{ flexBasis: '100%' }} className="flex gap-5 flex-wrap">
            <label className="uz-check">
              <input type="checkbox" checked={form.featured}
                     onChange={(e) => set({ featured: e.target.checked })} />
              {t('editor.featured')}
            </label>
            <label className="uz-check">
              <input type="checkbox" checked={form.popular}
                     onChange={(e) => set({ popular: e.target.checked })} />
              {t('editor.popular')}
            </label>
          </div>
        </div>
      )}
    </Modal>
  );
}
