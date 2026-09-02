/**
 * Tomoshabin pleyeri.
 *
 * <h2>⚠️ Bu yerda nima jimgina buziladi</h2>
 * Uchta narsa — va uchalasi ham ekranda «ishlayotgandek» ko'rinadi:
 * sifat almashganda bufer tozalanishi, saqlangan pozitsiyaga
 * qaytmaslik va sahifa yopilganda pozitsiyani yo'qotish.
 */
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import ViewerPlayer, { formatTime, qualityLabel, resumePosition } from '../ViewerPlayer';

/**
 * ⚠️ `hls.js` MOCK qilinadi: jsdom da `MediaSource` yo'q va haqiqiy
 * kutubxona ishga tusha olmaydi. Bizni MANTIQ qiziqtiradi.
 */
const mockHls = {
  on: jest.fn(),
  loadSource: jest.fn(),
  attachMedia: jest.fn(),
  destroy: jest.fn(),
  currentLevel: -1,
  nextLevel: -1,
  levels: [
    { width: 854, height: 480 },
    { width: 1280, height: 720 },
    { width: 1920, height: 1080 },
  ],
};

jest.mock('hls.js', () => {
  const ctor = jest.fn(() => mockHls);
  ctor.isSupported = () => true;
  ctor.Events = {
    LEVEL_SWITCHED: 'levelSwitched',
    ERROR: 'error',
    MANIFEST_PARSED: 'manifestParsed',
  };
  return { __esModule: true, default: ctor };
});

jest.mock('../../api/client', () => ({
  BASE_URL: '',
  fetchProgress: jest.fn(),
  saveProgress: jest.fn(),
}));

/**
 * ⚠️ Mok `t` o'zgaruvchilarni javobga QO'SHIB qaytaradi.
 *
 * Haqiqiy `t` ni taqlid qilib, `{time}` ni kalit ichida almashtirish
 * ishlamaydi: mok lug'atga qaramaydi va kalitning O'ZINI qaytaradi,
 * kalitda esa `{time}` yo'q. Natijada vaqt javobga umuman tushmasdi
 * va «qayerdan davom etyapti» degan tekshiruv imkonsiz bo'lardi.
 */
jest.mock('../../i18n', () => ({
  useViewerI18n: () => ({
    t: (key, vars) => (vars ? `${key} ${Object.values(vars).join(' ')}` : key),
    locale: 'uz',
    setLocale: () => {},
  }),
}));

const { fetchProgress, saveProgress } = require('../../api/client');

const source = { mediaId: 7, url: '/raw', hlsUrl: '/hls/master.m3u8' };

/** `MANIFEST_PARSED` ni qo'lda chaqiradi. */
function manifestParsed() {
  mockHls.on.mock.calls
    .filter(([event]) => event === 'manifestParsed')
    .forEach(([, handler]) => handler());
}

beforeEach(() => {
  jest.clearAllMocks();

  // ⚠️ Implementatsiya SHU YERDA tiklanadi: CRA jest sozlamasida
  // `resetMocks: true` va u har testdan oldin uni o'chiradi.
  Object.values(mockHls).forEach((v) => {
    if (typeof v?.mockReset === 'function') v.mockReset();
  });
  mockHls.currentLevel = -1;
  mockHls.nextLevel = -1;

  // ⚠️ Konstruktor implementatsiyasi ham TIKLANADI. `resetMocks`
  // fabrikada berilganini ham o'chiradi, va usiz `new Hls()` bo'sh
  // obyekt qaytarardi — «hls.on is not a function».
  require('hls.js').default.mockImplementation(() => mockHls);

  fetchProgress.mockResolvedValue(null);
  saveProgress.mockResolvedValue(null);
});

// -------------------------------------------------------------- toza funksiyalar

describe('resumePosition', () => {
  it('Saqlangan soniyani qaytaradi', () => {
    expect(resumePosition({ position: 5565, duration: 7200 })).toBe(5565);
  });

  /**
   * ⚠️ `null`, nol EMAS: nol ham pozitsiya, va chaqiruvchi «boshidan»
   * ni «tegmaslik» dan ajrata olmasdi.
   */
  it('Yozuv yo‘q — null', () => {
    expect(resumePosition(null)).toBeNull();
  });

  /** Odam ochib darhol yopgan — beshinchi soniyaga sakrash sbo'y kabi. */
  it('Boshidagi soniyalarda qaytarilmaydi', () => {
    expect(resumePosition({ position: 5, duration: 7200 })).toBeNull();
  });

  /** ⚠️ Aks holda tugatgan odam har safar titrlarga tushardi. */
  it('Oxirigacha ko‘rilgani qaytarilmaydi', () => {
    expect(resumePosition({ position: 7100, duration: 7200 })).toBeNull();
    expect(resumePosition({ position: 100, duration: 7200, completed: true })).toBeNull();
  });

  it('Davomiylik noma‘lum bo‘lsa ham ishlaydi', () => {
    expect(resumePosition({ position: 4000, duration: null })).toBe(4000);
  });
});

describe('qualityLabel', () => {
  it('Yotma videoda balandlik bo‘yicha', () => {
    expect(qualityLabel({ width: 1920, height: 1080 })).toBe('1080p');
  });

  /**
   * ⚠️ TIK videoda balandlik yolg'on beradi: 1080×1920 uchun «1920p»
   * chiqardi — bunday sifat umuman yo'q. Transkodlash profillari ham
   * qisqa tomon bilan nomlangan.
   */
  it('Tik videoda QISQA tomon bo‘yicha', () => {
    expect(qualityLabel({ width: 1080, height: 1920 })).toBe('1080p');
  });
});

describe('formatTime', () => {
  it('Soatli va soatsiz', () => {
    expect(formatTime(5565)).toBe('1:32:45');
    expect(formatTime(125)).toBe('2:05');
  });
});

// ------------------------------------------------------------------- komponent

describe('Pleyer', () => {
  it('HLS manbasi yuklanadi', async () => {
    render(<ViewerPlayer type="content" targetId="6" source={source} />);
    await waitFor(() => expect(mockHls.loadSource).toHaveBeenCalledWith('/hls/master.m3u8'));
  });

  it('Sifat tugmalari faqat haqiqiy variantlar uchun', async () => {
    render(<ViewerPlayer type="content" targetId="6" source={source} />);
    await waitFor(() => expect(mockHls.on).toHaveBeenCalled());
    manifestParsed();

    expect(await screen.findByRole('button', { name: '1080p' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '720p' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '480p' })).toBeInTheDocument();
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV — bufer tozalanmasin.
   *
   * `currentLevel` ham, `nextLevel` ham `currentTime` ni saqlaydi,
   * ya'ni video boshidan boshlanmaydi. Farq sezilishida:
   * `currentLevel` buferni tozalab, 1-3 soniya to'xtash beradi.
   * Aynan shu «video qaytadan yuklanyapti» shikoyatining sababi edi.
   */
  it('Sifat almashganda currentLevel ga TEGILMAYDI', async () => {
    render(<ViewerPlayer type="content" targetId="6" source={source} />);
    await waitFor(() => expect(mockHls.on).toHaveBeenCalled());
    manifestParsed();

    fireEvent.click(await screen.findByRole('button', { name: '720p' }));

    expect(mockHls.nextLevel).toBe(1);
    expect(mockHls.currentLevel).toBe(-1);
  });

  it('Avto tanlansa -1 uzatiladi', async () => {
    render(<ViewerPlayer type="content" targetId="6" source={source} />);
    await waitFor(() => expect(mockHls.on).toHaveBeenCalled());
    manifestParsed();

    fireEvent.click(await screen.findByRole('button', { name: '480p' }));
    expect(mockHls.nextLevel).toBe(0);

    fireEvent.click(screen.getByRole('button', { name: 'watch.qualityAuto' }));
    expect(mockHls.nextLevel).toBe(-1);
  });

  /**
   * ⚠️ HLS yo'q bo'lsa tugmalar CHIZILMAYDI: asl faylda sifat bitta
   * va tanlov ko'rsatish yolg'on bo'lardi.
   */
  it('HLS yo‘q bo‘lsa sifat tanlovi yo‘q', async () => {
    render(
      <ViewerPlayer type="content" targetId="6" source={{ mediaId: 7, url: '/raw', hlsUrl: null }} />
    );

    await waitFor(() => expect(fetchProgress).toHaveBeenCalled());
    expect(screen.queryByRole('button', { name: 'watch.qualityAuto' })).toBeNull();
  });

  /** Saqlangan pozitsiyaga qaytadi va buni odamga aytadi. */
  it('Saqlangan soniyadan davom etadi', async () => {
    fetchProgress.mockResolvedValue({ position: 5565, duration: 7200, completed: false });

    render(<ViewerPlayer type="content" targetId="6" source={source} />);

    expect(await screen.findByText(/1:32:45/)).toBeInTheDocument();
  });

  /**
   * ⚠️ ORALIQ XATO — eng qimmatlisi shu yerda.
   *
   * Pozitsiya so'rovi va HLS manifesti bir vaqtda ketadi. So'rov
   * birinchi qaytsa, `readyState` hali 0 bo'ladi va o'sha lahzada
   * qo'yilgan `currentTime` JIMGINA yo'qoladi — video boshidan
   * boshlanadi.
   *
   * Xatolik oraliq: sekin tarmoqda manifest oldin keladi va davom
   * ettirish ishlaydi, tez tarmoqda esa yo'q. Brauzerda u aynan
   * shunday chiqdi — bir marta 95-soniyadan davom etdi, keyingi
   * safar noldan boshladi.
   */
  it('Metama\'lumot kelmagan bo‘lsa surish KUTADI', async () => {
    fetchProgress.mockResolvedValue({ position: 95, duration: 165, completed: false });

    const { container } = render(
      <ViewerPlayer type="content" targetId="13" source={source} />
    );

    const video = container.querySelector('video');
    // jsdom da `readyState` 0 — ya'ni hali surib bo'lmaydi.
    Object.defineProperty(video, 'readyState', { value: 0, configurable: true });

    await waitFor(() => expect(fetchProgress).toHaveBeenCalled());

    // ⚠️ Hali surilmagan bo'lishi KERAK.
    expect(video.currentTime).toBe(0);

    // Metama'lumot keldi — endi suriladi.
    Object.defineProperty(video, 'readyState', { value: 1, configurable: true });
    fireEvent(video, new Event('loadedmetadata'));

    await waitFor(() => expect(video.currentTime).toBe(95));
  });

  /** Metama'lumot allaqachon bor — kutmasdan darhol suriladi. */
  it('Metama\'lumot tayyor bo‘lsa darhol suriladi', async () => {
    fetchProgress.mockResolvedValue({ position: 95, duration: 165, completed: false });

    const { container } = render(
      <ViewerPlayer type="content" targetId="13" source={source} />
    );
    const video = container.querySelector('video');
    Object.defineProperty(video, 'readyState', { value: 4, configurable: true });

    await waitFor(() => expect(video.currentTime).toBe(95));
  });

  /**
   * ⚠️ Pozitsiyasiz sahifada hech narsa aytilmaydi — «0:00 dan davom
   * etmoqda» degan yozuv odamni chalg'itardi.
   */
  it('Yozuv yo‘q bo‘lsa xabar ham yo‘q', async () => {
    render(<ViewerPlayer type="content" targetId="6" source={source} />);

    await waitFor(() => expect(fetchProgress).toHaveBeenCalled());
    expect(screen.queryByText(/watch.resumed/)).toBeNull();
  });

  /**
   * ⚠️ Mehmonda yoki tarmoq yo'qligida saqlash XATOSI tomoshani
   * to'xtatmasligi kerak: pleyer baribir ochilishi shart.
   */
  it('Pozitsiya so‘rovi yiqilsa ham pleyer ishlaydi', async () => {
    fetchProgress.mockRejectedValue(new Error('401'));

    render(<ViewerPlayer type="content" targetId="6" source={source} />);

    await waitFor(() => expect(mockHls.loadSource).toHaveBeenCalled());
  });

  /**
   * ⚠️ TIK kadr alohida shaklga muhtoj.
   *
   * Keng ramkada tik video yon tomonlarida katta qora maydonli tor
   * tasmaga aylanadi — kadrning o'zi kichkina qoladi. Bu platformada
   * Reels formatidagi kontent bor, ya'ni holat kamdan-kam emas.
   */
  it('Tik videoga alohida sinf beriladi', async () => {
    const { container } = render(
      <ViewerPlayer type="content" targetId="6" source={source} orientation="VERTICAL" />
    );

    await waitFor(() => expect(mockHls.loadSource).toHaveBeenCalled());
    expect(container.querySelector('video').className).toContain('uz-viewer-video-vertical');
  });

  /**
   * ⚠️ Yotma videoda u sinf BO'LMASLIGI kerak: balandligi qat'iy
   * bo'lib qolsa, keng kadr ekranga sig'may ketardi.
   */
  it('Yotma videoda tik sinf yo‘q', async () => {
    const { container } = render(
      <ViewerPlayer type="content" targetId="6" source={source} orientation="LANDSCAPE" />
    );

    await waitFor(() => expect(mockHls.loadSource).toHaveBeenCalled());
    expect(container.querySelector('video').className).not.toContain('vertical');
  });

  /** Shakl noma'lum — yotma deb hisoblanadi, bu keng tarqalgan holat. */
  it('Shakl berilmasa yotma', async () => {
    const { container } = render(
      <ViewerPlayer type="content" targetId="6" source={source} />
    );

    await waitFor(() => expect(mockHls.loadSource).toHaveBeenCalled());
    expect(container.querySelector('video').className).not.toContain('vertical');
  });

  /** Saqlash manzili yo'q — so'rov ham yubormaydi. */
  it('targetId bo‘lmasa pozitsiya so‘ralmaydi', async () => {
    render(<ViewerPlayer type="content" targetId={null} source={source} />);

    await waitFor(() => expect(mockHls.loadSource).toHaveBeenCalled());
    expect(fetchProgress).not.toHaveBeenCalled();
  });
});
