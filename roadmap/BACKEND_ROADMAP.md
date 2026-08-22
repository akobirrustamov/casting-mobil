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
- `[x]` ⚠️ **BARCHA matnlar uch tilda.** Kartochka uch qatordan iborat:

  ```
  Qalbing egasi        <- title       (majburiy)
  Tez kunda            <- subtitle    (ixtiyoriy)
  Treylerni ko'rish    <- description / tugma matni
  ```

  Sarlavha nashrda uchala tilda **majburiy**. Qolgan matnlar ixtiyoriy —
  kartochka faqat sarlavhadan iborat bo'lishi mumkin — lekin bittasi
  to'ldirila boshlagan bo'lsa, uchala tilda ham to'ldirilsin. Aks holda
  rus tilidagi ekranda o'zbekcha qator turardi va kartochka yarim tarjima
  bo'lib chiqardi
- `[x]` ⚠️ **Sarlavhasiz til qatori JIMGINA TASHLANARDI.** Admin rus
  tabida «Tez kunda» va tavsifni yozib sarlavhani to'ldirmasa, sikl butun
  qatorni o'tkazib yuborardi: saqlash muvaffaqiyatli ko'rinardi, matn esa
  izsiz yo'qolardi. Sarlavhani `null` qilib saqlab ham bo'lmaydi — ustun
  `not null`. Endi aniq xato chiqadi
- `[x]` **Ochiq (klient) endpoint** — `GET /api/v1/app/home` ichida,
  «Yangi premyeralar» bo'limi sifatida
- `[x]` `PremiereModuleTest` — 20 test, uch til qoidalari mutatsiya bilan tekshirilgan
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
- `[x]` ⚠️ **Qo'lda tartiblash BARCHA kontent qatorlarida.** Ilgari u faqat
  `CUSTOM_ROW` da bor edi, qolgan qatorlar qat'iy `publicationDate desc`
  bilan chiqardi — ya'ni admin «Tanlangan» yoki «Mini seriallar» qatorida
  qaysi film birinchi turishini umuman hal qila olmasdi. Endi ro'yxat
  berilsa u ustun, berilmasa avtomatik qoida ishlaydi (aks holda admin har
  bir qatorni qo'lda to'ldirishga majbur bo'lardi va yangi kontent bosh
  sahifaga tushmasdi)
- `[x]` ⚠️ **`PUT /homepage/sections/order` — atomar qayta tartiblash.**
  Ilgari tartib bittalab o'rnatilardi: bo'limni sudrash 8 ta so'rov
  talab qilardi va ular orasida ikkita bo'lim bir xil raqamda turardi —
  o'sha lahzada `/app/home` ni so'ragan foydalanuvchi ARALASHIB KETGAN
  bosh sahifani ko'rardi
- `[x]` Ro'yxatga kirmagan bo'limlar oxiriga suriladi — panel faqat
  ko'rinib turgan bo'limlarni yuborsa ham ziddiyat (bir xil raqam)
  yuzaga kelmaydi
- `[x]` **Kategoriya qatori** tartibi `Category.sortOrder` bilan
  boshqariladi; nofaol kategoriya qatorga tushmaydi
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
- `[x]` `HomeFeedTest` — 23 test. N+1 nazorati, qo'lda tartib, atomar
  qayta tartiblash va kategoriya tartibi — hammasi mutatsiya bilan
  tasdiqlangan
- `[ ]` `POPULAR_CONTENT` analitika reytingi bo'yicha (hozir qo'lda bayroq,
  §25 dagi kabi — arxitektura tayyor)

## 19. Notifications `[x]` — ТЗ §32 · hisobot §33

**Entity:** `Notification` + `NotificationTranslation` · **Migratsiya:** V5

- `[x]` `NotificationType`: `APP_NOTIFICATION` · `CASTING_NOTIFICATION`
- `[x]` Holatlar to'liq: `DRAFT` · `SCHEDULED` · `SENDING` · `SENT` ·
  `FAILED` · `CANCELLED`
- `[x]` `NotificationAudience`: `ALL` · `PREMIUM_ONLY` · `NON_PREMIUM`
- `[x]` `InternalLink` — reklama va premyera bilan **ayni mexanizm** (§28);
  nishon mavjudligi saqlashda tekshiriladi
- `[x]` ТЗ dagi `externalUrl` = `InternalLink.linkUrl`. Nom ataylab
  o'zgartirilmadi: buyurtmachi «bir xil mexanizmni reuse qil» degan (§28),
  uchala modulda bitta ustun nomi turishi kerak
- `[x]` **`NotificationDispatcher` (yangi)** — rejalashtirilganlarni vaqti
  kelganda yuboradi. `scheduledAt` maydoni ham, `SCHEDULED` holati ham bor
  edi, lekin ularni **O'QIYDIGAN hech narsa yo'q edi**: ertaga soat 9 ga
  qo'yilgan xabar abadiy `SCHEDULED` bo'lib qolardi va admin buni faqat
  foydalanuvchilar shikoyat qilganda bilardi
- `[x]` Bir yugurishda 50 ta — yuzlab xabar bir vaqtda kelsa baza uzoq
  band bo'lmasin
- `[x]` ⚠️ **Uch til tekshiruvi YUBORISHNING O'ZIDA.** Ilgari u faqat
  saqlashda va faqat `scheduledAt` berilgan bo'lsa ishlardi — ya'ni teshik
  bor edi: qoralamani o'zbekcha yaratib «yuborish» tugmasini bosish kifoya
  edi va rus tilidagi foydalanuvchiga o'zbekcha push ketardi. Push
  telefonga chiqqandan keyin uni qaytarib olib bo'lmaydi, shuning uchun
  tekshiruv qaysi yo'l bilan kelishidan qat'i nazar bir joyda turadi
- `[x]` Tarjimasi to'liqsiz xabar **`FAILED` deb belgilanmaydi** — u
  buzilgan emas, shunchaki tayyor emas. `FAILED` «provayder ishlamadi»
  degan ma'noni berardi va admin muammoni boshqa joydan qidirardi
- `[x]` ⚠️ **Yarim to'ldirilgan til qatori JIMGINA TASHLANARDI.** Admin
  rus tabida sarlavhani yozib matnni unutsa, sikl butun qatorni o'tkazib
  yuborardi: saqlash muvaffaqiyatli ko'rinardi, sarlavha esa izsiz
  yo'qolardi. Ikkala ustun ham `not null` — endi aniq xato
- `[x]` Rejalashtirishda ham uchala til majburiy
- `[x]` O'tmishdagi vaqtga rejalashtirib bo'lmaydi — u jimgina «hozir
  yuborish» ga aylanib qolmasin
- `[x]` ⚠️ **Provayder sozlanmagan bo'lsa `FAILED`** — «yuborildi» deb
  belgilanmaydi. Soxta muvaffaqiyat foydalanuvchilar xabar olgandek
  taassurot qoldirardi va admin muammoni umuman ko'rmasdi
- `[x]` Urinish natijasi saqlanadi (istisno tashlanmaydi) — izsiz
  yo'qolmaydi
- `[x]` **Ruxsat ROLGA bog'lanmagan** — `NOTIFICATION_VIEW` ·
  `NOTIFICATION_CREATE` · `NOTIFICATION_SEND` alohida. Worker'ga ruxsat
  orqali beriladi (ТЗ §32). Rolga bog'lansa, Workerga bu ishni topshirish
  uchun uni Adminga ko'tarish kerak bo'lardi va u butunlay keraksiz
  huquqlarni ham olardi
- `[x]` Yuborish yaratishdan alohida ruxsat: xabar tayyorlash bilan uni
  minglab telefonga jo'natish bir xil mas'uliyat emas
- `[x]` `NotificationModuleTest` — 24 test; dispatcher va uch til qoidalari
  mutatsiya bilan tekshirilgan
- `[ ]` ⚠️ **FCM ulanmagan** — `APP_FCM_CREDENTIALS` berilishi kerak
- `[ ]` Auditoriya bo'yicha haqiqiy yuborish (token ro'yxati)

### 19.1. Notification report `[x]` — ТЗ §33

`GET /api/v1/app/admin/notifications/{id}/report`

ТЗ: «Mavjud infrastructure qaysi metricni real berishi mumkinligini
aniqlab ishlat. Real ma'lumot bo'lmasa fake statistic yaratma.»

**Beshta ko'rsatkich — bu QABUL QILUVCHILAR bo'yicha voronka:**

```
sent → delivered → opened → clicked
     ↘ failed
```

Ya'ni har biri ODAMLAR sonini bildiradi, xabarning o'z holatini emas.

| Ko'rsatkich | Manba | Holat |
|---|---|---|
| `audienceSize` | bazadagi foydalanuvchilar soni | ✅ real |
| `sent` | push provayderi | ❌ **o'lchanmaydi** |
| `delivered` | provayder kvitansiyasi | ❌ **o'lchanmaydi** |
| `failed` | push provayderi | ❌ **o'lchanmaydi** |
| `opened` | klient hodisasi `NOTIFICATION_OPEN` | ✅ real, unikal bilan |
| `clicked` | klient hodisasi `NOTIFICATION_CLICK` | ✅ real, unikal bilan |

- `[x]` ⚠️ **`sent` ham «o'lchanmaydi» ga o'tkazildi.** Ilgari u
  `1` qaytarardi — «bu xabarning holati SENT». Bu voronkani ma'nosiz
  qilardi: 1 kishiga yuborilgan xabarni 250 kishi ochgan bo'lib chiqardi.
  Raqam o'ylab topilmagan, lekin u **boshqa narsani** o'lchaydi va qo'shni
  ustunlar bilan solishtirib bo'lmaydi — bu ham soxta statistika, faqat
  nozikroq turi
- `[x]` Xabarning O'Z holati yo'qolmadi — u `status` va `failureReason`
  maydonlarida, o'z joyida turadi
- `[x]` Har bir ko'rsatkichda `available` bayrog'i. O'lchanmasa **nol
  emas, `null` + sabab**: nol «bo'lmadi» degani, bilmaslik esa boshqa
  narsa. `delivered = 0` ko'rsatilsa admin «hech kimga yetib bormadi» deb
  o'ylab, butunlay boshqa muammoni qidirardi
- `[x]` **`audienceSize` qo'shildi** — nishon auditoriyasining hozirgi
  hajmi. Usiz `opened` ni umuman talqin qilib bo'lmaydi: 250 ta ochilish
  ko'p ham, oz ham bo'lishi mumkin — auditoriya 300 kishimi yoki
  300 000 kishimi, bilinmaydi. ⚠️ Bu «yuborildi» EMAS, va xodimlar
  sanalmaydi
- `[x]` `NOTIFICATION_CLICK` hodisa turi — odam xabarni ochib, havolani
  bosmasligi mumkin; ikkalasi bitta hodisa bo'lsa «clicked» «opened» ning
  nusxasi bo'lib qolardi
- `[x]` `NotificationModuleTest$Report` — 10 test, mutatsiya bilan
  tekshirilgan
- `[ ]` Qabul qiluvchilar bo'yicha yozuv (`notification_delivery`) — FCM
  ulangandan keyin. Shundan keyin `sent`, `delivered` va `failed` real
  bo'ladi

## 20. Comments `[x]` — ТЗ §34 to'liq

**Entity:** `Comment` · **Migratsiya:** V5

- `[x]` Ro'yxat maydonlari: user · content · episode · matn · createdAt ·
  status · reportsCount
- `[x]` Amallar: ko'rish · yashirish · tiklash · `DELETED` deb belgilash.
  **Hard delete yo'q** — moderator qarori saqlanadi
- `[x]` ⚠️ **Filtrlar BIRGA ishlaydi.** Ilgari `if/else` zanjiri edi va
  filtrlar bir-birini INKOR QILARDI: moderator «yashirilgan» + «shu kino»
  ni birga tanlasa, kino filtri status filtrini jimgina yutib yuborardi va
  ro'yxatda ko'rinadigan izohlar ham chiqardi. Ekranda hech qanday xato
  ko'rinmasdi — shunchaki noto'g'ri ro'yxat edi
- `[x]` **Foydalanuvchi va sana filtri qo'shildi** — ТЗ ro'yxatida bor edi,
  kodda umuman yo'q edi
- `[x]` Matn qidiruvi kamida 2 belgi — bitta harf butun bazani
  skanerlashiga arzimaydi
- `[x]` Ro'yxat maydonlari to'liq: `authorId` · `authorName` ·
  `contentId` · `contentSlug` · `episodeId` (ixtiyoriy) · `text` ·
  `createdAt` · `status` · `reportsCount`
- `[x]` **Muallifni bloklash izoh ekranidan** — moderator `authorId` ni
  ro'yxatdan oladi, bloklash esa `USER_BLOCK` ruxsatini talab qiladi
  (ТЗ: «block user **where authorized**»)
- `[x]` ⚠️ **Telefon raqami standart holatda BERILMAYDI.** Ilgari u har
  doim qaytarilardi — ya'ni faqat `COMMENT_VIEW` ruxsati berilgan xodim
  ham har bir izoh muallifining telefonini ko'rardi. Izoh moderatsiyasi
  butun foydalanuvchi bazasining telefon raqamlariga ochiq eshik bo'lardi.
  Endi u faqat `USER_VIEW` ruxsati bilan ko'rinadi
- `[x]` ⚠️ **N+1 tuzatildi.** `author`, `content` va `episode` — dangasa
  `@ManyToOne`, DTO uchalasiga ham tegadi. Ustiga `User.roles` **EAGER**
  edi va har bir muallif uchun alohida so'rov ketardi: 10 qatorlik sahifa
  11 ta ortiqcha so'rov yuborardi. `join fetch` + `User.roles` ga
  `@BatchSize(50)` — endi 2 ta.
  ⚠️ `@BatchSize` faqat YUKLASH usulini o'zgartiradi: jadval, ustun va
  xatti-harakat o'zgarmaydi, ya'ni eski casting kodi ta'sirlanmaydi
  (`OldCastingFrozenTest` o'tadi)
- `[x]` `ModerationAndUsersTest` — 21 test; filtrlar, maxfiylik va N+1
  mutatsiya bilan tekshirilgan
- `[ ]` Izoh yozish (klient endpointi) — hozir faqat moderatsiya

## 20.1. User management `[x]` — ТЗ §35 to'liq

**Entity:** `User` (eski) + `UserAccount` · **Migratsiya:** V5

- `[x]` Ro'yxat maydonlari: id · avatar · ism · telefon · email · status ·
  premium holati · `premiumUntil` · **`createdAt`** · `lastActiveAt`
- `[x]` ⚠️ **`createdAt` — HAQIQIY ro'yxatdan o'tish sanasi (V17).**
  Bunday ustun hech qayerda yo'q edi. Men uni avval
  `cms_user_account.created_at` dan olgandim — bu XATO edi: hisob satri
  DANGASA yaratiladi, faqat admin biror amal qilganda. Ya'ni ko'pchilik
  foydalanuvchida u umuman yo'q, bo'lganda ham «admin birinchi marta
  tekkan vaqt» ni bildiradi: 2020-yilda ro'yxatdan o'tib 2026-yilda
  bloklangan odam ro'yxatda «2026» bo'lib chiqardi. **Bo'sh katakdan ham
  yomon — admin raqamga ishonadi.**
  Endi `users.created_at`, `@PrePersist` orqali barcha yaratish yo'llarida
  to'ladi
- `[x]` ⚠️ **Mavjud satrlar TO'LDIRILMADI** — ular qachon ro'yxatdan
  o'tganini bilmaymiz va o'ylab topilgan sana yozilmaydi. `null` halol
  javob: «ma'lum emas»
- `[x]` Ustun QO'SHILDI, hech narsa o'chirilmadi — eski casting kodi bu
  ustunni bilmaydi va undan ta'sirlanmaydi
- `[x]` Amallar: qidiruv · ko'rish · bloklash · blokdan chiqarish ·
  premium berish · premium qaytarib olish
- `[x]` **Xodimlar ro'yxatga tushmaydi** — ular §12 dagi alohida ekranda.
  Aralashsa admin o'zini bloklab qo'yishi mumkin edi
- `[x]` ⚠️ **Ro'yxat endi bazada sahifalanadi.** Ilgari `findAll()`
  chaqirilib, xodimlar Java'da ajratilardi va chegara faqat shundan keyin
  qo'llanardi — ya'ni panelni ochish BUTUN jadvalni xotiraga tortardi.
  100 000 ta foydalanuvchida bu har bir sahifa ochilishida 100 000 satr
- `[x]` ⚠️ **N+1 tuzatildi.** Har bir foydalanuvchi uchun hisob, balans va
  qurilmalar alohida so'ralardi: 50 kishilik sahifa 150 ta qo'shimcha
  so'rov degani edi. Endi uchalasi bitta `in (...)` so'rovi bilan
- `[x]` **USER admin panelga kira olmaydi** — ТЗ §35 talabi, **IKKI
  QAVATDA** qo'riqlanadi:
  1. `SecurityConfig` — `/api/v1/app/admin/**` xodim rolini talab qiladi;
  2. `hasPermission` — `PlatformRole.USER` ga ruxsat YOZUVI bo'lsa ham
     `false` qaytaradi, ya'ni oddiy foydalanuvchiga bitta ruxsat berib
     qo'yish uni panelga kiritib yubormaydi.

  `UserCannotEnterPanelTest` — 5 test, uch yo'l tekshiriladi: admin
  login, ilova tokeni bilan admin endpointi, ilova tokeni bilan
  «men kimman». Mutatsiya bilan isbotlangan: bitta qavat buzilsa
  ikkinchisi ushlaydi, ikkalasi birdan buzilsa test yiqiladi

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

## 22. Tariffs `[x]` — ТЗ §36 to'liq

**Entity:** `Tariff` + `TariffTranslation` · **Migratsiya:** V5

- `[x]` Maydonlar: id · code · `durationMonths` · price · currency ·
  active · sortOrder · uch tilli **nom · bejak · tavsif · imkoniyatlar**
- `[x]` ⚠️ **`description` va `features` ALOHIDA (V18).** Ilgari umumiy
  `TranslationDto` ishlatilardi — unda uchta maydon bor, tarifga esa
  to'rttasi kerak. Natijada ТЗ dagi ikki tushuncha bitta ustunga qo'shib
  yuborilgan edi: `description` — nasriy izoh, `features` — nima kirishi
  ro'yxati. Bittasiga ikkalasini sig'dirish admin panelida bitta katta
  matn maydoniga aylanardi va mobil ilova ularni alohida ko'rsata olmasdi
- `[x]` `TariffTextDto` — tarifning o'z DTO'si
- `[x]` Faol tarif nomi uch tilda majburiy — foydalanuvchi PUL to'laydi
- `[x]` ⚠️ **`durationMonths` bezak edi.** Premium berishda u umuman
  o'qilmasdi: muddat faqat `months` parametridan kelardi. Admin
  «12 oy — 159 900» tarifini tanlab `months=1` yuborsa, foydalanuvchi
  1 oy olardi, obuna yozuvida esa 12 oylik tarif turardi — hisobot yolg'on
  ko'rsatardi. Endi tarif o'z muddatini belgilaydi, `months` esa faqat
  tarifsiz erkin sovg'a uchun
- `[x]` Mavjud bo'lmagan tarif ID rad etiladi (ilgari `orElse(null)` —
  noto'g'ri ID jimgina e'tiborsiz qolardi)
- `[x]` ⚠️ **V19: seed qilingan jadvallarning ketma-ketligi tuzatildi.**
  V5 tarif, tarjima va valyuta paketlarini ANIQ ID bilan qo'shadi, lekin
  ketma-ketlikni oldinga surmagan. Natijada admin panelida **birinchi
  yangi tarif yaratish `duplicate key` bilan yiqilardi** — ya'ni §36 ning
  asosiy talabi («admin panel orqali o'zgartirilishi shart») amalda
  bajarilmasdi.
  Java migratsiya: sintaksis portativ emas (PostgreSQL — `setval`,
  H2 — `alter column ... restart with`), V5 ning o'zi tahrirlanmadi —
  u bajarilgan va nazorat summasi buzilardi
- `[x]` ⚠️ **Narx `BigDecimal`** — floating point pul uchun yaramaydi
  (0.1 + 0.2 ≠ 0.3). Buyurtmachi talabi
- `[x]` **Narxlar kodda hardcoded emas** — barchasi bazada, admin panel
  orqali o'zgartiriladi
- `[x]` `GET · POST · PUT /tariffs`
- `[x]` **Valyuta paketi endpointlari DTO'ga o'tkazildi.** Ilgari
  controller `@RequestBody CurrencyPackage` — entity'ni to'g'ridan-to'g'ri
  va HECH QANDAY tekshiruvsiz qabul qilardi: `kind` bo'sh yuborilsa xato
  faqat bazada chiqib, panelda «500 Internal Server Error» ko'rinardi va
  admin nimani to'ldirmaganini bilmasdi
- `[x]` `PlatformSetting` ham DTO orqali qaytadi — API baza sxemasiga
  bog'lanib qolmasin
- `[x]` `TariffModuleTest` — 16 test; pul turi, seed narxlari, tarif
  muddati va ketma-ketlik tuzatishi mutatsiya bilan tekshirilgan

## 22.2. Premium huquqlari va sovg'a `[x]` — ТЗ §37, §38

- `[x]` **Markazlashtirilgan** — barcha entitlement qarorlari
  `AccessService` da. Klientga sochilmagan: mobil ilova, sayt va backend
  bir xil javob beradi
- `[x]` Premium huquqlari: barcha premium kontent · premyeralar ·
  seriallar · filmlar · **reklamasiz ko'rish** (`shouldShowAds`) ·
  **casting loyihasiga kirish** (`canAccessCasting`)
- `[x]` Sovg'a qilish: foydalanuvchi **telefon, email yoki ID** orqali
  topiladi (§38)
- `[x]` ⚠️ ID bo'yicha qidiruv alohida parametr: UUID'ni `like` bilan
  qidirib bo'lmaydi
- `[x]` **Premium UZAYTIRILADI, boshidan boshlanmaydi** — aks holda
  ikkinchi sovg'a birinchisini yeb qo'yardi va foydalanuvchi to'lagan
  muddatini yo'qotardi
- `[x]` Bekor qilish (`revokePremium`)
- `[x]` Har bir amal audit jurnalida: `PREMIUM_GRANTED`, `PREMIUM_REVOKED`

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

## 23. Donations `[x]` — ТЗ §39–§44

**Entity:** `DonationTransaction` · `CurrencyPackage` · `UserBalance` ·
**Migratsiya:** V5

### Valyutalar (§39–§41)

- `[x]` Ikkita virtual valyuta: `STARS` · `COIN`
- `[x]` Nishonlar: **ijodkor** va **kontent** — har biri bo'yicha alohida
  hisoblanadi
- `[x]` Paketlar boshqariladi: miqdor · narx · faol/nofaol · tartib
- `[x]` **Kurs sozlamada, kodda emas**: `currency.star.rate`,
  `currency.coin.rate`
- `[x]` ⚠️ Kurs **0** — buyurtmachi hali aytmagan. Soxta kurs yozilmadi:
  0 «belgilanmagan» degani va so'mdagi ekvivalent umuman hisoblanmaydi

### Balans (§43)

- `[x]` `UserBalance`: `starsBalance` · `coinBalance` · `moneyBalance`
- `[x]` Mobil UI yozilmadi (ТЗ shunday deydi), lekin ma'lumot modeli tayyor

### Hisobot (§42)

- `[x]` `GET /donations/report` — valyuta bo'yicha jamlanma · top
  ijodkorlar · top kontent · kunlik summalar
- `[x]` ⚠️ **STARS va COIN QO'SHILMAYDI.** Ularni bitta «jami» ga qo'shish
  10 so'm va 10 dollarni qo'shishday bo'lardi — kurslari alohida
- `[x]` **Top ijodkorlar va top kontent ALOHIDA ro'yxat** — ilgari faqat
  aralash «top nishonlar» bor edi
- `[x]` `GET /donations/transactions` — sahifalangan ro'yxat
- `[x]` ⚠️ **O'zgarmas tarix**: tahrirlash va o'chirish endpointi ataylab
  YO'Q. Moliyaviy yozuv hard delete qilinmaydi
- `[x]` `DonationAndPaymentTest` — 9 test

### To'lov (§44)

- `[x]` `PaymentProvider` interfeysi — chegara belgilangan, qaysi
  provayder ulansa ham qolgan kod o'zgarmaydi
- `[x]` Provayder nomi **konfiguratsiyadan** (`app.payment.provider`),
  business logic ichida «agar Payme bo'lsa» degan shartlar yo'q
- `[x]` ⚠️ **`NotConfiguredPaymentProvider` — soxta muvaffaqiyat
  QAYTARMAYDI.** Soxta «to'landi» eng xavfli variant bo'lardi:
  foydalanuvchi premium olardi, pul esa hech qayerdan kelmasdi va buni
  faqat oy oxirida hisob-kitobda payqashardi
- `[x]` Xato **503**, 500 emas — bu dastur xatosi emas, sozlama yetishmayapti
- `[ ]` ⚠️ **Haqiqiy integratsiya yo'q** — Payme / Click / Uzum merchant
  ma'lumotlari berilmagan. Buyurtmachi provayderni tanlashi kerak (§44)

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
| `PremiereModuleTest` | 20 | **ТЗ §30** — maydonlar, havola, BARCHA matnlar uch tilda |
| `HomeFeedTest` | 23 | **ТЗ §31** — bosh sahifa backenddan, qator tartibi, N+1 nazorati |
| `NotificationModuleTest` | 30 | **ТЗ §32/§33** — rejalashtirish, soxta muvaffaqiyat yo'q |
| `TariffModuleTest` | 16 | **ТЗ §36** — BigDecimal, seed narxlari, tarif muddati |
| `DonationAndPaymentTest` | 9 | **ТЗ §42/§44** — valyutalar qo'shilmaydi, soxta to'lov yo'q |
| `ModerationAndUsersTest` | 23 | **ТЗ §34/§35/§38** — filtrlar birga, xodimlar ajratilgan, ID qidiruvi |
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
| `UserCannotEnterPanelTest` | 5 | **ТЗ §35** — USER panelga kira olmaydi, ikki qavat |
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
