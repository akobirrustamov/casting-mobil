import { render, screen } from '@testing-library/react';

import BarChart from '../charts/BarChart';
import DonutChart from '../charts/DonutChart';
import { SERIES, seriesColor } from '../charts/theme';
import TrendChart from '../TrendChart';
import { PanelI18nProvider } from '../../i18n';

/**
 * Grafiklar.
 *
 * <h2>⚠️ Nima qo'riqlanadi</h2>
 * Grafikdagi xato «chiroyli emas» bo'lib emas, YOLG'ON ma'lumot bo'lib
 * ko'rinadi: bo'sh ma'lumot nol chiziq bo'lib chizilsa, admin «hech
 * kim ko'rmagan» deb xulosa qiladi — aslida esa raqam umuman yo'q.
 *
 * Shuning uchun tekshiriladigan narsalar ko'rinish emas, MA'NO:
 * bo'sh holat, jami sonning to'g'riligi va ranglarning takrorlanmasligi.
 */

function show(ui) {
  return render(<PanelI18nProvider>{ui}</PanelI18nProvider>);
}

describe('Palitra', () => {
  /**
   * ⚠️ Aynan to'rtta. Beshinchisi qorong'i yuzada ishonchli
   * ajralmaydi — bu `dataviz` validatori bilan hisoblangan, taxmin
   * emas.
   */
  it('to\'rtta rangdan iborat', () => {
    expect(SERIES).toHaveLength(4);
  });

  it('ranglar takrorlanmaydi', () => {
    expect(new Set(SERIES).size).toBe(SERIES.length);
  });

  /**
   * ⚠️ ENG MUHIM QOIDA.
   *
   * Ilgari ranglar `% uzunlik` bilan aylantirilardi: to'rtinchi qator
   * birinchisi bilan bir xil rangda chizilardi va ikkalasini ajratib
   * bo'lmasdi.
   *
   * Aylantirish o'rniga oxirgi rang qaytariladi — bu ham to'g'ri
   * emas, lekin u KO'RINADI: ikkita bir xil rang darhol sezilib,
   * «bu yerda faceting kerak» degan savolni tug'diradi.
   */
  it('ranglar AYLANTIRILMAYDI', () => {
    expect(seriesColor(4)).not.toBe(seriesColor(0));
    expect(seriesColor(9)).not.toBe(seriesColor(1));
  });

  it('har bir slot o\'z rangini beradi', () => {
    SERIES.forEach((color, i) => expect(seriesColor(i)).toBe(color));
  });

  /**
   * ⚠️ Status ranglari seriya rangi sifatida ishlatilmaydi.
   *
   * Yashil «yaxshi», sariq «e'tibor ber» degan ma'no tashiydi. Oddiy
   * uchinchi qator uchun ishlatilsa bu ma'no yo'qoladi.
   */
  it('status ranglari palitraga kirmaydi', () => {
    const status = ['var(--p-success)', 'var(--p-warning)', 'var(--p-danger)'];
    status.forEach((c) => expect(SERIES).not.toContain(c));
  });
});

describe('Dinamika grafigi', () => {
  /**
   * ⚠️ Bo'sh ma'lumot — bo'sh holat, NOL CHIZIQ emas.
   *
   * Nol qiymatli chiziq «hech kim ko'rmagan» degan ma'noni berardi,
   * aslida esa ma'lumot umuman yo'q. Bu ikki boshqa xulosa.
   */
  it.each([[[]], [null], [undefined]])('ma\'lumotsiz bo\'sh holat ko\'rsatadi', (points) => {
    const { container } = show(<TrendChart points={points} />);

    expect(container.querySelector('svg')).toBeNull();
    expect(screen.getByText(/ma'lumot|нет данных|no data/i)).toBeInTheDocument();
  });

  /**
   * ⚠️ «SVG chizildimi» bu yerda TEKSHIRILMAYDI.
   *
   * jsdom da elementning o'lchami har doim nol, `ResponsiveContainer`
   * esa ota elementni o'lchab hech narsa chizmaydi. Uni aldab
   * o'tkazish mumkin, lekin unda test muhitni sinagan bo'lardi,
   * grafikni emas.
   *
   * Chizilishi brauzerda ko'z bilan tekshiriladi — bu yerda esa
   * MA'NO tekshiriladi: bo'sh holat, jami son, ranglar.
   */
  it('ma\'lumot bo\'lsa bo\'sh holat KO\'RSATILMAYDI', () => {
    show(
      <TrendChart
        points={[{ day: '01', value: 5 }, { day: '02', value: 9 }]}
        series={[{ key: 'value', label: 'Ko\'rishlar' }]}
      />,
    );

    expect(screen.queryByText(/ma'lumot yo'q|нет данных|no data/i)).not.toBeInTheDocument();
  });
});

describe('Tarkib grafigi', () => {
  const rows = [
    { label: 'Nashr', value: 80 },
    { label: 'Qoralama', value: 30 },
  ];

  it('jami markazda ko\'rsatiladi', () => {
    show(<DonutChart data={rows} />);

    expect(screen.getByText('110')).toBeInTheDocument();
  });

  /**
   * ⚠️ Nol qiymatli bo'lak chizilmaydi: u ko'rinmas bo'lak bo'lib,
   * legendda esa nomi turaverardi.
   */
  it('nol qiymatlar tashlab yuboriladi', () => {
    show(<DonutChart data={[...rows, { label: 'Rejada', value: 0 }]} />);

    expect(screen.queryByText('Rejada')).not.toBeInTheDocument();
  });

  it('hammasi nol bo\'lsa bo\'sh holat', () => {
    show(<DonutChart data={[{ label: 'Nashr', value: 0 }]} />);

    expect(screen.getByText(/ma'lumot|нет данных|no data/i)).toBeInTheDocument();
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV.
   *
   * To'rtdan ortiq bo'lak «Boshqalar» ga YIG'ILADI, kesib
   * tashlanmaydi. Oddiy `slice(0, 4)` markazdagi jami sonni
   * bo'laklar yig'indisidan katta qilib qo'yardi — va bu farqni
   * tushuntirib bo'lmasdi.
   */
  it('ortiqcha bo\'laklar «Boshqalar» ga yig\'iladi, yo\'qolmaydi', () => {
    show(<DonutChart data={[
      { label: 'A', value: 50 },
      { label: 'B', value: 40 },
      { label: 'C', value: 30 },
      { label: 'D', value: 20 },
      { label: 'E', value: 10 },
    ]} />);

    // «Boshqalar» qatori bor va u 20 + 10 ni bildiradi.
    expect(screen.getByText(/Boshqalar|Прочее|Other/)).toBeInTheDocument();
    // ⚠️ Jami O'ZGARMAGAN — kesib tashlanganda u 120 bo'lib qolardi.
    expect(screen.getByText('150')).toBeInTheDocument();
  });

  /**
   * Legend har doim bor: halqada bo'laklar faqat rang bilan
   * farqlanadi va legendsiz kimligini bilib bo'lmaydi.
   */
  it('har bir bo\'lak legendda nomlanadi', () => {
    show(<DonutChart data={rows} />);

    expect(screen.getByText('Nashr')).toBeInTheDocument();
    expect(screen.getByText('Qoralama')).toBeInTheDocument();
  });
});

describe('Taqqoslash grafigi', () => {
  const ads = [
    { label: 'Yozgi chegirma', impressions: 1572, clicks: 120 },
    { label: 'Premium taklifi', impressions: 1558, clicks: 117 },
  ];
  const bars = [
    { key: 'impressions', label: 'Ko\'rsatishlar' },
    { key: 'clicks', label: 'Bosishlar' },
  ];

  it('ma\'lumotsiz bo\'sh holat ko\'rsatadi', () => {
    show(<BarChart data={[]} bars={bars} />);

    expect(screen.getByText(/ma'lumot|нет данных|no data/i)).toBeInTheDocument();
  });

  it('ikkita qator uchun legend chizadi', () => {
    show(<BarChart data={ads} bars={bars} />);

    expect(screen.getByText('Ko\'rsatishlar')).toBeInTheDocument();
    expect(screen.getByText('Bosishlar')).toBeInTheDocument();
  });

  /**
   * ⚠️ Bitta qator uchun legend ortiqcha — sarlavha uni allaqachon
   * nomlagan va qo'shimcha qator bekorga joy egallardi.
   */
  it('bitta qator uchun legend chizmaydi', () => {
    show(<BarChart data={ads} bars={[bars[0]]} />);

    expect(screen.queryByText('Bosishlar')).not.toBeInTheDocument();
  });
});

describe('⚠️ Foiz va son bitta o\'qqa qo\'shilmaydi', () => {
  /**
   * Bu grafiklardagi eng ko'p uchraydigan xato.
   *
   * CTR — foiz (7.57), ko'rsatishlar — son (4358). Ularni bitta o'qqa
   * qo'yish 7.57 ustunini ko'rinmas chiziqqa aylantiradi; ikkinchi o'q
   * qo'shish esa ikkita shkalani bir-biriga yolg'on taqqoslatadi.
   *
   * Shuning uchun hisobotlar sahifasida CTR grafikka EMAS, yonidagi
   * jadvalga tushadi. Bu test aynan shuni qo'riqlaydi.
   */
  it('reklama grafigida CTR qatori yo\'q', () => {
    const source = require('fs').readFileSync(
      'src/adminpanel/pages/ReportsPage.jsx', 'utf8',
    );

    const start = source.indexOf('<BarChart');
    expect(start).toBeGreaterThan(0);

    const chart = source.slice(start, source.indexOf('/>', start));
    expect(chart).not.toMatch(/ctr/i);
  });

  /**
   * ⚠️ Valyutalar ham qo'shilmaydi: 100 yulduz va 100 tanga bir xil
   * son, lekin ular boshqa narsa. Backend DTO izohida bu qoida ochiq
   * yozilgan va sahifa unga amal qilishi kerak.
   */
  it('donatlar valyuta bo\'yicha AJRATILADI', () => {
    const source = require('fs').readFileSync(
      'src/adminpanel/pages/DonationsPage.jsx', 'utf8',
    );

    // ⚠️ Ta'rifning borligi YETARLI EMAS — u chaqirilishi kerak.
    // Avvalgi variant faqat nomni qidirardi va valyutalar bitta
    // grafikka qo'shib yuborilganda ham yashil qolaverdi.
    expect(source).toMatch(/groupByKind\(\s*report\.data\?\.daily/);
    expect(source).toMatch(/groupByKind\(\s*\n?\s*report\.data\?\.monthly/);
  });
});
