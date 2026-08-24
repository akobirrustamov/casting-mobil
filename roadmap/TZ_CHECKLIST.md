# ТЗ BO'YICHA VAZIFALAR — QAT'IY RO'YXAT

> Manba: [my-idea.md](./my-idea.md) — ТЗ ning o'zi, §0–§110.
> Holat: [roadmap.md](./roadmap.md) · [BACKEND_ROADMAP.md](./BACKEND_ROADMAP.md) ·
> [FRONTEND_ROADMAP.md](./FRONTEND_ROADMAP.md)

Tuzilgan sana: **23.08.2026** · Oxirgi yangilanish: **23.08.2026**

**Belgilar**

| Belgi | Ma'nosi |
|---|---|
| ~~ustidan chizilgan~~ ✅ | To'liq bajarilgan — backend ham, panel ham |
| `[~]` | Qisman — asosiy qismi bor, nimadir yetishmaydi |
| `[ ]` | Bajarilmagan |
| `[!]` | Bloklangan yoki tashqi qarorga bog'liq |

⚠️ **§0–§5 va §100–§110 vazifa emas** — ular ish uslubi, hujjat formati va
texnologiya tanlovi haqidagi qoidalar. Ular pastdagi sanoqqa kirmaydi.

---

## Yakuniy sanoq — §6–§98 (92 ta bo'lim)

⚠️ 93 emas, **92**: ТЗ da §81 raqami umuman yo'q — `my-idea.md` da §80 dan
keyin darhol §82 keladi.

| Holat | Soni |
|---|---|
| ✅ To'liq | **75** |
| `[~]` Qisman | **14** |
| `[ ]` / `[!]` Bajarilmagan | **3** |

### ⚠️ Roadmap hujjatlari eskirgan edi — to'rtta tuzatish

Kod tekshirilganda ma'lum bo'ldiki, `BACKEND_ROADMAP.md` haqiqiy
holatdan orqada qolgan. Bu ro'yxatning birinchi versiyasi o'sha
hujjatga asoslangan edi va quyidagilarni **noto'g'ri ochiq** deb
ko'rsatgan:

| § | Avval yozilgani | Haqiqat |
|---|---|---|
| §92 | sirlar repozitoriyda (CRITICAL) | ✅ `application.properties` **gitignore** da va git tarixida ham yo'q; JWT secret default'i olib tashlangan (kalitsiz ilova ko'tarilmaydi); `AutoRun` da default parol yo'q |
| §61 | refresh token bazada saqlanmaydi, rotatsiya yo'q | ✅ `V25__refresh_tokens.sql`, `RefreshTokenService`, `replaced_by` (rotatsiya) va `revoked_at` (bekor qilish) mavjud |
| §64 | OpenAPI yo'q | ✅ `Config/OpenApiConfig.java` + `springdoc` bog'liqligi bor |
| §47, §86 | qisman | ✅ shu sessiyada yopildi — pastga qarang |

---

## A. ROLLAR VA AVTORIZATSIYA — §6–§12

- ~~**§6** Asosiy rollar: HYPER_ADMIN · SUPER_ADMIN · ADMIN · WORKER · USER~~ ✅
- ~~**§7** Rol ierarxiyasi — kim kimni yaratadi~~ ✅
- ~~**§8** SUPER_ADMIN huquqlari~~ ✅
- ~~**§9** ADMIN huquqlari~~ ✅
- ~~**§10** WORKER — fine-grained `Permission` orqali~~ ✅
- ~~**§11** Backend avtorizatsiyasi — ikki qavat (rol + ruxsat), har so'rovda bazadan~~ ✅
- ~~**§12** Staff management — ro'yxat, yaratish, tahrirlash, rol, ruxsatlar, parol, faollashtirish, bloklash~~ ✅ **(F1)**

---

## B. KONTENT PLATFORMASI — §13–§23

- ~~**§13** Kontent platformasi — tur · tuzilish · format uchta mustaqil o'lchov~~ ✅
- ~~**§14** Kontent tuzilishi: SINGLE · EPISODIC · SEASONAL~~ ✅
- ~~**§15** Content entity — status ≠ visibility ≠ language ≠ tarjima~~ ✅
- **§16** Category — `[~]`
  - ~~CRUD, uch tilli tarjima, ikonka, slug, tartib~~ ✅
  - `[ ]` O'chirish endpointi (kontentga bog'langanini tekshirish bilan)
  - `[ ]` Tartibni panelda sudrab o'zgartirish
- **§17** Genres — `[~]`
  - ~~CRUD, uch tilli tarjima~~ ✅
  - `[ ]` O'chirish endpointi
  - `[ ]` Janr bo'yicha kontent sanog'i
- ~~**§18** Content media — afisha, muqova, galereya, tilga xos afisha~~ ✅
- ~~**§19** Video parts — segmentlar, dublyaj tili~~ ✅
- ~~**§20** Season — fasllar, afisha~~ ✅
- ~~**§21** Episode — qism, kadr, davomiylik, kirish siyosati, narx~~ ✅
- ~~**§22** Content creation UI — 6 bo'limli muharrir, bitta 100 inputli forma EMAS~~ ✅
- ~~**§23** Content access model — FREE · PREMIUM · PURCHASE · meros~~ ✅

---

## C. IJODKORLAR VA MEDIA — §24–§26

- ~~**§24** Ijodkorlar — CRUD, uch tilli ism, foto, muqova, `displayName`~~ ✅
- ~~**§25** Mashhur ijodkorlar — `featured` bayrog'i, reyting sozlamasi, panelda ko'rish~~ ✅ **(F4)**
- ~~**§26** Media library — ro'yxat, qidiruv, filtr, qayerda ishlatilgani, arxivlash, tiklash, o'chirish~~ ✅ **(F2)**

---

## D. REKLAMA · PREMYERA · BOSH SAHIFA — §27–§31

- ~~**§27** Advertisement — CRUD, rasm + mobileImage, vaqt oynasi, auditoriya~~ ✅
- ~~**§28** Ad link types — ichki/tashqi havola turlari~~ ✅
- **§29** Advertisement analytics — `[~]`
  - ~~Har bir reklama uchun statistika: ko'rsatish, bosish, unikal, CTR, kunlik kesim~~ ✅ **(F5)**
  - `[ ]` Davr bo'yicha **aniq** unikal — hozir kunlik unikallar YIG'INDISI
- ~~**§30** Premiere module — CRUD, treyler, bog'langan kontent~~ ✅
- ~~**§31** Homepage management — 12 bo'lim, toggle, tartiblash, qo'lda kontent tanlash~~ ✅ **(F4)**

---

## E. MULOQOT — §32–§34

- **§32** Notifications — `[!]`
  - ~~CRUD, rejalashtirish, auditoriya, havola, panel UI~~ ✅
  - `[!]` **FCM ulanmagan** — `APP_FCM_CREDENTIALS` berilishi kerak. Xabar
    saqlanadi va rejalashtiriladi, lekin **yuborilmaydi**. Bu panelda sariq
    ogohlantirish bilan ochiq yozilgan.
- **§33** Notification report — `[~]`
  - ~~Hisobot ekrani, voronka, `available: false` bo'lsa bo'sh katak + sabab~~ ✅ **(F5)**
  - `[!]` `delivered` va `sent` (qabul qiluvchilar soni) FCM ulangandan keyin
    paydo bo'ladi. Hozir **nol emas, «o'lchanmaydi»** — bu ataylab.
- **§34** Comments — `[~]`
  - ~~Moderatsiya: yashirish, tiklash, o'chirilgan deb belgilash, shikoyat filtri~~ ✅
  - `[ ]` Izoh **yozish** (klient endpointi) — admin panel scope'ida emas

---

## F. FOYDALANUVCHI VA MONETIZATSIYA — §35–§44

- ~~**§35** User management — ro'yxat, qidiruv, bloklash, qurilmalar, bitta foydalanuvchi sahifasi~~ ✅ **(F6)**
- **§36** Premium tariflar — `[~]`
  - ~~4 tarif, narx, imkoniyatlar, «oyiga» hisobi~~ ✅
  - `[ ]` Avtomatik uzaytirish (recurring)
  - `[ ]` Muddat tugashidan oldin ogohlantirish
- ~~**§37** Premium huquqlari~~ ✅
- ~~**§38** Premium sovg'a qilish va tortib olish — audit bilan~~ ✅
- ~~**§39** Donation system~~ ✅
- ~~**§40** Stars~~ ✅
- ~~**§41** UzCasting Coin~~ ✅
- ~~**§42** Donation report — `/app/panel/donations`, valyutalar ALOHIDA~~ ✅
- **§43** Donation balance — `[~]`
  - ~~Balans ko'rsatiladi (pul · ⭐ · ◎)~~ ✅
  - `[!]` Balansni **to'ldirish** — to'lov provayderiga bog'liq (§44)
- **§44** Payment — `[!]` **BAJARILMAGAN**
  - Payme / Click / Uzum merchant ma'lumotlari **buyurtmachidan kelmagan**.
    Haqiqiy integratsiya yo'q.
  - ⚠️ Valyuta kursi ham aytilmagan — shuning uchun panelda narxlar 0 va bu
    «sozlanmagan» degani, taxminiy raqam emas.

---

## G. ANALITIKA — §45–§48

- ~~**§45** Analytics — kunlik jamlanma, xom hodisa ustida `COUNT(*)` yo'q~~ ✅
- ~~**§46** Content analytics — voronka: ochildi → o'ynatildi → tugatildi~~ ✅ **(F5)**
- ~~**§47** Report filters — davr + kontent · kategoriya · ijodkor · tarif · reklama, birga ishlaydi~~ ✅
  - `pages/reports/ReportFilters.jsx` — beshta obyekt filtri va qo'lda sana oralig'i
  - Filtr qo'llanganda sahifa buni **ochiq aytadi** (`appliedFilters`):
    usiz admin «bu son butun platformanikimi yoki filtrlanganmi?» degan
    savolga javob topa olmasdi — ayniqsa hamkasbga yuborilgan skrinshotda
  - Kontent va ijodkor — qidiruvli tanlash (§53), ro'yxatlari cheklanmagan
  - Bo'sh natija «xato» emas, **bo'sh natija** deb yoziladi (§45)
- ~~**§48** Dashboard — kartochkalar, grafiklar, jadvallar, davr tanlash~~ ✅ **(F3)**

---

## H. ADMIN PANEL UI — §49–§55

- ~~**§49** Admin sidebar — rol va ruxsatga qarab filtrlanadi~~ ✅
- ~~**§50** Brend va dizayn tokenlari — hex faqat `theme/panel.css` da~~ ✅
- **§51** UI talablari — `[~]`
  - ~~Loading · empty · error · retry · forbidden holatlari~~ ✅
  - ~~Qidiruv · filtr · sahifalash~~ ✅
  - `[ ]` **Ustun bo'yicha saralash (sorting)** — hech bir jadvalda yo'q
- ~~**§52** Form validation — frontend + backend, xato maydon yonida (`useFieldErrors`)~~ ✅
- ~~**§53** Content editor UX — bo'limlarga bo'lingan, saqlanmagan o'zgarish ogohlantirishi~~ ✅
- ~~**§54** Creator selection — qidiruv, joyida yaratish, rol va qahramon ismi~~ ✅
- ~~**§55** Search — uch tilda, kontent · ijodkor · foydalanuvchi~~ ✅

---

## I. BAZA VA ARXITEKTURA — §56–§68

- ~~**§56** Database qoidalari~~ ✅
- ~~**§57** ID strategiyasi~~ ✅
- ~~**§58** Soft delete~~ ✅
- **§59** Audit log — `[~]`
  - ~~Barcha buzuvchi amallar jurnalga tushadi, `/app/panel/audit` sahifasi~~ ✅
  - `[ ]` Saqlash muddati siyosati (eski yozuvlarni arxivlash)
- ~~**§60** Concurrency — optimistik qulf (`@Version`), DTO da `version` qaytadi~~ ✅
- **§61** Authentication — `[~]`
  - ~~Admin login, access token **xotirada**, refresh `httpOnly` cookie'da,
    401 da bitta yangilash (poyga yo'q)~~ ✅
  - ~~Refresh token **bazada** (`refresh_token` jadvali, V25), rotatsiya
    (`replaced_by`) va bekor qilish (`revoked_at`)~~ ✅
  - `[ ]` **OTP** kirish — bu ilova foydalanuvchilari uchun va
    `/app/auth/**` hali yozilmagan (F7 doirasi)
  - `[ ]` O'zi parolni tiklash oqimi — kanal (SMS / email) tanlanmagan,
    ya'ni §63 bilan bir xil to'siq. Xodim parolini ADMIN paneldan
    tiklay oladi (§12), bu ishlaydi.
- ~~**§62** Password — BCrypt, murakkablik talabi, javobda qaytmaydi~~ ✅
- **§63** 2FA — `[ ]` **ATAYLAB BAJARILMAGAN**
  - HYPER_ADMIN / SUPER_ADMIN uchun. Sabab `roadmap.md §14.1` da yozilgan:
    kanal (SMS / TOTP / email) tanlanmagan va buyurtmachi qarori kutilmoqda.
- ~~**§64** API design — bir xil xato formati `{code, message, errors}`, versiyalangan namespace, OpenAPI hujjati~~ ✅
  - `Config/OpenApiConfig.java` + `springdoc-openapi-starter-webmvc-ui`.
    Swagger UI prod'da o'chiriladi (`springdoc.swagger-ui.enabled`).
- ~~**§65** DTO qoidasi — entity qaytarilmaydi~~ ✅
- ~~**§66** N+1 muammosi — jamlangan so'rovlar~~ ✅
- ~~**§67** Media security — pullik video himoyalangan (B15)~~ ✅
- ~~**§68** Time — UTC, bir xil format~~ ✅

---

## J. FRONTEND ARXITEKTURASI — §69–§74

- ~~**§69** Frontend API layer — komponentda `axios` chaqirilmaydi, `api/client.js`~~ ✅
- **§70** Server state — `[~]`
  - ~~`useApi` hook: loading / error / reload~~ ✅
  - `[ ]` **Kesh, retry, invalidation yo'q** — TanStack Query qo'shilmagan.
    Sahifalar `useApi` ga bog'langan, ya'ni kutubxona qo'shilsa faqat shu
    hook almashadi.
- ~~**§71** Frontend route guards — `RequireAuth`, `RequirePermission`, 403 sahifasi~~ ✅
- **§72** Table components — `[~]`
  - ~~`TableWrap` · `Pagination` · `SearchInput` · `PageHeader` · `StatusBadge` ·
    `ConfirmDialog` · `MediaField` · `MediaPicker` · `PermissionGuard`~~ ✅
  - `[ ]` Umumiy `DataTable` va `FilterPanel` **ataylab yaratilmadi** —
    ustunlar va filtrlar sahifalar orasida juda har xil (§90 refactor siyosati)
- ~~**§73** Dashboard performance — bitta `summary`, so'rovlar parallel~~ ✅ **(F3)**
- ~~**§74** Ad / content tracking API — rate limiting bilan~~ ✅

---

## K. SCOPE CHEGARALARI — §75–§77

- ~~**§75** Mavjud casting moduli — o'chirilmadi, o'zgartirilmadi, panelda ko'rinadi~~ ✅
- ~~**§76** Mobil — hozir scope emas, roadmapda yozilgan~~ ✅
- ~~**§77** Watch later / Saved — Future Mobile Scope sifatida yozilgan (`roadmap.md §14.2`)~~ ✅

---

## L. QABUL MEZONLARI — §78–§84

- ~~**§78** Auth / RBAC — 5 roldan kirish, USER kira olmasligi, 403, menyu filtri~~ ✅
- ~~**§79** Content — yaratish, tahrirlash, nashr, slug, tarjima~~ ✅
- ~~**§80** Serial — fasl, qism, video segment, kirish siyosati merosi~~ ✅
- ~~**§82** Creator — yaratish, `displayName`, uch til~~ ✅
- ~~**§83** Premium — sovg'a, tortib olish, audit~~ ✅
- ~~**§84** Roadmap — uchala hujjat yuritiladi va yangilanadi~~ ✅

> ⚠️ ТЗ da **§81 raqami yo'q** — `my-idea.md` da §80 dan keyin darhol §82
> keladi. Reklama CTR hisoboti roadmapda «§81» deb atalgan; u §29 ning
> bir qismi va **bajarilgan** (F5).

---

## M. TESTLAR VA SIFAT — §85–§98

- ~~**§85** Backend testing — 294 ta test~~ ✅
- ~~**§86** Frontend test — sakkizala kritik oqim~~ ✅
  - ~~`login` (token yangilash, poyga yo'qligi, `localStorage` ga tushmasligi)~~
  - ~~`forbidden route` (ruxsat, rol darajasi, 403)~~
  - ~~`create content` · `edit content` — `pages/__tests__/contentFlow.test.jsx`~~
  - ~~`create creator` — `creatorFlow.test.jsx`~~
  - ~~`add episode` — `episodeFlow.test.jsx`~~
  - ~~`create advertisement` — `advertisementFlow.test.jsx`~~
  - Jami **38 ta test**, hammasi o'tadi
- ~~**§87** Har bosqichda build~~ ✅
- ~~**§88** Baseline test — kod o'zgartirishdan oldin~~ ✅
- ~~**§89** No duplicate code~~ ✅
- ~~**§90** Refactor policy — ishlayotgan kodni bekorga qayta yozmaslik~~ ✅
- ~~**§91** Database migration policy — Flyway~~ ✅
- ~~**§92** Secret security~~ ✅
  - `application.properties` **gitignore** da (`.gitignore:77`) va
    `git log --all` bo'yicha **hech qachon commit qilinmagan** — ya'ni
    tarixda ham sir yo'q. Tracked nusxa — `application.properties.example`.
  - `JwtService`: default qiymat olib tashlangan. Kalit berilmasa ilova
    **ko'tarilmaydi** — jim ishlab, lekin himoyasiz qolishdan ko'ra
    darhol yiqilgani yaxshiroq.
  - `AutoRun`: `00000000` yo'q. Parol `app.*.password` orqali beriladi;
    berilmasa hisob **yaratilmaydi**. Kuchsiz parol faqat ataylab
    (`app.bootstrap.allow-weak-password=true`) va ogohlantirish bilan.
- ~~**§93** Logging~~ ✅
- ~~**§94** Error handling — kliyent xatosi 500 qaytarmaydi, noma'lum yo'l JSON~~ ✅
- ~~**§95** Pagination~~ ✅
- ~~**§96** Search debounce — `SearchInput` da 400 ms~~ ✅
- **§97** Accessibility — `[~]`
  - ~~Modal fokus tuzog'i, Escape, fokusni qaytarish~~ ✅
  - ~~Tugma `aria-label`, `role="alert"`, `aria-pressed`, `aria-invalid`~~ ✅
  - `[ ]` To'liq klaviatura o'tishi va kontrast **tizimli tekshirilmagan**
- **§98** Responsive — `[~]`
  - ~~CSS media so'rovlari yozilgan: 1024px va 640px sinish nuqtalari~~ ✅
  - `[ ]` ⚠️ **Qurilmada tasdiqlanmagan** — sinov muhitida brauzer viewport'i
    1512px da qotib qolgan edi

---

## N. §99 — IMPLEMENTATION PHASES

- ~~**PHASE 0** — Audit~~ ✅
- ~~**PHASE 1** — Core architecture (RBAC, ruxsatlar, audit, layout, guard, API client)~~ ✅
- ~~**PHASE 2** — Staff management~~ ✅ **(F1)**
- ~~**PHASE 3** — CMS foundation (kategoriya, janr, ijodkor, media)~~ ✅
- ~~**PHASE 4** — Content (tur, tuzilish, fasl, qism, segment, galereya, kirish)~~ ✅
- ~~**PHASE 5** — Homepage (sozlash, reklama, karusel, premyera, tanlanganlar)~~ ✅ **(F4)**
- **PHASE 6** — Engagement — `[~]` izohlar ✅, bildirishnomalar `[!]` FCM
- **PHASE 7** — Users & monetization — `[~]` hammasi ✅, `[!]` to'lov (§44)
- **PHASE 8** — Analytics — ✅ dashboard, grafiklar, jadvallar, hisobotlar **(F3, F5)**;
  `[ ]` hisobot obyekt filtrlari (§47)
- **PHASE 9** — Hardening — `[~]`
  - ~~Ruxsat testlari, indekslar, xato/yuklanish holatlari, audit, build,
    regressiya testlari, katta video yuklash~~ ✅
  - `[ ]` Jadval saralash · a11y auditi · qurilmada responsive · 5 ta frontend oqim testi

---

## SHU SESSIYADA YOPILGANI (23.08.2026)

### §47 — hisobot filtrlari

`pages/reports/ReportFilters.jsx`. Backend beshta obyekt filtrini
allaqachon qo'llab-quvvatlardi va ular davr bilan **birga** ishlardi;
panelda esa faqat davr tugmalari bor edi, ya'ni «shu kategoriya,
oxirgi 7 kun» degan savolni umuman berib bo'lmasdi.

⚠️ **Davr tugmasi va qo'lda sana bir-birini tozalaydi.** Backend
ikkalasini birga qabul qiladi, lekin qaysi biri ustun ekani
foydalanuvchiga ko'rinmasdi — «oxirgi 7 kun» tanlangan holda eski
sanalar qolib ketsa, admin qaysi davr amalda ekanini bilmasdi.

⚠️ **Filtr qo'llangani sahifada ochiq yoziladi.** Backend javobda
`appliedFilters` ni qaytaradi. Usiz saqlangan yoki hamkasbga
yuborilgan skrinshotda «bu son butun platformanikimi?» degan savolga
javob yo'q edi.

### §86 — beshta yetishmagan oqim testi

`pages/__tests__/` da to'rtta yangi fayl: `contentFlow` (yaratish +
tahrirlash), `creatorFlow`, `episodeFlow`, `advertisementFlow`.
Panelda endi **38 ta test**, hammasi o'tadi.

Testlar aynan **jimgina buziladigan** xatti-harakatlarni qo'riqlaydi:
tahrirlashda slug o'zgarmasligi (B13), janr va ijodkorlar
saqlanib qolishi (B17), `version` yuborilishi (§60), qism raqami
avtomatik hisoblanishi, meros holatida `accessPolicyOverride` ning
`null` ketishi (B24), reklama so'roviga premyera maydonlari
tushmasligi.

### ⚠️ Test topgan HAQIQIY ikkita xato — tuzatildi

**1. Modalda har harfdan keyin fokus yo'qolardi.**
`Modal` ning effekti `onClose` ni bog'liqlik sifatida ushlab turardi.
Ota-komponent odatda `onClose={() => setOpen(false)}` beradi — bu
funksiya **har renderda yangi**. Formasi o'z ichida turgan oynalarda
(kontent muharriri, reklama, bildirishnoma, bosh sahifa) har bosilgan
harf render keltirib chiqaradi, effekt qayta ishga tushadi va fokusni
oynaning birinchi elementiga olib qo'yadi. Natijada maydonga faqat
**bitta belgi** tushardi.

Tuzatish: `onClose` `ref` orqali ushlanadi, effekt faqat `open` ga
bog'liq.

**2. Bitta bo'sh maydon «Fasl va qismlar» bo'limini butunlay
yiqitardi.** `EpisodesTab` da `e.effectiveAccessPolicy.replace(...)`
himoyasiz chaqirilardi. Qiymat kelmasa oq ekran chiqib, admin
qismlarni umuman ko'ra olmasdi. Endi `String(... || '—')`.

---

## QOLGAN ISHLAR — ustuvorlik bo'yicha

### 1-daraja · Buyurtmachi qaroriga bog'liq — kod yozilmaydi

1. `[!]` **§44** — to'lov provayderi merchant ma'lumotlari
   (Payme · Click · Uzum).
2. `[!]` **§40, §41** — Stars va Coin **kursi**. Usiz donat pulda
   hisoblanmaydi va valyuta paketlari narxi 0 bo'lib turadi.
3. `[!]` **§32** — FCM kaliti (`APP_FCM_CREDENTIALS`). Usiz
   bildirishnoma saqlanadi, lekin **yuborilmaydi**.
4. `[ ]` **§63** — 2FA kanali tanlanishi kerak: SMS · TOTP · email.
   Shu tanlov **§61** dagi «o'zi parolni tiklash» ni ham ochadi.

### 2-daraja · Panel funksiyasini to'ldirish

5. `[ ]` **§51** — jadvallarda ustun bo'yicha saralash.
   ⚠️ Bu **backend ishi**: ro'yxatlar sahifalangan, ya'ni faqat
   ko'rinayotgan sahifani saralash **yolg'on** natija berardi. Avval
   `sort` parametri kerak.
6. `[ ]` **§16, §17** — kategoriya va janr uchun o'chirish endpointi
   (kontentga bog'langanini tekshirish bilan) va panel tugmasi.
7. `[ ]` **§36** — obunani avtomatik uzaytirish va muddat
   ogohlantirishi. Ikkalasi ham §44 va §32 ga qisman bog'liq.

### 3-daraja · Sifat va tasdiqlash

8. `[ ]` **§98** — responsivlikni haqiqiy qurilmada tasdiqlash.
9. `[ ]` **§97** — a11y auditi: klaviatura o'tishi va kontrast.
10. `[ ]` **§70** — TanStack Query. Faqat `useApi` hook almashadi,
    sahifalar o'zgarmaydi.
11. `[ ]` **§29** — davr bo'yicha aniq unikal hisoblash.
12. `[ ]` **§59** — audit jurnali saqlash muddati siyosati.
13. `[ ]` **§34** — izoh yozish klient endpointi (F7 doirasi).

---

## ⚠️ MUHIT MUAMMOSI — backend bu mashinada yig'ilmaydi

Backend ishlarini (§16, §17, §51, §59) shu sessiyada bajarib
bo'lmadi, chunki **kodni kompilyatsiya qilib tekshirib bo'lmaydi**:

- mashinada faqat **JDK 25** bor (`/c/Program Files/Java` da yagona);
- `mvnw` buzilgan — `.mvn/wrapper/maven-wrapper.properties` yo'q
  (Maven `~/.m2/wrapper/dists` dan topildi);
- JDK 23 dan boshlab `javac` **annotatsiya protsessorlarini sukut
  bo'yicha o'chiradi**, `maven-compiler-plugin` esa 3.11.0 da
  qotirilgan va `proc` parametrini bilmaydi. Natijada **Lombok
  umuman ishlamaydi** va 410 ta «cannot find symbol» xatosi chiqadi.

Tekshirib bo'lmaydigan backend o'zgarishini kiritish — ko'r-ko'rona
ish bo'lardi, shuning uchun to'xtatildi.

**Yechim (bittasi yetarli):**

1. JDK 17 yoki 21 o'rnatib, `JAVA_HOME` ni shunga qaratish; yoki
2. `pom.xml` da `maven-compiler-plugin` ni 3.13+ ga ko'tarib,
   `<annotationProcessorPaths>` ga Lombok'ni ochiq yozish. Bu
   qurilishni har qanday JDK da takrorlanadigan qiladi va aslida
   to'g'ri yechim — lekin bu **build sozlamasini o'zgartirish**,
   shuning uchun sizning tasdiqingizsiz qilinmadi.

`mvnw` ni ham tiklash kerak (`maven-wrapper.properties` yo'q).
