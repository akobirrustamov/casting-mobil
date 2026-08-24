/**
 * ТЗ §86 — kritik oqim: REKLAMA YARATISH.
 *
 * <h2>Nima uchun aynan bu tekshiriladi</h2>
 * Reklama va premyera BITTA komponentda (`BannerPage`) — maydonlari
 * deyarli bir xil. Bu ikkita xavf tug'diradi:
 *
 * <ul>
 *   <li>Reklamaga xos maydonlar (auditoriya, mobil rasm) premyera
 *       so'roviga tushib ketishi mumkin va aksincha;</li>
 *   <li>ikkala marshrut bir joyda chizilgani uchun React komponentni
 *       QAYTA ISHLATADI va holat saqlanib qolardi — shuning uchun
 *       `key` majburiy (roadmapda yozilgan «ikkita jimgina buzuvchi
 *       xato»).</li>
 * </ul>
 *
 * Bundan tashqari «Ichki nom» majburiy: usiz ro'yxatda bannerni
 * ajratib bo'lmasdi, chunki sarlavha uch tilli va bo'sh bo'lishi
 * mumkin.
 *
 * ⚠️ Bu test SERVERNI tekshirmaydi — `adminApi` almashtiriladi.
 * Backend tomoni `AdvertisementModuleTest` da qamralgan.
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BannerPage from '../BannerPage';
import { PanelI18nProvider } from '../../i18n';

jest.mock('../../api/client', () => ({
  adminApi: {
    advertisements: jest.fn(),
    premieres: jest.fn(),
    createAd: jest.fn(),
    updateAd: jest.fn(),
    deleteAd: jest.fn(),
    createPremiere: jest.fn(),
    updatePremiere: jest.fn(),
    deletePremiere: jest.fn(),
    content: jest.fn(),
    media: jest.fn().mockResolvedValue({ items: [], totalPages: 0, totalItems: 0 }),
    uploadMedia: jest.fn(),
  },
  mediaUrl: (id) => (id ? `/media/${id}` : null),
}));

jest.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    user: { role: 'ADMIN' },
    can: () => true,
    atLeast: () => true,
    isAuthed: true,
    restoring: false,
  }),
}));

const { adminApi } = require('../../api/client');

const setup = (kind = 'ad') => render(
  <PanelI18nProvider>
    <BannerPage kind={kind} />
  </PanelI18nProvider>
);

/**
 * «+ Yangi ...» tugmasi. Sarlavha turga qarab boshqacha
 * («Yangi banner» / «Yangi premyera»), shuning uchun umumiy qism
 * bo'yicha qidiriladi. Tugma ro'yxat yuklangach chiziladi.
 */
const newButton = () => screen.findByRole('button', { name: /^\+ Yangi/ });
const saveButton = () => screen.getByRole('button', { name: /^Saqlash$/ });

beforeEach(() => {
  jest.clearAllMocks();
  adminApi.advertisements.mockResolvedValue([]);
  adminApi.premieres.mockResolvedValue([]);
  adminApi.content.mockResolvedValue({ items: [] });
  adminApi.createAd.mockResolvedValue({ id: 1 });
  adminApi.createPremiere.mockResolvedValue({ id: 1 });
});

describe('Reklama yaratish (§86)', () => {
  test("ichki nom bo'sh bo'lsa so'rov YUBORILMAYDI", async () => {
    const user = userEvent.setup();
    setup('ad');

    await user.click(await newButton());
    await user.click(saveButton());

    expect(adminApi.createAd).not.toHaveBeenCalled();
    // Sabab ko'rinishi shart — jim qolish «saqlandi» degan taassurot berardi.
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  test("nom kiritilgach reklama YARATILADI va premyera endpointi tegilmaydi", async () => {
    const user = userEvent.setup();
    setup('ad');

    await user.click(await newButton());
    await user.type(document.getElementById('b-name'), 'Bahorgi aksiya');
    await user.click(saveButton());

    await waitFor(() => expect(adminApi.createAd).toHaveBeenCalledTimes(1));
    // ⚠️ Ikkala tur bitta komponentda — noto'g'ri endpoint chaqirilsa
    // reklama premyera bo'lib qolardi.
    expect(adminApi.createPremiere).not.toHaveBeenCalled();

    const payload = adminApi.createAd.mock.calls[0][0];
    expect(payload.name).toBe('Bahorgi aksiya');
    // Reklamaga XOS maydonlar bor.
    expect(payload).toHaveProperty('audience');
    expect(payload).toHaveProperty('mobileImageMediaId');
    // Premyeraga xos maydonlar YO'Q.
    expect(payload).not.toHaveProperty('videoMediaId');
    expect(payload).not.toHaveProperty('contentId');
    // Vaqt oynasi kiritilmagan — bo'sh satr emas, `null`.
    expect(payload.startAt).toBeNull();
    expect(payload.endAt).toBeNull();
  });

  test("uchala tildagi sarlavha bitta so'rovda ketadi", async () => {
    const user = userEvent.setup();
    setup('ad');

    await user.click(await newButton());
    await user.type(document.getElementById('b-name'), 'Banner');
    await user.type(document.getElementById('b-ti'), 'Chegirma');

    await user.click(screen.getByRole('tab', { name: /^RU/ }));
    await user.type(document.getElementById('b-ti'), 'Скидка');

    await user.click(saveButton());

    await waitFor(() => expect(adminApi.createAd).toHaveBeenCalledTimes(1));
    const { translations } = adminApi.createAd.mock.calls[0][0];
    expect(translations.UZ.title).toBe('Chegirma');
    expect(translations.RU.title).toBe('Скидка');
  });

  test("premyerada o'zbekcha sarlavha ham MAJBURIY", async () => {
    const user = userEvent.setup();
    setup('premiere');

    await user.click(await newButton());
    await user.type(document.getElementById('b-name'), 'Premyera');
    await user.click(saveButton());

    // Reklamada nom yetarli, premyerada esa sarlavha ham kerak —
    // u mobil ilovada kartochka matni sifatida chiqadi.
    expect(adminApi.createPremiere).not.toHaveBeenCalled();
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });
});
