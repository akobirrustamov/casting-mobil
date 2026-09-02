/**
 * «Ko'rish» tugmasi HAQIQATAN pleyerni ochadi.
 *
 * <h2>⚠️ Haqiqiy nosozlik</h2>
 * `MediaField` da tugma ham, `previewOpen` holati ham bor edi, lekin
 * `<VideoPreview>` ning O'ZI chizilmasdi — u `git pull` paytida
 * yo'qolgan.
 *
 * Natijada tugma bosilardi, holat o'zgarardi va HECH NARSA
 * OCHILMASDI. Xato yo'q, konsolda ham jim.
 *
 * Buni `no-unused-vars` ogohlantirishi ochdi: `previewOpen`
 * «ishlatilmagan» deb belgilandi — ya'ni uni o'qiydigan komponent
 * yo'qolgan degani.
 *
 * ⚠️ Bu test AYNAN o'sha bog'lanishni tekshiradi: tugma → modal.
 */
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import MediaField from '../MediaField';
import { PanelI18nProvider } from '../../i18n';

jest.mock('../../api/client', () => ({
  adminApi: {
    mediaAsset: jest.fn(),
    mediaPreview: jest.fn(),
    media: jest.fn(),
  },
  mediaUrl: (id) => (id ? `/media/${id}` : null),
  BASE_URL: '',
}));

const mockHls = {
  on: jest.fn(), loadSource: jest.fn(), attachMedia: jest.fn(),
  destroy: jest.fn(), levels: [], currentLevel: -1,
};

jest.mock('hls.js', () => {
  const ctor = jest.fn(() => mockHls);
  ctor.isSupported = () => true;
  ctor.Events = { LEVEL_SWITCHED: 'levelSwitched', ERROR: 'error',
                  MANIFEST_PARSED: 'manifestParsed' };
  return { __esModule: true, default: ctor };
});

const { adminApi } = require('../../api/client');

beforeEach(() => {
  jest.clearAllMocks();

  // ⚠️ CRA jest sozlamasida `resetMocks: true` — implementatsiya
  // har testdan oldin o'chadi va uni SHU YERDA tiklash kerak.
  require('hls.js').default.mockImplementation(() => mockHls);
  adminApi.mediaAsset.mockResolvedValue({ id: 7, playable: true, transcoding: null });
  adminApi.mediaPreview.mockResolvedValue({
    mediaId: 7, url: '/raw', hlsUrl: '/hls/master.m3u8',
  });
  adminApi.media.mockResolvedValue({ items: [], totalPages: 0, totalItems: 0 });
});

describe('MediaField — videoni ko\'rish', () => {

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV.
   *
   * Tugma bosilganda pleyer manzil SO'RASHI kerak. So'ramasa —
   * demak `<VideoPreview>` chizilmayapti va tugma bo'sh ishlaydi.
   */
  it('«Ko\'rish» bosilganda pleyer ochiladi', async () => {
    render(
      <PanelI18nProvider>
        <MediaField label="Asosiy video" type="VIDEO" value={7} onChange={jest.fn()} />
      </PanelI18nProvider>
    );

    const btn = await screen.findByRole('button', { name: /Ko'rish/i });
    fireEvent.click(btn);

    await waitFor(() => expect(adminApi.mediaPreview).toHaveBeenCalledWith(7));
  });

  /**
   * Rasm uchun tugma UMUMAN chizilmaydi — eskiz allaqachon ko'rinadi.
   */
  it('Rasm uchun «Ko\'rish» tugmasi yo\'q', async () => {
    render(
      <PanelI18nProvider>
        <MediaField label="Afisha" type="IMAGE" value={7} onChange={jest.fn()} />
      </PanelI18nProvider>
    );

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /Ko'rish/i })).toBeNull();
    });
  });
});
