/**
 * SINGLE kontentga (film) VIDEO biriktirish.
 *
 * <h2>⚠️ Qanday kamchilikni yopadi</h2>
 * Panelda filmga video biriktirishning HECH QANDAY yo'li yo'q edi:
 *
 * <ul>
 *   <li>«Qismlar» bo'limi — videoning yagona joyi — SINGLE tuzilishda
 *       ko'rsatilmasdi (`hasParts = structureType !== 'SINGLE'`);</li>
 *   <li>«Media» bo'limida esa video maydoni umuman yo'q edi, garchi
 *       o'sha faylning izohi «Afisha, muqova, galereya va videolar»
 *       deb yozilgan bo'lsa ham.</li>
 * </ul>
 *
 * Backend buni to'liq qo'llab-quvvatlardi: `AccessService` SINGLE
 * kontentning asosiy videosini `ContentMedia(role=VIDEO)` dan qidiradi.
 * Ya'ni yetishmagani faqat panel tomoni edi.
 *
 * ⚠️ Ikkinchi, jimroq kamchilik: muharrir DTO'dagi xom `media`
 * ro'yxatini umuman O'QIMASDI. Backend saqlashda ro'yxatni butunlay
 * almashtirgani uchun, `VIDEO` bog'lanishi bor kontentda sarlavhadagi
 * bitta harfni tuzatish ham filmning O'ZINI uzardi — hech qanday xato
 * ko'rinmasdan.
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
    mediaAsset: jest.fn(),
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

/** Bitta qismlik film — video kontentning O'ZIGA biriktiriladi. */
const FILM = {
  id: 7,
  version: 2,
  slug: 'behind-the-dreams',
  contentType: 'MOVIE',
  structureType: 'SINGLE',
  orientation: 'LANDSCAPE',
  status: 'PUBLISHED',
  accessPolicy: 'FREE',
  categoryId: 5,
  genreIds: [1],
  credits: [],
  translations: { UZ: { title: 'Orzular ortida' }, RU: {}, EN: {} },
  posterMediaId: 11,
  localePosters: {},
  coverMediaId: 13,
  gallery: [],
  media: [
    { role: 'POSTER', mediaId: 11, sortOrder: 0 },
    { role: 'COVER', mediaId: 13, sortOrder: 0 },
    { role: 'VIDEO', mediaId: 144, sortOrder: 0 },
    { role: 'TRAILER', mediaId: 145, sortOrder: 0 },
    // ⚠️ Panel bu ikkisini KO'RSATMAYDI — saqlashda yo'qolmasligi kerak.
    { role: 'TEASER', mediaId: 146, sortOrder: 0 },
    { role: 'THUMBNAIL', mediaId: 147, sortOrder: 0 },
  ],
};

function setup(contentId) {
  render(
    <PanelI18nProvider>
      <ContentEditor open contentId={contentId} onSaved={jest.fn()} onClose={jest.fn()} />
    </PanelI18nProvider>
  );
}

const saveButton = () => screen.getByRole('button', { name: /^Saqlash$/ });
const openTab = (user, name) =>
  user.click(screen.getByRole('tab', { name: new RegExp(`^${name}`, 'i') }));

beforeEach(() => {
  jest.clearAllMocks();
  adminApi.categories.mockResolvedValue({
    items: [{ id: 5, slug: 'drama', translations: { UZ: { title: 'Drama' } } }] });
  adminApi.genres.mockResolvedValue({
    items: [{ id: 1, slug: 'romantika', translations: { UZ: { title: 'Romantika' } } }] });
  adminApi.creators.mockResolvedValue({ items: [] });
  adminApi.media.mockResolvedValue({ items: [], totalPages: 0, totalItems: 0 });
  adminApi.seasons.mockResolvedValue([]);
  adminApi.episodes.mockResolvedValue([]);
  // ⚠️ `MediaField` VIDEO uchun aktivni so'raydi: `playable === false`
  // bo'lsa «hali tayyor emas» deb ogohlantiradi — transcoding tugamagan
  // videoni biriktirib qo'yishning oldi olinadi.
  //
  // ⚠️ Aynan SHU YERDA berilishi shart: CRA jest sozlamasida
  // `resetMocks: true` va u har testdan oldin implementatsiyani
  // o'chiradi. Factory'da berilgani ikkinchi testgacha yetib bormaydi.
  adminApi.mediaAsset.mockResolvedValue({ id: 144, playable: true });
  adminApi.contentById.mockResolvedValue(FILM);
  adminApi.updateContent.mockResolvedValue({ ...FILM });
});

/** Saqlashga ketgan media ro'yxatini rol bo'yicha qidiradi. */
function sentMedia(role) {
  const [, payload] = adminApi.updateContent.mock.calls[0];
  return payload.media.filter((m) => m.role === role);
}

describe('SINGLE kontent — asosiy video', () => {

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV.
   *
   * Media bo'limida video maydoni KO'RINISHI kerak. Ilgari u yo'q edi
   * va «Qismlar» bo'limi ham yopiq bo'lgani uchun filmga video
   * biriktirib bo'lmasdi.
   */
  it('Media bo\'limida asosiy video maydoni bor', async () => {
    const user = userEvent.setup();
    setup(7);
    await waitFor(() => expect(adminApi.contentById).toHaveBeenCalled());

    await openTab(user, 'Media');

    expect(screen.getByText(/Asosiy video/i)).toBeInTheDocument();
    expect(screen.getByText(/Treyler/i)).toBeInTheDocument();
  });

  /**
   * ⚠️ Ko'p qismli kontentda video QISMGA tegishli. Bu yerda maydon
   * ko'rsatilsa ikkita bir-biriga zid joy paydo bo'lardi va admin
   * qaysi biri ishlashini bilmasdi.
   */
  it('Ko\'p qismli kontentda asosiy video maydoni KO\'RSATILMAYDI', async () => {
    const user = userEvent.setup();
    adminApi.contentById.mockResolvedValue({
      ...FILM, structureType: 'SEASONAL', contentType: 'SERIES' });
    setup(7);
    await waitFor(() => expect(adminApi.contentById).toHaveBeenCalled());

    await openTab(user, 'Media');

    expect(screen.getByText(/Qismlar.*bo'limida biriktiriladi/i)).toBeInTheDocument();
  });

  /**
   * Mavjud video YUKLANADI va saqlashda qaytariladi.
   *
   * ⚠️ Yuklanmasa, formadagi bo'sh qiymat saqlanardi va backend
   * bog'lanishni o'chirardi.
   */
  it('Mavjud video yuklanadi va saqlashda saqlanib qoladi', async () => {
    const user = userEvent.setup();
    setup(7);
    await waitFor(() => expect(adminApi.contentById).toHaveBeenCalled());

    await user.click(saveButton());
    await waitFor(() => expect(adminApi.updateContent).toHaveBeenCalled());

    expect(sentMedia('VIDEO')).toEqual([
      { role: 'VIDEO', mediaId: 144, sortOrder: 0 },
    ]);
    expect(sentMedia('TRAILER')).toEqual([
      { role: 'TRAILER', mediaId: 145, sortOrder: 0 },
    ]);
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV — JIMGINA YO'QOLISH.
   *
   * Panel ko'rsatmaydigan rollar ham saqlanib qolishi kerak. Backend
   * media ro'yxatini butunlay almashtiradi, ya'ni forma yubormagan
   * bog'lanish o'chadi.
   *
   * Bu eng xavfli tur: xato chiqmaydi, ma'lumot esa yo'qoladi.
   */
  it('Panel ko\'rsatmaydigan rollar (TEASER, THUMBNAIL) O\'CHMAYDI', async () => {
    const user = userEvent.setup();
    setup(7);
    await waitFor(() => expect(adminApi.contentById).toHaveBeenCalled());

    await user.click(saveButton());
    await waitFor(() => expect(adminApi.updateContent).toHaveBeenCalled());

    expect(sentMedia('TEASER')).toEqual([
      { role: 'TEASER', mediaId: 146, sortOrder: 0 },
    ]);
    expect(sentMedia('THUMBNAIL')).toEqual([
      { role: 'THUMBNAIL', mediaId: 147, sortOrder: 0 },
    ]);
  });

  /**
   * Xom ro'yxat kelmasa ham muharrir yiqilmasligi kerak — eski
   * javoblarda `media` maydoni bo'lmasligi mumkin.
   */
  it('media ro\'yxati kelmasa ham yiqilmaydi', async () => {
    const user = userEvent.setup();
    const { media, ...withoutRaw } = FILM;
    adminApi.contentById.mockResolvedValue(withoutRaw);
    setup(7);
    await waitFor(() => expect(adminApi.contentById).toHaveBeenCalled());

    await user.click(saveButton());
    await waitFor(() => expect(adminApi.updateContent).toHaveBeenCalled());

    expect(sentMedia('VIDEO')).toEqual([]);
    expect(sentMedia('POSTER')).toHaveLength(1);
  });
});
