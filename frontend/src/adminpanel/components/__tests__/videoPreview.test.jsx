/**
 * Panelda videoni ko'rish.
 *
 * <h2>⚠️ Qanday kamchilikni yopadi</h2>
 * Xodim video yuklardi, lekin uni HECH QACHON ko'ra olmasdi — panelda
 * pleyer umuman yo'q edi. Videoning buzuq emasligini tekshirishning
 * yagona yo'li kontentni nashr qilib, ilovadan ochish edi.
 *
 * <h2>⚠️ Nega manzil alohida so'raladi</h2>
 * Brauzerning {@code <video src>} elementi {@code Authorization}
 * sarlavhasini YUBORMAYDI. Shuning uchun server manzilning O'ZIGA
 * imzo qo'yib beradi va u har safar qayta so'raladi — imzo muddati
 * cheklangan.
 */
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import VideoPreview from '../VideoPreview';

/**
 * ⚠️ `hls.js` MOCK qilinadi.
 *
 * jsdom da `MediaSource` yo'q, ya'ni haqiqiy kutubxona ishlay
 * olmaydi. Bizni esa MANTIQ qiziqtiradi: qaysi manba tanlandi va
 * sifat almashuvi ko'rsatiladimi.
 */
const mockHls = {
  on: jest.fn(),
  loadSource: jest.fn(),
  attachMedia: jest.fn(),
  destroy: jest.fn(),
  currentLevel: -1,
  levels: [{ width: 480, height: 852 },
           { width: 720, height: 1280 },
           { width: 1080, height: 1920 }],
};

jest.mock('hls.js', () => {
  const ctor = jest.fn(() => mockHls);
  ctor.isSupported = () => true;
  ctor.Events = { LEVEL_SWITCHED: 'levelSwitched', ERROR: 'error',
                  MANIFEST_PARSED: 'manifestParsed' };
  return { __esModule: true, default: ctor };
});

jest.mock('../../api/client', () => ({
  adminApi: { mediaPreview: jest.fn() },
  mediaUrl: (id) => (id ? `/media/${id}` : null),
  BASE_URL: '',
}));

const { adminApi } = require('../../api/client');

/** Tarjimani soddalashtiramiz — bu test matnni emas, xatti-harakatni sinaydi. */
const t = (key) => key;

/** `MANIFEST_PARSED` hodisasini qo'lda chaqiradi. */
function manifestParsed() {
  const handler = mockHls.on.mock.calls
    .find(([event]) => event === 'manifestParsed')?.[1];
  if (handler) handler();
}

beforeEach(() => {
  jest.clearAllMocks();
  adminApi.mediaPreview.mockReset();

  // ⚠️ Implementatsiya SHU YERDA tiklanadi: CRA jest sozlamasida
  // `resetMocks: true` va u har testdan oldin uni o'chiradi.
  // Fabrikada berilgani faqat BIRINCHI testgacha yetadi.
  Object.values(mockHls).forEach((v) => {
    if (typeof v?.mockReset === 'function') v.mockReset();
  });
  require('hls.js').default.mockImplementation(() => mockHls);
});

describe('Videoni ko\'rish', () => {

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV — pleyer serverdan kelgan MANZILNI
   * ishlatsin.
   *
   * Ilgari bunday manzil umuman yo'q edi va `<video>` ga oddiy
   * `/raw` berilsa 404 qaytarardi: element token yubormaydi.
   */
  it('Serverdan kelgan imzolangan manzil pleyerga beriladi', async () => {
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7,
      url: 'https://s3.example/videos/7.mp4?X-Amz-Signature=abc',
      type: 'VIDEO',
    });

    const { container } = render(
      <VideoPreview open mediaId={7} title="Asosiy video" onClose={jest.fn()} t={t} />
    );

    await waitFor(() => {
      const video = container.querySelector('video');
      expect(video).toBeTruthy();
      expect(video.getAttribute('src'))
        .toBe('https://s3.example/videos/7.mp4?X-Amz-Signature=abc');
    });
  });

  /**
   * ⚠️ YOPIQ modal so'rov YUBORMAYDI.
   *
   * Qism muharririda o'nlab video maydoni bo'lishi mumkin. Har biri
   * sahifa ochilishida manzil so'rasa, imzo bekorga sarflanardi va
   * server ortiqcha yuk olardi.
   */
  it('Modal yopiq bo\'lsa so\'rov YUBORILMAYDI', () => {
    render(<VideoPreview open={false} mediaId={7} onClose={jest.fn()} t={t} />);

    expect(adminApi.mediaPreview).not.toHaveBeenCalled();
  });

  /**
   * ⚠️ Manzil HAR OCHILISHDA qayta so'raladi.
   *
   * U imzolangan va muddati cheklangan. Bir marta olib keshlansa,
   * bir necha soatdan keyin «video ochilmadi» bo'lardi va sababi
   * ko'rinmasdi.
   */
  it('Boshqa video ochilsa manzil QAYTA so\'raladi', async () => {
    adminApi.mediaPreview.mockResolvedValue({ mediaId: 7, url: 'https://s3/a.mp4' });

    const { rerender } = render(
      <VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />
    );
    await waitFor(() => expect(adminApi.mediaPreview).toHaveBeenCalledWith(7));

    adminApi.mediaPreview.mockResolvedValue({ mediaId: 9, url: 'https://s3/b.mp4' });
    rerender(<VideoPreview open mediaId={9} onClose={jest.fn()} t={t} />);

    await waitFor(() => expect(adminApi.mediaPreview).toHaveBeenCalledWith(9));
    expect(adminApi.mediaPreview).toHaveBeenCalledTimes(2);
  });

  /**
   * Manzil kelmasa xato KO'RSATILADI.
   *
   * ⚠️ Busiz modal bo'sh qora to'rtburchak bo'lib qolardi va admin
   * nimaga video ochilmayotganini bilmasdi.
   */
  it('Xato bo\'lsa sabab ko\'rsatiladi', async () => {
    adminApi.mediaPreview.mockRejectedValue({ message: 'Ruxsat yo\'q' });

    render(<VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />);

    await waitFor(() => {
      expect(screen.getByText(/Ruxsat yo'q/)).toBeInTheDocument();
    });
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV — HLS bo'lsa ASL FAYL ISHLATILMAYDI.
   *
   * Asl fayl bitta sifat: 4K manbada u 600 MB va sekin internetda
   * umuman ochilmasdi. HLS esa uch variantni e'lon qiladi va pleyer
   * tezlikka qarab tanlaydi.
   */
  it('HLS bor bo\'lsa — o\'sha ishlatiladi, asl fayl EMAS', async () => {
    const Hls = require('hls.js').default;
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7,
      url: '/api/v1/app/media/7/raw?t=abc',
      hlsUrl: '/api/v1/app/media/7/hls/master.m3u8?t=abc',
    });

    const { container } = render(
      <VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />
    );

    await waitFor(() => expect(Hls).toHaveBeenCalled());

    expect(mockHls.loadSource)
      .toHaveBeenCalledWith('/api/v1/app/media/7/hls/master.m3u8?t=abc');

    // ⚠️ `src` QO'YILMAYDI: qo'yilsa brauzer asl faylni ham
    // parallel tortardi.
    expect(container.querySelector('video').getAttribute('src')).toBeNull();
  });

  /**
   * ⚠️ ENG NOZIK XATO — CHROME «maybe» DEYDI.
   *
   * Chrome `canPlayType('application/vnd.apple.mpegurl')` uchun
   * «maybe» qaytaradi. Bu ROSTGA teng, lekin Chrome HLS'ni aslida
   * O'YNATA OLMAYDI.
   *
   * Ilgari o'sha tekshiruv birinchi turardi va Chrome unga kirardi:
   * `src` m3u8 ga qo'yilar, pleyer abadiy qotardi — `readyState: 0`,
   * spinner aylanadi, XATO YO'Q.
   *
   * ⚠️ Buni jsdom SEZMAGAN: u `canPlayType` uchun bo'sh satr
   * qaytaradi, ya'ni test yashil bo'lib turardi. Xato faqat haqiqiy
   * Chrome'da ko'rindi va shu test o'shandan keyin yozildi.
   */
  it('Chrome «maybe» desa ham hls.js ISHLATILADI', async () => {
    const Hls = require('hls.js').default;

    // Chrome'ning javobini takrorlaymiz.
    const original = window.HTMLMediaElement.prototype.canPlayType;
    window.HTMLMediaElement.prototype.canPlayType = () => 'maybe';

    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7, url: '/raw', hlsUrl: '/hls/master.m3u8',
    });

    const { container } = render(
      <VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />
    );

    await waitFor(() => expect(Hls).toHaveBeenCalled());
    expect(mockHls.loadSource).toHaveBeenCalledWith('/hls/master.m3u8');

    // ⚠️ `src` QO'YILMASLIGI kerak — aynan shu qotib qolishga
    // olib kelardi.
    expect(container.querySelector('video').getAttribute('src')).toBeNull();

    window.HTMLMediaElement.prototype.canPlayType = original;
  });

  /**
   * ⚠️ Boshlang'ich sifat AVTOMATIK tanlanadi.
   *
   * Qat'iy 1080p qo'yilsa sekin internetda birinchi soniyalar
   * uzilib-uzilib ketardi — aynan foydalanuvchi qochmoqchi bo'lgan
   * narsa.
   */
  it('Boshlang\'ich sifat qat\'iy belgilanmaydi', async () => {
    const Hls = require('hls.js').default;
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7, url: '/raw', hlsUrl: '/hls/master.m3u8',
    });

    render(<VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />);
    await waitFor(() => expect(Hls).toHaveBeenCalled());

    expect(Hls.mock.calls[0][0]).toMatchObject({ startLevel: -1 });
  });

  /**
   * ⚠️ TIK VIDEODA SIFAT NOMI — «1920p» EMAS.
   *
   * `level.height` ni olish yotma videoda to'g'ri, tik videoda esa
   * yolg'on: 1080x1920 uchun «1920p» chiqardi va bunday sifat
   * umuman yo'q.
   *
   * Transcoding profillari qisqa tomon bilan nomlangan
   * (1080p / 720p / 480p) — ya'ni bu ko'rinish emas, moslik masalasi.
   *
   * ⚠️ Buni faqat haqiqiy brauzerda ko'rdim: pleyer ishga tushib,
   * yorliqda «1920p» yozilgan edi.
   */
  it('Tik videoda sifat QISQA TOMON bilan nomlanadi', async () => {
    const Hls = require('hls.js').default;
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7, url: '/raw', hlsUrl: '/hls/master.m3u8',
    });

    render(<VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />);
    await waitFor(() => expect(mockHls.on).toHaveBeenCalled());

    // `LEVEL_SWITCHED` ni qo'lda chaqiramiz — 1080x1920 variantga.
    const handler = mockHls.on.mock.calls
      .find(([event]) => event === 'levelSwitched')[1];
    handler(null, { level: 2 });

    await waitFor(() => {
      expect(screen.getByText(/1080p/)).toBeInTheDocument();
    });
    expect(screen.queryByText(/1920p/)).toBeNull();
  });

  /**
   * Manifest o'qilgach mavjud sifatlar ro'yxati chiqadi.
   *
   * ⚠️ Faqat HAQIQATAN mavjudlari. Manba 720p bo'lsa 1080p umuman
   * yasalmaydi va uni ko'rsatish adminni aldardi: bosardi, hech
   * narsa o'zgarmasdi.
   */
  it('Mavjud sifatlar ro\'yxati ko\'rsatiladi', async () => {
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7, url: '/raw', hlsUrl: '/hls/master.m3u8',
    });

    render(<VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />);
    await waitFor(() => expect(mockHls.on).toHaveBeenCalled());

    manifestParsed();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '1080p' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '720p' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '480p' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'media.qualityAuto' })).toBeInTheDocument();
    });
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV — tanlov `hls.js` ga YETIB BORSIN.
   *
   * Tugma chizilib, hech narsa qilmasa — bu eng yomon holat:
   * admin sifatni tanladim deb o'ylaydi, aslida hech nima
   * o'zgarmaydi.
   */
  it('Sifat tanlansa hls.js ga uzatiladi', async () => {
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7, url: '/raw', hlsUrl: '/hls/master.m3u8',
    });

    render(<VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />);
    await waitFor(() => expect(mockHls.on).toHaveBeenCalled());
    manifestParsed();

    const btn = await screen.findByRole('button', { name: '720p' });
    fireEvent.click(btn);

    // 720p — ro'yxatdagi 1-indeks (480, 720, 1080).
    expect(mockHls.currentLevel).toBe(1);
  });

  /**
   * ⚠️ «Avto» ga qaytish `-1` beradi.
   *
   * `hls.js` da `-1` = «o'zing hisobla». Boshqa qiymat qo'yilsa
   * moslashuv butunlay o'chib qolardi va admin buni sezmasdi.
   */
  it('Avto tanlansa -1 uzatiladi', async () => {
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7, url: '/raw', hlsUrl: '/hls/master.m3u8',
    });

    render(<VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />);
    await waitFor(() => expect(mockHls.on).toHaveBeenCalled());
    manifestParsed();

    fireEvent.click(await screen.findByRole('button', { name: '480p' }));
    expect(mockHls.currentLevel).toBe(0);

    fireEvent.click(screen.getByRole('button', { name: 'media.qualityAuto' }));
    expect(mockHls.currentLevel).toBe(-1);
  });

  /**
   * ⚠️ HLS YO'Q bo'lsa sifat tugmalari CHIZILMAYDI.
   *
   * Asl faylda bitta sifat bor — tugmalarni ko'rsatish yolg'on
   * tanlov berardi.
   */
  it('HLS yo\'q bo\'lsa sifat tugmalari yo\'q', async () => {
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7, url: '/raw', hlsUrl: null,
    });

    render(<VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />);

    await waitFor(() => expect(adminApi.mediaPreview).toHaveBeenCalled());
    expect(screen.queryByRole('button', { name: 'media.qualityAuto' })).toBeNull();
  });

  /**
   * ⚠️ HLS YO'Q bo'lsa asl faylga qaytadi.
   *
   * Transcoding tugamagan bo'lishi mumkin. Pleyer umuman
   * ochilmasligi noto'g'ri bo'lardi: admin aynan «nima yuklandi»
   * degan savolga javob izlaydi.
   */
  it('HLS yo\'q bo\'lsa asl fayl ko\'rsatiladi', async () => {
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7, url: '/api/v1/app/media/7/raw?t=abc', hlsUrl: null,
    });

    const { container } = render(
      <VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />
    );

    await waitFor(() => {
      expect(container.querySelector('video').getAttribute('src'))
        .toBe('/api/v1/app/media/7/raw?t=abc');
    });
  });

  /**
   * ⚠️ `preload="metadata"` — butun fayl TORTILMAYDI.
   *
   * 600 MB lik manbada `preload="auto"` modal ochilishi bilan
   * yuzlab megabayt yuklardi.
   */
  it('Butun fayl oldindan yuklanmaydi', async () => {
    adminApi.mediaPreview.mockResolvedValue({ mediaId: 7, url: 'https://s3/a.mp4' });

    const { container } = render(
      <VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />
    );

    await waitFor(() => {
      const video = container.querySelector('video');
      expect(video).toBeTruthy();
      expect(video.getAttribute('preload')).toBe('metadata');
    });
  });
});
