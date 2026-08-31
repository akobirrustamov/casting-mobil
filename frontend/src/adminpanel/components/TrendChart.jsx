import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { usePanelI18n } from '../i18n';
import ChartTooltip from './charts/ChartTooltip';
import { ACTIVE_DOT_RADIUS, AXIS, STROKE_WIDTH, seriesColor } from './charts/theme';

/**
 * Kunlik dinamika grafigi.
 *
 * <h2>⚠️ Nima uchun qo'lda yozilgan SVG almashtirildi</h2>
 * Eski variantda o'q ham, tooltip ham yo'q edi. U shaklni ko'rsatardi,
 * lekin sonni bermasdi: «o'sish bor» ko'rinardi, «14-avgustda nechta»
 * degan savolga esa javob yo'q edi. Admin uchun aynan son kerak.
 *
 * Eski izohda «bir nechta turdagi grafik kerak bo'lganda qayta ko'rib
 * chiqiladi» deb yozilgan edi (§70). Aynan shu payt keldi: ustun,
 * halqa va uchqun grafiklar qo'shildi.
 *
 * <h2>API o'zgarmadi</h2>
 * {@code points}, {@code series}, {@code height}, {@code formatValue} —
 * hammasi eskisidek. To'rtta chaqiruv joyi tegilmasdan ishlaydi.
 *
 * ⚠️ Endi {@code series[].color} IXTIYORIY. Berilmasa rang tekshiruvdan
 * o'tgan palitradan tartib bo'yicha olinadi — chaqiruv joylarida rang
 * qo'lda yozilsa, ular vaqt o'tib palitradan chetga chiqib ketardi.
 */
export default function TrendChart({ points, series, height = 180, formatValue }) {
  const { t } = usePanelI18n();

  // ⚠️ Bo'sh ma'lumot — bo'sh holat, soxta grafik EMAS (§45).
  // Nol qiymatli chiziq «hech kim ko'rmagan» degan ma'noni berardi,
  // aslida esa ma'lumot umuman yo'q.
  if (!points || points.length === 0) {
    return <p className="uz-muted" style={{ fontSize: 13 }}>{t('rp.noData')}</p>;
  }

  const lines = (series && series.length ? series : [
    { key: 'views', label: t('rp.views') },
    { key: 'plays', label: t('rp.plays') },
    { key: 'completes', label: t('rp.completes') },
  ]).map((s, i) => ({ ...s, color: s.color || seriesColor(i) }));

  const format = formatValue || ((v) => Number(v).toLocaleString());

  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={points} margin={{ top: 4, right: 8, bottom: 0, left: -12 }}>
        <defs>
          {lines.map((s) => (
            /* ⚠️ To'ldirish SHAFFOF gradient.
               Qattiq rang bilan to'ldirilsa ikkita qator bir-birini
               to'sib qo'yardi va pastdagisi umuman ko'rinmasdi. */
            <linearGradient key={s.key} id={`fill-${s.key}`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={s.color} stopOpacity={0.28} />
              <stop offset="100%" stopColor={s.color} stopOpacity={0} />
            </linearGradient>
          ))}
        </defs>

        {/* Faqat gorizontal chiziqlar: ular qiymatni o'qishga yordam
            beradi. Vertikallari esa sanalar o'qi bilan takrorlanardi. */}
        <CartesianGrid vertical={false} stroke={AXIS.grid} />

        <XAxis
          dataKey="day"
          tick={AXIS.tick}
          tickLine={false}
          axisLine={{ stroke: AXIS.stroke }}
          minTickGap={24}
        />
        <YAxis
          tick={AXIS.tick}
          tickLine={false}
          axisLine={false}
          width={56}
          tickFormatter={format}
          /*
            ⚠️ Kasr belgilar YO'Q.
            Bu o'qda son turadi: obunachi, ko'rsatish, video. «0.5
            obunachi» degan belgi ma'nosiz va u brauzerda aynan
            shunday chiqdi. Kasr kerak bo'lgan o'lchov (masalan CTR)
            bu grafiklarga umuman tushmaydi — u alohida turadi.
          */
          allowDecimals={false}
        />

        <Tooltip
          content={<ChartTooltip formatValue={format} />}
          // Butun ustun bo'ylab kursor — nuqtaning o'ziga tegish shart emas.
          cursor={{ stroke: AXIS.stroke, strokeWidth: 1 }}
        />

        {/* ⚠️ Bitta qator uchun legend chizilmaydi: sarlavha uni
            allaqachon nomlagan va ortiqcha qator joy egallardi. */}
        {lines.length > 1 && (
          <Legend
            iconType="circle"
            iconSize={8}
            wrapperStyle={{ fontSize: 12, paddingTop: 4 }}
            /*
              ⚠️ Matn SERIYA RANGIDA emas.
              Recharts sukut bo'yicha yozuvni chiziq rangiga bo'yaydi.
              Qorong'i fonda bu kontrast tekshiruvidan o'tmaydi va
              rang ko'rmaydigan odam uchun ikkita yozuv bir xil
              bo'lib qoladi. Rangni yonidagi nishon tashiydi —
              u shakl, harf emas.
            */
            formatter={(value) => (
              <span style={{ color: 'var(--p-muted)' }}>{value}</span>
            )}
          />
        )}

        {lines.map((s) => (
          <Area
            key={s.key}
            type="monotone"
            dataKey={s.key}
            name={s.label}
            stroke={s.color}
            strokeWidth={STROKE_WIDTH}
            fill={`url(#fill-${s.key})`}
            // ⚠️ Har nuqtada doira chizilmaydi — 90 kunlik grafikda
            // ular bir-biriga yopishib, chiziqni yo'qotardi.
            dot={false}
            activeDot={{ r: ACTIVE_DOT_RADIUS, strokeWidth: 0 }}
            isAnimationActive={false}
          />
        ))}
      </AreaChart>
    </ResponsiveContainer>
  );
}
