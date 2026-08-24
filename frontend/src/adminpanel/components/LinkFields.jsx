import { usePanelI18n } from '../i18n';
import Select from './Select';

const TARGET_TYPES = ['CONTENT', 'EPISODE', 'CATEGORY', 'CREATOR', 'CASTING', 'PREMIERE', 'OTHER'];

/**
 * Havola tahrirlagichi — reklama va premyera uchun UMUMIY.
 *
 * Backend'da ham bitta mexanizm ({@code InternalLink} @Embeddable), shuning
 * uchun UI ham bitta komponent: uch joyda takrorlanmaydi.
 *
 * Turga qarab kerakli maydonlar ko'rsatiladi — ortiqchasi chalkashtirmasin.
 */
export default function LinkFields({ value, onChange }) {
  const { t } = usePanelI18n();
  const link = value || { linkType: 'NONE' };
  const set = (patch) => onChange({ ...link, ...patch });

  return (
    <div className="uz-card p-4">
      <div className="uz-h2 mb-3" style={{ fontSize: 15 }}>{t('ads.link')}</div>

      <div className="uz-row">
        <div className="uz-col">
          <label className="uz-label" htmlFor="lk-type">{t('ads.linkType')}</label>
          <Select id="lk-type" className="uz-select" value={link.linkType || 'NONE'}
                  onChange={(e) => set({ linkType: e.target.value })}>
            <option value="NONE">{t('ads.linkNone')}</option>
            <option value="EXTERNAL">{t('ads.linkExternal')}</option>
            <option value="INTERNAL">{t('ads.linkInternal')}</option>
          </Select>
        </div>

        {link.linkType === 'EXTERNAL' && (
          <div className="uz-col" style={{ flexBasis: '100%' }}>
            <label className="uz-label" htmlFor="lk-url">{t('ads.linkUrl')}</label>
            <input id="lk-url" className="uz-input" type="url" placeholder="https://..."
                   value={link.linkUrl || ''}
                   onChange={(e) => set({ linkUrl: e.target.value })} />
          </div>
        )}

        {link.linkType === 'INTERNAL' && (
          <>
            <div className="uz-col">
              <label className="uz-label" htmlFor="lk-tt">{t('ads.targetType')}</label>
              <Select id="lk-tt" className="uz-select" value={link.internalTargetType || ''}
                      onChange={(e) => set({ internalTargetType: e.target.value || null })}>
                <option value="">{t('common.none')}</option>
                {TARGET_TYPES.map((x) => <option key={x} value={x}>{x}</option>)}
              </Select>
            </div>
            <div className="uz-col">
              <label className="uz-label" htmlFor="lk-tid">{t('ads.targetId')}</label>
              <input id="lk-tid" className="uz-input" type="number" min="1"
                     value={link.internalTargetId ?? ''}
                     onChange={(e) => set({
                       internalTargetId: e.target.value === '' ? null : Number(e.target.value),
                     })} />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
