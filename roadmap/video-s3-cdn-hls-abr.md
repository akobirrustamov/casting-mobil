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

### 4.5. Profil tanlash `[x]`

- `[x]` `TranscodingProfile` · `VideoTranscodingProperties`
      (`app.video.transcoding.profiles[]`)
- `[x]` `VideoProfileSelector` — manbadan **yuqoriga chiqmaydi**
- `[x]` `VideoProfileSelectorTest` — 17 test, 6 mutatsiya bilan tasdiqlangan

```
2160p manba → 1080p · 720p · 480p
1080p manba → 1080p · 720p · 480p
 720p manba →  720p · 480p
 480p manba →  480p
 360p manba →  360p (o'z o'lchamida, cho'zilmaydi)
```

Standart zinapoya: 1080p/5000k · 720p/2800k · 480p/1200k. Sozlamada
tartib buzilgan bo'lsa u **o'zi tuzatiladi** — `master.m3u8` da
variantlar sifat bo'yicha tartiblangan bo'lishi kerak, aks holda ba'zi
pleyerlar birinchisini sukut deb oladi va u eng past sifat bo'lib
qolardi.

#### ⚠️ Vertikal video — «1080p», «1920p» EMAS

Loyihada vertikal kontent birinchi darajali (ТЗ §19 — Reels,
`Content.orientation`), ya'ni bu chekka holat emas.

1080×1920 lik videoni odamlar «1080p vertikal» deyishadi. Balandlik
bo'yicha taqqoslasak, 1920 ≥ 1080 bo'lib chiqardi va 1080p variant
yaratilardi — u esa **607×1080**, ya'ni manbadan **past** sifat.
Aslida manba allaqachon 1080p.

Shuning uchun taqqoslash **kichik tomon** bo'yicha boradi va
masshtablashda ham aynan u belgilanadi. Chiqish vertikal qoladi:
`1080x1920`, `720x1280`, `480x854`.

#### ⚠️ O'lchamlar JUFT bo'lishi shart

H.264 ning `yuv420p` formati toq o'lchamni qabul qilmaydi: xromatik
kanallar ikki barobar kichik va ular butun songa bo'linishi kerak.

1919×1079 lik manbani 720 ga keltirsak kenglik 1281.4 → 1281 chiqadi
va **FFmpeg xato berardi** — bu faqat transcoding paytida, allaqachon
yuklangan videoda bilinardi.

#### Eng past profildan kichik manba

360p video, zinapoyaning pastki pog'onasi esa 480p. Hech narsa
qaytarmaslik videoni HLS'siz qoldirardi va u pleyerda umuman
ochilmasdi.

Eng past profil olinadi, **lekin o'lchamlar manbaning o'zida qoladi** —
480p ga cho'zilmaydi.

### 4.6. FFmpeg → HLS `[x]`

- `[x]` `FfmpegCommandBuilder` — buyruq qurish, jarayondan AJRATILGAN
- `[x]` `HlsTranscodingService` — ishga tushirish, progress, xatolar
- `[x]` H.264 (`libx264`, `main` profil) + AAC, `yuv420p`
- `[x]` fMP4 segmentlar, 6 soniyalik
- `[x]` `master.m3u8` ni **FFmpeg ning o'zi** yozadi
- `[x]` Bitratelar sozlamadan, `maxrate`/`bufsize` bilan chegaralangan
- `[x]` Progress `-progress pipe:1` dan
- `[x]` `FfmpegCommandTest` — 17 test, 6 mutatsiya bilan tasdiqlangan

#### ⚠️ Eng muhim detal: segment chegaralari

ABR ishlashi uchun **barcha variantlarda segmentlar ayni vaqtlarda**
boshlanishi shart. Aks holda pleyer sifatni almashtirganda kadr
sakraydi yoki oqim umuman uziladi.

Bu nosozlik transcoding paytida **ko'rinmaydi**: fayllar yaratiladi,
playlist to'g'ri chiqadi, video ham ochiladi. U faqat qurilmada,
internet sekinlashib sifat almashgan paytda bilinadi.

Ikki bayroq buni ta'minlaydi:

- `-force_key_frames expr:gte(t,n_forced*6)` — kalit kadr har 6
  soniyada, **kadr chastotasidan qat'i nazar**. GOP ni qo'lda hisoblash
  (fps × 6) noto'g'ri fps da chegaralarni siljitardi, `ffprobe` esa fps
  ni har doim ham bermaydi
- `-sc_threshold 0` — aks holda FFmpeg sahna almashganda **o'zi** kalit
  kadr qo'yadi va u variantlarda turli joyda chiqadi

#### Bitta chaqiruv, uchta emas

`-var_stream_map` bilan barcha variantlar **bitta** FFmpeg chaqiruvida
yasaladi. Alohida chaqiruvlar bo'lsa manba **uch marta** dekodlanardi —
ikki soatlik film uchun protsessor vaqtining uch barobari.

`name:1080p` qismi `%v` nima bo'lishini belgilaydi. Usiz papkalar `0`,
`1`, `2` deb atalardi va S3 dagi kalitdan qaysi sifat ekanini bilib
bo'lmasdi.

#### `master.m3u8` ni FFmpeg yozadi

Qo'lda yozilsa `CODECS` atributini ham qo'lda hisoblash kerak bo'lardi
(`avc1.4d401f` kabi qatorlar profil va darajaga bog'liq). Noto'g'ri
qiymat pleyerni oqimni **umuman ochmaslikka** olib boradi — va bu faqat
qurilmada bilinadi.

#### Boshqa qarorlar

**`-hls_list_size 0`.** Sukut qiymat oxirgi bir nechta segmentni
qoldirib, qolganini **o'chiradi** — jonli efir uchun to'g'ri, VOD uchun
esa fayllarning yarmi yo'qolishini bildiradi.

**`maxrate` + `bufsize`.** Ularsiz FFmpeg bitrate'ni o'rtacha deb
qabul qiladi va murakkab sahnalarda uni bir necha barobar oshirib
yuborishi mumkin — sekin kanalda bu uzilish demakdir.

**`yuv420p` majburiy.** Manba 10-bitli bo'lsa FFmpeg uni saqlab
qolardi va natija ko'p qurilmada ochilmasdi.

**Ovozsiz video.** `0:a:0` ni ulashga urinish FFmpeg ni «Stream map
matches no streams» bilan yiqitardi. Ovoz yo'q bo'lsa u umuman
ulanmaydi.

**Xato oqimi alohida ipda o'qiladi.** Usiz FFmpeg osilib qolardi:
operatsion tizim bufer to'lganda yozishni to'xtatadi, biz esa stdout ni
o'qib turgan bo'lardik va hech kim stderr ni bo'shatmasdi.

**Progress faqat davomiylik ma'lum bo'lganda.** Aks holda bo'linuvchi
yo'q va har qanday raqam o'ylab topilgan bo'lardi.

#### ✅ HAQIQIY FFmpeg bilan tasdiqlandi (27.08.2026)

FFmpeg 9.0.1 o'rnatildi va `HlsPipelineIntegrationTest` yozildi —
4 test, haqiqiy kodlash bilan. FFmpeg bo'lmasa test **o'tkazib
yuboriladi** (`assumeTrue`), ya'ni CI qizil bo'lmaydi.

Buyurtmachining haqiqiy videosi bilan ham sinaldi: **2160×3840
vertikal 4K**, 59.94 fps, ovozli, 2:44, 591 MB.

**To'liq video → 3 variant: 76 soniya** (real vaqtdan 2.1× tez,
8 yadroda). 28 ta segment.

⚠️ Uchala variantning `#EXTINF` ro'yxati **bayt-baytiga bir xil** —
ABR chegaralari 28 nuqtada ham moslashdi. Har variant `master.m3u8`
orqali xatosiz dekodlandi va to'liq davomiylikni (164.564403s)
manbaga aynan mos qaytardi.

| | manba | 1080p | 720p | 480p |
|---|---|---|---|---|
| hajm | 591 MB | 97 MB | 55 MB | 24 MB |

| Tekshiruv | Natija |
|---|---|
| ABR segment chegaralari | `6.006000` va `4.037367` — **uchala variantda bir xil** |
| Vertikal o'lchamlar | `1080x1920` · `720x1280` · `480x854` — almashmadi |
| `CODECS` | `avc1.4d402a` (Main 4.2) — `-profile:v main` ga mos |
| `BANDWIDTH` | 5 104 422 ≈ sozlamadagi `5000k` |
| HLS versiyasi | 7 (fMP4 uchun majburiy) |
| Hajmlar | 40 MB manba → 5.8 / 3.3 / 1.5 MB |

#### ⚠️ Haqiqiy ishga tushirish topgan narsa

Mock testlar **hech qachon ko'ra olmaydigan** detal: ko'p variantda
FFmpeg init faylini **qayta nomlaydi**.

```
bitta variant    → init.mp4
uchta variant    → init_0.mp4 · init_1.mp4 · init_2.mp4
```

Ishlab chiqarish kodi **to'g'ri** edi — `HlsUploadService` papkani
rekursiv yuklaydi va playlistlar to'g'ri havola qiladi. Xato
**testda** edi: u har doim `init.mp4` deb hisoblagan.

Test kuchliroq tekshiruvga almashtirildi: **playlist havola qilgan
fayl haqiqatan bormi**. Bu nom o'zgarsa ham to'g'ri qoladi va
playlist bilan fayllar mos kelishini kafolatlaydi.

### 4.7. Worker `[x]` · 4.8 bilan BIRGA

- `[x]` `TranscodeWorker` — `@Scheduled(fixedDelay)`, sozlanadigan oraliq
- `[x]` `app.video.max-concurrent-jobs` (default **1**), semafor bilan
- `[x]` `try/finally` bilan kafolatli tozalash (§16)
- `[x]` Uzilib qolgan ishlarni ishga tushishda navbatga qaytarish
- `[x]` `HlsUploadService` — rekursiv yuklash (4.8)
- `[x]` `StorageService.storeAt` — aniq kalitga saqlash
- `[x]` `TranscodeWorkerTest` — 12 test, 5 mutatsiya bilan tasdiqlangan

#### ⚠️ 4.8 nega birga bajarildi

Yuklamaydigan worker ni **sinab bo'lmaydi**: u `READY` ni yuklashsiz
qo'yardi va bu jimgina noto'g'ri holat bo'lardi. Zanjir to'liq bo'lishi
kerak edi.

#### Zanjir

```
navbatdan ol → ombordan yuklab ol → ffprobe → profil tanla
→ ffmpeg → HLS'ni omborga yukla → READY
                                   ↓ har qanday xatoda
                                 FAILED / navbatga qaytish
va HAR QANDAY holatda → vaqtinchalik fayllarni o'chir
```

#### Qabul qilingan qarorlar

**Manba lokal diskka tushiriladi.** FFmpeg faylga **tasodifiy joydan**
murojaat qiladi (indeks odatda faylning oxirida). Uni to'g'ridan-to'g'ri
S3 dan o'qish har seek uchun yangi HTTP so'rov degani — transcoding bir
necha barobar sekinlashardi.

**`hlsMasterKey` FAQAT yuklash tugagach yoziladi.** U «video tayyor»
belgisi. Ilgariroq yozilsa pleyer mavjud bo'lmagan fayllarni so'rardi.

**`master.m3u8` eng oxirida yuklanadi.** Uning paydo bo'lishi «tayyor»
degani. Birinchi yuklansa, segmentlar hali kelmagan paytda pleyer uni
o'qib, uzilib qoladigan videoni ko'rsatardi.

**Ish alohida ipda.** Rejalashtiruvchi ipida bajarilsa u o'nlab daqiqa
band bo'lardi va boshqa barcha vazifalar (`NotificationDispatcher`,
`AnalyticsService`) to'xtab qolardi.

**Bo'sh joy bo'lmasa navbatga umuman qaralmaydi.** Ish olinib, keyin
bajarilmay qolsa u `PROBING` da **muzlab** qolardi.

**Uzilib qolgan ishlar ishga tushishda navbatga qaytariladi.** Server
transcoding paytida qayta ishga tushsa, ish `TRANSCODING` holatida
muzlab qolardi: uni hech kim olmaydi va hech kim tugatmaydi. Admin
panelda u abadiy «bajarilmoqda» bo'lib turardi — jimgina nosozlikning
eng yomon turi.

⚠️ `attempts` bunda **kamaytirilmaydi**: qayta ishga tushish sababi
transcoding'ning o'zi bo'lishi mumkin (xotira tugashi).

**Kengaytma oq ro'yxati `storeAt` da qo'llanmaydi.** HLS fayllarini
(`.m3u8`, `.m4s`) **server yaratadi**, foydalanuvchi emas. Oq ro'yxat
foydalanuvchi bergan nomdan himoya qiladi; yo'l himoyasi esa qoladi.

**Worker testlarda o'chirilgan** (`app.video.worker-enabled=false`).
Yoqiq qolsa har test konteksti navbatdagi ishlarni haqiqiy FFmpeg bilan
bajarishga urinardi.

### 4.8. S3 ga HLS yuklash `[x]` — 4.7 bilan birga

- `[x]` Rekursiv yuklash, har bir fayl uchun to'g'ri `Content-Type`:

| Kengaytma | Content-Type |
|---|---|
| `.m3u8` | `application/vnd.apple.mpegurl` |
| `.m4s` | `video/iso.segment` |
| `.mp4` (init) | `video/mp4` |

- `[x]` `status = READY` **faqat hammasi yuklangach** (§20)
- `[x]` Qisman yuklash → `FAILED` + tozalash

### 4.9. CDN va yetkazish `[x]`

- `[x]` `CdnUrlService` — kalitni mutlaq manzilga aylantiradi
- `[x]` `app.video.cdn.base-url` sozlamasi (bo'sh bo'lsa HLS berilmaydi)
- `[x]` Bazada **obyekt kaliti** saqlanadi, to'liq URL emas (§22)
- `[x]` `WatchController` → **yangi** `hlsUrl` maydoni, `url` TEGILMADI
- `[x]` `application.properties.example` — 16 ta `app.video.*` sozlamasi
- `[x]` `CdnUrlTest` (9) · `HlsDeliveryTest` (4), 4 mutatsiya bilan tasdiqlangan

#### ⚠️ Mobil shartnomasi — eng nozik joy

Mobil `url` ni **nisbiy** deb hisoblaydi va oldiga o'z `BASE_URL` ini
qo'yadi (`WatchDetail.tsx`):

```ts
useVideoPlayer({ uri: `${BASE_URL}${source.url}` })
```

CDN manzili o'sha maydonga yozilsa `https://uzcasting.sitehttps://cdn…`
chiqardi — **jimgina buzilish**, hech qanday xato ko'rsatmasdan.

Yechim: `url` **o'zgarmadi**, CDN manzili **yangi** `hlsUrl` maydoniga
yoziladi.

| Holat | `url` | `hlsUrl` |
|---|---|---|
| Transcoding tugagan | nisbiy, ishlaydi | mutlaq CDN manzili |
| Transcoding tugamagan | nisbiy, ishlaydi | `null` |
| CDN sozlanmagan | nisbiy, ishlaydi | `null` |
| Ruxsat yo'q | manbalar **umuman yo'q** | — |

Eski mobil ilova `hlsUrl` ni bilmaydi va `url` orqali ishlashda davom
etadi. Yangisi `hlsUrl` bo'lsa uni oladi, bo'lmasa `url` ga qaytadi.
Ikkalasi bitta relizda yonma-yon ishlaydi (§33).

⚠️ Bu shartnoma **test bilan qulflangan**: `CdnUrlTest.MobileContract`
`WatchController` manbasini o'qib, `url` ga CDN berilmaganini tekshiradi.
Mutatsiya sinovida bu buzilish **3 ta testni** yiqitdi.

#### Nega `null`, o'ylab topilgan manzil emas

CDN sozlanmagan yoki transcoding tugamagan bo'lsa `null` qaytariladi.

O'ylab topilgan manzil pleyerni **mavjud bo'lmagan** faylga yuborardi va
nosozlik «video buzuq» bo'lib ko'rinardi — sabab esa oddiy sozlama
yetishmasligi edi.

#### Nega baza kalitni saqlaydi

To'liq URL saqlansa, CDN domeni o'zgarganda **ming qatorli `UPDATE`**
kerak bo'lardi va u paytgacha barcha eski videolar ochilmay qolardi.
Domen sozlamada bo'lsa — bitta qatorni tahrirlash.

### 4.10. Pullik kontent himoyasi `[x]`

Hozir `/api/v1/app/media/{id}/raw` har so'rovda `AccessService.canReadMedia()`
ni chaqiradi. CDN'ga o'tilganda **bu tekshiruv yo'qoladi**: segmentlar Spring
Boot'dan o'tmaydi.

Uchta variant ko'rib chiqilgan edi:

| Variant | Qanday | Xavf |
|---|---|---|
| A. Ochiq CDN | Segmentlar hammaga ochiq | ❌ Pullik kontent bepul tarqaladi. **Qabul qilib bo'lmaydi** |
| B. CDN Secure Token | Timeweb tokenli havola qo'llab-quvvatlasa | ⚠️ Timeweb hujjatidan tasdiqlash kerak |
| C. Playlist proksi + presigned | Playlist bizdan, segmentlar imzolangan havoladan | ✅ **Tanlandi** — hech qanday tashqi tasdiq kutmaydi |

**Nima uchun C.** B varianti Timeweb javobiga bog'liq edi va u kelmadi.
C esa har qanday S3-mos ombor bilan ishlaydi. Imzolash `SignedUrlProvider`
interfeysi ortida — Timeweb tokeni tasdiqlansa, uni almashtirish playlist
mantig'iga umuman tegmaydi.

#### Qanday ishlaydi

```
pleyer → BIZ:    master.m3u8        huquq tekshiriladi
pleyer → BIZ:    720p/index.m3u8    huquq QAYTA tekshiriladi
pleyer → OMBOR:  segment_00001.m4s  imzolangan havola
```

⚠️ **Video baribir serverimizdan O'TMAYDI.** Playlist — bir necha kilobayt
matn; gigabaytlar to'g'ridan-to'g'ri ombordan keladi. Asosiy talab
buzilmadi.

#### ⚠️ Chipta HUQUQ bermaydi

Havola ichidagi chipta faqat «bu so'rov kimniki» degan savolga javob beradi.
Ko'rish huquqi **har so'rovda** `AccessService` dan qayta so'raladi.

Bu ataylab: obuna tugasa yoki xarid qaytarilsa, kirish **o'sha zahoti**
yopiladi. Chipta ichiga «ruxsat berilgan» deb yozilganda esa u muddati
tugagunicha ishlayverardi — pulini qaytarib olgan odam filmni ko'rishda
davom etardi.

#### ⚠️ Nega chipta sarlavhada emas, MANZIL ichida

Bu eng nozik joy va noto'g'ri qaror video umuman ochilmasligiga olib
kelardi.

AVPlayer (iOS) va ExoPlayer (Android) sarlavhalarni **butun oqim uchun**
bir marta oladi — ular segment so'roviga ham qo'shiladi. Segment esa
imzolangan havola bilan to'g'ridan-to'g'ri S3 ga boradi, S3 esa ikkita
avtorizatsiyani birga qabul qilmaydi: so'rovda ham `Authorization`, ham
`X-Amz-Signature` bo'lsa u **400** qaytaradi.

Ya'ni `Authorization` yuborilsa **hech bir segment ochilmasdi**.

#### ⚠️ Imzo keshi — CDN uchun hal qiluvchi

MinIO bilan o'lchandi: S3 imzosi `X-Amz-Date` ni o'z ichiga oladi va u
imzolash **vaqti**. Uch soniya farq bilan yasalgan ikkita havola boshqa
satr beradi.

Ya'ni har foydalanuvchi o'z havolasini olardi. CDN uchun bu boshqa manzil
degani: **3000 tomoshabin → 3000 kesh yozuvi**, kesh umuman ishlamaydi va
butun trafik omborga tushadi.

Yechim — vaqt oynasi bo'yicha kesh: havola bir marta yasaladi va oyna
tugagunicha hammaga bir xil qaytariladi.

#### Bajarilgani

- `[x]` `SignedUrlProvider` — imzolash interfeysi (Timeweb tokeni uchun
      almashtiriladigan joy)
- `[x]` `PresignedUrlProvider` — S3 imzolash + vaqt oynasi keshi
- `[x]` `PlaybackTicketService` — chipta (kimligi, huquq emas)
- `[x]` `HlsPlaylistService` — playlist yo'llarini qayta yozish,
      `#EXT-X-MAP` bilan birga
- `[x]` `HlsController` — `GET /api/v1/app/media/{id}/hls/{*path}`
- `[x]` `PlaybackUrlService` — S3 bo'lsa proksi, bo'lmasa eski CDN yo'li
- `[x]` `WatchController` — `hlsUrl` shu servisdan
- `[x]` Mobil: nisbiy `hlsUrl` ga `BASE_URL` qo'shiladi, sarlavha
      HLS yo'liga **yuborilmaydi**
- `[x]` Sozlamalar hujjatlandi (`ticket-ttl`, `signed-url-ttl`,
      `signed-url-window`)

#### Qat'iy chegaralar

- `[x]` Endpoint **faqat `.m3u8`** beradi. Segmentni ham bersa,
      gigabaytlar Spring Boot orqali oqib, butun ishning ma'nosi
      qolmasdi
- `[x]` Chipta **aynan bitta media** uchun. Aks holda bepul klipning
      chiptasi bilan pullik filmni ochish mumkin bo'lardi
- `[x]` Kirish tokeni chipta o'rniga **ishlamaydi**. Ikkalasi bir xil
      kalit bilan imzolangan, farqni faqat tur belgisi qiladi
- `[x]` `..` rad etiladi. Hozir uni Spring'ning `StrictHttpFirewall`
      bizga yetib kelishidan oldin to'xtatadi — kontrollerdagi tekshiruv
      ikkinchi qavat bo'lib qoladi

#### Testlar

- `[x]` `HlsPlaylistRewriteTest` (7) — qayta yozish mantig'i
- `[x]` `HlsProtectionTest` (21) — chipta, huquq, yo'l chegaralari
- `[x]` `S3IntegrationTest$SignedUrls` (4) — **haqiqiy MinIO**: havola
      ochiladimi, oyna ichida o'zgarmaydimi
- `[x]` `S3IntegrationTest$EndToEnd` (1) — **uchma-uch**: playlistdagi
      segment havolasi haqiqatdan fayl qaytaradi
- `[x]` `CdnUrlTest$MobileContract` — mobil shartnomasi yangilandi

Har bir himoya **mutatsiya bilan tekshirildi**: shart olib tashlanganda
test yiqilishi tasdiqlandi. Ikkita test shu jarayonda kuchaytirildi —
ular bekorga yashil edi:

- `ticketIsBoundToMedia` pullik media ishlatardi, ya'ni so'rovni
  `AccessService` to'xtatardi va bog'lanish tekshiruvi umuman
  sinalmasdi;
- `segmentsNotProxied` mavjud bo'lmagan faylni so'rardi va 404 «fayl
  yo'q» degani uchun kelardi, «segment berilmaydi» degani uchun emas.

### 4.11. Admin panel `[x]`

#### Hozirgi holat — koddan aniqlangan

Media kutubxonasi (`MediaPage`) ishlaydi va ko'rsatadi: tur, holat
(`READY`/`ARCHIVED`), o'lchamlar, davomiylik, hajm, `playable`
bayrog'i, ishlatilish joylari.

⚠️ **Transcoding haqida hech narsa bilmaydi.** `MediaDto` da
`transcodingStatus` ham, `progress` ham, `error` ham yo'q. Admin
video yuklaydi va keyin **hech narsa ko'rmaydi**: u tayyormi,
navbatdami, yiqildimi — bilib bo'lmaydi.

Bu jimgina nosozlikning eng yomon turi: video kutubxonada
`READY` bo'lib turadi (bu `MediaStatus`, ya'ni «arxivlanmagan»),
lekin HLS'i yo'q va pleyerda ochilmaydi.

#### Bosqich A — backend tomoni (avval shu)

- `[x]` `MediaDto` ga `transcoding` obyekti:

```jsonc
"transcoding": {
  "status": "TRANSCODING",   // QUEUED · PROBING · TRANSCODING · UPLOADING · READY · FAILED
  "progress": 62,
  "error": null,
  "attempts": 1,
  "startedAt": "…", "finishedAt": null
}
```

  ⚠️ Video BO'LMAGAN media uchun `null` — «ish yo'q» va «ish
  yiqilgan» boshqa narsa. `playable` da qilinganidek.

- `[x]` `MediaController.list()` — ishlarni **bitta so'rovda** olish
      (`TranscodingJobService.forMediaIds`). Har media uchun alohida
      so'rov 40 elementli sahifada **N+1** bo'lardi
- `[x]` `POST /api/v1/app/admin/media/{id}/retry-transcoding`
      → `MEDIA_UPLOAD` ruxsati
- `[x]` ⚠️ Yangi ruxsat **qo'shilmaydi**. `MEDIA_UPLOAD` ni kim
      olgan bo'lsa, u video yuklaydi — qayta urinish ham o'sha ishning
      davomi. Yangi ruxsat mavjud rollarni qayta sozlashni talab
      qilardi
- `[x]` `GET /api/v1/app/admin/media/queue` — navbat holati
      (nechta `QUEUED`, nechta ishlamoqda, nechta `FAILED`)
- `[x]` Testlar: DTO shakli, N+1 yo'qligi, ruxsat, qayta urinish

#### Bosqich B — kutubxona ro'yxati

- `[x]` `MediaPage` kartochkasida transcoding nishoni:

```
🎞 kino.mp4          [VIDEO]  [⏳ Navbatda]
🎞 film.mp4          [VIDEO]  [▶ 62%]
🎞 klip.mp4          [VIDEO]  [✓ HLS tayyor]
🎞 buzuq.mp4         [VIDEO]  [⚠ Yiqildi]
```

- `[x]` Faqat VIDEO uchun. Rasm va hujjatda nishon **umuman
      chizilmaydi** — `playable` dagi `=== false` qoidasi bilan bir xil
- `[x]` Filtrga `transcoding=FAILED` qo'shish — admin yiqilganlarni
      topa olishi kerak
- `[x]` ⚠️ Mavjud `Badge` va `uz-*` komponentlari. Yangi dizayn
      tizimi yaratilmaydi (§25)

#### Bosqich C — tafsilot oynasi

- `[x]` `MediaPage` tafsilot oynasida transcoding bo'limi:
      holat · progress · urinishlar soni · boshlangan/tugagan vaqt
- `[x]` `FAILED` uchun **xato matni** ko'rsatiladi

  ⚠️ Faqat «yiqildi» deyish adminni logga qarashga majbur qilardi,
  logga esa uning kirishi yo'q. Sabab bazada saqlanadi (`error`
  ustuni) — uni ko'rsatmaslik ma'noni yo'qotardi

- `[x]` **Qayta urinish** tugmasi — faqat `FAILED` holatida va faqat
      `MEDIA_UPLOAD` ruxsati bo'lsa
- `[x]` Tugma bosilgach ro'yxat yangilanadi

#### Bosqich D — progressni kuzatish

- `[x]` `PROCESSING` holatidagi media bo'lsa ro'yxat **davriy
      yangilanadi** (10–15 soniyada)
- `[x]` ⚠️ So'rov faqat KERAK bo'lganda: barcha ishlar tugagan bo'lsa
      yangilash **to'xtaydi**. Doimiy so'rov ochiq turgan panel
      serverga bekorga yuk berardi
- `[x]` Sahifa fokusda bo'lmaganda ham to'xtaydi
      (`document.visibilityState`)

#### ✅ A · B · C · D bajarildi (27.08.2026)

| Nima | Qayerda |
|---|---|
| `transcoding` obyekti DTO'da | `MediaController.TranscodingDto` |
| Qayta urinish endpointi | `POST /media/{id}/retry-transcoding` → `MEDIA_UPLOAD` |
| Navbat holati | `GET /media/transcoding-queue` |
| `FAILED` filtri | `?transcoding=FAILED` |
| Nishon | `components/TranscodingBadge.jsx` |
| Tafsilot bo'limi | `MediaPage.TranscodingPanel` |
| Davriy yangilash | `MediaPage.useTranscodingPolling` |

Testlar: `MediaTranscodingApiTest` (10) · `transcodingBadge.test.jsx` (12).
Mutatsiya sinovi: 4 backend + 4 frontend.

#### ⚠️ N+1 testi ikki marta yozildi

Birinchi variant «`cms_transcoding_job` ga **bitta** so'rov» deb
tekshirardi. U alohida o'tardi, lekin to'liq to'plamda **yiqildi**.

Sabab: `FAILED` filtri uchun `library` so'roviga `exists` kichik
so'rovi qo'shilgan va u ham `cms_transcoding_job` ni eslatadi. Ya'ni
so'rovlar ikkita — biri ro'yxat, biri ishlar to'plami.

Qat'iy son tekshiruvi masalaning O'ZI haqida hech narsa aytmasdi.
Ikkinchi variant aynan niyatni o'lchaydi: **media soni 4 → 12 ga
o'ssa, so'rovlar soni o'zgarmasligi kerak**.

Mutatsiya bilan tasdiqlandi: N+1 kiritilganda so'rovlar `14 → 22`
bo'lib o'sdi va test yiqildi.

#### ⚠️ Mutatsiya sinovi topgan o'lik shox

`TranscodingDto.from` da `asset.getType() != VIDEO` tekshiruvi bor edi.
Mutatsiya uni olib tashlaganda **hech bir test yiqilmadi**.

Sabab: ish faqat VIDEO uchun yaratiladi (`enqueue` buni kafolatlaydi),
ya'ni ish bor bo'lsa media albatta video. Takroriy tekshiruv **hech
qachon ishlamaydigan shox** edi — uni sinab bo'lmaydi va shuning uchun
to'g'ri ekaniga ishonch ham yo'q. Olib tashlandi.

#### Qabul qilingan qarorlar

**`exists`, `join` emas** — `FAILED` filtrida. `join` bilan ishi
**yo'q** media umuman chiqmay qolardi, ya'ni filtrsiz ham eski fayllar
ro'yxatdan yo'qolardi.

**Qayta urinish oynani YOPMAYDI.** Mavjud `onChanged` oynani yopadi —
u o'chirish va arxivlash uchun to'g'ri. Qayta urinishda esa admin
aynan holat o'zgarishini kutyapti; oyna yopilsa u natijani ko'rmasdan
qolardi. Alohida `onRefresh` qo'shildi.

**`retryable` ni SERVER hisoblaydi.** Klient «tugagan ish» qoidasini
o'zi hisoblasa, u ikki joyda yashardi va ajralib ketardi — panel
tugmani ko'rsatardi, server esa 422 qaytarardi.

**Davriy yangilash uch shart bilan:** tugamagan ish bor · vkladka
ko'rinib turibdi · 12 soniya o'tdi. Usiz ochiq qolgan panel serverga
soatlab bekorga so'rov yuborardi.

#### Bosqich E — yuklash oqimi

- `[x]` `MediaPicker` da yuklash tugagach **darhol** «Navbatda»
      ko'rsatiladi

  ⚠️ Hozir yuklash tugagach oyna yopiladi va admin videoni tanlaydi.
  U HLS tayyor deb o'ylaydi, aslida esa transcoding endi boshlanadi.
  Bu «yuklandi = tayyor» degan noto'g'ri taassurot

- `[x]` Epizod muharririda biriktirilgan video hali tayyor bo'lmasa
      ogohlantirish — `notPlayable` naqshi bo'yicha
- `[x]` Uchala tilga i18n kalitlari (`uz` · `ru` · `en`)

#### ✅ Bosqich E bajarildi (27.08.2026)

**Yuklash javobiga `transcoding` qo'shildi** (`UploadController.complete`).
Usiz panel yuklash tugagach «tayyor» deb ko'rsatardi, aslida esa qayta
ishlash **endi boshlanadi**. Admin videoni darhol epizodga biriktirib,
uni ishlaydi deb o'ylardi — foydalanuvchi esa ochilmaydigan video
ko'rardi.

**`MediaPicker` kartochkasida nishon** — yangi yuklangan video darhol
«Navbatda» bo'lib ko'rinadi.

**`MediaField` da ikkita AYRIM ogohlantirish:**

| Holat | Ma'nosi | Admin nima qiladi |
|---|---|---|
| `notPlayable` | format noto'g'ri (`.mkv`) | **boshqa fayl** kerak |
| `pending` | format to'g'ri, HLS tayyor emas | **kutadi** |
| `failed` | qayta ishlash yiqilgan | kutubxonada **qayta urinadi** |

⚠️ Ularni bitta ogohlantirishga qo'shish adminni chalkashtirardi:
birinchisida boshqa fayl kerak, ikkinchisida shunchaki kutish.

Format noto'g'ri bo'lsa **faqat format ogohlantirishi** chiqadi —
qayta ishlash u yerda yordam bermaydi va ikkita ogohlantirish birdan
chiqsa admin qaysi biriga ishonishni bilmasdi.

Yiqilish sababi **ko'rsatiladi** — usiz admin logga qarashga majbur
bo'lardi, logga esa uning kirishi yo'q.

Testlar: `mediaPlayable.test.jsx` (11, +5). Mutatsiya sinovi: 4 ta.

#### ⚠️ Ochiq savol: ikkita «holat» chalkashligi

Panelda endi **ikkita** turli holat bo'ladi:

| Maydon | Ma'nosi | Qiymatlari |
|---|---|---|
| `status` | kutubxonada ko'rinadimi | `READY` · `ARCHIVED` |
| `transcoding.status` | HLS tayyormi | `QUEUED` … `FAILED` |

Ikkalasi ham «READY» so'zini ishlatadi va bu **adminni chalkashtiradi**.

✅ **Hal qilindi 27.08.2026** — buyurtmachi tasdiqladi.

Panelda transcoding «Video qayta ishlash» deb ataladi va holatlari
`Navbatda · Tekshirilmoqda · Qayta ishlanmoqda · Yuklanmoqda · Video
tayyor · Yiqildi`. Ya'ni «READY» so'zi faqat bitta joyda qoladi.

**Backend nomlari o'zgarmadi** — ular API shartnomasi. Faqat tarjima
(`tc.*` kalitlari, uchala tilda 17 tadan).

### 4.12. Mobil `[x]`

- `[x]` `VideoSource` turiga `hlsUrl` qo'shildi
- `[x]` `mapSource` uni o'qiydi — eski backend bermasa `null`
- `[x]` `playbackSource()` — sof funksiya, qaror shu yerda
- `[x]` Pleyer: `hlsUrl` bo'lsa CDN, bo'lmasa `BASE_URL + url`
- `[x]` ⚠️ Token faqat ESKI yo'lga yuboriladi
- `[x]` Pleyer `key` iga yo'l turi qo'shildi
- `[x]` `expo-video` ABR'ni o'zi boshqaradi — qo'lda tezlik hisoblash
      yozilmadi (§26)
- `[x]` Qo'riqchi testlar: `CdnUrlTest.MobileContract` (+2), 3 mutatsiya

#### ⚠️ Token CDN'ga YUBORILMAYDI

Eski yo'lda `Authorization` **majburiy** — server ruxsatni tekshiradi.
CDN'da esa u keraksiz va zararli:

- bitta epizodda **yuzlab segment** bor va har biriga ortiqcha sarlavha
  keshlashga xalaqit beradi;
- ba'zi CDN'lar kutilmagan avtorizatsiyali so'rovni umuman rad etadi.

Qaror `playbackSource()` sof funksiyasida — u ATAYLAB ajratilgan.
Komponent ichida bu qarorni na o'qib, na tekshirib bo'lardi.

#### ⚠️ Pleyer `key` idagi nozik xato

`key={source.mediaId ?? part}` edi. Media `raw` dan HLS'ga o'tsa (ya'ni
transcoding tugasa), kalit **o'zgarmasdi** va pleyer eski manzilda
qolardi.

Ssenariy: foydalanuvchi epizodni qayta ishlash paytida ochadi (server
orqali ijro), keyin ekranni pastga tortadi — `/watch` endi `hlsUrl`
qaytaradi, lekin video baribir **CDN'dan chetlab** oqishda davom
etardi.

Kalitga yo'l turi qo'shildi: `${mediaId}-${hls|raw}`.

#### Nega mobil testlari yozilmadi

Mobil loyihada test freymvorki umuman **yo'q** (`jest` ham, test
skripti ham). Uni qo'shish funksiyaning o'zidan kattaroq iz bo'lardi
va buyurtmachi mobil ilovaga minimal tegishni so'ragan.

O'rniga ikkita narsa qilindi:

1. **`tsc --noEmit`** — bog'liqliklar o'rnatildi, turlar toza;
2. **Backend qo'riqchi testi** mobil manbasini o'qiydi va shartnomani
   tekshiradi: ikkala yo'l ham bor, token faqat eskisiga ketadi.

Mutatsiya sinovi buni tasdiqladi — mobil kodini buzganda **backend
testi yiqiladi**.

### 4.13. Infratuzilma `[~]` — kod tayyor, server kutilmoqda

#### Server — TANLANGAN (27.08.2026)

```
12 yadro · 16 GB RAM · 200 GB NVMe  +  Timeweb Object Storage
```

Buyurtmachi qarori. Quyidagi o'lchovlar asosida tasdiqlangan.

#### ⚠️ O'lchov — taxmin EMAS

Ikkita haqiqiy transcoding o'lchandi (Apple M4, 10 yadro, buyurtmachining
haqiqiy 2160×3840 vertikal videosi):

| Manba | 30 soniya → | Tezlik |
|---|---|---|
| 4K 60fps | 13.3s | real vaqtdan 2.3× tez |
| 1080p 30fps | 5.5s | real vaqtdan 5.4× tez |

⚠️ **1080p30 manba 4K60 dan 2.4 barobar arzon.** Bu server tanlashda
protsessordan ham muhimroq omil.

Server yadrosi M4 dan sekinroq (x264 da ~1.7×) — quyidagi hisoblarda
shu zaxira olingan.

#### Kunlik yuk — 30 ta video

Eng og'ir holat: 4K60 manba, 30 daqiqalik video.

| Yadro | Kuniga | Bandlik | Bitta video |
|---|---|---|---|
| 8 | 12.4 soat | 52% | ~25 daq |
| **12** ⭐ | **8.3 soat** | **35%** | ~17 daq |
| 16 | 6.2 soat | 26% | ~12 daq |

1080p30 manbada 12 yadroda bandlik **14%** ga tushadi.

**Nega 12:** 8 yadroda 52% bandlik ishlaydi, lekin 45 daqiqalik videoda
18.7 soatga chiqadi va sutka deyarli to'ladi. 16 esa hozircha ortiqcha.

#### ⚠️ RAM — eng zaif bo'g'in

12 yadro `max-concurrent-jobs=3` degani:

| Nima | RAM |
|---|---|
| Spring Boot | 1–2 GB |
| PostgreSQL | 2–4 GB |
| 3 ta parallel FFmpeg (4K) | 3–4.5 GB |
| OS + disk keshi | 2–4 GB |
| **Jami** | **8–14 GB** |

16 GB da sig'adi, lekin PostgreSQL keshiga joy kam qoladi — bu baza
so'rovlarini sekinlashtiradi, transcoding'ni emas.

24 GB muvozanatliroq bo'lardi. Buyurtmachi 16 GB ni tanladi — bu
ishlaydigan qaror, faqat zaxira kam.

#### ⚠️ 200 GB — S3 MAJBURIY qiladi

| Holat | 200 GB |
|---|---|
| S3 ishlatilsa | juda yetarli — ~2.4 GB vaqtinchalik + ~40 GB tizim |
| S3 ishlatilmasa | kuniga 24 GB → **8 kunda to'ladi** |

Ya'ni server bilan birga **Timeweb Object Storage ham sotib olinishi
shart**. Kod tayyor (4.1), lekin `app.storage.provider=local` bo'lib
turibdi.

#### CDN — asosiy xarajat

Server oyiga ~$40–80. CDN esa foydalanuvchilarga bog'liq:

| 3000 foydalanuvchi | Oyiga |
|---|---|
| kuniga 20 daqiqa, 720p | ~39 TB |
| kuniga 1 soat, 720p | ~117 TB |

⚠️ Timeweb CDN narxi tekshirilsin — u server narxidan bir necha barobar
oshib ketishi mumkin.

#### Server tayyorligi — kod tomoni `[x]` (28.08.2026)

##### ⚠️ Muammo: jimgina, ommaviy yiqilish

FFmpeg o'rnatilmagan serverda shunday bo'lardi: admin video yuklaydi,
ish navbatga tushadi, uch marta urinib yiqiladi va `FAILED` bo'ladi.
Keyingi video ham. Va keyingisi ham.

Har bir ishning xato matni to'g'ri («ffprobe ishga tushmadi»), lekin
hech kim «ular BIRGA yiqilyapti, ya'ni muammo videolarda emas,
SERVERDA» degan xulosaga kelmasdi. Admin buzuq fayl izlab yurardi.

##### Yechim: `VideoSystemHealth`

- `[x]` FFmpeg va ffprobe **ishga tushadimi** — fayl borligini
      tekshirish yetarli emas: yo'l papkaga ishora qilishi, ijro
      huquqi bo'lmasligi yoki boshqa arxitektura uchun yig'ilgan
      bo'lishi mumkin
- `[x]` ⚠️ **Kerakli kodlovchilar bormi** (`libx264`, `aac`).

  Ba'zi yig'malar ularsiz keladi. Bunday FFmpeg `-version` ga chiroyli
  javob beradi, lekin transcoding «Unknown encoder» bilan yiqiladi —
  va bu faqat birinchi video yuklangach ma'lum bo'lardi

- `[x]` Diskda bo'sh joy
- `[x]` Ishga tushishda logga aniq ogohlantirish
- `[x]` ⚠️ **Ilova baribir ko'tariladi.** Transcoding — qo'shimcha
      imkoniyat; uning yo'qligi uchun ishlaydigan saytni yiqitish
      nomutanosib bo'lardi. Videolar eski `/raw` yo'li bilan
      ko'rsatilishda davom etadi

##### Panelda ko'rinadi

- `[x]` Holat navbat bilan **birga** qaytariladi
      (`GET /media/transcoding-queue`).

  ⚠️ Alohida endpoint bo'lsa panel uni so'rashni unutardi

- `[x]` `TranscodingQueue` banneri — `MediaPage` da.

  ⚠️ Navbat endpointi allaqachon bor edi va `client.js` da chaqiruv
  ham yozilgan edi, lekin **hech bir sahifa uni ishlatmasdi**

- `[x]` Hammasi joyida bo'lsa **hech narsa chizilmaydi**. Doimiy
      «0 · 0 · 0» qatori shovqin bo'lardi va odam unga qaramay
      qo'yardi — aynan shunda u kerak bo'lganda ham ko'rinmasdi
- `[x]` Xabar **nima qilish kerakligini** aytadi. Admin serverga kira
      olmaydi, ya'ni «FFmpeg yo'q» undan hech qanday amal talab
      qilmaydi — u kimga murojaat qilishni bilishi kerak

#### Disk monitoringi `[x]` (28.08.2026)

Disk to'lganda `Files.copy` yarim yo'lda uziladi, ish yiqiladi va qayta
urinadi — yana yarim fayl yozib. Uchala urinish ham diskni yanada
to'ldiradi.

Undan ham yomoni: disk to'lgach **PostgreSQL ham yozolmay qoladi** va
nosozlik video bilan umuman bog'liq bo'lmagan joyda ko'rinadi.

- `[x]` `app.video.min-free-disk` (sukut `10GB`) — joy kam bo'lsa ish
      navbatdan **umuman olinmaydi**.

  ⚠️ Olinsa urinishlar sarflanardi va uch marta yiqilgach video
  `FAILED` bo'lib qolardi — sabab esa videoda emas, serverda edi.
  Tegilmagan ish esa joy bo'shashi bilan o'z-o'zidan bajariladi

- `[x]` Har bir ish uchun alohida tekshiruv: manba hajmi × 2.5.

  ⚠️ Umumiy chegara va «aynan shu fayl sig'adimi» — boshqa savollar.
  40 GB bo'sh joy odatiy video uchun ko'p, 30 GB lik 4K manba uchun
  esa yetmaydi

- `[x]` Log ogohlantirishi **10 daqiqada bir marta** — navbat har 15
      soniyada tekshiriladi va har safar yozilsa log bir kechada bir
      xil qator bilan to'lardi

#### Sozlamalar `[x]`

- `[x]` `app.video.temp-dir` (`app.upload.temp-dir` naqshi bo'yicha)
- `[x]` `app.video.max-concurrent-jobs` — **ishlab chiqarishda 3**.

  Kod sukuti 1 bo'lib qoladi va bu ataylab: ishlab chiquvchining
  noutbukida ikkita parallel transcoding ilovani ishlatib bo'lmas
  holga keltirardi.

  ⚠️ Nega 3, ko'proq emas: har bir FFmpeg 4K manbada ~1.5 GB RAM
  oladi. 16 GB dan Spring Boot 1–2 GB va PostgreSQL 2–4 GB oladi.
  To'rtinchi ish PostgreSQL keshini siqib chiqarardi va **butun
  saytdagi** so'rovlar sekinlashardi — transcoding esa sezilarli
  tezlashmasdi

- `[x]` `app.video.min-free-disk`

#### ⚠️ FFmpeg o'rnatish — SERVERDA bajariladi

Docker yo'q, ya'ni qo'lda o'rnatiladi. Ubuntu/Debian uchun:

```bash
sudo apt update
sudo apt install -y ffmpeg

# ⚠️ IKKALASI ham kerak — kod ffprobe ni alohida chaqiradi.
ffmpeg -version
ffprobe -version

# ⚠️ ENG MUHIM TEKSHIRUV: kodlovchilar bormi.
# Ularsiz FFmpeg o'rnatilgan bo'lib ko'rinadi, lekin transcoding
# «Unknown encoder» bilan yiqiladi.
ffmpeg -hide_banner -encoders | grep -E ' (libx264|aac) '
```

Ikkala qator ham chiqishi kerak. Chiqmasa — `apt` dagi yig'ma
kesilgan; boshqa manba kerak.

**Tekshirish:** ilovani qayta ishga tushiring va **media
kutubxonasini oching**. Banner ko'rinmasa — hammasi joyida. Log'da
ham bitta qator bo'ladi:

```
Video transcoding tayyor: ffmpeg version … · ffprobe version … · diskda … GB bo'sh
```

⚠️ Bu buyruqlar **bu yerda ishga tushirilmadi** — server hali sotib
olinmagan. Ular tekshirilishi kerak.

#### Manbani 1080p bilan cheklash `[x]` (28.08.2026)

**Qaror: brauzerda, yuklashdan OLDIN ogohlantirish. Rad etish yo'q.**

##### Nima uchun rad etilmaydi

Auditda muhim narsa aniqlandi: **chiqish allaqachon 1080p bilan
cheklangan** — `VideoProfileSelector` manbadan yuqori variant
yasamaydi. Ya'ni 4K manba SIFAT bermaydi; u faqat qayta ishlashni
~2.4 barobar uzaytiradi, chunki har kadr baribir dekodlanishi kerak.

Bu tezlik masalasi, to'g'ri-noto'g'ri masalasi emas — shuning uchun
qaror admin qo'lida qoladi.

##### Nima uchun brauzerda

O'lcham server tomonda faqat transcoding paytida, ya'ni fayl to'liq
yuklab bo'lingandan KEYIN ma'lum bo'ladi. 4 GB lik faylni yuklab
bo'lgach «bu 4K ekan» deyish kech.

Brauzer esa faylni serverga yubormasdan o'qiy oladi.

- `[x]` `probeVideoSize()` — `<video preload="metadata">` orqali.

  ⚠️ Butun fayl yuklanmaydi: u bir necha gigabayt bo'lishi mumkin va
  uni xotiraga tortish brauzerni qotirardi

- `[x]` Aniqlab bo'lmasa — **yuklash to'xtatilmaydi**. Brauzer `.mkv`
      va `.avi` ni odatda ocholmaydi; «bilmayman» sababli to'xtatish
      adminni yaroqli faylni yuklay olmaydigan holga qo'yardi
- `[x]` 5 soniyalik chegara — ba'zi fayllarda `<video>` na
      `loadedmetadata`, na `error` beradi va jimgina osilib qoladi
- `[x]` Tasdiqlash oynasi: «Baribir yuklash» / «Bekor qilish».
      Tugma **qizil emas** — bu maslahat, xato emas
- `[x]` Fayl saqlanadi: «baribir yuklash» desa qaytadan tanlash
      kerak bo'lmaydi

##### ⚠️ Vertikal video tuzog'i — topilgan XATO

Birinchi yozilishida chegara **balandlik** bo'yicha hisoblangan edi.
1080×1920 (Reels) esa «1080p vertikal», «1920p» emas — backend uni
tushirmaydi (`Math.min(width, height)`).

Ya'ni **har bir oddiy vertikal rolik** ogohlantirish oynasini
ochardi. Loyihada vertikal kontent birinchi darajali (§19), ya'ni bu
chekka holat emas — u har kuni takrorlanardi.

Va aynan shu ogohlantirishni o'ldirardi: har safar chiqadigan oyna
o'qilmasdan yopiladigan bo'lib qoladi, keyin esa haqiqiy 4K fayl
kelganda ham ishlamaydi.

- `[x]` Chegara **qisqa tomon** bo'yicha — backend bilan bir xil
- `[x]` `VideoProfileSelectorTest$PanelContract` ikkalasi ajralib
      ketmasligini qo'riqlaydi (manba matnini o'qiydi)

#### Qolgan vazifalar

- `[ ]` FFmpeg serverga o'rnatish (yuqoridagi buyruqlar) — **server
      sotib olingandan keyin**
- `[ ]` Timeweb'da 12 yadro toifasi bor-yo'qligini tasdiqlash
      (odatiy toifalar 2/4/8/16)

---

### 4.14. HAQIQIY S3 bilan tekshiruv `[x]` (28.08.2026)

#### ⚠️ Bu butun ish ichidagi eng katta xavf edi

`S3StorageService`, `S3MultipartUploadService` va `HlsUploadService`
**hech qachon haqiqiy S3 bilan gaplashmagan**. Barcha testlar
`S3Client` ni mock qilardi: ular bizning hisob-kitobimizni
tekshirardi, S3 ning javobini emas.

Presigned imzolar, multipart yig'ish, `ListParts` — hammasi faqat
kutilgan xatti-harakat asosida yozilgan edi.

#### Yechim: MinIO

S3 bilan mos server, lokalda ishlaydi. Hech narsa sotib olish kerak
emas va Timeweb kalitlari ishlatilmaydi (§30).

```bash
brew install minio/stable/minio
MINIO_ROOT_USER=testkey MINIO_ROOT_PASSWORD=testsecret123 \
  minio server /tmp/minio-data --address :9100
```

`S3IntegrationTest` — **9 test, hammasi o'tdi**:

| Nima | Natija |
|---|---|
| Oqim saqlash va qayta o'qish | ✅ |
| `Content-Type` S3 da saqlanadi | ✅ `application/vnd.apple.mpegurl` |
| Boshlang'ich `/` obyekt nomiga tushmaydi | ✅ |
| Mavjudlik va o'chirish | ✅ |
| **Presigned multipart to'liq aylanma** | ✅ 5 MB + 1 KB → yig'ildi |
| Bekor qilish bo'laklarni tozalaydi | ✅ |
| Bo'lak yetishmasa rad etiladi | ✅ |
| HLS papkasini rekursiv yuklash | ✅ 4 fayl, to'g'ri turlar |
| `master.m3u8` bo'lmasa aniq xato | ✅ |

⚠️ MinIO ishlamayotgan bo'lsa test **o'tkazib yuboriladi**
(`assumeTrue`) — CI da u bo'lmasligi mumkin.

**Eng muhimi:** «brauzer imzolangan havola bilan to'g'ridan-to'g'ri
omborga yozadi» degan butun ishning ma'nosi endi **isbotlangan**, taxmin
emas.

---

### 4.15. Sozlama nosozligi tuzatildi `[x]` (28.08.2026)

`application.properties` da:

```properties
spring.datasource.password=${akow8434}
```

Bu **parol emas** — mavjud bo'lmagan xususiyatga havola. Prod
profilida ilova `Could not resolve placeholder 'akow8434'` bilan
**ko'tarilmasdi**.

Lokalda bilinmasdi: `dev` profili o'z `datasource` ini beradi va bu
qatorga umuman yetib bormaydi. Nosozlik faqat serverda, **birinchi
ishga tushirishda** chiqardi.

- `[x]` `${DB_PASSWORD:akow8434}` shakliga o'tkazildi
- `[x]` `application.properties.example` ga `datasource` bo'limi qo'shildi
- `[x]` `ConfigurationPlaceholderTest` — 2 test, 2 mutatsiya

Qo'riqchi test ikki narsani tekshiradi: har bir `${...}` da zaxira
qiymat bor, va namuna faylida ochiq maxfiy qiymat yo'q.

⚠️ Test avval **soxta signal** berdi — `<openssl rand -hex 32>` kabi
hujjat o'rinbosarlarini parol deb hisobladi. Aniqlashtirildi: soxta
signal beradigan testni odamlar e'tiborsiz qoldirishni o'rganadi.

---

### 4.16. OTP endpointlari ko'chirildi `[x]` (28.08.2026)

Ikki kundan beri qizil turgan `OldCastingFrozenTest` yopildi.

**Sabab:** `POST /api/v1/auth/otp/send` va `/verify` ESKI casting
modulining kontrollerida yozilgan edi. U makon **muzlatilgan** —
Telegram bot, eski admin sayti va boshqa mijozlar unga tayanadi.

Buyurtmachi qarori: **ko'chirish**.

```
/api/v1/auth/otp/**      →  /api/v1/app/auth/otp/**
```

- `[x]` `Cms/Controller/AppAuthController` — yangi makonda
- `[x]` Eski `AuthController` dan olib tashlandi
- `[x]` `SecurityConfig` va `RateLimitFilter` yangilandi
- `[x]` Mobil: `api.ts`, ikkita ekran izohi, `docs/API.md`
- `[x]` ⚠️ Mobil `WRITE_ALLOWLIST` ga yangi prefiks

#### ⚠️ Oq ro'yxat — jimgina buziladigan joy

Mobil `READ_ONLY` rejimida ishlaydi va yozish so'rovlarini
**klientda** bloklaydi. Oq ro'yxat `/api/v1/auth/` edi; yangi yo'l
unga tushmasdi.

Qo'shilmaganda SMS orqali kirish **jimgina** buzilardi: interceptor
so'rovni **yuborishdan oldin** yiqitadi va foydalanuvchi serverning
javobini emas, klientning ichki xatosini ko'rardi.

#### Jonli tekshiruv

| So'rov | Javob |
|---|---|
| `POST /api/v1/app/auth/otp/send` | `503 SMS_NOT_CONFIGURED` — Eskiz dev'da sozlanmagan |
| `POST /api/v1/app/auth/otp/verify` | `422 OTP_EXPIRED` |
| `POST /api/v1/auth/otp/send` (eski) | `401` — endpoint yo'q |
| Rate limit (5/daqiqa) | `503 503 503 → 429 429 429` ✅ |

⚠️ `SMS_NOT_CONFIGURED` — bu **to'g'ri xatti-harakat**: provayder
sozlanmaganda soxta muvaffaqiyat qaytarilmaydi.

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
- `[x]` Transcoding profillari
- `[x]` HLS generatori
- `[x]` `master.m3u8`
- `[x]` S3 HLS yuklovchi
- `[x]` Processing holatlari
- `[x]` Fon worker
- `[x]` Yiqilishni boshqarish va qayta urinish
- `[x]` Vaqtinchalik fayllarni tozalash
- `[x]` CDN URL integratsiyasi
- `[x]` ⚠️ Xavfsiz video kirish (playlist proksi + presigned, §4.10)
- `[x]` Admin upload progress (4.11-E)
- `[x]` Admin processing holati (4.11-B, C, D)
- `[x]` Mobil HLS ijro
- `[x]` ABR tekshiruvi (4.6 da haqiqiy FFmpeg bilan)
- `[x]` Testlar
- `[~]` FFmpeg o'rnatish — kod tomoni tayyor (tekshiruv, banner,
      buyruqlar §4.13 da), o'rnatishning o'zi server sotib olingach
- `[x]` Hujjat

---

## 6. Buyurtmachiga savollar

1. ~~**Timeweb CDN tokenli havolani qo'llab-quvvatlaydimi?**~~
   ✅ **Endi bloklamaydi.** §4.10 javob kutmasdan hal qilindi: playlist
   proksi + presigned havola har qanday S3-mos ombor bilan ishlaydi.

   Savol foydali bo'lib qoladi, lekin **ixtiyoriy**: Timeweb tokenni
   qo'llab-quvvatlasa, `SignedUrlProvider` ni almashtirish keshlashni
   yaxshilaydi (segment CDN'dan kelardi). Almashtirish playlist
   mantig'iga tegmaydi.
2. ~~**FFmpeg qayerda ishlaydi?**~~ ✅ **Javob berildi 27.08.2026.**
   API bilan **bitta serverda**: 12 yadro · 16 GB · 200 GB NVMe.
   O'lchovlar va sabab — §4.13.
3. **Docker joriy qilinadimi?** Hozir repoda yo'q. FFmpeg va worker'ni
   ajratish uchun u eng tabiiy yo'l
4. **Mavjud videolar migratsiya qilinadimi?** Lokal diskdagi fayllar S3 ga
   ko'chirilib, HLS ga o'girilsinmi — yoki eski yo'lda qolsinmi

---

## 7. Tarix

**26.08.2026** — audit o'tkazildi, roadmap yozildi. Kod hali yozilmagan.

**27.08.2026** — 4.1–4.9 bajarildi va **haqiqiy FFmpeg bilan
tasdiqlandi** (922 test). Buyurtmachining haqiqiy 4K vertikal videosi
to'liq zanjirdan o'tkazildi: 2:44 → 3 variant, 76 soniya, ABR
chegaralari 28 segmentda ham moslashdi.

Server tanlandi: **12 yadro · 16 GB · 200 GB NVMe** — o'lchovlar §4.13 da.

Qolgan: 4.10 (xavfsizlik, Timeweb qarorini kutmoqda) · 4.11 (admin
panel) · 4.12 (mobil) · 4.13 (FFmpeg o'rnatish).

**28.08.2026** — **4.10 bajarildi** (988 test, hammasi yashil).

Timeweb javobi kutilmadi: tanlangan yechim (playlist proksi + presigned
havola) hech qanday tashqi tasdiqqa bog'liq emas va har qanday S3-mos
ombor bilan ishlaydi. Imzolash `SignedUrlProvider` ortida — Timeweb
tokeni tasdiqlansa uni almashtirish playlist mantig'iga tegmaydi.

Ikki nozik joy o'lchov bilan hal qilindi:

- **imzo keshi** — MinIO'da tasdiqlandiki S3 imzosi vaqtga bog'liq, ya'ni
  keshsiz har tomoshabin o'z havolasini olardi va CDN keshi ishlamasdi;
- **chipta manzil ichida** — pleyer sarlavhalarni segment so'roviga ham
  qo'shadi, S3 esa `Authorization` + `X-Amz-Signature` ni birga rad etadi.

Butun zanjir haqiqiy MinIO bilan uchma-uch sinaldi: playlist ombordan
o'qildi, qayta yozildi, undagi segment havolasi haqiqatdan fayl qaytardi.

Qolgan: 4.13 (serverga FFmpeg o'rnatish, disk monitoringi).

**28.08.2026 (davomi)** — **4.13 ning kod qismi bajarildi** (1013
backend + 121 frontend test).

Uchta jimgina nosozlik yopildi:

- **FFmpeg yo'qligi** endi bir joyda ko'rinadi. Ilgari har bir video
  alohida yiqilardi va admin buzuq fayl izlab yurardi. Tekshiruv
  kodlovchilarni ham ko'radi (`libx264`, `aac`) — ularsiz FFmpeg
  o'rnatilgan bo'lib ko'rinadi, lekin transcoding «Unknown encoder»
  bilan yiqiladi.

- **Disk to'lishi** endi ishni to'xtatadi, urinishlarni sarflamaydi.
  Ilgari disk to'lgach uchala urinish ham yarim fayl yozib, diskni
  yanada to'ldirardi — va PostgreSQL ham yozolmay qolardi.

- **Navbat endpointi** hech bir sahifada ishlatilmasdi. Backend uni
  berardi, `client.js` da chaqiruv ham bor edi, lekin panel uni
  so'ramasdi.

4K ogohlantirishi brauzerda, yuklashdan oldin. Ishlab chiqish paytida
xato topildi: chegara balandlik bo'yicha hisoblangan edi va har bir
oddiy vertikal rolik (1080×1920) bekorga ogohlantirilardi — vertikal
kontent esa loyihada birinchi darajali.

Qolgan: FFmpeg'ni serverga o'rnatish va Timeweb toifasini tasdiqlash —
ikkalasi ham server sotib olingandan keyin.
