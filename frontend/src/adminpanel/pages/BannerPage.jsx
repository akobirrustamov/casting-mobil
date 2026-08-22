import { useEffect, useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import ConfirmDialog, { useConfirm } from '../components/ConfirmDialog';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import LinkFields from '../components/LinkFields';
import LocaleTabs from '../components/LocaleTabs';
import MediaField from '../components/MediaField';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, StatusBadge, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

const STATUSES = ['DRAFT', 'SCHEDULED', 'PUBLISHED', 'ARCHIVED'];

const emptyAd = () => ({
  name: '', imageMediaId: null, mobileImageMediaId: null,
  buttonEnabled: false, link: { linkType: 'NONE' },
  audience: 'ADVERTISEMENT', status: 'DRAFT',
  startAt: '', endAt: '', sortOrder: 0,
  translations: { UZ: {}, RU: {}, EN: {} },
});

const emptyPremiere = () => ({
  name: '', imageMediaId: null, videoMediaId: null, contentId: '',
  buttonEnabled: true, link: { linkType: 'NONE' },
  status: 'DRAFT', startAt: '', endAt: '', sortOrder: 0,
  translations: { UZ: {}, RU: {}, EN: {} },
});

/**
 * Reklama bannerlari va premyera kartochkalari.
 *
 * Ikkalasi bitta komponentda: maydonlari deyarli bir xil (rasm, matn, havola,
 * vaqt oynasi, tartib). Farqi — reklamada auditoriya, premyerada video va
 * bog'langan kontent.
 */
export default function BannerPage({ kind }) {
  const { t, locale } = usePanelI18n();
  const { can } = useAuth();
  const isAd = kind === 'ad';
  const bl = locale.toUpperCase();

  const perm = isAd ? 'ADVERTISEMENT' : 'PREMIERE';
  const confirmer = useConfirm(() => reload());

  const { data, error, loading, reload } = useApi(
    () => (isAd ? adminApi.advertisements() : adminApi.premieres()),
    [kind]
  );

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(isAd ? emptyAd : emptyPremiere);
  const [formLocale, setFormLocale] = useState('UZ');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [contents, setContents] = useState([]);

  // Premyerada kontent tanlanadi — ro'yxatni oldindan olamiz
  useEffect(() => {
    if (!isAd && open) {
      adminApi.content({ size: 100 })
        .then((r) => setContents(r.items || []))
        .catch(() => setContents([]));
    }
  }, [isAd, open]);

  const titleOf = (row) =>
    row.translations?.[bl]?.title || row.translations?.UZ?.title || row.name;

  const openForm = (row) => {
    setEditing(row);
    setFormLocale('UZ');
    setSaveError(null);
    if (!row) {
      setForm(isAd ? emptyAd() : emptyPremiere());
    } else {
      const base = {
        name: row.name || '',
        imageMediaId: row.imageMediaId ?? null,
        buttonEnabled: Boolean(row.buttonEnabled),
        link: row.link || { linkType: 'NONE' },
        status: row.status,
        startAt: row.startAt ? row.startAt.slice(0, 16) : '',
        endAt: row.endAt ? row.endAt.slice(0, 16) : '',
        sortOrder: row.sortOrder ?? 0,
        translations: { UZ: {}, RU: {}, EN: {}, ...(row.translations || {}) },
      };
      setForm(isAd
        ? { ...base, mobileImageMediaId: row.mobileImageMediaId ?? null, audience: row.audience }
        : { ...base, videoMediaId: row.videoMediaId ?? null, contentId: row.contentId ?? '' });
    }
    setOpen(true);
  };

  const setTr = (field, value) =>
    setForm((p) => ({
      ...p,
      translations: {
        ...p.translations,
        [formLocale]: { ...(p.translations[formLocale] || {}), [field]: value },
      },
    }));

  const save = async () => {
    if (!form.name?.trim()) {
      setSaveError({ message: t('ads.name') + ' — ' + t('common.required') });
      return;
    }
    if (!isAd && !form.translations?.UZ?.title?.trim()) {
      setFormLocale('UZ');
      setSaveError({ message: t('editor.uzRequired') });
      return;
    }
    setSaving(true);
    setSaveError(null);

    const payload = {
      name: form.name.trim(),
      imageMediaId: form.imageMediaId,
      buttonEnabled: form.buttonEnabled,
      link: form.link,
      status: form.status,
      startAt: form.startAt ? `${form.startAt}:00` : null,
      endAt: form.endAt ? `${form.endAt}:00` : null,
      sortOrder: Number(form.sortOrder) || 0,
      translations: form.translations,
      ...(isAd
        ? { mobileImageMediaId: form.mobileImageMediaId, audience: form.audience }
        : { videoMediaId: form.videoMediaId,
            contentId: form.contentId === '' ? null : Number(form.contentId) }),
    };

    try {
      if (editing) {
        await (isAd ? adminApi.updateAd(editing.id, payload)
                    : adminApi.updatePremiere(editing.id, payload));
      } else {
        await (isAd ? adminApi.createAd(payload) : adminApi.createPremiere(payload));
      }
      setOpen(false);
      reload();
    } catch (err) {
      setSaveError(err);
    } finally {
      setSaving(false);
    }
  };

  const remove = (row) => confirmer.ask({
    message: `${row.name} — ${t('common.remove')}?`,
    confirmLabel: t('common.remove'),
    run: async () => {
      try {
        await (isAd ? adminApi.deleteAd(row.id) : adminApi.deletePremiere(row.id));
      } catch (err) {
        setSaveError(err);
        throw err;
      }
    },
  });

  return (
    <>
      <PageHeader
        title={isAd ? t('ads.title') : t('pr.title')}
        subtitle={isAd ? t('ads.subtitle') : t('pr.subtitle')}
        right={can(`${perm}_CREATE`) && (
          <button type="button" className="uz-btn uz-btn-primary" onClick={() => openForm(null)}>
            + {isAd ? t('ads.new') : t('pr.new')}
          </button>
        )}
      />

      {saveError && !open && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                      border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13 }}>
          {saveError.message}
        </div>
      )}

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !data?.length ? <EmptyState icon={isAd ? '📢' : '🎬'} /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th style={{ width: 80 }} />
                  <th>{t('ads.name')}</th>
                  {isAd && <th>{t('ads.audience')}</th>}
                  <th>{t('content.col.status')}</th>
                  <th>{t('ads.window')}</th>
                  <th>{t('common.sortOrder')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.map((row) => (
                  <tr key={row.id}>
                    <td>
                      {row.imageMediaId ? (
                        <img src={mediaUrl(row.imageMediaId)} alt="" loading="lazy"
                             style={{ width: 64, height: 36, objectFit: 'cover',
                                      borderRadius: 6, border: '1px solid var(--p-border)' }} />
                      ) : <div className="uz-skeleton" style={{ width: 64, height: 36 }} />}
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{titleOf(row)}</div>
                      <div className="uz-muted" style={{ fontSize: 12 }}>{row.name}</div>
                    </td>
                    {isAd && (
                      <td>
                        <Badge tone={row.audience === 'ADMIN_ANNOUNCEMENT' ? 'info' : 'gold'}>
                          {row.audience === 'ADMIN_ANNOUNCEMENT'
                            ? t('ads.audienceAnnouncement').split('—')[0].trim()
                            : t('ads.audienceAd').split('—')[0].trim()}
                        </Badge>
                      </td>
                    )}
                    <td>
                      <StatusBadge status={row.status} />
                      <div style={{ marginTop: 4 }}>
                        <Badge tone={row.live ? 'published' : 'draft'}>
                          {row.live ? t('ads.live') : t('ads.notLive')}
                        </Badge>
                      </div>
                    </td>
                    <td className="uz-muted" style={{ fontSize: 12 }}>
                      {row.startAt ? row.startAt.slice(0, 10) : '—'}
                      {' → '}
                      {row.endAt ? row.endAt.slice(0, 10) : '∞'}
                    </td>
                    <td className="uz-mono uz-muted">{row.sortOrder}</td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      {can(`${perm}_EDIT`) && (
                        <button type="button" className="uz-btn uz-btn-ghost"
                                style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                                onClick={() => openForm(row)}>
                          {t('common.edit')}
                        </button>
                      )}
                      {can(`${perm}_DELETE`) && (
                        <button type="button" className="uz-btn uz-btn-danger"
                                style={{ minHeight: 32, padding: '0 12px', fontSize: 12, marginLeft: 8 }}
                                onClick={() => remove(row)}>
                          ✕
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>

      <Modal
        open={open}
        title={editing ? (isAd ? t('ads.edit') : t('pr.edit')) : (isAd ? t('ads.new') : t('pr.new'))}
        onClose={() => setOpen(false)}
        width={760}
        footer={
          <>
            {saveError && (
              <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
                {saveError.message}
              </span>
            )}
            <button type="button" className="uz-btn uz-btn-ghost" onClick={() => setOpen(false)}>
              {t('common.cancel')}
            </button>
            <button type="button" className="uz-btn uz-btn-primary" onClick={save} disabled={saving}>
              {saving ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <div className="uz-row mb-4">
          <div className="uz-col" style={{ flexBasis: '100%' }}>
            <label className="uz-label" htmlFor="b-name">{t('ads.name')}</label>
            <input id="b-name" className="uz-input" value={form.name}
                   onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <p className="uz-muted mt-1" style={{ fontSize: 11 }}>{t('ads.nameHint')}</p>
          </div>

          {isAd && (
            <div className="uz-col" style={{ flexBasis: '100%' }}>
              <label className="uz-label" htmlFor="b-aud">{t('ads.audience')}</label>
              <select id="b-aud" className="uz-select" value={form.audience}
                      onChange={(e) => setForm({ ...form, audience: e.target.value })}>
                <option value="ADVERTISEMENT">{t('ads.audienceAd')}</option>
                <option value="ADMIN_ANNOUNCEMENT">{t('ads.audienceAnnouncement')}</option>
              </select>
            </div>
          )}

          {!isAd && (
            <div className="uz-col" style={{ flexBasis: '100%' }}>
              <label className="uz-label" htmlFor="b-content">{t('pr.content')}</label>
              <select id="b-content" className="uz-select" value={form.contentId}
                      onChange={(e) => setForm({ ...form, contentId: e.target.value })}>
                <option value="">{t('common.none')}</option>
                {contents.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.translations?.UZ?.title || c.slug}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        <LocaleTabs active={formLocale} onChange={setFormLocale}
                    isFilled={(c) => Boolean(form.translations?.[c]?.title?.trim())} />

        <div className="mb-4">
          <label className="uz-label" htmlFor="b-ti">
            {t('editor.title')}
            {!isAd && formLocale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
          </label>
          <input id="b-ti" className="uz-input" value={form.translations?.[formLocale]?.title || ''}
                 onChange={(e) => setTr('title', e.target.value)} />
        </div>

        {!isAd && (
          <div className="mb-4">
            <label className="uz-label" htmlFor="b-sub">{t('pr.subtitleField')}</label>
            <input id="b-sub" className="uz-input" placeholder={t('pr.subtitleHint')}
                   value={form.translations?.[formLocale]?.subtitle || ''}
                   onChange={(e) => setTr('subtitle', e.target.value)} />
          </div>
        )}

        <div className="mb-4">
          <label className="uz-label" htmlFor="b-desc">{t('editor.desc')}</label>
          <textarea id="b-desc" className="uz-input" rows={2} style={{ resize: 'vertical' }}
                    value={form.translations?.[formLocale]?.description || ''}
                    onChange={(e) => setTr('description', e.target.value)} />
        </div>

        <div className="uz-card p-4 mb-4">
          <label className="uz-check mb-2">
            <input type="checkbox" checked={form.buttonEnabled}
                   onChange={(e) => setForm({ ...form, buttonEnabled: e.target.checked })} />
            {t('ads.buttonEnabled')}
          </label>
          {form.buttonEnabled && (
            <div>
              <label className="uz-label" htmlFor="b-btxt">{t('ads.buttonText')}</label>
              <input id="b-btxt" className="uz-input"
                     value={form.translations?.[formLocale]?.buttonText || ''}
                     onChange={(e) => setTr('buttonText', e.target.value)} />
            </div>
          )}
        </div>

        <div className="mb-4">
          <LinkFields value={form.link} onChange={(link) => setForm({ ...form, link })} />
        </div>

        <div className="uz-row mb-4">
          <div className="uz-col" style={{ maxWidth: 260 }}>
            <MediaField label={t('ads.image')} value={form.imageMediaId}
                        onChange={(id) => setForm({ ...form, imageMediaId: id })} />
          </div>
          {isAd ? (
            <div className="uz-col" style={{ maxWidth: 260 }}>
              <MediaField label={t('ads.mobileImage')} value={form.mobileImageMediaId}
                          hint={t('ads.mobileHint')}
                          onChange={(id) => setForm({ ...form, mobileImageMediaId: id })} />
            </div>
          ) : (
            <div className="uz-col" style={{ maxWidth: 260 }}>
              <MediaField label={t('pr.video')} type="VIDEO" value={form.videoMediaId}
                          onChange={(id) => setForm({ ...form, videoMediaId: id })} />
            </div>
          )}
        </div>

        <div className="uz-row">
          <div className="uz-col">
            <label className="uz-label" htmlFor="b-st">{t('editor.status')}</label>
            <select id="b-st" className="uz-select" value={form.status}
                    onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {STATUSES.map((x) => (
                <option key={x} value={x} disabled={x === 'PUBLISHED' && !can('CONTENT_PUBLISH')}>
                  {x}
                </option>
              ))}
            </select>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="b-sa">{t('ads.startAt')}</label>
            <input id="b-sa" className="uz-input" type="datetime-local" value={form.startAt}
                   onChange={(e) => setForm({ ...form, startAt: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="b-ea">{t('ads.endAt')}</label>
            <input id="b-ea" className="uz-input" type="datetime-local" value={form.endAt}
                   onChange={(e) => setForm({ ...form, endAt: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="b-so">{t('common.sortOrder')}</label>
            <input id="b-so" className="uz-input" type="number" value={form.sortOrder}
                   onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
          </div>
        </div>
      </Modal>
      <ConfirmDialog {...confirmer.props} />
    </>
  );
}
