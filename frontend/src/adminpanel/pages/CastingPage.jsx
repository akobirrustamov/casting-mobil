import { useMemo, useState } from 'react';
import { adminApi, BASE_URL } from '../api/client';
import { useApi } from '../api/useApi';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, SearchInput, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';
import { money } from '../utils/format';

/**
 * Kasting anketalari — bot, sayt va mobil ilova orqali kelganlarning HAMMASI.
 *
 * <h2>⚠️ Nega `/api/v1/casting-user`, `/web` emas (24.09.2026)</h2>
 * Ilgari sahifa `/web` ni o'qirdi — bu saytdagi OCHIQ katalog: faqat
 * «saytda ko'rsatish» yoqilgan anketalar va faqat vitrina maydonlari.
 * Yangi yuborilgan anketada bu belgi o'chiq bo'ladi, ya'ni admin panelda
 * nomzodlar umuman ko'rinmasdi. `GET /api/v1/casting-user` — xodimlar
 * uchun to'liq ro'yxat (SecurityConfig: STAFF), eski admin sayti ham
 * shundan foydalanadi.
 *
 * Eski endpoint o'zgartirilmaydi (ТЗ §49): qidiruv va filtr shu yerda,
 * brauzerda.
 */

const STATUS = {
  0: { key: 'new', tone: 'scheduled' },
  1: { key: 'accepted', tone: 'published' },
  2: { key: 'rejected', tone: 'blocked' },
};

const FILTERS = ['all', 'new', 'accepted', 'rejected'];

const photoUrl = (id) => `${BASE_URL}/api/v1/file/getFile/${id}`;

const date = (value) => (value ? String(value).slice(0, 10).split('-').reverse().join('.') : null);
const dateTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : null);

/** Yosh: bot anketasida `age` bor, ilovada faqat tug'ilgan sana. */
function ageOf(c) {
  if (c.age) return c.age;
  if (!c.birthday) return null;
  const b = new Date(c.birthday);
  if (Number.isNaN(b.getTime())) return null;
  const now = new Date();
  let age = now.getFullYear() - b.getFullYear();
  if (now < new Date(now.getFullYear(), b.getMonth(), b.getDate())) age -= 1;
  return age;
}

function statusOf(c) {
  return STATUS[c.status] || STATUS[0];
}

export default function CastingPage() {
  const { t } = usePanelI18n();
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [open, setOpen] = useState(null);

  const { data, error, loading, reload } = useApi(() => adminApi.castingApplications(), []);

  const all = useMemo(() => (Array.isArray(data) ? data : (data?.items || [])), [data]);

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return all.filter((c) => {
      if (filter !== 'all' && statusOf(c).key !== filter) return false;
      if (!q) return true;
      return [c.name, c.phone, c.region, c.email, c.telegram]
        .some((v) => v && String(v).toLowerCase().includes(q));
    });
  }, [all, query, filter]);

  const countBy = (key) => (key === 'all' ? all.length : all.filter((c) => statusOf(c).key === key).length);

  return (
    <>
      <PageHeader
        title={t('cs.title')}
        subtitle={t('cs.subtitle')}
        right={<SearchInput value={query} onChange={setQuery} placeholder={t('cs.search')} />}
      />
      <p className="uz-muted mb-4 text-sm">{t('cs.hint')}</p>

      <div className="flex flex-wrap gap-2 mb-4">
        {FILTERS.map((f) => (
          <button key={f} type="button"
                  className={`uz-btn ${filter === f ? 'uz-btn-primary' : 'uz-btn-ghost'}`}
                  style={{ minHeight: 32, padding: '0 14px', fontSize: 12 }}
                  onClick={() => setFilter(f)}>
            {t(`cs.filter.${f}`)} ({countBy(f)})
          </button>
        ))}
      </div>

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !rows.length ? <EmptyState icon="🎭" /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th style={{ width: 56 }} />
                  <th>{t('cs.name')}</th>
                  <th>{t('cs.type')}</th>
                  <th>{t('cs.age')}</th>
                  <th>{t('cs.region')}</th>
                  <th>{t('cs.phone')}</th>
                  <th>{t('cs.status')}</th>
                  <th>{t('cs.createdAt')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((c, i) => {
                  const s = statusOf(c);
                  const cover = c.photos?.[0]?.id;
                  return (
                    <tr key={c.id ?? i}>
                      <td>
                        {cover ? (
                          <img src={photoUrl(cover)} alt="" loading="lazy"
                               style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 8 }} />
                        ) : null}
                      </td>
                      <td>{c.name || '—'}</td>
                      <td className="uz-muted">{c.castingType || '—'}</td>
                      <td className="uz-muted">{ageOf(c) ?? '—'}</td>
                      <td className="uz-muted">{c.region || '—'}</td>
                      <td className="uz-muted uz-mono" style={{ fontSize: 12 }}>{c.phone || '—'}</td>
                      <td><Badge tone={s.tone}>{t(`cs.st.${s.key}`)}</Badge></td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>{dateTime(c.createdAt) || '—'}</td>
                      <td style={{ textAlign: 'right' }}>
                        <button type="button" className="uz-btn uz-btn-ghost"
                                style={{ minHeight: 32, padding: '0 14px', fontSize: 12 }}
                                onClick={() => setOpen(c)}>
                          {t('cs.details')}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>

      <CastingDetails app={open} onClose={() => setOpen(null)} />
    </>
  );
}

/** «Batafsil» — nomzod to'ldirgan hamma maydon va rasmlar. */
function CastingDetails({ app, onClose }) {
  const { t } = usePanelI18n();
  if (!app) return null;

  const s = statusOf(app);
  const rows = [
    ['castingType', app.castingType],
    ['gender', app.gender],
    ['birthday', date(app.birthday)],
    ['age', ageOf(app)],
    ['region', app.region],
    ['nationality', app.nationality],
    ['height', app.height ? `${app.height} sm` : null],
    ['hairColor', app.hairColor],
    ['eyeColor', app.eyeColor],
    ['clothSize', app.clothSize],
    ['shoeSize', app.shoeSize],
    ['bust', app.bust],
    ['waist', app.waist],
    ['son', app.son],
    ['phone', app.phone],
    ['email', app.email],
    ['telegram', app.telegram],
    ['facebook', app.facebook],
    ['instagram', app.instagram],
    ['price', app.price ? money(app.price) : null],
    ['webShow', app.isWebShow ? t('common.yes') : t('common.no')],
    ['createdAt', dateTime(app.createdAt)],
  ];

  return (
    <Modal open title={app.name || t('cs.details')} onClose={onClose} width={820}>
      <div className="flex items-center gap-2 mb-4">
        <Badge tone={s.tone}>{t(`cs.st.${s.key}`)}</Badge>
        <span className="uz-muted text-sm">#{app.id}</span>
      </div>

      <table className="uz-table mb-5">
        <tbody>
          {rows.map(([key, value]) => (
            <tr key={key}>
              <td className="uz-muted" style={{ width: 200 }}>{t(`cs.f.${key}`)}</td>
              <td style={{ userSelect: 'text' }}>
                {value === null || value === undefined || value === '' ? '—' : String(value)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="uz-h2 mb-3" style={{ fontSize: 15 }}>
        {t('cs.photos')} ({app.photos?.length || 0})
      </div>
      {app.photos?.length ? (
        <div className="flex flex-wrap gap-3">
          {app.photos.map((p) => (
            <a key={p.id} href={photoUrl(p.id)} target="_blank" rel="noreferrer">
              <img src={photoUrl(p.id)} alt="" loading="lazy"
                   style={{ width: 140, height: 180, objectFit: 'cover', borderRadius: 10 }} />
            </a>
          ))}
        </div>
      ) : (
        <p className="uz-muted text-sm">{t('cs.noPhotos')}</p>
      )}
    </Modal>
  );
}
