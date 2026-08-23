# UZCASTING DEVELOPMENT ROADMAP

> Bu fayl loyihaning yagona holat manbai. Har bir katta ish tugagach yangilanadi.
> Sessiya uzilib qolsa, keyingi sessiya shu fayldan holatni tiklaydi.
>
> Backend detali → [BACKEND_ROADMAP.md](./BACKEND_ROADMAP.md)
> Frontend detali → [FRONTEND_ROADMAP.md](./FRONTEND_ROADMAP.md)
> Arxitektura → [BACKEND_ARCHITECTURE.md](./BACKEND_ARCHITECTURE.md) · [FRONTEND_ARCHITECTURE.md](./FRONTEND_ARCHITECTURE.md)

## 📁 Hujjatlar qayerda turadi

**Barcha loyiha hujjatlari `/roadmap/` papkasida** — buyurtmachi qarori (19.08.2026).

```
roadmap/
├── roadmap.md                  ← shu fayl, umumiy holat
├── BACKEND_ROADMAP.md          backend checklist
├── BACKEND_ARCHITECTURE.md     backend arxitekturasi
├── FRONTEND_ROADMAP.md         frontend checklist
└── FRONTEND_ARCHITECTURE.md    frontend arxitekturasi
```

⚠️ Dastlabki topshiriq (§0) bu fayllarni root, `backend/` va `frontend/` ichida
so'ragan edi. **Buyurtmachi ularni bitta papkaga yig'ishni so'radi** — shuning uchun
haqiqiy joylashuv yuqoridagicha. `backend/ARCHITECTURE.md` va `frontend/ARCHITECTURE.md`
bir xil nomda bo'lgani uchun `BACKEND_` / `FRONTEND_` prefiksi qo'shildi.

Bu papkaga tegishli **emas** (o'z joyida qoladi): `README.md`, `frontend/README.md`,
`mobile/CLAUDE.md`, `mobile/AGENTS.md`, `mobile/docs/*.md`, `tz/overal.md`.

Status belgilari: `[ ]` TODO · `[~]` IN PROGRESS · `[x]` DONE · `[!]` BLOCKED

Oxirgi yangilanish: 21.08.2026 — ТЗ §12–22; SINGLE kontent videosi (B22)

---

## 🎯 SOURCE OF TRUTH — konsepsiya va scope

> Bu bo'lim **hal qiluvchi**. Miro'dagi eski TZ bilan ziddiyat chiqsa,
> shu yerdagi qaror ustun turadi.

### Yangi platforma nima

**UZCASTING** — video streaming platformasi: qisqa kino, mini-serial, serial,
podkast, shou, ijodkorlar, casting, monetizatsiya va media.

### Scope'ga KIRMAYDI

Eski TZ'da «ijtimoiy tarmoq / messenger» konsepsiyasi bor. U **asosiy vazifa
deb qabul qilinmadi**. Quyidagilar hozirgi development scope'ida **yo'q**:

| Nima | Holat |
|---|---|
| Do'stlar tizimi (friends) | ❌ scope'da yo'q |
| Messenger | ❌ scope'da yo'q |
| Ijtimoiy lenta (social feed) | ❌ scope'da yo'q |
| Shaxsiy chat (private chat) | ❌ scope'da yo'q |

Tekshirildi: yangi platforma kodida (`Cms/`, `Admin/`, `src/adminpanel/`)
bunday funksiya **yaratilmagan**.

### Mavjud casting moduli — O'CHIRILMAYDI

Buyurtmachi talabi aniq: eski casting kodidagi **ishlayotgan funksiyalar
o'chirilmaydi va regressiyaga uchramaydi**.

Bu ikki qavat bilan kafolatlanadi:

| Test | Nimani ushlaydi |
|---|---|
| `OldCastingFrozenTest` (5) | Yo'l, controller, entity, jadval nomi yoki repozitoriy o'zgarsa |
| `ExistingCastingRegressionTest` (7) | Oqimning O'ZI buzilsa — anketa, qabul/rad, bot xabari |

Ikkinchisi muhimroq: yo'l joyida turib ham ichki mantiq buzilishi mumkin.
Ataylab tekshirildi — bot xabari yozilmay qo'yilsa test yiqiladi.

### ⚠️ Tuzoq: `Message` entity — bu MESSENGER EMAS

Eski kodda `Message` entity va `message` jadvali bor. Nomiga qarab uni
«messenger qoldig'i» deb o'ylash va o'chirib yuborish oson — **bu xato
bo'lardi**.

U aslida **Telegram botga yuboriladigan javob**:

- nomzod anketa yuboradi;
- admin qabul qiladi yoki rad etadi;
- shu paytda `message` jadvaliga yozuv tushadi
  («🟢Siz Castingdan o'tdingiz!» yoki «⛔️ so'rovingiz rad etildi»);
- bot o'sha yozuvni olib nomzodga yuboradi.

O'chirilsa — nomzodlar hech qachon javob olmaydi. `OldCastingFrozenTest`
uni alohida qo'riqlaydi.

---

## 1. Project Goal

Mavjud `casting` loyihasi ustiga production-ready **UZCASTING Admin Web Platform** qurish.

UZCASTING — qisqa metrajli kino, mini-serial, serial, film, podkast va shou kontentlarini
joylash, boshqarish, monetizatsiya qilish, ijodkorlarni boshqarish, reklama, premyera,
notification, tarif, donat va analitikani boshqaruvchi platforma.

Admin panel foydalanuvchilari: `HYPER_ADMIN` · `SUPER_ADMIN` · `ADMIN` · `WORKER`.
`USER` admin panelga **kira olmaydi** — u kelajakda mobil ilovadan foydalanadi.

### Scope chegaralari

**Kiradi:** admin web platform (backend + React admin UI), RBAC, CMS, media, monetizatsiya
model, analitika, audit.

**Kirmaydi (hozir):** USER uchun mobil UI, social network / messenger / do'stlar / lenta
konsepsiyasi. Eski TZ'dagi messenger g'oyasi asosiy vazifa emas.

**Buzilmasligi shart:** mavjud casting moduli (sayt, bot admin, Telegram bot oqimi) va
`mobile/` ichidagi Expo ilovasi.

---

## 2. Existing Project Audit

Audit sanasi: 19.08.2026. **Bu greenfield emas** — ishlayotgan production tizim.

### 2.1. Repozitoriy tarkibi

| Path | Nima | Holat |
|---|---|---|
| `backend/` | Spring Boot 3.1.2, Java 17, Maven | Production'da — `uzcasting.site` |
| `frontend/` | React 18 CRA (JavaScript, TS emas) | Sayt + 2 ta admin panel |
| `mobile/` | Expo SDK 57 (React Native) | Ishlab chiqilmoqda, scope'dan tashqari |
| `tz/` | 4 ta PDF ТЗ + `overal.md` | Hujjat |
| `tools/miro_build.py` | Miro doskasini yig'uvchi skript | Yordamchi |

Papkalar `backend/` va `frontend/` nomi bilan **mavjud** — qayta yaratilmadi, ko'chirilmadi.

### 2.2. Backend — mavjud kod

**Package tuzilishi** (`com.example.backend`), nomlar bosh harfli — nostandart, lekin izchil:

```
Config/      AutoRun, LocalSeeder, RestTemplateConfig, WebMvcConfig
Controller/  Admin, Attachment, Auth, CastingUser, News, Security
DTO/         AdminStatistic, CastingUser, GoogleLogin, News, ReqLogin, User
Entity/      Attachment, CastingUser, Message, News, Role, User
Enums/       AppealTypes, UserRoles
Repository/  Attachment, CastingUser, Message, News, Role, User
Security/    GoogleTokenVerifier, JwtService, MyFilter, SecurityConfig
Services/    AttachmentService, AuthService, SecurityService
Projection/  DashboardProjection
```

**Mavjud entity'lar:** `User` (UUID id, phone/email/googleSub/avatarUrl, `UserDetails`
implement qiladi), `Role` (int id, `UserRoles` enum), `Attachment` (UUID id, prefix, name,
isWebShow), `CastingUser` (Integer id, 30+ maydon), `News`, `Message`.

**Auth:** JWT (jjwt, HS256), access + refresh token, BCrypt password.
`MyFilter` tokenni tekshirib `SecurityContext`ga joylaydi — **to'g'ri yozilgan**.
Google login (`POST /api/v1/auth/google`) ishlaydi — `GoogleTokenVerifier` orqali.

**Mavjud endpointlar:** `/api/v1/auth/*` · `/api/v1/casting-user/*` · `/api/v1/news` ·
`/api/v1/file/*` · `/api/v1/admin/statistic` · `/api/v1/security`

**Media:** `Attachment.createAttachment()` faylni `backend/files{prefix}/` ga yozadi.
Nom `UUID + "_" + originalFilename`. Storage abstraction yo'q, path nisbiy.

**Migration:** yo'q. Flyway ham, Liquibase ham yo'q. `spring.jpa.hibernate.ddl-auto=update`.

### 2.3. Frontend — mavjud kod

React 18.3 + CRA 5, **JavaScript** (TypeScript emas).

- **Routing:** `react-router-dom` v6, barcha route'lar `src/App.js` ichida
- **HTTP:** `src/config/index.js` — markazlashtirilgan axios wrapper, `{error, data}` qaytaradi
- **State:** server-state kutubxonasi yo'q (TanStack Query yo'q), faqat `useState`/`useEffect`
- **UI:** aralash — Bootstrap 5 + PrimeReact + Tailwind 3.4 + FontAwesome + react-icons
- **Token:** `localStorage.access_token`, `Authorization` header (Bearer prefiksisiz)

**Ikkita alohida admin panel mavjud:**

| Path | Papka | Kim uchun |
|---|---|---|
| `/aadmin/*` | `src/admin/` | Sayt admini — casting anketalari |
| `/admin/*` | `src/bot-admin/` | Telegram bot admini — anketa, yangiliklar |

**Public sahifalar:** `/` (Home), `/models`, `/bot/:userId`, `/data-form/:userId`,
`/history/:userId`, `/appeal/:userId`.


### 2.6. Eski casting moduli — aniq ro'yxat (ТЗ §75)

**Bu modul ISHLAYAPTI va o'chirilmaydi.** Yangi UZCASTING platformasi
uning yonida quriladi: yangi backend yo'llari `/api/v1/app/**`, yangi
panel `/app/panel/**`. Eski yo'llarning birortasi ham o'zgartirilmagan.

| Entity | Jadval | Controller | API yo'li | Frontend sahifasi |
|---|---|---|---|---|
| `User` | `users`, `users_roles` | `AuthController`, `SecurityController` | `/api/v1/auth/*`, `/api/v1/security` | `/aadmin/login`, `/admin/*` login |
| `Role` | `roles` | — | — | — |
| `CastingUser` | `casting_user` | `CastingUserController` | `/api/v1/casting-user/*` | `/aadmin/casting-users/web`, `/aadmin/casting-users/:id`, `/admin/casting-users`, `/admin/accepted` |
| `News` | `news` | `NewsController` | `/api/v1/news` | `/admin/news` |
| `Message` | `message` | — | — | Telegram bot oqimi |
| `Attachment` | `attachment` | `AttachmentController` | `/api/v1/file/*` | barcha rasm ko'rsatuvchi sahifalar |
| — | — | `AdminController` | `/api/v1/admin/statistic` | `/admin/home` |

**Public sahifalar:** `/`, `/models`, `/bot/:userId`, `/data-form/:userId`,
`/history/:userId`, `/appeal/:userId`.

**Integratsiya nuqtalari** (yangi platforma eskisini qayta ishlatadi,
nusxalamaydi — ТЗ §89):

- `User` va `Role` **umumiy** — yangi RBAC shu jadvallar ustiga qurildi
  (`user_permission` qo'shildi, dublikat `User` yaratilmadi)
- `JwtService` va `MyFilter` **umumiy** — §61 tuzatishlari ikkala
  modulga ham tegishli
- Kasting anketasiga havola qilish yangi kontentdan mumkin
  (`InternalTargetType.CASTING`, §28)
- Kasting bildirishnomalari yangi bildirishnoma moduliga ulangan (§32)

**Regressiya qo'riqchilari:** `ExistingCastingRegressionTest` (7 test) va
`OldCastingFrozenTest` — eski endpointlar va jadvallar o'zgarmaganini
tekshiradi.


## 2.7. Qabul mezonlari — ТЗ §78–§83

| ТЗ | Mezon | Qayerda tekshiriladi |
|---|---|---|
| §78 | 8 ta RBAC bandi | `AcceptanceCriteriaTest` — raqamlangan, bandma-band |
| §78 | chuqur tekshiruv | `HyperAdminHierarchyTest`, `StaffManagementTest`, `RbacIntegrationTest`, `BackendAuthorizationTest`, `UserCannotEnterPanelTest`, `SidebarPermissionsTest` |
| §79 | 5 ta kontent turi + barcha maydonlar | `ContentStructureTest`, `ContentEditRoundTripTest`, `ContentClassificationTest` |
| §80 | Serial / mini serial tuzilishi | `ContentStructureTest` (SEASONAL fasl bilan, EPISODIC fasilsiz) |
| §81 | Reklama + CTR hisoboti | `AdAnalyticsTest`, `AdStatisticsEndpointTest`, `AdvertisementModuleTest` |
| §82 | Ijodkor yaratish, qidirish, biriktirish | `CreatorSelectionTest`, `ContentEditRoundTripTest` |
| §83 | Premium berish/bekor qilish + audit | `PremiumGiftTest`, `PremiumLifecycleTest`, `TariffModuleTest` |

⚠️ §78 uchun alohida fayl ochildi: har bir band boshqa testlarda
qamrab olingan edi, lekin **raqamlangan ro'yxat sifatida** hech qayerda
yig'ilmagandi. Buyurtmachi «shu sakkiztasi test qilinsin» deganda javob
bitta joydan ko'rinishi kerak.

### 2.4. Baseline build/test — kod o'zgartirishdan OLDIN

| Buyruq | Natija |
|---|---|
| `./mvnw -f backend/pom.xml -DskipTests package` | ✅ SUCCESS (jar 88 MB) |
| `./mvnw -f backend/pom.xml test` | ❌ **FAIL — pre-existing** |
| `npm install --legacy-peer-deps` (frontend) | ✅ SUCCESS |
| `CI=true react-scripts build` (frontend) | ✅ SUCCESS (ESLint warning'lar bilan) |

**Pre-existing issue:** `BackendApplicationTests.contextLoads` yiqiladi — test tirik
PostgreSQL (`localhost:5432`) talab qiladi, test profili va Testcontainers yo'q.
Bu **bizning o'zgarishimiz emas**, avvaldan shunday.

Frontend build o'tadi, lekin ~40 ta ESLint warning bor (`no-unused-vars`,
`react-hooks/exhaustive-deps`, `jsx-a11y/*`, `react/jsx-no-target-blank`) — pre-existing.

---

### 2.5. Miro doskasi va buyurtmachi talablari

⚠️ **Doskalar MCP orqali ochilmadi** (`uXjVHyaTTxc=` va `uXjVHw7AG28=` — «Board access
denied»; live-embed havolasi ham faqat JS qobiq qaytardi). Talablar buyurtmachining
xabaridagi doska mazmunidan olindi.

Quyidagilar ТЗ PDF'larida **yo'q** va yangi talab hisoblanadi:

| # | Talab | Qayerga tushdi |
|---|---|---|
| R1 | Kontent 2 formatda: **YouTube (yonlama)** va **Reels (tik)** | `ContentOrientation` enum |
| R2 | Home bo'limlari: Reels seriallar, Podkastlar, Shoular, Streamlar, Kliplar | `ContentType` + `HomepageSection` |
| R3 | **Mashhur ijodkorlar — eng pastda** | `HomepageSection.sortOrder` |
| R4 | Har bir home bo'limini admin paneldan **yoqish/o'chirish** | `HomepageSection.enabled` |
| R5 | Reklama **faqat faol tarifi yo'q** userlarga; admin xabarlari — hammaga | Ad `audienceType` |
| R6 | Reklama/premyera tugmasi va URL admin paneldan boshqariladi | `buttonEnabled`, `linkUrl` |
| R7 | **Bitta hisobdan max 2 qurilma**, begonasini chiqarish | `UserDevice` entity |
| R8 | User pul **balansi** + Stars + UzCasting Coin | `UserBalance` entity |
| R9 | Donat balansdan yechiladi; yetmasa — «hisobni to'ldirish» xabari | `DonationService` |
| R10 | Hisobni to'ldirish + to'lov/xaridlar tarixi | `TopUp`, `PurchaseHistory` |
| R11 | To'lov: **Click · Payme · Uzum bank · Stripe** (Visa/MC) | `PaymentProvider` |
| R12 | Bir martalik xarid — **casting loyihasiga kirish huquqi bermaydi**; Premium — beradi | `AccessService` |
| R13 | Kontent bo'yicha izoh/Stars/Coin sanog'i (16 / 56 / 586) | `ContentCounters` |
| R14 | OTP: 4–6 xona, 5 daqiqa, qayta yuborish 60 s dan keyin, 3 xato → 15 daqiqa blok | Auth moduli |
| R15 | Parol ≥8 belgi, katta harf + raqam; telefon `+998 XX XXX XX XX` | Validatsiya |
| R16 | Fayl saqlash — **Timeweb**; push — **Firebase FCM**; tillar UZ/RU/EN | `StorageService`, FCM |

### 2.6. Scope'dan chiqarilgan

Eski ТЗ'dagi «Ijtimoiy tarmoq / Messenger» konsepsiyasi (do'stlar, lenta, shaxsiy chat,
end-to-end shifrlash) **asosiy vazifa deb qabul qilinmadi**.

To'liq qaror va uning kodda qanday kafolatlanishi → yuqoridagi
**🎯 SOURCE OF TRUTH** bo'limi.


### Kontent tasnifi — uch mustaqil o'lchov (ТЗ §13)

**Qoida:** `content type`, `category` va `genre` bir xil narsa EMAS.

| O'lchov | Nima | Qayerda | Nechta |
|---|---|---|---|
| **Content type** | kontentning SHAKLI | `ContentType` enum | bittasi |
| **Category** | katalog BO'LIMI | `cms_category` jadval + tarjimalar | bittasi (ixtiyoriy) |
| **Genre** | USLUB | `cms_genre` + ko'p-ko'pga | bir nechta |

Buyurtmachi misoli: `MINI_SERIES` (tur) + Drama (kategoriya) + Romance (janr).

Model boshidan to'g'ri qurilgan va uch qavatda ham ajratilgan:
baza (`content_type` ustuni · `category_id` FK · `cms_content_genre`),
API (`contentType` · `categoryId` · `genreIds`), panel (uchta alohida maydon).

**Nega qo'riqchi kerak:** bu jimgina buzilishi mumkin. Eng ehtimolli yo'l —
kimdir «bir xil ro'yxatga o'xshaydi» deb kategoriyani enum'ga aylantirishi.
Natijada drama janridagi podkastni ifodalab bo'lmasdi.
`ContentClassificationTest` (8 test) — mutatsiya bilan tekshirilgan.

**Yo'l-yo'lakay topildi va tuzatildi:** dev seeder'da «Podkast»,
«Mini seriallar», «Intervyu» KATEGORIYA sifatida turgan edi — ular esa
`ContentType` qiymatlari. Model to'g'ri, lekin namuna ma'lumot noto'g'ri
naqsh o'rgatardi. Ular o'rniga haqiqiy katalog bo'limlari:
`uzbek` · `foreign` · `kids`.

---

### ⚠️ Bunny Stream — qaror kutilmoqda

Buyurtmachi 21.08.2026 da `tz/roadmap for bunny stream.md` va
`tz/roadmap for bunny stream another.md` qo'shdi. Ular video arxitekturasini
**tubdan boshqacha** belgilaydi:

> «Сервер — это турникет, а не труба. Ни один байт видео не проходит
> через наш VPS.»

| Bunny hujjati | Hozir qurilgani |
|---|---|
| Yuklash paneldan **to'g'ridan-to'g'ri Bunny'ga** (TUS) | bo'laklar **bizning serverdan** o'tadi |
| Ko'rish **Bunny CDN'dan**, imzolangan HLS | video **bizning serverdan**, `Range`/206 |
| Transkodlash + HLS Bunny'da | yo'q |
| Metadata'da `bunny_video_id` | `MediaAsset` + `storageKey` + lokal disk |

Hujjat boshqa stek nazarda tutadi (Java 21, Gradle, `uz.streaming`, Vite) —
bizda Java 17, Maven, `com.example.backend`, CRA. Ikkita qoida esa
allaqachon bajarilgan: PATCH yo'q, sirlar env'da.

**Qaror qabul qilinmaguncha video qismi o'zgartirilmaydi.**

---

### Kontent: status ≠ visibility ≠ language ≠ tarjima (ТЗ §15)

To'rt boshqa tushuncha, ular bir-birini almashtirmaydi:

| Maydon | Savol | Qiymatlar |
|---|---|---|
| `status` | hayot siklining qaysi bosqichi | DRAFT · IN_REVIEW · SCHEDULED · PUBLISHED · ARCHIVED · BLOCKED |
| `visibility` | kimga topiladi | PUBLIC · UNLISTED · PRIVATE |
| `language` | asar qaysi tilda suratga olingan | ISO 639-1: `uz` · `ko` · `tr`… |
| `*Translation` | sarlavha/tavsif qaysi tilda ko'rsatiladi | UZ · RU · EN |

**Misollar:**
- Nashr qilingan (`PUBLISHED`) film premyeradan oldin `UNLISTED` bo'lishi
  mumkin — havola bilan ochiladi, katalogda hali chiqmaydi.
- Koreys seriali `language = ko` bo'ladi, lekin sarlavhasi baribir uch
  tilda saqlanadi. Asl til tarjimalarga ta'sir qilmaydi.

`PRIVATE` — faqat panel xodimlari. Oddiy foydalanuvchi havola bilan ham
ocholmaydi (`AccessService`). `UNLISTED` esa ochiladi.

⚠️ Tahrirlashda `visibility` berilmasa **eskisi saqlanadi** — sarlavhani
tuzatish kontentni tasodifan katalogga chiqarib yubormasin.

### Uch tillilik — qamrov va MAJBURIYLIK

11 ta tarjima jadvali barcha matnli entitylarni qamraydi: kontent, qism,
fasl, kategoriya, janr, ijodkor, reklama, premyera, bildirishnoma, bosh
sahifa bo'limi, tarif. Har birida `UNIQUE(parent_id, locale)`.

Tarjimasiz yagona matn — `PlatformSetting.description`, u admin uchun ichki
izoh va foydalanuvchiga ko'rinmaydi.

**Buyurtmachi talabi:** «hamma ma'lumot 3 ta tilda qo'shilishi kerak».
Bu endi kodda MAJBURLANADI (`TranslationRules`):

| Holat | Talab |
|---|---|
| `DRAFT` · `IN_REVIEW` | o'zbekchasi yetarli |
| `PUBLISHED` · `SCHEDULED` | **UZ + RU + EN majburiy** |
| Faol kategoriya / janr / ijodkor | **UZ + RU + EN majburiy** |
| Faolsizlantirilgan | o'zbekchasi yetarli |

**Nega saqlashda emas, nashrda.** Ikkalasida ham majburiy qilsak, admin
qoralamani ham saqlay olmasdi — kontent odatda bitta tilda yoziladi, keyin
tarjima qilinadi. Har saqlashda uchala tilni talab qilish odamlarni bo'sh
joyga nuqta yozishga majbur qilardi, ya'ni qoida amalda buzilardi.

Natija: **bazada tarjimasiz nashr qilingan kontent bo'lmaydi**, lekin ish
jarayoni ham to'xtamaydi.

`SCHEDULED` ham majburiy ro'yxatda: u belgilangan vaqtda avtomatik
`PUBLISHED` bo'ladi va o'shanda tarjima yo'qligi bilinardi — kech bo'lardi.

Xato xabarida AYNAN qaysi til yetishmayotgani sanaladi.

⚠️ Hozircha **sarlavha** majburiy, tavsif emas: sarlavha ro'yxat va
menyularda chiqadi, uning yo'qligi bo'sh katak beradi. Tavsifni ham
qo'shish kerak bo'lsa — `TranslationRules.requireAll` ga bitta maydon
qo'shiladi.

`ThreeLanguageRuleTest` (11 test) qo'riqlaydi.

---

### Backend avtorizatsiyasi — ikki qavat

> Frontend menyusida elementni yashirish **xavfsizlik emas**. Barcha
> tekshiruv backendda, va u panel umuman ishtirok etmaydigan HTTP
> so'rovlar bilan sinaladi.

**Qavat 1 — Spring Security (bazaviy rol).**
`/api/v1/app/admin/**` uchun `hasAnyAuthority(ROLE_GIPERSUPERADMIN,
ROLE_SUPERADMIN, ROLE_ADMIN, ROLE_WORKER)`. USER tokeni bu makonga
**routing'dan oldin** to'xtatiladi.

Nega kerak: ichkaridagi ruxsat tekshiruvi *yozilishi* kerak. Kimdir yangi
endpoint qo'shib uni yozishni unutsa, ilgari `/api/**` faqat
autentifikatsiya talab qilardi — oddiy USER tokeni bilan yetib borish
mumkin edi. Endi bu avtomatik yopiq, endpoint qanday yozilishidan qat'i
nazar.

**Qavat 2 — ruxsat va ierarxiya (endpoint darajasida).**
`@RequirePermission` + `PermissionInterceptor` (so'rov tanasidan oldin),
`PermissionAspect` (servis qatlami uchun), va `canCreateRole`/`canManageUser`
ierarxiya tekshiruvlari.

**Nega ruxsatlar Spring Security authority'lariga chiqarilmadi:**
ular har so'rovda bazadan o'qiladi — shuning uchun ruxsat olib tashlansa
mavjud token darhol kuchsizlanadi. Ularni token yoki authority sifatida
keshlash bu xossani yo'qotardi, xavfsizlikka esa hech nima qo'shmasdi.

**Amalda tekshirilgan — 77 ta admin endpoint × 6 rol:**

| Rol | To'xtatildi | O'tdi |
|---|---|---|
| USER | 76 | 1 — faqat `auth/login` (u ataylab ochiq) |
| WORKER (4 ta ko'rish ruxsati) | 65 | 12 — faqat ruxsati bor joylari |
| WORKER (13 ta ruxsat) | 0 | 77 |
| ADMIN · SUPER_ADMIN · HYPER_ADMIN | 0 | 77 |

Cheklangan WORKER ruxsati **yo'q** biror joydan o'tmadi. Ruxsatga bog'liq
misol: `POST /media` — `MEDIA_UPLOAD` bo'lmasa **403**, bo'lsa **200**.

**Qo'riqchilar:**

| Test | Nimani ushlaydi |
|---|---|
| `AdminEndpointGuardTest` | Qo'riqlanmagan endpoint qo'shib bo'lmaydi (build vaqtida) |
| `BackendAuthorizationTest` (9) | Ikkala qavat va 6 xil huquq oshirish urinishi |
| `RbacIntegrationTest` (7) | Rollar HTTP darajasida |
| `HyperAdminHierarchyTest` (9) · `SuperAdminScopeTest` (7) | Rol doiralari |

### Rol ierarxiyasi — HYPER_ADMIN qarori

**Savol:** HYPER_ADMIN boshqa HYPER_ADMIN yarata olsinmi?

**Qaror: YO'Q.** Qoida butun ierarxiyada bir xil — faqat o'zidan **qat'iy
quyi** rolni yaratish mumkin.

**Nega:** `canManage` qat'iy taqqoslash ishlatadi (`this.level > other.level`).
Ikkita HYPER_ADMIN bir-birini **boshqara olmaydi**: na o'chirish, na rolini
pasaytirish. Agar teng rol yaratishga ruxsat berilsa, bitta o'g'irlangan
hisob cheksiz «abadiy» HYPER_ADMIN yaratardi va ularni **hech kim** — hatto
dastlabki egasi ham — olib tashlay olmasdi. Bir martalik buzilish doimiy va
qaytarib bo'lmas nazoratga aylanardi.

**Yagona HYPER_ADMIN yo'qolsa:** tiklash ilova ichida emas, **serverda** —
`APP_GIPERSUPERADMIN_PHONE` va `APP_GIPERSUPERADMIN_PASSWORD` environment
o'zgaruvchilari berilib, ilova qayta ishga tushiriladi. Parol
`BootstrapPasswordPolicy` talabidan o'tishi shart (B18).

Bu ataylab qiyinroq yo'l: serverga kirish huquqini talab qiladi, ya'ni
veb-interfeys orqali huquq oshirib bo'lmaydi.

### Ko'rish va boshqarish — AJRATILGAN

Talab: HYPER_ADMIN **barcha staff hisoblarini ko'rishi** kerak.

Ilgari u boshqa HYPER_ADMIN hisobini **umuman ko'ra olmasdi** — ro'yxat
faqat qat'iy quyi rollarni chiqarardi. Ya'ni `AutoRun` yaratgan master hisob
hamma narsaga qodir bo'lib, hech kimning ro'yxatida ko'rinmasdi. Bu amalda
**backdoor** edi: uni audit qilish ham, mavjudligini bilish ham mumkin emasdi.

Endi:

| | HYPER_ADMIN | quyi rollar |
|---|---|---|
| **Ko'radi** | barcha xodimlarni (o'zini va teng rolni ham) | faqat quyi rollarni |
| **Boshqaradi** | faqat quyi rollarni | faqat quyi rollarni |

Javobdagi `manageable` bayrog'i panelga tugmalarni faolsizlantirishga xizmat
qiladi. ⚠️ U **faqat interfeys uchun** — haqiqiy tekshiruv har bir amalda
backendda bajariladi.

Ko'rish huquqi boshqarish huquqini **bermaydi**, shuning uchun bu huquq
oshirish yo'li ochmaydi.

`HyperAdminHierarchyTest` (9 test) shu qarorni HTTP darajasida qo'riqlaydi.

---

## 3. Architecture Decisions

Batafsil sabablar → §13 Important Decisions.

| # | Qaror |
|---|---|
| D1 | Mavjud `backend/` va `frontend/` papkalari saqlanadi, ko'chirilmaydi |
| D2 | Spring Boot 3.1.2 / Java 17 davom ettiriladi |
| D3 | Frontend **JavaScript**da qoladi — TypeScript'ga rewrite qilinmaydi |
| D4 | **Flyway** kiritiladi (ikkalasi ham yo'q edi), `baseline-on-migrate` bilan |
| D5 | Mavjud `UserRoles` enum kengaytiriladi, yangi role tizimi yaratilmaydi |
| D6 | `Permission` — alohida entity, WORKER uchun fine-grained |
| D7 | Yangi admin API `/api/v1/app/admin/**`, mavjud `/api/v1/**` tegilmaydi |
| D8 | Pul — `BigDecimal`, hech qachon `double`/`float` emas |
| D9 | ID strategiyasi entity bo'yicha saqlanadi (User=UUID, CastingUser=Integer) |
| D10 | Yangi `MediaAsset` entity; `Attachment` casting moduli uchun tegilmaydi |
| D11 | Admin UI mavjud `src/admin/` ichida emas, yangi `src/adminpanel/` da |
| D12 | **Ko'p tillilik: alohida tarjima jadvallari**, JSON ustun emas |
| D13 | **Media til bo'yicha ixtiyoriy**: `locale = null` → umumiy, `RU` → faqat ruslarga |
| D14 | Admin panel — **to'q ko'k**; mobil ilova ТЗ palitrasida qoladi |
| D15 | Dev muhiti — **H2 fayl bazasi**, lokal PostgreSQL'ga tegilmaydi |
| D16 | CMS alohida `Cms.*` paketida, mavjud `Entity`/`Repository` aralashtirilmaydi |
| D17 | **Flyway** — `baseline-on-migrate`, mavjud production sxemasi V1 sifatida qayd etiladi |
| D18 | Migratsiyalarda **enum check constraint YO'Q** — har bir yangi qiymat migration talab qilardi |
| D19 | Ochiq katalog vitrina DTO'si — shaxsiy ma'lumot chiqmaydi |
| D20 | Bosh sahifa klientda qotirilmaydi — `cms_homepage_section` dan quriladi |
| D21 | Mobil foydalanuvchi holati `cms_user_account` da — `users` jadvaliga tegilmaydi |
| D22 | Narx va kurslar `cms_platform_setting` da — deploy kutmasdan o'zgaradi |
| D23 | Bildirishnoma yuborilmasa ham urinish SAQLANADI, HTTP 503 qaytadi |
| D24 | Analitika ikki qatlamli: xom hodisa → fon vazifasi → kunlik jamlanma |
| D25 | «Unikal» = kunlik unikallar yig'indisi, davr bo'yicha distinct emas |
| D26 | Grafik uchun kutubxona qo'shilmadi — inline SVG yetarli |

---

## 3.1. Ko'p tillilik arxitekturasi

Buyurtmachi talabi: **barcha kontent va menyular uch tilda** (UZ/RU/EN), va
«hatto rasmlarni ham ayrimini 3 til uchun maxsus yuklaydi».

### Matnlar — alohida tarjima jadvallari

```
cms_content            ← tilga bog'liq BO'LMAGAN maydonlar
  id, slug, content_type, status, access_policy, premiere_price, ...

cms_content_translation
  content_id, locale (UZ|RU|EN)   ← UNIQUE(content_id, locale)
  title, short_description, description
```

Xuddi shu naqsh: `cms_category_translation`, `cms_genre_translation`,
`cms_creator_translation`, `cms_season_translation`, `cms_episode_translation`.

**Nega JSON ustun emas:** tilga qarab qidirish (`WHERE title LIKE ...`) va
indekslash kerak. Ruscha qidiruv ruscha sarlavhani topishi shart — bu brauzerda
tekshirildi va ishlaydi.

**Nega slug tarjima qilinmaydi:** u barqaror identifikator, URL va havolalar
unga bog'lanadi.

### Media — til bo'yicha IXTIYORIY

```
cms_content_media
  content_id, role (POSTER|COVER|TRAILER|GALLERY|...), media_id, sort_order
  locale   ← NULL = barcha tillar uchun; RU = faqat rus tilida
```

Tanlash qoidasi: avval aniq til uchun fayl qidiriladi, topilmasa `locale = NULL`
olinadi. Ya'ni **har bir til uchun alohida afisha yuklash mumkin, lekin majburiy emas**.

Xuddi shu mexanizm `cms_episode_video.locale` da — dublyaj tili uchun.

### Interfeys tili

Admin panelda `src/adminpanel/i18n.js` — 100+ kalit, uch tilda. Mavjud
`src/i18next.js` (sayt uchun) ga tegilmaydi: kalitlar to'plami boshqa.
Til tanlovi `localStorage` da saqlanadi va kontent tarjimasiga ham ta'sir qiladi.

---

## 4. Development Phases

| Phase | Nomi | Status |
|---|---|---|
| 0 | Audit va hujjatlar | `[x]` DONE |
| 1 | Core architecture — RBAC, permissions, auth, audit, layout | `[x]` DONE |
| 2 | Staff management — Hyper/Super/Admin/Worker | `[x]` backend to'liq (ТЗ §12); panel keyin |
| 3 | CMS foundation — Category, Genre, Creator, Media Library | `[x]` model + ro'yxat + CRUD + media yuklash |
| 4 | Content — Content/Season/Episode/VideoPart/Access | `[x]` to'liq: kontent, fasl, qism, video segmentlar |
| 5 | Homepage — sections, Ads, Premieres, Featured | `[x]` bo'limlar, reklama, premyeralar, **ochiq `/app/home` feed** (ТЗ §29–§31) |
| 6 | Engagement — Comments, Notifications | `[x]` moderatsiya + bildirishnoma: rejalashtirish ishlaydi, hisobot halol (ТЗ §32–§33). FCM ulanmagan |
| 7 | Users & Monetization — tariffs, premium, Stars, Coin | `[x]` foydalanuvchi, tarif, balans, qurilma, donat |
| 8 | Analytics — events, aggregation, dashboard, reports | `[x]` ikki qatlamli: xom hodisa + kunlik jamlanma |
| 9 | Hardening — tests, performance, security, indexes | `[~]` 693 test; migratsiyalar V1–V26 |

---

## 5. Current Tasks

PHASE 1 — Core Architecture:

- `[x]` Repozitoriy auditi
- `[x]` Baseline build/test
- `[x]` roadmap.md, BACKEND_ROADMAP.md, FRONTEND_ROADMAP.md, ARCHITECTURE.md × 2
- `[x]` `UserRoles` — `ROLE_WORKER` qo'shildi, meros rollari `@Deprecated`
- `[x]` `PlatformRole` enum — ierarxiya, `canCreate`, `canManage`
- `[x]` `RoleMapper` — `UserRoles` ↔ `PlatformRole`
- `[x]` `Permission` enum (33 ta ruxsat)
- `[x]` `UserPermission` entity + `UserPermissionRepo`
- `[x]` `PermissionService` + `PermissionServiceImpl`
- `[x]` `ApiError` + `BusinessException` + `GlobalExceptionHandler`
- `[x]` `AuditLog` entity + `AuditLogRepo` + `AuditService` + `AuditAction`
- `[x]` `AutoRun` — `ROLE_WORKER` seed
- `[x]` `SecurityConfig` qayta yozildi — ochiq ro'yxat + `/api/**` yopiq
- `[x]` `RestAuthErrorHandler` — 401/403 JSON formatda
- `[x]` `STATELESS` sessiya (JWT uchun to'g'ri)
- `[x]` Test profili (H2) — `contextLoads` tiklandi
- `[x]` `SecurityRulesTest` — 19 ta kirish nazorati testi
- `[x]` **CMS modeli** — 15 entity, uch tilli tarjimalar bilan
- `[x]` **Dev muhiti** — H2 profil + mock data seeder (7 kategoriya, 8 janr, 8 ijodkor, 12 kontent, 12 qism, 111 media)
- `[x]` **Admin API** — login, /me, dashboard, content, creators, categories, genres, media, staff
- `[x]` **Admin panel UI** — to'q ko'k, 3 til, 7 sahifa, responsiv CSS
- `[x]` **Brauzerda tekshirildi** — 5 roldan kirish, ruxsat filtri, til almashish, tilga xos afisha
- `[x]` **Secret'lar env'ga ko'chirildi** — DB paroli va JWT kaliti kodda qolmadi
- `[x]` **RBAC unit testlari** — `PlatformRoleTest` (14) + `PermissionServiceTest` (12)
- `[x]` **Kontent CRUD** — 6 bo'limli muharrir, uch til, tilga xos afisha, optimistic locking
- `[x]` **Kategoriya / janr / ijodkor CRUD** — modal formalar, uch til
- `[x]` **Media yuklash** — `MediaPicker`, progress bar, oqimli saqlash
- `[x]` `SlugGenerator` + testlari (kirill translitеratsiya, o'zbek apostrofi)
- `[x]` **Fasl va qism muharriri** — SEASONAL/EPISODIC/SINGLE tuzilishlari
- `[x]` **Video segmentlar** — bir qismda bir nechta fayl + dublyaj tili
- `[x]` Tuzilish qoidalari server tomonda tekshiriladi (7 ta qoida)
- `[x]` `ContentStructureTest` — ТЗ §80 qabul mezoni, 11 test
- `[ ]` Flyway + `V1__baseline.sql` ← **keyingi ish**
- `[ ]` `@RequirePermission` annotation + aspect

---

## 6. Completed Tasks

- `[x]` **PHASE 0 — AUDIT** (19.08.2026)
  - `[x]` Repozitoriy strukturasi aniqlandi (backend/frontend/mobile/tz/tools)
  - `[x]` Backend stack: Spring Boot 3.1.2, Java 17, PostgreSQL, JWT, Maven
  - `[x]` Frontend stack: React 18.3 CRA, JavaScript, react-router v6, axios wrapper
  - `[x]` Mavjud entity, controller, service, security kodi o'qildi
  - `[x]` Mavjud casting moduli xaritalandi (§75)
  - `[x]` Baseline build/test bajarildi, pre-existing xatolar qayd etildi
  - `[x]` 5 ta roadmap/architecture fayli yaratildi
  - `[x]` Miro doskasi mazmunidan 16 ta yangi talab qayd etildi (§2.5)

- `[~]` **PHASE 1 — CORE ARCHITECTURE** (19.08.2026, davom etmoqda)
  - `[x]` Rol ierarxiyasi: `PlatformRole` + `RoleMapper` + `ROLE_WORKER`
  - `[x]` Ruxsat tizimi: `Permission` + `UserPermission` + `PermissionService`
  - `[x]` Xato formati: `ApiError` + `BusinessException` + `GlobalExceptionHandler`
  - `[x]` Audit: `AuditLog` + `AuditService` + `AuditAction`
  - `[x]` Backend `package` build ✅ o'tdi
  - `[x]` **B1 yopildi** — `SecurityConfig` qayta yozildi, butun API ochiqligi tugadi
  - `[x]` `RestAuthErrorHandler` — 401/403 uchun JSON javob
  - `[x]` **B8 yopildi** — test profili (H2), `contextLoads` ishlaydi
  - `[x]` `SecurityRulesTest` — 21 ta test, `package` testlar bilan o'tadi

- `[~]` **PHASE 3–4 — CMS ASOSI** (19.08.2026)
  - `[x]` 9 ta enum: `Locale`, `ContentType`, `StructureType`, `ContentOrientation`,
    `PublicationStatus`, `AccessPolicy`, `MediaType`, `MediaRole`, `CreatorProfession`
  - `[x]` 15 ta entity: Category/Genre/Creator/Content/Season/Episode + tarjimalar +
    `ContentMedia`, `ContentCredit`, `EpisodeVideo`, `MediaAsset`
  - `[x]` Uch tilli tarjima jadvallari (D12), tilga bog'liq media (D13)
  - `[x]` `StorageService` abstraksiyasi + `LocalStorageService` (path traversal himoyasi)
  - `[x]` 7 ta repozitoriy, N+1 uchun `@EntityGraph`
  - `[x]` `BackendApplication` — CMS paketlari skanerlashga qo'shildi
  - `[x]` `spring-boot-starter-validation` qo'shildi (pom'da yo'q edi)
  - `[x]` Dev profil (H2 fayl) + `DevDataSeeder` + `DevMediaFactory` (rasmlar kod bilan chiziladi)
  - `[x]` Admin API: `AdminAuthController`, `CmsCatalogController`, `MediaController`,
    `DashboardController`, `StaffController`
  - `[x]` Admin panel: 7 sahifa, to'q ko'k mavzu, 3 til, holatlar (loading/empty/error/403)

---

## 7. Pending Tasks

PHASE 2–9 tasklari `BACKEND_ROADMAP.md` va `FRONTEND_ROADMAP.md` ichida batafsil.

---

## 8. Bugs / Technical Debt

Tartib — xavflilik darajasi bo'yicha.

### `[x]` B1 — Butun API himoyasiz — **TUZATILDI 19.08.2026**

Avval `SecurityConfig` da `GET/POST/PUT/DELETE "/**" permitAll` turgan — butun API,
jumladan `DELETE` va admin amallari, tokensiz ochiq edi.

**Yangi tamoyil:** ochiq endpointlar aniq sanaladi, qolgan barcha `/api/**` yopiq.
Kelajakdagi har qanday yangi endpoint sukut bo'yicha himoyalangan.

Ochiq qoldirildi (klientlar sinmasligi uchun):
`POST /auth/login|google|refresh` · `GET /news`, `/news/{id}` · `GET /file/getFile/**` ·
`POST /file/upload` · `GET /casting-user/web` · `POST /casting-user` ·
`GET /casting-user/my/**` · `GET /casting-user/appeal/**` · SPA marshrutlari

⚠️ `POST /file/upload` va `POST /casting-user` — **yozish amallari, lekin ochiq**.
Telegram bot foydalanuvchisi tizimga kirmasdan anketa va rasm yuboradi. Yopilsa bot sinadi.
Kelajakda bot uchun imzolangan token yoki rate limiting kerak.

Yopildi: `GET /casting-user` (to'liq ro'yxat) · `payed/**` · `PUT status|price|web-show` ·
`DELETE /casting-user/**` · `PUT /file/**` · `POST|PUT|DELETE /news` · `/admin/**` ·
`/security` · `/auth/decode` · **`PUT /auth/password/**`** (istalgan admin hisobini
egallab olish mumkin edi) · `/api/v1/app/admin/**`

Qo'shildi: `RestAuthErrorHandler` — 401/403 uchun JSON javob; `STATELESS` sessiya.
Tekshiruv: `SecurityRulesTest` — 19 ta test, ochiq va yopiq qoidalarni ikki tomondan qo'riqlaydi.

### `[~]` B2 — Ochiq endpoint shaxsiy ma'lumot qaytarardi — **ASOSIY QISMI YOPILDI**

`GET /casting-user/web` endi `CastingUserPublicDto` qaytaradi: telefon, email,
telegram, facebook, instagram, telegramId va tana o'lchovlari (bust/waist/son),
kiyim/oyoq o'lchami, narx va status **umuman chiqmaydi**. Xom matnda ham
tekshirildi. `PublicCatalogPrivacyTest` — 2 test, regressiyani qo'riqlaydi.

Klientlar sinmadi: sayt katalogi 6 maydon, mobil ilova 13 maydon ishlatadi —
hammasi DTO'da bor.

**Qolgan qism (past darajadagi risk):**

| Endpoint | Holat |
|---|---|
| `GET /casting-user` (to'liq ro'yxat) | ✅ token talab qiladi (B1) |
| `GET /casting-user/web` (katalog) | ✅ vitrina DTO'si, PD yo'q |
| `GET /casting-user/appeal/{id}` | ⚠️ **ochiq va to'liq anketani qaytaradi** |
| `GET /casting-user/my/{telegramId}` | ⚠️ **ochiq va to'liq anketani qaytaradi** |

Oxirgi ikkitasini Telegram bot WebApp'i ishlatadi va u yerda foydalanuvchi
tizimga KIRMAYDI — ya'ni ularni yopish bot oqimini sindiradi. Bundan tashqari
foydalanuvchi u yerda **o'z anketasini** ko'radi, ya'ni ma'lumot unga tegishli.
Risk: `id` ketma-ket butun son, ya'ni birov boshqasining anketasini ochib olishi
mumkin.

Yechim (alohida task, bot jamoasi bilan): manzilda `telegramId` ni ham talab
qilish va anketaning egasi bilan solishtirish, yoki bot yuboradigan imzolangan
`initData` ni tekshirish. Ikkalasi ham bot tomonini o'zgartirishni talab qiladi.

`birthday` katalogda qoldirildi: sayt undan yoshni hisoblaydi. Uni olib tashlash
uchun avval klientlarni `age` ga o'tkazish kerak.

### `[!]` B3 — Secret'lar repozitoriyda (CRITICAL)

- `application.properties`: `spring.datasource.password = root` — commit qilingan
- `JwtService.java:26`: JWT secret default qiymati kodda qotirilgan
- `AutoRun.java`: default parol `00000000`, yashirin master hisoblar

### `[x]` B4 — Frontend admin guard ishlamasdi — **TUZATILDI 20.08.2026**

Ikkita xato bor edi:
1. `blockedPages = ["/dashboard"]` — bunday marshrut yo'q, ya'ni tekshiruv
   **hech qachon ishlamagan**. Endi `/aadmin` va `/admin`.
2. Rol tekshiruvi `if (res?.error)` ichida turgan — ya'ni faqat XATO bo'lganda
   bajarilardi, muvaffaqiyatli javobda hech qachon. Endi to'g'ri joyda.

Konstantalar komponentdan tashqariga chiqarildi — `useEffect` har renderda
qayta ishga tushmasin. Brauzerda tekshirildi: tokensiz `/aadmin/casting-users/web`
→ `/aadmin/login`.

### `[~]` B5 — `UserRoles` enum boshqa loyihadan qolgan (qisman)

`ROLE_REKTOR`, `ROLE_STUDENT`, `ROLE_TEACHER`, `ROLE_DEKAN` — universitet loyihasidan.

✅ `ROLE_WORKER` qo'shildi, meros rollari `@Deprecated`.
✅ `SecurityConfig` dagi qoldiq route'lar (`/student`, `/superadmin/**`, `/groups/**`,
`/subject/`) olib tashlandi.
⬜ Enum qiymatlarining o'zi DB'da qolmoqda — o'chirish mavjud satrlarni buzadi.

### `[x]` B6 — Migration tizimi yo'q — **TUZATILDI 20.08.2026**

**Flyway** ulandi, `ddl-auto=none`. To'rtta migratsiya:

| Fayl | Mazmuni |
|---|---|
| `V1__baseline.sql` | mavjud production sxemasi (9 jadval) |
| `V2__rbac_and_audit.sql` | `user_permission`, `audit_log` |
| `V3__cms.sql` | kategoriya, janr, ijodkor, kontent, fasl, qism, media |
| `V4__homepage_ads_premieres.sql` | bosh sahifa bo'limlari, reklama, premyeralar |

`baseline-on-migrate=true`: mavjud bazada V1 **bajarilmaydi**, faqat qayd etiladi.
Bu **haqiqiy baza ustida sinaldi** — eski foydalanuvchilar saqlanib qoldi,
faqat yangi jadvallar qo'shildi.

Migratsiyalar dev va test profillarida ham ishlaydi, ya'ni har bir build ularni
haqiqatda ishga tushiradi.

⚠️ **Enum check constraint'lari ataylab olib tashlandi** (32 ta): Hibernate ularni
har bir enum ustuni uchun yasaydi va enum'ga bitta qiymat qo'shilishi bilan
constraint eskiradi. ТЗ bo'yicha aynan shu enum'lar o'sishi kutilmoqda.

### `[ ]` B7 — `Role.id` auto-generate emas

Qo'lda beriladi (`AutoRun.ensureRole` max+1 hisoblaydi) — race condition ehtimoli.

### `[x]` B8 — Backend testi pre-existing FAIL — **TUZATILDI 19.08.2026**

`contextLoads` tirik PostgreSQL talab qilardi. Endi `test` profili xotiradagi H2 beradi
(`src/test/resources/application-test.properties`). `./mvnw package` testlar bilan o'tadi.

### `[ ]` B9 — Media storage abstraction yo'q

`Attachment.createAttachment()` — entity ichida file I/O, nisbiy path `backend/files`,
faqat repozitoriy root'idan ishlaydi. Provider almashtirib bo'lmaydi.

### `[x]` B11 — `EnumSet.copyOf` bo'sh to'plamda yiqilardi — **TUZATILDI**

`PermissionServiceImpl.replacePermissions` da: ruxsatsiz WORKER yaratmoqchi
bo'lsangiz `IllegalArgumentException: Collection is empty` chiqardi va butun
so'rov yiqilardi. Unit test topdi.

### `[x]` B12 — Tarjima yangilanganda unikal cheklov buzilardi — **TUZATILDI**

`clear()` + qayta qo'shish naqshi: Hibernate bitta flush ichida eski satrni
o'chirishdan OLDIN yangisini qo'shardi va `UNIQUE(parent_id, locale)` ni buzardi.
Endi tarjimalar **joyida yangilanadi**. Brauzerdagi sinov topdi.

### `[x]` B13 — Tahrirlashda slug jim o'zgarardi — **TUZATILDI**

Kategoriya nomini o'zgartirsangiz slug ham qayta yasalardi (`drama` →
`drama-tahrirlangan`), ya'ni unga bog'langan havolalar sinardi. Endi slug faqat
yaratishda yoki ataylab berilganda o'zgaradi.

### `[x]` B15 — Pullik video ochiq edi — **TUZATILDI 20.08.2026**

Eng jiddiy topilma. `GET /api/v1/app/media/{id}/raw` `permitAll` edi va **fayl
turiga qaramasdan** hamma narsani berardi. Rasm uchun bu to'g'ri (afisha
baribir hammaga ko'rinadi), lekin qism videolari ham aynan shu endpointdan
uzatiladi.

Natijada: `AccessService` klientga «sotib oling» deb turgan paytda, o'sha
video id ni terib bemalol yuklab olsa bo'lardi. Ya'ni butun monetizatsiya
faqat klient halolligiga tayanardi.

**Sabab:** Spring Security URL ko'radi, fayl TURINI bilmaydi — u bazadan
aniqlanadi. Shuning uchun bu qatlamda hal qilib bo'lmasdi.

**Yechim:** endpoint ichida `AccessService.canReadMedia`:
rasm — ochiq; qismga bog'langan video — `canWatch` qaroriga bo'ysunadi;
biriktirilmagan video — faqat panel xodimiga. Ruxsat yo'q bo'lsa **404**
(403 emas: faylning bor-yo'qligini ham oshkor qilmaymiz).

Pullik videoga `Cache-Control: no-store` — obuna tugagach keshdan
ochilib ketmasligi uchun.

`PaidContentLeakTest` (7 test) qo'riqlaydi va mutatsiya bilan tekshirilgan.

### `[x]` B14 — Analitika endpointida rate limiting yo'q — **TUZATILDI 20.08.2026**

`POST /api/v1/app/analytics/events` ochiq: reklama ko'rsatilishi tizimga kirmagan
foydalanuvchida ham qayd etilishi kerak edi, ya'ni soxta ko'rsatkich yuborish
mumkin edi.

`RateLimitFilter` Spring Security zanjiridan OLDIN turadi
(`HIGHEST_PRECEDENCE + 10`), xotirada token-bucket. Analitika 60/daqiqa,
login 10/daqiqa, anketa 20/daqiqa, yuklash 30/daqiqa. Limit oshsa —
429 + `Retry-After`.

⚠️ Xotirada, ya'ni bitta instansiya uchun. Bir nechta instansiya bo'lsa
Redis kerak bo'ladi.

### `[x]` B25 — Yangi sozlamani o'zgartirib bo'lmasdi — **TUZATILDI 21.08.2026**

B24 bilan bir xil sinf. `SettingsService.update()` bazada satr bo'lmasa
shartsiz **404** qaytarardi. Sozlamalar esa faqat admin «Sozlamalar»
sahifasini ochganda yaratilardi.

Natijada kodga yangi sozlama qo'shilganda uni o'zgartirib bo'lmasdi —
`homepage.creators.ranking` ni qo'shganda aynan shunga duch keldim.

**Yechim:** satr yo'q bo'lsa-yu kalit `SettingKeys` da E'LON QILINGAN
bo'lsa — yaratiladi. Noma'lum kalit baribir rad etiladi, aks holda admin
xato yozib, hech kim o'qimaydigan satr yaratib qo'yardi.

### `[x]` B24 — Qism narxi 0 so'm bo'lib qolardi — **TUZATILDI 21.08.2026**

ТЗ §23: «Bitta seriyani sotib olish default narxi 3 000 UZS. Lekin bu kod
ichida hardcoded bo'lmasin. Admin settings orqali o'zgartirish mumkin
bo'lsin.»

Narx haqiqatan sozlamada edi, LEKIN sozlamalar faqat
`SettingsService.all()` chaqirilganda yozilardi — u esa admin
«Sozlamalar» sahifasini ochganda ishlaydi.

Ya'ni yangi o'rnatishda `cms_platform_setting` **bo'sh** bo'lib turardi va
`getMoney(EPISODE_PRICE)` zaxira sifatida `"0"` qaytarardi. Natijada narxi
alohida ko'rsatilmagan pullik qism foydalanuvchiga **0 so'm** deb
ko'rinardi.

**Ikki qavatli yechim:**

1. `V12__seed_platform_settings.sql` — satrlarni yaratadi, admin panelida
   darhol ko'rinadi. `where not exists` bilan: admin o'zgartirgan qiymat
   bosib o'tilmaydi.
2. `SettingsService.get(key)` — zaxira endi `SettingKeys.defaultValue()`
   dan olinadi, ya'ni satr yo'q bo'lsa ham kod e'lon qilgan qiymat
   ishlaydi.

Amalda tekshirildi (qayta ishga tushirmasdan): 3000 → sozlama 7500 →
javob 7500 → 3000 ga qaytarilganda javob 3000.

⚠️ **Pullik SINGLE kontent uchun narx baribir MAJBURIY** — u zaxiraga
qoldirilmaydi. Sabab: film sotiladigan yagona narsa va uning narxi juda
xilma-xil; global standartga qoldirish jimgina noto'g'ri narx qo'yishga
olib kelardi. Serialda esa premyera narxi ixtiyoriy — asosiy savdo
qismlar bo'yicha ketadi.

### `[x]` B23 — Tahrirlash VIDEO va TREYLERNI o'chirardi — **TUZATILDI 21.08.2026**

B17 ning kengroq ko'rinishi. `ContentService.update` media ro'yxatini
BUTUNLAY almashtiradi (`getMedia().clear()`), `ContentListDto` esa faqat
**uchta** rolni qaytarardi: POSTER, COVER, GALLERY.

Natijada muharrir orqali kontentni tahrirlash `VIDEO`, `TRAILER`,
`TEASER` va `THUMBNAIL` bog'lanishlarini jimgina o'chirardi — ya'ni
filmning asosiy videosi yo'qolardi.

**Yechim:** DTO endi BARCHA media bog'lanishlarini xom ko'rinishda
qaytaradi (`media[]` — role · locale · mediaId · sortOrder). Qulaylik
maydonlari (`posterMediaId`, `gallery`…) o'z joyida qoladi.

Test `MediaRole.values().length` bo'yicha tekshiradi — ya'ni kelajakda
yangi rol qo'shilsa, DTO'ni yangilash esdan chiqsa darhol bilinadi.

### `[x]` B22 — SINGLE kontentda VIDEO saqlanmasdi — **TUZATILDI 21.08.2026**

**Filmni umuman tomosha qilib bo'lmasdi.**

SINGLE tuzilmada qism bo'lmaydi (ТЗ §14), demak `EpisodeVideo` ham yo'q.
Asosiy videoni saqlaydigan joy umuman mavjud emasdi:

| Joy | Nima saqlaydi |
|---|---|
| `ContentMedia` | POSTER · COVER · THUMBNAIL · TRAILER · TEASER · GALLERY — **asosiy video yo'q** |
| `EpisodeVideo` | qism videosi — **SINGLE da qism yo'q** |
| `WatchController` | faqat `{episodeId}` — **SINGLE da episode yo'q** |

Dev bazasida 7 ta SINGLE kontent bor edi (MOVIE, SHORT_FILM, CLIP, SHOW,
STREAM) — hech biriga video biriktirib bo'lmasdi.

**Yechim** ТЗ §22 (Step 2 — Media, «videos») ga asoslanadi:

- `MediaRole.VIDEO` qo'shildi — video `ContentMedia` da yotadi
  (`sortOrder` = segment tartibi §19, `locale` = dublyaj tili);
- `AccessService.canWatch(User, Content)` — SINGLE uchun entitlement.
  Xarid turi `PREMIERE` (butun kontent), chunki qism yo'q;
- `AccessService.canReadMedia` endi kontent videosini ham tekshiradi —
  aks holda pullik film fayli ochiq qolardi;
- `GET /api/v1/app/watch/content/{contentId}` — yangi endpoint;
- ⚠️ `SecurityConfig` da `/watch/*` → `/watch/**`: yangi yo'l ikki
  darajali va bitta yulduzcha uni qamramasdi.

**TRAILER va TEASER ataylab kirmaydi** — ular reklama roligi. Kirsa, pullik
filmni sotib olmagan odam treylerni «film» deb olib ketardi.

Toza bazada tekshirildi: tomosha javobi 2 segment beradi, ikkala fayl ham
`video/mp4` bilan 200 qaytaradi, `Range` → 206. Pullik filmniki — 404.
`SingleContentWatchTest` (10 test), ikki mutatsiya bilan tekshirilgan.

### `[x]` B21 — Kliyent xatolari 500 qaytarardi — **TUZATILDI 20.08.2026**

Quyidagilar `GlobalExceptionHandler` da ushlanmagan edi va **500** berardi:

| Xato | Misol |
|---|---|
| `MethodArgumentTypeMismatchException` | `/users/abc` — UUID kutiladi |
| `MissingServletRequestParameterException` | majburiy parametr yuborilmagan |
| `MultipartException` · `MissingServletRequestPartException` | fayl yuborilmagan |
| `HttpMessageNotReadableException` | buzuq JSON |

Hammasi **kliyent** xatosi, lekin 500 «serverda nosozlik» degani — ya'ni
ularni haqiqiy nosozliklardan ajratib bo'lmasdi va monitoring shovqinga
to'lardi.

Avtorizatsiya matritsasini yuritayotib topildi: ruxsati yo'q WORKER ba'zi
endpointlarda 403 o'rniga 500 olardi va bu **teshikka o'xshab ko'rinardi**.
Tekshirilganda sabab boshqa ekan — so'rov noto'g'ri shakllantirilgan edi,
argument o'girish esa ruxsat tekshiruvidan oldin yiqilardi.

Endi hammasi **400** + tushunarli xabar qaytaradi.

### `[x]` B20 — Noma'lum API yo'li HTML qaytarardi — **TUZATILDI 20.08.2026**

`WebMvcConfig` dagi SPA fallback API yo'llarini chetlab o'tishi kerak edi,
lekin tekshiruv `ignoredPaths.contains(path)` — ya'ni faqat AYNAN `"api"`
degan yo'lni tanirdi.

Natijada mavjud bo'lmagan API manzili (`/api/v1/app/admin/xato`) 404 emas,
**`index.html` sahifasini 200 bilan** qaytarardi. Klient JSON kutgan joyda
HTML olardi va xato «yo'q endpoint» emas, «javobni o'qib bo'lmadi»
ko'rinishida chiqardi.

Backend avtorizatsiya testini yozayotib topildi: xodim tokeni bilan
mavjud bo'lmagan yo'l 404 o'rniga 200 qaytardi.

**Yechim:** prefiks bo'yicha tekshirish. SPA fallback `/aadmin/*`,
`/app/panel/*` va `/` uchun o'z ishini davom ettiradi — tekshirildi.

### `[x]` B19 — Mavjud ruxsatni qayta berish 500 qaytarardi — **TUZATILDI 20.08.2026**

`replacePermissions` avval BARCHA ruxsatni o'chirib, keyin qaytadan yozardi.
Hibernate `DELETE` ni flush paytigacha kechiktiradi va `INSERT` ni undan
oldin yuboradi — natijada `UNIQUE(user_id, permission)` buzilardi.

Ko'rinish sharti: yangi to'plam eskisi bilan **kesishsa**. Ya'ni eng oddiy
holat — admin bitta ruxsat qo'shib, qolganini o'sha holicha qoldirsa.

SUPER_ADMIN doirasini tekshirayotib topildi. HYPER_ADMIN sinovida
ko'rinmagan edi, chunki u yerda xodim yangi yaratilgan va ruxsatlari bo'sh edi.

**Yechim:** farq hisoblanadi — faqat olib tashlanadigani o'chiriladi, faqat
yangisi qo'shiladi. Ortiqcha yozuv-o'chiruv ham yo'qoladi.

⚠️ **Testlar ham qayta yozildi.** Ular `deleteAllByUserId` chaqirilganini
tekshirardi, ya'ni IMPLEMENTATSIYANI qotirib qo'ygan edi va aynan buzuq
usulni «to'g'ri» deb saqlab turardi. Endi natijaga qaraydi: oxirida qaysi
ruxsatlar qolgani.

### `[x]` B18 — Standart parolli HYPER_ADMIN hisobi — **TUZATILDI 20.08.2026**

Eng jiddiy topilma. `AutoRun` har ishga tushishda hisob yaratardi va parolning
**standart qiymati kodda** turardi:

```java
@Value("${app.gipersuperadmin.password:00000000}")
```

Ya'ni har bir yangi o'rnatishda `gipersuperadmin / 00000000` paydo bo'lardi —
platformadagi **eng yuqori rol**, paroli esa manba kodda. Xuddi shunday
`superadmin` va ikkita `admin1234*` hisobi ham.

**Amalda tekshirilgan edi:** shu ma'lumot bilan ikkala login endpointidan
token olindi, `/staff`, `/audit-logs`, `/settings`, `/users` ochildi va
SUPER_ADMIN hisobi yaratildi.

**Yechim:**

- parol uchun standart qiymat butunlay olib tashlandi;
- `BootstrapPasswordPolicy` — zaif parol rad etiladi (≥8 belgi, harf+raqam,
  ma'lum zaif parollar ro'yxati);
- parol berilmasa yoki zaif bo'lsa — hisob **yaratilmaydi**, ogohlantirish
  yoziladi (parolning o'zi hech qachon logga tushmaydi);
- lokal qulaylik uchun `app.bootstrap.allow-weak-password` faqat `dev`
  profilida `true`.

Toza bazada tekshirildi: hisob yaratilmadi, eski ma'lumot bilan login **401**.

⚠️ **MAVJUD o'rnatishlar uchun operatsion vazifa:** bu tuzatish faqat YANGI
hisob yaratishga ta'sir qiladi. Agar prod bazasida `gipersuperadmin`,
`superadmin` yoki `admin1234` allaqachon `00000000` paroli bilan mavjud
bo'lsa — **parollarni qo'lda almashtirish shart**. Buni avtomatik qilmadik:
egasini tizimdan chiqarib yuborish xavfi bor.

### `[x]` B16 — Validatsiya ruxsat tekshiruvidan OLDIN ishlardi — **TUZATILDI**

`@Valid` request body Spring tomonidan controller metodiga KIRISHDAN oldin
tekshiriladi. Natijada ruxsatsiz xodim noto'g'ri tana yuborsa **422
VALIDATION_ERROR** oladi, **403** emas.

Amalda tekshirilgan:

| Kim | Bo'sh tana | To'g'ri tana |
|---|---|---|
| WORKER (CONTENT_CREATE yo'q) | 422 | ✅ **403 ACCESS_DENIED** |
| WORKER (CONTENT_CREATE bor) | 422 | ✅ 201 |

**Xavfsizlik teshigi EMAS** — ruxsatsiz odam hech narsa yarata olmaydi.
Lekin u validatsiya qoidalarini bilib oladi (qaysi maydonlar majburiy).
Bu past darajadagi ma'lumot oshkorligi.

Tuzatish: ruxsatni `@Valid` dan oldin tekshiradigan filtr yoki
`HandlerInterceptor` kerak. `@RequirePermission` aspекti ham metod
chaqirilgandan keyin ishlaydi, ya'ni bu muammoni yechmaydi.

### `[ ]` B10 — Frontend ESLint warning'lari (~40 ta)

`no-unused-vars`, `react-hooks/exhaustive-deps`, `jsx-a11y/*`,
`react/jsx-no-target-blank`. Build'ni to'xtatmaydi.

---

## 9. Database Changes

### Bajarildi (19.08.2026)

⚠️ Flyway hali ulanmagan, shuning uchun quyidagi jadvallarni **`ddl-auto=update` o'zi yaratadi**.
Flyway kiritilganda ular `V1__baseline.sql` ichiga kiritiladi.

| Jadval | Nima uchun |
|---|---|
| `user_permission` | WORKER uchun fine-grained ruxsat. UNIQUE(user_id, permission) |
| `audit_log` | Muhim admin amallari. 3 ta index: actor, entity, created_at |

Mavjud jadvallarga o'zgarish **yo'q**. `role` jadvaliga `ROLE_WORKER` satri qo'shiladi
(`AutoRun.ensureRole` orqali, idempotent).

### Keyingi o'zgarishlar

Flyway migration orqali bo'ladi (D4).

Rejalashtirilgan (PHASE 1+):
- `V1__baseline.sql` — mavjud sxema (`ddl-auto=update` yaratganini qayd etish)
- `permission`, `role_permission`, `user_permission`
- `audit_log`
- PHASE 3+: `category`, `genre`, `creator`, `media_asset`
- PHASE 4+: `content`, `season`, `episode`, `episode_video`, `content_credit`

⚠️ **Hech qachon** `DROP DATABASE` / `DROP TABLE` / production data o'chirish yo'q (§91).

---

## 10. API Changes

### Bajarildi (19.08.2026)

**Bironta endpoint qo'shilmadi, o'chirilmadi yoki nomi o'zgarmadi.** O'zgargani — kirish
nazorati (B1). Javob formatlari o'sha-o'sha, ya'ni sayt, bot va mobil ilova uchun
kontrakt buzilmadi.

Yagona yangi xatti-harakat: himoyalangan endpointga tokensiz kirishda endi HTML emas,
JSON qaytadi — `{"code":"UNAUTHORIZED","message":"..."}` (401) yoki
`{"code":"ACCESS_DENIED",...}` (403).

⚠️ **Frontend'ga ta'siri:** admin panellar token bilan ishlaydi, shuning uchun normal
oqim o'zgarmaydi. Lekin token muddati tugaganda ilgari 200 kelardi, endi 401 keladi —
`config/index.js` buni qanday qayta ishlashini tekshirish kerak (§11).

### Bajarildi (20.08.2026)

**Yangi ochiq endpoint:** `GET /api/v1/app/watch/{episodeId}?locale=UZ`

Klient uchun yagona kirish nuqtasi: «ko'ra olamanmi» va «qaysi faylni
o'ynatay» — bitta tranzaksiyada. Alohida so'ralsa, orada holat o'zgarishi
mumkin edi (obuna tugadi, xarid qaytarildi).

```json
{ "episodeId": 2, "title": "Sir", "allowed": false,
  "reason": "PAYMENT_REQUIRED", "requiredAction": "BUY_OR_SUBSCRIBE",
  "episodePrice": 3000, "premierePrice": 15000,
  "showAds": false, "sources": [] }
```

`reason`: `FREE` · `PREMIUM` · `EPISODE_PURCHASE` · `PREMIERE_PURCHASE` ·
`NOT_PUBLISHED` · `USER_BLOCKED` · `NOT_AUTHENTICATED` · `PAYMENT_REQUIRED`
`requiredAction`: `NONE` · `SIGN_IN` · `SUBSCRIBE` · `BUY_EPISODE` ·
`BUY_PREMIERE` · `BUY_OR_SUBSCRIBE`

⚠️ **Ruxsat bo'lmasa `sources` bo'sh** (null emas — klientda `.length`
xatosi chiqmasin). Havola hech qachon rad javobi bilan birga ketmaydi.

**O'zgargan xatti-harakat:** `GET /api/v1/app/media/{id}/raw` endi video uchun
entitlement tekshiradi (B15) va `Range` so'rovini qo'llab-quvvatlaydi
(206 Partial Content). Rasmlar uchun hech nima o'zgarmadi.

### Bo'laklab yuklash — katta video uchun (20.08.2026)

**Muammo:** prodda `spring.servlet.multipart.max-file-size = 50MB`. Haqiqiy
epizod videosi yuz megabaytdan gigabaytgacha, ya'ni **umuman yuklab
bo'lmasdi**. Sig'gan taqdirda ham ulanish uzilsa hammasi boshidan boshlanardi.

```
POST   /api/v1/app/admin/uploads                  → uploadId, chunkSize (5 MB)
PUT    /api/v1/app/admin/uploads/{id}/chunks/{n}  → xom baytlar (multipart EMAS)
GET    /api/v1/app/admin/uploads/{id}             → qaysi bo'laklar yetib kelgan
POST   /api/v1/app/admin/uploads/{id}/complete    → yig'ish → MediaAsset
DELETE /api/v1/app/admin/uploads/{id}             → bekor qilish
```

Muhim qarorlar:

- **Bo'lak xom tanada, multipart emas** — aks holda o'sha 50 MB chegarasi
  yana ishlab ketardi. Xom `application/octet-stream` oqim sifatida
  o'qiladi va to'g'ridan-to'g'ri diskka yoziladi, RAM'da to'planmaydi.
- **Yetib kelgan bo'laklar bazada emas, diskda** — `.part` fayllar ro'yxati
  yagona haqiqat manbai. Baza va disk bir-biriga mos kelmay qolishi mumkin,
  disk esa o'zini o'zi tekshiradi.
- **Bo'lak avval `.tmp` nomiga yoziladi**, keyin ko'chiriladi. Yozish
  yarmida uzilsa, yarim bo'lak «tayyor» deb hisoblanmaydi.
- **Kengaytma ENG BOSHIDA tekshiriladi** — gigabaytlab ma'lumot yuborib
  bo'lgach rad javobi olmaslik uchun.
- **Sessiya faqat egasiga ochiq** — boshqa xodim (hatto ADMIN) 404 oladi.
  Aks holda bir xodim ikkinchisining faylini buzishi mumkin edi.
- **`MediaAsset` faqat yig'ish tugagach yaratiladi** — yarim video media
  kutubxonasiga tushmaydi.
- Tashlab ketilgan sessiyalar sutkadan keyin tozalanadi (`@Scheduled`).

Amalda tekshirilgan: 12 MB fayl 3 bo'lakda, 1-bo'lak ataylab yuborilmadi →
`complete` 422 va «Yetishmayapti: [1]», keyin yuborilgach yig'ildi.
Natija SHA-256 bo'yicha manba bilan **bayt-bayt bir xil**.

### Media o'chirish — mavjud bo'lmagan endpoint (20.08.2026)

`MEDIA_DELETE` ruxsati bor edi, lekin uni ishlatadigan endpoint **yo'q** edi —
ya'ni panelda ko'rinadigan, amalda hech narsa qilmaydigan huquq.

```
GET    /api/v1/app/admin/media/{id}/usage   → fayl qayerda ishlatilyapti
DELETE /api/v1/app/admin/media/{id}         → o'chirish (ishlatilayotgan bo'lsa 409)
```

Media **12 xil joydan** havola qilinadi (kontent galereyasi, qism videosi,
qism eskizi, fasl afishasi, ijodkor surati/muqovasi, kategoriya ikonkasi,
reklama ×2, premyera ×2, bildirishnoma). Ko'r-ko'rona o'chirish sinib qolgan
rasm va o'ynamaydigan video demakdir, shuning uchun 409 javobida AYNAN
qayerda ishlatilayotgani yoziladi.

### Keyingi

Yangi admin API namespace: `/api/v1/app/admin/**` — batafsil `BACKEND_ROADMAP.md`.

---

## 11. Frontend Changes

**Hozircha o'zgarish yo'q.** Yangi admin panel `src/adminpanel/` da quriladi (D11).
Mavjud `src/admin/` va `src/bot-admin/` tegilmaydi.

⚠️ **B1 dan keyin tekshirilishi kerak:** endi yaroqsiz/muddati o'tgan token 401 oladi.
`src/config/index.js` 401 ni ushlab login sahifasiga yo'naltirishi kerak. Hozir u
xatoni `{error: true}` qilib qaytaradi — sahifa bo'sh ko'rinishi mumkin.
Bu B4 (guard ishlamaydi) bilan birga PHASE 1 frontend ishida hal qilinadi.

---

## 12. Testing Status

| | Holat |
|---|---|
| Test infrastructure | ✅ `test` profili + xotiradagi H2 |
| `contextLoads` | ✅ ishlaydi (avval yiqilardi) |
| `SecurityRulesTest` | ✅ **29 ta test** — barcha modullar, analitika ham |
| `PlatformRoleTest` | ✅ **14 ta test** — ierarxiya, privilege escalation |
| `PermissionServiceTest` | ✅ **12 ta test** — ruxsatlar, «o'zida yo'qni bera olmaydi» |
| `SlugGeneratorTest` | ✅ **6 ta test** — kirill, apostrof, unikallik |
| `ContentStructureTest` | ✅ **11 ta test** — ТЗ §80: SEASONAL / EPISODIC / SINGLE |
| `PublicCatalogPrivacyTest` | ✅ **2 ta test** — ochiq katalogda PD yo'qligi |
| `AccessServiceTest` | ✅ **17 ta test** — entitlement 4 manbadan (ТЗ §37) |
| `PaidContentLeakTest` | ✅ **7 ta test** — pullik video sizib chiqmasligi |
| `MediaRangeDeliveryTest` | ✅ **3 ta test** — `Range` → 206, seek ishlashi |
| `RateLimiterTest` | ✅ **5 ta test** — oqim cheklovi, ko'p oqimli xavfsizlik |
| `AdminEndpointGuardTest` | ✅ arxitektura testi — qo'riqlanmagan endpoint yo'q |
| `ChunkedUploadTest` | ✅ **9 ta test** — bo'laklab yuklash, davom ettirish, rad etish |
| `MediaDeletionTest` | ✅ **3 ta test** — ishlatilayotgan fayl o'chirilmasligi |
| `SecurityRulesTest` (PHASE 6–7) | ✅ izoh, bildirishnoma, foydalanuvchi, tarif, sozlama, audit |
| Brauzerda qo'lda sinov | ✅ 5 rol, ruxsat filtri, 3 til, tilga xos afisha |
| Responsivlik | ⚠️ **brauzerda sinalmadi** — muhitda viewport 1512'da qotib qolgan |
| Boshqa backend unit test | Yo'q |
| Frontend test | Yo'q |

`./mvnw -f backend/pom.xml package` — **294 ta test, hammasi o'tadi**.

**Mutatsiya bilan tekshirilgan.** `PaidContentLeakTest` va `AdminEndpointGuardTest`
ataylab buzib ko'rildi (rad javobiga havola qo'shildi; qo'riqlanmagan endpoint
qo'shildi) — ikkalasida ham testlar yiqildi. Ya'ni ular bekorga o'tmayapti.

PHASE 1 ichida: test profili (H2 yoki Testcontainers) qo'shish — `contextLoads`ni tiklash.

⚠️ PHASE 1 da qo'shilgan `PlatformRole`, `RoleMapper`, `PermissionService` **testsiz**.
Ular RBAC yadrosi, shuning uchun test PHASE 2 dan oldin yozilishi shart (§78).

---

## 13. Important Decisions

**D1 — Mavjud papka strukturasi saqlanadi.**
`backend/` va `frontend/` allaqachon mavjud va prompt talabiga mos. Ko'chirish git tarixini
buzadi va deploy'ni sindiradi. Roadmap fayllari o'sha papkalar ichiga qo'yildi.

**D2 — Spring Boot 3.1.2 / Java 17 davom ettiriladi.**
Ishlayotgan production stack. Yangilash alohida task, development'ni bloklamasligi kerak.

**D3 — Frontend JavaScript'da qoladi.**
Prompt §5 aniq: "Agar JavaScript project bo'lsa butun loyihani sababsiz TypeScript'ga
rewrite qilma." Yangi admin modullar ham JS'da yoziladi — izchillik uchun.

**D4 — Flyway tanlandi.**
Na Flyway, na Liquibase mavjud emas edi, ya'ni tanlov erkin. Flyway SQL-first, Spring Boot
bilan integratsiyasi sodda, jamoa uchun o'rganish qiyin emas.
Mavjud production DB borligi uchun `baseline-on-migrate=true` va `V1__baseline.sql`.
`ddl-auto` `update` → `validate` ga o'tkaziladi, lekin **faqat baseline tekshirilgandan keyin**.

**D5 — Mavjud `UserRoles` enum kengaytiriladi.**
§89 duplicate taqiqlaydi. `ROLE_GIPERSUPERADMIN` allaqachon HYPER_ADMIN vazifasini bajaradi
va production'da hisob mavjud — qayta nomlash mavjud login'ni sindiradi.
Qaror: enum saqlanadi, `ROLE_WORKER` qo'shiladi, universitet rollari `@Deprecated` qilinadi
lekin **o'chirilmaydi** (DB'da satrlar bor). Kod darajasida mapping:
`ROLE_GIPERSUPERADMIN` → HYPER_ADMIN, `ROLE_SUPERADMIN` → SUPER_ADMIN,
`ROLE_ADMIN` → ADMIN, `ROLE_WORKER` → WORKER, `ROLE_USER` → USER.

**D6 — Permission alohida entity.**
WORKER uchun fine-grained ruxsat kerak (§10). Enum'ga sig'maydi, chunki Admin/SuperAdmin
Worker yaratganda ruxsatlarni **tanlaydi** — ya'ni runtime'da o'zgaradi.

**D7 — Yangi admin API alohida namespace'da.**
Mavjud `/api/v1/**` ni sayt, Telegram bot va mobil ilova ishlatadi. Ularni sindirish
uchta klientni bir vaqtda buzadi. Yangi `/api/v1/app/admin/**` — toza va versiyalangan.

**D8 — Pul `BigDecimal`.**
Mavjud `CastingUser.price` `Double` — bu xato, lekin casting moduliga tegilmaydi.
Yangi monetizatsiya kodida faqat `BigDecimal`.

**D9 — ID strategiyasi entity bo'yicha.**
`User`=UUID, `CastingUser`=Integer, `Role`=int, `Attachment`=UUID. Bir kunda UUID'ga
rewrite qilish FK'larni buzadi. Yangi entity'lar uchun standart: `Long` + IDENTITY.

**D10 — Yangi `MediaAsset`, `Attachment` tegilmaydi.**
`Attachment`da faqat 4 ta maydon bor (id, prefix, name, isWebShow) — mime, size, duration,
width, height, status, sortOrder yo'q. Uni kengaytirish casting modulini va bot oqimini
regressiyaga uchratadi. Yangi entity xavfsizroq; keyinchalik `Attachment` → `MediaAsset`
migratsiyasi alohida task.

**D11 — Admin UI yangi papkada.**
`src/admin/` — casting anketalari uchun, `src/bot-admin/` — bot uchun. Ikkalasi ham
ishlayapti. Yangi UZCASTING admin paneli boshqa domen — aralashtirish ikkalasini buzadi.
Yangi papka: `src/adminpanel/`, route prefiksi `/app/panel/*`.

---

**D12 — Vaqt mintaqasi `Asia/Tashkent`, UTC ga to'liq o'tilmadi (§68).**
ТЗ «imkon qadar UTC» deydi. Loyihada 310 dan ortiq `LocalDateTime` bor;
`Instant` ga ko'chirish panelning har bir sana maydonini ham o'girishni
talab qilardi. **Yarim bajarilgan o'tish umuman qilmaslikdan yomonroq**:
bir qism qiymat UTC, bir qismi mahalliy bo'lib qolardi va ularni
farqlashning yo'li yo'q edi. UTC talab qilinishining asosiy sababi —
yozgi vaqt; O'zbekiston 1996 yildan beri UTC+5 da qat'iy turadi, ya'ni
takrorlanadigan yoki tushib qoladigan soat yo'q. Sozlamasiz esa
konteynerdagi UTC tufayli rejalashtirilgan premyera besh soat kech
chiqardi. Ko'p mintaqali kengayish bo'lsa `Instant` ga o'tish yo'li
`TimeZoneConfig` izohida yozilgan.

**D13 — Access token xotirada, refresh token `httpOnly` cookie'da (§61).**
ТЗ «localStorage'ga tashlashdan oldin xavfsizlikni hisobga ol» deydi.
`localStorage` ni sahifadagi har qanday JavaScript o'qiy oladi — bitta
XSS (masalan buzilgan npm paketi) uzoq muddatli kirish beradi. Cookie'ni
esa JavaScript umuman ko'rmaydi. Narxi: sahifa yangilanganda access
token yo'qoladi va bitta qo'shimcha `/auth/refresh` chaqiruvi bo'ladi.

**D14 — Eski, turi ko'rsatilmagan tokenlar access sifatida qabul qilinadi (§61).**
`typ` da'vosi qo'shilganda eski tokenlarni darhol rad etish mumkin edi,
lekin bu joriy etilgan zahoti **barcha ishlab turgan foydalanuvchilarni
tizimdan chiqarib yuborardi**. Eski tokenlar 24 soat ichida o'z-o'zidan
eskiradi va muammo yo'qoladi.

**D15 — Server-state kutubxonasi qo'shilmadi (§70).**
TanStack Query yoki shunga o'xshash kutubxona loyihada yo'q. Mavjud
`useApi` hook keshsiz, lekin panel uchun yetarli: ro'yxatlar
sahifalangan, har o'tishda yangi ma'lumot kerak. ТЗ «yangi bog'liqlik
real muammoni hal qilsin» deydi — bu yerda hal qilinadigan muammo hali
ko'rinmadi.

**D16 — springdoc-openapi qo'shildi (§106).**
Yangi bog'liqlik, lekin real muammoni hal qiladi: mobil va frontend
jamoasi endpointni backend kodini o'qimasdan ishlata olishi kerak.
Mavjud annotatsiyalardan hujjat hosil qiladi, qo'shimcha kod talab
qilmaydi. **Ishlab chiqarishda yopiq** — hujjat API'ning butun
xaritasini beradi.

**D17 — `@testing-library` qo'shilmadi, testlar sof Jest'da (§86).**
Kutubxona o'rnatilmagan va faqat test uchun uchta yangi bog'liqlik
qo'shish §70 ga zid bo'lardi. Eng xavfli mantiq — 401 da tokenni
yangilash oqimi — DOM talab qilmaydi va sof Jest bilan sinaladi.
Komponent renderini talab qiladigan testlar kerak bo'lsa, o'shanda
qo'shiladi.


**D18 — API yo'l konvensiyasi o'zgartirilmadi (§64).**
ТЗ {@code /api/admin/v1/...} ni misol qilib keltiradi, loyihada esa
`/api/v1/app/admin/...` ishlatiladi. Ikkalasi ham versiyalangan — farq
faqat versiya raqamining joyida. ТЗ ning o'zi «existing API convention
bo'lsa uni to'satdan sindirma» deydi; qirqdan ortiq endpointni qayta
nomlash ishlab turgan panelni sindirardi va evaziga hech narsa
bermasdi. `ApiConventionTest` yangi endpoint versiyasiz yoki boshqa
prefiks bilan qo'shilishini taqiqlaydi.

**D19 — 2FA hozir yozilmadi, alohida security task sifatida rejalashtirildi (§63).**
ТЗ shunga ruxsat beradi. Sabab: tiklash kodlarisiz 2FA **xavfli** —
telefonini yo'qotgan HYPER_ADMIN tizimga abadiy kira olmaydi va uni
tiklaydigan yuqoriroq rol yo'q. Ya'ni yarim bajarilgan 2FA
xavfsizlikni oshirmaydi, tizimni egasiz qoldirish xavfini tug'diradi.
Arxitektura tayyor: §61 dagi token turi mexanizmi `2fa_pending` ni
qo'shishga imkon beradi, qurilma tarixi `refresh_token` da bor.
To'liq reja — `roadmap.md → 14.1`.


## 14. Next Exact Steps

> ⚠️ **23.08.2026 holati.** ТЗ §29–§83 va §93–§106 ko'rib chiqildi.
> Build yashil: backend **679 test**, frontend **5 test**, migratsiyalar
> **V1–V26**.

### Keyingi aniq qadamlar

1. **`UserAccount.language` ni panelga chiqarish.**
   Maydon va migratsiya (V26) tayyor, `HomeFeedService` uni o'qiydi.
   Yetishmagani: `UsersPage` da ustun va `NotificationsPage` da
   «qaysi tilda nechta foydalanuvchi bor» ko'rsatkichi.

2. **Bildirishnoma yuborishda oluvchi tilini tanlash.**
   `NotificationDispatcher` hozir barcha tarjimani saqlaydi, lekin
   yuborishda tilni tanlash mantig'i FCM ulanmagani uchun yozilmagan.
   Ulanganda: `UserAccount.language` bo'yicha tarjimani tanlash.

3. **`AdminAuthController` ni bo'lish.**
   §61 dan keyin u 220 qatordan oshdi (login, refresh, logout, me,
   cookie yordamchilari). `AuthCookieService` ajratilsin.

4. **Eski `/api/v1/auth/refresh` ni cookie'ga o'tkazish.**
   Yangi modul tuzatildi, eski modul hali refresh tokenni **URL query
   parametrida** qabul qiladi va u loglarga tushadi. Eski frontend ham
   birga o'zgartirilishi kerak.

5. **`ContentService.apply()` dagi shartsiz `clear()`.**
   `media.clear()` va `credits.clear()` har saqlashda chaqiriladi. Bu
   §60 versiyasini tasodifan oshirib turibdi va §66 da ortiqcha yozuv
   hosil qiladi. O'zgarmagan ro'yxatga tegmaslik kerak — lekin avval
   `ConcurrentEditTest` dagi `touch()` ishonchli ishlashini tasdiqlash.

### Qaror kutilayotgan (kod yozilmaydi)

1. **Video arxitekturasi** — `tz/roadmap for bunny stream*.md` hozirgi
   lokal saqlash yechimiga zid («server — turniket, quvur emas»).
   Uch marta so'raldi, javob yo'q. Video kodiga tegilmagan.
2. **To'lov provayderi** — Payme / Click / Uzum + test hisob ma'lumoti.
3. **Star va Coin kurslari** — hozir 0, ya'ni paketlar sotib olinmaydi.
4. **FCM kaliti** — bildirishnoma yoziladi, yuborilmaydi (503, soxta
   «yuborildi» yozilmaydi).
5. **§49 submenyulari** — Kontent (Film/Mini serial/Serial/Podkast) va
   Xodimlar (Super admin/Admin/Worker) alohida menyu bandimi yoki
   sahifa filtrimi?

### Bajarilgani — 23.08.2026 sessiyasi

| ТЗ | Nima qilindi |
|---|---|
| §58 | Soft delete — reklama/premyera arxivlanadi, sotilgan qism/paket o'chirilmaydi |
| §59 | Audit filtrlari bir-birini istisno qilardi; maxfiy qiymatlar `***` |
| §60 | Optimistik qulf ikki joyda ham o'lik edi |
| §61 | Token turi, rotatsiya, bekor qilish, hisob bo'yicha blok |
| §62 | Parol hash'i javobda chiqmaydi |
| §65 | DTO qoidasi — tekshirildi, buzilish yo'q |
| §66 | Tahrirda janr va ijodkorlar o'chib ketardi |
| §68 | Vaqt mintaqasi — besh soatlik siljish |
| §75 | Eski casting moduli aniq ro'yxati |
| §76/§77 | `backend/FUTURE_MOBILE_API.md` |
| §78 | Sakkizta qabul mezoni — raqamlangan test |
| §86 | Frontend testi — 401 da yangilash oqimi |
| §106 | OpenAPI (prod'da yopiq) |
| §63/§64 | API konvensiyasi qo'riqchisi; 2FA — alohida security task |
| — | Ijodkor kartochkasi muharrirda (ism + surat), tartib raqami tuzatildi |

### Qolgan ishlar

1. **FCM ulash** — `NotificationAdminService.send()` dagi TODO.
   ⚠️ Kalit yo'q. Hozir 503 qaytaradi va urinish FAILED holatda saqlanadi —
   soxta «yuborildi» yozilmaydi.
2. To'lov provayderlari abstraksiyasi.
   ⚠️ Store billing riski hal qilinmaguncha boshlanmasin (`MONETIZATION.md`).
3. `casting-user/appeal` va `/my` uchun bot jamoasi bilan himoya (B2 qoldig'i).
4. PHASE 6–8 sahifalarini brauzerda tekshirish.
5. Responsivlikni haqiqiy qurilmada tekshirish.
6. Indekslarni profiling asosida qayta ko'rib chiqish.
7. Kontent muharririda galereya boshqaruvi.

---

## 14.1. Security task: 2FA (ТЗ §63)

**Holat:** `[ ]` bajarilmagan — ataylab. ТЗ «agar hozir implementation
katta scope talab qilsa roadmapga alohida security task qilib yoz»
deydi. Quyida uni to'g'ridan-to'g'ri olib bajarish uchun yetarli reja.

### Nega hozir yozilmadi

To'liq TOTP uchta narsani talab qiladi: yangi bog'liqlik, QR bilan
ulash oqimi va tiklash kodlari. Tiklash kodlarisiz 2FA **xavfli**:
telefonini yo'qotgan HYPER_ADMIN tizimga abadiy kira olmay qoladi va
uni tiklaydigan yuqoriroq rol yo'q. Ya'ni yarim bajarilgan 2FA
xavfsizlikni oshirmaydi, aksincha tizimni egasiz qoldirish xavfini
tug'diradi.

### Arxitektura TAYYOR — nima tufayli

| Nima | Qayerda | Nega yetarli |
|---|---|---|
| Token turi (`typ`) | `JwtService` (§61) | `2fa_pending` uchinchi tur sifatida qo'shiladi; `MyFilter` uni API kaliti sifatida rad etadi — refresh tokenda bo'lgani kabi |
| Ulanish nuqtasi | `AdminAuthController.login()` | Parol tekshirilgan, token hali berilmagan — izoh bilan belgilangan |
| Qurilma tarixi | `refresh_token.ip`, `user_agent` (V25) | «Yangi qurilma» ni aniqlash uchun qo'shimcha jadval kerak emas |
| Sessiyani yopish | `RefreshTokenService.revokeAll` | 2FA yoqilganda barcha eski sessiya yopiladi |
| Audit | `AuditAction` | `TWO_FACTOR_ENABLED` / `_DISABLED` / `_FAILED` qo'shiladi |

### Bajarish tartibi

1. **Migratsiya V27** — `user_two_factor` jadvali:
   `user_id` (PK, FK), `secret` (shifrlangan), `enabled_at`,
   `recovery_codes_hash` (BCrypt, bittalab), `last_used_at`.
   ⚠️ `secret` ochiq matnda saqlanmasin — baza o'qilsa 2FA ma'nosini
   yo'qotadi. Kalit `APP_2FA_KEY` environment'dan.

2. **Bog'liqlik** — TOTP uchun `dev.samstevens.totp:totp` yoki
   `com.warrenstrange:googleauth`. Ikkalasi ham RFC 6238.
   Qo'lda yozilmasin (§92 ruhi: kriptografiya qo'lda yozilmaydi).

3. **Endpointlar:**
   ```
   POST /api/v1/app/admin/auth/2fa/setup     QR uchun secret (bir marta)
   POST /api/v1/app/admin/auth/2fa/confirm   kod bilan yoqish
   POST /api/v1/app/admin/auth/2fa/verify    kirish paytida (challenge token bilan)
   POST /api/v1/app/admin/auth/2fa/disable   parol + kod talab qiladi
   GET  /api/v1/app/admin/auth/2fa/recovery  yangi tiklash kodlari
   ```

4. **Majburiylik** — `HYPER_ADMIN` va `SUPER_ADMIN` uchun. Amalga
   oshirish tartibi muhim: avval **ixtiyoriy** qilib chiqarilsin, bu
   rollar yoqib olsin, keyingina majburiy qilinsin. Teskarisi — barcha
   super adminni bir vaqtda tizimdan chiqarib yuborish.

5. **Rate limit** — `/2fa/verify` ga alohida qoida (6 xonali kod
   1 000 000 variant, sekundiga o'nlab urinish uni bir necha soatda
   sindiradi). `LoginAttemptService` shu yerda ham ishlatilsin.

6. **Tiklash kodlari** — 10 ta, bir martalik, BCrypt bilan
   xeshlangan, faqat yaratilganda bir marta ko'rsatiladi.

7. **Testlar:** yoqish/o'chirish oqimi, noto'g'ri kod, ishlatilgan
   tiklash kodi ikkinchi marta o'tmasligi, `2fa_pending` token bilan
   API'ga kirib bo'lmasligi, vaqt siljishi (±1 oyna).


## 15. Dev muhitini ishga tushirish

```bash
# 1. Backend (repozitoriy ILDIZIDAN - fayl yo'llari nisbiy)
./backend/mvnw -f backend/pom.xml -DskipTests package
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 2. Frontend
cd frontend && npm install --legacy-peer-deps && npm start

# 3. Panel
http://localhost:3000/app/panel/login
```

Baza: `backend/data/uzcasting-dev.mv.db` (H2, gitignore'da). O'chirsangiz — toza boshlanadi.

### Dev hisoblari — parol hammasida `12345678`

| Telefon | Rol | Ruxsatlar |
|---|---|---|
| `+998901110001` | HYPER_ADMIN | hammasi |
| `+998901110002` | SUPER_ADMIN | hammasi |
| `+998901110003` | ADMIN | hammasi |
| `+998901110004` | WORKER | 13 ta ruxsat |
| `+998901110005` | WORKER | 4 ta (faqat ko'rish) |
| `+998901110009` | USER | ❌ admin panelga kira olmaydi |

### Ilova foydalanuvchilari — entitlement holatlari (parol `12345678`)

Bular admin panelga emas, **mobil/ilova oqimiga** kiradi
(`POST /api/v1/auth/login`). Har biri boshqa ruxsat manbasini tekshiradi:

| Telefon | Holat | Pullik qismda nima bo'ladi |
|---|---|---|
| `+998901112001` | Faol Premium (1 oy) | ✅ `PREMIUM` — ko'radi, reklamasiz |
| `+998901112002` | Muddati o'tgan Premium | ❌ `PAYMENT_REQUIRED` |
| `+998901112003` | Bitta qism sotib olgan | ✅ faqat **o'sha** qismda `EPISODE_PURCHASE` |
| `+998901112004` | Bloklangan | ❌ `USER_BLOCKED` |
| `+998901112005` | Oddiy (bepul) | ❌ `PAYMENT_REQUIRED`, reklama ko'radi |

⚠️ Bu hisoblar faqat `dev` profilida yaratiladi (`app.dev.seed=true`).

### Dev bazadagi mock ma'lumot

7 kategoriya · 8 janr · 8 ijodkor · 12 kontent · 12 qism · 111 media ·
7 izoh (ba'zilariga shikoyat qilingan) · 12 donat · 12 bosh sahifa bo'limi ·
4 tarif · 10 valyuta paketi · 6 sozlama · 3 banner · 3 premyera ·
~10 500 analitika hodisasi (30 kun) · 5 ilova foydalanuvchisi · 2 obuna · 1 xarid

⚠️ **Video fayllar haqiqiy video EMAS** — ichida tasodifiy baytlar (256 KB).
Ular pleyerda o'ynamaydi. Sabab: haqiqiy video uchun kodlagich (ffmpeg) kerak,
u loyiha talabi emas. Fayl baribir yoziladi, chunki usiz yetkazish yo'lini
(entitlement, `Range`, 206) umuman tekshirib bo'lmasdi — hamma so'rov 404
qaytarardi va "ruxsat yo'q" bilan "fayl yo'q" farqlanmasdi.

### ⚠️ Ishlab turgan ilova ustidan qayta yig'ish mumkin emas

Spring Boot fat-jar klasslarni **kechiktirilgan** yuklaydi. Ilova ishlab
turganda `mvn package` jar'ni ustidan yozadi va JVM keyingi klassni topa
olmay qoladi (`ClassNotFoundException`). Avval to'xtating, keyin yig'ing:

```bash
pkill -f "backend-0.0.1-SNAPSHOT.jar"
./backend/mvnw -f backend/pom.xml package
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```
