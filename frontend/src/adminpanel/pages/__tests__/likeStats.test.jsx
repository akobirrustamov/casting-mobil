/**
 * «Yoqdi» statistikasi kontent oynasida.
 *
 * <h2>Nima bu yerda jim buziladi</h2>
 * Uchta narsa, va uchalasi ham skrinshotda to'g'ri ko'rinadi:
 *
 * 1. <b>Davr soni va JAMI son chalkashishi.</b> Ilovada kontent ostida
 *    jami turadi, hisobotda esa davr bo'yicha son. Ikkalasi bitta
 *    katakka tushib qolsa, admin «ilovada boshqacha ko'rsatilyapti»
 *    deb xato qidirardi — aslida ikkala son ham to'g'ri.
 * 2. <b>Kunlik ustun.</b> U yo'qolsa jadval to'la ko'rinaveradi,
 *    faqat «yoqdi» qayerdan kelganini bilib bo'lmasdi.
 * 3. <b>Ogohlantirish.</b> Yechilgan «yoqdi» o'tmish sonidan ham
 *    yo'qoladi. Bu yozuv olib tashlansa, hisobot buzuqdek ko'rinardi
 *    va bu xulosa mantiqan to'g'ri bo'lardi.
 */
import { render, screen, waitFor } from '@testing-library/react';
import ContentStatsModal from '../reports/ContentStatsModal';
import ReportsPage from '../ReportsPage';
import { PanelI18nProvider } from '../../i18n';

jest.mock('../../api/client', () => ({
  adminApi: {
    contentStatistics: jest.fn(),
    reportOverview: jest.fn(),
    // Filtrlar ro'yxati bu testning mavzusi emas — bo'sh javob yetarli.
    content: jest.fn().mockResolvedValue({ items: [] }),
    categories: jest.fn().mockResolvedValue({ items: [] }),
    creators: jest.fn().mockResolvedValue({ items: [] }),
    tariffs: jest.fn().mockResolvedValue([]),
    advertisements: jest.fn().mockResolvedValue([]),
  },
}));

// Grafik bu testning mavzusi emas — u `charts.test.jsx` da qamralgan.
jest.mock('../../components/TrendChart', () => () => <div data-testid="trend" />);
jest.mock('../../components/charts/BarChart', () => () => <div data-testid="bars" />);

const { adminApi } = require('../../api/client');

const STATS = {
  contentId: 7,
  from: '2026-08-11',
  to: '2026-09-09',
  views: 1200,
  plays: 400,
  completes: 100,
  uniqueViewers: 900,
  playRate: 33.3,
  completionRate: 25,
  likes: 42,
  likesTotal: 137,
  daily: [
    { date: '2026-09-08', views: 500, plays: 200, completes: 50,
      uniqueViewers: 400, completionRate: 25, likes: 17 },
    { date: '2026-09-09', views: 700, plays: 200, completes: 50,
      uniqueViewers: 500, completionRate: 25, likes: 25 },
  ],
};

function open(data = STATS) {
  adminApi.contentStatistics.mockResolvedValue(data);
  return render(
    <PanelI18nProvider>
      <ContentStatsModal content={{ id: 7 }} name="Film" onClose={() => {}} />
    </PanelI18nProvider>
  );
}

beforeEach(() => {
  adminApi.contentStatistics.mockReset();
});

test('davr soni va jami son ALOHIDA ko\'rsatiladi', async () => {
  open();

  // ⚠️ Ikkalasi ham bo'lishi shart. Bittasi yo'qolsa qolgani to'g'ri
  // ko'rinadi, lekin savolga javob bermaydi.
  await waitFor(() => expect(screen.getByText('42')).toBeInTheDocument());
  expect(screen.getByText('137')).toBeInTheDocument();
});

test('kunlik jadvalda «yoqdi» ustuni bor', async () => {
  open();

  await waitFor(() => expect(screen.getByText('17')).toBeInTheDocument());
  expect(screen.getByText('25')).toBeInTheDocument();
});

test('yechilgan «yoqdi» haqida ogohlantirish yozilgan', async () => {
  open();

  // Panel sukut bo'yicha o'zbekcha (`i18n.LOCALES` dagi birinchi til).
  // Apostrofsiz bo'lak tanlangan: tarjima matnida ular ekranlangan va
  // qidiruv shablonini o'qib bo'lmas qilardi.
  await waitFor(() =>
    expect(screen.getByText(/olib tashlanganda/)).toBeInTheDocument()
  );
});

test('nol — bu ma\'lumot yo\'qligi emas, sanoq', async () => {
  // ⚠️ Kontentda hali «yoqdi» bo'lmasligi normal holat. Nol o'rniga
  // bo'sh katak chiqsa, admin «hisobot ishlamayapti» deb o'ylardi.
  open({ ...STATS, likes: 0, likesTotal: 0, daily: [] });

  await waitFor(() => expect(adminApi.contentStatistics).toHaveBeenCalled());
  expect(screen.getAllByText('0').length).toBeGreaterThanOrEqual(2);
});

/**
 * Umumiy hisobot sahifasi.
 *
 * ⚠️ Bu yerda «yoqdi» reklama ko'rsatkichlaridan OLDIN turadi va
 * jadvalda o'z ustuniga ega. Ikkalasi ham osongina tushib qolishi
 * mumkin: sahifa baribir to'la ko'rinaveradi, faqat savolga javob
 * bermaydi.
 */
describe('umumiy hisobot', () => {
  const OVERVIEW = {
    appliedFilters: {},
    from: '2026-08-11',
    to: '2026-09-09',
    totalViews: 5000,
    totalPlays: 2000,
    totalCompletes: 500,
    completionRate: 25,
    totalLikes: 312,
    adImpressions: 100,
    adClicks: 10,
    adCtr: 10,
    pendingEvents: 0,
    subscriptionRevenue: 0,
    series: [{ day: '2026-09-09', views: 5000, plays: 2000, completes: 500 }],
    topContent: [
      { contentId: 7, slug: 'film-bir', views: 3000, plays: 1200,
        completes: 300, uniqueViewers: 2500, likes: 200 },
    ],
    topAds: [],
  };

  beforeEach(() => {
    adminApi.reportOverview.mockResolvedValue(OVERVIEW);
  });

  test('umumiy «yoqdi» soni chiqadi', async () => {
    render(
      <PanelI18nProvider>
        <ReportsPage />
      </PanelI18nProvider>
    );

    await waitFor(() => expect(screen.getByText('312')).toBeInTheDocument());
  });

  test('top kontent jadvalida «yoqdi» ustuni bor', async () => {
    render(
      <PanelI18nProvider>
        <ReportsPage />
      </PanelI18nProvider>
    );

    await waitFor(() => expect(screen.getByText('film-bir')).toBeInTheDocument());
    // ⚠️ Aynan qator ichida: sahifada boshqa joyda ham 200 chiqishi
    // mumkin, shuning uchun qatordan qidiramiz.
    const row = screen.getByText('film-bir').closest('tr');
    expect(row).toHaveTextContent('200');
  });
});
