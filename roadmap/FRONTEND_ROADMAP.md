# UZCASTING FRONTEND ROADMAP

> Admin web panel uchun checklist.
> Root [roadmap.md](./roadmap.md) · Arxitektura → [FRONTEND_ARCHITECTURE.md](./FRONTEND_ARCHITECTURE.md)

Status: `[ ]` TODO · `[~]` IN PROGRESS · `[x]` DONE · `[!]` BLOKLANGAN

**Oxirgi to'liq tekshiruv: 24.08.2026** — har bir band kod bilan
solishtirildi.

---

## Hozirgi holat

| | Natija |
|---|---|
| Panel sahifalari | 23 ta `pages/*.jsx` + `editor/`, `staff/`, `homepage/`, `reports/` bo'limlari |
| API metodlari | `client.js` da **105 ta** |
| Frontend testlari | **44 ta**, 9 to'plam — hammasi yashil |
| Backend testlari | **767 ta** — hammasi yashil |
| Build | `react-scripts build` ✅ — `adminpanel` da ogohlantirish **yo'q** |
| Backend qamrovi | 74 endpointdan **73 tasi** ishlatiladi |

⚠️ **Bu fayl 24.08 da tuzatildi.** Ilgari unda **yolg'on da'volar** bor
edi: PHASE 2–8 `[ ]` deb belgilangan, holbuki ular bajarilgan; «bitta
foydalanuvchi sahifasi» va «charts/tables» bajarilgan deb yozilgan,
holbuki panel ularni chaqirmasdi. Endi har bir band kod bilan
tasdiqlangan.

---

## Backend ↔ Frontend moslik jadvali

Bu jadval ТЗ ning asosiy talabi: **backendda yozilgan har bir imkoniyat
panelda ko'rinsin.**

| Backend endpoint | Panel | Holat |
|---|---|---|
| `POST /auth/login` · `/refresh` · `/logout` · `GET /me` | `LoginPage`, `AuthContext` | `[x]` |
| `GET /dashboard/summary` · `/charts` · `/tables` | `DashboardPage` | `[x]` |
| `GET/POST/PUT/DELETE /content` | `ContentPage`, `ContentEditor` | `[x]` |
| `/content/{id}/seasons` · `/episodes` (CRUD) | `EpisodesTab` | `[x]` |
| `/categories` · `/genres` (CRUD) | `TaxonomyPage`, `TaxonomyForm` | `[x]` |
| `/creators` (CRUD) | `CreatorsPage`, `CreatorForm`, `CreatorQuickCreate` | `[x]` |
| `/media` · `/archive` · `/restore` · `/usage` · `DELETE` | `MediaPage` | `[x]` |
| `/uploads` (bo'laklab yuklash) | `client.js` → `uploadFile` | `[x]` |
| `/advertisements` (CRUD) · `/{id}/statistics` | `BannerPage`, `reports/AdStatsModal` | `[x]` |
| `/premieres` (CRUD) | `BannerPage` | `[x]` |
| `/homepage/sections` · `/order` · `/{id}/items` · `/creators` | `HomepagePage`, `homepage/SectionItemsModal`, `CreatorsPreviewModal` | `[x]` |
| `/comments` · `/{id}/status` | `CommentsPage` | `[x]` |
| `/notifications` (CRUD) · `/send` · `/cancel` · `/report` · `/audience` | `NotificationsPage`, `reports/NotificationReportModal` | `[x]` |
| `/users` · `/{id}` · `/block` · `/premium` · `/devices` | `UsersPage`, `UserDetailPage` | `[x]` |
| `/staff` (CRUD) · `/role` · `/permissions` · `/password` · `/activate` · `/block` | `staff/StaffPage`, `StaffForm`, `PermissionPicker` | `[x]` |
| `/subscriptions` | `SubscriptionsPage` | `[x]` |
| `/tariffs` · `/currency-packages` | `TariffsPage` | `[x]` |
| `/donations/report` · `/transactions` | `DonationsPage` | `[x]` |
| `/settings` | `SettingsPage` | `[x]` |
| `/reports/overview` · `/content/{id}/statistics` | `ReportsPage`, `reports/ContentStatsModal`, `ReportFilters` | `[x]` |
| `/audit-logs` | `AuditPage` | `[x]` |
| `/api/v1/casting-user/web` (eski modul) | `CastingPage` | `[x]` |
| `GET /uploads/{id}` (yuklash holati) | `client.js` → `findResumable` | `[x]` |
| `DELETE /uploads/{id}` (bekor qilish) | `client.js` → `cancelUpload` | `[x]` |
| `GET /donations/top` | — | `[!]` **ataylab ishlatilmaydi** — pastga qarang |

---

## Uchta band — bajarildi (24.08.2026)

### 1. Donat kimga berilgani `[x]`

⚠️ **Dastlabki tashxis noto'g'ri edi.** «`GET /donations/top` ishlatilmaydi»
deb yozgandim, lekin tekshirsam `/donations/report` **allaqachon** shu
ma'lumotni beradi (`topCreators`, `topContent`) va sahifa uni
ko'rsatadi. Ikkinchi chaqiruv qo'shish §103 ogohlantirgan «duplicate
API calls» bo'lardi.

**Haqiqiy nuqson boshqa joyda edi:** donat **kimga** berilgani hech
qayerda ko'rinmasdi — hamma joyda `#5`. Asimmetriya buni e'tibordan
chetda qolgan deb ko'rsatadi: yuboruvchining ismi qaytarilardi
(`senderName`), oluvchiniki esa yo'q.

- `[x]` `DonationTargetNames` — ijodkor ismi yoki kontent sarlavhasi
- `[x]` Nomlar **to'plam bo'lib** yuklanadi, bittalab emas (§66)
- `[x]` Topilmasa `null` → panel `#5` ko'rsatadi. O'chirilgan
  ijodkorga berilgan eski donat shu holatda bo'ladi va nom **to'qib
  chiqarilmaydi** (§45)

`GET /donations/top` shu sababdan ishlatilmaydi va bu **qaror**, kamchilik
emas.

### 2. Yuklashni davom ettirish `[x]`

Bo'laklab yuklash bir necha daqiqa davom etadi. Server seansni saqlab
turadi va qaysi bo'laklar yetganini aytadi, lekin klient `uploadId` ni
unutsa — bir gigabaytlik video **boshidan** yuklanardi.

- `[x]` `uploadId` `localStorage` da, fayl imzosi bo'yicha
  (nom + o'lcham + o'zgartirilgan vaqt)
- `[x]` Sahifa yangilangach yarim qolgan seans topiladi va davom etadi
- `[x]` Server seansni unutgan bo'lsa — toza boshlanadi, eski yozuv
  o'chiriladi
- `[x]` Tugagach seans esdan chiqariladi (tugagan seansga bo'lak
  yuborish 404 berardi)

⚠️ Bu maxfiy ma'lumot emas — shunchaki seans identifikatori. Ruxsat
baribir serverda tekshiriladi.

### 3. Yuklashni bekor qilish `[x]`

- `[x]` `cancelUpload(file, uploadId)` → `DELETE /uploads/{id}`
- `[x]` Server xato bersa ham klient seansni **unutadi** — u baribir
  yaroqsiz
- `[x]` `signal` bilan oqim to'xtatiladi: bekor qilingandan keyin
  keyingi bo'lak yuborilmaydi

⚠️ Chaqirilmasa server bo'laklari diskda qolib ketardi.

---

## Qolgan ish

- `[ ]` Ro'yxatlarda ustun bo'yicha **saralash** (hozir filtr va
  sahifalash bor)
- `[!]` **Xaridlar tarixi** — frontend ishi emas: `Purchase` entity bor,
  lekin `GET /users/{id}/purchases` admin endpointi **yozilmagan**

---

## Bosqichlar — tuzatilgan holat

| Bosqich | Holat | Izoh |
|---|---|---|
| 0. Mavjud React auditi | `[x]` | 24.08 da qayta tekshirildi |
| 1. Core | `[x]` | ⚠️ «Token refresh yo'q» yozuvi eskirgan edi — §61 da qo'shilgan |
| 2. Staff Management | `[x]` | `pages/staff/` — forma, ruxsat tanlash, 8 amal |
| 3. CMS Foundation | `[x]` | kategoriya, janr, ijodkor, media |
| 4. Content | `[x]` | 6 bo'limli muharrir, fasl/qism, galereya |
| 5. Homepage / Ads / Premieres | `[x]` | + qo'lda tanlash va tartiblash |
| 6. Engagement | `[x]` | + bildirishnoma hisoboti |
| 7. Users & Monetization | `[x]` | + `UserDetailPage`, `SubscriptionsPage` |
| 8. Analytics | `[x]` | + grafiklar, jadvallar, reklama/kontent statistikasi |
| 9. Hardening | `[~]` | quyida |

### 9. Hardening — qolgan bandlar

- `[x]` Katta video yuklash — bo'laklab, qayta urinish bilan (§16)
- `[x]` Qidiruv debounce — `SearchInput` da 400 ms (§96)
- `[x]` Accessibility — modal fokus tuzoq'i, `aria-*`, `htmlFor`,
  `prefers-reduced-motion` (§97)
- `[x]` Responsive — 1024px da yon menyu sirg'aluvchi panel, jadval
  gorizontal siljiydi (§98)
- `[x]` Kritik oqim testlari — **38 ta**, 8 to'plam (§86)
- `[x]` Filtr o'zgarganda sahifa boshiga qaytadi — test bilan
  qo'riqlanadi (§72)
- `[x]` Pul va sanoq formatlash markazlashtirilgan (§103, §104)
- `[ ]` Barcha ro'yxatlarda **saralash** (`sorting`) — hozir faqat
  filtr va sahifalash bor

---

## Testlar `[x]` — ТЗ §86

| Fayl | Nimani qamraydi |
|---|---|
| `api/__tests__/refreshFlow.test.js` | 401 da yangilash: qayta urinish, bitta yangilash, cheksiz halqa yo'qligi, `localStorage` ga yozilmasligi |
| `auth/__tests__/guards.test.jsx` | Ruxsatli/ruxsatsiz sahifa, 403, rol darajasi, sessiya tiklanishi |
| `pages/__tests__/contentFlow.test.jsx` | Kontent yaratish va tahrirlash |
| `pages/__tests__/episodeFlow.test.jsx` | Qism qo'shish |
| `pages/__tests__/creatorFlow.test.jsx` | Ijodkor yaratish |
| `pages/__tests__/advertisementFlow.test.jsx` | Reklama yaratish |
| `pages/staff/__tests__/permissionGroups.test.js` | Ruxsat guruhlari |
| `utils/__tests__/format.test.js` | Pul va sanoq — `null` va `0` farqi |

**Stack:** CRA jest + `@testing-library/react` (§86 uchun ataylab
qo'shilgan). `src/setupTests.js` — usiz `toBeInTheDocument` matcheri
mavjud bo'lmaydi.

---

## Umumiy komponentlar `[x]` — ТЗ §72

| ТЗ so'ragan | Loyihada |
|---|---|
| `Pagination`, `SearchInput`, `PageHeader`, `StatusBadge`, `Badge` | `components/Ui.jsx` |
| `EmptyState`, `ErrorState`, `LoadingState`, `ForbiddenState` | `components/States.jsx` |
| `ConfirmDialog` | `components/ConfirmDialog.jsx` + `useConfirm` |
| `MediaUploader`, `ImagePicker`, `VideoUploader` | `MediaField`, `MediaPicker`, `GalleryField` |
| `PermissionGuard` | `auth/Guards.jsx` — `RequirePermission` |
| `DataTable` | ⚠️ **ataylab yaratilmadi** — ustunlar sahifadan sahifaga juda farq qiladi, bitta komponentga tiqish §72 ogohlantirgan «haddan tashqari abstraction» bo'lardi |
| `FilterPanel` | ⚠️ ataylab yaratilmadi — filtrlar sahifaga xos |

### ⚠️ Bir komponent ikki marshrutda — `key` MAJBURIY

`TaxonomyPage` (kategoriya/janr) va `BannerPage` (reklama/premyera)
bitta komponentni ikki marshrutda ishlatadi. React ularni bir xil tur
deb hisoblab **holatni saqlab qoladi**: kategoriyani tahrirlash oynasi
ochiq turganda «Janrlar» ga o'tilsa, saqlash kategoriya ma'lumotini
**janr ustiga yozardi**.

`key` qo'yilgan va bu test bilan qo'riqlanadi
(`PanelUiRequirementsTest.sharedComponentRoutesAreKeyed`).

---

## Pul va sanoq formatlash `[x]` — ТЗ §103, §104

`utils/format.js` — `money()` va `count()`.

| Funksiya | `null` | `0` | Nega |
|---|---|---|---|
| `money()` | `—` | `0` | Sovg'a obunada to'lov `null` — «sotilmagan». Uni «0 so'm» deb ko'rsatish «bepul sotildi» degan boshqa ma'no |
| `count()` | `0` | `0` | Sanoqda nol haqiqiy: «hech kim ko'rmagan» ham ma'lumot |

`toLocaleString` panelda **hech qayerda to'g'ridan-to'g'ri
chaqirilmaydi**.

---

## Keyingi ish

Panel backendni deyarli to'liq qamragan. Qolgan uchta band yuqorida
(«Yetishmayotgan uchta narsa») va ular kichik.

⚠️ **Foydalanuvchi (USER) qismi hozir boshlanmaydi** — buyurtmachi
tartibi bo'yicha u eng oxirgi. Bundan tashqari **backend tayyor emas**:
`/app/auth/**` (OTP kirish), `/app/catalog`, `/app/content/{slug}`,
`/app/search`, `/app/me`, `/app/purchases`, `/app/comments` yozilmagan.

---

## Tarix — brauzerda sinalgan qaydlar

<details>
<summary>19–20.08.2026 sinov qaydlari</summary>

## 11. Brauzerda tekshirilgan (19.08.2026)

Chrome, `http://localhost:3000/panel`, backend `dev` profilida.

| Nima | Natija |
|---|---|
| 5 roldan kirish | ✅ HYPER/SUPER/ADMIN/WORKER×2 |
| USER admin panelga kira olmasligi | ✅ `ACCESS_DENIED` |
| Menyu ruxsatga qarab filtrlanishi | ✅ cheklangan WORKER'da «Janrlar» va «Xodimlar» yo'q |
| Manzilni qo'lda kiritish (`/app/panel/staff`) | ✅ frontend 403 sahifasi, backend `403 ACCESS_DENIED` |
| Til almashish (UZ/RU/EN) | ✅ interfeys ham, kontent sarlavhalari ham |
| Ruscha qidiruv | ✅ «сердц» → «Хозяин моего сердца» topildi |
| Tilga xos afisha | ✅ RU uchun alohida afisha va belgisi ko'rindi |
| Valyuta tarjimasi | ✅ so'm / сум / UZS |
| Dashboard | ✅ real raqamlar; modul yo'qlarida «—» va izoh |

### Yo'l-yo'lakay tuzatilgan xatolar

1. **Admin login bloklangan edi** — `/api/v1/app/admin/auth/login` `permitAll` ro'yxatiga
   kirmagan. Regressiya testi qo'shildi.
2. **Chrome autofill** input'larni oq qilib qo'yardi → `:-webkit-autofill` bosildi.
3. **Serif shrift** — `pages/home/home.css` dagi `* { font-family: Lora !important }`
   butun saytga sizadi. Panel qoidasiga ustunlik berildi.
4. **Yon panel kontentni surib yuborardi** — `.uz-panel > * { position: relative }`
   Tailwind'ning `.fixed` klassini bosgan. Joylashuv aniq berildi.
5. **Menyu tugmasi desktopda ko'rinardi** — `.uz-btn` Tailwind `lg:hidden` ni bosgan.
   O'z media so'rovimiz yozildi.

### Tekshirilmagan

⚠️ **Responsivlik** — bu muhitda brauzer viewport'i 1512px da qotib qolgan
(`resize_window` ta'sir qilmadi), shuning uchun tor ekran haqiqiy sinovdan
o'tmadi. CSS media so'rovlari yozilgan va ko'zdan kechirilgan, lekin qurilmada
tasdiqlanishi kerak.


---

## 12. CRUD brauzerda sinaldi (19.08.2026)

| Oqim | Natija |
|---|---|
| Kontent yaratish — 3 til, janr, muharrir orqali | ✅ slug avtomatik: `brauzer-sinovi` |
| Kontent tahrirlash — UZ o'zgardi | ✅ RU/EN saqlanib qoldi, slug o'zgarmadi |
| Statusni PUBLISHED qilish | ✅ audit'ga `CONTENT_PUBLISHED` tushdi |
| Kategoriya yaratish — 3 til | ✅ slug `talim-va-kurslar` (apostrof tashlandi) |
| Ijodkor yaratish — 3 til | ✅ `displayName` ism+familiyadan yig'ildi |
| Tarjimasiz til belgisi | ✅ tab'da qizil nuqta, majburiy maydonda xato |
| Ruxsat nazorati | ✅ `CONTENT_CREATE` yo'q worker → 403 |

### Sinov davomida topilgan va tuzatilgan 3 ta bug

1. **`EnumSet.copyOf` bo'sh to'plamda yiqilardi** — ruxsatsiz Worker yaratib bo'lmasdi. Unit test topdi.
2. **Tarjima yangilanganda `UNIQUE(parent, locale)` buzilardi** — `clear()`+`add` o'rniga joyida yangilash. Brauzer sinovi topdi.
3. **Tahrirlashda slug jim o'zgarardi** — havolalar sinardi. API sinovi topdi.


---

## 13. Fasl/qism muharriri brauzerda sinaldi (20.08.2026)

| Oqim | Natija |
|---|---|
| SEASONAL serial — fasllar va ichidagi qismlar | ✅ 2 fasl, 5 qism ko'rindi |
| «BEPUL» vs «PREMIUM OR PURCHASE» bejaklari | ✅ override va meros farqlanadi |
| Yangi qism qo'shish | ✅ fasl oldindan tanlangan, raqam avtomatik 4 |
| «Kontentdan meros (PREMIUM OR PURCHASE)» varianti | ✅ kontent siyosati nomi bilan ko'rsatildi |
| Uch tilni to'ldirish | ✅ qizil nuqtalar yo'qoldi |
| Saqlash → ro'yxatga qaytish | ✅ yangi qism darhol ko'rindi, sanoq 4 ga o'zgardi |
| EPISODIC mini-serial | ✅ faslsiz tekis ro'yxat |
| SINGLE film | ✅ bo'lim umuman yo'q (6 tab, 7 emas) |


---

## 14. PHASE 5 brauzerda sinaldi (20.08.2026)

| Oqim | Natija |
|---|---|
| Bosh sahifa bo'limlari | ✅ 12 ta, avtomatik yaratildi |
| «Mashhur ijodkorlar» tartibi | ✅ 999 — eng pastda |
| Bo'limni o'chirish/yoqish (toggle) | ✅ bazada saqlandi, qator xiralashdi |
| Reklama ro'yxati | ✅ auditoriya bejaklari, «Efirda / Efirda emas» |
| Vaqt oynasi | ✅ 2027-yilda boshlanadigan banner «Efirda emas» |
| Premyera ro'yxati | ✅ bog'langan kontent, treyler |
| Til almashish | ✅ sarlavha tarjima bo'ldi, ichki nom o'zgarmadi |


---

## 15. PHASE 6–7 sahifalari (20.08.2026)

Qo'shilgan: `/app/panel/comments`, `/app/panel/notifications`, `/app/panel/users`,
`/app/panel/tariffs`, `/app/panel/settings`, `/app/panel/audit`.

Menyu uchta yangi guruhga bo'lindi: **Muloqot** (izohlar, bildirishnomalar),
**Foydalanuvchilar** (foydalanuvchilar, tariflar), **Tizim** (xodimlar,
sozlamalar, audit).

### Ikkita joyda holat ochiq aytiladi

1. **Bildirishnomalar** — sariq ogohlantirish: FCM ulanmagan, xabarlar
   saqlanadi va rejalashtiriladi, lekin YUBORILMAYDI. Soxta statistika yo'q.
2. **Valyuta paketlari** — kurs buyurtmachi tomonidan aytilmagan, narxlar 0.
   Bu «sozlanmagan» degani, taxminiy raqam emas.

### ⚠️ Brauzerda tekshirilmadi

Bu sahifalar API darajasida to'liq sinaldi (izoh moderatsiyasi, shikoyat
filtri, premium sovg'a/tortib olish, bloklash, tarif seed'i, donat reytingi,
sozlamalar), lekin **brauzerda ko'z bilan tekshirilmadi** — sessiya oxirida
Chrome kengaytmasi uzilib qoldi. Oldingi sahifalar bir xil komponentlardan
qurilgani uchun risk past, lekin tasdiqlash kerak.

---

## 16. Katta video yuklash (20.08.2026)

`api/client.js` dagi `uploadFile` endi o'lchamga qarab **o'zi tanlaydi**:

| Fayl | Usul |
|---|---|
| ≤ 8 MB | bitta `multipart` so'rov (avvalgidek) |
| > 8 MB | bo'laklab yuklash, 5 MB'lik bo'laklar |

Chaqiruvchi kod **umuman o'zgarmadi** — `MediaPicker` bir xil
`adminApi.uploadMedia(file, folder, setProgress)` ni chaqiradi va progress
bari ikkala holatda ham ishlaydi.

### Nega kerak edi

Serverda `multipart` chegarasi 50 MB. Epizod videosi bunga sig'masdi, ya'ni
**panel orqali video yuklab bo'lmasdi**.

### Bo'laklab yuklashda nima qilinadi

- Har bir bo'lak alohida `PUT` — bittasi uzilsa faqat o'sha qayta yuboriladi.
- Har bo'lak uchun **3 martagacha qayta urinish**, lekin faqat tarmoq va
  server (5xx) xatolarida. 4xx — bu serverning ongli «yo'q» javobi, uni
  qayta urinish bilan yengib bo'lmaydi va bekorga vaqt ketardi.
- Bo'lak so'rovi uchun alohida, uzoqroq kutish (120 s) — 5 MB sekin
  internetda standart 20 soniyaga sig'maydi.
- Server allaqachon qabul qilgan bo'laklarni aytadi, ular o'tkazib
  yuboriladi (davom ettirish).
- Progress bo'laklar bo'yicha 99% gacha, 100% esa yig'ish tugagach —
  aks holda bar to'lgan holda foydalanuvchi kutib qolardi.

---


</details>
