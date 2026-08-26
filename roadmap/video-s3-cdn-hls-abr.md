# VIDEO — S3 · CDN · HLS · ABR

Belgilar: `[x]` bajarilgan · `[~]` qisman · `[ ]` bajarilmagan
`⚠️` — qaror yoki tashqi bog'liqlik kutilmoqda

Holat **koddan tekshirilgan**, taxmin emas.

**Boshlanish nuqtasi (26.08.2026):** 797 backend test · 26 migratsiya ·
`MediaAsset` + `LocalStorageService` + `ChunkedUploadService` ishlayapti ·
Docker, navbat, `@Async` va deployment konfiguratsiyasi repoda **yo'q**.

---

## 1. Hozirgi arxitektura — koddan aniqlangan `[x]`

### Video qanday yuklanadi

```
ADMIN PANEL (React)
    │  8 MB dan katta fayl
    ▼
POST   /api/v1/app/admin/uploads              sessiya ochiladi
PUT    /api/v1/app/admin/uploads/{id}/chunks/{n}   5 MB lik bo'laklar
POST   /api/v1/app/admin/uploads/{id}/complete
    │
    ▼
SPRING BOOT (ChunkedUploadService)
    │  bo'laklar → backend/files/.uploads/{sessionId}/
    │  yig'ish  → SequenceInputStream
    ▼
LocalStorageService.store()
    │
    ▼
backend/files/{folder}/{uuid}.mp4     ← LOKAL DISK
    │
    ▼
MediaAsset {storageKey, type=VIDEO, sizeBytes, mimeType}
```

### Video qanday ko'riladi

```
MOBIL (expo-video)
    │
    ▼
GET /api/v1/app/watch/{episodeId}
    │  AccessService → allowed / reason / requiredAction
    ▼
sources[] = [{ partNumber, mediaId, url: "/api/v1/app/media/7/raw" }]
    │
    ▼
GET /api/v1/app/media/7/raw        ← SPRING BOOT ORQALI OQADI
    │  AccessService.canReadMedia() qayta tekshiradi
    │  Range qo'llab-quvvatlanadi
    ▼
FileSystemResource → HTTP javob
```

### Mavjud tarkibiy qismlar

| Nima | Qayerda | Holat |
|---|---|---|
| Fayl saqlash abstraksiyasi | `Cms/Service/StorageService` | interfeys tayyor, bitta implementatsiya |
| Lokal disk | `Cms/Service/LocalStorageService` | `backend/files/` ildizi |
| Bo'laklab yuklash | `Cms/Service/ChunkedUploadService` | 5 MB bo'lak, davom ettirish, sutkalik tozalash |
| Sessiya jadvali | `UploadSession` (V8 migratsiya) | `PENDING/COMPLETED/ABORTED` |
| Media yozuvi | `MediaAsset` | `storageKey · type · mimeType · sizeBytes · durationSeconds · width · height · status` |
| Video biriktirish | `EpisodeVideo` → `MediaAsset` | `partNumber · locale · sortOrder` |
| Kontent videosi | `ContentMedia` → `MediaAsset` | yaxlit kontent uchun |
| Ruxsat | `Cms/Service/AccessService` | `/watch` va `/media/{id}/raw` ikkalasini qo'riqlaydi |
| Yetkazish | `Admin/Controller/MediaController` | `Range`, `CacheControl.noStore()` video uchun |

### ⚠️ Nima YO'Q

- `Docker`, `docker-compose`, `nginx`, deployment skriptlari — repoda umuman yo'q
- Navbat infratuzilmasi (RabbitMQ, Kafka, Redis) — yo'q
- `@Async`, `TaskExecutor`, `ExecutorService` — **butun kod bazasida bitta ham yo'q**
- `ffmpeg` / `ffprobe` — ishlab chiqish mashinasida o'rnatilmagan
- AWS SDK — `pom.xml` da yo'q

Mavjud bo'lgani: `@EnableScheduling` va uchta `@Scheduled` metod
(`ChunkedUploadService.cleanUpAbandoned`, `NotificationDispatcher`,
`AnalyticsService`). **Fon ishi uchun mavjud naqsh — aynan shu.**

### ⚠️ Yo'l-yo'lakay topilgan nosozlik (bu ish doirasidan tashqarida)

`application.properties`:

```properties
spring.datasource.password=${akow8434}
```

Bu **parol emas**, `akow8434` nomli xususiyatga havola. Prod profilida
`Could not resolve placeholder 'akow8434'` bilan ko'tarilmaydi. Dev profili
o'z `datasource` ini bergani uchun lokalda bilinmaydi. Tuzatish: qiymatni
`${DB_PASSWORD:...}` shaklida yozish. **Bu ishda tegilmaydi** — ayrim
tuzatish, alohida qaror kerak.

---

## 2. Maqsadli arxitektura

```
ADMIN WEB
    │ 1. POST /admin/media/uploads          (video yozuvi + sessiya)
    ▼
SPRING BOOT ────────► 2. presigned URL / multipart part URL'lari
    │                                              │
    │                                              ▼
    │                          3. ADMIN WEB ──────────────► TIMEWEB S3
    │                             (fayl Spring Boot'dan O'TMAYDI)      │
    │ 4. POST .../complete                                             │
    ▼                                                                  ▼
  HEAD S3 → obyekt bormi?                              videos/{id}/original/source.mp4
    │                                                                  │
    │ 5. transcoding_job → QUEUED                                      │
    ▼                                                                  │
TRANSCODE WORKER (@Scheduled poller)                                   │
    │ 6. S3 → /tmp/video/{id}/source  ◄─────────────────────────────────
    │ 7. ffprobe → kenglik/balandlik/davomiylik/kodek
    │ 8. profil tanlash (upscale QILINMAYDI)
    │ 9. ffmpeg → HLS
    │        ├── 1080p/index.m3u8 + *.m4s
    │        ├── 720p/...
    │        └── 480p/...
    │        └── master.m3u8
    │ 10. rekursiv S3 upload (Content-Type bilan)
    │ 11. /tmp tozalash (try/finally)
    ▼
  status = READY · hlsMasterKey = videos/{id}/hls/master.m3u8
    │
    ▼
TIMEWEB CDN  ◄── origin: S3 bucket
    │
    ▼
MOBIL (expo-video → AVPlayer / ExoPlayer)
    │  master.m3u8 → ABR avtomatik
    ▼
1080p ⇄ 720p ⇄ 480p
```

---

## 3. Qabul qilingan qarorlar va sabablari

### 3.1. HLS maydonlari `MediaAsset` ga qo'shiladi — yangi entity YARATILMAYDI

`EpisodeVideo`, `ContentMedia` va reklama bannerlari — **uchalasi ham**
`MediaAsset` ga ko'rsatadi. Alohida `Video` entity yaratilsa, uchala
biriktirish nuqtasini ham qayta yozish kerak bo'lardi va media kutubxonasi
ikkiga bo'linardi.

`MediaAsset` da allaqachon `durationSeconds`, `width`, `height` bor — ular
hozir **hech qachon to'ldirilmaydi** (`null`), chunki ularni o'lchaydigan
narsa yo'q edi. `ffprobe` aynan shularni to'ldiradi. Ya'ni maydonlar
oldindan o'ylangan, faqat manbasi yo'q edi.

### 3.2. Navbat: baza jadvali + `@Scheduled` poller

Uchta variant baholandi:

| Variant | Nega yo'q / ha |
|---|---|
| `@Async` yolg'iz | ❌ Navbat xotirada. Server qayta ishga tushsa **ish yo'qoladi** va hech kim bilmaydi. 40 daqiqalik transcoding uchun qabul qilib bo'lmaydi |
| RabbitMQ | ❌ Loyihada Docker ham, broker ham yo'q. Yangi infratuzilma + yangi nosozlik nuqtasi. §14 «keraksiz yangi texnologiya qo'shma» |
| **Baza jadvali + `@Scheduled`** | ✅ Qayta ishga tushirishdan omon qoladi · holat va progress tabiiy ravishda saqlanadi · admin panel uni **so'rov bilan ko'ra oladi** · loyihada bu naqsh allaqachon bor (`UploadSession` + `cleanUpAbandoned`) |

Kelajakda RabbitMQ kerak bo'lsa, jadval o'rnini broker egallaydi va
`TranscodeWorker` ning ichki mantig'i o'zgarmaydi.

### 3.3. Multipart upload — MAJBURIY, ixtiyoriy emas

Bitta presigned `PUT` uchun S3 chegarasi — **5 GB**. Talab 20 GB gacha.
Ya'ni multipart tanlov emas, zarurat.

Qo'shimcha foyda: bitta bo'lak uzilsa faqat o'sha qayta yuboriladi — bu
allaqachon `ChunkedUploadService` da amalga oshirilgan mantiq, endi u
S3 tomonga ko'chadi.

### 3.4. Segment formati: fMP4 (`.m4s`), `.ts` emas

| | fMP4 | MPEG-TS |
|---|---|---|
| iOS AVPlayer | ✅ HLS v7 (iOS 10+) | ✅ |
| Android ExoPlayer | ✅ | ✅ |
| Hajm | ~10% kichikroq (kontейner ustamasi kam) | — |
| Kelajakda DASH | ✅ **aynan shu segmentlar** ishlatiladi | ❌ qayta paketlash kerak |

`expo-video` iOS'da AVPlayer, Android'da ExoPlayer ishlatadi — ikkalasi
ham fMP4 HLS'ni nativ qo'llab-quvvatlaydi. Eski qurilma talabi yo'q
(ТЗ 1-bosqich: Android, 2-bosqich: iOS).

**Tanlov: fMP4.**

### 3.5. ⚠️ Mobil bilan shartnoma buzilishi — yangi maydon kerak

Mobil hozir shunday qiladi (`WatchDetail.tsx`):

```ts
useVideoPlayer({ uri: `${BASE_URL}${source.url}`, headers: authHeaders() })
```

Ya'ni `url` ni **nisbiy** deb hisoblaydi va oldiga `BASE_URL` ni qo'yadi.
CDN manzili mutlaq (`https://cdn.../...`), demak `url` maydoniga uni yozish
`https://uzcasting.sitehttps://cdn...` beradi — **jimgina buzilish**.

Shuning uchun `VideoSource` ga **yangi maydon** qo'shiladi:

```java
private String url;      // eski, nisbiy — TEGILMAYDI
private String hlsUrl;   // yangi, mutlaq CDN manzili yoki null
```

- Eski mobil qurilma `hlsUrl` ni bilmaydi → `url` orqali ishlashda davom etadi
- Yangi mobil `hlsUrl` bo'lsa uni oladi, bo'lmasa `url` ga qaytadi
- Bitta relizda ikkalasi ham ishlaydi

Bu §33 (legacy compatibility) va §27 (mavjud response strukturasini saqla)
talablarining bevosita natijasi.

### 3.6. Obyekt kaliti: `mediaId` bo'yicha, series/season/episode bo'yicha EMAS

```
videos/{mediaId}/original/source.{ext}
videos/{mediaId}/hls/master.m3u8
videos/{mediaId}/hls/1080p/index.m3u8
videos/{mediaId}/hls/1080p/segment_00001.m4s
```

Sabab: `MediaAsset` — mustaqil kutubxona yozuvi. Bitta media **bir nechta**
qismga biriktirilishi mumkin (`EpisodeVideo` — alohida jadval, `ManyToOne`).
Kalitni epizodga bog'lash media qayta biriktirilganda kalitni yaroqsiz
qilardi yoki nusxa talab qilardi.

### 3.7. Original faylning taqdiri — Variant A (doim saqlanadi)

§24 uchta variant taklif qiladi. Tanlov: **doim saqlanadi, `private`**.

Sabab: transcoding profillari o'zgarishi mumkin (masalan 1440p qo'shilsa,
yoki kodek AV1 ga o'tsa). Original yo'q bo'lsa qayta transcoding imkonsiz
va kontent abadiy eski sifatda qoladi. Saqlash narxi transcoding'ni qayta
qilish narxidan arzon.

Avtomatik o'chirish **qo'shilmaydi** — buyurtmachi talabi tekshirilmagan
(§24: «avtomatik delete qilishdan oldin mavjud biznes talablarini tekshir»).

---

## 4. Bosqichlar

### 4.1. Fundament — S3 mijoz `[x]`

- `[x]` `pom.xml`: AWS SDK 2.25.60, versiya BOM orqali (26 ta jar)
- `[x]` `Cms/Service/Storage/S3Properties` — `@ConfigurationProperties("app.storage.s3")`
- `[x]` `Cms/Service/Storage/S3Config` — `@ConditionalOnProperty(app.storage.provider=s3)`
- `[x]` `Cms/Service/Storage/S3StorageService implements StorageService`
- `[x]` `Cms/Service/Storage/RoutingStorageService` — `@Primary`, ikki ombor orasida
- `[x]` `Cms/Service/Storage/S3Resource` — `Range` uchun
- `[x]` `Cms/Service/Storage/MediaContentTypes` — HLS turlari bilan
- `[x]` `application.properties.example` yangilandi, kalitlar environment orqali
- `[x]` `StorageKeysTest` — 12 test, 3 mutatsiya bilan tasdiqlangan
- `[x]` `StorageWiringTest` — ikkala rejimda kontekst ko'tarilishi

#### ⚠️ Yo'l-yo'lakay qilingan xato va uning sababi

Birinchi urinishda `S3StorageService` va `RoutingStorageService` da
`@ConditionalOnBean` ishlatildi. **213 ta test yiqildi.**

Sabab: `@ConditionalOnBean` faqat AVTOKONFIGURATSIYA klasslari uchun
ishonchli. Oddiy `@Service` da shart komponent **skanerlash tartibida**
baholanadi — `RoutingStorageService` yaratildi, uning `S3StorageService`
bog'liqligi esa hali ro'yxatga olinmagan edi. Natijada butun ilova
konteksti ko'tarilmadi.

Nosozlik **kompilyatsiyada ko'rinmadi**. Tuzatish: uchala klassda ham
bir xil `@ConditionalOnProperty(app.storage.provider=s3)` — u tartibga
bog'liq emas. `StorageWiringTest` ikkala rejimni ham qo'riqlaydi.

#### Yo'l-yo'lakay qilingan refaktoring va sababi

`StorageKeys` klassi ajratildi. Kengaytma oq ro'yxati va kalit yasash
ilgari `LocalStorageService` ichida **yopiq** turardi. S3 implementatsiyasi
uchun uni nusxalash kerak bo'lardi — ya'ni **xavfsizlik qoidasi ikki
joyda** yashardi va birinchi o'zgarishdayoq ajralardi (aynan shu sabab
`UploadFormatContractTest` yozilgan edi).

§34 «sababsiz refaktoring qilma» talabiga mos: nusxalangan xavfsizlik
tekshiruvi tekshiruvsizlikdan yomonroq — u himoya bor degan taassurot
beradi. `LocalStorageService` ning xatti-harakati o'zgarmadi (36 ta
mavjud test o'tadi).

#### ⚠️ Qabul qilingan muhim qaror: ikki ombor orasida yo'naltirish

S3 yoqilganda barcha murojaatlar unga ketsa, **ilgari yuklangan fayllar
ochilmay qolardi** — ular lokal diskda, S3 da esa umuman yo'q. Baza
yozuvlari joyida, afishalar va videolar «topilmadi» beradi.

`RoutingStorageService` shuni hal qiladi:

```
YOZISH  →  har doim S3
O'QISH  →  lokalda bormi?  ha → lokal · yo'q → S3
O'CHIRISH → IKKALASIDAN ham
```

Lokal tekshiruv avval, chunki u fayl tizimining `stat` chaqiruvi
(mikrosoniya), S3 niki esa tarmoq murojaati (o'nlab millisoniya).

#### ⚠️ `S3Resource` — nega alohida klass kerak bo'ldi

Video `Range` bilan bo'laklab beriladi. Spring buni `ResourceRegion`
orqali qiladi, u esa oqimni ochib **`skip(boshlanish)`** chaqiradi.

Oddiy oqimda `skip` — bu baytlarni **o'qib, tashlab yuborish**. Ya'ni
2 GB lik videoning oxiriga o'tish uchun S3 dan 2 GB tortilardi. Har
safar foydalanuvchi videoni oldinga surganda.

`S3Resource` `skip` ni ushlaydi va o'rniga **ranged GET** yuboradi.

⚠️ `LocalStorageService` **o'chirilmadi va o'zgartirilmadi** (§33).

### 4.2. Presigned upload `[ ]`

- `[ ]` `POST /api/v1/app/admin/uploads` — mavjud endpoint **kengaytiriladi**,
      yangisi yaratilmaydi. Javobga `uploadMode: CHUNKED | S3_MULTIPART` qo'shiladi
- `[ ]` S3 rejimida: `createMultipartUpload` → `uploadId` + bo'lak URL'lari
- `[ ]` `POST .../parts` — keyingi N ta bo'lak uchun presigned URL
- `[ ]` `POST .../complete` — `completeMultipartUpload` + `HEAD` tekshiruvi
- `[ ]` `DELETE .../{id}` — `abortMultipartUpload` (yarim yuklangan bo'laklar
      S3 da pul turadi)
- `[ ]` Frontend: `client.js` da S3 rejimi, progress mavjud UI bilan
- `[ ]` Testlar: holat o'tishlari, `complete` da obyekt yo'q bo'lsa rad etish

⚠️ Presigned URL **logga yozilmaydi** (§29) — u imzo bilan birga to'liq
kirish huquqini beradi.

### 4.3. Transcoding jadvali va holatlar `[ ]`

- `[ ]` Migratsiya `V27__video_transcoding.sql`
- `[ ]` `MediaAsset` ga qo'shiladigan maydonlar:

| Maydon | Nega kerak |
|---|---|
| `processing_status` | `MediaStatus` **kengaytirilmaydi** — u `READY/ARCHIVED` va kutubxona ko'rinishini boshqaradi. Transcoding holati boshqa o'q |
| `hls_master_key` | `null` = HLS yo'q → eski `raw` yo'liga qaytiladi (§33) |
| `original_object_key` | `storageKey` dan farqli: S3 dagi original |
| `video_codec` · `audio_codec` | `ffprobe` dan; qayta transcoding kerakligini aniqlash uchun |
| `processing_error` | Admin panel nima uchun yiqilganini ko'rsatishi kerak (§18) |

`duration_seconds`, `width`, `height` **allaqachon bor** — yangi maydon emas,
faqat endi to'ldiriladi.

- `[ ]` `transcoding_job` jadvali: `media_id · status · attempts · progress ·
      started_at · finished_at · error · created_at`
- `[ ]` `VideoProcessingStatus` enum: `NONE · QUEUED · PROBING · TRANSCODING ·
      UPLOADING · READY · FAILED`

⚠️ `NONE` — eski yozuvlar uchun. Migratsiya mavjud videolarga `NONE` qo'yadi,
`FAILED` emas: ular buzilgan emas, shunchaki HLS'siz.

### 4.4. ffprobe `[ ]`

- `[ ]` `Cms/Service/Video/VideoProbeService` — `ffprobe -v quiet -print_format
      json -show_format -show_streams`
- `[ ]` JSON tahlili → `width · height · durationSeconds · fps · videoCodec ·
      audioCodec · bitrate`
- `[ ]` `ffprobe` topilmasa — aniq xato, jimgina `null` emas
- `[ ]` Testlar: haqiqiy `ffprobe` JSON namunalari bo'yicha tahlil (mock, real S3 emas)

### 4.5. Profil tanlash `[ ]`

- `[ ]` `app.video.transcoding.profiles[]` konfiguratsiyasi
- `[ ]` `VideoProfileSelector` — original balandligidan **yuqoriga chiqmaydi**

```
2160p manba → 1080p · 720p · 480p
1080p manba → 1080p · 720p · 480p
 720p manba →  720p · 480p
 480p manba →  480p
 360p manba →  360p (eng past profil, o'zgarishsiz)
```

- `[ ]` Testlar: har bir manba balandligi uchun kutilgan ro'yxat

⚠️ Upscale **qilinmaydi**: 720p ni 1080p ga cho'zish sifat qo'shmaydi,
faqat disk va CPU sarflaydi (§9).

### 4.6. FFmpeg → HLS `[ ]`

- `[ ]` `Cms/Service/Video/HlsTranscodingService`
- `[ ]` H.264 (`libx264`) + AAC — mobil moslik uchun (§10)
- `[ ]` fMP4 segmentlar, 6 soniyalik (HLS tavsiyasi)
- `[ ]` `master.m3u8` — `BANDWIDTH`, `RESOLUTION`, `CODECS` atributlari bilan
- `[ ]` Bitratelar konfiguratsiyadan, kodda qattiq yozilmaydi (§10)
- `[ ]` Progress: `ffmpeg -progress` chiqishidan foiz hisoblanadi (§19 Phase 2)

### 4.7. Worker `[ ]`

- `[ ]` `Cms/Service/Video/TranscodeWorker` — `@Scheduled(fixedDelay)`
- `[ ]` `app.video.max-concurrent-jobs` (default **1**)
- `[ ]` Ish olish: `SELECT ... FOR UPDATE SKIP LOCKED` — ikki instans bir
      ishni ikki marta olmasin
- `[ ]` `try/finally` bilan kafolatli `/tmp` tozalash (§16)
- `[ ]` Yiqilishda: `attempts++`, 3 martadan keyin `FAILED`
- `[ ]` Testlar: holat o'tishlari, tozalash, bir vaqtdagilar chegarasi

⚠️ Default `1` — FFmpeg butun protsessorni egallaydi va u **API server bilan
bitta mashinada** turibdi (Docker yo'q, ajratish yo'q). Ikkitasi API'ni
sekinlashtiradi.

### 4.8. S3 ga HLS yuklash `[ ]`

- `[ ]` Rekursiv yuklash, har bir fayl uchun to'g'ri `Content-Type`:

| Kengaytma | Content-Type |
|---|---|
| `.m3u8` | `application/vnd.apple.mpegurl` |
| `.m4s` | `video/iso.segment` |
| `.mp4` (init) | `video/mp4` |

- `[ ]` `status = READY` **faqat hammasi yuklangach** (§20)
- `[ ]` Qisman yuklash → `FAILED` + tozalash

### 4.9. CDN va yetkazish `[ ]`

- `[ ]` `app.video.cdn.base-url` konfiguratsiyasi
- `[ ]` Bazada **obyekt kaliti** saqlanadi, to'liq URL emas (§22) — domen
      almashtirish uchun
- `[ ]` `WatchController.sources()` → `hlsUrl` (yangi maydon, §3.5)
- `[ ]` `hlsMasterKey == null` bo'lsa `hlsUrl = null`, eski `url` ishlaydi
- `[ ]` Testlar: URL yasash, eski media uchun `null`

### 4.10. ⚠️ Xavfsizlik — QAROR KUTILMOQDA `[ ]`

Hozir `/api/v1/app/media/{id}/raw` har so'rovda `AccessService.canReadMedia()`
ni chaqiradi. CDN'ga o'tilganda **bu tekshiruv yo'qoladi**: segmentlar Spring
Boot'dan o'tmaydi.

Uchta variant:

| Variant | Qanday | Xavf |
|---|---|---|
| A. Ochiq CDN | Segmentlar hammaga ochiq | ❌ Pullik kontent bepul tarqaladi. **Qabul qilib bo'lmaydi** |
| B. CDN Secure Token | Timeweb tokenli havola qo'llab-quvvatlasa | ⚠️ Timeweb hujjatidan tasdiqlash kerak |
| C. Qisqa muddatli presigned | `master.m3u8` presigned, segmentlar ham | ⚠️ Har segment uchun URL kerak → playlist dinamik yasaladi |

**Tanlanmaydi** — Timeweb CDN ning tokenli havola imkoniyati tasdiqlanmaguncha.
Bu tashqi bog'liqlik, taxmin qilinmaydi.

Vaqtincha: `4.1`–`4.9` bosqichlari **bepul kontentda** to'liq ishlaydi.
Pullik kontent hozirgi `raw` yo'lida qoladi — ya'ni regressiya yo'q.

### 4.11. Admin panel `[ ]`

- `[ ]` `MediaPicker` da holat: `UPLOADING n% · PROCESSING n% · READY · FAILED`
- `[ ]` `FAILED` uchun sabab va **qayta urinish** tugmasi (§18)
- `[ ]` `POST /api/v1/app/admin/media/{id}/retry-transcoding`
- `[ ]` Mavjud `uz-*` komponentlari ishlatiladi, yangi dizayn tizimi yaratilmaydi (§25)
- `[ ]` Uchala tilga i18n kalitlari

### 4.12. Mobil `[ ]`

- `[ ]` `VideoSource` turiga `hlsUrl` qo'shiladi
- `[ ]` Pleyer: `hlsUrl ?? BASE_URL + url`
- `[ ]` `expo-video` ABR'ni o'zi boshqaradi — **qo'lda tezlik hisoblash yozilmaydi** (§26)
- `[ ]` Testlar: `hlsUrl` bor/yo'q holatlari

### 4.13. Infratuzilma `[ ]`

- `[ ]` ⚠️ FFmpeg serverga o'rnatilishi kerak — Docker yo'q, ya'ni
      **qo'lda o'rnatish** yoki Docker joriy qilish qarori kerak
- `[ ]` `app.video.temp-dir` konfiguratsiyasi (`app.upload.temp-dir` naqshi bo'yicha)
- `[ ]` Disk joyi monitoringi

---

## 5. Umumiy checklist

- `[x]` Mavjud video arxitekturasi auditi
- `[x]` Mavjud upload oqimi auditi
- `[x]` Navbat variantlarini baholash
- `[x]` Segment formati tanlovi (fMP4)
- `[x]` Mobil shartnoma buzilishini aniqlash (`hlsUrl`)
- `[x]` S3 konfiguratsiyasi
- `[x]` S3 mijoz (`S3StorageService`)
- `[ ]` Presigned multipart upload
- `[ ]` Upload tugaganini tekshirish (`HEAD`)
- `[ ]` `ffprobe` integratsiyasi
- `[ ]` Transcoding profillari
- `[ ]` HLS generatori
- `[ ]` `master.m3u8`
- `[ ]` S3 HLS yuklovchi
- `[ ]` Processing holatlari
- `[ ]` Fon worker
- `[ ]` Yiqilishni boshqarish va qayta urinish
- `[ ]` Vaqtinchalik fayllarni tozalash
- `[ ]` CDN URL integratsiyasi
- `[ ]` ⚠️ Xavfsiz video kirish (Timeweb qarorini kutmoqda)
- `[ ]` Admin upload progress
- `[ ]` Admin processing holati
- `[ ]` Mobil HLS ijro
- `[ ]` ABR tekshiruvi
- `[ ]` Testlar
- `[ ]` FFmpeg o'rnatish / Docker qarori
- `[ ]` Hujjat

---

## 6. Buyurtmachiga savollar

1. **Timeweb CDN tokenli havolani qo'llab-quvvatlaydimi?** §4.10 shunga
   bog'liq. Qo'llab-quvvatlamasa, pullik kontent uchun boshqa yechim kerak
2. **FFmpeg qayerda ishlaydi?** API bilan bitta VPS'da (arzon, lekin
   transcoding paytida API sekinlashadi) yoki alohida mashinada (qimmat,
   lekin ajratilgan)
3. **Docker joriy qilinadimi?** Hozir repoda yo'q. FFmpeg va worker'ni
   ajratish uchun u eng tabiiy yo'l
4. **Mavjud videolar migratsiya qilinadimi?** Lokal diskdagi fayllar S3 ga
   ko'chirilib, HLS ga o'girilsinmi — yoki eski yo'lda qolsinmi

---

## 7. Tarix

**26.08.2026** — audit o'tkazildi, roadmap yozildi. Kod hali yozilmagan.
