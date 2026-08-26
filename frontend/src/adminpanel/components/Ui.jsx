/**
 * Panelning umumiy kichik komponentlari.
 * Bir xil jadval/bejak/qidiruv mantiqini 20 marta nusxalamaslik uchun (§72).
 */
import { useEffect, useState } from 'react';
import { LOCALES, LOCALE_LABELS, usePanelI18n } from '../i18n';

export function PageHeader({ title, subtitle, right }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-4 mb-6">
      <div>
        <h1 className="uz-h1">{title}</h1>
        {subtitle && <p className="uz-muted text-sm mt-1">{subtitle}</p>}
      </div>
      {right && <div className="flex items-center gap-2 flex-wrap">{right}</div>}
    </div>
  );
}

const STATUS_CLASS = {
  PUBLISHED: 'uz-badge-published',
  DRAFT: 'uz-badge-draft',
  SCHEDULED: 'uz-badge-scheduled',
  ARCHIVED: 'uz-badge-archived',
  BLOCKED: 'uz-badge-blocked',
  IN_REVIEW: 'uz-badge-info',
};

export function StatusBadge({ status }) {
  return (
    <span className={`uz-badge ${STATUS_CLASS[status] || 'uz-badge-draft'}`}>
      {String(status || '').replace(/_/g, ' ')}
    </span>
  );
}

export function Badge({ tone = 'info', children }) {
  return <span className={`uz-badge uz-badge-${tone}`}>{children}</span>;
}

/** Har bosilishda so'rov yubormaslik uchun kechikish bilan (§96). */
export function SearchInput({ value, onChange, placeholder, delay = 400 }) {
  const [local, setLocal] = useState(value || '');

  useEffect(() => setLocal(value || ''), [value]);

  useEffect(() => {
    const id = setTimeout(() => {
      if (local !== value) onChange(local);
    }, delay);
    return () => clearTimeout(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [local]);

  return (
    <input
      type="search"
      className="uz-input"
      style={{ maxWidth: 280 }}
      value={local}
      placeholder={placeholder}
      aria-label={placeholder}
      onChange={(e) => setLocal(e.target.value)}
    />
  );
}

export function Pagination({ page, totalPages, onPage }) {
  const { t } = usePanelI18n();
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-end gap-3 p-4">
      <button
        type="button"
        className="uz-btn uz-btn-ghost"
        disabled={page <= 0}
        onClick={() => onPage(page - 1)}
      >
        {t('common.prev')}
      </button>
      <span className="uz-muted text-sm uz-mono">
        {t('common.page', { p: page + 1, t: totalPages })}
      </span>
      <button
        type="button"
        className="uz-btn uz-btn-ghost"
        disabled={page >= totalPages - 1}
        onClick={() => onPage(page + 1)}
      >
        {t('common.next')}
      </button>
    </div>
  );
}

/** Interfeys tilini almashtiradi va kontent tarjimasini ham shu til bo'yicha ko'rsatadi. */
export function LanguageSwitcher() {
  const { locale, setLocale, t } = usePanelI18n();
  return (
    <div className="flex items-center gap-1" role="group" aria-label={t('common.language')}>
      {LOCALES.map((l) => (
        <button
          key={l}
          type="button"
          onClick={() => setLocale(l)}
          aria-pressed={locale === l}
          title={LOCALE_LABELS[l]}
          className="uz-btn"
          style={{
            minHeight: 34,
            padding: '0 12px',
            fontSize: 12,
            background: locale === l ? 'var(--p-primary)' : 'transparent',
            borderColor: locale === l ? 'var(--p-primary)' : 'var(--p-border)',
            color: locale === l ? 'var(--on-brand)' : 'var(--p-muted)',
          }}
        >
          {l.toUpperCase()}
        </button>
      ))}
    </div>
  );
}

/** Keng jadvallar o'z ichida siljiydi - sahifa gorizontal siljimaydi. */
export function TableWrap({ children }) {
  return <div className="uz-table-wrap">{children}</div>;
}

/**
 * Saralanadigan ustun sarlavhasi (ТЗ §95).
 *
 * ⚠️ Nega alohida komponent. Saralash uch narsani birga talab qiladi:
 * bosilganda yo'nalishni almashtirish, faol ustunni ko'rsatish va
 * ekran o'quvchisi uchun holatni e'lon qilish. Buni har bir sahifada
 * qo'lda yozish §72 ogohlantirgan takrorlanish bo'lardi.
 *
 * @param field  bu ustunning backenddagi kaliti
 * @param sort   hozir saralanayotgan ustun
 * @param dir    `asc` yoki `desc`
 * @param onSort (field, dir) chaqiriladi
 */
export function SortableTh({ field, sort, dir, onSort, children }) {
  const active = sort === field;
  // Faol ustunni qayta bosish yo'nalishni almashtiradi. Yangi ustun
  // esa kamayish tartibida boshlanadi - ro'yxatlar odatda yangidan
  // eskiga qaraladi.
  const next = active && dir === 'desc' ? 'asc' : 'desc';

  return (
    // ⚠️ `aria-sort` `<th>` da bo'lishi SHART. Tugmada u e'tiborsiz
    // qoladi va ekran o'quvchisi ustun saralanganini aytmaydi.
    <th aria-sort={active ? (dir === 'asc' ? 'ascending' : 'descending') : 'none'}>
      <button
        type="button"
        onClick={() => onSort(field, next)}
        style={{
          background: 'none',
          border: 0,
          padding: 0,
          font: 'inherit',
          color: 'inherit',
          cursor: 'pointer',
          display: 'inline-flex',
          alignItems: 'center',
          gap: 4,
        }}>
        {children}
        <span aria-hidden="true" style={{ opacity: active ? 1 : 0.25, fontSize: 11 }}>
          {active && dir === 'asc' ? '▲' : '▼'}
        </span>
      </button>
    </th>
  );
}

/**
 * Saralash holati.
 *
 * ⚠️ Ustun o'zgarganda sahifa boshiga qaytariladi: 3-sahifada turib
 * saralash tartibini o'zgartirsangiz, o'sha sahifadagi qatorlar
 * butunlay boshqa bo'lib qoladi va bu chalkashtiradi.
 */
export function useSort(defaultField, defaultDir = 'desc', onChange) {
  const [sort, setSort] = useState(defaultField);
  const [dir, setDir] = useState(defaultDir);

  const apply = (field, direction) => {
    setSort(field);
    setDir(direction);
    if (onChange) onChange();
  };

  return { sort, dir, onSort: apply };
}
