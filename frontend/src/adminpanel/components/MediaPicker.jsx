import { useCallback, useEffect, useRef, useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import { usePanelI18n } from '../i18n';
import { LoadingState, ErrorState, EmptyState } from './States';
import Modal from './Modal';

/**
 * Media kutubxonadan fayl tanlash yoki yangisini yuklash.
 *
 * Ikki vazifani birlashtiradi (§54 dagi creator naqshi kabi): avval mavjudini
 * qidiradi, topilmasa shu yerning o'zida yuklaydi.
 */
export default function MediaPicker({ open, onClose, onSelect, type = 'IMAGE' }) {
  const { t } = usePanelI18n();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);
  const [progress, setProgress] = useState(null);
  const fileRef = useRef(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    adminApi
      .media({ page: 0, size: 60, type })
      .then((res) => setItems(res.items || []))
      .catch(setError)
      .finally(() => setLoading(false));
  }, [type]);

  useEffect(() => {
    if (open) {
      setSelected(null);
      load();
    }
  }, [open, load]);

  async function handleUpload(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    setProgress(0);
    setError(null);
    try {
      const created = await adminApi.uploadMedia(file, 'content', setProgress);
      setItems((prev) => [created, ...prev]);
      setSelected(created.id);
    } catch (err) {
      setError(err);
    } finally {
      setProgress(null);
      if (fileRef.current) fileRef.current.value = '';
    }
  }

  return (
    <Modal
      open={open}
      title={t('media.pick')}
      onClose={onClose}
      width={760}
      footer={
        <>
          <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose}>
            {t('common.cancel')}
          </button>
          <button
            type="button"
            className="uz-btn uz-btn-primary"
            disabled={!selected}
            onClick={() => {
              onSelect(selected);
              onClose();
            }}
          >
            {t('common.select')}
          </button>
        </>
      }
    >
      <div className="mb-4">
        <input
          ref={fileRef}
          type="file"
          /* ⚠️ `video/*` EMAS — ro'yxat server bilan AYNAN bir xil
             (`LocalStorageService.ALLOWED`). `video/*` bo'lsa brauzer
             `.flv` yoki `.wmv` ni ham taklif qilardi va admin faylni
             tanlagach 422 xato olardi, nima uchun ekanini tushunmasdan. */
          accept={type === 'VIDEO'
            ? '.mp4,.mov,.webm,.m4v,.mkv,.avi'
              + ',video/mp4,video/quicktime,video/webm,video/x-matroska,video/x-msvideo'
            : '.jpg,.jpeg,.png,.webp,.gif,.svg,image/*'}
          onChange={handleUpload}
          style={{ display: 'none' }}
          id="uz-media-upload"
        />
        <label htmlFor="uz-media-upload" className="uz-btn uz-btn-ghost" style={{ cursor: 'pointer' }}>
          ⬆ {t('media.upload')}
        </label>
        {progress !== null && (
          <div className="uz-progress mt-3" role="progressbar" aria-valuenow={progress}>
            <div style={{ width: `${progress}%` }} />
          </div>
        )}
      </div>

      {loading ? (
        <LoadingState rows={3} />
      ) : error ? (
        <ErrorState error={error} onRetry={load} />
      ) : items.length === 0 ? (
        <EmptyState icon="🖼" />
      ) : (
        <div className="uz-media-grid">
          {items.map((m) => (
            <button
              key={m.id}
              type="button"
              className={`uz-media-tile ${selected === m.id ? 'selected' : ''}`}
              onClick={() => setSelected(m.id)}
              aria-pressed={selected === m.id}
              title={m.originalFilename || String(m.id)}
            >
              {m.type === 'IMAGE' ? (
                <img src={mediaUrl(m.id)} alt="" loading="lazy" />
              ) : (
                <span style={{ fontSize: 28 }} aria-hidden="true">🎞</span>
              )}
              {/* ⚠️ `=== false` ataylab: rasm va hujjatda `playable`
                  `null` bo'ladi — «o'ynatib bo'lmaydi» emas, «bu savol
                  tegishli emas». `!m.playable` bo'lsa har bir rasmga
                  ogohlantirish yopishtirilardi. */}
              {m.playable === false && (
                <span className="uz-media-warn" title={t('media.notPlayableHint')}>
                  ⚠ {t('media.notPlayable')}
                </span>
              )}
            </button>
          ))}
        </div>
      )}
    </Modal>
  );
}
