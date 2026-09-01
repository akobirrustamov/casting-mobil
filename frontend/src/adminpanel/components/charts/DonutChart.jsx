import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';

import { usePanelI18n } from '../../i18n';
import ChartTooltip from './ChartTooltip';
import { seriesColor } from './theme';

/**
 * Tarkib grafigi — butunning qismlarga bo'linishi.
 *
 * <h2>Qachon ishlatiladi</h2>
 * Faqat BUTUNNING qismlari uchun: kontent holati bo'yicha taqsimot,
 * valyutalar ulushi. Ular qo'shilganda 100% chiqishi shart.
 *
 * ⚠️ Bir-biriga bog'liq bo'lmagan sonlar uchun ishlatilmaydi.
 * «Foydalanuvchilar 1200, videolar 80» — bu butunning qismlari emas
 * va ularni bitta doiraga qo'yish ma'nosiz raqam yasardi.
 *
 * <h2>⚠️ Nega halqa, to'liq doira emas</h2>
 * O'rtadagi bo'shliqqa JAMI yoziladi. To'liq doirada bu son yonga
 * chiqarilardi va o'qiydigan odam «bu nimaning jamisi» degan savolga
 * ko'zi bilan javob izlardi.
 *
 * <h2>⚠️ Bo'laklar soni cheklangan</h2>
 * Palitrada to'rtta rang bor va beshinchisi ishonchli ajralmaydi.
 * Ortiqchasi «Boshqalar» ga yig'iladi — yangi rang o'ylab topilmaydi.
 */

/** Ortiqcha bo'laklar shu nom ostida yig'iladi. */
const MAX_SLICES = 4;

export default function DonutChart({ data, height = 180, formatValue, total, totalLabel }) {
  const { t } = usePanelI18n();

  const rows = (data || []).filter((d) => Number(d.value) > 0);

  // ⚠️ Bo'sh ma'lumot — bo'sh holat, nol bo'lakli doira emas.
  // Bo'sh halqa «hammasi nol» degan ma'noni berardi, aslida esa
  // ma'lumot umuman yo'q.
  if (rows.length === 0) {
    return <p className="uz-muted" style={{ fontSize: 13 }}>{t('rp.noData')}</p>;
  }

  const slices = foldTail(rows, t('chart.other'));
  const format = formatValue || ((v) => Number(v).toLocaleString());
  const sum = total ?? slices.reduce((acc, s) => acc + Number(s.value), 0);

  return (
    <div style={{ position: 'relative' }}>
      <ResponsiveContainer width="100%" height={height}>
        <PieChart>
          <Pie
            data={slices}
            dataKey="value"
            nameKey="label"
            innerRadius="70%"
            outerRadius="86%"
            // ⚠️ Bo'laklar orasida 2px bo'shliq: qo'shni ikkita rang
            // tegib tursa, chegara ko'rinmay ular bitta bo'lak bo'lib
            // o'qilardi.
            paddingAngle={2}
            stroke="var(--p-surface)"
            strokeWidth={2}
            isAnimationActive={false}
          >
            {slices.map((s, i) => (
              <Cell key={s.label} fill={seriesColor(i)} />
            ))}
          </Pie>
          <Tooltip content={<ChartTooltip formatValue={format} />} />
        </PieChart>
      </ResponsiveContainer>

      {/* Jami — halqa markazida */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          pointerEvents: 'none',
        }}
      >
        {/*
          ⚠️ Matn halqa ICHIGA sig'ishi kerak.
          Kengroq yozuv yoyga tegib, harflari kesilib qolardi —
          brauzerda aynan shunday chiqdi.
        */}
        <div className="uz-mono" style={{ fontSize: 20, fontWeight: 700, lineHeight: 1.1 }}>
          {format(sum)}
        </div>
        {totalLabel && (
          <div
            className="uz-muted"
            style={{ fontSize: 10, maxWidth: '58%', textAlign: 'center', lineHeight: 1.2 }}
          >
            {totalLabel}
          </div>
        )}
      </div>

      {/*
        ⚠️ Legend HAR DOIM bor.
        Halqada bo'laklar faqat rang bilan farqlanadi, ya'ni legendsiz
        kimligini bilib bo'lmaydi — rang ko'rmaydigan odam uchun esa
        grafik umuman ma'nosiz bo'lardi.
      */}
      <ul className="flex flex-wrap gap-3 mt-3" style={{ listStyle: 'none', padding: 0 }}>
        {slices.map((s, i) => (
          <li key={s.label} className="flex items-center gap-2" style={{ fontSize: 12 }}>
            <span
              aria-hidden="true"
              style={{ width: 8, height: 8, borderRadius: 2, background: seriesColor(i) }}
            />
            <span className="uz-muted">{s.label}</span>
            <span className="uz-mono">{format(s.value)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/**
 * To'rtdan ortiq bo'lakni «Boshqalar» ga yig'adi.
 *
 * ⚠️ Kesib tashlamaydi, QO'SHADI. Oddiy `slice(0, 4)` jami sonni
 * buzardi: halqa markazidagi raqam bo'laklar yig'indisidan katta
 * bo'lib qolardi va uni tushuntirib bo'lmasdi.
 */
function foldTail(rows, otherLabel) {
  const sorted = [...rows].sort((a, b) => Number(b.value) - Number(a.value));
  if (sorted.length <= MAX_SLICES) {
    return sorted;
  }

  const head = sorted.slice(0, MAX_SLICES - 1);
  const tail = sorted.slice(MAX_SLICES - 1);
  return [
    ...head,
    { label: otherLabel, value: tail.reduce((acc, r) => acc + Number(r.value), 0) },
  ];
}
