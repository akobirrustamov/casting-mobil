/**
 * Har bir media maydoni uchun TAVSIYA ETILGAN o'lcham.
 *
 * <h2>Nega bu kerak</h2>
 * Server rasmni QAYTA O'LCHAMAYDI: `/api/v1/app/media/{id}/raw` faylni
 * qanday yuklangan bo'lsa, shundayligicha qaytaradi. Mobil ilova esa uni
 * `contentFit="cover"` bilan chizadi — ya'ni ramkaga SIG'DIRMAYDI, balki
 * ortiqcha qismini QIRQADI.
 *
 * Demak yuklangan faylning nisbati noto'g'ri bo'lsa:
 *   - keng rasm afishada — chap va o'ng cheti kesiladi (yuzning yarmi yo'qoladi);
 *   - kichik rasm — telefonda cho'zilib, donador bo'lib ko'rinadi;
 *   - juda katta rasm — trafik va xotira behuda sarflanadi, ro'yxat sekinlashadi.
 *
 * Shuning uchun o'lcham adminga YUKLASHDAN OLDIN aytiladi. Keyin aytish
 * kech: fayl allaqachon kontentga biriktirilgan bo'ladi.
 *
 * <h2>⚠️ Raqamlar qayerdan olingan</h2>
 * Mobil ilovadagi HAQIQIY ramkadan, taxminan emas. Hisob shunday:
 *
 *     piksel = dp (ilovadagi o'lcham) × 3 (zamonaviy telefon zichligi)
 *
 * Eng katta telefon olinadi (430dp keng, iPhone Pro Max) — eng kichigida
 * to'g'ri chiqqan fayl eng kattasida cho'zilib ketardi, teskarisi esa yo'q.
 *
 * <h2>⚠️ Bitta maydon — bir nechta ramka</h2>
 * `frames` — shu maydon chiziladigan BARCHA ramkalar, pikselda. Tavsiya
 * etilgan o'lcham ularning eng kattasini QOPLASHI kerak:
 *
 *     masshtab = max(ramkaW / faylW, ramkaH / faylH)
 *
 * `masshtab > 1` bo'lsa fayl cho'ziladi va donador chiqadi.
 *
 * Aynan shu joyda xato qilish oson. Masalan afisha kartochkada bor-yo'g'i
 * 396×594 px (132dp), va shunga qarab 600×900 tavsiya qilish mantiqiy
 * ko'rinadi. Lekin YOPIQ kontent ekranida o'sha afisha butun ekran
 * kengligida, 1194×672 bo'lib chiziladi (`WatchDetail.LockedPoster`) —
 * 600×900 u yerda ikki barobar cho'ziladi. Shuning uchun ramkalar ro'yxati
 * bu yerda saqlanadi va test ularni har safar qayta hisoblaydi
 * (`__tests__/mediaSpec.test.jsx`).
 *
 * Ilovadagi ramka o'zgarsa, shu yerdagi `frames` ham o'zgarishi kerak — aks
 * holda panel yolg'on maslahat berib turaveradi.
 *
 * <h2>Nega faqat izoh tarjima qilinadi</h2>
 * «600×900 px · 2:3 · JPG/PNG/WebP · ≤2 MB» — bu raqamlar, uchala tilda
 * ham bir xil o'qiladi. Uni uch marta yozish faqat xato imkoniyatini
 * qo'shardi. Tarjima qilinadigani — IZOH, chunki u gap.
 */

/** Rasm uchun odatiy formatlar (`MediaPicker` dagi `accept` bilan bir xil). */
const IMG = 'JPG / PNG / WebP';

/** Video — faqat pleyer ochadigani. `.mkv`, `.avi` saqlanadi, lekin o'ynatilmaydi. */
const VID = 'MP4 (H.264 + AAC)';

/**
 * Video manba uchun umumiy izoh.
 *
 * ⚠️ 1080p dan kattasi SIFAT QO'SHMAYDI: `VideoProfileSelector` manbadan
 * yuqori variant yasamaydi (profillar: 1080p / 720p / 480p). U faqat qayta
 * ishlashni ~2.4 barobar uzaytiradi — panel buni yuklashdan oldin ham
 * ogohlantiradi (`videoProbe.needsDownscaleWarning`).
 */
const VIDEO_NOTE = {
  uz: "Gorizontal — 1920×1080, vertikal (Reels) — 1080×1920. 1080p dan kattasi sifat qo'shmaydi, faqat qayta ishlashni uzaytiradi. mkv va avi saqlanadi, lekin pleyer ularni ochmaydi.",
  ru: 'Горизонтальное — 1920×1080, вертикальное (Reels) — 1080×1920. Больше 1080p качества не добавит, только удлинит обработку. mkv и avi сохранятся, но плеер их не откроет.',
  en: 'Landscape 1920×1080, vertical (Reels) 1080×1920. Above 1080p adds no quality, only longer processing. mkv and avi are stored but the player cannot open them.',
};

export const MEDIA_SPECS = {
  // ─────────────────────────────────────────────── kontent
  /**
   * Kontent afishasi.
   *
   * ⚠️ IKKI xil ramkada ko'rsatiladi va ikkalasi ham qirqadi:
   *   - kartochka — 2:3 (`orientation.cardRatio`), eng kattasi 132dp × 198dp;
   *   - yopiq kontent ekrani — 16:9 (`WatchDetail.LockedPoster` `frameRatio` oladi).
   * Shuning uchun asosiy tasvir MARKAZDA turishi kerak.
   */
  poster: {
    size: '1200×1800',
    ratio: '2:3',
    formats: IMG,
    maxMb: 2,
    where: 'PosterCard 132dp × 198dp (@3x = 396×594); LockedPoster 398dp × 224dp (@3x = 1194×672)',
    frames: [[396, 594], [1194, 672]],
    note: {
      uz: "Vertikal (Reels) kontent uchun 720×1280 (9:16). ⚠️ Yopiq kontent ekranida afisha BUTUN EKRAN kengligida, 16:9 ga qirqilib chiziladi — shuning uchun fayl katta bo'lishi shart, aks holda u yerda donador chiqadi. Yuz va asosiy tasvirni markazda saqlang.",
      ru: 'Для вертикального (Reels) контента — 720×1280 (9:16). ⚠️ На экране закрытого контента афиша рисуется во ВСЮ ширину экрана с обрезкой до 16:9 — поэтому файл должен быть крупным, иначе там он будет зернистым. Держите лицо и главное в центре.',
      en: 'For vertical (Reels) content use 720×1280 (9:16). ⚠️ On the locked-content screen the poster is drawn at FULL screen width, cropped to 16:9 — so the file must be large or it looks grainy there. Keep the subject centred.',
    },
  },

  /**
   * Muqova.
   *
   * ⚠️ Hozircha mobil ilova uni CHIZMAYDI — `HomeFeedService` kartochkaga
   * faqat `POSTER` rolini beradi. Maydon saqlanadi (veb va kelajakdagi
   * detal ekrani uchun), lekin telefonda ko'rinmaydi.
   */
  cover: {
    size: '1600×900',
    ratio: '16:9',
    formats: IMG,
    maxMb: 3,
    where: 'hozircha ilovada ishlatilmaydi (`HomeFeedService` faqat POSTER ni beradi)',
    note: {
      uz: "Hozircha mobil ilovada ko'rsatilmaydi — u faqat afishani oladi. Keng gorizontal kadr yuklang.",
      ru: 'Пока не показывается в приложении — оно берёт только афишу. Загружайте широкий горизонтальный кадр.',
      en: 'Not shown in the app yet — it only uses the poster. Upload a wide landscape frame.',
    },
  },

  gallery: {
    size: '1600×900',
    ratio: '16:9',
    formats: IMG,
    maxMb: 3,
    where: 'hozircha ilovada ishlatilmaydi',
    note: {
      uz: "Barcha rasmlar BIR XIL nisbatda bo'lsin — aks holda galereya qatori teng bo'lmagan balandlikda ko'rinadi.",
      ru: 'Все изображения должны быть в ОДНОЙ пропорции — иначе ряд галереи получится разной высоты.',
      en: 'Keep every image at the SAME ratio — otherwise the gallery row ends up uneven.',
    },
  },

  video: {
    size: '1920×1080',
    ratio: '16:9',
    formats: VID,
    maxMb: null,
    where: 'Player; chiqish profillari 1080p / 720p / 480p',
    note: VIDEO_NOTE,
  },

  trailer: {
    size: '1920×1080',
    ratio: '16:9',
    formats: VID,
    maxMb: null,
    where: 'Player; chiqish profillari 1080p / 720p / 480p',
    note: VIDEO_NOTE,
  },

  // ─────────────────────────────────────────────── fasl va qism
  /**
   * Fasl afishasi.
   *
   * ⚠️ Hozircha ilova fasl afishasini so'ramaydi, lekin nisbati kontent
   * afishasi bilan BIR XIL bo'lishi kerak: ishlatila boshlanganda u xuddi
   * shu 2:3 kartochkaga tushadi.
   */
  seasonPoster: {
    size: '1200×1800',
    ratio: '2:3',
    formats: IMG,
    maxMb: 2,
    where: 'kontent afishasi bilan bir xil ramka',
    frames: [[396, 594]],
    note: {
      uz: "Kontent afishasi bilan bir xil o'lcham. Vertikal kontent uchun 720×1280 (9:16).",
      ru: 'Та же пропорция, что у афиши контента. Для вертикального — 720×1280 (9:16).',
      en: 'Same ratio as the content poster. For vertical content use 720×1280 (9:16).',
    },
  },

  /**
   * Qism kadri.
   *
   * ⚠️ Ramka 3:2, 16:9 EMAS (`EpisodeList`: `h-16 w-24` = 96dp × 64dp).
   * 16:9 kadr yuklansa chap va o'ng cheti qirqiladi — chekkadagi yozuv
   * yoki subtitr yo'qoladi.
   */
  episodeThumb: {
    size: '720×480',
    ratio: '3:2',
    formats: IMG,
    maxMb: 1,
    where: 'EpisodeList 96dp × 64dp (@3x = 288×192); vertikalda 54dp × 96dp (@3x = 162×288)',
    frames: [[288, 192]],
    note: {
      uz: "⚠️ Ramka 3:2, 16:9 emas — keng kadrning chap va o'ng cheti qirqiladi. Vertikal kontentda ramka 9:16 bo'ladi: 720×1280 yuklang.",
      ru: '⚠️ Рамка 3:2, а не 16:9 — у широкого кадра обрежутся левый и правый края. У вертикального контента рамка 9:16: загружайте 720×1280.',
      en: '⚠️ The frame is 3:2, not 16:9 — a wide frame loses its left and right edges. For vertical content the frame is 9:16: upload 720×1280.',
    },
  },

  episodeVideo: {
    size: '1920×1080',
    ratio: '16:9',
    formats: VID,
    maxMb: null,
    where: 'Player; chiqish profillari 1080p / 720p / 480p',
    note: VIDEO_NOTE,
  },

  // ─────────────────────────────────────────────── vitrina
  /**
   * Reklama banneri — bosh sahifadagi karusel.
   *
   * ⚠️ Nisbat TELEFONGA QARAB o'zgaradi: `HeroCarousel` da balandlik qat'iy
   * 210dp, kenglik esa ekran kengligidan 32dp kam. Ya'ni 360dp ekranda
   * 1.56:1, 430dp ekranda 1.89:1. Hech bir fayl ikkalasiga ham aniq to'g'ri
   * kelmaydi — shuning uchun 16:9 (o'rtasi) tavsiya etiladi va muhim narsa
   * markazda saqlanadi.
   */
  banner: {
    size: '1280×720',
    ratio: '16:9',
    formats: IMG,
    maxMb: 2,
    where: 'HeroCarousel (ekran − 32dp) × 210dp; eng kattasi 398dp × 210dp (@3x = 1194×630)',
    frames: [[1194, 630]],
    note: {
      uz: "Nisbat telefon kengligiga qarab 1.6:1 dan 1.9:1 gacha o'zgaradi — matn va logotipni MARKAZDA saqlang. Pastki chap burchakni sarlavha, tavsif va tugma yopadi: u yerga yozuv qo'ymang.",
      ru: 'Пропорция плавает от 1.6:1 до 1.9:1 в зависимости от ширины телефона — держите текст и логотип В ЦЕНТРЕ. Левый нижний угол закрывают заголовок, описание и кнопка: не размещайте там надписи.',
      en: 'The ratio floats between 1.6:1 and 1.9:1 depending on phone width — keep text and logo CENTRED. The bottom-left corner is covered by the title, subtitle and button: put no text there.',
    },
  },

  /**
   * Tor ekran uchun banner.
   *
   * ⚠️ Mobil ILOVA buni OLMAYDI. `HomeFeedService` bannerga faqat
   * `a.getImage()` ni qo'yadi — ya'ni telefonda ham ASOSIY rasm ko'rinadi.
   * Bu maydon veb sayt uchun.
   */
  bannerMobile: {
    size: '1080×1350',
    ratio: '4:5',
    formats: IMG,
    maxMb: 2,
    where: 'veb sayt; mobil ILOVA bu maydonni olmaydi',
    note: {
      uz: "⚠️ Mobil ILOVA bu rasmni OLMAYDI — u har doim asosiy rasmni ko'rsatadi. Bu maydon faqat veb saytdagi tor ekran uchun.",
      ru: '⚠️ Мобильное ПРИЛОЖЕНИЕ эту картинку НЕ берёт — оно всегда показывает основную. Поле работает только для узкого экрана на сайте.',
      en: '⚠️ The mobile APP does not use this image — it always shows the main one. This field only affects narrow screens on the website.',
    },
  },

  /**
   * Premyera rasmi.
   *
   * ⚠️ Bu BANNER EMAS, AFISHA. `PremiereRail` premyeralarni `PosterCard`
   * bilan, `cardRatio('LANDSCAPE')` = 2:3 da chizadi. Reklama banneri kabi
   * keng rasm yuklansa, uning ko'p qismi qirqiladi.
   */
  premiereImage: {
    size: '600×900',
    ratio: '2:3',
    formats: IMG,
    maxMb: 2,
    where: 'PremiereRail → PosterCard 132dp × 198dp (@3x = 396×594)',
    /* ⚠️ Kontent afishasidan FARQLI: premyera rasmi yopiq kontent
       ekraniga tushmaydi, u faqat qatorda turadi. Shuning uchun 600×900
       yetarli va uni 1200×1800 ga ko'tarish faqat trafikni oshirardi. */
    frames: [[396, 594]],
    note: {
      uz: "⚠️ Premyera reklama banneridan FARQ QILADI: u tik afisha (2:3) sifatida ko'rsatiladi, keng banner emas. Keng rasm yuklansa chap va o'ng cheti qirqiladi.",
      ru: '⚠️ Премьера ОТЛИЧАЕТСЯ от рекламного баннера: она показывается вертикальной афишей (2:3), а не широким баннером. У широкой картинки обрежутся края.',
      en: '⚠️ A premiere differs from an ad banner: it is shown as an upright 2:3 poster, not a wide banner. A wide image loses its left and right edges.',
    },
  },

  premiereVideo: {
    size: '1920×1080',
    ratio: '16:9',
    formats: VID,
    maxMb: null,
    where: 'Player; chiqish profillari 1080p / 720p / 480p',
    note: VIDEO_NOTE,
  },

  // ─────────────────────────────────────────────── ijodkorlar va bo'limlar
  /** Ijodkor surati — `StoryCircle` da DUMALOQ qirqiladi (64dp). */
  creatorPhoto: {
    size: '400×400',
    ratio: '1:1',
    formats: IMG,
    maxMb: 1,
    where: 'StoryCircle 64dp (@3x = 192×192)',
    frames: [[192, 192]],
    note: {
      uz: "Rasm DUMALOQ qirqiladi — yuz aniq markazda bo'lsin, chekkalarda bo'sh joy qoldiring. Kvadrat bo'lmagan rasmning cheti kesiladi.",
      ru: 'Изображение обрезается по КРУГУ — лицо строго по центру, оставьте поля по краям. У неквадратного фото обрежутся края.',
      en: 'The image is cropped to a CIRCLE — centre the face and leave margins. A non-square photo loses its edges.',
    },
  },

  creatorCover: {
    size: '1600×900',
    ratio: '16:9',
    formats: IMG,
    maxMb: 2,
    where: 'hozircha ilovada ishlatilmaydi',
    note: {
      uz: "Hozircha mobil ilovada ko'rsatilmaydi — ilova faqat suratni oladi.",
      ru: 'Пока не показывается в приложении — оно берёт только фото.',
      en: 'Not shown in the app yet — it only uses the photo.',
    },
  },

  /**
   * Bo'lim ikonkasi.
   *
   * ⚠️ `CategoryTile` bu maydonni ATAYLAB olmaydi: bazada bu yerga afisha
   * o'lchamidagi rasm tushib qolgan, va 116×76 lik plitkada undan
   * o'qib bo'lmaydigan dog' hosil bo'lardi. Hozir plitkada vektor glif
   * chiziladi.
   */
  categoryIcon: {
    size: '512×512',
    ratio: '1:1',
    formats: 'PNG / SVG (shaffof fon)',
    maxMb: 1,
    where: "CategoryTile 116dp × 76dp (@3x = 348×228) — hozir o'rniga vektor glif chiziladi",
    frames: [[348, 228]],
    note: {
      uz: "⚠️ Hozircha ilova plitkada o'z belgisini chizadi, bu ikonkani emas. Yuklaganda — SHAFFOF fonli, bitta rangli belgi bo'lsin; afisha yoki yozuvli rasm emas.",
      ru: '⚠️ Пока приложение рисует на плитке свой значок, а не эту иконку. Если загружаете — это должен быть одноцветный знак на ПРОЗРАЧНОМ фоне, а не афиша с надписью.',
      en: '⚠️ The app currently draws its own glyph on the tile, not this icon. If you upload one, use a single-colour mark on a TRANSPARENT background — not a poster with text.',
    },
  },

  // ─────────────────────────────────────────────── bildirishnoma
  /**
   * Bildirishnoma rasmi.
   *
   * ⚠️ Hozircha u HECH QAYERDA ko'rsatilmaydi: `NotificationDto` uni faqat
   * panelga qaytaradi, ilovada esa rasm chizadigan ekran yo'q. O'lcham push
   * bildirishnomaning odatiy formatiga qarab berilgan.
   */
  notificationImage: {
    size: '1024×512',
    ratio: '2:1',
    formats: IMG,
    maxMb: 1,
    where: 'push bildirishnoma standarti; hozircha ilovada chizilmaydi',
    note: {
      uz: "⚠️ Hozircha ilovada ko'rsatilmaydi. Push bildirishnoma rasmi keng va past bo'ladi — matnni rasmga yozmang, u qirqilishi mumkin.",
      ru: '⚠️ Пока в приложении не показывается. Картинка пуш-уведомления широкая и низкая — не пишите текст на изображении, его может обрезать.',
      en: '⚠️ Not shown in the app yet. A push image is wide and short — do not put text on it, it can be cropped.',
    },
  },
};

/**
 * Bitta qatorlik texnik yozuv: «600×900 px · 2:3 · JPG/PNG/WebP · ≤2 MB».
 *
 * Tarjima qilinmaydi — bu raqamlar, gap emas.
 */
export function specLine(name) {
  const s = MEDIA_SPECS[name];
  if (!s) return null;
  const parts = [`${s.size} px`, s.ratio, s.formats];
  if (s.maxMb) parts.push(`≤${s.maxMb} MB`);
  return parts.join(' · ');
}

/** Izoh — tanlangan tilda. Til topilmasa o'zbekchasi. */
export function specNote(name, locale) {
  const s = MEDIA_SPECS[name];
  if (!s || !s.note) return null;
  return s.note[locale] || s.note.uz;
}
