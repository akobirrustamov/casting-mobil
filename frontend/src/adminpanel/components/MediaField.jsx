import { useState } from 'react';
import { mediaUrl } from '../api/client';
import { usePanelI18n } from '../i18n';
import MediaPicker from './MediaPicker';

/**
 * Bitta rasm maydoni: oldindan ko'rish + tanlash/almashtirish/olib tashlash.
 */
export default function MediaField({ label, value, onChange, hint, type = 'IMAGE' }) {
  const { t } = usePanelI18n();
  const [pickerOpen, setPickerOpen] = useState(false);

  return (
    <div>
      <label className="uz-label">{label}</label>
      {value ? (
        <img className="uz-thumb" src={mediaUrl(value)} alt="" loading="lazy" />
      ) : (
        <div
          className="uz-thumb flex items-center justify-center"
          style={{ color: 'var(--p-disabled)', fontSize: 24 }}
          aria-hidden="true"
        >
          🖼
        </div>
      )}
      <div className="flex gap-2 mt-2 flex-wrap">
        <button
          type="button"
          className="uz-btn uz-btn-ghost"
          style={{ minHeight: 36, fontSize: 13 }}
          onClick={() => setPickerOpen(true)}
        >
          {value ? t('media.change') : t('media.upload')}
        </button>
        {value && (
          <button
            type="button"
            className="uz-btn uz-btn-ghost"
            style={{ minHeight: 36, fontSize: 13 }}
            onClick={() => onChange(null)}
          >
            {t('media.clear')}
          </button>
        )}
      </div>
      {hint && <p className="uz-muted mt-1" style={{ fontSize: 11 }}>{hint}</p>}

      <MediaPicker
        open={pickerOpen}
        type={type}
        onClose={() => setPickerOpen(false)}
        onSelect={onChange}
      />
    </div>
  );
}
