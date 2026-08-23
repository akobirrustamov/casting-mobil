# UZCASTING BACKEND ARCHITECTURE

> Hozirgi holat + qurilayotgan arxitektura. Kod bilan birga yangilanadi.
> Task ro'yxati → [BACKEND_ROADMAP.md](./BACKEND_ROADMAP.md)

Oxirgi yangilanish: 19.08.2026 — CMS modeli qurildi

---

## 1. Current architecture

Spring Boot 3.1.2 · Java 17 · Maven · PostgreSQL · qatlamli monolit.

```
Controller  →  Service (interface + Impl)  →  Repository (Spring Data JPA)  →  PostgreSQL
                     ↑
              Security (JwtService, MyFilter, SecurityConfig)
```

Build: `./mvnw -f backend/pom.xml package` → `target/backend-0.0.1-SNAPSHOT.jar` (fat jar,
frontend build'i `src/main/resources/static/` ichida — bitta jar butun saytni beradi).

⚠️ Ilova **repozitoriy root'idan** ishga tushirilishi kerak: `Attachment` fayllarni
nisbiy `backend/files/...` yo'liga yozadi.

---

## 2. Packages / modules

Paket nomlari bosh harfli (nostandart, lekin izchil — saqlanadi):

| Paket | Mazmuni |
|---|---|
| `Config` | `AutoRun` (rol/admin seeder), `LocalSeeder`, `RestTemplateConfig`, `WebMvcConfig` |
| `Controller` | REST kirish nuqtalari |
| `DTO` | Request/response modellari |
| `Entity` | JPA entity'lar |
| `Enums` | `UserRoles`, `AppealTypes` |
| `Repository` | Spring Data JPA |
| `Security` | `JwtService`, `MyFilter`, `SecurityConfig`, `GoogleTokenVerifier` |
| `Services` | Biznes logika, `*Service` + `*ServiceImpl` |
| `Projection` | Interfeys-proyeksiyalar (`DashboardProjection`) |

**Yangi modullar** (PHASE 3+) shu konvensiyaga amal qiladi.

---

## 3. Authentication

- **JWT** (jjwt 0.11.5, HS256). Subject — `User.id` (UUID).
- **Token turi ajratilgan** (§61): `typ` da'vosi — `access` yoki `refresh`.
  `MyFilter` refresh tokenni API kaliti sifatida rad etadi. Ilgari ikkalasi
  bir xil edi va o'g'irlangan refresh token bilan butun API ochiq edi.
- Access token **15 daqiqa** (`app.jwt.access-token-ms`, ilgari 100 daqiqa),
  refresh 24 soat.
- **Refresh token ro'yxati** — `refresh_token` jadvali (V25). Rotatsiya: har
  yangilashda eskisi bekor qilinadi. Bekor qilingan token qayta kelsa —
  o'g'rilik belgisi, foydalanuvchining butun zanjiri yopiladi.
  Token matni saqlanmaydi, faqat `jti`.
- **Chiqish** — `POST /api/v1/app/admin/auth/logout`, token serverda bekor qilinadi.
- Refresh token **httpOnly + Secure + SameSite=Strict cookie**da, javob tanasida emas.
- **Muvaffaqiyatsiz kirish himoyasi** — `LoginAttemptService`: hisob bo'yicha
  5 xatodan keyin 15 daqiqa. IP bo'yicha rate limit (`RateLimitFilter`) alohida.
- **BCrypt** parol xeshlash (`SecurityConfig.passwordEncoder`). `User.password`
  `WRITE_ONLY` — hash javobda hech qachon chiqmaydi.
- **Google login** — `POST /api/v1/auth/google`, `GoogleTokenVerifier` ID-token'ni tekshiradi,
  `googleSub` bo'yicha user topiladi/yaratiladi.
- `MyFilter` (`OncePerRequestFilter`) — `Authorization` header'dan token oladi, tekshiradi,
  `SecurityContext`ga joylaydi. **Hozir yaroqsiz tokenda so'rovni to'xtatmaydi.**

`User implements UserDetails`; `getUsername()` — telefon, yo'q bo'lsa email.

**Vaqt mintaqasi:** `app.timezone=Asia/Tashkent` (§68). Kodda 310 dan
ortiq `LocalDateTime` bor va u mintaqani saqlamaydi — sozlamasiz
konteynerdagi UTC tufayli rejalashtirilgan premyera besh soat kech
chiqardi.

---

## 4. Authorization

**Hozirgi holat — himoya amalda yo'q.** `SecurityConfig` barcha metod va yo'lda
`permitAll` beradi, shu sababli `MyFilter` natijasi ishlatilmaydi.

**Rejalashtirilgan model:**

```
PlatformRole:  HYPER_ADMIN > SUPER_ADMIN > ADMIN > WORKER > USER
```

- Role ierarxiyasi — yuqori rol quyi rolning barcha huquqiga ega
- `WORKER` — fine-grained `Permission` orqali (`CONTENT_CREATE`, `COMMENT_MODERATE`, ...)
- Staff yaratish ierarxiyasi endpoint darajasida tekshiriladi (privilege escalation yo'q)
- **Frontend menyu yashirish xavfsizlik emas** — har bir endpoint mustaqil tekshiradi

Mavjud `UserRoles` enum saqlanadi (production DB'da satrlar bor), `PlatformRole`ga
`RoleMapper` orqali o'giriladi. `ROLE_GIPERSUPERADMIN` → `HYPER_ADMIN`.

---

## 5. Database

PostgreSQL. Hozir **`ddl-auto=update`** — Hibernate sxemani o'zi boshqaradi.

**Reja:** Flyway kiritiladi (`baseline-on-migrate=true`), `V1__baseline.sql` mavjud
sxemani qayd etadi, keyingi barcha o'zgarish migration orqali. `ddl-auto` → `validate`
faqat baseline production'da tekshirilgandan keyin.

Mavjud jadvallar: `users`, `role`, `users_roles`, `attachment`, `casting_user`,
`casting_user_photos`, `news`, `message`.

### ID strategiyasi

| Entity | ID |
|---|---|
| `User` | `UUID` (AUTO) |
| `Attachment` | `UUID` (qo'lda) |
| `CastingUser` | `Integer` (IDENTITY) |
| `Role` | `int` (qo'lda — texnik qarz) |
| **Yangi entity'lar** | **`Long` + IDENTITY** |

---

## 6. Media storage

**Hozir:** `Attachment.createAttachment()` — entity ichida `FileOutputStream`.
Fayl `backend/files{prefix}/{uuid}_{originalFilename}` ga yoziladi. Abstraction yo'q.

**Qurildi:** `MediaAsset` entity + `StorageService` interfeysi:

```
StorageService
  ├── LocalStorageService      ✅ backend/files/ ostida
  ├── TimewebStorageService    ⬜ prod uchun
  └── S3StorageService         ⬜ kelajak
```

Provider nomi biznes logikaga qotirilmagan. Yuklash `Files.copy(inputStream, ...)`
orqali — katta video RAM'ga to'liq yuklanmaydi.

**Xavfsizlik:** kengaytma oq ro'yxati; fayl nomi butunlay server tomonida
(`UUID + kengaytma`); foydalanuvchi yuborgan nom yo'l sifatida ishlatilmaydi;
`normalize()` + `startsWith(ROOT)` tekshiruvi saqlashda ham, o'qishda ham.

`Attachment` (casting moduli) TEGILMAYDI — u ishlayotgan bot oqimida.

---

## 7. Content model — QURILDI

Joylashuv: `com.example.backend.Cms.{Entity,Enums,Repository,Service,Dev}`.
Alohida paket — mavjud `Entity`/`Repository` bilan aralashtirilmaydi (D16).
⚠️ `BackendApplication` dagi `@EntityScan`/`@EnableJpaRepositories` ro'yxatiga
qo'shilishi SHART, aks holda repozitoriylar topilmaydi.

```
Content (SINGLE | EPISODIC | SEASONAL)
  ├── orientation: LANDSCAPE (YouTube) | VERTICAL (Reels)
  ├── ContentTranslation*  ← UZ / RU / EN: title, shortDescription, description
  ├── ContentMedia*        ← role + locale(nullable) + MediaAsset
  ├── ContentCredit*       → Creator (profession, characterName, sortOrder)
  ├── Category (1)
  ├── Genre*               (many-to-many)
  ├── Season*              (faqat SEASONAL)
  │     ├── SeasonTranslation*
  │     └── Episode*
  └── Episode*             (EPISODIC — seasonId = NULL)
        ├── EpisodeTranslation*
        └── EpisodeVideo*  ← bir epizodda bir nechta video part + dublyaj tili
```

### Ko'p tillilik

**Matn** — alohida jadval, `UNIQUE(parent_id, locale)`. JSON ustun EMAS: tilga
qarab qidirish va indekslash kerak.

**Media** — `ContentMedia.locale` ixtiyoriy:
`NULL` = barcha tillar uchun, `RU` = faqat rus tilida. Tanlash: avval aniq til,
topilmasa `NULL`. Ya'ni har bir til uchun alohida afisha **mumkin, majburiy emas**.

### Boshqa

- `Content` va `Episode` da `@Version` — ikki admin bir vaqtda tahrirlaganda konflikt aniqlanadi
- `Content.deletedAt` — soft delete
- Narxlar `BigDecimal`, hech qachon `double` emas
- **`episode.videoUrl` kabi yagona maydon YO'Q** — `EpisodeVideo` ro'yxati
- `Episode.effectiveAccessPolicy()` — o'zinikini, bo'lmasa kontentnikini beradi
- Ro'yxat so'rovlarida `@EntityGraph` — N+1 oldini olish

---

## 8. Monetization model

```
AccessPolicy: FREE | PREMIUM_ONLY | PURCHASE_ONLY | PREMIUM_OR_PURCHASE
```

Entitlement 4 manbadan kelishi mumkin:
1. Bitta qism xaridi (default 3 000 so'm — DB'da, hardcode emas)
2. Butun premyera xaridi
3. Faol Premium obuna
4. Bepul kontent

**Yagona kirish nuqtasi:** `AccessService.canWatch(user, episode)`. Logika klientga
yoki bir nechta servisga sochilmaydi.

**Casting loyihasiga kirish:** Premium ✓ · bir martalik xarid ✗

### Balans va donat

```
UserBalance: moneyBalance (so'm) · starsBalance · coinBalance
```

Kurs (`1 STAR = X so'm`, `1 COIN = X so'm`) va paketlar admin paneldan boshqariladi.
`DonationTransaction` — **immutable**, hard delete yo'q. Donat har bir kontent va
ijodkor bo'yicha alohida hisoblanadi.

---

## 9. Analytics model

Klient event yuboradi → ingestion endpoint → raw event → kunlik agregat.

```
AD_IMPRESSION · AD_CLICK · CONTENT_VIEW · CONTENT_PLAY · CONTENT_COMPLETE · NOTIFICATION_OPEN
        ↓
  AdDailyStatistic · ContentDailyStatistic
        ↓
  GET /api/v1/app/admin/dashboard/summary
```

Dashboard hech qachon millionlab raw event ustida `COUNT(*)` qilmaydi.
Ma'lumot yo'q bo'lsa — empty state, **fake raqam emas**.

---

## 10. Audit model

`AuditLog`: `actorId`, `actorRole`, `action`, `entityType`, `entityId`, `before`, `after`,
`ip`, `userAgent`, `timestamp`.

Parol, token, to'lov credential'i **yozilmaydi**. Oddiy Admin audit logni o'chira olmaydi.

---

## 11. Important dependencies

| Dependency | Versiya | Nima uchun |
|---|---|---|
| `spring-boot-starter-web` | 3.1.2 | REST |
| `spring-boot-starter-data-jpa` | 3.1.2 | ORM |
| `spring-boot-starter-security` | 3.1.2 | Auth/authz |
| `jjwt` (api/impl/jackson) | 0.11.5 | JWT |
| `postgresql` | runtime | DB |
| `h2` | runtime | lokal profil |
| `lombok` | optional | boilerplate |
| `poi`, `poi-ooxml` | 5.2.3 | Excel eksport (casting) |
| `ucanaccess` | 5.0.1 | ⚠️ MS Access drayveri — ishlatilishi tekshirilishi kerak |

**Reja bo'yicha qo'shiladi:** `flyway-core`, `springdoc-openapi`, `testcontainers`.
Yangi dependency faqat real muammoni hal qilsa qo'shiladi (§70).
