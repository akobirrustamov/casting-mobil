import { useEffect, useState } from 'react';
import { adminApi, mediaUrl } from '../../api/client';
import Modal from '../../components/Modal';
import { LoadingState } from '../../components/States';
import { Badge, SearchInput } from '../../components/Ui';
import { toBackendLocale, usePanelI18n } from '../../i18n';

/**
 * Qatorga qo'lda kontent tanlash (ТЗ §31 — «Custom content rows»).
 *
 * <h2>Bo'sh ro'yxat ham QAROR</h2>
 * Ro'yxat bo'sh bo'lsa bosh sahifa avtomatik qoidaga qaytadi: qator
 * kontent turi yoki `featured` / `popular` bayroqlari bo'yicha o'zi
 * to'ladi. Shuning uchun «Ro'yxatni tozalash» — buzuq holat emas, balki
 * «avtomatik qoidaga qaytar» degan amal, va u shunday tushuntiriladi.
 *
 * <h2>Nega tartib ro'yxatning o'zi</h2>
 * Backend `contentIds` ni KELGAN tartibda saqlaydi — alohida
 * `sortOrder` maydoni yo'q. Ya'ni ro'yxatdagi joylashuv bevosita bosh
 * sahifadagi joylashuv, oradagi tarjima yo'q.
 */
export default function SectionItemsModal({ section, onClose, onSaved }) {
  const { t, locale } = usePanelI18n();
  const bl = toBackendLocale(locale);

  const [items, setItems] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);

  const [q, setQ] = useState('');
  const [candidates, setCandidates] = useState([]);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    let cancelled = false;
    adminApi.homepageSectionItems(section.id)
      .then((rows) => { if (!cancelled) setItems(rows); })
      .catch((err) => { if (!cancelled) setLoadError(err); });
    return () => { cancelled = true; };
  }, [section.id]);

  useEffect(() => {
    if (!q.trim()) {
      setCandidates([]);
      return undefined;
    }
    let cancelled = false;
    setSearching(true);
    adminApi.content({ q: q.trim(), page: 0, size: 10 })
      .then((res) => { if (!cancelled) setCandidates(res.items || []); })
      .catch(() => { if (!cancelled) setCandidates([]); })
      .finally(() => { if (!cancelled) setSearching(false); });
    return () => { cancelled = true; };
  }, [q]);

  const titleOf = (c) =>
    c.translations?.[bl]?.title || c.translations?.UZ?.title || c.slug;

  const move = (index, delta) => {
    const next = [...items];
    const target = index + delta;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    setItems(next);
  };

  const add = (content) => {
    if (items.some((i) => i.id === content.id)) return;
    setItems([...items, content]);
    setQ('');
  };

  const save = async () => {
    setSaving(true);
    setSaveError(null);
    try {
      await adminApi.replaceHomepageSectionItems(section.id, items.map((i) => i.id));
      onSaved();
      onClose();
    } catch (err) {
      setSaveError(err);
    } finally {
      setSaving(false);
    }
  };

  const overLimit = section.itemLimit && items && items.length > section.itemLimit;

  return (
    <Modal
      open
      title={`${t('hp.itemsTitle')} — ${section.type}`}
      onClose={saving ? () => {} : onClose}
      width={760}
      footer={
        <>
          {saveError && (
            <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
              {saveError.message}
            </span>
          )}
          {items?.length > 0 && (
            <button type="button" className="uz-btn uz-btn-ghost"
                    style={{ marginRight: 'auto' }} disabled={saving}
                    onClick={() => setItems([])}>
              {t('hp.clearItems')}
            </button>
          )}
          <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose} disabled={saving}>
            {t('common.cancel')}
          </button>
          <button type="button" className="uz-btn uz-btn-primary" onClick={save}
                  disabled={saving || items === null}>
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <p className="uz-muted mb-4" style={{ fontSize: 12, lineHeight: 1.6 }}>
        {t('hp.itemsHint')}
      </p>

      {loadError ? (
        <p style={{ color: 'var(--p-danger)', fontSize: 13 }} role="alert">{loadError.message}</p>
      ) : items === null ? (
        <LoadingState rows={3} />
      ) : (
        <>
          <div className="flex items-center justify-between gap-3 mb-2 flex-wrap">
            <span className="uz-muted" style={{ fontSize: 12 }}>
              {t('hp.itemCount', { n: items.length })}
            </span>
            {overLimit && (
              <span style={{ fontSize: 12, color: 'var(--p-warning)' }}>
                {t('hp.limitWarn', { n: section.itemLimit })}
              </span>
            )}
          </div>

          {items.length === 0 ? (
            <p className="uz-muted mb-4" style={{ fontSize: 13 }}>{t('hp.itemsEmpty')}</p>
          ) : (
            <ol style={{ listStyle: 'none', margin: '0 0 20px', padding: 0 }}>
              {items.map((c, i) => (
                <li key={c.id} className="flex items-center gap-3 py-2"
                    style={{ borderTop: i === 0 ? 'none' : '1px solid var(--p-border-soft)' }}>
                  <span className="uz-mono uz-muted" style={{ width: 22, fontSize: 12 }}>
                    {i + 1}
                  </span>
                  <Thumb id={c.posterMediaId} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 600, fontSize: 13 }}>{titleOf(c)}</div>
                    <div className="uz-muted uz-mono" style={{ fontSize: 11 }}>{c.slug}</div>
                  </div>
                  {/* ⚠️ Nashr qilinmagan kontent bosh sahifada KO'RINMAYDI —
                      uni qatorga qo'shish mumkin, lekin admin buni
                      bilishi kerak, aks holda «qo'shdim, chiqmadi» degan
                      savol tug'ilardi. */}
                  {c.status !== 'PUBLISHED' && (
                    <Badge tone="draft">{c.status}</Badge>
                  )}
                  <button type="button" className="uz-icon-btn" onClick={() => move(i, -1)}
                          disabled={i === 0} title={t('hp.moveUp')} aria-label={t('hp.moveUp')}>
                    ↑
                  </button>
                  <button type="button" className="uz-icon-btn" onClick={() => move(i, 1)}
                          disabled={i === items.length - 1}
                          title={t('hp.moveDown')} aria-label={t('hp.moveDown')}>
                    ↓
                  </button>
                  <button type="button" className="uz-icon-btn uz-icon-btn-danger"
                          onClick={() => setItems(items.filter((x) => x.id !== c.id))}
                          title={t('hp.remove')} aria-label={t('hp.remove')}>
                    ✕
                  </button>
                </li>
              ))}
            </ol>
          )}

          <div className="uz-label">{t('hp.addContent')}</div>
          <SearchInput value={q} onChange={setQ} placeholder={t('hp.searchContent')} />

          {searching ? (
            <p className="uz-muted mt-2" style={{ fontSize: 12 }}>{t('common.loading')}</p>
          ) : q.trim() && candidates.length === 0 ? (
            <p className="uz-muted mt-2" style={{ fontSize: 12 }}>{t('common.noResults')}</p>
          ) : (
            candidates.map((c) => {
              const added = items.some((i) => i.id === c.id);
              return (
                <button key={c.id} type="button"
                        className="flex items-center gap-3 py-2 w-full"
                        style={{ background: 'none', border: 0, borderTop: '1px solid var(--p-border-soft)',
                                 textAlign: 'left', cursor: added ? 'default' : 'pointer',
                                 opacity: added ? 0.5 : 1, color: 'inherit' }}
                        disabled={added}
                        onClick={() => add(c)}>
                  <Thumb id={c.posterMediaId} />
                  <span style={{ flex: 1, minWidth: 0 }}>
                    <span style={{ display: 'block', fontWeight: 600, fontSize: 13 }}>{titleOf(c)}</span>
                    <span className="uz-muted uz-mono" style={{ display: 'block', fontSize: 11 }}>
                      {c.slug}
                    </span>
                  </span>
                  {added && <span className="uz-muted" style={{ fontSize: 11 }}>{t('hp.alreadyAdded')}</span>}
                </button>
              );
            })
          )}
        </>
      )}
    </Modal>
  );
}

function Thumb({ id }) {
  return (
    <span
      className="uz-thumb"
      style={{ width: 34, height: 46, flex: '0 0 auto', display: 'block',
               background: 'var(--p-surface-2)', borderRadius: 6, overflow: 'hidden' }}
      aria-hidden="true"
    >
      {id && (
        <img src={mediaUrl(id)} alt="" loading="lazy"
             style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
      )}
    </span>
  );
}
