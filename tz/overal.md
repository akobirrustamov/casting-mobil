# UzCasting — loyihaning umumiy mazmuni

> Butun loyiha bo'yicha bir sahifalik xulosa: mahsulot nima, kod qayerda,
> nima tayyor, nima to'sib turibdi.
>
> Batafsil hujjatlar — `mobile/docs/`. Kirish nuqtasi: **`mobile/docs/ROADMAP.md`**.
> Admin platforma hujjatlari — **`roadmap/`** papkasida.
> Miro doskasi — `tools/miro_build.py` orqali yig'iladi.

Oxirgi yangilanish: 19.08.2026

---

## 1. Mahsulot

UzCasting — uchta narsani birlashtiruvchi mobil platforma:

1. **Casting** — aktyor, model, bloger va boshqa ijodkorlar professional portfolio
   yuritadi va rollarga ariza beradi.
2. **Premyera** — pullik eksklyuziv kontent: seriallar, shou, filmlar.
3. **Creator economy** — ijodkor kontent yuklaydi, sotuvdan ulush oladi va pulini yechadi.

### Monetizatsiya yadrosi

```
Bloger 1-qismni YouTube/Instagram'da BEPUL joylaydi
        ↓
Video oxirida CTA: «Davomi — UzCasting'da»
        ↓
Foydalanuvchi ilovada qismni sotib oladi (Click / Payme / Uzum / UZCARD / Visa / MC)
        ↓
To'lov tasdiqlandi → entitlement → himoyalangan player
        ↓
Revenue share 50/50 — ijodkor va platforma o'rtasida
```

Bu ilovaning asosiy stsenariysi. Qolgan hammasi — shu atrofdagi qobiq.

---

## 2. Repozitoriy tuzilishi

| Papka | Nima |
|---|---|
| `mobile/` | **Asosiy ish.** Expo SDK 57 ilovasi (RN 0.86, React 19), ~5 000 qator |
| `backend/` | Spring Boot 3.1 + JPA + PostgreSQL. **Allaqachon ishlayapti** — `uzcasting.site` |
| `frontend/` | CRA sayti, o'sha backend ustida. Mavjud mahsulot |
| `roadmap/` | Admin platforma hujjatlari: roadmap va arxitektura |
| `tz/` | Buyurtmachi ТЗ'sining 4 ta PDF versiyasi + shu fayl |
| `tools/miro_build.py` | Miro doskasini to'liq yig'uvchi skript |

**Muhim:** bu noldan boshlanadigan loyiha emas. Casting qismi allaqachon ishlaydi —
backend, sayt, Telegram-bot va admin panel bor. Mobil ilova mavjud API ustiga quriladi.

---

## 3. Texnik stek (mobil)

| Qatlam | Tanlov |
|---|---|
| Platforma | Expo SDK 57, expo-router (file-based) |
| Stillar | NativeWind 4.2 + Tailwind 3.4 |
| Server state | TanStack Query |
| Klient state | Zustand |
| HTTP | axios |
| i18n | i18next — UZ (asosiy) / RU / EN |
| Video | expo-video |
| Tokenlar | expo-secure-store |
| To'lovlar | expo-web-browser + deeplink `uzcasting://` |

---

## 4. Dizayn tizimi

Palitra ТЗ V2, 18-betdan — **tasdiqlangan, o'zgartirilmaydi**:

| Token | HEX | Roli |
|---|---|---|
| `ink` | `#07070D` | Ilova foni |
| `surface` | `#11111F` | Kartochka |
| `purple` | `#7C3AED` | Asosiy CTA |
| `magenta` | `#EC4899` | Premium / «PREMYERA» bejigi |
| `cyan` | `#22D3EE` | Info / secondary |
| `gold` | `#F5C542` | Verified / pul yechish |
| `text` | `#FFFFFF` | Asosiy matn |

Qo'shimcha (bizning yechim, 8 holat uchun zarur): `surface-2`, `border`,
`text-muted`, `text-disabled`, `success`, `danger`.

Kodda manba: `tailwind.config.js` + `src/theme/tokens.ts` — **ikkalasi ham tuzatiladi**.

### Qoidalar
- Spacing 8–16px · radius 14–22px · touch target ≥44px
- Har ekranda **bitta** asosiy CTA
- Dark mode birinchi — light tema yo'q
- Har ekranda 8 majburiy holat:
  `loading · empty · error · success · locked · purchased · disabled · offline`

### Navigatsiya — 5 ta vkladka (ТЗ'dan)
```
Bosh sahifa · Casting · Premyera · Xabarlar · Profil
```
Rollar: **User** (5 vkladka) · **Creator** (+ Studio, Profil ichida) · **Admin** (alohida veb).

---

## 5. Monetizatsiya — 4 daraja

Buyurtmachining 13.08.2026 xabari ТЗ raqamlarini **bekor qiladi**.

| Daraja | Narx | Nima beradi |
|---|---|---|
| Bitta qism | **3 000 so'm** | Faqat tanlangan qism (ТЗ'da 5 000 edi — eskirgan) |
| Bitta premyera | **15 000 so'm** | Tanlangan serialning mavjud qismlari |
| Premium obuna | 24 000 / 49 999 / 99 000 / **159 900** so'm | 1 / 3 / 6 / 12 oy. Reklamasiz, barcha kontent |
| Stars | paketlar: 10 · 50 · 100 · 500 · 1 000 | Donat + reyting + oylik taqdirlash shousi |

Premium hammasini ochadi; qism va premyera sotib olish — obuna istamaganlar uchun.

⚠️ Premyera, Stars, reyting va reklama — **ТЗ'da umuman yo'q**, hammasi 13.08 xabaridan keladi.

---

## 6. Hozirgi holat

**Tayyor (🟩):** Splash · Onboarding · Google orqali kirish (end-to-end) · Home ·
Kategoriyalar · Kataloglar (filtr: jins/yosh/shahar) · Ijodkor profili · Qidiruv ·
Sevimlilar · Foydalanuvchi profili · Casting lentasi · Premyera katalogi

**Boshlanmagan (⬜):** Casting detali va ariza · Qism detali · Video player · To'lov ·
Creator Studio · Premium · Sozlamalar

**Ma'lumot manbalari:** ijodkorlar — **haqiqiy API'dan**
(`GET /api/v1/casting-user/web`). Premyera va casting e'lonlari —
`src/lib/placeholder.ts` (vaqtinchalik, endpoint chiqishi bilan butunlay o'chiriladi).

---

## 7. 🔴 Uchta bloker

Tartib — arxitektura yoki iqtisodni buzish darajasi bo'yicha.

### 1. Store billingi
Qismlar, premyeralar, obuna va Stars — ilova ichidagi **raqamli kontent**.
Google Play va App Store o'z billingini talab qiladi: **15–30% komissiya**.
Click/Payme/Uzum bunday kontent uchun qoidani buzadi → ilova olib tashlanadi.

Komissiya 50/50 dan **oldin** yechiladi: 3 000 so'mlik qismda ijodkor 1 500 emas,
~1 050–1 275 so'm oladi.

**To'lov moduli yozilishidan oldin hal qilinsin.**

### 2. Butun API himoyasiz
`SecurityConfig.java` — barcha metod va yo'llarda `permitAll`.
`DELETE` va admin endpointlar tokensiz ochiq. JWT-filtr yozilgan, lekin tekshiradigan
narsa yo'q. **Hozircha klientdagi himoya hech narsani anglatmaydi.**

### 3. Voyaga yetmaganlar ma'lumoti
Ochiq endpoint telefon, email, aniq tug'ilgan sana va tana o'lchovlarini qaytaradi —
`age: 17` anketalar ham. Mobil ilova bu maydonlarni ataylab olmaydi, lekin muammo
backend tomonida. **Bu texnik qarz emas, yuridik risk.**

---

## 8. Buyurtmachiga savollar

Javobsiz bu ekranlar loyihalanmaydi:

1. **Stars narxi** — 10 Stars necha so'm?
2. **Stars ijodkor daromadimi?** Pul qilib yechsa bo'ladimi, 50/50 amal qiladimi?
3. **15 000 so'mlik premyera** kelajakdagi qismlarga tarqaladimi?
4. **Reklama** — qaysi tarmoq, formatlar, joylar? ТЗ'da yo'q.
5. **Obuna avtoprodleniesi** — ha yoki yo'q?
6. **5 000 vs 3 000** — 3 000 yakuniy ekanini tasdiqlash.
7. **«Styling» va «Kurslar»** — alohida katalog kerakmi?
8. **Kategoriyalar: 10 mi yoki 4?** Bazada `castingType` bo'yicha 10 tadan faqat
   4 tasi bor (`actor`, `model`, `bloger`, `influencer`).
9. **Lotin yoki kirill?** Hujjatlar kirillda, maketlar lotinda, kodda lotin.
10. **Refund qoidalari** — ТЗ eslatadi, lekin yozmagan.
11. **Kontent himoyasi** — DRM (Widevine) kerakmi yoki qisqa TTL'li signed URL yetarlimi?
12. **Verified badge** — kim va qanday mezon bo'yicha beradi?

---

## 9. Materiallar

### `tz/` ichidagi PDF'lar — bir hujjatning 4 versiyasi

| Fayl | Nima olinadi |
|---|---|
| `..._Premium_UIUX_..._V2.pdf` | **Asosiy.** Dizayn tizimi (18-bet), navigatsiya va rollar (19), bosqichlar va QA (20) |
| `..._0dan_Toliq_..._V3.pdf` | **26 ta ekranning to'liq ro'yxati** |
| `..._Premium_UZ_Toliq_Dizayn_V4.pdf` | Maketlar 1170×2532 (15 ta) |
| `UzCasting_UI_UX_Texnik_Topshiriq.pdf` | Bazaviy versiya, texnik talablar |

⚠️ **V4 maketlari vyorstka uchun referens bo'la olmaydi** — AI generatsiya qilgan
kollajlar, matn o'qib bo'lmaydi. Ishonchli tarzda faqat shular olinadi: tab-bar
tarkibi, to'q binafsha gamma, yumaloqlangan kartochkalar, pushti «PREMYERA» bejigi,
oltin pul yechish tugmasi. **Haqiqiy maketlar Figma'da noldan qilinadi.**

### Hujjatlar

| Fayl | Nima haqida |
|---|---|
| `roadmap/roadmap.md` | **Admin platforma** — umumiy holat, audit, bosqichlar |
| `roadmap/BACKEND_ROADMAP.md` | Backend checklist |
| `roadmap/BACKEND_ARCHITECTURE.md` | Backend arxitekturasi |
| `roadmap/FRONTEND_ROADMAP.md` | Frontend checklist |
| `roadmap/FRONTEND_ARCHITECTURE.md` | Frontend arxitekturasi |
| `mobile/docs/ROADMAP.md` | **Mobil ilova** — bosh hujjat |
| `mobile/docs/API.md` | Mavjud backend, endpointlar, xavfsizlik muammosi |
| `mobile/docs/STRUCTURE.md` | Yangi.TV tahlili — ekranlar tuzilishi |
| `mobile/docs/MONETIZATION.md` | Narxlar, tariflar, Stars, store riski |
| `mobile/docs/GOOGLE_AUTH.md` | OAuth klientlarni sozlash (3 ta kerak) |
| `mobile/docs/LOCAL_BACKEND.md` | Telefondan test uchun lokal backend |
| `mobile/docs/BRANDING.md` | Logotip va ikonkalar |

---

## 10. Keyingi qadamlar

1. **Bosqich 1 — dizayn.** ТЗ talabi: *«Figma — final source of truth, ishlanma
   maketlar tasdiqlangandan keyin boshlanadi»*. Hozir shu bosqichda.
2. **Bosqich 2 — backend kontrakti.** Premyera, xarid, entitlement, to'lov, chat,
   balans, Creator Studio uchun bitta ham endpoint yo'q.
3. **Bosqich 3 — Android + UZ/RU**, reliz Google Play'ga.
4. **Bosqich 4 — iOS + EN**, reliz App Store'ga.

### Kod bo'yicha kelishuvlar
- Matnlar — faqat `t()` orqali, hardcode review'dan o'tmaydi
- Ranglar — faqat token orqali, komponentda HEX yozilmaydi
- Yangi ekran happy path'dan emas, **holatlardan** boshlanadi
