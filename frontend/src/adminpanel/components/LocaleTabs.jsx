import { LOCALES, toBackendLocale, usePanelI18n } from '../i18n';

/**
 * Uch til uchun tab'lar.
 *
 * Tarjimasi bo'sh til qizil nuqta bilan belgilanadi - admin qaysi til
 * to'ldirilmaganini bir qarashda ko'radi.
 */
export default function LocaleTabs({ active, onChange, isFilled }) {
  const { t } = usePanelI18n();
  return (
    <div className="uz-tabs" role="tablist" aria-label={t('common.language')}>
      {LOCALES.map((l) => {
        const code = toBackendLocale(l);
        const filled = isFilled ? isFilled(code) : true;
        return (
          <button
            key={l}
            type="button"
            role="tab"
            aria-selected={active === code}
            className={`uz-tab ${active === code ? 'active' : ''}`}
            onClick={() => onChange(code)}
            title={filled ? undefined : t('editor.noTranslation')}
          >
            {l.toUpperCase()}
            {!filled && <span className="uz-dot" aria-label={t('editor.noTranslation')} />}
          </button>
        );
      })}
    </div>
  );
}
