import { useEffect, useRef } from 'react';

/**
 * Oddiy modal oyna.
 *
 * Escape bilan yopiladi, ochiq bo'lganda sahifa siljimaydi va fokus
 * oyna ichida ushlab turiladi (§97).
 *
 * ⚠️ Fokus qismi ilgari faqat izohda yozilgan edi, kodda esa yo'q edi.
 * Ya'ni klaviatura bilan ishlaydigan foydalanuvchi modal ochilganda
 * orqadagi sahifada qolib ketardi va Tab bosib ko'rinmaydigan
 * tugmalar bo'ylab yurardi — modal ochiq turgani holda.
 */
export default function Modal({ open, title, onClose, children, footer, width = 720 }) {
  const dialogRef = useRef(null);
  const returnFocusRef = useRef(null);

  /**
   * ⚠️ `onClose` REF orqali ushlanadi va effekt bog'liqligiga KIRMAYDI.
   *
   * Bu bezak emas — usiz muharrirga yozib bo'lmasdi. Ota-komponent
   * odatda `onClose={() => setOpen(false)}` yoki `onClose={requestClose}`
   * beradi, ya'ni funksiya HAR RENDERDA yangi bo'ladi. Agar u
   * bog'liqlikda tursa, effekt har renderda qaytadan ishga tushadi:
   * tozalash fokusni ochgan tugmaga qaytaradi, keyingi ishga tushish
   * esa uni oynaning BIRINCHI elementiga olib qo'yadi.
   *
   * Formasi o'z ichida turgan oynalarda (kontent muharriri, reklama,
   * bildirishnoma, bosh sahifa) har bosilgan harf renderni keltirib
   * chiqaradi — natijada fokus har harfdan keyin ko'chib ketardi va
   * maydonga faqat BITTA belgi tushardi. `§86` oqim testi shuni
   * topdi.
   */
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!open) return undefined;

    // Yopilgach fokus qaytadigan joy: modalni ochgan tugma.
    returnFocusRef.current = document.activeElement;

    const focusable = () => {
      if (!dialogRef.current) return [];
      return Array.from(dialogRef.current.querySelectorAll(
        'a[href], button:not([disabled]), input:not([disabled]), '
        + 'select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      )).filter((el) => el.offsetParent !== null);
    };

    // Ochilganda fokusni ichkariga olib kiramiz. Yopish tugmasi emas,
    // birinchi mazmunli element - aks holda har ochilishda fokus
    // «Yopish» da turardi va bu foydalanuvchini chalkashtirardi.
    const items = focusable();
    (items.find((el) => el.getAttribute('aria-label') !== 'Yopish') || items[0]
      || dialogRef.current)?.focus();

    const onKey = (e) => {
      if (e.key === 'Escape') {
        onCloseRef.current();
        return;
      }
      if (e.key !== 'Tab') return;

      // Fokus tuzog'i: oxirgi elementdan keyin birinchisiga qaytadi.
      const list = focusable();
      if (list.length === 0) {
        e.preventDefault();
        return;
      }
      const first = list[0];
      const last = list[list.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      } else if (!dialogRef.current?.contains(document.activeElement)) {
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', onKey);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prevOverflow;
      // Fokusni ochgan tugmaga qaytaramiz: aks holda u sahifa
      // boshiga tushib ketardi va foydalanuvchi joyini yo'qotardi.
      if (returnFocusRef.current instanceof HTMLElement) {
        returnFocusRef.current.focus();
      }
    };
    // Faqat `open` — sabab yuqoridagi izohda.
  }, [open]);

  if (!open) return null;

  return (
    <div
      className="uz-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-label={title}
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="uz-modal" style={{ maxWidth: width }} ref={dialogRef} tabIndex={-1}>
        <div className="uz-modal-head">
          <h2 className="uz-h2">{title}</h2>
          <button type="button" className="uz-btn uz-btn-ghost" style={{ minHeight: 34, padding: '0 12px' }}
                  onClick={onClose} aria-label="Yopish">
            ✕
          </button>
        </div>
        <div className="uz-modal-body">{children}</div>
        {footer && <div className="uz-modal-foot">{footer}</div>}
      </div>
    </div>
  );
}
