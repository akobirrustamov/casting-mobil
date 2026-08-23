import { POLICIES } from './constants';
/**
 * Kirish siyosati va narx (ТЗ §23).
 *
 * Narx bo'sh qoldirilsa sozlamadagi standart qiymat ishlatiladi —
 * u kodda emas, admin panelda boshqariladi.
 */
export default function AccessTab({ form, set, t }) {
  return (
      <div className="uz-row">
        <div className="uz-col">
          <label className="uz-label" htmlFor="ap">{t('editor.accessPolicy')}</label>
          <select id="ap" className="uz-select" value={form.accessPolicy}
                  onChange={(e) => set({ accessPolicy: e.target.value })}>
            {POLICIES.map((x) => <option key={x} value={x}>{x.replace(/_/g, ' ')}</option>)}
          </select>
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="pp">{t('editor.premierePrice')}</label>
          <input id="pp" className="uz-input" type="number" min="0" step="1000"
                 value={form.premierePrice} disabled={form.accessPolicy === 'FREE'}
                 onChange={(e) => set({ premierePrice: e.target.value })} />
        </div>
      </div>
  );
}
