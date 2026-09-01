import { useEffect, useState } from 'react';
import Modal from './Modal';
import { adminApi } from '../api/client';

/**
 * Videoni panelda ko'rish.
 *
 * <h2>⚠️ Nima uchun kerak bo'ldi</h2>
 * Xodim video yuklardi, lekin uni HECH QACHON ko'ra olmasdi — panelda
 * pleyer umuman yo'q edi. Videoning buzuq emasligini, dublyaj mos
 * kelishini yoki to'g'ri fayl biriktirilganini tekshirishning yagona
 * yo'li kontentni NASHR QILIB, ilovadan ochish edi.
 *
 * <h2>⚠️ Manzil har safar QAYTA so'raladi</h2>
 * U imzolangan va muddati cheklangan. Bir marta olib saqlab qo'yilsa
 * bir necha soatdan keyin «video ochilmadi» bo'lardi va sababi
 * ko'rinmasdi.
 *
 * ⚠️ Havola faqat modal OCHILGANDA so'raladi. Aks holda har bir
 * qism muharriridagi har bir video maydoni sahifa ochilishida
 * ortiqcha so'rov yuborardi.
 */
export default function VideoPreview({ open, mediaId, title, onClose, t }) {
  const [url, setUrl] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open || !mediaId) {
      setUrl(null);
      setError(null);
      return undefined;
    }

    let alive = true;
    setUrl(null);
    setError(null);

    adminApi.mediaPreview(mediaId)
      .then((r) => {
        if (alive) setUrl(r.url);
      })
      .catch((e) => {
        if (alive) setError(e?.message || t('common.error'));
      });

    return () => { alive = false; };
  }, [open, mediaId, t]);

  return (
    <Modal open={open} title={title || t('media.preview')} onClose={onClose}>
      {error && <div className="uz-alert uz-alert-danger">{error}</div>}

      {!url && !error && (
        <div className="uz-muted" style={{ padding: 24, textAlign: 'center' }}>
          {t('common.loading')}
        </div>
      )}

      {url && (
        // ⚠️ `controls` shart — busiz pleyerda to'xtatish ham,
        // oldinga o'tkazish ham bo'lmasdi.
        //
        // ⚠️ `preload="metadata"`: butun fayl tortilmaydi, faqat
        // davomiylik va o'lcham o'qiladi. 600 MB lik manbada bu
        // muhim — qolganini brauzer `Range` bilan kerak bo'lganda
        // oladi.
        <video
          src={url}
          controls
          preload="metadata"
          style={{ width: '100%', maxHeight: '70vh', background: 'var(--p-video-backdrop)' }}
        >
          {t('media.previewUnsupported')}
        </video>
      )}
    </Modal>
  );
}
