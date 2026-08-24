# UZCASTING FRONTEND ROADMAP

> Admin web panel uchun batafsil checklist.
> Root [roadmap.md](./roadmap.md) · Arxitektura → [FRONTEND_ARCHITECTURE.md](./FRONTEND_ARCHITECTURE.md)

Status: `[ ]` TODO · `[~]` IN PROGRESS · `[x]` DONE · `[!]` BLOCKED

Oxirgi yangilanish: 20.08.2026 — PHASE 5–8 qo'shildi

---

## 0. Existing React Audit `[x]`

- `[x]` React 18.3.1 + CRA 5 (`react-scripts`), **JavaScript** — TypeScript emas
- `[x]` Routing: `react-router-dom` v6, barcha route `src/App.js` ichida
- `[x]` HTTP: `src/config/index.js` — axios wrapper, `{error, data}` qaytaradi
- `[x]` Server-state kutubxonasi **yo'q** (TanStack Query yo'q)
- `[x]` UI: Bootstrap 5 + PrimeReact + Tailwind 3.4 + FontAwesome + react-icons (aralash)
- `[x]` i18n: `i18next` + `react-i18next` mavjud (`src/i18next.js`)
- `[x]` Token: `localStorage.access_token`
- `[x]` Baseline `react-scripts build` ✅ (≈40 ta ESLint warning bilan)

### Mavjud sahifalar

| Route | Papka | Tavsif |
|---|---|---|
| `/` | `pages/home` | Public landing |
| `/models` | `pages/models` | Anketa katalogi, filtrlar bilan |
| `/aadmin/login` | `admin/LoginAdmin.js` | Sayt admin login |
| `/aadmin/casting-users/*` | `admin/admin/` | Casting anketalari admini |
| `/admin/*` | `bot-admin/admin/` | Telegram bot admini |
| `/bot/:userId`, `/data-form/:userId`, `/history/:userId`, `/appeal/:userId` | `pages/` | Bot WebApp oqimi |

### Aniqlangan muammolar

- **B4 (HIGH):** `App.js:31` — `blockedPages = ["/dashboard"]`, bunday route yo'q.
  Haqiqiy admin route'lar `/aadmin/*`. **Guard hech qachon ishlamaydi.**
- Barcha route bitta faylda — kengaytirish qiyin
- Uchta UI kutubxona parallel — dizayn izchilligi yo'q
- Reusable table/pagination/filter komponentlari yo'q
- ~40 ta ESLint warning (pre-existing)

---

## 1. PHASE 1 — Core `[~]`

- `[x]` Audit va hujjatlar
- `[x]` `src/adminpanel/` papkasi — mavjud `admin/` va `bot-admin/` TEGILMADI
- `[x]` Marshrutlar `adminpanel/PanelApp.jsx` da; `App.js` ga bitta qator qo'shildi
- `[x]` `AdminLayout` — yon menyu + yuqori panel + `<Outlet />`
- `[x]` `AuthContext` — token, user, rol, ruxsatlar; sahifa yangilanganda `/me` bilan tiklanadi
- `[x]` `RequireAuth` / `RequirePermission`
- `[x]` 403 sahifasi (`ForbiddenState`)
- `[x]` API qatlami: `adminpanel/api/client.js` — 401 bitta joyda ushlanadi
- `[x]` Design token'lar (`theme/panel.css`) — **to'q ko'k**, hex faqat shu faylda
- `[x]` `i18n.js` — 100+ kalit, UZ/RU/EN
- `[x]` `useApi` hook — loading/error/reload
- `[ ]` Token refresh oqimi (hozir 401 da chiqariladi)

---

## 2. PHASE 2 — Staff Management `[x]`

- `[x]` `/app/panel/staff` — ro'yxat: ism, telefon, rol, ruxsatlar soni
- `[x]` Faqat o'zidan quyi rollar ko'rinadi (backendda filtrlanadi)
- `[x]` Avatar, createdBy, lastLoginAt ustunlari
- `[x]` Filter: role, active/inactive, qidiruv, sana
- `[x]` Create/Edit modal — role tanlash yaratuvchi huquqiga qarab cheklanadi
- `[x]` Worker uchun permission tanlash UI (checkbox guruhlari)
- `[x]` Activate / deactivate / reset password / block
- `[x]` Confirmation dialog barcha xavfli action'larga

---

## 3. PHASE 3 — CMS Foundation `[ ]`

- `[x]` `/app/panel/categories` — ro'yxat, **uchala tarjima ustunda** (yetishmasa qizil «—»)
- `[x]` `/app/panel/genres` — ro'yxat, uchala tarjima
- `[x]` `/app/panel/creators` — kartochkalar, foto/cover, featured bejagi, Stars soni, qidiruv
- `[x]` `/app/panel/media` — ro'yxat, oldindan ko'rish, sahifalash
- `[x]` **CRUD formalar** — `TaxonomyForm` (kategoriya+janr), `CreatorForm`
- `[x]` **`MediaPicker`** — kutubxonadan tanlash yoki shu yerda yuklash, progress bar
- `[x]` **`MediaField`** — oldindan ko'rish + almashtirish + olib tashlash
- `[x]` `LocaleTabs` — to'ldirilmagan til qizil nuqta bilan belgilanadi
- `[x]` `Modal` — Escape bilan yopiladi, fon siljimaydi

---

## 4. PHASE 4 — Content `[ ]`

- `[x]` `/app/panel/content` — ro'yxat: afisha, sarlavha (tanlangan tilda), tur, format,
  status, kirish siyosati + narx, ko'rishlar
- `[x]` Filtrlar: status, tur, qidiruv (debounce bilan)
- `[x]` **Tilga xos afisha** ko'rsatiladi va belgilanadi
- `[x]` Sahifalash

**`ContentEditor` — 6 bo'limli, bitta 100 inputli forma EMAS (§22, §53):**

- `[x]` Asosiy — tur, tuzilish, **format (Yonlama/Reels)**, kategoriya, janr chiplari, yosh, davomiylik
- `[x]` Matnlar — **UZ/RU/EN tab'lari**, to'ldirilmagan til belgilanadi
- `[x]` Media — umumiy afisha, muqova + **har bir til uchun alohida afisha**
- `[x]` Ijodkorlar — qidiruv, qo'shish, rol va qahramon ismi
- `[x]` Monetizatsiya — kirish siyosati, premyera narxi
- `[x]` Nashr — status, sana, slug, featured/popular
- `[x]` Saqlanmagan o'zgarish haqida ogohlantirish
- `[x]` `CONTENT_PUBLISH` ruxsati yo'q bo'lsa PUBLISHED tanlanmaydi
- `[x]` **Fasl va qismlar** — `EpisodesTab`, tuzilishga qarab moslashadi
- `[ ]` Galereya rasmlarini boshqarish

**`EpisodesTab` — tuzilishga qarab uch xil ishlaydi:**

- `[x]` SEASONAL — fasllar, har birining ichida qismlari; fasl qo'shish/tahrirlash/o'chirish
- `[x]` EPISODIC — faslsiz tekis qismlar ro'yxati
- `[x]` SINGLE — bo'lim umuman ko'rsatilmaydi
- `[x]` Qism formasi: 3 til, kadr, davomiylik, holat, kirish siyosati, narx
- `[x]` **Video segmentlar** — qo'shish/o'chirish, segment raqami, dublyaj tili
- `[x]` Kirish siyosatida «Kontentdan meros (...)» varianti — meros aniq ko'rinadi
- `[x]` Qism raqami avtomatik hisoblanadi (mavjudlaridan keyingisi)
- `[x]` Modal ichida modal yo'q — ro'yxat ↔ forma almashadi, «qaytish» tugmasi bilan
- `[x]` Qismlar bo'limida ikkinchi «Saqlash» ko'rsatilmaydi — chalkashlik bo'lmasin

---

## 5. PHASE 5 — Homepage / Ads / Premieres `[ ]`

- `[x]` `/app/panel/homepage` — 12 bo'lim, **ro'yxatning o'zida toggle**, tartib, element soni
- `[x]` Uchala tarjima ustunda — yetishmagani qizil «—» bilan
- `[x]` `/app/panel/ads` — CRUD, rasm + mobileImage, tugma on/off, havola turi
- `[x]` Ad audience bejagi: «Reklama» (oltin) / «Admin e'loni» (ko'k)
- `[x]` «Efirda / Efirda emas» — vaqt oynasi hisobga olinadi
- `[x]` `/app/panel/premieres` — CRUD, treyler, subtitle, bog'langan kontent
- `[x]` `LinkFields` — reklama va premyera uchun UMUMIY havola tahrirlagichi
- `[x]` `BannerPage` — ikkalasi bitta komponentda (maydonlari deyarli bir xil)

---

## 6. PHASE 6 — Engagement `[ ]`

- `[x]` `/app/panel/comments` — moderatsiya: yashirish, tiklash, o'chirilgan deb belgilash
- `[x]` Filtrlar: status, matn qidiruv, «faqat shikoyat qilinganlar»
- `[x]` Shikoyat soni qizil bejak bilan
- `[x]` `/app/panel/notifications` — CRUD, rejalashtirish, auditoriya, havola
- `[x]` **Provayder ulanmagani sahifada ochiq yozilgan** — sariq ogohlantirish
- `[x]` Yiqilgan yuborish sababi qatorda ko'rinadi
- `[ ]` Notification report — provayder ulangandan keyin

---

## 7. PHASE 7 — Users & Monetization `[ ]`

- `[x]` `/app/panel/users` — ro'yxat, qidiruv, block/unblock (sabab bilan)
- `[x]` Har bir qatorda premium, balans (pul + ⭐ + ◎), qurilmalar soni
- `[x]` Qurilmalar modali — ro'yxat va «chiqarib yuborish»
- `[x]` Premium sovg'a qilish — modal, muddat ustiga qo'shilishi tushuntirilgan
- `[x]` Premium tortib olish — tasdiq bilan
- `[x]` `/app/panel/tariffs` — 4 tarif, narx va imkoniyatlarni tahrirlash
- `[x]` «Oyiga» ustuni avtomatik hisoblanadi
- `[x]` Valyuta paketlari — narxni joyida tahrirlash
- `[x]` **Kurs aytilmagani ochiq yozilgan** — 0 «sozlanmagan» degani
- `[x]` Bitta foydalanuvchi sahifasi — profil, balans, premium, qurilmalar, obuna tarixi
- `[x]` Donation report ekrani — `DonationsPage.jsx` (bu qator eskirgan edi, ekran allaqachon bor)
- `[ ]` Xaridlar tarixi

---

## 8. PHASE 8 — Analytics `[ ]`

- `[x]` `/app/panel` — dashboard: 10 ta real ko'rsatkich; modul yo'q bo'lganlar ochiq «—» bilan
- `[x]` **Bitta** `dashboard/summary` chaqiruvi (§73)
- `[x]` Charts: user growth, views, subscription revenue, donations
- `[x]` Tables: latest content, top content, latest users, best ads, top creators
- `[x]` `/app/panel/reports` — filter: today / yesterday / 7 / 30 / custom + content/category/creator/tariff/ad (§47)
- `[!]` Ma'lumot yo'q bo'lsa **empty state**, fake grafik emas

---

## 9. PHASE 9 — Hardening `[~]`

- `[x]` Katta video yuklash — bo'laklab, qayta urinish bilan (§16)
- `[ ]` Barcha list sahifalarda: search, filter, pagination, sorting, loading, empty, error, retry
- `[x]` Search debounce — `SearchInput` da 400 ms (§96)
- `[ ]` Accessibility: button label, keyboard, modal focus, form label, kontrast
- `[ ]` Responsive: desktop-first, laptop/tablet'da buzilmasin
- `[x]` Critical flow testlari (§86): login, forbidden route, create/edit content, create creator, add episode, create ad, staff permission — **38 ta test**

---

## 10. Shared komponentlar rejasi

```
DataTable · Pagination · SearchInput · FilterPanel · StatusBadge
ConfirmDialog · MediaUploader · ImagePicker · VideoUploader
PermissionGuard · PageHeader · EmptyState · ErrorState · LoadingState
InternalTargetPicker · MoneyInput · SortableList
```

Bir xil pagination/filter logikasi 20 marta copy-paste qilinmaydi (§72).
Lekin haddan tashqari abstraction ham qilinmaydi.


---

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

## Umumiy komponentlar va jadval mantiqi `[x]` — ТЗ §72

### Mavjud komponentlar

| ТЗ so'ragan | Loyihada |
|---|---|
| `DataTable` | `TableWrap` + sahifaga xos jadval — to'liq umumiy `DataTable` yaratilmadi (ustunlar juda har xil) |
| `Pagination`, `SearchInput`, `PageHeader`, `StatusBadge` | `components/Ui.jsx` |
| `EmptyState`, `ErrorState`, `LoadingState`, `ForbiddenState` | `components/States.jsx` |
| `ConfirmDialog` | `components/ConfirmDialog.jsx` + `useConfirm` |
| `MediaUploader`, `ImagePicker`, `VideoUploader` | `MediaField`, `MediaPicker`, `GalleryField` |
| `PermissionGuard` | `auth/Guards.jsx` — `RequirePermission` |
| `FilterPanel` | yaratilmadi — filtrlar sahifaga xos |

### ⚠️ Ikkita jimgina buzuvchi xato topildi

`TaxonomyPage` (kategoriya/janr) va `BannerPage` (reklama/premyera) —
**bitta komponent ikki marshrutda**. React ularni bir xil tur deb
hisoblab qayta ishlatadi va **holatni saqlab qoladi**.

Oqibati eng yomon holatda: admin kategoriyani tahrirlash oynasini ochib,
menyudan «Janrlar» ga o'tsa, oyna yopilmasdi. `isEdit` rost, `isCategory`
esa endi yolg'on — saqlash **kategoriya ma'lumotini o'sha raqamli janr
ustiga yozardi**. Reklama/premyera juftida ham xuddi shunday.

Yengilroq oqibati: qidiruv matni va sahifa raqami eski bo'limdan qolib
ketardi — admin bo'sh ro'yxat ko'rib «ma'lumot yo'q» deb o'ylardi.

- `[x]` Ikkala juftga ham `key` qo'yildi — marshrut almashganda toza mount
- `[x]` Qo'riqchi test: `kind` bilan ajratilgan komponentga `key` majburiy

### Filtr va sahifa raqami

Qoida — «filtr o'zgarsa, birinchi sahifaga qayt» — 11 ta sahifada
qo'lda takrorlangan. Ishlaydigan 11 sahifani qayta yozish o'rniga
(§90) qoidaning o'zi test bilan qo'riqlandi.

⚠️ Detektorning birinchi ikki varianti ishlamadi: «hammasini tozalash»
tugmasi ham `setPage(0)` ni o'z ichiga oladi va butun sahifani
«qamralgan» qilib ko'rsatardi. Farq argumentda: filtr haqiqiy qiymat
uzatadi, tozalash esa bo'sh literal.

---

## Testlar `[~]` — ТЗ §86

| Fayl | Nimani qamraydi |
|---|---|
| `api/__tests__/refreshFlow.test.js` | 401 da tokenni yangilash: qayta urinish, bitta yangilash (poyga yo'q), cheksiz halqa yo'qligi, auth endpointlari istisnosi, tokenning `localStorage` ga tushmasligi |
| `auth/__tests__/guards.test.jsx` | Ruxsatli/ruxsatsiz sahifa, 403, rol darajasi, kirmagan foydalanuvchi, sessiya tiklanayotgan holat |

**Stack:** CRA jest + `@testing-library/react`. Kutubxona §86 uchun
ataylab qo'shildi — ТЗ sakkizta oqimni test qilishni talab qiladi va
ularning ko'pi DOM'siz tekshirilmaydi.

⚠️ `src/setupTests.js` yaratildi: usiz `toBeInTheDocument` matcheri
mavjud bo'lmaydi va testlar «is not a function» bilan yiqiladi.

### Hali yozilmagan oqimlar

`create content`, `edit content`, `create creator`, `add episode`,
`create advertisement` — bular server bilan ishlaydigan ko'p bosqichli
formalar. Ularning **backend tomoni to'liq qamralgan**
(`ContentAcceptanceTest`, `SeriesStructureAcceptanceTest`,
`CreatorModuleTest`, `AdvertisementModuleTest`), frontend tomoni esa
hozircha qo'lda tekshiriladi.

⚠️ **Muhim tafsilot:** eng qimmatli ikkitasi allaqachon yozildi.
`login` va `forbidden route` — xavfsizlik bilan bog'liq va jimgina
buziladigan yagona oqimlar; qolganlari buzilsa admin darhol ko'radi.

---

## Pul va sanoq formatlash `[x]` — ТЗ §103, §104

`utils/format.js` — `money()` va `count()`.

⚠️ **Nega markazlashtirildi.** Bu mantiq **to'rtta sahifada** qo'lda
takrorlangan edi (`ReportsPage`, `TariffsPage`, `UsersPage`,
`SubscriptionsPage`) va har biri `null` bilan **boshqacha** ishlardi:
uchtasi uni nolga aylantirardi, bittasi chiziqcha ko'rsatardi.

Farq bezak emas. Sovg'a obunada to'lov summasi `null` —
«sotilmagan». Uni «0 so'm» deb ko'rsatish «bepul sotildi» degan
**boshqa ma'noni** beradi (§45, §71).

| Funksiya | `null` | `0` | Nega |
|---|---|---|---|
| `money()` | `—` | `0` | Pulda nol va noaniqlik boshqa narsa |
| `count()` | `0` | `0` | Sanoqda nol haqiqiy: «hech kim ko'rmagan» |

`toLocaleString` endi panelda **hech qayerda to'g'ridan-to'g'ri
chaqirilmaydi**.


---

# ADMIN PANEL — BOSQICHMA-BOSQICH ТЗ (23.08.2026)

> **Qoidalar.** Eski kodga tegilmaydi: `/aadmin/*`, `/admin/*`, `/`,
> `/models`, `/bot/:id`, `/data-form/:id`, `/history/:id`, `/appeal/:id`
> — hammasi ishlab turadi, ma'lumot saqlanadi. Mobil dastur
> (`mobile/`) **umuman tegilmaydi**. Barcha ish faqat
> `frontend/src/adminpanel/` ichida, `/app/panel/*` marshrutida.

## Auditda topilgani

Backend **74 ta admin endpoint** taklif qiladi, panel esa ularning bir
qismini **umuman chaqirmaydi**. Ya'ni backendda yozilgan va sinalgan
ish frontendда ko'rinmaydi:

| Sahifa | Yetishmagan UI | Holat |
|---|---|---|
| Xodimlar | **yaratish**, tahrirlash, rol, ruxsatlar, parol, faollashtirish, bloklash — **8 amal** | `[x]` F1 |
| Media | arxivlash, tiklash, qayerda ishlatilgani, o'chirish — **4 amal** | `[x]` F2 |
| Dashboard | `charts`, `tables` — **2 endpoint** | `[x]` F3 |
| Bosh sahifa | tartiblash, qo'lda kontent tanlash, tanlangan ijodkorlar | `[x]` F4 |
| Hisobotlar | kontent analitikasi (§46) | `[x]` F5 |
| — | reklama CTR hisoboti (§81) | `[x]` F5 |
| — | bildirishnoma hisoboti (§33) | `[x]` F5 |
| Foydalanuvchilar | bitta foydalanuvchi sahifasi | `[x]` F6 |

⚠️ **Eng jiddiysi — xodim yaratish.** Backend to'liq tayyor (ierarxiya,
ruxsatlar, audit — 745 testdan bir qismi shuni tekshiradi), lekin
panelda tugma yo'q edi. Ya'ni yangi admin yoki worker **faqat baza
orqali** qo'shilardi. §78 qabul mezoni buni talab qiladi — F1 da
yopildi.

### Yakuniy holat (23.08.2026)

F1–F6 bajarildi, keyin ТЗ ning qolgan **qisman** bandlaridan §47 va
§86 yopildi. F7 (foydalanuvchi qismi) boshlanmaydi: buyurtmachi
tartibi bo'yicha avval admin panel, bundan tashqari `/app/auth/**`,
`/app/catalog`, `/app/content/{slug}` va qolgan ommaviy endpointlar
backendda hali yozilmagan — interfeys quradigan ma'lumot yo'q.

`react-scripts build` o'tadi va **panelda birorta ESLint
ogohlantirishi yo'q**. Testlar: **38 ta**, hammasi o'tadi.

To'liq ТЗ holati → [TZ_CHECKLIST.md](./TZ_CHECKLIST.md).

---

## §47 — hisobot filtrlari (23.08.2026)

`pages/reports/ReportFilters.jsx`. Backend beshta obyekt filtrini
(kontent · kategoriya · ijodkor · tarif · reklama) allaqachon
qo'llab-quvvatlardi va ular davr bilan **birga** ishlardi; panelda esa
faqat davr tugmalari bor edi. Ya'ni «shu kategoriya, oxirgi 7 kun»
degan savolni umuman berib bo'lmasdi.

⚠️ **Davr tugmasi va qo'lda sana bir-birini tozalaydi.** Backend
ikkalasini birga qabul qiladi, lekin qaysi biri ustun ekani
foydalanuvchiga ko'rinmasdi: «oxirgi 7 kun» tanlangan holda eski
sanalar qolib ketsa, admin qaysi davr amalda ekanini bilmasdi.

⚠️ **Filtr qo'llangani sahifada ochiq yoziladi** (`appliedFilters`).
Usiz saqlangan yoki hamkasbga yuborilgan skrinshotda «bu son butun
platformanikimi yoki filtrlanganmi?» degan savolga javob yo'q edi.

⚠️ **Bo'sh natija «xato» emas.** Filtr mos kelmasa alohida matn
chiqadi — ikkalasini bir xil ko'rsatish admin uchun butunlay boshqa
xulosa bo'lardi (§45).

---

## §86 — beshta yetishmagan oqim testi (23.08.2026)

| Fayl | Oqim |
|---|---|
| `pages/__tests__/contentFlow.test.jsx` | kontent yaratish **va** tahrirlash |
| `pages/__tests__/creatorFlow.test.jsx` | ijodkor yaratish |
| `pages/__tests__/episodeFlow.test.jsx` | qism qo'shish |
| `pages/__tests__/advertisementFlow.test.jsx` | reklama yaratish |

Testlar aynan **jimgina buziladigan** xatti-harakatlarni qo'riqlaydi:
tahrirlashda slug o'zgarmasligi (B13), janr va ijodkorlar saqlanib
qolishi (B17), `version` yuborilishi (§60), qism raqami avtomatik
hisoblanishi, meros holatida `accessPolicyOverride` ning `null`
ketishi (B24), reklama so'roviga premyera maydonlari tushmasligi.

### ⚠️ Test topgan ikkita HAQIQIY xato — tuzatildi

**1. Modalda har harfdan keyin fokus yo'qolardi.**

`Modal` ning effekti `onClose` ni bog'liqlik sifatida ushlab turardi.
Ota-komponent odatda `onClose={() => setOpen(false)}` yoki
`onClose={requestClose}` beradi — bu funksiya **har renderda yangi**.
Formasi o'z ichida turgan oynalarda (kontent muharriri, reklama,
bildirishnoma, bosh sahifa, premium sovg'a) har bosilgan harf render
keltirib chiqaradi; effekt qayta ishga tushib, tozalashda fokusni
ochgan tugmaga qaytaradi va keyin oynaning **birinchi** elementiga
olib qo'yadi.

Natija: maydonga faqat **bitta belgi** tushardi. Ya'ni kontent
muharririga amalda yozib bo'lmasdi.

Tuzatish: `onClose` `ref` orqali ushlanadi, effekt faqat `open` ga
bog'liq (`components/Modal.jsx`).

**2. Bitta bo'sh maydon «Fasl va qismlar» bo'limini yiqitardi.**

`EpisodesTab` da `e.effectiveAccessPolicy.replace(...)` himoyasiz
chaqirilardi. Qiymat kelmasa butun bo'lim oq ekranga aylanib, admin
qismlarni umuman ko'ra olmasdi. Endi `String(... || '—')`.

---

## ⚠️ Backend ishlari to'xtatildi — muhit muammosi

§16, §17, §51 va §59 backend o'zgarishini talab qiladi va ular bu
mashinada **kompilyatsiya qilib tekshirib bo'lmaydi**:

- faqat **JDK 25** o'rnatilgan;
- `mvnw` buzilgan — `.mvn/wrapper/maven-wrapper.properties` yo'q;
- JDK 23 dan boshlab `javac` annotatsiya protsessorlarini sukut
  bo'yicha o'chiradi, `maven-compiler-plugin` esa `pom.xml` da 3.11.0
  ga qotirilgan va `proc` parametrini bilmaydi → **Lombok umuman
  ishlamaydi**, 410 ta «cannot find symbol».

Tekshirib bo'lmaydigan backend o'zgarishini kiritish ko'r-ko'rona ish
bo'lardi. Yechim: JDK 17/21 o'rnatish, yoki `pom.xml` da compiler
plaginini 3.13+ ga ko'tarib `<annotationProcessorPaths>` ga Lombok'ni
ochiq yozish (bu build sozlamasini o'zgartirish, shuning uchun
tasdiqsiz qilinmadi).

---

## BOSQICH F1 — Xodimlar boshqaruvi `[x]`

**Nega birinchi:** RBAC butun tizimning asosi va u hozir paneldan
boshqarilmaydi.

**Fayl:** `pages/StaffPage.jsx` (126 → ~400 qator, kerak bo'lsa
`pages/staff/` ga bo'linadi)

| # | Vazifa | API |
|---|---|---|
| 1 | «+ Xodim» tugmasi va yaratish formasi | `POST /staff` |
| 2 | Rol tanlash — faqat **yaratuvchi bera oladigan** rollar ko'rinsin | — |
| 3 | WORKER uchun ruxsatlar ro'yxati (checkbox) | `PUT /{id}/permissions` |
| 4 | Tahrirlash: ism, telefon, email | `PUT /{id}` |
| 5 | Rolni o'zgartirish | `PUT /{id}/role` |
| 6 | Parolni tiklash | `PUT /{id}/password` |
| 7 | Faollashtirish / faolsizlantirish | `POST /{id}/activate`, `/deactivate` |
| 8 | Bloklash / blokdan chiqarish (sabab bilan) | `POST /{id}/block`, `/unblock` |

**Qabul mezonlari:**

- `[x]` ADMIN faqat WORKER yarata oladi — rol ro'yxatida boshqasi **yo'q**
- `[x]` SUPER_ADMIN — ADMIN va WORKER yarata oladi, HYPER_ADMIN yo'q
- `[x]` Ruxsatlar bloki faqat WORKER tanlanganda ko'rinadi
- `[x]` O'zini bloklash yoki rolini o'zgartirish tugmasi **ko'rinmaydi**
- `[x]` Har bir buzuvchi amal `ConfirmDialog` bilan
- `[x]` Xato `useFieldErrors` orqali maydonga bog'lanadi (§52)
- `[x]` Uch tilda tarjima
- `[x]` ⚠️ Frontend yashirishi **xavfsizlik emas** — backend baribir 403
  qaytaradi va bu allaqachon sinalgan

### Nima qilindi (23.08.2026)

`pages/StaffPage.jsx` (126 qator) → `pages/staff/` papkasi:
`StaffPage.jsx` (ro'yxat, filtrlar, sakkizta amal), `StaffForm.jsx`
(yaratish/tahrirlash), `PermissionPicker.jsx`, `permissionGroups.js`.

Rol tanlash ro'yxati `/auth/me` dagi `creatableRoles` dan quriladi —
ya'ni ierarxiya frontendda qayta yozilmaydi, backend nima ruxsat
bersa, o'sha ko'rinadi.

⚠️ **Rol, ruxsat va parol alohida oynalarda.** Backend ham shunday
bo'lingan: `PUT /staff/{id}` faqat ism, telefon, email va avatarni
o'zgartiradi. Har birining o'z xavfsizlik qoidasi bor («o'zida
bo'lmagan ruxsatni bera olmaydi», «yangi rol ham yaratish doirasida
bo'lsin»). Ularni bitta formaga qo'shish tekshiruvlarni foydalanuvchi
uchun ko'rinmas qilardi: bitta «Saqlash» ba'zan o'tib, ba'zan
tushunarsiz 403 qaytarardi.

⚠️ **Uch modal ataylab shartli chiziladi** (`{permissionsFor && ...}`).
Ular boshlang'ich holatini prop'dan oladi; doim chizilib faqat `open`
bilan yashirilsa, ikkinchi marta ochilganda holat eskisidan qolardi —
parol oynasida oldingi matn, ruxsatlar oynasida boshqa xodimning
qutichalari.

### Yo'l-yo'lakay tuzatilgan xato

**Build butunlay sinib turgan edi.** `TariffsPage.jsx` da `money()`
chaqirilardi, lekin import 94052b7 («pul formatlash markazlashtirildi»)
da olib tashlangan. `react-scripts build` `no-undef` bilan yiqilardi,
ya'ni panelning **hech qanday** o'zgarishini yig'ib bo'lmasdi.

### Qo'shilgan test

`pages/staff/__tests__/permissionGroups.test.js` — paneldagi ruxsatlar
ro'yxatini backenddagi `Enums/Permission.java` bilan solishtiradi.

⚠️ Nega kerak: WORKER huquqlari faqat shu ro'yxatdagi qutichalar orqali
beriladi. Backendga yangi ruxsat qo'shilsayu bu yerga qo'shilmasa, u
panel orqali **hech qachon berib bo'lmaydigan** huquqqa aylanadi —
va buni hech kim sezmaydi, chunki hech qayerda xato chiqmaydi:
quticha shunchaki yo'q bo'ladi.

---

## BOSQICH F2 — Media kutubxonasi `[x]`

**Fayl:** `pages/MediaPage.jsx` (93 → ~250 qator)

| # | Vazifa | API |
|---|---|---|
| 1 | «Qayerda ishlatilgan» oynasi | `GET /media/{id}/usage` |
| 2 | Arxivlash / tiklash | `POST /media/{id}/archive`, `/restore` |
| 3 | O'chirish — **faqat ishlatilmayotgan fayl** | `DELETE /media/{id}` |
| 4 | Arxivlanganlarni ko'rsatish filtri | `GET /media?status=ARCHIVED` |

**Qabul mezonlari:**

- `[x]` O'chirishdan oldin qayerda ishlatilgani **ko'rsatiladi**
- `[x]` Ishlatilayotgan fayl o'chirilmoqchi bo'lsa, backend 409 qaytaradi
  va panel qaysi kontentda ekanini ro'yxat qilib ko'rsatadi
- `[x]` Arxivlanganlar sukut bo'yicha **ko'rinmaydi** (§26)

### Nima qilindi (23.08.2026)

Kartochka bosilganda «Fayl ma'lumoti» oynasi ochiladi: oldindan
ko'rish, o'lcham va davomiylik, holat bejagi, **qayerda ishlatilgani**
ro'yxati va amallar (arxivlash / tiklash / butunlay o'chirish).

⚠️ **O'chirish tugmasi ishlatilish ro'yxatiga bog'langan.** U ro'yxat
hali yuklanmaganda ham bloklanadi: «bilmayman» holatida o'chirishga
ruxsat berish tekshiruvni butunlay ma'nosiz qilardi. Ro'yxat bo'sh
bo'lgandagina tugma yoqiladi.

⚠️ Bu — QULAYLIK tekshiruvi. Ikki admin bir vaqtda ishlayotgan bo'lsa,
fayl oyna ochiq turganda band qilinishi mumkin. Shuning uchun
backendning 409 `MEDIA_IN_USE` javobi ham baribir ushlanadi va sabab
bilan ko'rsatiladi.

⚠️ **Holat filtrida bo'sh qiymat «hammasi» EMAS.** Backend holat
berilmasa faqat `READY` qaytaradi (§26), shuning uchun filtrda ikki
variant bor: «Kutubxonada» va «Arxivlangan». «Barchasi» varianti
qo'shilsa, u ishlamaydigan tanlov bo'lardi.

---

## BOSQICH F3 — Dashboard grafiklari va jadvallari `[x]`

**Fayl:** `pages/DashboardPage.jsx` (95 → ~200 qator)

Backendda `charts` va `tables` §48 da yozilgan va sinalgan, lekin panel
ularni chaqirmaydi.

| # | Vazifa | API |
|---|---|---|
| 1 | Ro'yxatdan o'tish, ko'rish, daromad grafiklari | `GET /dashboard/charts` |
| 2 | Oxirgi kontent va foydalanuvchilar jadvali | `GET /dashboard/tables` |
| 3 | Davr tanlash (7 / 30 / 90 kun) | `?days=` |

**Qabul mezonlari:**

- `[x]` Ma'lumot yo'q bo'lsa **bo'sh holat**, soxta grafik emas (§45)
- `[x]` Uchta so'rov **parallel** ketadi, ketma-ket emas (§73)
- `[x]` `TrendChart` komponenti qayta ishlatiladi

### Nima qilindi (23.08.2026)

To'rtta grafik (yangi foydalanuvchilar, ko'rishlar, obuna daromadi,
donatlar) va beshta jadval (oxirgi kontent, eng ko'p ko'rilgan, yangi
foydalanuvchilar, eng samarali reklama, donat bo'yicha ijodkorlar).
Davr: 7 / 30 / 90 kun.

⚠️ **`summary` + `tables` bitta `Promise.all` da, `charts` alohida
hook'da.** Ikkala effekt bir renderda ishga tushadi, ya'ni so'rovlar
parallel ketadi. Grafiklarni ajratish sababi boshqa: davr
almashtirilganda FAQAT grafiklar qayta so'raladi — kartochkalar va
jadvallar davrga bog'liq emas va ularni har bosishda qayta yuklash
bekorga sarf bo'lardi.

⚠️ **Grafik xatosi butun sahifani yiqitmaydi.** Kartochkalar va
jadvallar allaqachon kelgan bo'ladi va ular baribir foydali — xato
faqat grafiklar blokida, «qayta urinish» tugmasi bilan ko'rsatiladi.

⚠️ **Donatlar valyuta bo'yicha alohida chiziqda.** Backend
`{day, series: 'STARS', value}` qatorlarini beradi; panel ularni kun
bo'yicha pivotlaydi. Qo'shib yuborish 10 so'm bilan 10 dollarni
qo'shishday bo'lardi.

### `TrendChart` umumiylashtirildi

Ilgari `views / plays / completes` uchtaligi komponentga **qotirilgan**
edi. Dashboard butunlay boshqa qatorlarni chizadi, ya'ni qotirilgan
holda har biri uchun yangi komponent yozilardi va ular asta-sekin
bir-biridan farq qilib ketardi. Endi qatorlar `series` prop'i orqali
keladi; standart qiymat hisobotlar sahifasi uchun eskisicha qoladi va
`ReportsPage` umuman o'zgarmadi.

Bundan tashqari grafik ostiga **eng yuqori qiymat** yozildi: usiz
grafik balandligi hech narsani anglatmasdi — bir xil ko'rinishdagi
ikkita chiziq 10 ta va 10 000 ta hodisani bildirishi mumkin edi.

⚠️ **Ikkita jadvalda faqat identifikator ko'rinadi** («eng ko'p
ko'rilgan», «eng samarali reklama»). Backend bu jadvallarda sarlavha
qaytarmaydi va uni olish har qator uchun qo'shimcha so'rov talab
qilardi. Buni yashirish o'rniga jadval ostida ochiq yozildi.

---

## BOSQICH F4 — Bosh sahifa: tartiblash va qo'lda tanlash `[x]`

**Fayl:** `pages/HomepagePage.jsx` (221 → ~350 qator)

| # | Vazifa | API |
|---|---|---|
| 1 | Bo'limlarni tartiblash | `PUT /homepage/sections/order` |
| 2 | Bo'limga **qo'lda kontent tanlash** | `GET/PUT /homepage/sections/{id}/items` |
| 3 | Tanlangan ijodkorlar ro'yxati | `GET /homepage/creators` |

**Qabul mezonlari:**

- `[x]` Qo'lda tanlangan ro'yxat bo'sh bo'lsa — avtomatik qoida
  ishlaydi (§31 dagi mantiq)
- `[x]` Tartib saqlangach ommaviy lentada ham o'zgaradi

### Nima qilindi (23.08.2026)

`pages/homepage/SectionItemsModal.jsx` va
`pages/homepage/CreatorsPreviewModal.jsx` qo'shildi;
`HomepagePage.jsx` ga «yuqoriga / pastga» va «Tartibni saqlash».

⚠️ **Tartib mahalliy, saqlash bitta so'rov.** O'q tugmalari faqat
mahalliy ro'yxatni o'zgartiradi, bazaga esa «Tartibni saqlash»
bosilganda BITTA `PUT /homepage/sections/order` ketadi. Har bosishda
alohida so'rov yuborilsa, oradagi lahzada ikkita bo'lim bir xil
raqamda turardi va o'sha paytda `/app/home` ni so'ragan foydalanuvchi
aralashib ketgan bosh sahifani ko'rardi.

⚠️ **Mahalliy tartib bo'lim OBYEKTLARI emas, ID lar ro'yxati.**
Bo'limni yoqish/o'chirish ro'yxatni serverdan qayta yuklaydi va
obyektlar almashadi; ID lar esa o'zgarmaydi, ya'ni saqlanmagan tartib
yo'qolmaydi.

⚠️ **Bo'sh ro'yxat — buzuq holat emas, QAROR.** «Ro'yxatni tozalash»
qatorni avtomatik qoidaga qaytaradi (kontent turi yoki
`featured`/`popular` bayroqlari bo'yicha o'zi to'ladi). Oynada shu
ochiq yozilgan, aks holda admin bo'sh ro'yxatni «sozlanmagan» deb
o'ylardi.

⚠️ **Nashr qilinmagan kontent qatorga qo'shilsa, bejak bilan
belgilanadi.** U bosh sahifada ko'rinmaydi (`isVisible` filtri), va
buni aytmaslik «qo'shdim, chiqmadi» degan savolni tug'dirardi.

⚠️ **«Mashhur ijodkorlar» qatorida «Kontent» tugmasi yo'q.** Bu bo'lim
kontent bilan to'ldirilmaydi — u `featured` bayrog'i va
`homepage.creators.ranking` sozlamasidan quriladi. Uning o'rniga
«Ijodkorlar» oynasi backend qaytaradigan HAQIQIY ro'yxatni ko'rsatadi:
ilgari sozlama satri bor edi, mazmuni esa faqat mobil ilovada
ko'rinardi.

---

## BOSQICH F5 — Hisobotlar `[x]`

| # | Vazifa | API | ТЗ |
|---|---|---|---|
| 1 | Reklama CTR hisoboti | `GET /advertisements/{id}/statistics` | §81 |
| 2 | Bildirishnoma hisoboti | `GET /notifications/{id}/report` | §33 |
| 3 | Kontent analitikasi | `GET /content/{id}/statistics` | §46 |

**Qabul mezonlari:**

- `[x]` ⚠️ Mavjud bo'lmagan ko'rsatkich **`null`** ko'rsatiladi, nol emas.
  FCM ulanmagani uchun «yetkazildi» soni yo'q va uni 0 deb ko'rsatish
  yolg'on bo'lardi (§33)
- `[x]` Ko'rsatishsiz CTR — nol, nolga bo'linish yo'q

### Nima qilindi (23.08.2026)

Uchta hisobot **obyektning o'z sahifasida** ochiladi, alohida
«hisobotlar» ro'yxatida emas: reklama qatorida 📊, kontent qatorida 📊,
bildirishnoma qatorida «Hisobot». Sabab oddiy — admin bu savolni aynan
o'sha yozuvni ko'rib turganda beradi, uni boshqa sahifadan qidirib
topishga majburlash ortiqcha qadam bo'lardi.

Fayllar: `pages/reports/AdStatsModal.jsx`,
`ContentStatsModal.jsx`, `NotificationReportModal.jsx` va ular
bo'lishadigan `StatBits.jsx` (davr tanlash, kartochka, davr sarlavhasi).

⚠️ **`StatTile` da `null` va `0` har xil ko'rinadi.** Nol — «bo'lmadi»,
bo'sh katak esa «o'lchanmaydi», va oynada sababi ham yoziladi
(`unavailableReason`). Bildirishnomada `delivered = 0` ko'rsatilsa,
admin «hech kimga yetib bormadi» deb o'ylab butunlay boshqa muammoni
qidirardi — aslida push provayderi ulanmagan va kvitansiya umuman
kelmaydi.

⚠️ **Auditoriya hajmi «yuborildi» emas.** U nishon auditoriyasining
hozirgi hajmi. Usiz `opened` ni talqin qilib bo'lmaydi: 250 ta
ochilish auditoriya 300 kishimi yoki 300 000 kishimi — butunlay
boshqa xulosa.

⚠️ **Hisobot tugmasi yuborilmagan xabarda ham ochiladi** — auditoriya
hajmi aynan yuborishdan OLDIN kerak bo'ladigan raqam.

⚠️ **Premyerada statistika tugmasi yo'q.** Backendda faqat reklama
uchun kunlik jamlanma bor; tugma bo'lsa-yu hech narsa ko'rsatmasa, u
yo'qidan yomonroq bo'lardi.

⚠️ **Kontent statistikasi `REPORT_VIEW` ni talab qiladi.** Kontentni
ko'rish huquqi analitikani ko'rish huquqini bermaydi, shuning uchun
tugma shu ruxsatga bog'landi — aks holda xodim bosib 403 olardi.

### Yo'l-yo'lakay tuzatilgan xato

`ReportsPage` da `money` importi ishlatilmay turardi va build ogohlantirish
berardi. Sababi kattaroq: backend `subscriptionRevenue` ni allaqachon
qaytarardi, sahifa esa uni **umuman ko'rsatmasdi**. Ko'rsatkich
qo'shildi. Endi panelda birorta ESLint ogohlantirishi yo'q.

---

## BOSQICH F6 — Foydalanuvchi sahifasi `[x]`

| # | Vazifa | API |
|---|---|---|
| 1 | Bitta foydalanuvchi sahifasi | `GET /users/{id}` |
| 2 | Obuna tarixi, balans, qurilmalar bir joyda | mavjud endpointlar |

### Nima qilindi (23.08.2026)

`pages/UserDetailPage.jsx`, marshrut `/app/panel/users/:userId`
(`USER_VIEW` bilan qo'riqlangan). Ro'yxatdagi ism havolaga aylandi;
amal tugmalari qatorda QOLDI — bloklash va premium ko'p uchraydigan
ishlar va ular uchun sahifaga o'tish ortiqcha qadam bo'lardi.

Sahifada: profil (holat, til, ro'yxatdan o'tgan sana, oxirgi faollik,
bloklash sababi), balans (pul · ⭐ · ◎), premium holati, qurilmalar
ro'yxati va obuna tarixi — hammasi bitta ekranda.

⚠️ **Obuna tarixi — vaqtinchalik yechim va u ochiq aytilgan.**
Backendda «foydalanuvchi bo'yicha obunalar» endpointi YO'Q: qidiruv
faqat telefon va ism bo'yicha. Panel telefon bo'yicha qidirib, keyin
aynan shu `userId` ga tegishlilarini ajratib oladi. Skanerlash
chegarasi 100 qator; mos keluvchi yozuvlar shundan ko'p bo'lsa,
sariq ogohlantirish chiqadi va «Obunalar» bo'limiga havola beriladi.
Jim qolib qisqartirish «bu odamning boshqa obunasi yo'q» degan
yolg'on xulosaga olib borardi (§45).

⚠️ **`SUBSCRIPTION_VIEW` alohida tekshiriladi.** Obuna ro'yxati KIM
qancha to'laganini ochadi, shuning uchun `USER_VIEW` uni bermaydi
(§71, §107). Ruxsat yo'q bo'lsa bo'lim o'rniga sabab yoziladi.

⚠️ **Sovg'a obunada to'lov «—», nol emas.** `paidAmount = null`
«sotilmagan» degani; «0 so'm» esa «bepul sotildi» degan boshqa
ma'noni berardi (§45, §103).

⚠️ **`SubscriptionsPage` endi `?q=` ni manzildan o'qiydi.** Usiz
foydalanuvchi sahifasidagi havola bo'limni ochib, filtrni bo'sh
qoldirardi va admin telefonni qo'lda qayta terardi. Keyingi
o'zgarishlar manzilga yozilmaydi — bu faqat kirish nuqtasi.

---

## BOSQICH F7 — FOYDALANUVCHI (USER) QISMI — **ENG OXIRGI**

⚠️ **Hozir boshlanmaydi.** Buyurtmachi tartibi: avval admin panel
to'liq tugaydi.

Bundan tashqari **backend tayyor emas**: `/app/auth/**` (OTP kirish),
`/app/catalog`, `/app/content/{slug}`, `/app/search`, `/app/me`,
`/app/purchases`, `/app/comments` — yozilmagan. Ya'ni interfeys
quradigan ma'lumot yo'q.

---

## Har bosqichda bajariladigan tekshiruv (§102)

```
[ ] kod yozildi
[ ] validatsiya ishlaydi
[ ] ruxsat tekshirildi (frontend yashiradi, backend 403 qaytaradi)
[ ] API ulandi
[ ] yuklanish holati bor
[ ] bo'sh holat bor
[ ] xato holati + qayta urinish tugmasi bor
[ ] uch tilda tarjima
[ ] test qo'shildi
[ ] build o'tdi
[ ] roadmap yangilandi
```
