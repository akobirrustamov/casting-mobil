/**
 * Yo'riqnoma sahifasi.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * <ul>
 *   <li>xodim FAQAT o'ziga ochiq ishlarni asosiy ro'yxatda ko'radi —
 *       aks holda u kira olmaydigan bo'limlarni menyudan izlab
 *       yurardi;</li>
 *   <li>ruxsat yetmaydiganlar YASHIRILMAYDI, lekin alohida turadi va
 *       qaysi ruxsat kerakligini aytadi;</li>
 *   <li>ruxsat qoidasi panel bilan BIR XIL — `can()` va `atLeast()`.</li>
 * </ul>
 */
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import HelpPage from '../HelpPage';
import { PanelI18nProvider } from '../../i18n';

const mockAuth = { user: null, can: () => false, atLeast: () => false };

jest.mock('../../auth/AuthContext', () => ({
  useAuth: () => mockAuth,
}));

function renderAs({ role, permissions = [] }) {
  const ROLE_LEVEL = { WORKER: 1, ADMIN: 2, SUPER_ADMIN: 3, HYPER_ADMIN: 4 };
  mockAuth.user = { role, permissions };
  mockAuth.can = (p) =>
    (ROLE_LEVEL[role] || 0) >= ROLE_LEVEL.ADMIN || permissions.includes(p);
  mockAuth.atLeast = (r) => (ROLE_LEVEL[role] || 0) >= (ROLE_LEVEL[r] || 0);

  return render(
    <PanelI18nProvider>
      <HelpPage />
    </PanelI18nProvider>,
  );
}

describe('Ruxsatga qarab filtrlash', () => {
  test('kontent muharriri O\'ZIGA ochiq ishlarni ko\'radi', () => {
    renderAs({ role: 'WORKER', permissions: ['CONTENT_CREATE', 'CONTENT_EDIT'] });

    expect(screen.getByText(/Yangi kontent qo'shish/)).toBeInTheDocument();
    expect(screen.getByText(/Fasl va qism/)).toBeInTheDocument();
  });

  /**
   * ⚠️ Eng muhim tekshiruv.
   *
   * Ochiq ishlar ro'yxatida faqat o'ziniki bo'lishi kerak. Aks holda
   * xodim kira olmaydigan bo'lim haqida o'qib, uni menyudan izlab
   * yurardi.
   */
  test('ruxsati yo\'q ish ASOSIY ro\'yxatda chiqmaydi', () => {
    renderAs({ role: 'WORKER', permissions: ['CONTENT_CREATE'] });

    // Xodimlar bo'limi ADMIN talab qiladi.
    const locked = screen.getByRole('button', { name: /Ruxsat yetmaydigan/ });
    expect(locked).toBeInTheDocument();

    // Sarlavha faqat yopiq ro'yxatda — u hali ochilmagan.
    expect(screen.queryByText(/Xodimlar va ruxsatlar/)).not.toBeInTheDocument();
  });

  test('ADMIN uchun xodimlar bo\'limi OCHIQ', () => {
    renderAs({ role: 'ADMIN' });

    expect(screen.getByText(/Xodimlar va ruxsatlar/)).toBeInTheDocument();
  });

  /**
   * ⚠️ Ruxsatsiz ishlar YASHIRILMAYDI.
   *
   * Xodim «bunday ish bormi?» degan savolga javob topishi va
   * admindan aynan qaysi ruxsatni so'rashni bilishi kerak.
   */
  test('ruxsatsiz ishlar ro\'yxati ochiladi va RUXSAT NOMINI aytadi', async () => {
    renderAs({ role: 'WORKER', permissions: ['CONTENT_CREATE'] });

    await userEvent.click(screen.getByRole('button', { name: /Ruxsat yetmaydigan/ }));

    expect(screen.getByText(/Xodimlar va ruxsatlar/)).toBeInTheDocument();
    // Nima so'rash kerakligi ko'rsatiladi.
    expect(screen.getByText('NOTIFICATION_SEND')).toBeInTheDocument();
  });
});

describe('Mavzu tafsiloti', () => {
  test('bosilganda QADAMLAR ochiladi', async () => {
    renderAs({ role: 'WORKER', permissions: ['MEDIA_UPLOAD'] });

    const topic = screen.getByRole('button', { name: /Video va rasm yuklash/ });
    expect(topic).toHaveAttribute('aria-expanded', 'false');

    await userEvent.click(topic);

    expect(topic).toHaveAttribute('aria-expanded', 'true');
    // ⚠️ Aynan shu ogohlantirish muhim: mkv yuklanadi, lekin
    // o'ynatilmaydi.
    expect(screen.getByText(/mkv va avi qabul qilinadi/)).toBeInTheDocument();
  });

  test('ochiq mavzu kerakli RUXSATNI ko\'rsatadi', async () => {
    renderAs({ role: 'WORKER', permissions: ['MEDIA_UPLOAD'] });

    await userEvent.click(screen.getByRole('button', { name: /Video va rasm yuklash/ }));

    expect(screen.getByText('MEDIA_UPLOAD')).toBeInTheDocument();
  });

  /**
   * ⚠️ Qayta bosish YOPADI.
   *
   * Usiz ochilgan mavzuni yopib bo'lmasdi — faqat boshqasini
   * ochish orqali. Uzun ro'yxatda bu bezovta qiladi.
   */
  test('ochiq mavzuni qayta bosish uni YOPADI', async () => {
    renderAs({ role: 'WORKER', permissions: ['MEDIA_UPLOAD'] });

    const topic = screen.getByRole('button', { name: /Video va rasm yuklash/ });

    await userEvent.click(topic);
    expect(topic).toHaveAttribute('aria-expanded', 'true');

    await userEvent.click(topic);
    expect(topic).toHaveAttribute('aria-expanded', 'false');
  });

  test('bir vaqtda FAQAT bitta mavzu ochiq turadi', async () => {
    renderAs({ role: 'WORKER', permissions: ['MEDIA_UPLOAD', 'MEDIA_VIEW'] });

    const upload = screen.getByRole('button', { name: /Video va rasm yuklash/ });
    const status = screen.getByRole('button', { name: /qayta ishlash holati/i });

    await userEvent.click(upload);
    expect(upload).toHaveAttribute('aria-expanded', 'true');

    await userEvent.click(status);
    // Uzun ro'yxatda ikkalasi ham ochiq qolsa sahifa cho'zilib ketardi.
    expect(upload).toHaveAttribute('aria-expanded', 'false');
    expect(status).toHaveAttribute('aria-expanded', 'true');
  });
});

describe('Rol ko\'rsatiladi', () => {
  /**
   * «Nega bu bo'lim menda yo'q» degan savolning javobi ko'pincha
   * rolda. Uni ko'rsatish murojaatni qisqartiradi.
   */
  test('xodimning roli va ochiq ishlar soni chiqadi', () => {
    renderAs({ role: 'WORKER', permissions: ['CONTENT_CREATE'] });

    expect(screen.getByText('WORKER')).toBeInTheDocument();
    expect(screen.getByText(/ish ochiq/)).toBeInTheDocument();
  });
});

describe('Hamma uchun ochiq mavzular', () => {
  test('boshqaruv paneli ruxsatsiz ham ko\'rinadi', () => {
    // Ruxsati umuman yo'q xodim ham kirish sahifasini tushunishi kerak.
    renderAs({ role: 'WORKER', permissions: [] });

    expect(screen.getByText(/Boshqaruv paneli/)).toBeInTheDocument();
  });

  test('bo\'sh guruhda tushunarli xabar chiqadi', () => {
    renderAs({ role: 'WORKER', permissions: [] });

    const empty = screen.getAllByText(/ochiq ish yo'q/);
    expect(empty.length).toBeGreaterThan(0);
  });
});
