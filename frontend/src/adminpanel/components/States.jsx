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

export function EmptyState({ title, body, icon = '📭' }) {
  const { t } = usePanelI18n();
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
