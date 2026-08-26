import { STATUSES } from './constants';
import Select from '../../components/Select';
/**
 * Nashr holati, sanalar va bayroqlar (ТЗ §22 Step 6).
 *
 * ⚠️ SCHEDULED holati NASHR SANASINI talab qiladi: usiz kontent
 * «rejalashtirilgan» bo'lib turadi, lekin qachon chiqishi noma'lum.
 */
export default function PublishTab({ form, set, t, can }) {
  return (
    <div className="uz-row">
      <div className="uz-col">
        <label className="uz-label" htmlFor="stt">{t('editor.status')}</label>
        <Select id="stt" className="uz-select" value={form.status}
                onChange={(e) => set({ status: e.target.value })}>
          {STATUSES.map((x) => (
            <option key={x} value={x}
                    disabled={x === 'PUBLISHED' && !can('CONTENT_PUBLISH')}>
              {x.replace(/_/g, ' ')}
            </option>
          ))}
        </Select>
      </div>
      <div className="uz-col">
        {/* ⚠️ NASHR sanasi — rejalashtirish uchun (ТЗ §53).
            Ilgari panelda faqat premyera sanasi bor edi: admin SCHEDULED
            holatini tanlay olardi, lekin kontent QACHON chiqishini
            belgilay olmasdi. Maydon backend DTO'sida bor edi, panelda
            esa yo'q — ya'ni rejalashtirish amalda ishlamasdi. */}
        <label className="uz-label" htmlFor="pubd">{t('editor.publicationDate')}</label>
        <input id="pubd" className="uz-input" type="datetime-local"
               value={form.publicationDate || ''}
               aria-invalid={form.status === 'SCHEDULED' && !form.publicationDate}
               onChange={(e) => set({ publicationDate: e.target.value })} />
        {form.status === 'SCHEDULED' && !form.publicationDate && (
          <div className="uz-field-error">{t('editor.scheduleNeedsDate')}</div>
        )}
      </div>
      <div className="uz-col">
        <label className="uz-label" htmlFor="pd">{t('editor.premiereDate')}</label>
        <input id="pd" className="uz-input" type="datetime-local" value={form.premiereDate}
               onChange={(e) => set({ premiereDate: e.target.value })} />
      </div>
      <div className="uz-col" style={{ flexBasis: '100%' }}>
        <label className="uz-label" htmlFor="sl">{t('editor.slug')}</label>
        <input id="sl" className="uz-input" value={form.slug} placeholder="auto"
               onChange={(e) => set({ slug: e.target.value })} />
        <p className="uz-muted mt-1" style={{ fontSize: 11 }}>{t('editor.slugHint')}</p>
      </div>
      <div style={{ flexBasis: '100%' }} className="flex gap-5 flex-wrap">
        <label className="uz-check">
          <input type="checkbox" checked={form.featured}
                 onChange={(e) => set({ featured: e.target.checked })} />
          {t('editor.featured')}
        </label>
        <label className="uz-check">
          <input type="checkbox" checked={form.popular}
                 onChange={(e) => set({ popular: e.target.checked })} />
          {t('editor.popular')}
        </label>
      </div>
    </div>
  );
}
