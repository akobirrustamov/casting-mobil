import GalleryField from '../../components/GalleryField';
import MediaField from '../../components/MediaField';
import { LOCALES, toBackendLocale } from '../../i18n';
/**
 * Afisha, muqova, galereya va videolar (ТЗ §18).
 *
 * Galereyada tartib o'zgartiriladi: birinchi rasm kartochkada ko'rinadi.
 *
 * <h2>⚠️ Video bu yerda — faqat SINGLE tuzilishda</h2>
 * Ko'p qismli kontentda video QISMGA tegishli (`EpisodeVideo`): bir qismda
 * turli til va bo'lak uchun bir nechta video bo'lishi mumkin. Shuning uchun
 * u yerda video «Qismlar» bo'limida biriktiriladi.
 *
 * SINGLE kontentda esa qism ham, fasl ham bo'lmaydi — film videosi
 * kontentning O'ZIGA `ContentMedia(role=VIDEO)` sifatida biriktiriladi.
 * `AccessService` uni aynan shu roldan qidiradi.
 *
 * Ilgari panelda bu maydon YO'Q edi va «Qismlar» bo'limi SINGLE da
 * ko'rsatilmasdi — ya'ni filmga video biriktirishning HECH QANDAY yo'li
 * yo'q edi. Backend esa buni to'liq qo'llab-quvvatlardi.
 *
 * ⚠️ Treyler har qanday tuzilishda ko'rinadi: u kontentning o'zi emas,
 * reklama roligi va obunasiz ham ochiq.
 */
export default function MediaTab({ form, set, t, locale, isSingle }) {
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
          <div className="uz-row">
            {/*
              ⚠️ Asosiy video FAQAT SINGLE da. Ko'p qismli kontentda u
              qismga tegishli va bu yerda ko'rsatilsa ikkita bir-biriga
              zid joy paydo bo'lardi.
            */}
            {isSingle ? (
              <div className="uz-col">
                <MediaField
                  type="VIDEO"
                  label={t('editor.mainVideo')}
                  hint={t('editor.mainVideoHint')}
                  value={form.video}
                  onChange={(id) => set({ video: id })}
                />
              </div>
            ) : (
              <div className="uz-col">
                <label className="uz-label">{t('editor.mainVideo')}</label>
                <p className="uz-muted" style={{ fontSize: 12 }}>
                  {t('editor.videoInEpisodes')}
                </p>
              </div>
            )}

            <div className="uz-col">
              <MediaField
                type="VIDEO"
                label={t('editor.trailer')}
                hint={t('editor.trailerHint')}
                value={form.trailer}
                onChange={(id) => set({ trailer: id })}
              />
            </div>
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
