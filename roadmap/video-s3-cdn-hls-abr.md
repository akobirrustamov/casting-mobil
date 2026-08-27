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

### 4.2. Presigned upload `[x]`

- `[x]` Migratsiya `V27__upload_session_s3.sql` — uchta ustun, sukut
      qiymat `CHUNKED` (mavjud sessiyalar davom ettiriladi)
- `[x]` `UploadMode` enum · `UploadSession` ga `uploadMode · s3UploadId · storageKey`
- `[x]` `S3MultipartUploadService` — `begin · presignPart · receivedParts ·
      complete · abort`
- `[x]` `POST /api/v1/app/admin/uploads` **kengaytirildi**, yangi endpoint
      yaratilmadi. Javobga `uploadMode` qo'shildi
- `[x]` `POST .../parts` — guruh bilan imzolangan havolalar (sukut 20 ta)
- `[x]` `complete` — `ListParts` + yig'ish + `HEAD` tekshiruvi
- `[x]` `abort` va sutkalik tozalash S3 bo'laklarini ham bekor qiladi
- `[x]` `saveChunk` S3 rejimida **rad etadi**
- `[x]` Frontend: `client.js` da S3 rejimi, havolalar keshlanadi
- `[x]` `S3MultipartUploadTest` 14 test · `uploadResume.test.js` +3 test
- `[x]` 9 mutatsiya bilan tasdiqlangan (6 backend + 3 frontend)

#### Qabul qilingan qarorlar

**Bo'lak o'lchami 10 MB, 5 MB emas.** 20 GB lik fayl 10 MB da 2048 ta
bo'lak beradi (S3 chegarasi 10 000), 5 MB da esa 4096 ta bo'lardi va
har biri uchun alohida imzolangan havola kerak bo'lardi.

**ETag'lar bazada saqlanmaydi.** Yig'ishda ular S3 dan `ListParts`
bilan so'raladi. Klient qaytarib yuborishi ham mumkin edi, lekin unda
yolg'on ma'lumot yuborish imkoni paydo bo'lardi va uzilishdan keyin
klient ularni unutgan bo'lardi. Bu `ChunkedUploadService` dagi qaror
bilan bir xil — u ham bo'laklarni bazada emas, **omborning o'zida**
sanaydi.

**`listPartsPaginator`, oddiy `listParts` emas.** Oddiy chaqiruv bir
marta atigi 1000 ta bo'lak qaytaradi. 20 GB lik faylda ular 2048 ta va
ro'yxat **jimgina yarmida kesilardi** — «bo'laklar to'liq emas» xatosi
sababi tushunarsiz bo'lgan holda.

**Frontendda `fetch`, `axios` emas.** `axios` bizning interceptor'imizga
ega va u har bir so'rovga `Authorization` qo'shadi. Ombor esa imzoni
tekshiradi va begona sarlavhani ko'rib so'rovni **rad etadi**.

**`saveChunk` S3 rejimida rad etadi.** Usiz klient eski yo'ldan
foydalanishda davom etardi va butun maqsad yo'qolardi: 10 GB baribir
server orqali oqardi. Undan ham yomoni — bo'laklar diskka yozilardi,
S3 ga esa hech narsa tushmasdi.

#### ⚠️ Ikkinchi marta yo'l qo'ygan xatoim: bo'sh test

`presignPart` uchun yozgan birinchi testim **hech narsani sinamasdi**:
u `1` berib `1` kutgan. Aylantirish esa `ChunkedUploadService` da
yashaydi, ya'ni test noto'g'ri qatlamda edi.

Mutatsiya sinovi buni ochdi — `chunkIndex + 1` ni `chunkIndex` ga
o'zgartirganda test o'tishda **davom etdi**. Klient 0 dan, S3 esa 1 dan
sanaydi: aylantirish yo'qolsa birinchi bo'lak 0 raqami bilan ketardi va
S3 uni rad etardi.

Test to'g'ri qatlamga ko'chirildi (`IndexConversion`) va endi
mutatsiyani ushlaydi.

⚠️ Presigned URL **logga yozilmaydi** (§29).

### 4.3. Transcoding jadvali va holatlar `[x]`

- `[x]` Migratsiya `V28__video_transcoding.sql` (V27 upload uchun band edi)
- `[x]` `VideoProcessingStatus` enum · `TranscodingJob` entity · repo
- `[x]` `TranscodingJobService` — navbat, holat, yiqilish, qayta urinish
- `[x]` Yuklash tugagach video AVTOMATIK navbatga tushadi
- `[x]` `TranscodingJobTest` — 15 test, 6 mutatsiya bilan tasdiqlangan

#### ⚠️ Rejadagi uchta qaror TUZATILDI

**1. `processing_status` `MediaAsset` ga QO'SHILMADI.**

Reja shuni aytardi, lekin o'shanda holat **ikki joyda** yashardi (media
va ish) va birinchi nosozlikdayoq ajralardi: ish `FAILED` bo'lib, media
`TRANSCODING` da qolib ketardi. Bu aynan shu loyihada bir necha marta
uchragan naqsh.

Endi yagona manba — `cms_transcoding_job`. `MediaAsset` faqat
**natijani** saqlaydi: `hlsMasterKey` bor bo'lsa HLS tayyor.

**2. `original_object_key` QO'SHILMADI.**

U ortiqcha: `storage_key` ning **o'zi** original faylning kaliti.
Ikkinchi ustun bir xil qiymatni saqlab, ajralib ketish uchun yana bir
imkoniyat yaratardi.

**3. `NONE` holati YO'Q.**

Reja eski yozuvlarga `NONE` qo'yishni aytardi. Kerak emas: eski
medialarda ish **umuman yo'q**, ya'ni jadvalda qator ham yo'q. «Ish
yo'q» — bu allaqachon «transcoding qilinmagan» degani.

#### Boshqa qarorlar

**Har media uchun BITTA ish** (`unique(media_id)`). Qayta urinish
mavjud qatorni yangilaydi va `attempts` ni oshiradi. Har urinishga
alohida qator bo'lsa, kutubxona sahifasidagi 40 ta media uchun 40
marta «eng oxirgi qatorni top» kerak bo'lardi.

**`SKIP LOCKED`** navbatdan olishda: ikki instans bir vaqtda qarasa,
ikkalasi ham ayni ishni olardi va bitta video **ikki marta** transcoding
qilinardi.

**`claimNext()` — `REQUIRES_NEW`.** Ish band qilinishi transcoding
boshlanishidan oldin commit bo'lishi kerak. Bitta tranzaksiyada bo'lsa
qulf o'nlab daqiqa ushlab turilardi.

**Progress 100 ga faqat `READY` da yetadi.** «Progress 100, lekin hali
`TRANSCODING`» — admin uchun chalkash holat. Tayyorlikni faqat `status`
aytadi.

**Xato matni muvaffaqiyatdan keyin tozalanadi.** Aks holda admin
muvaffaqiyatli videoda eski xatoni ko'rib, uni yangi nosozlik deb
o'ylardi.

**Urinishlar chegarasi 3.** Cheksiz bo'lsa buzuq fayl navbatni abadiy
band qilardi va boshqa videolar hech qachon yetib bormasdi.

### 4.4. ffprobe `[x]`

- `[x]` `VideoMetadata` record — barcha maydonlar `null` bo'lishi mumkin
- `[x]` `FfprobeOutputParser` — tahlil mantig'i, jarayondan AJRATILGAN
- `[x]` `VideoProbeService` — `ProcessBuilder`, kutish muddati, aniq xatolar
- `[x]` `VideoProcessingException` — fon ishi uchun, `BusinessException` emas
- `[x]` `FfprobeParsingTest` — 13 test, 6 mutatsiya bilan tasdiqlangan

#### Nega tahlil jarayondan ajratildi

`ffprobe` ishlab chiqish mashinasida o'rnatilmagan va CI da ham
kafolatlanmagan. Haqiqiy mantiq esa aynan tahlilda: qaysi oqim video,
aylantirish qanday hisobga olinadi, `"30000/1001"` kabi kasr qanday
o'qiladi.

Ajratilgach tahlil `ffprobe`siz ham to'liq sinaladi — haqiqiy chiqish
namunalari bilan.

#### ⚠️ Ikkita jimgina xato oldi olindi

**1. Muqova rasmi «video oqim» bo'lib ko'rinadi.**

Albom muqovasi joylashtirilgan `.mp4` da **ikkita** video oqim bo'ladi:
haqiqiy video va `mjpeg` formatidagi bitta kadr. Muqova ko'pincha
ro'yxatda **birinchi** turadi.

Oddiygina birinchisini olsak, 600×600 muqova o'lchamlari videoning
o'lchami deb qabul qilinardi — va **1080p film 600×600 ga siqilardi**.
Parser `disposition.attached_pic = 1` ni o'tkazib yuboradi.

**2. Aylantirilgan video.**

Telefonda vertikal olingan video faylda **gorizontal** bo'lib yotadi va
90° belgisi bilan keladi. Belgi e'tiborga olinmasa profil tanlash uni
gorizontal deb hisoblardi va natija cho'zilgan chiqardi.

⚠️ `ffprobe` burchakni **ikki xil joyda** beradi: eski fayllarda
`tags.rotate`, yangilarida `side_data_list` ichidagi Display Matrix.
Faqat bittasiga qarash yarim holatlarni o'tkazib yuborardi — ikkalasi
ham o'qiladi.

#### Boshqa qarorlar

**`N/A` → `null`, `0` emas.** «Nol soniya» va «noma'lum» butunlay
boshqa narsa; ikkinchisi pleyer uchun muhim.

**Davomiylik: avval `format`, keyin oqim.** `.mkv` da umumiy
davomiylik ko'rsatilmasligi mumkin, lekin oqim darajasida bo'ladi.

**`0/0` chastotasi** — `ffprobe` ning odatiy javobi. Nolga bo'lish shu
yerda kutib olinadi.

**`ProcessBuilder` ro'yxat bilan, qobiq orqali EMAS.** Fayl nomidagi
bo'sh joy yoki `;` buyruqni bo'lib yuborardi.

**Kutish muddati 30s.** Chegarasiz bo'lsa buzuq fayl `ffprobe` ni
abadiy osib qo'yishi va u bilan birga butun worker to'xtashi mumkin
edi — navbat esa o'sib boraverardi.

⚠️ `ffprobe` o'rnatilmagan bo'lsa xabar buni **aniq aytadi**. Jimgina
bo'sh natija qaytarilmaydi: u «video 0×0» ma'nosini berardi va profil
tanlash uni tushunarsiz tarzda rad etardi.

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
- `[x]` Presigned multipart upload
- `[x]` Upload tugaganini tekshirish (`HEAD`)
- `[x]` `ffprobe` integratsiyasi
- `[ ]` Transcoding profillari
- `[ ]` HLS generatori
- `[ ]` `master.m3u8`
- `[ ]` S3 HLS yuklovchi
- `[x]` Processing holatlari
- `[ ]` Fon worker
- `[x]` Yiqilishni boshqarish va qayta urinish
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
