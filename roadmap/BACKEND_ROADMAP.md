# BACKEND ROADMAP — batafsil checklist

Belgilar: `[x]` bajarilgan · `[~]` qisman · `[ ]` bajarilmagan
`⚠️` — qaror yoki tashqi bog'liqlik kutilmoqda

Holat **koddan tekshirilgan**, taxmin emas. Har bir `[x]` ortida yo ishlayotgan
endpoint, yo o'tayotgan test bor.

**Umumiy holat:** 294 test o'tadi · 15 migratsiya · 49 entity ·
15 servis (+5 yordamchi klass) · 83 admin endpoint.

---

## 1. Existing backend audit `[x]`

- `[x]` Mavjud modullar sanab chiqildi: casting anketalar, yangiliklar,
  fayl (attachment), auth, bot admin
- `[x]` 25 ta eski endpoint va ularning **mijozlari** aniqlandi
  (Telegram bot · eski admin sayti · mobil ilova)
- `[x]` Eski entity va repozitoriylar xaritasi: `Attachment` `CastingUser`
  `Message` `News` `Role` `User`
- `[x]` Xavfsizlik auditi → **B1**: butun API `permitAll` edi
- `[x]` Maxfiylik auditi → **B2**: ochiq katalog shaxsiy ma'lumot qaytarardi
- `[x]` Sirlar auditi → **B3**: DB paroli va JWT kaliti kodda edi
- `[x]` Eski tizim **muzlatildi** — `OldCastingFrozenTest`
- `[x]` Eski `Message` entity aniqlandi — u **messenger emas**, Telegram
  botga yuboriladigan «qabul qilindingiz / rad etildi» javobi.
  `CastingUserController.status()` uni yozadi, bot o'qiydi.
  **O'chirilmaydi** (buyurtmachi talabi), test bilan qo'riqlanadi
- `[x]` Casting oqimi xatti-harakat testi bilan qoplandi —
  `ExistingCastingRegressionTest` (7 test)

## 2. Package architecture `[x]`

```
Admin/        yangi admin API      → /api/v1/app/admin/**
  Controller/ Dto/ RequirePermission · PermissionAspect · PermissionInterceptor
Cms/          kontent moduli       → /api/v1/app/**
  Entity/ Repository/ Service/ Controller/ Enums/ Dev/
Security/     JWT · SecurityConfig · RateLimit/
Controller/   ESKI casting — MUZLATILGAN
Entity/ Repository/ Services/   ESKI casting
```

- `[x]` Yangi kod eski paketlardan **butunlay ajratilgan**
- `[x]` `@EnableJpaRepositories` / `@EntityScan` ikkala paketni qamraydi
- `[x]` Eski paketga yangi controller qo'shilishi test bilan taqiqlangan
- `[x]` `AnalyticsIngestController` eski paketdan `Cms/Controller/` ga ko'chirildi
- `[ ]` `Cms/Service/` 19 ta faylga yetdi — modul bo'yicha ichki paketlarga
  bo'lish kerakmi, ko'rib chiqilsin

## 3. Authentication `[~]`

- `[x]` `POST /api/v1/app/admin/auth/login` — telefon + parol
- `[x]` `GET /api/v1/app/admin/auth/me` — joriy profil
- `[x]` BCrypt, parol hech qayerda ochiq saqlanmaydi
- `[x]` `USER` roli admin panelga kira olmaydi (403)
- `[x]` Login rate limiting — 10/daqiqa (brute-force)
- `[x]` Google ID-token orqali kirish (eski `AuthService`)
- `[ ]` **OTP** (ТЗ R14): 4–6 xona, 5 daqiqa, qayta yuborish 60 s,
  3 xato → 15 daqiqa blok — **umuman yozilmagan**
- `[ ]` Parolni tiklash oqimi
- `[ ]` 2FA (HYPER_ADMIN / SUPER_ADMIN) — hatto tayyorgarlik ham yo'q
- `[ ]` ⚠️ Ilova foydalanuvchilari hali **eski** `/api/v1/auth/login` dan
  kiradi. Yangi `/api/v1/app/auth/**` ochilsa, eski endpoint mijozlar
  ko'chguncha ishlab turishi shart

## 4. JWT `[~]`

- `[x]` HS256, `jjwt 0.11.5`
- `[x]` Kalit `app.jwt.secret` orqali, **koddan olib tashlangan** (B3)
- `[x]` Access token muddati: 100 daqiqa (`app.jwt.access-token-ms`)
- `[x]` `MyFilter` har so'rovda tokenni tekshiradi va `SecurityContext` to'ldiradi
- `[x]` Yaroqsiz/muddati o'tgan token → JSON 401 (ilgari HTML qaytardi)
- `[x]` **Ruxsatlar tokenga yozilmaydi** — har so'rovda bazadan o'qiladi,
  shuning uchun ruxsat olib tashlansa token darhol kuchsizlanadi
  (`RbacIntegrationTest`)
- `[ ]` Token ichida `jti` yo'q — bitta tokenni bekor qilib bo'lmaydi
- `[ ]` Kalit rotatsiyasi rejasi yo'q

## 5. Refresh token `[~]`

- `[x]` `generateJwtRefreshToken` — muddati 24 soat
- `[x]` `POST /api/v1/auth/refresh` (eski, muzlatilgan)
- `[x]` Admin login javobida `refreshToken` qaytadi
- `[ ]` ⚠️ **Bazada saqlanmaydi** — refresh token butunlay stateless.
  Ya'ni **o'g'irlansa muddati tugaguncha ishlaydi**, bekor qilib bo'lmaydi
- `[ ]` **Rotatsiya yo'q** — ishlatilgan refresh token qayta ishlatilaveradi
- `[ ]` `refresh_token` jadvali + qurilmaga bog'lash + `revoke` endpointi
- `[ ]` Chiqish (logout) — hozir faqat klient tomonda token o'chiriladi

## 6. RBAC `[x]`

- `[x]` `PlatformRole` — 5 daraja: HYPER_ADMIN(100) · SUPER_ADMIN(80) ·
  ADMIN(60) · WORKER(40) · USER(10)
- `[x]` `RoleMapper` — eski `UserRoles` ↔ yangi `PlatformRole`
- `[x]` `canCreate` — quyi rol yuqorini yarata olmaydi
- `[x]` `canManage` — teng rolni boshqarib bo'lmaydi
- `[x]` `canAccessAdminPanel` — USER uchun `false`
- `[x]` ADMIN va yuqorisi ruxsat jadvaliga qaramaydi
- `[x]` `PlatformRoleTest` — 14 test
- `[x]` `RbacIntegrationTest` — 7 test, **HTTP darajasida**
- `[x]` HYPER_ADMIN barcha xodimlarni **ko'radi** (audit uchun), quyi rollar
  faqat o'zidan quyini. Boshqarish qoidasi o'zgarmaydi — `manageable` bayrog'i

## 7. Permissions `[x]`

- `[x]` `Permission` enum — **43 ta**, 13 modul bo'yicha
- `[x]` `user_permission` jadvali (V2)
- `[x]` `PermissionService`: `hasPermission` · `permissionsOf` ·
  `replacePermissions` · `roleOf` · `canCreateRole` · `canManageUser`
- `[x]` «O'zida bo'lmagan ruxsatni bera olmaydi» qoidasi
- `[x]` `@RequirePermission` annotatsiyasi
- `[x]` `PermissionAspect` — servis qatlami uchun
- `[x]` `PermissionInterceptor` — **so'rov tanasidan oldin** (B16)
- `[x]` `AdminEndpointGuardTest` — qo'riqlanmagan endpoint qo'shib bo'lmaydi
- `[x]` `PermissionServiceTest` — 12 test
- `[ ]` Ruxsatlarni **guruh/shablon** bilan berish (masalan «Kontent muharriri»)
- `[ ]` Ruxsat o'zgarishi auditga tushishi tekshirilmagan

## 8. Users (ilova foydalanuvchilari) `[~]`

**Entity:** `UserAccount` · `UserBalance` · `UserDevice` · `Subscription` ·
`Purchase` · (eski `User`)

- `[x]` `GET /users` — ro'yxat, `USER_VIEW`
- `[x]` `GET /users/{id}` — batafsil
- `[x]` `POST /users/{id}/block` · `/unblock` — `USER_BLOCK`, sabab bilan
- `[x]` `POST /users/{id}/premium` — sovg'a, mavjud muddat **ustiga** (ТЗ §38)
- `[x]` `DELETE /users/{id}/premium` — tortib olish, obuna **o'chirilmaydi**,
  `revokedAt` belgilanadi
- `[x]` `GET /users/{id}/devices` · `DELETE .../devices/{rowId}`
- `[x]` Bloklangan foydalanuvchi hech narsani ko'ra olmaydi (`AccessService`)
- `[x]` `PremiumLifecycleTest` — 8 test
- `[x]` `UserAdminService` barcha amallarni auditga yozadi
- `[ ]` **Qurilma chegarasi 2 ta** (ТЗ R7) — `UserDevice` bor, lekin
  chegarani **majburlaydigan kod yo'q**; qurilma ro'yxatdan o'tkazish
  endpointi ham yo'q
- `[ ]` Foydalanuvchini o'chirish / anonimlashtirish (GDPR uslubida)
- `[ ]` Foydalanuvchi qidiruvi (telefon, ism bo'yicha)

## 9. Staff `[x]` — ТЗ §12 to'liq

- `[x]` `GET /staff` — rol ≥ ADMIN, faqat quyi rollar ko'rinadi
- `[x]` `POST /staff` — rol ierarxiyasi tekshiriladi (`canCreateRole`)
- `[x]` `@RequirePermission(role = ADMIN)` — tanadan oldin tekshiriladi
- `[x]` Telefon `+998 XX XXX XX XX`, parol ≥8 belgi + harf + raqam (R15)
- `[x]` Dublikat telefon rad etiladi
- `[x]` Yaratish auditga yoziladi (parolsiz)
- `[x]` `PUT /staff/{id}/permissions` — ruxsatlarni almashtirish.
  Aktor o'zida bo'lmagan ruxsatni bera olmaydi
- `[x]` `PUT /staff/{id}/role` — rolni o'zgartirish. Ikki tomonlama
  tekshiruv: nishonni boshqara olishi VA yangi rolni yarata olishi
- `[x]` `POST /staff/{id}/block` · `/unblock` — bloklash `cms_staff_profile`
  orqali (eski `users` jadvaliga tegilmadi), **darhol kuchga kiradi**
- `[x]` O'z hisobiga tegib bo'lmaydi — rolni oshirish yoki o'zini bloklash yo'q
- `[x]` `HyperAdminHierarchyTest` — 9 test
- `[x]` `PUT /staff/{id}` — ism, telefon, email, avatar. Telefon unikalligi
  tekshiriladi
- `[x]` `PUT /staff/{id}/password` — parolni tiklash. Parol javobda
  **qaytarilmaydi** va auditga **yozilmaydi**
- `[x]` `POST /staff/{id}/activate` · `/deactivate` — **hard delete o'rnida**
- `[x]` Ro'yxatda ТЗ §12 dagi barcha maydonlar: id · avatar · ism · telefon ·
  email · rol · **status** · **createdBy** · **createdAt** · **lastLoginAt**
- `[x]` Filtrlar: rol · holat · qidiruv (ism/telefon/email) · sana oralig'i
- `[x]` `cms_staff_profile` (V10) — eski muzlatilgan `users` jadvaliga
  tegilmadi
- `[x]` Holat kirishni to'xtatadi: login rad etiladi va **mavjud token ham**
  darhol kuchsizlanadi
- `[x]` `StaffManagementTest` — 14 test
- `[ ]` Ro'yxat sahifalash — hozir xotirada filtrlanadi (xodimlar soni kam).
  Minglab bo'lsa SQL'ga ko'chirilsin

## 10. Categories `[x]`

**Entity:** `Category` + `CategoryTranslation` · **Migratsiya:** V3

- `[x]` `GET /categories` · `POST /categories` · `PUT /categories/{id}`
- `[x]` Uch tilda tarjima, `UNIQUE(category_id, locale)`
- `[x]` Slug avtomatik, tahrirlashda **o'zgarmaydi** (B13)
- `[x]` Ikonka (`MediaAsset`)
- `[x]` `active` + `sortOrder`, `idx_category_active`
- `[ ]` O'chirish endpointi (kontentga bog'langanini tekshirish bilan)
- `[ ]` Tartibni panel orqali sudrab o'zgartirish

## 11. Genres `[x]`

**Entity:** `Genre` + `GenreTranslation` · **Migratsiya:** V3

- `[x]` `GET /genres` · `POST /genres` · `PUT /genres/{id}`
- `[x]` Uch tilda tarjima
- `[x]` `cms_content_genre` — ko'p-ko'pga bog'lanish
- `[ ]` O'chirish endpointi
- `[ ]` Janr bo'yicha kontent sanog'i

## 12. Creators `[x]` — ТЗ §24 to'liq

**Entity:** `Creator` + `CreatorTranslation` · **Migratsiya:** V3, V13

- `[x]` `GET /creators` (qidiruv bilan) · `POST` · `PUT /{id}`
- `[x]` Surat va muqova (`photo`, `cover`)
- `[x]` `featured` + `sortOrder` — «Mashhur ijodkorlar» uchun (R3)
- `[x]` `cms_content_credit` — kontentdagi roli va tartibi
- `[x]` Uch tilda ism va biografiya
- `[x]` **ТЗ §24 maydonlari**: photo · cover · birthDate · active ·
  featured · createdAt · **updatedAt** (V13 da qo'shildi, yo'q edi)
- `[x]` Ism, otasining ismi va biografiya **uch tilda** —
  `CreatorTranslation` da 5 maydon
- `[x]` 10 kasb: ТЗ dagi 8 tasi + ACTRESS, OPERATOR
- `[x]` **Kengaytiriladigan**: `profession` ustunida CHECK cheklovi yo'q,
  yangi kasb migratsiyasiz qo'shiladi (D18)
- `[x]` **Bitta ijodkor turli rollarda** — bir kinoda aktyor, boshqasida
  rejissyor; hatto bitta kinoda ikki rol. Kasb `ContentCredit` da, `Creator`
  da EMAS — aks holda bir odam bir vaqtda bitta kasbga ega bo'lardi
- `[x]` `CreatorModuleTest` — 8 test, mutatsiya bilan tekshirilgan
- `[ ]` O'chirish endpointi
- `[x]` **ТЗ §25 «Mashhur ijodkorlar»**: `GET /homepage/creators` —
  bo'limga tushadigan ijodkorlar. Ilgari bo'lim sozlamada bor edi, lekin
  uni ma'lumot bilan bog'laydigan kod YO'Q edi (repo so'rovi o'lik kod)
- `[x]` **Analitikaga tayyor**: tartib `homepage.creators.ranking`
  sozlamasidan — `MANUAL` (featured + sortOrder) yoki `STARS`. Kod
  o'zgartirmasdan almashadi, amalda tekshirilgan
- `[x]` STARS tanlansa-yu Stars to'planmagan bo'lsa — **ogohlantirish**
  yoziladi, jim qolib soxta «analitika ishlayapti» taassuroti bermaydi
- `[ ]` Ijodkor sahifasi uchun ochiq (klient) endpoint

## 13. Content `[x]`

**Entity:** `Content` · `ContentTranslation` · `ContentMedia` ·
`ContentCredit` · **Migratsiya:** V3

- `[x]` `GET /content` — sahifalash, holat/tur filtri, qidiruv
- `[x]` `GET /content/{id}` · `POST /content` · `PUT /{id}` · `DELETE /{id}`
- `[x]` Uch tuzilma: SINGLE · EPISODIC · SEASONAL (`ContentStructureTest`)
- `[x]` 10 tur, 2 orientatsiya (LANDSCAPE / VERTICAL — R1)
- `[x]` **ТЗ §13: uch mustaqil o'lchov** — `contentType` (enum, kontentning
  SHAKLI) · `category` (FK, katalog BO'LIMI) · `genres` (ko'p-ko'pga, USLUB).
  Uchtasi erkin kombinatsiyalanadi: `MINI_SERIES` + Drama + Romance.
  `ContentClassificationTest` (8 test) qo'riqlaydi — mutatsiya bilan
  tekshirilgan
- `[x]` Namuna ma'lumot ham to'g'ri naqsh o'rgatadi: kategoriya slug'i
  kontent turi nomini takrorlamaydi (manbadan o'qib tekshiriladi)
- `[x]` Yumshoq o'chirish (`deletedAt`)
- `[x]` Optimistik qulflash (`version`)
- `[x]` Tarjimalar **joyida** yangilanadi (`clear()+add` UNIQUE ni buzardi)
- `[x]` **B17**: tahrirlash kategoriya, muqova va galereyani yo'qotmaydi
  (`ContentEditRoundTripTest`)
- `[x]` Sahifalash bazada kesiladi, N+1 yo'q (`ContentListPerformanceTest`)
- `[ ]` Ommaviy amallar (bir nechta kontentni birga nashr qilish)
- `[ ]` Nashr jadvali (`SCHEDULED` → avtomatik `PUBLISHED`) — holat bor,
  **uni ishga tushiradigan vazifa yo'q**
- `[x]` **B22: SINGLE kontent videosi** — `MediaRole.VIDEO` + `ContentMedia`.
  Ilgari filmni tomosha qilish oqimi UMUMAN yo'q edi
- `[x]` **B23**: DTO barcha media rollarini qaytaradi — tahrirlash video va
  treylerni o'chirmaydi
- `[x]` **ТЗ §15 to'liq**: `visibility` (PUBLIC · UNLISTED · PRIVATE) va
  `language` (asl til, ISO 639-1) qo'shildi. Ikkalasi ham `status` va
  tarjimalardan ALOHIDA tushuncha
- `[x]` Uch tillilik — 11 ta tarjima jadvali barcha matnli entitylarni
  qamraydi (kontent, qism, fasl, kategoriya, janr, ijodkor, reklama,
  premyera, bildirishnoma, bosh sahifa bo'limi, tarif)
- `[x]` **Uch til MAJBURIY** — nashr qilingan kontent, qism, fasl va faol
  kategoriya/janr/ijodkor uchun (`TranslationRules`). Qoralamada asosiy til
  yetarli — aks holda ish jarayoni to'xtardi
- `[ ]` Tavsif (description) hozircha ixtiyoriy — faqat sarlavha majburiy
- `[ ]` Kontent nusxalash

## 14. Seasons `[x]`

**Entity:** `Season` + `SeasonTranslation` · **Migratsiya:** V3

- `[x]` `GET/POST /content/{id}/seasons` · `PUT/DELETE .../{seasonId}`
- `[x]` Faqat SEASONAL tuzilmada ruxsat (validatsiya bilan)
- `[x]` Afisha, tartib raqami, uch tilda nom
- `[ ]` Faslni boshqa kontentga ko'chirish

## 15. Episodes `[x]`

**Entity:** `Episode` · `EpisodeTranslation` · `EpisodeVideo` ·
**Migratsiya:** V3

- `[x]` `GET/POST /content/{id}/episodes` · `PUT/DELETE .../{episodeId}`
- `[x]` Bir nechta video segment (`partNumber`) — Reels formati uchun
- `[x]` Tilga xos video (`locale` bo'sh = barcha tillar uchun)
- `[x]` Qism darajasida narx va `accessPolicyOverride`
  (masalan 1-qism bepul «ilinma»)
- `[x]` Eskiz, davomiylik, premyera sanasi
- `[ ]` Qismlar tartibini ommaviy o'zgartirish
- `[ ]` Subtitr / audio dorojka (ТЗ da yo'q, lekin video platforma uchun kerak)

## 16. Media `[x]` — ТЗ §26 to'liq

**Entity:** `MediaAsset` · `UploadSession` · **Migratsiya:** V3, V8

- `[x]` `GET /media` — ro'yxat, **qidiruv** (asl fayl nomi bo'yicha),
  tur va holat filtri. Qidiruv YO'Q edi
- `[x]` **Arxivlash** (`POST /media/{id}/archive` · `/restore`) — ТЗ §26
  «remove/archive». Arxivlangan fayl kutubxonada ko'rinmaydi, lekin
  MAVJUD havolalar ishlashda davom etadi
- `[x]` `MediaStatus` enum — ilgari `status` oddiy `String` edi va doim
  `"READY"` yozilardi
- `[x]` `POST /media` — bitta so'rovli yuklash (≤ 50 MB)
- `[x]` **Bo'laklab yuklash** — 5 ta endpoint, davom ettirish bilan
- `[x]` `GET /media/{id}/usage` — fayl **12 joydan** qidiriladi
- `[x]` `DELETE /media/{id}` — ishlatilayotgan bo'lsa 409 + qayerdaligi
- `[x]` `GET /api/v1/app/media/{id}/raw` — rasm ochiq, video entitlement bilan
  (qism videosi ham, kontent videosi ham)
- `[x]` **B15**: pullik video ochiq edi — yopildi
- `[x]` `Range` → 206, seek ishlaydi
- `[x]` Pullik videoga `Cache-Control: no-store`
- `[x]` Qat'iy CSP — SVG ichidagi skript ishlamaydi
- `[x]` UUID nomlar, path traversal ikki marta tekshiriladi
- `[x]` Oqim orqali yozish — RAM'ga to'liq yuklanmaydi
- `[x]` Kengaytma oq ro'yxati, bo'laklashda **eng boshida** tekshiriladi
- `[x]` Tashlab ketilgan sessiyalar sutkada tozalanadi (`@Scheduled`)
- `[x]` `ChunkedUploadTest` (9) · `MediaRangeDeliveryTest` (4) ·
  `MediaDeletionTest` (3) · `PaidContentLeakTest` (7)
- `[ ]` ⚠️ **Timeweb / S3 adapteri** (ТЗ R16) — `StorageService` interfeysi
  tayyor, lekin faqat `LocalStorageService` bor
- `[ ]` Video transkodlash (sifat darajalari, HLS)
- `[ ]` Eskizni videodan avtomatik olish
- `[ ]` Egasiz fayllarni tozalash vazifasi (bazada yo'q, diskda bor)

## 17. Ads `[x]` — ТЗ §27 to'liq

**Entity:** `Advertisement` + `AdvertisementTranslation` ·
**Migratsiya:** V4

- `[x]` `GET · POST · PUT · DELETE /advertisements`
- `[x]` `AdAudience`: `ADVERTISEMENT` (faqat obunasizlarga) ·
  `ADMIN_ANNOUNCEMENT` (hammaga) — R5
- `[x]` Ko'rsatish oynasi (`startAt` / `endAt`), `idx_ad_window`
- `[x]` Tugma va havola boshqariladi (R6): `buttonEnabled`, `linkUrl`
- `[x]` **ТЗ §28**: `LinkType` (NONE · EXTERNAL · INTERNAL) va
  `InternalTargetType` (7 ta) — ro'yxat aynan mos
- `[x]` `InternalLink` @Embeddable — **advertisement, premiere VA
  notification** uchala modulda ham ayni bir mexanizm. Arxitektura testi
  ajralib ketishga yo'l qo'ymaydi
- `[x]` Tilga xos rasm + mobil rasm
- `[x]` `AccessService.shouldShowAds` — Premium reklama ko'rmaydi
- `[x]` **Tugma va havola IXTIYORIY** — `buttonEnabled = false` bo'lsa tugma
  umuman chiqmaydi; banner faqat rasmdan iborat bo'lishi mumkin
- `[x]` `updatedAt` (V15 da qo'shildi, ТЗ §27 talab qiladi)
- `[x]` Sarlavha, tavsif va tugma matni **uch tilda**
- `[x]` `AdvertisementModuleTest` — 10 test, mutatsiya bilan tekshirilgan
- `[x]` **Havola nishoni MAVJUDLIGI tekshiriladi** — `InternalLinkValidator`.
  Ilgari faqat «maydon bo'sh emasmi» tekshirilardi, ya'ni `CONTENT #999999`
  ga havola qiluvchi banner bemalol saqlanardi va mobil ilovada hech
  qayerga olib bormasdi. Xato faqat foydalanuvchi bosgandan keyin bilinardi
- `[x]` **Tashqi havola faqat `http`/`https`** — `javascript:` kabi sxemalar
  admin paneli orqali klientga uzatilmasin
- `[x]` **Matn bo'lsa — uchala tilda** (`TranslationRules.requireAllIfAny`).
  Reklama faqat rasmdan iborat bo'lishi mumkin (matn ixtiyoriy), lekin
  o'zbekcha yozib nashr qilinsa, rus tilidagi foydalanuvchi tushunmaydigan
  bannerni ko'rardi
- `[x]` **Ochiq (klient) endpoint** — `GET /api/v1/app/home` ichida,
  reklama karuseli bo'limi sifatida
- `[ ]` Ko'rsatish chastotasi / kunlik limit

### 17.1. Reklama analitikasi `[x]` — ТЗ §29

**Entity:** `AnalyticsEvent` (xom) → `AdDailyStatistic` (kunlik jamlanma) ·
**Migratsiya:** V6

- `[x]` Beshta ko'rsatkich: impressions · clicks · unique impressions ·
  unique clicks · CTR
- `[x]` **Yengil hodisa endpointi** — `POST /api/v1/app/analytics/events`,
  mobil klient uchun
- `[x]` **Ikki qatlamli**: xom hodisa yoziladi, `@Scheduled` har 5 daqiqada
  jamlaydi. Dashboard millionlab satr ustida `COUNT(*)` qilmaydi
- `[x]` **Har bir reklama uchun alohida endpoint** —
  `GET /advertisements/{id}/statistics?days=N`. Umumiy hisobotda faqat
  TOP-10 chiqardi, ya'ni 30 ta banneri bor admin 25-chisini umuman ko'ra
  olmasdi, ТЗ esa «har bir reklama uchun» deydi
- `[x]` ⚠️ **Unikal sanoq XATOSI tuzatildi.** Jamlash har 5 daqiqada
  ishlaydi va faqat yangi hodisalarni ko'radi. Unikal sanoq ham QO'SHIB
  borilardi — natijada bir soat tomosha qilgan odam ~12 ta «unikal» bo'lib
  hisoblanardi va ko'rsatkich asta-sekin JAMI ga yaqinlashib ma'nosini
  yo'qotardi. Endi unikal butun kun bo'yicha QAYTA hisoblanadi
  (`countUniquesForDay`)
- `[x]` `AdAnalyticsTest` (8) + `AdStatisticsEndpointTest` (6),
  unikal tuzatish mutatsiya bilan tasdiqlangan (2 test yiqiladi)
- `[ ]` Davr bo'yicha aniq unikal (hozir kunlik unikallar YIG'INDISI —
  bir odam ikki kun ko'rsa ikki marta sanaladi)

## 18. Premieres `[x]` — ТЗ §30 to'liq

**Entity:** `Premiere` + `PremiereTranslation` · **Migratsiya:** V4

- `[x]` `GET · POST · PUT · DELETE /premieres`
- `[x]` Oyna, tartib, holat
- `[x]` Rasm va video (`MediaAsset`)
- `[x]` `Purchase` bilan bog'lanish — butun premyera xaridi
- `[x]` **ТЗ §30 maydonlari to'liq**: rasm · video · title · text
  (`subtitle`) · description · external link · internal link · kontentga
  link · CTA tugma · start date · end date · sort order · active/inactive
- `[x]` **Havola nishoni** — film · serial · qism · ijodkor · kasting,
  hammasi `InternalTargetType` da; mavjudligi saqlashda tekshiriladi
- `[x]` **Uch til**: nashrda sarlavha uchala tilda majburiy; tugma yoqilgan
  bo'lsa tugma matni ham — aks holda rus tilidagi ekranda o'zbekcha tugma
  turardi
- `[x]` **Ochiq (klient) endpoint** — `GET /api/v1/app/home` ichida,
  «Yangi premyeralar» bo'limi sifatida
- `[x]` `PremiereModuleTest` — 14 test
- `[ ]` Premyera boshlanganda bildirishnoma yuborish

## 18.1. Homepage `[x]` — ТЗ §31 to'liq

**Entity:** `HomepageSection` + `HomepageSectionTranslation` +
**`HomepageSectionItem`** · **Migratsiya:** V4, **V16**

- `[x]` Bo'limlar: `enabled` · `sortOrder` · `itemLimit` · uch tilli sarlavha
- `[x]` Bo'lim turi yangi qo'shilsa **avtomatik yaratiladi** — enum'ga qiymat
  qo'shish uchun migratsiya yozish shart emas
- `[x]` **`MINI_SERIES` bo'limi qo'shildi** — ТЗ §31 ro'yxatida bor edi,
  kodda yo'q edi
- `[x]` **`HomepageSectionItem` (V16)** — «Custom content rows» uchun.
  Bo'limning O'ZI bor edi, lekin unga QAYSI kontent kirishini saqlaydigan
  joy yo'q edi: admin «Maxsus qator» ni yoqishi mumkin, to'ldirolmasdi.
  Bayroq (`featured`/`popular`) yaramaydi — u bitta qator, maxsus qatorlar
  esa bir nechta va bitta film bir nechtasida turishi mumkin
- `[x]` `GET · PUT /homepage/sections/{id}/items` — ro'yxat tartibi =
  ko'rinish tartibi; takror ID tushunarli xato beradi
- `[x]` **`GET /api/v1/app/home` — mobil ilova bosh sahifasi.** ТЗ: «homepage
  hardcoded bo'lmasin». Bu endpointgacha bosh sahifani boshqarish faqat
  admin panelida bor edi — ilova uni so'raydigan joy yo'q edi
- `[x]` Mehmon ham ko'ra oladi; token yuborilsa hisobga olinadi (faol
  obunasi borlarga tijorat reklamasi qaytarilmaydi, admin e'loni hammaga)
- `[x]` **Bo'sh bo'lim javobga tushmaydi** — klient sarlavhasi bor, ichi
  yo'q qator chizmasin. Soxta element o'ylab topilmaydi
- `[x]` Qatorlarda faqat `PUBLISHED` + `PUBLIC` kontent: `UNLISTED` havola
  orqali, `PRIVATE` xodimlarga
- `[x]` **N+1 yo'q** — `Content.translations` va `Content.media` da
  `@BatchSize(50)`. `@EntityGraph` yaramaydi: ikkita to'plamni birdan
  fetch join qilish `MultipleBagFetchException` beradi
- `[x]` `HomeFeedTest` — 16 test, N+1 nazorati mutatsiya bilan tasdiqlangan
  (batching o'chirilsa 12 ta so'rov, yoqilganda 1 ta)
- `[ ]` `POPULAR_CONTENT` analitika reytingi bo'yicha (hozir qo'lda bayroq,
  §25 dagi kabi — arxitektura tayyor)

## 19. Notifications `[~]`

**Entity:** `Notification` + `NotificationTranslation` · **Migratsiya:** V5

- `[x]` `GET · POST · PUT /notifications`
- `[x]` `POST /notifications/{id}/send` — `NOTIFICATION_SEND`
- `[x]` `POST /notifications/{id}/cancel`
- `[x]` Uch tilda sarlavha va matn, rasm
- `[x]` Rejalashtirish (`scheduledAt`), holat mashinasi
- `[x]` Sahifalash N+1 siz (`PageHydrator`)
- `[x]` **Halol xatti-harakat**: FCM sozlanmagan → **503**, urinish `FAILED`
  holatda saqlanadi, soxta «yuborildi» yozilmaydi
- `[ ]` ⚠️ **FCM ulanishi** (R16) — kalit yo'q. `NotificationAdminService.send()`
  da TODO
- `[ ]` Rejalashtirilganlarni yuboradigan `@Scheduled` vazifa
- `[ ]` Segmentlash (faqat Premium / faqat yangi foydalanuvchilar)
- `[ ]` Yuborish statistikasi (yetkazildi / ochildi)

## 20. Comments `[x]`

**Entity:** `Comment` · **Migratsiya:** V5

- `[x]` `GET /comments` — holat, kontent va shikoyat bo'yicha filtr
- `[x]` `PUT /comments/{id}/status/{status}` — `COMMENT_MODERATE`
- `[x]` `reportsCount` + shikoyat qilinganlar filtri
- `[x]` Indekslar: `status+created`, `content+created`, `reports`
- `[x]` `@EntityGraph` faqat to-one — sahifalash to'g'ri ishlaydi
- `[ ]` Izoh yozish uchun ochiq (klient) endpoint
- `[ ]` Javob (thread) — model yassi
- `[ ]` Avtomatik filtr (so'kinish, spam)

## 21. Subscriptions `[x]`

**Entity:** `Subscription` · `UserAccount.premiumUntil` · **Migratsiya:** V5

- `[x]` `SubscriptionSource`: `PURCHASE` · `ADMIN_GIFT` — sovg'a daromadga
  qo'shilmasligi uchun
- `[x]` Sovg'a mavjud muddat **ustiga** qo'shiladi (ТЗ §38)
- `[x]` Bekor qilinganda yozuv **o'chirilmaydi**, `revokedAt` belgilanadi
- `[x]` `hasActivePremium()` — `AccessService` shunga tayanadi
- `[x]` `idx_subscription_user (user_id, end_at)`
- `[x]` `PremiumLifecycleTest` — entitlement bilan birga tekshiriladi
- `[ ]` Avtomatik uzaytirish (recurring)
- `[ ]` Muddati tugashidan oldin ogohlantirish
- `[ ]` Obuna tarixi uchun klient endpointi

## 22. Tariffs `[x]`

**Entity:** `Tariff` + `TariffTranslation` · **Migratsiya:** V5 (seed bilan)

- `[x]` `GET /tariffs` · `POST` · `PUT /{id}`
- `[x]` Doskadan olingan narxlar seed qilingan
- `[x]` `durationMonths`, `price`, `currency`, `highlighted`, `active`
- `[x]` Uch tilda nom va tavsif
- `[x]` Pul `numeric(12,2)` — **hech qachon float emas**
- `[ ]` O'chirish (faol obunasi borlarni tekshirish bilan)
- `[ ]` Chegirma / aksiya narxi

## 22.1. Kirish siyosati va narx `[x]` — ТЗ §23

- `[x]` `AccessPolicy` — FREE · PREMIUM_ONLY · PURCHASE_ONLY ·
  PREMIUM_OR_PURCHASE
- `[x]` Qism darajasida `accessPolicyOverride` — masalan 1-qism bepul ilinma
- `[x]` `effectiveAccessPolicy()` — o'zinikini, bo'lmasa kontentnikini
- `[x]` Narx **kodda emas**, `cms_platform_setting` da (`3000` / `15000`)
- `[x]` **B24**: sozlamalar migratsiya bilan urug'lanadi va zaxira kod
  e'lon qilgan qiymatdan olinadi — ilgari narx 0 so'm bo'lib qolardi
- `[x]` Sozlama o'zgarishi **darhol** kuchga kiradi, qayta ishga tushirish
  kerak emas — amalda tekshirilgan
- `[x]` Qismning o'z narxi sozlamadan ustun
- `[x]` Pullik SINGLE uchun narx majburiy (film — sotiladigan yagona narsa)
- `[x]` `AccessPricingTest` — 9 test, mutatsiya bilan tekshirilgan

## 23. Donations `[~]`

**Entity:** `DonationTransaction` · `CurrencyPackage` · `UserBalance` ·
**Migratsiya:** V5

- `[x]` `GET /donations/top` — reyting
- `[x]` `GET · POST · PUT · DELETE /currency-packages`
- `[x]` `CurrencyKind`: Stars · Coin (R8)
- `[x]` `DonationTargetType` — kontent yoki ijodkorga
- `[x]` Indekslar: yuboruvchi, maqsad, sana
- `[x]` **Halol**: kurs berilmagan → paketlar narxi 0, daromad `null`
- `[ ]` ⚠️ Stars/Coin **kursi** — buyurtchidan kutilmoqda
- `[ ]` Donat yuborish oqimi (R9): balansdan yechish, yetmasa «to'ldiring»
- `[ ]` Balansni to'ldirish (R10)
- `[ ]` Ijodkorga pul yechib berish

## 24. Analytics `[x]`

**Entity:** `AnalyticsEvent` · `ContentDailyStatistic` · `AdDailyStatistic` ·
**Migratsiya:** V6

- `[x]` `POST /api/v1/app/analytics/events` — ochiq, partiya (≤50 hodisa)
- `[x]` Ikki qavatli model: xom hodisa → `@Scheduled` agregatsiya → kunlik
- `[x]` `AnalyticsEventType` — ko'rish, tugatish, reklama, donat …
- `[x]` `idx_event_agg (event_date, type, processed)`
- `[x]` Rate limiting 60/daqiqa (B14) — soxta ko'rsatkichga qarshi
- `[ ]` Hodisa dublikatini aniqlash (bir xil sessiyadan takroriy)
- `[ ]` Eski xom hodisalarni arxivlash/tozalash
- `[ ]` Voronka (qancha boshladi → qancha tugatdi)

## 25. Reports `[~]`

- `[x]` `GET /reports/overview` — `REPORT_VIEW`
- `[x]` `GET /dashboard/summary` — asosiy ko'rsatkichlar
- `[x]` **Ma'lumot yo'q bo'lsa bo'sh holat** — soxta raqam chiqarilmaydi
- `[ ]` Sana oralig'i bo'yicha filtr
- `[ ]` CSV / Excel eksport
- `[ ]` Daromad hisoboti (obuna + xarid + donat bo'yicha ajratilgan)
- `[ ]` Kontent bo'yicha batafsil hisobot

## 26. Audit logs `[x]`

**Entity:** `AuditLog` · **Migratsiya:** V2

- `[x]` `GET /audit-logs` — rol ≥ ADMIN, filtrlar bilan
- `[x]` `AuditService` — 29 joydan chaqiriladi
- `[x]` Eski/yangi qiymat, aktor, IP, vaqt
- `[x]` **Parol va token yozilmaydi** — chaqiruvlar aniq maydonlar beradi
- `[x]` O'zgartirish/o'chirish endpointi **yo'q** — jurnal faqat qo'shiladi
- `[x]` Indekslar: `actor+created`, `entity+created` (V9)
- `[ ]` «Maxfiy ma'lumot yozilmaydi» degan **avtomatik test** yo'q
- `[ ]` Saqlash muddati siyosati (eski yozuvlarni arxivlash)

## 27. Database migrations `[x]`

| Migratsiya | Nima | Jadvallar |
|---|---|---|
| `V1__baseline` | Eski casting sxemasi — **prodda ishlamaydi** (baseline) | 9 |
| `V2__rbac_and_audit` | RBAC va audit | 2 |
| `V3__cms` | Kontent yadrosi | 17 |
| `V4__homepage_ads_premieres` | Bosh sahifa, reklama, premyera | 6 |
| `V5__engagement_users_monetization` | Izoh, foydalanuvchi, monetizatsiya | 12 |
| `V6__analytics` | Analitika | 3 |
| `V7__purchases` | Bir martalik xaridlar | 1 |
| `V8__upload_sessions` | Bo'laklab yuklash | 1 |
| `V9__query_indexes` | Ro'yxat so'rovlari uchun indekslar | — |
| `V10__staff_profile` | Xodim holati, createdBy, lastLoginAt | 1 |
| `V11__content_visibility_language` | Kontent: visibility, language | — |
| `V12__seed_platform_settings` | Narx va kurs sozlamalarini urug'lash | — |
| `V13__creator_updated_at` | Ijodkor: updatedAt | — |
| `V14__creator_ranking_setting` | Ijodkor reytingi sozlamasi | — |
| `V15__banner_updated_at` | Reklama va premyera: updatedAt | — |

- `[x]` `baseline-on-migrate=true`, `baseline-version=1` — mavjud prod bazasi
  sinmaydi
- `[x]` `ddl-auto=none` — sxemani faqat Flyway boshqaradi
- `[x]` Testlar ham **haqiqiy migratsiya** bilan ishlaydi (H2)
- `[x]` Yangilanish yo'li simulyatsiya qilib tekshirilgan: eski jadvallar
  bilan baza → V1 baseline, V2+ qo'llandi, eski ma'lumot **saqlanib qoldi**
- `[x]` Enum uchun `check` cheklovlari **ataylab yo'q** — har yangi qiymat
  migratsiya talab qilardi
- `[x]` Hech bir migratsiya `drop table` / `delete` qilmaydi
- `[ ]` Orqaga qaytarish (rollback) skriptlari yo'q
- `[ ]` Katta jadval uchun `CONCURRENTLY` indeks strategiyasi

## 28. Indexes `[x]`

- `[x]` **59 indeks** (61 yaratilgan − 2 ortiqchasi olib tashlangan) —
  barchasi kodda mavjud so'rovga asoslangan
- `[x]` V9: `content(deleted_at, created_at)` · `content(deleted_at, status)` ·
  `media_asset(type, created_at)` · `notification(created_at)` ·
  `audit_log(actor_id, created_at)` · `audit_log(entity_type, entity_id, created_at)`
- `[x]` Ortiqcha prefiks indekslar olib tashlandi (`idx_audit_actor`,
  `idx_audit_entity`) — yozish tezligini bekorga sekinlashtirardi
- `[x]` `users.phone/email/google_sub` — `unique` orqali indekslangan
- `[ ]` ⚠️ PostgreSQL'da `where deleted_at is null` **qisman indeksi**
  samaraliroq bo'lardi, lekin H2 uni qo'llab-quvvatlamaydi
- `[ ]` Qidiruv `LIKE '%matn%'` — indeks yordam bermaydi. PostgreSQL'da
  `pg_trgm` yoki full-text kerak
- `[ ]` Haqiqiy yuk ostida `EXPLAIN ANALYZE` bilan tekshirish

## 29. Validation `[~]`

- `[x]` `spring-boot-starter-validation` ulangan
- `[x]` 11 ta DTO da cheklovlar
- `[x]` Telefon `+998 XX XXX XX XX`, parol ≥8 + harf + raqam (R15)
- `[x]` Biznes validatsiyasi servisda: tuzilma mosligi, majburiy o'zbek tili,
  pullik SINGLE uchun narx
- `[x]` **Ruxsat validatsiyadan oldin tekshiriladi** (B16)
- `[x]` Uch til majburiyligi — `TranslationRules`, nashr nuqtasida
- `[x]` Bosh sahifa bo'limlari avtomatik yaratilganda **uch tilda** — ilgari
  faqat o'zbekcha edi
- `[ ]` `HomepageSectionSaveRequest` da **hech qanday cheklov yo'q**
- `[ ]` Sana oralig'i (`startAt < endAt`) annotatsiya bilan tekshirilmaydi
- `[ ]` Narx manfiy bo'lmasligi — `@PositiveOrZero` qo'yilmagan

## 30. Exception handling `[x]`

- `[x]` `GlobalExceptionHandler` — 6 tur ushlanadi
- `[x]` `BusinessException` — kod + xabar + HTTP holat
- `[x]` Bir xil JSON shakl: `{code, message, errors, timestamp}`
- `[x]` `MethodArgumentNotValidException` → 422 + maydon xatolari
- `[x]` 401/403 uchun JSON (ilgari HTML qaytardi)
- `[x]` `MethodArgumentTypeMismatchException` → **400** (B21)
- `[x]` `MissingServletRequestParameterException` · `MultipartException` ·
  `HttpMessageNotReadableException` → **400** (B21)
- `[x]` `HttpRequestMethodNotSupportedException` → **405** (B21)
- `[ ]` `MaxUploadSizeExceededException` → tushunarli xabar
- `[ ]` `DataIntegrityViolationException` → tushunarli 409
- `[ ]` Xato kodlari ro'yxati hujjatlashtirilmagan

## 31. Tests `[x]` — 294 ta

| Test | Soni | Nimani qo'riqlaydi |
|---|---|---|
| `SecurityRulesTest` | 29 | Tokensiz hech narsa ochilmaydi |
| `HyperAdminHierarchyTest` | 9 | HYPER_ADMIN qarori, staff boshqaruvi |
| `SuperAdminScopeTest` | 7 | SUPER_ADMIN doirasi, HyperAdmin yarata olmasligi |
| `StaffManagementTest` | 14 | **ТЗ §12** — ro'yxat, filtrlar, amallar |
| `ContentClassificationTest` | 8 | **ТЗ §13** — tur ≠ kategoriya ≠ janr |
| `SingleContentWatchTest` | 10 | **ТЗ §14/§19/§22** — SINGLE film videosi |
| `ContentVisibilityTest` | 8 | **ТЗ §15** — visibility va language |
| `ThreeLanguageRuleTest` | 11 | **Uch til majburiyligi** — nashrda tekshiriladi |
| `AccessPricingTest` | 9 | **ТЗ §23** — narx sozlamadan, kodda emas |
| `CreatorModuleTest` | 8 | **ТЗ §24** — maydonlar, kasblar, ko'p rolli ijodkor |
| `FeaturedCreatorsTest` | 7 | **ТЗ §25** — mashhur ijodkorlar, reyting strategiyasi |
| `MediaLibraryTest` | 10 | **ТЗ §26** — qidiruv, filtr, arxivlash |
| `AdvertisementModuleTest` | 10 | **ТЗ §27** — ixtiyoriy tugma va havola, o'lik havola rad etiladi |
| `AdAnalyticsTest` | 8 | **ТЗ §29** — beshta ko'rsatkich, unikal qayta hisoblanadi |
| `AdStatisticsEndpointTest` | 6 | **ТЗ §29** — har bir reklama uchun statistika |
| `PremiereModuleTest` | 14 | **ТЗ §30** — maydonlar, havola, uch til |
| `HomeFeedTest` | 16 | **ТЗ §31** — bosh sahifa backenddan, N+1 nazorati |
| `InternalLinkReuseTest` | 9 | **ТЗ §28** — havola mexanizmi uchala modulda bir xil |
| `BackendAuthorizationTest` | 10 | **Ikki qavatli avtorizatsiya, 6 xil escalation urinishi** |
| `BootstrapAccountSecurityTest` | 7 | **Standart parolli master hisob qaytmasin** |
| `AccessServiceTest` | 17 | Entitlement — 4 manbadan |
| `PlatformRoleTest` | 14 | Rol ierarxiyasi, huquq oshirish |
| `PermissionServiceTest` | 12 | Ruxsatlar |
| `ContentStructureTest` | 11 | SINGLE / EPISODIC / SEASONAL |
| `ChunkedUploadTest` | 9 | Bo'laklab yuklash, davom ettirish |
| `PremiumLifecycleTest` | 8 | Premium + entitlement |
| `PaidContentLeakTest` | 7 | Pullik video sizmaydi |
| `RbacIntegrationTest` | 7 | RBAC — HTTP darajasida |
| `SlugGeneratorTest` | 6 | Kirill, apostrof, unikallik |
| `RateLimiterTest` | 5 | Oqim cheklovi, ko'p oqimli xavfsizlik |
| `MediaRangeDeliveryTest` | 4 | `Range` → 206, CSP |
| `ContentListPerformanceTest` | 3 | Sahifalash bazada, N+1 yo'q |
| `MediaDeletionTest` | 3 | Ishlatilayotgan fayl o'chmaydi |
| `PermissionBeforeValidationTest` | 3 | Ruxsat validatsiyadan oldin |
| `ContentEditRoundTripTest` | 4 | Tahrirlash ma'lumot yo'qotmaydi (barcha media roli) |
| `PublicCatalogPrivacyTest` | 2 | Ochiq katalogda PD yo'q |
| `OldCastingFrozenTest` | 5 | **Eski casting yo'l, entity, jadval nomi o'zgarmaydi** |
| `ExistingCastingRegressionTest` | 7 | **Casting oqimi buzilmaydi** — anketa, qabul/rad, bot xabari |
| `AdminEndpointGuardTest` | 1 | Qo'riqlanmagan endpoint yo'q |
| `BackendApplicationTests` | 1 | Kontekst ko'tariladi |

- `[x]` Test profili — xotiradagi H2, tashqi xizmatsiz
- `[x]` **Mutatsiya bilan tekshirilgan**: `PaidContentLeakTest`,
  `AdminEndpointGuardTest`, `ContentListPerformanceTest`,
  `PermissionBeforeValidationTest`, `OldCastingFrozenTest`,
  `ExistingCastingRegressionTest`, `ContentClassificationTest` — kod ataylab
  buzib ko'rilgan va testlar ushlagan
- `[ ]` Xarid oqimi uchun integratsiya testi (`Purchase` yaratish)
- `[ ]` Bildirishnoma jo'natish oqimi testi
- `[ ]` Yuk testi (JMeter / k6)
- `[ ]` Qamrov o'lchanmagan (JaCoCo ulanmagan)

## 32. Security `[x]`

- `[x]` **B1** — ochiq allowlist, qolgan `/api/**` yopiq
- `[x]` **B3** — sirlar `application.properties` dan env'ga
- `[x]` **B14** — rate limiting, Spring Security zanjiridan oldin
- `[x]` **B15** — pullik video yopildi
- `[x]` **B16** — ruxsat validatsiyadan oldin
- `[x]` **B18** — standart parolli HYPER_ADMIN hisobi yopildi
- `[x]` **B19** — ruxsat almashtirishdagi UNIQUE buzilishi
- `[x]` **B20** — noma'lum API yo'li HTML o'rniga JSON 404 qaytaradi
- `[x]` **Spring Security bazaviy rol qoidasi** — `/api/v1/app/admin/**`
  uchun xodim authority'si talab qilinadi. Yangi endpoint qo'shilganda ham
  USER tokeni u yerga umuman yetib bormaydi
- `[x]` Qat'iy CSP media javoblarida
- `[x]` `X-Content-Type-Options: nosniff`
- `[x]` Path traversal — ikki qavat
- `[x]` Ruxsat rad etilganda **404** (mavjudlik oshkor qilinmaydi)
- `[x]` Eski tizim muzlatilgan — tasodifan buzilmaydi
- `[~]` **B2** — ochiq katalog tozalandi, lekin `casting-user/appeal/{id}`
  va `/my/{telegramId}` hali ochiq
- `[ ]` ⚠️ CORS `allowedOrigins("*")` — token bilan ishlaydi, lekin
  prod uchun domen ro'yxati aniqlanishi kerak
- `[ ]` Rate limiter **xotirada** — ko'p instansiya uchun Redis
- `[ ]` HTTPS majburlash / HSTS
- `[ ]` Zavisimostlar zaifligi skaneri (OWASP dependency-check)

## 33. OpenAPI `[ ]`

- `[ ]` `springdoc-openapi` **umuman ulanmagan**
- `[ ]` `/swagger-ui` va `/v3/api-docs`
- `[ ]` Swagger UI faqat xodimlar uchun ochiq bo'lsin
- `[ ]` DTO larga `@Schema` tavsiflari
- `[ ]` Xato javoblari misollari
- `[ ]` Yaratilgan spetsifikatsiyani mobil jamoaga berish

## 34. Performance `[~]`

- `[x]` Sahifalash **bazada** kesiladi — `HHH90003004` yo'qoldi
- `[x]` `PageHydrator` — to'plamlar bitta qo'shimcha so'rovda
- `[x]` N+1 yo'qligi test bilan qo'riqlanadi
- `[x]` Fayl yuklash va berish — oqim orqali, RAM'da to'planmaydi
- `[x]` Video `Range` — butun fayl tortilmaydi
- `[x]` Rasm 30 kun keshlanadi
- `[x]` 59 indeks, ortiqchalari olib tashlangan
- `[ ]` Boshqa ro'yxatlar (`comments`, `users`, `audit`) N+1 uchun
  tekshirilmagan
- `[ ]` Kesh qatlami yo'q (sozlamalar, tariflar har so'rovda bazadan)
- `[ ]` Ulanishlar hovuzi (HikariCP) sozlanmagan — standart qiymatlar
- `[ ]` Yuk ostida profiling qilinmagan

## 35. API namespace

**Eski — MUZLATILGAN** (`OldCastingFrozenTest` qo'riqlaydi):

```
/api/v1/auth · /api/v1/news · /api/v1/file
/api/v1/casting-user · /api/v1/security · /api/v1/admin/statistic
```

**Yangi — `/api/v1/app/**`:**

```
/api/v1/app/admin/auth · staff (+role, permissions, password,
                                 activate/deactivate, block/unblock)
/api/v1/app/admin/users · devices
/api/v1/app/admin/content · seasons · episodes
/api/v1/app/admin/categories · genres · creators
/api/v1/app/admin/media · uploads
/api/v1/app/admin/advertisements · premieres · homepage
/api/v1/app/admin/notifications · comments
/api/v1/app/admin/tariffs · currency-packages · donations
/api/v1/app/admin/reports · dashboard · audit-logs · settings

/api/v1/app/watch/{episodeId}       qism (EPISODIC / SEASONAL)
/api/v1/app/watch/content/{id}     SINGLE kontent (film, klip, shou)
/api/v1/app/media/{id}/raw          rasm ochiq, video entitlement bilan
/api/v1/app/analytics/events        ochiq, rate limit bilan
```

**Klient uchun hali ochilmagan:**

- `[ ]` `/api/v1/app/catalog` — bosh sahifa bo'limlari bilan
- `[ ]` `/api/v1/app/content/{slug}` — kontent sahifasi
- `[ ]` `/api/v1/app/search`
- `[ ]` `/api/v1/app/comments` — yozish va o'qish
- `[ ]` `/api/v1/app/ads` — ko'rsatiladigan reklama
- `[ ]` `/api/v1/app/me` — profil, obuna, balans, qurilmalar
- `[ ]` `/api/v1/app/purchases` — xarid qilish oqimi
- `[ ]` `/api/v1/app/auth/**` — OTP bilan kirish (eski auth o'rniga)

---

## Eng muhim keyingi qadamlar

Ta'siri bo'yicha tartiblangan:

1. **Klient API'lari** — hozir mobil ilova yangi kontentni umuman ololmaydi.
   Katalog, kontent sahifasi, qidiruv, profil.
2. **To'lov** — ⚠️ store billing riski hal qilinsin, keyin provayder
   abstraksiyasi (Click · Payme · Uzum · Stripe).
3. **FCM** — kalit olinsa, `NotificationAdminService.send()` to'ldiriladi.
4. **Refresh token bazasi** — hozir o'g'irlangan tokenni bekor qilib bo'lmaydi.
5. **Qurilma chegarasi (2 ta)** — entity bor, majburlash yo'q.
6. **OpenAPI** — mobil jamoa bilan kontrakt uchun.
7. **Video hosting** — ⚠️ buyurtmachi `tz/roadmap for bunny stream*.md`
   qo'shdi: video Bunny Stream orqali, server «turniket, quvur emas».
   Bu hozirgi lokal saqlash + bo'laklab yuklash + `Range` yechimiga ZID.
   Qaror kutilmoqda (`roadmap.md` → Bunny Stream).
