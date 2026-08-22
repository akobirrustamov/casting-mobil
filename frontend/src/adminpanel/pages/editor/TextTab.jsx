import LocaleTabs from '../../components/LocaleTabs';
/**
 * Uch tildagi matnlar (ТЗ §15).
 *
 * Til almashtirilganda forma qayta yuklanmaydi — barcha tillar bitta
 * holatda saqlanadi va faqat ko'rinadigan til o'zgaradi.
 */
export default function TextTab({ form, t, locale, setLocale, setTranslation, uzTitleMissing, titleError }) {
  return (
      <>
        <LocaleTabs
          active={locale}
          onChange={setLocale}
          isFilled={(code) => Boolean(form.translations?.[code]?.title?.trim())}
        />
        <div className="mb-4">
          <label className="uz-label" htmlFor="ti">
            {t('editor.title')}
            {locale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
          </label>
          <input id="ti" className="uz-input"
                 value={form.translations?.[locale]?.title || ''}
                 aria-invalid={Boolean(titleError) || (locale === 'UZ' && uzTitleMissing)}
                 onChange={(e) => setTranslation('title', e.target.value)} />
          {/* Avval BACKEND xatosi: u haqiqiy rad etish sababi va
              aniqroq (masalan «RU, EN tillarida to'ldirilmagan»).
              Klient tekshiruvi esa yuborishdan oldingi ogohlantirish. */}
          {titleError ? (
            <div className="uz-field-error">{titleError}</div>
          ) : locale === 'UZ' && uzTitleMissing ? (
            <div className="uz-field-error">{t('editor.uzRequired')}</div>
          ) : null}
        </div>
        <div className="mb-4">
          <label className="uz-label" htmlFor="sd">{t('editor.shortDesc')}</label>
          <input id="sd" className="uz-input"
                 value={form.translations?.[locale]?.shortDescription || ''}
                 onChange={(e) => setTranslation('shortDescription', e.target.value)} />
        </div>
        <div>
          <label className="uz-label" htmlFor="de">{t('editor.desc')}</label>
          <textarea id="de" className="uz-input" rows={5} style={{ resize: 'vertical' }}
                    value={form.translations?.[locale]?.description || ''}
                    onChange={(e) => setTranslation('description', e.target.value)} />
        </div>
      </>
  );
}
