/**
 * Har bir ro'yxat sahifasi uchun majburiy holatlar (§51):
 * loading / empty / error / forbidden.
 *
 * Bir xil ko'rinishni har sahifada qayta yozmaslik uchun shu yerda.
 */
import { usePanelI18n } from '../i18n';

export function LoadingState({ rows = 5 }) {
  const { t } = usePanelI18n();
  return (
    <div className="p-6" role="status" aria-live="polite" aria-busy="true">
      <span className="sr-only">{t('common.loading')}</span>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="uz-skeleton mb-3" style={{ height: 52 }} />
      ))}
    </div>
  );
}

/**
 * Ma'lumot yo'qligini bildiradi.
 *
 * <h2>`compact` nima uchun kerak</h2>
 * Dashboardda beshta kichik jadval yonma-yon turadi, foydalanuvchi
 * sahifasida esa qurilmalar va obunalar ro'yxati. Ularning har birida
 * to'liq balandlikdagi bo'sh holat sahifani bir necha ekranga
 * cho'zib yuborardi.
 *
 * ⚠️ Buning yechimi «u yerda oddiy `<p>` yozib qo'yish» EMAS edi.
 * Aynan shunday qilingan edi va natijada bo'sh holat har joyda
 * boshqacha ko'rinardi — matni, rangi, joylashuvi. Bitta komponent,
 * ikkita zichlik: ko'rinish bir xil qoladi, o'lchami esa joyiga
 * moslashadi (§72).
 */
export function EmptyState({ title, body, icon = '📭', compact = false }) {
  const { t } = usePanelI18n();

  if (compact) {
    return (
      <div className="flex items-center gap-3 px-5 py-6">
        <span style={{ fontSize: 20 }} aria-hidden="true">{icon}</span>
        <span className="uz-muted text-sm">{title || t('empty.title')}</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center text-center px-6 py-16">
      <div style={{ fontSize: 44 }} aria-hidden="true">{icon}</div>
      <div className="uz-h2 mt-4">{title || t('empty.title')}</div>
      <p className="uz-muted mt-2 max-w-md text-sm">{body || t('empty.body')}</p>
    </div>
  );
}

export function ErrorState({ error, onRetry }) {
  const { t } = usePanelI18n();
  const isNetwork = error?.code === 'NETWORK_ERROR';
  return (
    <div className="flex flex-col items-center justify-center text-center px-6 py-16">
      <div style={{ fontSize: 44 }} aria-hidden="true">⚠️</div>
      <div className="uz-h2 mt-4">{t('error.title')}</div>
      <p className="uz-muted mt-2 max-w-md text-sm">
        {isNetwork ? t('error.network') : error?.message}
      </p>
      {onRetry && (
        <button type="button" className="uz-btn uz-btn-ghost mt-5" onClick={onRetry}>
          {t('common.retry')}
        </button>
      )}
    </div>
  );
}

export function ForbiddenState() {
  const { t } = usePanelI18n();
  return (
    <div className="flex flex-col items-center justify-center text-center px-6 py-16">
      <div style={{ fontSize: 44 }} aria-hidden="true">🔒</div>
      <div className="uz-h2 mt-4">{t('error.forbidden')}</div>
      <p className="uz-muted mt-2 max-w-md text-sm">{t('error.forbiddenBody')}</p>
    </div>
  );
}
