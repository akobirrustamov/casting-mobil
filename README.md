# UZCASTING — tizim qo'llanmasi

Video streaming + casting platformasi. Bitta repozitoriyda uch mahsulot:
**backend** (Spring Boot), **admin panel** (React) va **mobil ilova** (Expo).

Bu hujjat — tizimning umumiy xaritasi: kim nima qila oladi, qaysi havola
nimaga olib boradi, fayllar qanday yuklanadi va boshqariladi.

### Konsepsiya va scope

**UZCASTING — video streaming platformasi**: qisqa kino, mini-serial, serial,
podkast, shou, ijodkorlar, casting, monetizatsiya va media.

Eski TZ'dagi «ijtimoiy tarmoq / messenger» konsepsiyasi **asosiy vazifa deb
qabul qilinmadi**. Hozirgi scope'da yo'q: do'stlar tizimi · messenger ·
ijtimoiy lenta · shaxsiy chat.

Mavjud **casting moduli o'chirilmaydi va regressiyaga uchramaydi** — bu ikki
test bilan kafolatlangan (`OldCastingFrozenTest`, `ExistingCastingRegressionTest`).

> ⚠️ Eski koddagi `Message` entity — **messenger emas**. U Telegram botga
> yuboriladigan «qabul qilindingiz / rad etildi» javobi. O'chirilsa nomzodlar
> hech qachon javob olmaydi. Batafsil: `roadmap/roadmap.md` → SOURCE OF TRUTH.

> Batafsil texnik hujjatlar `roadmap/` papkasida:
> `roadmap.md` (umumiy holat, qarorlar, buglar), `BACKEND_ROADMAP.md`,
> `FRONTEND_ROADMAP.md`, `BACKEND_ARCHITECTURE.md`, `FRONTEND_ARCHITECTURE.md`.

---

## 1. Repozitoriy tuzilishi

```
backend/     Spring Boot 3.1 · Java 17 · Flyway · JWT
  src/main/java/com/example/backend/
    Admin/        YANGI — admin panel API   → /api/v1/app/admin/**
    Cms/          YANGI — kontent moduli    → /api/v1/app/**
    Security/     JWT, SecurityConfig, rate limiting
    Controller/   ESKI casting — MUZLATILGAN, tegilmaydi
    Entity/ Repository/ Services/    ESKI casting moduli
  src/main/resources/db/migration/   V1…V9 Flyway migratsiyalari
  files/        yuklangan fayllar (git'ga tushmaydi)

frontend/    React 18 (CRA)
  src/adminpanel/    YANGI admin panel   → /app/panel/**
  src/admin/         ESKI casting admin  → /aadmin/**  (tegilmaydi)

mobile/      Expo (React Native) — foydalanuvchi ilovasi
roadmap/     texnik hujjatlar
tz/          buyurtmachi texnik topshiriqlari (PDF)
```

### Ishga tushirish (dev)

```bash
# 1. Backend — repozitoriy ILDIZIDAN ishga tushiriladi
#    (fayllar nisbiy 'backend/files' yo'lidan qidiriladi)
./backend/mvnw -f backend/pom.xml package
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 2. Frontend
cd frontend && npm install --legacy-peer-deps && npm start

# 3. Panel
open http://localhost:3000/app/panel/login
```

Dev bazasi — H2 fayl: `backend/data/uzcasting-dev.mv.db`. O'chirsangiz toza
boshlanadi va seeder mock ma'lumotni qaytadan yaratadi.

> ⚠️ Ishlab turgan ilova ustidan `package` qilmang — fat-jar klasslarni
> kechiktirib yuklaydi va `ClassNotFoundException` chiqadi. Avval to'xtating:
> `pkill -f "backend-0.0.1-SNAPSHOT.jar"`.

---

## 2. Eski va yangi — makon chegarasi

Repozitoriyda **ikkita tizim yonma-yon** yashaydi. Ular bir-biriga tegmaydi va
bu ataylab shunday.

### Eski casting tizimi — MUZLATILGAN

Undan hozir ishlab turgan mijozlar foydalanadi: **Telegram bot** (anketa
yuboradi, rasm yuklaydi), **eski admin sayti** (`/aadmin/*`) va **mobil ilova**.
Ular alohida joylashtirilgan va bir vaqtda yangilanmaydi — shuning uchun yo'lni
o'zgartirish ularning hammasini bir zumda sindiradi.

| Nima | Qayerda |
|---|---|
| Backend kodi | `backend/.../Controller/` · `Entity/` · `Repository/` · `Services/` |
| Backend yo'llari | `/api/v1/auth` · `/api/v1/news` · `/api/v1/file` · `/api/v1/casting-user` · `/api/v1/security` · `/api/v1/admin/statistic` |
| Frontend | `frontend/src/admin/` · `/aadmin/*` · `/admin/home` · `/` |
| Mobil ilova | `mobile/` — faqat eski API bilan ishlaydi |

> **`OldCastingFrozenTest`** shu ro'yxatni kodda qotirib qo'yadi: eski
> controller ko'chirilsa, nomi o'zgarsa yoki bitta endpoint yo'li o'zgarsa —
> build yiqiladi. Ya'ni "tegilmasin" qoidasi hujjatda emas, **testda**.
>
> Shu test eski paketga yangi controller qo'shilishiga ham yo'l qo'ymaydi.

### Yangi platforma — `/app` makonida

Bundan keyin ochiladigan **barcha** yo'llar shu makonda:

```
backend   /api/v1/app/**
frontend  /app/**
```

| Nima | Yo'l |
|---|---|
| Admin panel API | `/api/v1/app/admin/**` |
| Bosh sahifa (klient) | `/api/v1/app/home` |
| Tomosha (klient) | `/api/v1/app/watch/{episodeId}` |
| Media fayllari | `/api/v1/app/media/{id}/raw` |
| Analitika | `/api/v1/app/analytics/events` |
| Admin panel sahifalari | `/app/panel/**` |

**Nega shunday:** ikki makon hech qachon to'qnashmaydi. Yangi endpoint qo'shish
uchun eski kodga qarash ham, tegish ham kerak emas — ya'ni eski mijozlarni
tasodifan sindirib qo'yish imkoni yo'q.

**Yangi kod qayerda yoziladi:** backendda `Admin/` va `Cms/` paketlarida,
frontendda `src/adminpanel/` (yoki `/app` ostidagi yangi bo'limlarda).
Eski `Controller/` paketiga yangi fayl qo'shilmaydi.

> ⚠️ **Bitta istisno:** ilova foydalanuvchilari hozircha **eski**
> `POST /api/v1/auth/login` orqali kiradi — autentifikatsiya eski tizimda
> qolgan va u muzlatilgan. Yangi `/api/v1/app/auth/**` ochilsa, eski endpoint
> mijozlar ko'chguncha ishlab turishi kerak.

---

## 3. Rollar

Beshta rol, ierarxiya bo'yicha (raqam — daraja):

| Rol | Daraja | Kimni yarata oladi | Panelga kirish |
|---|---|---|---|
| **HYPER_ADMIN** | 100 | SUPER_ADMIN, ADMIN, WORKER | ✅ hamma modul |
| **SUPER_ADMIN** | 80 | ADMIN, WORKER | ✅ hamma modul |
| **ADMIN** | 60 | faqat WORKER | ✅ hamma modul |
| **WORKER** | 40 | hech kimni | ✅ faqat berilgan ruxsatlar |
| **USER** | 10 | — | ❌ **kira olmaydi** |

Asosiy qoidalar:

- **ADMIN va yuqorisi** ruxsatlar jadvaliga qaramaydi — ular uchun hamma
  narsa ochiq. Faqat rol ierarxiyasi cheklaydi.
- **WORKER** nima qila olishi aniq ruxsatlar bilan belgilanadi (pastda).
- **Hech kim o'zida bo'lmagan ruxsatni boshqaga bera olmaydi.**
- **Hech kim o'ziga teng yoki undan yuqori rolni boshqara olmaydi.**
- **Ko'rish va boshqarish ajratilgan:** HYPER_ADMIN barcha xodim hisoblarini
  ko'radi (o'zini va teng rolni ham) — audit uchun; quyi rollar esa faqat
  o'zidan quyini ko'radi. Boshqarish qoidasi ikkalasida bir xil qoladi.
- **HYPER_ADMIN ham o'ziga teng rol yarata olmaydi.** Sabab: ikkita
  HYPER_ADMIN bir-birini o'chira olmasdi, ya'ni bitta o'g'irlangan hisob
  hech kim olib tashlay olmaydigan hisoblar yaratardi. Yagona HYPER_ADMIN
  yo'qolsa, tiklash **serverda** — environment orqali (`roadmap.md`).
- **O'z hisobiga tegib bo'lmaydi** — rolni oshirish yoki o'zini bloklash yo'q.
- Ruxsat olib tashlansa, **mavjud token darhol kuchini yo'qotadi** — ruxsatlar
  tokenda emas, har so'rovda bazadan o'qiladi.

### Ruxsatlar (43 ta)

WORKER uchun ma'noli. Modul bo'yicha:

| Modul | Ruxsatlar |
|---|---|
| Kontent | `CONTENT_VIEW` `CONTENT_CREATE` `CONTENT_EDIT` `CONTENT_DELETE` `CONTENT_PUBLISH` |
| Kategoriya | `CATEGORY_VIEW` `CATEGORY_CREATE` `CATEGORY_EDIT` |
| Janr | `GENRE_VIEW` `GENRE_CREATE` `GENRE_EDIT` |
| Ijodkor | `CREATOR_VIEW` `CREATOR_CREATE` `CREATOR_EDIT` |
| Media | `MEDIA_VIEW` `MEDIA_UPLOAD` `MEDIA_DELETE` |
| Reklama | `ADVERTISEMENT_VIEW` `ADVERTISEMENT_CREATE` `ADVERTISEMENT_EDIT` `ADVERTISEMENT_DELETE` |
| Premyera | `PREMIERE_VIEW` `PREMIERE_CREATE` `PREMIERE_EDIT` `PREMIERE_DELETE` |
| Bosh sahifa | `HOMEPAGE_VIEW` `HOMEPAGE_EDIT` |
| Bildirishnoma | `NOTIFICATION_VIEW` `NOTIFICATION_CREATE` `NOTIFICATION_SEND` |
| Izohlar | `COMMENT_VIEW` `COMMENT_MODERATE` |
| Foydalanuvchilar | `USER_VIEW` `USER_BLOCK` `USER_PREMIUM_MANAGE` `USER_DEVICE_MANAGE` |
| Monetizatsiya | `TARIFF_VIEW` `TARIFF_EDIT` `DONATION_VIEW` `DONATION_PACKAGE_EDIT` |
| Sozlamalar | `SETTINGS_VIEW` `SETTINGS_EDIT` |
| Hisobot | `REPORT_VIEW` |

> **Muhim:** menyuda elementni yashirish xavfsizlik EMAS. Har bir endpoint
> backendda ham tekshiriladi. `AdminEndpointGuardTest` arxitektura testi
> qo'riqlanmagan endpoint qo'shishga yo'l qo'ymaydi.

### Dev hisoblari (parol hammasida `12345678`)

**Panel xodimlari** — `POST /api/v1/app/admin/auth/login`:

| Telefon | Rol | Izoh |
|---|---|---|
| `+998901110001` | HYPER_ADMIN | hamma narsa |
| `+998901110002` | SUPER_ADMIN | hamma narsa |
| `+998901110003` | ADMIN | hamma narsa |
| `+998901110004` | WORKER | 13 ta ruxsat (kontent bilan ishlaydi) |
| `+998901110005` | WORKER | 4 ta ruxsat (faqat ko'rish) |
| `+998901110009` | USER | ❌ panelga kira olmaydi — shuni tekshirish uchun |

**Ilova foydalanuvchilari** — `POST /api/v1/auth/login`:

| Telefon | Holat | Pullik qismda |
|---|---|---|
| `+998901112001` | Faol Premium | ✅ ko'radi, reklamasiz |
| `+998901112002` | Muddati o'tgan Premium | ❌ to'lov so'raladi |
| `+998901112003` | Bitta qism sotib olgan | ✅ faqat **o'sha** qismni |
| `+998901112004` | Bloklangan | ❌ hech narsani |
| `+998901112005` | Oddiy | ❌ pullikni, reklama ko'radi |

---

## 4. Admin panel — sahifalar

Kirish: **`/app/panel/login`** · Asosiy: **`/app/panel`**

Menyu foydalanuvchining ruxsatiga qarab yig'iladi:

| Havola | Sahifa | Ko'rinish sharti |
|---|---|---|
| `/app/panel` | Boshqaruv paneli | hammaga |
| `/app/panel/reports` | Hisobotlar | `REPORT_VIEW` |
| `/app/panel/content` | Kontent (film, serial, shou…) | `CONTENT_VIEW` |
| `/app/panel/creators` | Ijodkorlar | `CREATOR_VIEW` |
| `/app/panel/categories` | Kategoriyalar | `CATEGORY_VIEW` |
| `/app/panel/genres` | Janrlar | `GENRE_VIEW` |
| `/app/panel/media` | Media kutubxonasi | `MEDIA_VIEW` |
| `/app/panel/homepage` | Bosh sahifa bo'limlari | `HOMEPAGE_VIEW` |
| `/app/panel/ads` | Reklama va e'lonlar | `ADVERTISEMENT_VIEW` |
| `/app/panel/premieres` | Premyeralar | `PREMIERE_VIEW` |
| `/app/panel/comments` | Izohlar moderatsiyasi | `COMMENT_VIEW` |
| `/app/panel/notifications` | Bildirishnomalar | `NOTIFICATION_VIEW` |
| `/app/panel/users` | Foydalanuvchilar | `USER_VIEW` |
| `/app/panel/tariffs` | Tariflar va paketlar | `TARIFF_VIEW` |
| `/app/panel/staff` | Xodimlar | rol ≥ ADMIN |
| `/app/panel/settings` | Sozlamalar | `SETTINGS_VIEW` |
| `/app/panel/audit` | Audit jurnali | rol ≥ ADMIN |

Panel **uch tilda**: UZ / RU / EN. Til yuqoridagi tugmadan almashadi va
tanlov saqlanadi. Interfeys ham, kontent ham tarjima qilinadi.

---

## 5. Kontent modeli

### Tuzilma

Kontent uch xil shaklda bo'ladi (`structureType`):

```
SINGLE     Film            → qismlarsiz, video to'g'ridan-to'g'ri kontentda
EPISODIC   Mini-serial     → Kontent → Qism (faslsiz)
SEASONAL   Serial          → Kontent → Fasl → Qism
```

Har bir **qism** bir nechta **video segment**dan iborat bo'lishi mumkin
(`partNumber` bo'yicha tartiblangan) — Reels formatidagi kontent uchun.

### Turlari va sozlamalari

### ⚠️ Tur, kategoriya va janr — uch BOSHQA narsa

Bu eng ko'p chalkashtiriladigan joy:

| | Nima | Misol | Nechta |
|---|---|---|---|
| **Content type** | kontentning **shakli** | `MINI_SERIES` | bittasi |
| **Category** | katalog **bo'limi** | Drama | bittasi (ixtiyoriy) |
| **Genre** | **uslub** | Romance | bir nechta |

Bitta kontent uchalasiga bir vaqtda ega. Ular erkin kombinatsiyalanadi:
«Drama» kategoriyasida podkast ham, serial ham, film ham bo'lishi mumkin.

Shuning uchun kategoriya ro'yxatida `Podkast` yoki `Mini seriallar` kabi
nom **bo'lmasligi kerak** — ular tur, bo'lim emas. Kategoriya uchun to'g'ri
misollar: «O'zbek kinosi», «Xorijiy», «Bolalar uchun», «Hujjatli».

| Maydon | Qiymatlar |
|---|---|
| `contentType` | SHORT_FILM · MOVIE · MINI_SERIES · SERIES · PODCAST · SHOW · INTERVIEW · STREAM · CLIP · OTHER |
| `orientation` | LANDSCAPE (YouTube uslubi) · VERTICAL (Reels uslubi) |
| `status` | DRAFT · IN_REVIEW · SCHEDULED · PUBLISHED · ARCHIVED · BLOCKED |
| `visibility` | PUBLIC (katalogda) · UNLISTED (faqat havola) · PRIVATE (faqat xodim) |
| `language` | asarning **asl** tili, ISO 639-1 (`uz` · `ko` · `tr`…) — tarjima emas |
| `accessPolicy` | FREE · PREMIUM_ONLY · PURCHASE_ONLY · PREMIUM_OR_PURCHASE |

Faqat `PUBLISHED` holatdagi kontent foydalanuvchilarga ko'rinadi.

### Ko'p tillilik

Tarjimalar **alohida jadvallarda** saqlanadi (JSON ustunda emas) — shu sababli
har bir tilda qidiruv va indekslash ishlaydi:

- `content_translation`, `episode_translation`, `season_translation`,
  `category_translation`, `genre_translation`, `creator_translation` …
- Har birida `UNIQUE(parent_id, locale)`.
- **Qoralamada** o'zbekcha sarlavha majburiy — u asosiy til.
- **Nashr qilishda uchala til ham majburiy** (UZ + RU + EN). Xuddi shunday
  faol kategoriya, janr va ijodkor uchun — ular mobil ilova menyusida
  chiqadi. Xato xabarida qaysi til yetishmayotgani aytiladi.
- Tekshiruv saqlashda emas, **nashrda**: kontent odatda bitta tilda
  yoziladi, keyin tarjima qilinadi. Har saqlashda talab qilish ish
  jarayonini to'xtatardi.

**Media ham tilga bog'lanadi:** afisha har bir til uchun alohida bo'lishi
mumkin. `locale` bo'sh bo'lsa — u barcha tillar uchun ishlatiladi.

---

## 6. Media va fayllar

### Saqlash

```
backend/files/
  content/      panel orqali yuklangan fayllar (UUID nomlar)
  cms-dev/      dev seeder yaratgan namunalar
  .uploads/     bo'laklab yuklash — tugallanmagan bo'laklar (vaqtinchalik)
```

Qoidalar:

- **Fayl nomi butunlay server tomonida yasaladi** (UUID). Foydalanuvchi
  yuborgan nomdan faqat kengaytma olinadi — path traversal mumkin emas.
- Yo'l ikki marta tekshiriladi: yozishda ham, o'qishda ham ildizdan tashqariga
  chiqib bo'lmaydi.
- **Ruxsat etilgan kengaytmalar:** `jpg jpeg png webp gif svg` ·
  `mp4 mov webm m4v` · `pdf`. Boshqasi rad etiladi.
- Fayl **oqim orqali** yoziladi — katta video RAM'ga to'liq yuklanmaydi.

### Yuklash — ikki yo'l

Panel fayl o'lchamiga qarab **o'zi tanlaydi**, admin uchun farqi bilinmaydi:

| Fayl | Usul |
|---|---|
| ≤ 8 MB | bitta `multipart` so'rov |
| > 8 MB | **bo'laklab yuklash**, 5 MB'lik bo'laklar |

**Nega bo'laklab:** prodda `multipart` chegarasi 50 MB — epizod videosi
bunga sig'maydi. Bitta ulkan so'rov uzilsa hammasi boshidan boshlanardi.

Bo'laklab yuklash oqimi:

```
POST   /api/v1/app/admin/uploads                  → uploadId, chunkSize, totalChunks
PUT    /api/v1/app/admin/uploads/{id}/chunks/{n}  → bo'lak baytlari (xom tana)
GET    /api/v1/app/admin/uploads/{id}             → qaysi bo'laklar yetib kelgan
POST   /api/v1/app/admin/uploads/{id}/complete    → yig'ish → MediaAsset yaratiladi
DELETE /api/v1/app/admin/uploads/{id}             → bekor qilish
```

Xususiyatlari:

- **Uzilishdan keyin davom etadi** — server qabul qilgan bo'laklarni aytadi,
  klient faqat yetishmaganini qayta yuboradi.
- Har bo'lak uchun **3 martagacha qayta urinish** (faqat tarmoq va 5xx
  xatolarida; 4xx — serverning ongli rad javobi, qayta urinish foydasiz).
- **Sessiya faqat egasiga ochiq** — boshqa xodim, hatto ADMIN ham, 404 oladi.
- **Kengaytma eng boshida tekshiriladi** — gigabaytlab yuborib bo'lgach rad
  javobini olmaslik uchun.
- `MediaAsset` **faqat yig'ish tugagach** yaratiladi — yarim video media
  kutubxonasiga tushmaydi.
- Tashlab ketilgan sessiyalar sutkadan keyin avtomatik tozalanadi.
- Yuqori chegara: **5 GB** (`app.upload.max-bytes` bilan sozlanadi).

### Yetkazish

```
GET /api/v1/app/media/{id}/raw
```

| Fayl turi | Kirish |
|---|---|
| **Rasm** | ochiq — afishalar baribir hammaga ko'rinadi, 30 kun keshlanadi |
| **Video (qismga bog'langan)** | entitlement tekshiriladi — pastdagi jadval |
| **Video (biriktirilmagan)** | faqat panel xodimlari |

- Ruxsat bo'lmasa **404** qaytadi (403 emas) — faylning bor-yo'qligi ham
  oshkor qilinmaydi.
- Pullik videoga `Cache-Control: no-store` — obuna tugagach keshdan ochilib
  ketmasligi uchun.
- **`Range` qo'llab-quvvatlanadi** (206 Partial Content) — pleyer videoni
  butunlay yuklamaydi va oldinga o'tkazish (seek) ishlaydi.
- Barcha javoblarda qat'iy `Content-Security-Policy` — SVG ichidagi skript
  sayt domenida ishlab ketmasligi uchun.

### Boshqarish

```
GET    /api/v1/app/admin/media/{id}/usage   → fayl qayerda ishlatilyapti
DELETE /api/v1/app/admin/media/{id}         → o'chirish
```

Media **12 xil joydan** havola qilinadi: kontent galereyasi, qism videosi,
qism eskizi, fasl afishasi, ijodkor surati va muqovasi, kategoriya ikonkasi,
reklama (2 ta rasm), premyera (rasm va video), bildirishnoma rasmi.

**Arxivlash — o'chirishdan xavfsizroq.** Arxivlangan fayl kutubxonada
ko'rinmaydi (admin uni yangi kontentga qo'shib yubormaydi), lekin mavjud
havolalar ishlashda davom etadi. O'chirish esa faqat hech qayerda
ishlatilmagan fayl uchun.

Ishlatilayotgan faylni o'chirib bo'lmaydi — **409** qaytadi va javobda aynan
qayerda ishlatilayotgani yoziladi. Ko'r-ko'rona o'chirish sahifalarda sinib
qolgan rasm va o'ynamaydigan video degani.

---

## 7. Kim nimani ko'ra oladi (entitlement)

Ruxsat **to'rt manbadan** kelishi mumkin va tekshiruv **bitta joyda** —
`AccessService`. Klientga sochilmagan, aks holda mobil ilova, sayt va backend
uch xil javob berardi.

### Bosh sahifa — serverda quriladi (ТЗ §31)

Mobil ilova bosh sahifani **backenddan oladi**. Qaysi bo'limlar bor, ular
qanday tartibda va nima deb ataladi — hammasi bazada. Aks holda bo'lim
qo'shish yoki tartibini o'zgartirish uchun ilovaning yangi versiyasini
do'konga chiqarish kerak bo'lardi.

```
GET /api/v1/app/home?locale=UZ
```

| Bo'lim turi | Nima bilan to'ladi |
|---|---|
| `ADVERTISEMENT_CAROUSEL` | Faol bannerlar. Faol obunasi borlarga tijorat reklamasi ko'rsatilmaydi, admin e'loni hammaga |
| `NEW_PREMIERES` | Oynasi ochiq premyeralar |
| `CATEGORIES` | Faol kategoriyalar |
| `MINI_SERIES` · `PODCASTS` · `SHOWS` · `STREAMS` · `CLIPS` | Kontent TURI bo'yicha (kategoriya emas — ТЗ §13) |
| `REELS_SERIES` | Tik (`VERTICAL`) yo'nalishdagi kontent |
| `FEATURED_CONTENT` · `POPULAR_CONTENT` | `featured` / `popular` bayrog'i |
| `POPULAR_CREATORS` | Mashhur ijodkorlar (ТЗ §25) |
| `CUSTOM_ROW` | Admin qo'lda yiqqan qator — `HomepageSectionItem` |

**Qator tartibi ikki bosqichli.** Admin qatorga aniq ro'yxat bersa
(`PUT /homepage/sections/{id}/items`) — o'sha tartib ishlatiladi. Ro'yxat
bo'sh bo'lsa avtomatik qoida (yangi kontent birinchi). Shunda admin har bir
qatorni qo'lda to'ldirishga majbur emas, lekin xohlasa to'liq nazorat
qiladi.

**Bo'limlar tartibi bitta so'rovda** — `PUT /homepage/sections/order`.
Bittalab o'zgartirish oraliq holat yaratardi: ikkita bo'lim bir xil
raqamda qolib, o'sha lahzada foydalanuvchi aralashib ketgan bosh sahifani
ko'rardi.

**Bo'sh bo'lim javobga tushmaydi.** Elementi yo'q bo'lim klientda sarlavhasi
bor, ichi yo'q qator bo'lib chiqardi. Ma'lumot yo'q bo'lsa — bo'lim yo'q,
o'ylab topilgan element emas.

**Qatorlarda faqat `PUBLISHED` + `PUBLIC` kontent:** `UNLISTED` faqat havola
orqali ochiladi, `PRIVATE` esa xodimlarga.

---

```
GET /api/v1/app/watch/{episodeId}?locale=UZ      qism (EPISODIC / SEASONAL)
GET /api/v1/app/watch/content/{contentId}        SINGLE (film, klip, shou)
```

**Video qayerda saqlanadi:** ko'p qismli kontentda — qismga
(`EpisodeVideo`), SINGLE da — kontentga (`ContentMedia`, `role = VIDEO`).
Ikkalasida ham bir nechta segment bo'lishi mumkin (ТЗ §19).

Javob: ko'rish mumkinmi, mumkin bo'lmasa **nima qilish kerak** va narxlar.

| Holat | `reason` | `requiredAction` |
|---|---|---|
| Kontent bepul | `FREE` | — |
| Faol Premium obuna | `PREMIUM` | — |
| Shu qism sotib olingan | `EPISODE_PURCHASE` | — |
| Butun premyera sotib olingan | `PREMIERE_PURCHASE` | — |
| Tizimga kirmagan | `NOT_AUTHENTICATED` | `SIGN_IN` |
| To'lov kerak | `PAYMENT_REQUIRED` | `SUBSCRIBE` · `BUY_EPISODE` · `BUY_OR_SUBSCRIBE` |
| Foydalanuvchi bloklangan | `USER_BLOCKED` | — |
| Nashr qilinmagan | `NOT_PUBLISHED` | — |

> **Ruxsat bo'lmasa `sources` bo'sh qaytadi** — video havolasi hech qachon
> rad javobi bilan birga yuborilmaydi. Fayl endpointi ham mustaqil
> tekshiradi, ya'ni himoya ikki qavatli.

Alohida qoidalar:

- **Reklama** faqat Premium obunasi bo'lmaganlarga ko'rsatiladi.
- **Casting loyihasiga kirish**: bir martalik xarid huquq BERMAYDI, faqat
  faol Premium beradi (buyurtmachi talabi).
- Nashr qilinmagan kontentni sotib olgan odam ham ko'ra olmaydi.

---

## 8. Xavfsizlik

| Nima | Qanday |
|---|---|
| Autentifikatsiya | JWT (HS256), parollar BCrypt bilan. Google kirish ham bor |
| Avtorizatsiya | **Ikki qavat**: Spring Security bazaviy rol + endpoint darajasidagi ruxsat |
| Ruxsat tartibi | Ruxsat so'rov tanasi **tekshirilishidan oldin** ko'riladi (interceptor) |
| Rate limiting | Spring Security zanjiridan oldin, xotirada token-bucket |
| Audit | Barcha muhim amallar yoziladi; parol va token **yozilmaydi** |
| Fayl yo'llari | UUID nomlar, path traversal ikki marta tekshiriladi |
| Maxfiylik | Ochiq katalogda telefon, email, o'lchamlar chiqmaydi |

Rate limit qoidalari (daqiqasiga, IP bo'yicha):

| Endpoint | Limit |
|---|---|
| `POST /api/v1/app/analytics/events` | 60 |
| `POST /api/v1/auth/login` | 10 |
| `POST /api/v1/app/admin/auth/login` | 10 |
| `POST /api/v1/casting-user` | 20 |
| `POST /api/v1/file/upload` | 30 |

Limit oshsa **429** va `Retry-After` qaytadi.

> ⚠️ Rate limiter **xotirada** ishlaydi — bitta instansiya uchun. Bir nechta
> instansiya bo'lsa Redis kerak bo'ladi.

### Ochiq (tokensiz) endpointlar

Qolgan barcha `/api/**` yopiq. Ochiqlari aniq sanalgan:

```
POST /api/v1/auth/login · /auth/google · /auth/refresh
POST /api/v1/app/admin/auth/login
GET  /api/v1/news · /api/v1/news/{id}
GET  /api/v1/file/getFile/**          eski casting rasmlari
GET  /api/v1/app/media/{id}/raw           rasm ochiq, video entitlement bilan
GET  /api/v1/app/home                     bosh sahifa; token bo'lsa reklama filtrlanadi
GET  /api/v1/app/watch/{episodeId}        anonim ham so'ray oladi — javob "kiring"
GET  /api/v1/casting-user/web         shaxsiy ma'lumotsiz ro'yxat
POST /api/v1/casting-user             Telegram bot anketa yuboradi
GET  /api/v1/casting-user/my/** · /appeal/**
POST /api/v1/file/upload              bot anketa rasmini yuklaydi
POST /api/v1/app/analytics/events         reklama ko'rsatilishi anonim ham qayd etiladi
```

> ⚠️ `casting-user/my/**` va `/appeal/**` hozircha ochiq: Telegram bot oqimida
> autentifikatsiya yo'q. Yopish uchun bot jamoasi bilan kelishish kerak
> (`roadmap.md`, B2).

---

## 9. Admin API — to'liq ro'yxat

Har biri token talab qiladi. Ustunda — kerakli ruxsat.

### Kontent

| Metod | Yo'l | Ruxsat |
|---|---|---|
| GET | `/api/v1/app/admin/content` | `CONTENT_VIEW` |
| POST | `/api/v1/app/admin/content` | `CONTENT_CREATE` |
| GET | `/api/v1/app/admin/content/{id}` | `CONTENT_VIEW` |
| PUT | `/api/v1/app/admin/content/{id}` | `CONTENT_EDIT` |
| DELETE | `/api/v1/app/admin/content/{id}` | `CONTENT_DELETE` |
| GET | `/api/v1/app/admin/content/{id}/seasons` | `CONTENT_VIEW` |
| POST | `/api/v1/app/admin/content/{id}/seasons` | `CONTENT_CREATE` |
| PUT | `/api/v1/app/admin/content/{id}/seasons/{seasonId}` | `CONTENT_EDIT` |
| DELETE | `/api/v1/app/admin/content/{id}/seasons/{seasonId}` | `CONTENT_DELETE` |
| GET | `/api/v1/app/admin/content/{id}/episodes` | `CONTENT_VIEW` |
| POST | `/api/v1/app/admin/content/{id}/episodes` | `CONTENT_CREATE` |
| PUT | `/api/v1/app/admin/content/{id}/episodes/{episodeId}` | `CONTENT_EDIT` |
| DELETE | `/api/v1/app/admin/content/{id}/episodes/{episodeId}` | `CONTENT_DELETE` |

### Taksonomiya va ijodkorlar

| Metod | Yo'l | Ruxsat |
|---|---|---|
| GET · POST | `/api/v1/app/admin/categories` | `CATEGORY_VIEW` · `CATEGORY_CREATE` |
| PUT | `/api/v1/app/admin/categories/{id}` | `CATEGORY_EDIT` |
| GET · POST | `/api/v1/app/admin/genres` | `GENRE_VIEW` · `GENRE_CREATE` |
| PUT | `/api/v1/app/admin/genres/{id}` | `GENRE_EDIT` |
| GET · POST | `/api/v1/app/admin/creators` | `CREATOR_VIEW` · `CREATOR_CREATE` |
| PUT | `/api/v1/app/admin/creators/{id}` | `CREATOR_EDIT` |

### Media

| Metod | Yo'l | Ruxsat |
|---|---|---|
| GET | `/api/v1/app/admin/media` | `MEDIA_VIEW` |
| POST | `/api/v1/app/admin/media` | `MEDIA_UPLOAD` |
| GET | `/api/v1/app/admin/media/{id}/usage` | `MEDIA_VIEW` |
| DELETE | `/api/v1/app/admin/media/{id}` | `MEDIA_DELETE` |
| POST | `/api/v1/app/admin/uploads` | `MEDIA_UPLOAD` |
| PUT | `/api/v1/app/admin/uploads/{id}/chunks/{n}` | `MEDIA_UPLOAD` + egalik |
| GET · POST · DELETE | `/api/v1/app/admin/uploads/{id}[/complete]` | `MEDIA_UPLOAD` + egalik |

### Bosh sahifa, reklama, premyera

| Metod | Yo'l | Ruxsat |
|---|---|---|
| GET | `/api/v1/app/admin/homepage/sections` | `HOMEPAGE_VIEW` |
| GET | `/api/v1/app/admin/homepage/creators` | `HOMEPAGE_VIEW` |
| PUT | `/api/v1/app/admin/homepage/sections/{id}` | `HOMEPAGE_EDIT` |
| GET · POST | `/api/v1/app/admin/advertisements` | `ADVERTISEMENT_VIEW` · `_CREATE` |
| PUT · DELETE | `/api/v1/app/admin/advertisements/{id}` | `_EDIT` · `_DELETE` |
| GET · POST | `/api/v1/app/admin/premieres` | `PREMIERE_VIEW` · `_CREATE` |
| PUT · DELETE | `/api/v1/app/admin/premieres/{id}` | `_EDIT` · `_DELETE` |

### Muloqot

| Metod | Yo'l | Ruxsat |
|---|---|---|
| GET | `/api/v1/app/admin/comments` | `COMMENT_VIEW` |
| PUT | `/api/v1/app/admin/comments/{id}/status/{status}` | `COMMENT_MODERATE` |
| GET · POST · PUT | `/api/v1/app/admin/notifications[/{id}]` | `NOTIFICATION_VIEW` · `_CREATE` |
| POST | `/api/v1/app/admin/notifications/{id}/send` | `NOTIFICATION_SEND` |
| POST | `/api/v1/app/admin/notifications/{id}/cancel` | `NOTIFICATION_CREATE` |

### Foydalanuvchilar va monetizatsiya

| Metod | Yo'l | Ruxsat |
|---|---|---|
| GET | `/api/v1/app/admin/users[/{id}]` | `USER_VIEW` |
| POST | `/api/v1/app/admin/users/{id}/block` · `/unblock` | `USER_BLOCK` |
| POST · DELETE | `/api/v1/app/admin/users/{id}/premium` | `USER_PREMIUM_MANAGE` |
| GET | `/api/v1/app/admin/users/{id}/devices` | `USER_DEVICE_MANAGE` |
| DELETE | `/api/v1/app/admin/users/{id}/devices/{rowId}` | `USER_DEVICE_MANAGE` |
| GET | `/api/v1/app/admin/tariffs` | `TARIFF_VIEW` |
| POST · PUT | `/api/v1/app/admin/tariffs[/{id}]` | `TARIFF_EDIT` |
| GET | `/api/v1/app/admin/currency-packages` · `/donations/top` | `DONATION_VIEW` |
| POST · PUT · DELETE | `/api/v1/app/admin/currency-packages[/{id}]` | `DONATION_PACKAGE_EDIT` |

### Tizim

| Metod | Yo'l | Ruxsat |
|---|---|---|
| POST | `/api/v1/app/admin/auth/login` | — (ochiq) |
| GET | `/api/v1/app/admin/auth/me` | token |
| GET | `/api/v1/app/admin/dashboard/summary` | `CONTENT_VIEW` |
| GET | `/api/v1/app/admin/reports/overview` | `REPORT_VIEW` |
| GET | `/api/v1/app/admin/settings` | `SETTINGS_VIEW` |
| PUT | `/api/v1/app/admin/settings/{key}` | `SETTINGS_EDIT` |
| GET | `/api/v1/app/admin/staff` | rol ≥ ADMIN |
| POST | `/api/v1/app/admin/staff` | rol ≥ ADMIN + rol ierarxiyasi |
| PUT | `/api/v1/app/admin/staff/{id}/permissions` | rol ≥ ADMIN + o'zida bor ruxsat |
| PUT | `/api/v1/app/admin/staff/{id}/role` | rol ≥ ADMIN + ierarxiya |
| PUT | `/api/v1/app/admin/staff/{id}` | rol ≥ ADMIN |
| PUT | `/api/v1/app/admin/staff/{id}/password` | rol ≥ ADMIN |
| POST | `/api/v1/app/admin/staff/{id}/activate` · `/deactivate` | rol ≥ ADMIN |
| POST | `/api/v1/app/admin/staff/{id}/block` · `/unblock` | rol ≥ ADMIN |
| GET | `/api/v1/app/admin/audit-logs` | rol ≥ ADMIN |

---

## 10. Kundalik ish tartibi

**Yangi serial qo'shish.** Kontent muharriri yetti yorliqdan iborat:
**Asosiy · Matnlar · Media · Ijodkorlar · Fasl va qismlar · Monetizatsiya ·
Nashr**. «Fasl va qismlar» faqat tuzilma `SINGLE` dan farq qilganda
ko'rinadi — bitta filmda qism tushunchasi yo'q.

1. `/app/panel/media` — afishalar va videolarni yuklang (yoki muharrir ichidan).
2. `/app/panel/content` → **Yangi** → **Asosiy**: tur `SERIES`, tuzilma `SEASONAL`,
   kategoriya, yosh chegarasi.
3. **Matnlar**: uch tilda sarlavha va tavsif (o'zbekcha majburiy).
4. **Media**: afisha, muqova, galereya; kerak bo'lsa har til uchun alohida
   afisha.
5. **Ijodkorlar**: aktyor va rejissyorlarni biriktiring.
6. **Fasl va qismlar**: fasl → qism → video segmentlar.
7. **Monetizatsiya**: kirish siyosati va narx. Har bir qismga alohida narx
   yoki siyosat qo'yish mumkin — masalan birinchi qismni bepul qoldirish.
8. **Nashr**: tayyor bo'lgach holatni `PUBLISHED` ga o'tkazing.

**Xodim qo'shish:** `/app/panel/staff` → **Yangi**. Rol tanlang; WORKER bo'lsa
ruxsatlarni belgilang. O'zingizdan yuqori rol yarata olmaysiz.

**Premium sovg'a qilish:** `/app/panel/users` → foydalanuvchi → **Premium berish**,
oylar sonini kiriting. Sovg'a mavjud muddat **ustiga** qo'shiladi.

**Izohlarni moderatsiya qilish:** `/app/panel/comments`. Shikoyat qilinganlar
alohida filtrda.

---

## 11. Ma'lum cheklovlar

Bular ataylab shunday — soxta ishlayotgandek ko'rsatilmagan:

| Nima | Holat |
|---|---|
| **FCM push** | Kalit yo'q. Yuborishga urinilsa **503** qaytadi, urinish `FAILED` holatda saqlanadi. Soxta «yuborildi» yozilmaydi |
| **To'lov provayderlari** | Click/Payme/Uzum/Stripe ulanmagan. Store billing riski hal bo'lmagan |
| **Stars / Coin kursi** | Buyurtmachi aytmagan — paketlar narxi 0, donat daromadi `null` («sozlanmagan», taxmin emas) |
| **Dev videolari** | Haqiqiy video EMAS — 256 KB tasodifiy bayt. Yetkazish yo'lini sinash uchun yetarli, pleyerda o'ynamaydi |
| **Qidiruv** | `LIKE '%matn%'` — ma'lumot o'sganda sekinlashadi. PostgreSQL'da full-text yoki trigram indeks kerak bo'ladi |
| **Rate limiter** | Xotirada — ko'p instansiya uchun Redis kerak |

---

## 12. Testlar

```bash
./backend/mvnw -f backend/pom.xml package     # 294 test
cd frontend && CI=false npx react-scripts build
```

Muhim testlar:

| Test | Nimani qo'riqlaydi |
|---|---|
| `AdminEndpointGuardTest` | Qo'riqlanmagan admin endpoint qo'shib bo'lmaydi |
| `PaidContentLeakTest` | Pullik video sizib chiqmaydi |
| `AccessServiceTest` | Entitlement — 4 manbadan |
| `RbacIntegrationTest` | Rollar HTTP darajasida |
| `PremiumLifecycleTest` | Premium berish/tortib olish + kontentga ta'siri |
| `ChunkedUploadTest` | Bo'laklab yuklash, davom ettirish |
| `MediaRangeDeliveryTest` | Video `Range` (seek) |
| `ContentEditRoundTripTest` | Tahrirlash ma'lumot yo'qotmaydi |
| `ContentListPerformanceTest` | Sahifalash bazada kesiladi, N+1 yo'q |
| `PermissionBeforeValidationTest` | Ruxsat validatsiyadan oldin |
| `SecurityRulesTest` | Tokensiz hech narsa ochilmaydi |
| `OldCastingFrozenTest` | **Eski casting yo'l, entity va jadvallari o'zgarmaydi** |
| `HyperAdminHierarchyTest` | HYPER_ADMIN teng rol yarata olmaydi, o'ziga tegmaydi |
| `SuperAdminScopeTest` | SUPER_ADMIN doirasi — HyperAdmin yarata olmaydi |
| `StaffManagementTest` | Xodimlar ro'yxati, filtrlar, faolsizlantirish |
| `ContentClassificationTest` | **Tur ≠ kategoriya ≠ janr** — uch mustaqil o'lchov |
| `SingleContentWatchTest` | Film videosi — saqlash, tomosha, himoya |
| `ContentVisibilityTest` | `visibility` va `language` — status va tarjimadan alohida |
| `ThreeLanguageRuleTest` | **Uch til majburiyligi** — nashrda tekshiriladi |
| `AccessPricingTest` | Narx sozlamadan olinadi, kodda qotirilmagan |
| `CreatorModuleTest` | Ijodkor bir kinoda aktyor, boshqasida rejissyor |
| `FeaturedCreatorsTest` | «Mashhur ijodkorlar» — qo'lda va analitika tartibi |
| `MediaLibraryTest` | Media kutubxonasi — qidiruv, filtr, arxivlash |
| `AdvertisementModuleTest` | Reklama — tugma va havola ixtiyoriyligi |
| `InternalLinkReuseTest` | Havola mexanizmi reklama, premyera va bildirishnomada bir xil |
| `BackendAuthorizationTest` | Ikki qavatli avtorizatsiya, huquq oshirish urinishlari |
| `BootstrapAccountSecurityTest` | **Standart parolli master hisob qaytmasin** |
| `ExistingCastingRegressionTest` | **Casting oqimi buzilmaydi** — anketa, qabul/rad, bot xabari |

Bir nechtasi **mutatsiya bilan tekshirilgan**: kod ataylab buzib ko'rilgan va
testlar buni ushlagan — ya'ni ular bekorga o'tmayapti.
