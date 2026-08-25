import { useEffect, useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import { usePanelI18n } from '../i18n';
import MediaPicker from './MediaPicker';

/**
 * Bitta rasm maydoni: oldindan ko'rish + tanlash/almashtirish/olib tashlash.
 */
export default function MediaField({ label, value, onChange, hint, type = 'IMAGE' }) {
  const { t } = usePanelI18n();
  const [pickerOpen, setPickerOpen] = useState(false);

  /**
   * Tanlangan video pleyerda ochiladimi.
   *
   * ⚠️ Nega alohida so'rov. Maydonga faqat `mediaId` uziladi — fayl
   * nomi ham, formati ham bu yerda yo'q. Ogohlantirishsiz admin
   * `.mkv` ni epizodga biriktirib qo'yardi va nosozlik faqat
   * foydalanuvchi qora ekran ko'rganda, ancha keyin bilinardi.
   */
  const [notPlayable, setNotPlayable] = useState(false);

  useEffect(() => {
    if (!value || type !== 'VIDEO') {
      setNotPlayable(false);
      return undefined;
    }
    let alive = true;
    adminApi.mediaAsset(value)
      .then((m) => { if (alive) setNotPlayable(m.playable === false); })
      // Ogohlantirishni chizolmaslik maydonni ishdan chiqarmasin.
      .catch(() => { if (alive) setNotPlayable(false); });
    return () => { alive = false; };
  }, [value, type]);

  return (
    <div>
      <label className="uz-label">{label}</label>
      {/* ⚠️ VIDEO uchun `<img>` chizilmaydi. Ilgari chizilardi va qism
          muharriridagi har bir video qismi SINGAN rasm belgisini
          ko'rsatardi — admin uchun bu «video yuklanmadi» degan
          taassurot berardi, aslida fayl joyida edi. */}
      {value && type !== 'VIDEO' ? (
        <img className="uz-thumb" src={mediaUrl(value)} alt="" loading="lazy" />
      ) : (
        <div
          className="uz-thumb flex items-center justify-center"
          style={{ color: value ? 'var(--p-text)' : 'var(--p-disabled)', fontSize: 24 }}
          aria-hidden="true"
        >
          {type === 'VIDEO' ? '🎞' : '🖼'}
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
      {notPlayable && (
        <p className="uz-field-warn mt-2" role="status">
          ⚠ {t('media.notPlayableHint')}
        </p>
      )}
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
