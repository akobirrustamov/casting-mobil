import { Badge } from './Ui';
import { usePanelI18n } from '../i18n';

/**
 * Video qayta ishlash holati.
 *
 * <h2>⚠️ Nega alohida komponent</h2>
 * Bu nishon ikki joyda ko'rsatiladi — kutubxona kartochkasida va
 * tafsilot oynasida. Ikki marta yozilsa ohanglar va matnlar ajralib
 * ketardi: bir joyda «Yiqildi» qizil, boshqasida kulrang bo'lib
 * qolardi.
 *
 * <h2>⚠️ Ikkita «holat» chalkashligi</h2>
 * Panelda ikkita turli holat bor va backend ikkalasida ham
 * {@code READY} so'zini ishlatadi:
 *
 * <pre>
 *   media.status              READY · ARCHIVED   — kutubxonada ko'rinadimi
 *   media.transcoding.status  QUEUED … FAILED    — HLS tayyormi
 * </pre>
 *
 * Chalkashlik TARJIMA bilan hal qilinadi: birinchisi «Kutubxonada /
 * Arxivda», bu yerdagisi «Video tayyor / Navbatda / …». Backend
 * nomlari o'zgarmaydi — ular API shartnomasi.
 */

/** Holat → ohang va tarjima kaliti. */
const TONES = {
  QUEUED: { tone: 'draft', key: 'tc.QUEUED' },
  PROBING: { tone: 'scheduled', key: 'tc.PROBING' },
  TRANSCODING: { tone: 'scheduled', key: 'tc.TRANSCODING' },
  UPLOADING: { tone: 'scheduled', key: 'tc.UPLOADING' },
  READY: { tone: 'published', key: 'tc.READY' },
  FAILED: { tone: 'blocked', key: 'tc.FAILED' },
};

/**
 * @param transcoding `MediaDto.transcoding` — video bo'lmasa `null`
 * @param compact     kartochkada `true`: faqat qisqa matn
 */
export default function TranscodingBadge({ transcoding, compact = false }) {
  const { t } = usePanelI18n();

  // ⚠️ `null` — «bu savol tegishli emas» (rasm, hujjat) yoki «eski
  // fayl, ishi yo'q». Ikkala holatda ham nishon CHIZILMAYDI.
  //
  // Bo'sh nishon chizilsa admin «rasm qayta ishlanmoqda» degan
  // ma'nosiz holatni ko'rardi.
  if (!transcoding) return null;

  const meta = TONES[transcoding.status];
  // Backend yangi holat qo'shsa panel yiqilmasin — noma'lum qiymat
  // shunchaki ko'rsatilmaydi.
  if (!meta) return null;

  const isRunning = transcoding.status === 'TRANSCODING'
    || transcoding.status === 'PROBING'
    || transcoding.status === 'UPLOADING';

  // Progress faqat ishlash paytida va faqat NOLDAN katta bo'lsa.
  // «0%» hech qanday ma'lumot bermaydi, faqat joy egallaydi.
  const percent = isRunning && transcoding.progress > 0 ? transcoding.progress : null;

  const label = percent !== null
    ? `${t(meta.key)} · ${percent}%`
    : t(meta.key);

  return (
    <Badge tone={meta.tone}>
      {compact && percent !== null ? `${percent}%` : label}
    </Badge>
  );
}
