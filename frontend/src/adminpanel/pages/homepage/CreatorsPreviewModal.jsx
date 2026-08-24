import { useEffect, useState } from 'react';
import { adminApi, mediaUrl } from '../../api/client';
import Modal from '../../components/Modal';
import { ErrorState, LoadingState } from '../../components/States';
import { Badge } from '../../components/Ui';
import { toBackendLocale, usePanelI18n } from '../../i18n';
import { count } from '../../utils/format';

/**
 * «Mashhur ijodkorlar» bo'limining haqiqiy mazmuni (ТЗ §25).
 *
 * <h2>Nega kerak</h2>
 * Bu bo'lim ijodkorlarni QO'LDA tanlamaydi — u `featured` bayrog'i va
 * `homepage.creators.ranking` sozlamasidan quriladi. Ilgari panelda
 * sozlama satri bor edi, mazmuni esa noma'lum: admin bo'lim bo'sh
 * chiqqanini faqat mobil ilovada ko'rardi.
 *
 * Bu oyna aynan backend qaytaradigan ro'yxatni ko'rsatadi, ya'ni
 * bayroq va tartib to'g'ri qo'yilganini shu yerda tekshirsa bo'ladi.
 * Tahrirlash esa o'z joyida — «Ijodkorlar» bo'limida.
 */
export default function CreatorsPreviewModal({ limit, onClose }) {
  const { t, locale } = usePanelI18n();
  const bl = toBackendLocale(locale);

  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    adminApi.homepageCreators(limit || undefined)
      .then((res) => { if (!cancelled) setRows(res); })
      .catch((err) => { if (!cancelled) setError(err); });
    return () => { cancelled = true; };
  }, [limit]);

  const nameOf = (c) => {
    const tr = c.translations?.[bl] || c.translations?.UZ;
    return tr?.displayName
      || [tr?.firstName, tr?.lastName].filter(Boolean).join(' ')
      || c.slug;
  };

  return (
    <Modal
      open
      title={t('hp.creatorsTitle')}
      onClose={onClose}
      width={640}
      footer={
        <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose}>
          {t('common.close')}
        </button>
      }
    >
      <p className="uz-muted mb-4" style={{ fontSize: 12, lineHeight: 1.6 }}>
        {t('hp.creatorsHint')}
      </p>

      {error ? (
        <ErrorState error={error} />
      ) : rows === null ? (
        <LoadingState rows={3} />
      ) : rows.length === 0 ? (
        <p className="uz-muted" style={{ fontSize: 13 }}>{t('hp.creatorsEmpty')}</p>
      ) : (
        <ol style={{ listStyle: 'none', margin: 0, padding: 0 }}>
          {rows.map((c, i) => (
            <li key={c.id} className="flex items-center gap-3 py-2"
                style={{ borderTop: i === 0 ? 'none' : '1px solid var(--p-border-soft)' }}>
              <span className="uz-mono uz-muted" style={{ width: 22, fontSize: 12 }}>{i + 1}</span>
              <span aria-hidden="true"
                    style={{ width: 34, height: 34, borderRadius: '50%', overflow: 'hidden',
                             background: 'var(--p-surface-2)', flex: '0 0 auto', display: 'block' }}>
                {c.photoMediaId && (
                  <img src={mediaUrl(c.photoMediaId)} alt="" loading="lazy"
                       style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                )}
              </span>
              <span style={{ flex: 1, minWidth: 0 }}>
                <span style={{ display: 'block', fontWeight: 600, fontSize: 13 }}>{nameOf(c)}</span>
                <span className="uz-muted uz-mono" style={{ display: 'block', fontSize: 11 }}>
                  {c.slug}
                </span>
              </span>
              {c.featured && <Badge tone="gold">{t('creators.featured')}</Badge>}
              <span className="uz-mono uz-muted" style={{ fontSize: 12 }}>
                ⭐ {count(c.starsReceived)}
              </span>
            </li>
          ))}
        </ol>
      )}
    </Modal>
  );
}
