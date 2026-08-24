import { useEffect, useState } from 'react';
import { adminApi } from '../../api/client';
import SearchableSelect from '../../components/SearchableSelect';
import { toBackendLocale, usePanelI18n } from '../../i18n';

const PERIODS = ['today', 'yesterday', 'last7', 'last30'];

/**
 * Hisobot filtrlari (ТЗ §47).
 *
 * <h2>Nega kerak edi</h2>
 * Backend beshta obyekt filtrini qo'llab-quvvatlardi — kontent,
 * kategoriya, ijodkor, tarif, reklama — va ular davr bilan BIRGA
 * ishlardi. Panelda esa faqat davr tugmalari bor edi, ya'ni «shu
 * kategoriya, oxirgi 7 kun» degan savolni umuman berib bo'lmasdi.
 * Yozilgan va sinalgan backend ishi interfeysda ko'rinmasdi.
 *
 * <h2>Ro'yxatlar bir marta yuklanadi</h2>
 * Kategoriya, tarif va reklama ro'yxatlari filtr o'zgarganda qayta
 * so'ralmaydi — ular hisobot davriga bog'liq emas. Har filtr
 * almashishida ularni qayta tortish to'rtta bekorga so'rov bo'lardi.
 *
 * <h2>Kontent va ijodkor — qidiruvli tanlash</h2>
 * Ularning soni cheklanmagan. Oddiy `<select>` da kerakli filmni topish
 * uchun yuzlab qatorni aylantirib chiqish kerak bo'lardi (§53).
 */
export default function ReportFilters({ value, onChange }) {
  const { t, locale } = usePanelI18n();
  const bl = toBackendLocale(locale);

  const [options, setOptions] = useState({
    content: [], categories: [], creators: [], tariffs: [], ads: [],
  });

  useEffect(() => {
    let cancelled = false;

    // ⚠️ Beshta so'rov PARALLEL ketadi (§73). Ketma-ket bo'lsa filtr
    // paneli beshta aylanish vaqtidan keyin to'liq bo'lardi.
    Promise.all([
      adminApi.content({ page: 0, size: 200 }).catch(() => null),
      adminApi.categories({ page: 0, size: 200 }).catch(() => null),
      adminApi.creators({ page: 0, size: 200 }).catch(() => null),
      adminApi.tariffs().catch(() => null),
      adminApi.advertisements().catch(() => null),
    ]).then(([content, categories, creators, tariffs, ads]) => {
      if (cancelled) return;

      const trTitle = (row) =>
        row.translations?.[bl]?.title || row.translations?.UZ?.title || row.slug;

      const trName = (row) => {
        const tr = row.translations?.[bl] || row.translations?.UZ;
        return tr?.displayName
          || [tr?.firstName, tr?.lastName].filter(Boolean).join(' ')
          || row.slug;
      };

      setOptions({
        content: (content?.items || []).map((c) => ({ id: c.id, label: trTitle(c) })),
        categories: (categories?.items || []).map((c) => ({ id: c.id, label: trTitle(c) })),
        creators: (creators?.items || []).map((c) => ({ id: c.id, label: trName(c) })),
        tariffs: (tariffs || []).map((x) => ({
          id: x.id,
          label: x.translations?.[bl]?.title || x.translations?.UZ?.title || x.code,
        })),
        ads: (ads || []).map((a) => ({ id: a.id, label: a.name || `#${a.id}` })),
      });
    });

    return () => { cancelled = true; };
  }, [bl]);

  const set = (patch) => onChange({ ...value, ...patch });

  /**
   * Davr tugmasi bosilganda qo'lda kiritilgan sanalar TOZALANADI.
   *
   * Backend `period` va `from`/`to` ni birga qabul qiladi, lekin
   * ulardan qaysi biri ustun ekani foydalanuvchiga ko'rinmasdi:
   * «oxirgi 7 kun» tugmasi tanlangan holda eski sanalar qolib ketsa,
   * admin qaysi davr amalda ekanini bilmasdi.
   */
  const pickPeriod = (period) => set({ period, from: '', to: '' });

  /** Sana kiritilganda davr tugmasi bo'shatiladi — sabab yuqoridagi kabi. */
  const pickDate = (patch) => set({ ...patch, period: '' });

  const active = [
    value.contentId, value.categoryId, value.creatorId,
    value.tariffId, value.advertisementId,
  ].filter(Boolean).length;

  const dirty = active > 0 || Boolean(value.from) || Boolean(value.to)
    || value.period !== 'last30';

  const clear = () => onChange({
    period: 'last30', from: '', to: '',
    contentId: '', categoryId: '', creatorId: '', tariffId: '', advertisementId: '',
  });

  return (
    <div className="uz-card p-5 mb-6">
      <div className="flex items-center justify-between gap-3 flex-wrap mb-4">
        <div className="flex gap-2 flex-wrap" role="group" aria-label={t('rp.title')}>
          {PERIODS.map((p) => (
            <button key={p} type="button"
                    className={`uz-chip ${value.period === p ? 'selected' : ''}`}
                    aria-pressed={value.period === p}
                    onClick={() => pickPeriod(p)}>
              {t(`rp.${p}`)}
            </button>
          ))}
        </div>

        <div className="flex items-end gap-3 flex-wrap">
          <div>
            <label className="uz-label" htmlFor="rp-from">{t('rp.from')}</label>
            <input id="rp-from" type="date" className="uz-input" style={{ maxWidth: 165 }}
                   value={value.from}
                   onChange={(e) => pickDate({ from: e.target.value })} />
          </div>
          <div>
            <label className="uz-label" htmlFor="rp-to">{t('rp.to')}</label>
            <input id="rp-to" type="date" className="uz-input" style={{ maxWidth: 165 }}
                   value={value.to}
                   onChange={(e) => pickDate({ to: e.target.value })} />
          </div>
          {dirty && (
            <button type="button" className="uz-btn uz-btn-ghost" onClick={clear}>
              {t('rp.clearFilters')}
            </button>
          )}
        </div>
      </div>

      <div className="uz-row">
        <div className="uz-col">
          <label className="uz-label">{t('rp.filterContent')}</label>
          <SearchableSelect
            value={value.contentId}
            options={options.content}
            ariaLabel={t('rp.filterContent')}
            /* Tanlanmagan holat «Tanlang...» emas, «Barchasi» deb
               o'qilishi kerak: filtrda bo'sh qiymat — bu majburiy
               tanlov emas, «cheklamaslik» degani. */
            placeholder={t('rp.allContent')}
            emptyLabel={t('rp.allContent')}
            onChange={(id) => set({ contentId: id || '' })}
          />
        </div>
        <div className="uz-col">
          <label className="uz-label">{t('rp.filterCategory')}</label>
          <SearchableSelect
            value={value.categoryId}
            options={options.categories}
            ariaLabel={t('rp.filterCategory')}
            /* Tanlanmagan holat «Tanlang...» emas, «Barchasi» deb
               o'qilishi kerak: filtrda bo'sh qiymat — bu majburiy
               tanlov emas, «cheklamaslik» degani. */
            placeholder={t('rp.allCategories')}
            emptyLabel={t('rp.allCategories')}
            onChange={(id) => set({ categoryId: id || '' })}
          />
        </div>
        <div className="uz-col">
          <label className="uz-label">{t('rp.filterCreator')}</label>
          <SearchableSelect
            value={value.creatorId}
            options={options.creators}
            ariaLabel={t('rp.filterCreator')}
            /* Tanlanmagan holat «Tanlang...» emas, «Barchasi» deb
               o'qilishi kerak: filtrda bo'sh qiymat — bu majburiy
               tanlov emas, «cheklamaslik» degani. */
            placeholder={t('rp.allCreators')}
            emptyLabel={t('rp.allCreators')}
            onChange={(id) => set({ creatorId: id || '' })}
          />
        </div>
      </div>

      <div className="uz-row mt-3">
        <div className="uz-col">
          <label className="uz-label" htmlFor="rp-tariff">{t('rp.filterTariff')}</label>
          <select id="rp-tariff" className="uz-select" value={value.tariffId}
                  onChange={(e) => set({ tariffId: e.target.value })}>
            <option value="">{t('rp.allTariffs')}</option>
            {options.tariffs.map((o) => (
              <option key={o.id} value={o.id}>{o.label}</option>
            ))}
          </select>
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="rp-ad">{t('rp.filterAd')}</label>
          <select id="rp-ad" className="uz-select" value={value.advertisementId}
                  onChange={(e) => set({ advertisementId: e.target.value })}>
            <option value="">{t('rp.allAds')}</option>
            {options.ads.map((o) => (
              <option key={o.id} value={o.id}>{o.label}</option>
            ))}
          </select>
        </div>
      </div>

      <p className="uz-muted mt-4" style={{ fontSize: 12, lineHeight: 1.6 }}>
        {t('rp.contentFilterNote')}
      </p>
      <p className="uz-muted mt-1" style={{ fontSize: 12, lineHeight: 1.6 }}>
        {t('rp.adFilterNote')}
      </p>
    </div>
  );
}

/** Filtrni backend kutgan so'rov parametrlariga o'giradi. */
export function toReportParams(value) {
  return {
    // Qo'lda sana kiritilgan bo'lsa davr yuborilmaydi — ikkalasi birga
    // ketsa qaysi biri amal qilgani javobdan bilinmasdi.
    period: value.from || value.to ? undefined : (value.period || undefined),
    from: value.from || undefined,
    to: value.to || undefined,
    contentId: value.contentId || undefined,
    categoryId: value.categoryId || undefined,
    creatorId: value.creatorId || undefined,
    tariffId: value.tariffId || undefined,
    advertisementId: value.advertisementId || undefined,
  };
}

export const EMPTY_REPORT_FILTER = {
  period: 'last30', from: '', to: '',
  contentId: '', categoryId: '', creatorId: '', tariffId: '', advertisementId: '',
};
