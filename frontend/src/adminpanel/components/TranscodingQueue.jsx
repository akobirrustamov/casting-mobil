import { Badge } from './Ui';
import { usePanelI18n } from '../i18n';

/**
 * Navbat va server holati.
 *
 * <h2>⚠️ Nega bu komponent kerak bo'ldi</h2>
 * Backend `GET /media/transcoding-queue` ni allaqachon berardi va
 * `client.js` da chaqiruv ham yozilgan edi — lekin uni HECH BIR
 * sahifa ishlatmasdi. Ya'ni navbat mavjud, ammo ko'rinmasdi.
 *
 * <h2>⚠️ Eng muhimi — SERVER holati</h2>
 * FFmpeg o'rnatilmagan bo'lsa har bir video uch marta urinib
 * yiqilardi. Har bir ishning xato matni to'g'ri («ffprobe ishga
 * tushmadi»), lekin admin ularni alohida ko'rib, buzuq fayl izlab
 * yurardi.
 *
 * Bu banner javobni bir marta va aniq beradi: muammo videolarda emas,
 * serverda.
 *
 * <h2>Hammasi joyida bo'lsa — HECH NARSA chizilmaydi</h2>
 * Doim ko'rinadigan «0 · 0 · 0» qatori shovqin bo'lardi va bir
 * hafta ichida odam unga umuman qaramay qo'yardi. Aynan shunda u
 * kerak bo'lganda ham ko'rinmasdi.
 */

/** Baytni GB ga — panelda o'qish uchun bir xonali aniqlik. */
const gb = (bytes) => (bytes / 1024 / 1024 / 1024).toFixed(1);

/**
 * @param queue `QueueDto` — yuklanmagan bo'lsa `null`
 */
export default function TranscodingQueue({ queue }) {
  const { t } = usePanelI18n();

  if (!queue) return null;

  const system = queue.system;
  const problems = system?.problems || [];
  const idle = !queue.queued && !queue.running && !queue.failed;

  // Muammo ham yo'q, ish ham yo'q — chizilmaydi.
  if (!problems.length && idle) return null;

  return (
    <div className="mb-4">
      {problems.length > 0 && (
        <div
          className="uz-card mb-3"
          role="alert"
          style={{ borderColor: 'var(--p-danger)' }}
        >
          <div className="flex items-center gap-2 mb-2">
            <span aria-hidden="true">⚠️</span>
            <strong>{t('tc.queue.serverDown')}</strong>
          </div>

          <ul className="mb-2" style={{ paddingLeft: 20 }}>
            {problems.map((problem) => (
              <li key={problem}>{problem}</li>
            ))}
          </ul>

          {/*
            ⚠️ Nima qilish kerakligi AYTILADI. Admin serverga kira
            olmaydi, ya'ni «FFmpeg yo'q» degan xabar undan hech qanday
            amal talab qilmaydi — u kimga murojaat qilishni bilishi
            kerak.
          */}
          <p className="uz-muted text-sm">{t('tc.queue.serverDownHint')}</p>
        </div>
      )}

      {!idle && (
        <div className="uz-card flex items-center gap-3 flex-wrap">
          {queue.queued > 0 && (
            <Badge tone="draft">{t('tc.queue.queued')}: {queue.queued}</Badge>
          )}
          {queue.running > 0 && (
            <Badge tone="scheduled">{t('tc.queue.running')}: {queue.running}</Badge>
          )}
          {queue.failed > 0 && (
            <Badge tone="blocked">{t('tc.queue.failed')}: {queue.failed}</Badge>
          )}

          {/*
            ⚠️ `null` — «o'lchab bo'lmadi», NOL emas. Nol ko'rsatilsa
            admin mavjud bo'lmagan «disk to'ldi» muammosini tuzatishga
            urinardi.
          */}
          {system?.freeDiskBytes != null && (
            <span className="uz-muted text-sm">
              {t('tc.queue.freeDisk')}: {gb(system.freeDiskBytes)} GB
            </span>
          )}
        </div>
      )}
    </div>
  );
}
