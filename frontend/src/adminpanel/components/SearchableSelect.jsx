import Select from './Select';

/**
 * Qidiruvli tanlash — MASSIV shaklidagi API bilan (ТЗ §53).
 *
 * <h2>Nima uchun alohida komponent</h2>
 * Butun mantiq {@link Select} da. Bu yerda faqat API moslashtiriladi:
 * bu chaqiruv joylari `[{id, label}]` massivi va `onChange(id)` bilan
 * ishlaydi, `Select` esa nativ `<select>` kabi `<option>` bolalari va
 * `onChange(e)` bilan.
 *
 * ⚠️ Ikkala komponentda ALOHIDA ochiluvchi ro'yxat mantig'i bo'lmasin —
 * ilgari shunday edi va klaviatura bilan boshqarish faqat bittasiga
 * qo'shilardi. Endi yagona manba bor.
 *
 * <h2>`id` ning turi saqlanadi</h2>
 * `Select` nativ `<select>` kabi HAR DOIM satr qaytaradi. Bu yerda u asl
 * variantga qaytarib moslanadi, shuning uchun raqamli `id` chaqiruv
 * joyiga RAQAM bo'lib boradi — ilgarigidek.
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
  const handle = (e) => {
    const raw = e.target.value;
    // Tanlovni BEKOR qilish: kategoriya ixtiyoriy va uni olib tashlash
    // yo'li bo'lishi kerak.
    if (raw === '') {
      onChange(null);
      return;
    }
    const found = options.find((o) => String(o.id) === raw);
    onChange(found ? found.id : raw);
  };

  return (
    <Select
      value={value ?? ''}
      onChange={handle}
      placeholder={placeholder}
      aria-label={ariaLabel}
      aria-invalid={invalid || undefined}
      /* Bu ro'yxatlar (kontent, ijodkor, tarif) CHEKLANMAGAN darajada
         o'sadi, shuning uchun qidiruv maydoni har doim ko'rinadi. */
      searchThreshold={0}
    >
      {emptyLabel ? <option value="">{emptyLabel}</option> : null}
      {options.map((o) => (
        <option key={o.id} value={o.id}>{o.label}</option>
      ))}
    </Select>
  );
}
