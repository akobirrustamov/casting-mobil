import { ORIENTATIONS, STRUCTURES, TYPES } from './constants';
import SearchableSelect from '../../components/SearchableSelect';
/**
 * Asosiy ma'lumot: tur, tuzilish, kategoriya, janrlar (ТЗ §22 Step 1).
 *
 * ⚠️ Kontent TURI, KATEGORIYA va JANR — uch mustaqil o'lchov (ТЗ §13).
 * Masalan MINI_SERIES / Drama / Romantika. Ularni bitta ro'yxatga
 * qo'shish tasnifni buzardi.
 */
export default function BasicTab({ form, set, t, locale, categories, genres, categoryError }) {
  return (
      <div className="uz-row">
        <div className="uz-col">
          <label className="uz-label" htmlFor="ct">{t('editor.type')}</label>
          <select id="ct" className="uz-select" value={form.contentType}
                  onChange={(e) => set({ contentType: e.target.value })}>
            {TYPES.map((x) => <option key={x} value={x}>{x.replace(/_/g, ' ')}</option>)}
          </select>
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="st">{t('editor.structure')}</label>
          <select id="st" className="uz-select" value={form.structureType}
                  onChange={(e) => set({ structureType: e.target.value })}>
            {STRUCTURES.map((x) => <option key={x} value={x}>{x}</option>)}
          </select>
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="or">{t('editor.orientation')}</label>
          <select id="or" className="uz-select" value={form.orientation}
                  onChange={(e) => set({ orientation: e.target.value })}>
            {ORIENTATIONS.map((x) => (
              <option key={x} value={x}>
                {x === 'VERTICAL' ? t('content.vertical') : t('content.landscape')}
              </option>
            ))}
          </select>
        </div>
        <div className="uz-col" style={{ flexBasis: '100%' }}>
          <label className="uz-label" htmlFor="cat">{t('editor.category')}</label>
          {/* ⚠️ Qidiruvli tanlash: kategoriyalar soni cheklanmagan va
              oddiy `<select>` da kerakligini topish uchun butun ro'yxatni
              aylantirib chiqish kerak bo'lardi (ТЗ §53). */}
          <SearchableSelect
            value={form.categoryId}
            invalid={Boolean(categoryError)}
            ariaLabel={t('editor.category')}
            emptyLabel={t('editor.noCategory')}
            onChange={(id) => set({ categoryId: id })}
            options={categories.map((c) => ({
              id: c.id,
              label: c.translations?.[locale]?.title
                || c.translations?.UZ?.title || c.slug,
            }))}
          />
          {categoryError && <div className="uz-field-error">{categoryError}</div>}
        </div>
        <div style={{ flexBasis: '100%' }}>
          <label className="uz-label">{t('editor.genres')}</label>
          <div className="flex gap-2 flex-wrap">
            {genres.map((g) => {
              const on = form.genreIds.includes(g.id);
              return (
                <button key={g.id} type="button"
                        className={`uz-chip ${on ? 'selected' : ''}`}
                        aria-pressed={on}
                        onClick={() => set({
                          genreIds: on ? form.genreIds.filter((x) => x !== g.id)
                                       : [...form.genreIds, g.id],
                        })}>
                  {g.translations?.[locale]?.title || g.translations?.UZ?.title || g.slug}
                </button>
              );
            })}
          </div>
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="ar">{t('editor.ageRating')}</label>
          <input id="ar" className="uz-input" value={form.ageRating} placeholder="16+"
                 onChange={(e) => set({ ageRating: e.target.value })} />
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="du">{t('editor.duration')}</label>
          <input id="du" className="uz-input" type="number" min="0" value={form.durationMinutes}
                 onChange={(e) => set({ durationMinutes: e.target.value })} />
        </div>
      </div>
  );
}
