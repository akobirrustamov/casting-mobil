/**
 * ТЗ §86 — kritik oqimlar: KONTENT YARATISH va TAHRIRLASH.
 *
 * <h2>Nima uchun aynan bu ikkisi</h2>
 * Kontent muharriri paneldagi eng katta forma: olti bo'lim, o'nlab
 * maydon, uch til. Uning tarixi ham eng og'ir — roadmapda qayd
 * etilgan uchta jimgina buzuvchi xato aynan shu yerda bo'lgan:
 *
 * <ul>
 *   <li><b>B13</b> — tahrirlashda slug JIM o'zgarardi va havolalar sinardi;</li>
 *   <li><b>B17</b> — janr va ijodkorlar yuklanmasa, saqlashda ular
 *       O'CHIB KETARDI: backend ro'yxatlarni shartsiz almashtiradi,
 *       ya'ni sarlavhadagi bitta harfni tuzatgan admin barcha
 *       janrlarni yo'qotardi;</li>
 *   <li><b>§60</b> — `version` yuborilmasa optimistik qulf ishlamasdi va
 *       ikki admin bir-birining ishini indamay bosib ketardi.</li>
 * </ul>
 *
 * Uchalasi ham «xato chiqmaydi, shunchaki ma'lumot yo'qoladi» turidagi
 * xatolar — aynan shuning uchun test kerak.
 *
 * ⚠️ Bu test SERVERNI tekshirmaydi — `adminApi` almashtiriladi.
 * Backend tomoni `ContentAcceptanceTest` da qamralgan.
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ContentEditor from '../ContentEditor';
import { PanelI18nProvider } from '../../i18n';

jest.mock('../../api/client', () => ({
  adminApi: {
    categories: jest.fn(),
    genres: jest.fn(),
    creators: jest.fn(),
    contentById: jest.fn(),
    createContent: jest.fn(),
    updateContent: jest.fn(),
    media: jest.fn().mockResolvedValue({ items: [], totalPages: 0, totalItems: 0 }),
    uploadMedia: jest.fn(),
    seasons: jest.fn().mockResolvedValue([]),
    episodes: jest.fn().mockResolvedValue([]),
  },
  mediaUrl: (id) => (id ? `/media/${id}` : null),
}));

// Muharrir `can()` orqali nashr ruxsatini tekshiradi — to'liq huquqli
// admin sifatida kiramiz, aks holda PUBLISHED tanlanmasdi.
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

const EXISTING = {
  id: 42,
  version: 3,
  slug: 'mening-yuragim-egasi',
  contentType: 'SERIES',
  structureType: 'SEASONAL',
  orientation: 'LANDSCAPE',
  status: 'PUBLISHED',
  accessPolicy: 'PREMIUM_ONLY',
  premierePrice: null,
  categoryId: 5,
  genreIds: [1, 2, 3],
  credits: [{ creatorId: 9, role: 'ACTOR', characterName: 'Aziz', sortOrder: 0 }],
  ageRating: '16+',
  featured: true,
  popular: false,
  translations: {
    UZ: { title: 'Yurak egasi', description: 'Tavsif' },
    RU: { title: 'Хозяин моего сердца', description: 'Описание' },
    EN: { title: 'Owner of my heart' },
  },
  posterMediaId: 11,
  localePosters: { RU: 12 },
  coverMediaId: 13,
  gallery: [14, 15],
};

function setup(contentId = null) {
  const onSaved = jest.fn();
  const onClose = jest.fn();
  render(
    <PanelI18nProvider>
      <ContentEditor open contentId={contentId} onSaved={onSaved} onClose={onClose} />
    </PanelI18nProvider>
  );
  return { onSaved, onClose };
}

const saveButton = () => screen.getByRole('button', { name: /^Saqlash$/ });

/** Muharrir bo'limlari `role="tab"` — `button` emas (ContentEditor). */
const openTab = (user, name) =>
  user.click(screen.getByRole('tab', { name: new RegExp(`^${name}`, 'i') }));

/** Matnlar bo'limidagi sarlavha maydoni (`TextTab` da `id="ti"`). */
const titleInput = () => document.getElementById('ti');

beforeEach(() => {
  jest.clearAllMocks();
  adminApi.categories.mockResolvedValue({ items: [{ id: 5, slug: 'drama', translations: { UZ: { title: 'Drama' } } }] });
  adminApi.genres.mockResolvedValue({ items: [{ id: 1, slug: 'romantika', translations: { UZ: { title: 'Romantika' } } }] });
  adminApi.creators.mockResolvedValue({ items: [] });
  adminApi.createContent.mockResolvedValue({ id: 99 });
  adminApi.updateContent.mockResolvedValue({ id: 42 });
  adminApi.contentById.mockResolvedValue(EXISTING);
});

describe('Kontent yaratish (§86)', () => {
  test("o'zbekcha sarlavha bo'sh bo'lsa so'rov YUBORILMAYDI", async () => {
    const user = userEvent.setup();
    setup();

    await user.click(saveButton());

    expect(adminApi.createContent).not.toHaveBeenCalled();
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  test('sarlavha kiritilgach yaratish so\'rovi ketadi', async () => {
    const user = userEvent.setup();
    const { onSaved, onClose } = setup();

    await openTab(user, 'Matnlar');
    await user.type(titleInput(), 'Yangi film');
    await user.click(saveButton());

    await waitFor(() => expect(adminApi.createContent).toHaveBeenCalledTimes(1));

    const payload = adminApi.createContent.mock.calls[0][0];
    expect(payload.translations.UZ.title).toBe('Yangi film');
    // ⚠️ Yaratishda slug YUBORILMAYDI — uni backend sarlavhadan yasaydi.
    // Bo'sh satr yuborilsa slug bo'sh bo'lib qolardi.
    expect(payload.slug).toBeUndefined();
    expect(onSaved).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
  });
});

describe('Kontent tahrirlash (§86)', () => {
  /** Muharrir mavjud kontentni yuklab bo'lguncha kutamiz. */
  const waitLoaded = async () =>
    waitFor(() => expect(adminApi.contentById).toHaveBeenCalled());

  test('boshqa tillar va slug O\'ZGARMAYDI (B12, B13)', async () => {
    const user = userEvent.setup();
    setup(42);
    await waitLoaded();

    await openTab(user, 'Matnlar');
    const title = await screen.findByDisplayValue('Yurak egasi');
    await user.clear(title);
    await user.type(title, 'Yurak egasi 2');

    await user.click(saveButton());
    await waitFor(() => expect(adminApi.updateContent).toHaveBeenCalledTimes(1));

    const [id, payload] = adminApi.updateContent.mock.calls[0];
    expect(id).toBe(42);
    expect(payload.translations.UZ.title).toBe('Yurak egasi 2');
    // Tegilmagan tillar joyida qoladi.
    expect(payload.translations.RU.title).toBe('Хозяин моего сердца');
    expect(payload.translations.EN.title).toBe('Owner of my heart');
    // ⚠️ B13: slug jim o'zgarsa mavjud havolalar sinardi.
    expect(payload.slug).toBe('mening-yuragim-egasi');
  });

  test('janr, ijodkor va kategoriya SAQLANIB qoladi (B17)', async () => {
    const user = userEvent.setup();
    setup(42);
    await waitLoaded();

    await openTab(user, 'Matnlar');
    const title = await screen.findByDisplayValue('Yurak egasi');
    await user.type(title, '!');
    await user.click(saveButton());

    await waitFor(() => expect(adminApi.updateContent).toHaveBeenCalledTimes(1));
    const payload = adminApi.updateContent.mock.calls[0][1];

    // Backend bu ro'yxatlarni SHARTSIZ almashtiradi. Ular yuborilmasa
    // yoki bo'sh ketsa — sarlavhadagi bitta belgini tuzatgan admin
    // barcha janrlarni va biriktirilgan ijodkorlarni yo'qotardi.
    expect(payload.genreIds).toEqual([1, 2, 3]);
    expect(payload.credits).toHaveLength(1);
    expect(payload.credits[0]).toMatchObject({ creatorId: 9, role: 'ACTOR', sortOrder: 0 });
    expect(payload.categoryId).toBe(5);
  });

  test('media ro\'yxati afisha, tilga xos afisha, muqova va galereyadan yig\'iladi', async () => {
    const user = userEvent.setup();
    setup(42);
    await waitLoaded();

    await openTab(user, 'Matnlar');
    const title = await screen.findByDisplayValue('Yurak egasi');
    await user.type(title, '!');
    await user.click(saveButton());

    await waitFor(() => expect(adminApi.updateContent).toHaveBeenCalledTimes(1));
    const { media } = adminApi.updateContent.mock.calls[0][1];

    expect(media).toEqual(expect.arrayContaining([
      { role: 'POSTER', mediaId: 11, sortOrder: 0 },
      // Tilga xos afisha `locale` bilan ajratiladi — umumiysini bosmaydi.
      { role: 'POSTER', locale: 'RU', mediaId: 12, sortOrder: 0 },
      { role: 'COVER', mediaId: 13, sortOrder: 0 },
      { role: 'GALLERY', mediaId: 14, sortOrder: 0 },
      { role: 'GALLERY', mediaId: 15, sortOrder: 1 },
    ]));
  });

  test('optimistik qulf uchun `version` yuboriladi (§60)', async () => {
    const user = userEvent.setup();
    setup(42);
    await waitLoaded();

    await openTab(user, 'Matnlar');
    const title = await screen.findByDisplayValue('Yurak egasi');
    await user.type(title, '!');
    await user.click(saveButton());

    await waitFor(() => expect(adminApi.updateContent).toHaveBeenCalledTimes(1));
    // ⚠️ `null` bo'lsa backend tekshiruvi o'tkazib yuborilardi va ikki
    // admin bir-birining ishini indamay bosib ketardi.
    expect(adminApi.updateContent.mock.calls[0][1].version).toBe(3);
  });
});
