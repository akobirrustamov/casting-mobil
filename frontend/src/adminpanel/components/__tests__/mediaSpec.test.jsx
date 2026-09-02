/**
 * Maydon yonidagi O'LCHAM talabi.
 *
 * <h2>Nima uchun bu tekshiriladi</h2>
 * Server rasmni qayta o'lchamaydi, ilova esa ortiqchasini qirqadi —
 * ya'ni noto'g'ri o'lchamdagi fayl JIMGINA buziladi: yuklash muvaffaqiyatli
 * tugaydi, panelda hammasi joyida ko'rinadi, va faqat telefonda yuzning
 * yarmi kesilgani bilinadi.
 *
 * Yagona himoya — yozuvning O'ZI. Shuning uchun:
 *
 *   1. `spec` nomi xato yozilsa (`"posterr"`), `specLine` `null` qaytaradi
 *      va yozuv JIMGINA yo'qoladi. Aynan shu narsa sezilmasdan o'tib
 *      ketardi — barcha ishlatilgan nomlar ro'yxatda borligi tekshiriladi.
 *   2. Yozuv fayl tanlash OYNASIDA ham bo'lishi kerak: oyna maydonni
 *      to'sib qo'yadi va tanlash onida admin uni ko'ra olmasdi.
 */
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import MediaField from '../MediaField';
import { PanelI18nProvider } from '../../i18n';
import { MEDIA_SPECS, specLine } from '../../mediaSpecs';

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
  adminApi.media.mockResolvedValue({ items: [] });
});

/**
 * Panelda AMALDA ishlatilayotgan nomlar.
 *
 * ⚠️ Yangi media maydoni qo'shilsa, nomi shu yerga ham qo'shiladi.
 * Ro'yxatni fayllardan avtomatik yig'ish mumkin emas: test muhitida
 * `require.context` yo'q, qidiruv esa `spec={isAd ? ... : ...}` kabi
 * shartli qiymatni topa olmasdi.
 */
const USED = [
  'poster',
  'cover',
  'gallery',
  'video',
  'trailer',
  'seasonPoster',
  'episodeThumb',
  'episodeVideo',
  'banner',
  'bannerMobile',
  'premiereImage',
  'premiereVideo',
  'creatorPhoto',
  'creatorCover',
  'categoryIcon',
  'notificationImage',
];

describe("Media o'lchami", () => {
  test.each(USED)('«%s» uchun o\'lcham yozuvi mavjud', (name) => {
    expect(MEDIA_SPECS[name]).toBeDefined();
    expect(specLine(name)).toEqual(expect.stringContaining('px'));
  });

  /**
   * Tavsiya etilgan fayl ilovadagi ENG KATTA ramkani qoplaydimi.
   *
   * <h2>Nima uchun bu alohida tekshiriladi</h2>
   * Bu xatoni ko'z bilan topib bo'lmaydi. Afisha kartochkada 396×594 px,
   * va shunga qarab 600×900 tavsiya qilish to'g'ridek ko'rinadi — panel
   * ham, kartochka ham chiroyli chiqadi. Lekin O'SHA afisha yopiq kontent
   * ekranida 1194×672 bo'lib chiziladi va u yerda ikki barobar cho'ziladi.
   *
   * Ya'ni yozuv «to'g'ri» bo'lib turadi, admin unga rioya qiladi, va rasm
   * baribir donador chiqadi — aybdor esa topilmaydi.
   *
   * `contentFit="cover"` masshtabi: max(ramkaW/faylW, ramkaH/faylH).
   * 1 dan katta bo'lsa — fayl cho'ziladi.
   */
  /**
   * Ilovada AMALDA chiziladigan maydonlar.
   *
   * ⚠️ Ro'yxatsiz quyidagi test bo'sh joyda o'tib ketardi: `frames` ni
   * o'chirib qo'yish yetardi va «cho'zilmaydi» degan tekshiruv hech
   * narsani tekshirmay qolardi. Ya'ni testni «tuzatish» eng oson yo'li
   * uni ishdan chiqarish bo'lardi.
   */
  const RENDERED = [
    'poster',
    'seasonPoster',
    'episodeThumb',
    'banner',
    'premiereImage',
    'creatorPhoto',
    'categoryIcon',
  ];

  test.each(RENDERED)("«%s» uchun ramka o'lchami yozilgan", (name) => {
    expect(MEDIA_SPECS[name].frames?.length).toBeGreaterThan(0);
  });

  test('tavsiya etilgan fayl ramkalarni qoplaydi', () => {
    const stretched = [];

    Object.entries(MEDIA_SPECS).forEach(([name, spec]) => {
      if (!spec.frames) return;
      const [w, h] = spec.size.split('×').map(Number);
      spec.frames.forEach(([fw, fh]) => {
        const scale = Math.max(fw / w, fh / h);
        // 1.02 - yumaloqlash uchun zaxira: 0.5% cho'zilish ko'rinmaydi.
        if (scale > 1.02) {
          stretched.push(`${name}: ${spec.size} -> ${fw}×${fh} (×${scale.toFixed(2)})`);
        }
      });
    });

    expect(stretched).toEqual([]);
  });

  test("har bir o'lcham uchta tilda izohlangan", () => {
    // Bo'sh tarjima yozuvni yo'q qilmaydi, lekin izohsiz qoldiradi:
    // «1200×1800 px» raqamning O'ZI nima uchun kerakligini aytmaydi.
    const missing = [];
    Object.entries(MEDIA_SPECS).forEach(([name, spec]) => {
      ['uz', 'ru', 'en'].forEach((locale) => {
        if (!spec.note?.[locale]) missing.push(`${name}.note.${locale}`);
      });
    });
    expect(missing).toEqual([]);
  });

  test("fayl tanlash oynasida ham o'lcham takrorlanadi", async () => {
    // ⚠️ Oyna maydonni to'sib qo'yadi — takrorsiz yozuv aynan tanlash
    // ONIDA ko'rinmay qolardi.
    wrap(<MediaField label="Afisha" spec="poster" value={null} onChange={() => {}} />);

    fireEvent.click(screen.getByRole('button', { name: /yuklash/i }));

    await waitFor(() => {
      expect(screen.getAllByText(/1200×1800 px/).length).toBeGreaterThan(1);
    });
  });

  test("maydon tagida o'lcham ko'rinadi", async () => {
    wrap(<MediaField label="Afisha" spec="poster" value={null} onChange={() => {}} />);

    await waitFor(() => {
      expect(screen.getByText(/1200×1800 px/)).toBeInTheDocument();
    });
  });

  test("noma'lum nom berilsa hech narsa chizilmaydi", () => {
    // ⚠️ Xato nom yozuvni YO'Q QILADI, sahifani emas. Maydon shu holatda
    // ham ishlashi kerak: o'lcham maslahati kontentni tahrirlashdan
    // muhimroq emas.
    wrap(<MediaField label="Afisha" spec="bunday-nom-yoq" value={null} onChange={() => {}} />);

    expect(screen.getByText('Afisha')).toBeInTheDocument();
    expect(screen.queryByText(/px/)).not.toBeInTheDocument();
  });
});
