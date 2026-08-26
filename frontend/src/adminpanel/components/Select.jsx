import { Children, Fragment, isValidElement, useEffect, useId, useMemo, useRef, useState } from 'react';
import { usePanelI18n } from '../i18n';

/**
 * Qidiruvli tanlash — nativ `<select>` ning o'rnini bosadi.
 *
 * <h2>Nima uchun nativ `<select>` emas</h2>
 * Nativ ro'yxatda qidiruv yo'q: brauzerning o'z tugmacha-qidiruvi faqat
 * BIRINCHI harflardan boshlab ishlaydi. "Rejissyor" ni topish uchun "r"
 * bosish kerak, "yor" bo'yicha topilmaydi. Ijodkorlar, kontentlar va
 * tariflar ro'yxati esa cheklanmagan darajada o'sadi.
 *
 * <h2>Nima uchun API nativ `<select>` bilan bir xil</h2>
 * `value` + `onChange(e)` + `<option>` bolalari. Ya'ni mavjud 30 dan ortiq
 * chaqiruv joyida faqat teg nomi o'zgaradi, ichidagi mantiq — `e.target.value`,
 * `.map(...)` bilan yasalgan `<option>` lar — teginilmaydi.
 *
 * ⚠️ `onChange` ga NATIV hodisa emas, sintetik obyekt uziladi:
 * `{ target: { value, name } }`. `value` HAR DOIM satr — xuddi nativ
 * `<select>` dagidek. Aks holda `<option value={5}>` raqam qaytarardi va
 * chaqiruv joylaridagi `Number(e.target.value)` mantig'i buzilardi.
 *
 * <h2>Qidiruv maydoni qachon ko'rinadi</h2>
 * Variantlar soni {@code searchThreshold} dan OSHGANDA. 2-3 variantli
 * ro'yxatda ("Faol / Nofaol") qidiruv maydoni foydasiz: butun ro'yxat
 * allaqachon ko'z oldida, u faqat ortiqcha tugma bosish qo'shadi.
 * Kerak bo'lsa har bir joyda alohida `searchThreshold={0}` beriladi.
 */
export default function Select({
  value,
  onChange,
  children,
  id,
  name,
  className = 'uz-select',
  style,
  disabled = false,
  placeholder,
  searchThreshold = 7,
  'aria-label': ariaLabel,
  'aria-invalid': ariaInvalid,
  'aria-describedby': ariaDescribedBy,
}) {
  const { t } = usePanelI18n();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [active, setActive] = useState(0);
  const [dropUp, setDropUp] = useState(false);

  const boxRef = useRef(null);
  const buttonRef = useRef(null);
  const listRef = useRef(null);

  const reactId = useId();
  const listId = `${id || reactId}-listbox`;

  const options = useMemo(() => collectOptions(children), [children]);
  const searchable = options.length > searchThreshold;

  const selected = options.find((o) => String(o.value) === String(value ?? ''));

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return options;
    // Qiymat bo'yicha ham qidiriladi: enum kodlari ("MOVIE", "PUBLISHED")
    // ko'pincha yorliqdan ko'ra tezroq yoziladi.
    return options.filter(
      (o) => o.label.toLowerCase().includes(q) || String(o.value).toLowerCase().includes(q),
    );
  }, [options, query]);

  // Pastda joy yetmasa ro'yxat YUQORIGA ochiladi.
  //
  // ⚠️ Usiz uzun formaning oxiridagi tanlash (masalan `ContentEditor`
  // ning pastki maydonlari) ro'yxatni ekrandan tashqariga chizardi va
  // admin uni sahifani aylantirmasdan ko'ra olmasdi.
  useEffect(() => {
    if (!open || !buttonRef.current) return;
    const rect = buttonRef.current.getBoundingClientRect();
    const NEEDED = 300;                       // qidiruv maydoni + 240px ro'yxat
    setDropUp(window.innerHeight - rect.bottom < NEEDED && rect.top > NEEDED);
  }, [open]);

  // Ro'yxat ochilganda kursor TANLANGAN variantda turadi, birinchisida emas.
  // Aks holda uzun ro'yxatda pastga tushirish har safar boshidan boshlanardi.
  useEffect(() => {
    if (!open) return;
    const i = filtered.findIndex((o) => String(o.value) === String(value ?? ''));
    setActive(i >= 0 ? i : firstEnabled(filtered));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // Filtr qisqarganda kursor ro'yxatdan tashqarida qolib ketmasin.
  //
  // ⚠️ Bu ATAYLAB effekt EMAS, balki render paytida hisoblanadi.
  // Effekt bo'lganda ro'yxat bir marta noto'g'ri kursor bilan
  // chizilib, keyin ikkinchi render'da tuzatilardi.
  const activeIdx = filtered.length === 0 ? 0 : Math.min(active, filtered.length - 1);

  // Kursor ko'rinish maydonidan chiqib ketmasin — 240px lik ro'yxatda
  // klaviatura bilan pastga tushganda element ekranda qolishi kerak.
  useEffect(() => {
    if (!open || !listRef.current) return;
    const el = listRef.current.querySelector('[data-active="true"]');
    if (el && el.scrollIntoView) el.scrollIntoView({ block: 'nearest' });
  }, [activeIdx, open]);

  // Tashqariga bosilganda yopiladi — aks holda ochiq ro'yxat ostidagi
  // elementlarni to'sib turardi.
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e) => {
      if (boxRef.current && !boxRef.current.contains(e.target)) close(false);
    };
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function close(refocus = true) {
    setOpen(false);
    setQuery('');
    // Fokus tugmaga QAYTADI. Aks holda ro'yxat yopilgach fokus
    // `<body>` ga tushib, Tab bosilganda sahifa boshidan boshlanardi.
    if (refocus && buttonRef.current) buttonRef.current.focus();
  }

  function pick(option) {
    if (!option || option.disabled) return;
    close();
    if (String(option.value) !== String(value ?? '')) {
      const synthetic = { target: { value: String(option.value), name }, currentTarget: { value: String(option.value), name } };
      onChange(synthetic);
    }
  }

  function move(step) {
    setActive(() => {
      const i = activeIdx;
      let next = i;
      for (let n = 0; n < filtered.length; n += 1) {
        next += step;
        if (next < 0) next = filtered.length - 1;
        if (next >= filtered.length) next = 0;
        if (!filtered[next]?.disabled) return next;
      }
      return i;
    });
  }

  function onKeyDown(e) {
    if (!open) {
      if (e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        setOpen(true);
      }
      return;
    }
    if (e.key === 'ArrowDown') { e.preventDefault(); move(1); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); move(-1); }
    else if (e.key === 'Home') { e.preventDefault(); setActive(firstEnabled(filtered)); }
    else if (e.key === 'End') { e.preventDefault(); setActive(filtered.length - 1); }
    else if (e.key === 'Enter') { e.preventDefault(); pick(filtered[activeIdx]); }
    else if (e.key === 'Escape') { e.preventDefault(); close(); }
    else if (e.key === 'Tab') { close(false); }
  }

  return (
    <div ref={boxRef} className="uz-select-wrap" style={style}>
      <button
        ref={buttonRef}
        type="button"
        id={id}
        role="combobox"
        className={`${className} uz-select-button ${ariaInvalid ? 'invalid' : ''}`}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        aria-activedescendant={open && filtered[activeIdx] ? `${listId}-${activeIdx}` : undefined}
        aria-label={ariaLabel}
        aria-invalid={ariaInvalid}
        aria-describedby={ariaDescribedBy}
        onClick={() => (open ? close() : setOpen(true))}
        onKeyDown={searchable ? undefined : onKeyDown}
      >
        <span className="uz-select-value">
          {selected ? selected.label : (
            <span className="uz-muted">{placeholder || t('common.selectPlaceholder')}</span>
          )}
        </span>
        <span className="uz-select-caret" aria-hidden="true">▾</span>
      </button>

      {open && (
        <div className={`uz-dropdown${dropUp ? ' uz-dropdown-up' : ''}`}>
          {searchable && (
            <input
              className="uz-input"
              value={query}
              autoFocus
              placeholder={t('common.search')}
              aria-label={t('common.search')}
              aria-controls={listId}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={onKeyDown}
            />
          )}

          <div
            ref={listRef}
            id={listId}
            role="listbox"
            className="uz-dropdown-list"
            aria-label={ariaLabel}
          >
            {filtered.length === 0 ? (
              <div className="uz-dropdown-item uz-muted">{t('common.noResults')}</div>
            ) : filtered.map((o, i) => (
              <div
                key={`${o.value}`}
                id={`${listId}-${i}`}
                role="option"
                aria-selected={String(o.value) === String(value ?? '')}
                aria-disabled={o.disabled || undefined}
                data-active={i === activeIdx}
                className={`uz-dropdown-item${String(o.value) === String(value ?? '') ? ' selected' : ''}`
                  + (i === activeIdx ? ' active' : '')
                  + (o.disabled ? ' disabled' : '')}
                onMouseEnter={() => setActive(i)}
                /* ⚠️ Bosilganda fokus KO'CHMASIN. Bu `<div>` fokus
                   olmaydi, shuning uchun brauzer fokusni `<body>` ga
                   tashlardi — o'chirilgan variant bosilgach ro'yxat
                   ochiq qolib, klaviatura boshqaruvi YO'QOLARDI. */
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => pick(o)}
              >
                {o.label}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function firstEnabled(list) {
  const i = list.findIndex((o) => !o.disabled);
  return i >= 0 ? i : 0;
}

/**
 * `<option>` bolalarini oddiy massivga aylantiradi.
 *
 * `Children.toArray` massivlarni va Fragment'larni ochib beradi, shuning
 * uchun `{X.map(x => <option .../>)}` shakli o'zi ishlaydi.
 *
 * ⚠️ `value` berilmagan `<option>` uchun nativ brauzer matnning O'ZINI
 * qiymat sifatida oladi. Shu xatti-harakat takrorlanadi, aks holda
 * `<option>Hammasi</option>` jimgina bo'sh qiymatga aylanardi.
 */
function collectOptions(children) {
  const out = [];
  Children.toArray(children).forEach((child) => {
    if (!isValidElement(child)) return;
    // ⚠️ `Children.toArray` MASSIVLARNI ochadi, Fragment'ni esa YO'Q.
    // Usiz `<>...</>` ichiga o'ralgan variantlar jimgina yo'qolib,
    // ro'yxat bo'sh chiqardi.
    if (child.type === Fragment || child.type === 'optgroup') {
      out.push(...collectOptions(child.props.children));
      return;
    }
    if (child.type !== 'option') return;
    const label = textOf(child.props.children);
    out.push({
      value: child.props.value !== undefined ? child.props.value : label,
      label,
      disabled: Boolean(child.props.disabled),
    });
  });
  return out;
}

/** Bola tugunlaridan matn yig'adi: `{t('x')} ({n})` kabi shakllar uchun. */
function textOf(node) {
  if (node === null || node === undefined || node === false) return '';
  if (Array.isArray(node)) return node.map(textOf).join('');
  if (typeof node === 'object') return textOf(node.props?.children);
  return String(node);
}
