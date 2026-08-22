import { useCallback, useEffect, useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import LocaleTabs from '../components/LocaleTabs';
import MediaField from '../components/MediaField';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, StatusBadge } from '../components/Ui';
import { LOCALES, toBackendLocale, usePanelI18n } from '../i18n';

const STATUSES = ['DRAFT', 'IN_REVIEW', 'SCHEDULED', 'PUBLISHED', 'ARCHIVED'];
const POLICIES = ['FREE', 'PREMIUM_ONLY', 'PURCHASE_ONLY', 'PREMIUM_OR_PURCHASE'];

const emptySeason = () => ({
  seasonNumber: 1, posterMediaId: null, premiereDate: '',
  status: 'DRAFT', sortOrder: null, translations: { UZ: {}, RU: {}, EN: {} },
});

const emptyEpisode = (seasonId) => ({
  seasonId: seasonId ?? '', episodeNumber: 1, thumbnailMediaId: null,
  durationSeconds: '', premiereDate: '', status: 'DRAFT',
  accessPolicyOverride: '', price: '', sortOrder: null,
  translations: { UZ: {}, RU: {}, EN: {} }, videos: [], version: null,
});

/**
 * Kontent muharriridagi «Fasl va qismlar» bo'limi.
 *
 * Modal ichida modal ochilmaydi — bu chalkash bo'lardi. Buning o'rniga bo'lim
 * ichida ko'rinish almashadi: ro'yxat ↔ forma, «qaytish» tugmasi bilan.
 *
 * Tuzilishga qarab boshqacha ishlaydi:
 *   SEASONAL — fasllar, har birining ichida qismlari;
 *   EPISODIC — faslsiz tekis qismlar ro'yxati;
 *   SINGLE   — umuman ko'rsatilmaydi.
 */
export default function EpisodesTab({ contentId, structureType, contentAccessPolicy }) {
  const { t, locale } = usePanelI18n();
  const { can } = useAuth();
  const bl = toBackendLocale(locale);
  const isSeasonal = structureType === 'SEASONAL';

  const [view, setView] = useState('list');
  const [seasons, setSeasons] = useState([]);
  const [episodes, setEpisodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const [seasonForm, setSeasonForm] = useState(emptySeason);
  const [seasonId, setSeasonId] = useState(null);
  const [episodeForm, setEpisodeForm] = useState(() => emptyEpisode(null));
  const [episodeId, setEpisodeId] = useState(null);
  const [formLocale, setFormLocale] = useState('UZ');

  const load = useCallback(() => {
    if (!contentId || structureType === 'SINGLE') {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    Promise.all([
      isSeasonal ? adminApi.seasons(contentId) : Promise.resolve([]),
      adminApi.episodes(contentId),
    ])
      .then(([s, e]) => {
        setSeasons(s);
        setEpisodes(e);
      })
      .catch(setError)
      .finally(() => setLoading(false));
  }, [contentId, structureType, isSeasonal]);

  useEffect(() => { load(); }, [load]);

  const titleOf = (row) =>
    row.translations?.[bl]?.title || row.translations?.UZ?.title ||
    Object.values(row.translations || {})[0]?.title || '—';

  // ------------------------------------------------------------- holatlar

  if (structureType === 'SINGLE') {
    return <EmptyState icon="🎬" title={t('ep.tab')} body={t('ep.singleHint')} />;
  }
  if (!contentId) {
    return <EmptyState icon="💾" title={t('ep.tab')} body={t('ep.saveFirst')} />;
  }
  if (loading) return <LoadingState rows={3} />;
  if (error && view === 'list') return <ErrorState error={error} onRetry={load} />;

  // ------------------------------------------------------------ fasl formasi

  if (view === 'season') {
    const setTr = (field, value) =>
      setSeasonForm((p) => ({
        ...p,
        translations: { ...p.translations, [formLocale]: { ...(p.translations[formLocale] || {}), [field]: value } },
      }));

    const saveSeason = async () => {
      if (!seasonForm.translations?.UZ?.title?.trim()) {
        setFormLocale('UZ');
        setError({ message: t('editor.uzRequired') });
        return;
      }
      setSaving(true);
      setError(null);
      const payload = {
        seasonNumber: Number(seasonForm.seasonNumber),
        posterMediaId: seasonForm.posterMediaId,
        premiereDate: seasonForm.premiereDate ? `${seasonForm.premiereDate}:00` : null,
        status: seasonForm.status,
        sortOrder: seasonForm.sortOrder,
        translations: seasonForm.translations,
      };
      try {
        if (seasonId) await adminApi.updateSeason(contentId, seasonId, payload);
        else await adminApi.createSeason(contentId, payload);
        setView('list');
        load();
      } catch (err) {
        setError(err);
      } finally {
        setSaving(false);
      }
    };

    return (
      <>
        <FormHeader
          title={seasonId ? t('ep.editSeason') : t('ep.newSeason')}
          onBack={() => { setView('list'); setError(null); }}
          onSave={saveSeason}
          saving={saving}
          error={error}
        />

        <div className="uz-row mb-4">
          <div className="uz-col">
            <label className="uz-label" htmlFor="s-num">{t('ep.seasonNumber')}</label>
            <input id="s-num" className="uz-input" type="number" min="1" value={seasonForm.seasonNumber}
                   onChange={(e) => setSeasonForm({ ...seasonForm, seasonNumber: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="s-st">{t('editor.status')}</label>
            <select id="s-st" className="uz-select" value={seasonForm.status}
                    onChange={(e) => setSeasonForm({ ...seasonForm, status: e.target.value })}>
              {STATUSES.map((x) => (
                <option key={x} value={x} disabled={x === 'PUBLISHED' && !can('CONTENT_PUBLISH')}>
                  {x.replace(/_/g, ' ')}
                </option>
              ))}
            </select>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="s-pd">{t('editor.premiereDate')}</label>
            <input id="s-pd" className="uz-input" type="datetime-local" value={seasonForm.premiereDate}
                   onChange={(e) => setSeasonForm({ ...seasonForm, premiereDate: e.target.value })} />
          </div>
        </div>

        <LocaleTabs active={formLocale} onChange={setFormLocale}
                    isFilled={(c) => Boolean(seasonForm.translations?.[c]?.title?.trim())} />

        <div className="mb-4">
          <label className="uz-label" htmlFor="s-ti">
            {t('editor.title')}{formLocale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
          </label>
          <input id="s-ti" className="uz-input" value={seasonForm.translations?.[formLocale]?.title || ''}
                 onChange={(e) => setTr('title', e.target.value)} />
        </div>
        <div className="mb-4">
          <label className="uz-label" htmlFor="s-de">{t('editor.desc')}</label>
          <textarea id="s-de" className="uz-input" rows={3} style={{ resize: 'vertical' }}
                    value={seasonForm.translations?.[formLocale]?.description || ''}
                    onChange={(e) => setTr('description', e.target.value)} />
        </div>

        <div style={{ maxWidth: 280 }}>
          <MediaField label={t('ep.poster')} value={seasonForm.posterMediaId}
                      onChange={(id) => setSeasonForm({ ...seasonForm, posterMediaId: id })} />
        </div>
      </>
    );
  }

  // ------------------------------------------------------------ qism formasi

  if (view === 'episode') {
    const setTr = (field, value) =>
      setEpisodeForm((p) => ({
        ...p,
        translations: { ...p.translations, [formLocale]: { ...(p.translations[formLocale] || {}), [field]: value } },
      }));

    const saveEpisode = async () => {
      if (!episodeForm.translations?.UZ?.title?.trim()) {
        setFormLocale('UZ');
        setError({ message: t('editor.uzRequired') });
        return;
      }
      setSaving(true);
      setError(null);
      const payload = {
        seasonId: isSeasonal ? Number(episodeForm.seasonId) || null : null,
        episodeNumber: Number(episodeForm.episodeNumber),
        thumbnailMediaId: episodeForm.thumbnailMediaId,
        durationSeconds: episodeForm.durationSeconds === '' ? null : Number(episodeForm.durationSeconds),
        premiereDate: episodeForm.premiereDate ? `${episodeForm.premiereDate}:00` : null,
        status: episodeForm.status,
        accessPolicyOverride: episodeForm.accessPolicyOverride || null,
        price: episodeForm.price === '' ? null : Number(episodeForm.price),
        sortOrder: episodeForm.sortOrder,
        translations: episodeForm.translations,
        videos: episodeForm.videos
          .filter((v) => v.mediaId)
          .map((v, i) => ({
            mediaId: v.mediaId,
            locale: v.locale || null,
            partNumber: Number(v.partNumber) || i + 1,
            sortOrder: i,
          })),
        version: episodeForm.version,
      };
      try {
        if (episodeId) await adminApi.updateEpisode(contentId, episodeId, payload);
        else await adminApi.createEpisode(contentId, payload);
        setView('list');
        load();
      } catch (err) {
        setError(err);
      } finally {
        setSaving(false);
      }
    };

    return (
      <>
        <FormHeader
          title={episodeId ? t('ep.editEpisode') : t('ep.newEpisode')}
          onBack={() => { setView('list'); setError(null); }}
          onSave={saveEpisode}
          saving={saving}
          error={error}
        />

        <div className="uz-row mb-4">
          {isSeasonal && (
            <div className="uz-col">
              <label className="uz-label" htmlFor="e-se">{t('ep.season')}</label>
              <select id="e-se" className="uz-select" value={episodeForm.seasonId}
                      onChange={(e) => setEpisodeForm({ ...episodeForm, seasonId: e.target.value })}>
                <option value="">{t('common.none')}</option>
                {seasons.map((s) => (
                  <option key={s.id} value={s.id}>{s.seasonNumber}. {titleOf(s)}</option>
                ))}
              </select>
            </div>
          )}
          <div className="uz-col">
            <label className="uz-label" htmlFor="e-num">{t('ep.episodeNumber')}</label>
            <input id="e-num" className="uz-input" type="number" min="1" value={episodeForm.episodeNumber}
                   onChange={(e) => setEpisodeForm({ ...episodeForm, episodeNumber: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="e-du">{t('ep.duration')}</label>
            <input id="e-du" className="uz-input" type="number" min="0" value={episodeForm.durationSeconds}
                   onChange={(e) => setEpisodeForm({ ...episodeForm, durationSeconds: e.target.value })} />
          </div>
        </div>

        <LocaleTabs active={formLocale} onChange={setFormLocale}
                    isFilled={(c) => Boolean(episodeForm.translations?.[c]?.title?.trim())} />

        <div className="mb-4">
          <label className="uz-label" htmlFor="e-ti">
            {t('editor.title')}{formLocale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
          </label>
          <input id="e-ti" className="uz-input" value={episodeForm.translations?.[formLocale]?.title || ''}
                 onChange={(e) => setTr('title', e.target.value)} />
        </div>
        <div className="mb-5">
          <label className="uz-label" htmlFor="e-sd">{t('editor.shortDesc')}</label>
          <input id="e-sd" className="uz-input"
                 value={episodeForm.translations?.[formLocale]?.shortDescription || ''}
                 onChange={(e) => setTr('shortDescription', e.target.value)} />
        </div>

        <div className="uz-row mb-5">
          <div className="uz-col" style={{ maxWidth: 260 }}>
            <MediaField label={t('ep.thumbnail')} value={episodeForm.thumbnailMediaId}
                        onChange={(id) => setEpisodeForm({ ...episodeForm, thumbnailMediaId: id })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="e-st">{t('editor.status')}</label>
            <select id="e-st" className="uz-select" value={episodeForm.status}
                    onChange={(e) => setEpisodeForm({ ...episodeForm, status: e.target.value })}>
              {STATUSES.map((x) => (
                <option key={x} value={x} disabled={x === 'PUBLISHED' && !can('CONTENT_PUBLISH')}>
                  {x.replace(/_/g, ' ')}
                </option>
              ))}
            </select>

            <label className="uz-label mt-4" htmlFor="e-ap">{t('ep.access')}</label>
            <select id="e-ap" className="uz-select" value={episodeForm.accessPolicyOverride}
                    onChange={(e) => setEpisodeForm({ ...episodeForm, accessPolicyOverride: e.target.value })}>
              <option value="">{t('ep.inherit', { policy: (contentAccessPolicy || 'FREE').replace(/_/g, ' ') })}</option>
              {POLICIES.map((x) => <option key={x} value={x}>{x.replace(/_/g, ' ')}</option>)}
            </select>

            <label className="uz-label mt-4" htmlFor="e-pr">{t('ep.price')}</label>
            <input id="e-pr" className="uz-input" type="number" min="0" step="1000" value={episodeForm.price}
                   onChange={(e) => setEpisodeForm({ ...episodeForm, price: e.target.value })} />
          </div>
        </div>

        {/* Video segmentlar — bir qism bir nechta fayldan iborat bo'lishi mumkin */}
        <div className="uz-card p-4">
          <div className="flex items-center justify-between gap-3 mb-1 flex-wrap">
            <div className="uz-h2" style={{ fontSize: 15 }}>{t('ep.videoParts')}</div>
            <button type="button" className="uz-btn uz-btn-ghost" style={{ minHeight: 34, fontSize: 13 }}
                    onClick={() => setEpisodeForm({
                      ...episodeForm,
                      videos: [...episodeForm.videos,
                               { mediaId: null, locale: '', partNumber: episodeForm.videos.length + 1 }],
                    })}>
              + {t('ep.addVideo')}
            </button>
          </div>
          <p className="uz-muted mb-4" style={{ fontSize: 12 }}>{t('ep.videoHint')}</p>

          {episodeForm.videos.length === 0 ? (
            <p className="uz-muted" style={{ fontSize: 13 }}>{t('empty.body')}</p>
          ) : (
            episodeForm.videos.map((v, i) => (
              <div key={i} className="uz-row mb-4 items-end">
                <div className="uz-col" style={{ flex: '0 0 90px' }}>
                  <label className="uz-label">{t('ep.partNumber')}</label>
                  <input className="uz-input" type="number" min="1" value={v.partNumber}
                         onChange={(e) => {
                           const next = [...episodeForm.videos];
                           next[i] = { ...v, partNumber: e.target.value };
                           setEpisodeForm({ ...episodeForm, videos: next });
                         }} />
                </div>
                <div className="uz-col" style={{ flex: '0 0 180px' }}>
                  <label className="uz-label">{t('ep.dubLocale')}</label>
                  <select className="uz-select" value={v.locale || ''}
                          onChange={(e) => {
                            const next = [...episodeForm.videos];
                            next[i] = { ...v, locale: e.target.value };
                            setEpisodeForm({ ...episodeForm, videos: next });
                          }}>
                    <option value="">{t('ep.allLocales')}</option>
                    {LOCALES.map((l) => (
                      <option key={l} value={toBackendLocale(l)}>{l.toUpperCase()}</option>
                    ))}
                  </select>
                </div>
                <div className="uz-col" style={{ maxWidth: 220 }}>
                  <MediaField label="" type="VIDEO" value={v.mediaId}
                              onChange={(id) => {
                                const next = [...episodeForm.videos];
                                next[i] = { ...v, mediaId: id };
                                setEpisodeForm({ ...episodeForm, videos: next });
                              }} />
                </div>
                <button type="button" className="uz-btn uz-btn-danger" style={{ minHeight: 40 }}
                        onClick={() => setEpisodeForm({
                          ...episodeForm,
                          videos: episodeForm.videos.filter((_, x) => x !== i),
                        })}>
                  {t('common.remove')}
                </button>
              </div>
            ))
          )}
        </div>
      </>
    );
  }

  // ------------------------------------------------------------------ ro'yxat

  const openSeason = (s) => {
    setSeasonId(s?.id ?? null);
    setFormLocale('UZ');
    setError(null);
    setSeasonForm(s
      ? {
          seasonNumber: s.seasonNumber,
          posterMediaId: s.posterMediaId,
          premiereDate: s.premiereDate ? s.premiereDate.slice(0, 16) : '',
          status: s.status,
          sortOrder: s.sortOrder,
          translations: { UZ: {}, RU: {}, EN: {}, ...(s.translations || {}) },
        }
      : { ...emptySeason(), seasonNumber: seasons.length + 1 });
    setView('season');
  };

  const openEpisode = (e, forSeasonId) => {
    setEpisodeId(e?.id ?? null);
    setFormLocale('UZ');
    setError(null);
    if (e) {
      setEpisodeForm({
        seasonId: e.seasonId ?? '',
        episodeNumber: e.episodeNumber,
        thumbnailMediaId: e.thumbnailMediaId,
        durationSeconds: e.durationSeconds ?? '',
        premiereDate: e.premiereDate ? e.premiereDate.slice(0, 16) : '',
        status: e.status,
        accessPolicyOverride: e.accessPolicyOverride || '',
        price: e.price ?? '',
        sortOrder: e.sortOrder,
        translations: { UZ: {}, RU: {}, EN: {}, ...(e.translations || {}) },
        videos: (e.videos || []).map((v) => ({
          mediaId: v.mediaId, locale: v.locale || '', partNumber: v.partNumber,
        })),
        version: e.version,
      });
    } else {
      const siblings = episodes.filter((x) =>
        isSeasonal ? x.seasonId === forSeasonId : !x.seasonId);
      setEpisodeForm({
        ...emptyEpisode(forSeasonId ?? ''),
        episodeNumber: siblings.length + 1,
      });
    }
    setView('episode');
  };

  const removeSeason = async (s) => {
    if (!window.confirm(`${s.seasonNumber}. ${titleOf(s)} — ${t('ep.deleteSeason')}?`)) return;
    try {
      await adminApi.deleteSeason(contentId, s.id);
      load();
    } catch (err) {
      setError(err);
    }
  };

  const removeEpisode = async (e) => {
    if (!window.confirm(`${e.episodeNumber}. ${titleOf(e)} — ${t('ep.deleteEpisode')}?`)) return;
    try {
      await adminApi.deleteEpisode(contentId, e.id);
      load();
    } catch (err) {
      setError(err);
    }
  };

  const EpisodeRow = ({ e }) => (
    <div className="flex items-center gap-3 py-2 flex-wrap"
         style={{ borderTop: '1px solid var(--p-border-soft)' }}>
      {e.thumbnailMediaId ? (
        <img src={mediaUrl(e.thumbnailMediaId)} alt="" loading="lazy"
             style={{ width: 56, height: 32, objectFit: 'cover', borderRadius: 6,
                      border: '1px solid var(--p-border)' }} />
      ) : (
        <div className="uz-skeleton" style={{ width: 56, height: 32 }} />
      )}
      <div style={{ flex: '1 1 160px', minWidth: 0 }}>
        <div style={{ fontWeight: 600, fontSize: 14 }}>
          {e.episodeNumber}. {titleOf(e)}
        </div>
        <div className="uz-muted" style={{ fontSize: 12 }}>
          {e.videos?.length || 0} × video
          {e.durationSeconds ? ` · ${Math.round(e.durationSeconds / 60)} min` : ''}
        </div>
      </div>
      <StatusBadge status={e.status} />
      <Badge tone={e.effectiveAccessPolicy === 'FREE' ? 'published' : 'gold'}>
        {e.effectiveAccessPolicy === 'FREE' ? t('common.free') : e.effectiveAccessPolicy.replace(/_/g, ' ')}
      </Badge>
      {can('CONTENT_EDIT') && (
        <button type="button" className="uz-btn uz-btn-ghost"
                style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                onClick={() => openEpisode(e)}>
          {t('common.edit')}
        </button>
      )}
      {can('CONTENT_DELETE') && (
        <button type="button" className="uz-btn uz-btn-danger"
                style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                onClick={() => removeEpisode(e)}>
          ✕
        </button>
      )}
    </div>
  );

  return (
    <>
      {error && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                      border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13 }}>
          {error.message}
        </div>
      )}

      {isSeasonal ? (
        <>
          <div className="flex items-center justify-between gap-3 mb-4 flex-wrap">
            <div className="uz-h2" style={{ fontSize: 15 }}>{t('ep.seasons')}</div>
            {can('CONTENT_CREATE') && (
              <button type="button" className="uz-btn uz-btn-primary"
                      style={{ minHeight: 36, fontSize: 13 }} onClick={() => openSeason(null)}>
                + {t('ep.newSeason')}
              </button>
            )}
          </div>

          {seasons.length === 0 ? (
            <EmptyState icon="📚" title={t('ep.noSeasons')} body="" />
          ) : (
            seasons.map((s) => {
              const own = episodes.filter((e) => e.seasonId === s.id)
                                  .sort((a, b) => a.episodeNumber - b.episodeNumber);
              return (
                <div key={s.id} className="uz-card p-4 mb-4">
                  <div className="flex items-center gap-3 mb-2 flex-wrap">
                    {s.posterMediaId && (
                      <img src={mediaUrl(s.posterMediaId)} alt="" loading="lazy"
                           style={{ width: 48, height: 28, objectFit: 'cover', borderRadius: 6 }} />
                    )}
                    <div style={{ flex: '1 1 140px', fontWeight: 650 }}>
                      {s.seasonNumber}. {titleOf(s)}
                    </div>
                    <StatusBadge status={s.status} />
                    <span className="uz-muted" style={{ fontSize: 12 }}>
                      {t('ep.episodeCount', { n: own.length })}
                    </span>
                    {can('CONTENT_EDIT') && (
                      <button type="button" className="uz-btn uz-btn-ghost"
                              style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                              onClick={() => openSeason(s)}>
                        {t('common.edit')}
                      </button>
                    )}
                    {can('CONTENT_CREATE') && (
                      <button type="button" className="uz-btn uz-btn-ghost"
                              style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                              onClick={() => openEpisode(null, s.id)}>
                        + {t('ep.newEpisode')}
                      </button>
                    )}
                    {can('CONTENT_DELETE') && (
                      <button type="button" className="uz-btn uz-btn-danger"
                              style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                              onClick={() => removeSeason(s)}>
                        ✕
                      </button>
                    )}
                  </div>
                  {own.length === 0
                    ? <p className="uz-muted" style={{ fontSize: 13 }}>{t('ep.noEpisodes')}</p>
                    : own.map((e) => <EpisodeRow key={e.id} e={e} />)}
                </div>
              );
            })
          )}
        </>
      ) : (
        <>
          <div className="flex items-center justify-between gap-3 mb-4 flex-wrap">
            <div className="uz-h2" style={{ fontSize: 15 }}>{t('ep.episodes')}</div>
            {can('CONTENT_CREATE') && (
              <button type="button" className="uz-btn uz-btn-primary"
                      style={{ minHeight: 36, fontSize: 13 }} onClick={() => openEpisode(null, null)}>
                + {t('ep.newEpisode')}
              </button>
            )}
          </div>

          {episodes.length === 0 ? (
            <EmptyState icon="🎞" title={t('ep.noEpisodes')} body="" />
          ) : (
            <div className="uz-card p-4">
              {[...episodes].sort((a, b) => a.episodeNumber - b.episodeNumber)
                .map((e) => <EpisodeRow key={e.id} e={e} />)}
            </div>
          )}
        </>
      )}
    </>
  );
}

/** Forma ko'rinishining yuqori qismi: qaytish + saqlash + xato. */
function FormHeader({ title, onBack, onSave, saving, error }) {
  const { t } = usePanelI18n();
  return (
    <>
      <div className="flex items-center justify-between gap-3 mb-4 flex-wrap">
        <button type="button" className="uz-btn uz-btn-ghost"
                style={{ minHeight: 34, fontSize: 13 }} onClick={onBack}>
          {t('ep.back')}
        </button>
        <div className="uz-h2" style={{ fontSize: 15, flex: '1 1 auto' }}>{title}</div>
        <button type="button" className="uz-btn uz-btn-primary"
                style={{ minHeight: 36, fontSize: 13 }} onClick={onSave} disabled={saving}>
          {saving ? t('common.saving') : t('common.save')}
        </button>
      </div>
      {error && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                      border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13 }}>
          {error.message}
        </div>
      )}
    </>
  );
}
