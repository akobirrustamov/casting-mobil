import {
  MAX_RECOMMENDED_HEIGHT,
  needsDownscaleWarning,
  probeVideoSize,
} from '../videoProbe';

/**
 * Yuklashdan oldingi o'lcham tekshiruvi.
 *
 * <h2>⚠️ Nima qo'riqlanadi</h2>
 * Bu ogohlantirish YOLG'ON POZITIV bermasligi kerak. 1080p faylni
 * yuklayotgan admin har safar ortiqcha oyna ko'rsa, u tugmani
 * o'qimasdan bosishni o'rganib qoladi — va 4K fayl kelganda
 * ogohlantirish ishlamay qolgan bo'ladi.
 *
 * Shuning uchun shubhali holatlarning HAMMASI «ogohlantirma» tomonga
 * hal qilinadi.
 */

describe('Ogohlantirish qarori', () => {
  it('1080p dan katta — ogohlantiriladi', () => {
    expect(needsDownscaleWarning({ width: 3840, height: 2160 })).toBe(true);
  });

  it('aynan 1080p — ogohlantirilmaydi', () => {
    expect(needsDownscaleWarning({ width: 1920, height: 1080 })).toBe(false);
  });

  it('kichikroq — ogohlantirilmaydi', () => {
    expect(needsDownscaleWarning({ width: 1280, height: 720 })).toBe(false);
  });

  /**
   * ⚠️ Aniqlanmagan o'lcham ogohlantirmaydi.
   *
   * Brauzer `.mkv` va `.avi` ni odatda ocholmaydi. «Bilmayman»
   * sababli ogohlantirish har bunday faylda chiqardi va tezda
   * ma'nosini yo'qotardi.
   */
  it('aniqlanmagan o\'lcham — ogohlantirilmaydi', () => {
    expect(needsDownscaleWarning(null)).toBe(false);
    expect(needsDownscaleWarning(undefined)).toBe(false);
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV — TIK VIDEO TUZOG'I.
   *
   * 1080×1920 (Reels) — bu «1080p vertikal», «1920p» emas. Backend
   * uni TUSHIRMAYDI: `VideoProfileSelector` sifat darajasini
   * `Math.min(width, height)` bilan oladi.
   *
   * Agar bu yerda balandlik tekshirilsa, HAR BIR oddiy vertikal
   * rolik ogohlantirish oynasini ochardi. Loyihada vertikal kontent
   * birinchi darajali (§19) — bu chekka holat emas, u har kuni
   * takrorlanardi.
   *
   * Va aynan shu ogohlantirishni o'ldirardi: har safar chiqadigan
   * oyna o'qilmasdan yopiladigan bo'lib qoladi, keyin esa haqiqiy 4K
   * fayl kelganda ham ishlamaydi.
   */
  it('tik 1080×1920 rolik — ogohlantirilMAYDI', () => {
    expect(needsDownscaleWarning({ width: 1080, height: 1920 })).toBe(false);
  });

  /** Tik 4K esa haqiqatan qimmat — u ogohlantiriladi. */
  it('tik 2160×3840 (4K) — ogohlantiriladi', () => {
    expect(needsDownscaleWarning({ width: 2160, height: 3840 })).toBe(true);
  });

  it('gorizontal va vertikal 4K BIR XIL baholanadi', () => {
    expect(needsDownscaleWarning({ width: 3840, height: 2160 }))
      .toBe(needsDownscaleWarning({ width: 2160, height: 3840 }));
  });

  it('chegara `VideoProfileSelector` bilan bir xil', () => {
    expect(MAX_RECOMMENDED_HEIGHT).toBe(1080);
  });
});

describe('O\'lchamni o\'qish', () => {
  /** `<video>` ni boshqarish uchun soxta element. */
  function fakeVideo() {
    const video = {
      preload: '', muted: false, src: '',
      videoWidth: 0, videoHeight: 0,
      onloadedmetadata: null, onerror: null,
      removeAttribute: jest.fn(),
    };
    jest.spyOn(document, 'createElement').mockReturnValue(video);
    return video;
  }

  beforeEach(() => {
    global.URL.createObjectURL = jest.fn(() => 'blob:soxta');
    global.URL.revokeObjectURL = jest.fn();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('o\'lchamni qaytaradi', async () => {
    const video = fakeVideo();
    const promise = probeVideoSize(new Blob(['x']));

    video.videoWidth = 3840;
    video.videoHeight = 2160;
    video.onloadedmetadata();

    await expect(promise).resolves.toEqual({ width: 3840, height: 2160 });
  });

  /**
   * ⚠️ Butun fayl yuklanmasligi kerak — u bir necha gigabayt
   * bo'lishi mumkin va uni xotiraga tortish brauzerni qotirardi.
   */
  it('faqat metama\'lumotni so\'raydi', async () => {
    const video = fakeVideo();
    const promise = probeVideoSize(new Blob(['x']));

    expect(video.preload).toBe('metadata');

    video.onerror();
    await promise;
  });

  it('o\'qib bo\'lmasa `null`', async () => {
    const video = fakeVideo();
    const promise = probeVideoSize(new Blob(['x']));

    video.onerror();

    await expect(promise).resolves.toBeNull();
  });

  /**
   * ⚠️ 0×0 — «o'qildi, lekin video yo'q» (masalan faqat ovoz).
   * Uni haqiqiy o'lcham deb qabul qilish «0p video» degan ma'nosiz
   * holatni berardi.
   */
  it('0×0 o\'lcham deb qabul qilinmaydi', async () => {
    const video = fakeVideo();
    const promise = probeVideoSize(new Blob(['x']));

    video.videoWidth = 0;
    video.videoHeight = 0;
    video.onloadedmetadata();

    await expect(promise).resolves.toBeNull();
  });

  /** Xotira oqmasin: `blob:` havolasi bo'shatilishi shart. */
  it('blob havolasi bo\'shatiladi', async () => {
    const video = fakeVideo();
    const promise = probeVideoSize(new Blob(['x']));

    video.onerror();
    await promise;

    expect(global.URL.revokeObjectURL).toHaveBeenCalledWith('blob:soxta');
  });

  /**
   * ⚠️ Ba'zi fayllarda `<video>` na `loadedmetadata`, na `error`
   * beradi — u jimgina osilib qoladi. Chegara bo'lmasa yuklash
   * tugmasi abadiy javobsiz qolardi.
   */
  it('javob kelmasa vaqt bo\'yicha uziladi', async () => {
    jest.useFakeTimers();
    fakeVideo();

    const promise = probeVideoSize(new Blob(['x']));
    jest.advanceTimersByTime(5000);

    await expect(promise).resolves.toBeNull();
    jest.useRealTimers();
  });

  /**
   * ⚠️ Kechikkan javob natijani O'ZGARTIRMASLIGI kerak.
   *
   * Aks holda `resolve` ikki marta chaqirilardi va allaqachon
   * boshlangan yuklash o'rtasida ogohlantirish oynasi ochilardi.
   */
  it('vaqt tugagach kelgan javob e\'tiborga olinmaydi', async () => {
    jest.useFakeTimers();
    const video = fakeVideo();

    const promise = probeVideoSize(new Blob(['x']));
    jest.advanceTimersByTime(5000);
    await expect(promise).resolves.toBeNull();

    video.videoWidth = 3840;
    video.videoHeight = 2160;
    // Kech kelgan javob — hech narsa qilmasligi kerak.
    expect(() => video.onloadedmetadata()).not.toThrow();

    jest.useRealTimers();
  });

  it('fayl berilmasa `null`', async () => {
    await expect(probeVideoSize(null)).resolves.toBeNull();
  });
});
