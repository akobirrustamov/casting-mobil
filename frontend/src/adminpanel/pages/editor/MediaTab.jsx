import GalleryField from '../../components/GalleryField';
import MediaField from '../../components/MediaField';
import { LOCALES, toBackendLocale } from '../../i18n';
/**
 * Afisha, muqova, galereya va videolar (ТЗ §18).
 *
 * Galereyada tartib o'zgartiriladi: birinchi rasm kartochkada ko'rinadi.
 */
export default function MediaTab({ form, set, t, locale }) {
  return (
      <>
        <div className="uz-row mb-5">
          <div className="uz-col">
            <MediaField
              label={`${t('editor.poster')} — ${t('editor.posterDefault')}`}
              value={form.posterDefault}
              onChange={(id) => set({ posterDefault: id })}
            />
          </div>
          <div className="uz-col">
            <MediaField label={t('editor.cover')} value={form.cover}
                        onChange={(id) => set({ cover: id })} />
          </div>
        </div>

        <div className="uz-card p-4 mb-5">
          <GalleryField
            value={form.gallery}
            onChange={(gallery) => set({ gallery })}
          />
        </div>

        <div className="uz-card p-4">
          <div className="uz-h2 mb-1" style={{ fontSize: 15 }}>{t('editor.posterForLocale')}</div>
          <p className="uz-muted mb-4" style={{ fontSize: 12 }}>
            {t('editor.posterDefault')} — {t('common.none').toLowerCase()}
          </p>
          <div className="uz-row">
            {LOCALES.map((l) => {
              const code = toBackendLocale(l);
              return (
                <div className="uz-col" key={l}>
                  <MediaField
                    label={l.toUpperCase()}
                    value={form.posterByLocale[code] || null}
                    onChange={(id) =>
                      set({ posterByLocale: { ...form.posterByLocale, [code]: id } })}
                  />
                </div>
              );
            })}
          </div>
        </div>
      </>
  );
}
