import { useState } from 'react';
import { mediaUrl } from '../api/client';
import { usePanelI18n } from '../i18n';
import MediaPicker from './MediaPicker';
import MediaSpec from './MediaSpec';

/**
 * Galereya: bir nechta rasm, tartibi bilan.
 *
 * Tartib MUHIM — kontent sahifasida rasmlar aynan shu ketma-ketlikda
 * ko'rsatiladi, shuning uchun uni o'zgartirish imkoni bo'lishi kerak.
 *
 * Nega drag-and-drop emas: strelkali tugmalar klaviatura bilan ham
 * ishlaydi va sensorli ekranda ham ishonchli. Bu yerda tartib kamdan-kam
 * o'zgaradi, shuning uchun soddaligi afzal.
 *
 * ⚠️ O'lcham talabi bu yerda AYNIQSA muhim: galereya bitta rasm emas,
 * qator. Bittasi boshqa nisbatda bo'lsa, butun qator teng bo'lmagan
 * balandlikda ko'rinadi va bu «buzilgan» degan taassurot beradi.
 */
export default function GalleryField({ value = [], onChange, spec = 'gallery' }) {
  const { t } = usePanelI18n();
  const [pickerOpen, setPickerOpen] = useState(false);

  const items = Array.isArray(value) ? value : [];

  function add(id) {
    if (!id || items.includes(id)) return;
    onChange([...items, id]);
  }

  function removeAt(index) {
    onChange(items.filter((_, i) => i !== index));
  }

  function move(index, delta) {
    const target = index + delta;
    if (target < 0 || target >= items.length) return;
    const next = [...items];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <label className="uz-label" style={{ marginBottom: 0 }}>
          {t('editor.gallery')} {items.length > 0 && `(${items.length})`}
        </label>
        <button
          type="button"
          className="uz-btn uz-btn-ghost"
          style={{ minHeight: 36, fontSize: 13 }}
          onClick={() => setPickerOpen(true)}
        >
          + {t('media.upload')}
        </button>
      </div>

      {spec && (
        <div className="mb-3">
          <MediaSpec name={spec} />
        </div>
      )}

      {items.length === 0 ? (
        <p className="uz-muted" style={{ fontSize: 13 }}>{t('editor.galleryEmpty')}</p>
      ) : (
        <div className="uz-gallery-grid">
          {items.map((id, index) => (
            <div className="uz-gallery-item" key={`${id}-${index}`}>
              <img src={mediaUrl(id)} alt="" loading="lazy" />
              <div className="uz-gallery-actions">
                <button
                  type="button"
                  className="uz-icon-btn"
                  onClick={() => move(index, -1)}
                  disabled={index === 0}
                  aria-label={t('editor.moveLeft')}
                  title={t('editor.moveLeft')}
                >
                  ←
                </button>
                <button
                  type="button"
                  className="uz-icon-btn"
                  onClick={() => move(index, 1)}
                  disabled={index === items.length - 1}
                  aria-label={t('editor.moveRight')}
                  title={t('editor.moveRight')}
                >
                  →
                </button>
                <button
                  type="button"
                  className="uz-icon-btn uz-icon-btn-danger"
                  onClick={() => removeAt(index)}
                  aria-label={t('media.clear')}
                  title={t('media.clear')}
                >
                  ✕
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <MediaPicker
        open={pickerOpen}
        onClose={() => setPickerOpen(false)}
        onSelect={add}
        spec={spec}
        type="IMAGE"
      />
    </div>
  );
}
