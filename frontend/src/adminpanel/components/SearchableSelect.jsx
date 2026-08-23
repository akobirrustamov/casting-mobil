import { useEffect, useMemo, useRef, useState } from 'react';
import { usePanelI18n } from '../i18n';

/**
 * Qidiruvli tanlash (ТЗ §53).
 *
 * <h2>Nima uchun oddiy `<select>` yetarli emas</h2>
 * Kategoriya va ijodkor ro'yxatlari CHEKLANMAGAN. Ijodkorlar ayniqsa tez
 * o'sadi: har bir kino uchun aktyorlar, rejissyor, operator, ssenarist.
 *
 * Yuzlab elementli `<select>` da kerakli odamni topish uchun ro'yxatni
 * aylantirib chiqish kerak — brauzerning o'z qidiruvi esa faqat birinchi
 * harflar bo'yicha ishlaydi va u sahifa tilini bilmaydi.
 *
 * <h2>Nima uchun mahalliy filtr</h2>
 * Ro'yxat allaqachon yuklangan (ochiluvchi ro'yxat uchun to'liq kerak).
 * Har bosishda serverga so'rov yuborish kechikish qo'shardi va foyda
 * bermasdi.
 */
export default function SearchableSelect({
  value,
  options,
  onChange,
  placeholder,
  emptyLabel,
  ariaLabel,
  invalid = false,
}) {
  const { t } = usePanelI18n();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const boxRef = useRef(null);

  const selected = options.find((o) => String(o.id) === String(value));

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return options;
    return options.filter((o) => (o.label || '').toLowerCase().includes(q));
  }, [options, query]);

  // Tashqariga bosilganda yopiladi — aks holda ro'yxat ochiq qolib,
  // ostidagi elementlarni to'sib turardi.
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e) => {
      if (boxRef.current && !boxRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    const onKey = (e) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const pick = (id) => {
    onChange(id);
    setOpen(false);
    setQuery('');
  };

  return (
    <div ref={boxRef} style={{ position: 'relative' }}>
      <button
        type="button"
        className={`uz-select uz-select-button ${invalid ? 'invalid' : ''}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={ariaLabel}
        /* ⚠️ `aria-invalid` tugmada qo'llab-quvvatlanmaydi — uning
           o'rniga vizual belgi va `aria-describedby` ishlatiladi. */
        data-invalid={invalid || undefined}
        onClick={() => setOpen((v) => !v)}
      >
        {selected ? selected.label : (
          <span className="uz-muted">{placeholder || t('common.selectPlaceholder')}</span>
        )}
      </button>

      {open && (
        <div className="uz-dropdown" role="listbox">
          <input
            className="uz-input"
            value={query}
            autoFocus
            placeholder={t('common.search')}
            onChange={(e) => setQuery(e.target.value)}
          />

          <div className="uz-dropdown-list">
            {/* Tanlovni BEKOR qilish imkoniyati: kategoriya ixtiyoriy va
                uni olib tashlash yo'li bo'lishi kerak. */}
            {emptyLabel && (
              <button type="button" className="uz-dropdown-item" onClick={() => pick(null)}>
                <span className="uz-muted">{emptyLabel}</span>
              </button>
            )}

            {filtered.length === 0 ? (
              <div className="uz-dropdown-item uz-muted">{t('common.noResults')}</div>
            ) : filtered.map((o) => (
              <button
                key={o.id}
                type="button"
                role="option"
                aria-selected={String(o.id) === String(value)}
                className={`uz-dropdown-item ${String(o.id) === String(value) ? 'selected' : ''}`}
                onClick={() => pick(o.id)}
              >
                {o.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
