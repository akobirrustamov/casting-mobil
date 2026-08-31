import {
  Bar,
  CartesianGrid,
  BarChart as RBarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { usePanelI18n } from '../../i18n';
import ChartTooltip from './ChartTooltip';
import { AXIS, seriesColor } from './theme';

/**
 * Yotiq ustunli taqqoslash — «qaysi biri ko'proq».
 *
 * <h2>Nega YOTIQ</h2>
 * Taqqoslanadigan narsalar nomlangan: reklama nomi, ijodkor ismi.
 * Tik ustunlarda bunday yozuvlar o'qi ostiga sig'maydi va ular
 * qiyshaytirib yoziladi — o'qish uchun boshni burish kerak bo'ladi.
 * Yotiq ustunda nom oddiy chapdan o'ngga o'qiladi.
 *
 * <h2>⚠️ Bitta o'q</h2>
 * Barcha qatorlar BIR XIL o'lchovda bo'lishi shart: ko'rsatishlar va
 * bosishlar — ikkalasi ham son, ularni taqqoslash mumkin.
 *
 * CTR (foiz) esa bu yerga QO'SHILMAYDI. Foizni son bilan bitta o'qqa
 * qo'yish — grafikdagi eng ko'p uchraydigan xato: 7.57% ustuni 4358
 * yonida ko'rinmas chiziqqa aylanadi yoki ikkinchi o'q qo'shiladi va
 * ikkita shkala bir-birini yolg'on taqqoslaydi.
 *
 * Foiz uchun joy — YONIDAGI JADVAL. Grafikning vazifasi taqqoslash
 * («qaysi biri katta»), jadvalniki esa aniq son. Ikkalasi bir sahifada
 * turadi va bir-birini takrorlamaydi.
 *
 * Yon foyda: jadval rang ko'rmaydigan yoki ekran o'qiydigan
 * foydalanuvchi uchun grafikning muqobili bo'lib xizmat qiladi.
 */
export default function BarChart({ data, bars, height = 220, formatValue }) {
  const { t } = usePanelI18n();

  if (!data || data.length === 0) {
    // ⚠️ Bo'sh ma'lumot — bo'sh holat, nol uzunlikdagi ustunlar emas.
    return <p className="uz-muted" style={{ fontSize: 13 }}>{t('rp.noData')}</p>;
  }

  const series = bars.map((b, i) => ({ ...b, color: b.color || seriesColor(i) }));
  const format = formatValue || ((v) => Number(v).toLocaleString());

  return (
    <div>
      <ResponsiveContainer width="100%" height={height}>
        <RBarChart
          data={data}
          layout="vertical"
          margin={{ top: 0, right: 8, bottom: 0, left: 0 }}
          barCategoryGap="22%"
          // ⚠️ Ustunlar orasida 2px bo'shliq: qo'shni ikkita rang
          // tegib tursa ular bitta ustun bo'lib o'qilardi.
          barGap={2}
        >
          {/* Faqat vertikal chiziqlar — yotiq grafikda qiymat o'qi
              gorizontal bo'ylab boradi. */}
          <CartesianGrid horizontal={false} stroke={AXIS.grid} />

          <XAxis
            type="number"
            tick={AXIS.tick}
            tickLine={false}
            axisLine={false}
            tickFormatter={format}
          />
          <YAxis
            type="category"
            dataKey="label"
            tick={AXIS.tick}
            tickLine={false}
            axisLine={{ stroke: AXIS.stroke }}
            width={140}
          />

          <Tooltip
            content={<ChartTooltip formatValue={format} />}
            // Butun qator bo'ylab — ingichka ustunga tegish qiyin.
            cursor={{ fill: 'var(--p-surface-2)' }}
          />

          {series.map((b) => (
            <Bar
              key={b.key}
              dataKey={b.key}
              name={b.label}
              fill={b.color}
              // Uchi yumaloq — ustun boshlanishi o'qdan uziladi.
              radius={[0, 4, 4, 0]}
              isAnimationActive={false}
            />
          ))}
        </RBarChart>
      </ResponsiveContainer>

      {/*
        Legend — ikkitadan ortiq qator bo'lsa.
        Matn oddiy rangda, rangni yonidagi nishon tashiydi.
      */}
      {series.length > 1 && (
        <ul className="flex flex-wrap gap-3 mt-2" style={{ listStyle: 'none', padding: 0 }}>
          {series.map((b) => (
            <li key={b.key} className="flex items-center gap-2" style={{ fontSize: 12 }}>
              <span
                aria-hidden="true"
                style={{ width: 8, height: 8, borderRadius: 2, background: b.color }}
              />
              <span className="uz-muted">{b.label}</span>
            </li>
          ))}
        </ul>
      )}

    </div>
  );
}
