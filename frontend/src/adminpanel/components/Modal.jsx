import { useEffect } from 'react';

/**
 * Oddiy modal oyna.
 *
 * Escape bilan yopiladi, ochiq bo'lganda sahifa siljimaydi, fokus ichida qoladi
 * (§97 - modal focus).
 */
export default function Modal({ open, title, onClose, children, footer, width = 720 }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prevOverflow;
    };
  }, [open, onClose]);

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
      <div className="uz-modal" style={{ maxWidth: width }}>
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
