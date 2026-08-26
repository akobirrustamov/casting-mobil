/**
 * O'YNATIB BO'LMAYDIGAN format haqida ogohlantirish.
 *
 * <h2>Nima uchun bu muhim</h2>
 * `.mkv` va `.avi` omborga ataylab qabul qilinadi — admin manba
 * faylni saqlashi kerak bo'lishi mumkin. Lekin HTML5 pleyer ularni
 * ochmaydi: brauzer ham, mobil ilova ham.
 *
 * Ogohlantirishsiz nosozlik JIMGINA bo'lardi. Yuklash muvaffaqiyatli
 * tugaydi, fayl kutubxonada ko'rinadi, epizodga biriktiriladi, panel
 * hech narsa demaydi — va faqat FOYDALANUVCHI qora ekran ko'rganda,
 * ancha keyin bilinadi.
 */
import { render, screen, waitFor } from '@testing-library/react';
import MediaField from '../MediaField';
import MediaPicker from '../MediaPicker';
import { PanelI18nProvider } from '../../i18n';

jest.mock('../../api/client', () => ({
  adminApi: {
    media: jest.fn(),
    mediaAsset: jest.fn(),
    uploadMedia: jest.fn(),
  },
  mediaUrl: (id) => `/media/${id}`,
}));

const { adminApi } = require('../../api/client');

function wrap(ui) {
  return render(<PanelI18nProvider>{ui}</PanelI18nProvider>);
}

beforeEach(() => {
  adminApi.media.mockReset();
  adminApi.mediaAsset.mockReset();
});

describe('Media maydoni', () => {
  test("`.mkv` biriktirilgan bo'lsa ogohlantirish chiqadi", async () => {
    adminApi.mediaAsset.mockResolvedValue({ id: 5, playable: false, type: 'VIDEO' });

    wrap(<MediaField label="Video" type="VIDEO" value={5} onChange={() => {}} />);

    expect(await screen.findByRole('status')).toHaveTextContent(/mkv/i);
  });

  test('mp4 uchun ogohlantirish YO\'Q', async () => {
    adminApi.mediaAsset.mockResolvedValue({ id: 6, playable: true, type: 'VIDEO' });

    wrap(<MediaField label="Video" type="VIDEO" value={6} onChange={() => {}} />);

    await waitFor(() => expect(adminApi.mediaAsset).toHaveBeenCalledWith(6));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test("rasm maydoni ortiqcha so'rov YUBORMAYDI", async () => {
    // ⚠️ `playable` faqat VIDEO uchun ma'noli. Har bir rasm maydoni
    // uchun ham so'rov ketsa, kontent muharriri ochilganda o'nlab
    // keraksiz so'rov bo'lardi.
    wrap(<MediaField label="Afisha" type="IMAGE" value={7} onChange={() => {}} />);

    await waitFor(() => expect(screen.getByRole('img')).toBeInTheDocument());
    expect(adminApi.mediaAsset).not.toHaveBeenCalled();
  });

  test("so'rov yiqilsa maydon baribir ishlaydi", async () => {
    adminApi.mediaAsset.mockRejectedValue({ status: 500 });

    wrap(<MediaField label="Video" type="VIDEO" value={8} onChange={() => {}} />);

    await waitFor(() => expect(adminApi.mediaAsset).toHaveBeenCalled());
    // Ogohlantirishni chizolmaslik maydonni ishdan chiqarmasin.
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /almashtirish/i })).toBeInTheDocument();
  });
});

describe('Media kutubxonasi', () => {
  test("o'ynatib bo'lmaydigan fayl kartochkasi BELGILANADI", async () => {
    adminApi.media.mockResolvedValue({
      items: [
        { id: 1, type: 'VIDEO', playable: false, originalFilename: 'kino.mkv' },
        { id: 2, type: 'VIDEO', playable: true, originalFilename: 'kino.mp4' },
      ],
      totalPages: 1,
      totalItems: 2,
    });

    wrap(<MediaPicker open type="VIDEO" onClose={() => {}} onSelect={() => {}} />);

    const warnings = await screen.findAllByText(/ochilmaydi/i);
    // Ikkita fayldan FAQAT bittasi belgilanadi.
    expect(warnings).toHaveLength(1);
  });

  test('rasmlar belgilanmaydi — `playable` ular uchun `null`', async () => {
    // ⚠️ Bu `m.playable === false` va `!m.playable` orasidagi farq.
    // Ikkinchisi bo'lsa HAR BIR rasmga ogohlantirish yopishardi.
    adminApi.media.mockResolvedValue({
      items: [{ id: 3, type: 'IMAGE', playable: null, originalFilename: 'afisha.png' }],
      totalPages: 1,
      totalItems: 1,
    });

    wrap(<MediaPicker open type="IMAGE" onClose={() => {}} onSelect={() => {}} />);

    await waitFor(() => expect(adminApi.media).toHaveBeenCalled());
    expect(screen.queryByText(/ochilmaydi/i)).not.toBeInTheDocument();
  });
});
