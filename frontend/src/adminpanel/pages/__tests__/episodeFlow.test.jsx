/**
 * ТЗ §86 — kritik oqim: QISM QO'SHISH.
 *
 * <h2>Nima uchun aynan bu tekshiriladi</h2>
 * Qism formasida ikkita hisoblanadigan qiymat bor va ikkalasi ham
 * jimgina noto'g'ri bo'lishi mumkin:
 *
 * <ul>
 *   <li><b>Qism raqami</b> mavjudlaridan keyingisi bo'lishi kerak.
 *       Doim 1 dan boshlansa, admin har safar uni qo'lda tuzatardi —
 *       unutilsa esa ikkita «1-qism» paydo bo'lardi.</li>
 *   <li><b>Kirish siyosati merosi.</b> Bo'sh tanlov «kontentdan meros»
 *       degani va serverga <code>null</code> ketishi shart. Bo'sh satr
 *       yuborilsa backend uni noto'g'ri enum deb rad etardi, nol yoki
 *       FREE deb talqin qilinsa esa PULLIK qism BEPUL ochilib
 *       ketardi (B24).</li>
 * </ul>
 *
 * ⚠️ Bu test SERVERNI tekshirmaydi — `adminApi` almashtiriladi.
 * Backend tomoni `SeriesStructureAcceptanceTest` da qamralgan.
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EpisodesTab from '../EpisodesTab';
import { PanelI18nProvider } from '../../i18n';

jest.mock('../../api/client', () => ({
  adminApi: {
    seasons: jest.fn(),
    episodes: jest.fn(),
    createEpisode: jest.fn(),
    updateEpisode: jest.fn(),
    deleteEpisode: jest.fn(),
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

/** Faslsiz (EPISODIC) mini-serial: tekis qismlar ro'yxati. */
const episode = (id, number, title, status) => ({
  id,
  episodeNumber: number,
  seasonId: null,
  status,
  // Backend har doim qaytaradi — meros hisoblab berilgan siyosat.
  effectiveAccessPolicy: 'PREMIUM_OR_PURCHASE',
  accessPolicyOverride: null,
  price: null,
  durationSeconds: null,
  translations: { UZ: { title } },
  videos: [],
  version: 1,
});

const EXISTING_EPISODES = [
  episode(1, 1, 'Birinchi', 'PUBLISHED'),
  episode(2, 2, 'Ikkinchi', 'PUBLISHED'),
  episode(3, 3, 'Uchinchi', 'DRAFT'),
];

function setup(accessPolicy = 'PREMIUM_OR_PURCHASE') {
  render(
    <PanelI18nProvider>
      <EpisodesTab contentId={42} structureType="EPISODIC" contentAccessPolicy={accessPolicy} />
    </PanelI18nProvider>
  );
}

/** Ro'yxat yuklangach paydo bo'ladigan tugma — yuklashni kutish nuqtasi. */
const newEpisodeButton = () =>
  screen.findByRole('button', { name: /Yangi qism/i });

beforeEach(() => {
  jest.clearAllMocks();
  adminApi.seasons.mockResolvedValue([]);
  adminApi.episodes.mockResolvedValue(EXISTING_EPISODES);
  adminApi.createEpisode.mockResolvedValue({ id: 4 });
});

describe("Qism qo'shish (§86)", () => {
  test('yangi qism raqami mavjudlaridan KEYINGISI bo\'ladi', async () => {
    const user = userEvent.setup();
    setup();

    // Ro'yxat yuklanguncha kutamiz — usiz «keyingi raqam» 1 bo'lardi.
    //
    // ⚠️ Qism sarlavhasi bo'yicha kutib bo'lmaydi: qator
    // «3. Uchinchi» ko'rinishida bir nechta tugunga bo'linadi.
    // «Yangi qism» tugmasi esa faqat yuklash tugagach chiziladi.
    await user.click(await newEpisodeButton());

    const number = document.getElementById('e-num');
    expect(number).toHaveValue(4);
  });

  test("o'zbekcha sarlavha bo'sh bo'lsa so'rov YUBORILMAYDI", async () => {
    const user = userEvent.setup();
    setup();

    await user.click(await newEpisodeButton());
    await user.click(screen.getByRole('button', { name: /^Saqlash$/ }));

    expect(adminApi.createEpisode).not.toHaveBeenCalled();
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  test('meros holatida `accessPolicyOverride` NULL ketadi (B24)', async () => {
    const user = userEvent.setup();
    setup('PREMIUM_OR_PURCHASE');

    await user.click(await newEpisodeButton());

    // Meros varianti kontent siyosatining NOMI bilan ko'rsatiladi —
    // admin nimani meros qilib olayotganini ko'rishi kerak.
    // ⚠️ «PREMIUM OR PURCHASE» matni ikki joyda: meros variantida va
    // to'g'ridan-to'g'ri tanlash variantida. Bizga MEROS varianti kerak,
    // uni «Kontentdan meros» so'zlari ajratadi.
    //
    // ⚠️ Ro'yxat AVVAL ochiladi. Qidiruvli tanlashda variantlar faqat
    // ochilganda chiziladi — nativ `<select>` da esa ular yopiq holatda
    // ham DOM'da turardi.
    await user.click(document.getElementById('e-ap'));
    expect(screen.getByRole('option', { name: /Kontentdan meros \(PREMIUM OR PURCHASE\)/i }))
      .toBeInTheDocument();
    // Escape TANLOVNI o'zgartirmasdan yopadi: bu test aynan meros
    // holatida qolganini tekshiradi.
    await user.keyboard('{Escape}');

    await user.type(document.getElementById('e-ti'), "To'rtinchi");
    await user.click(screen.getByRole('button', { name: /^Saqlash$/ }));

    await waitFor(() => expect(adminApi.createEpisode).toHaveBeenCalledTimes(1));

    const [contentId, payload] = adminApi.createEpisode.mock.calls[0];
    expect(contentId).toBe(42);
    expect(payload.episodeNumber).toBe(4);
    expect(payload.translations.UZ.title).toBe("To'rtinchi");
    // ⚠️ Bo'sh satr emas, aynan `null`: «kontentdan meros».
    expect(payload.accessPolicyOverride).toBeNull();
    // Faslsiz tuzilishda fasl biriktirilmaydi.
    expect(payload.seasonId).toBeNull();
    // Narx kiritilmagan — `0` emas, `null` (B24).
    expect(payload.price).toBeNull();
  });

  test("bo'sh video segmentlar yuborilmaydi", async () => {
    const user = userEvent.setup();
    setup();

    await user.click(await newEpisodeButton());
    await user.type(document.getElementById('e-ti'), 'Beshinchi');
    await user.click(screen.getByRole('button', { name: /^Saqlash$/ }));

    await waitFor(() => expect(adminApi.createEpisode).toHaveBeenCalledTimes(1));
    // Media tanlanmagan segment serverga tushmasligi kerak — u yerda
    // `mediaId` majburiy va so'rov butunlay rad etilardi.
    expect(adminApi.createEpisode.mock.calls[0][1].videos).toEqual([]);
  });
});
