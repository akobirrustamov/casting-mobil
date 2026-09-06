# Timeweb: S3 + CDN sozlash

Video yetkazishni CDN orqali ishga tushirish. Bu yerda **nima topilgani**,
**nima qilingani** va **nima qolgani** yozilgan.

Sana: 2026-09-02

---

## Resurslar

| Nima | Qiymat |
|---|---|
| Proyekt | `Casting project` |
| Server | `72.56.247.79` — 4 CPU · 8 GB RAM · 80 GB NVMe |
| S3 bucket | `00847558-22cb-4af0-bdbf-d750dfbdac8a` (1.5 / 10 GB) |
| S3 endpoint | `https://s3.twcstorage.ru` |
| CDN resurs | `CDN-00847558-22cb-4af0-bdbf-d750dfbdac8a` (id `31973`) |
| CDN texnik domen | `5msryv35jk.cdn.twcstorage.ru` |
| CDN o'z domeni | `cdn.uzcasting.com` |
| Domen | `uzcasting.com` — 2027-08-29 gacha to'langan |

CDN paneli: `https://timeweb.cloud/my/cdn/31973/management`

---

## Boshlang'ich holat — nima allaqachon to'g'ri edi

Tekshirilgani:

```
Источник контента     → S3-бакет 00847558-…            ✓ to'g'ri
Домены раздачи        → cdn.uzcasting.com qo'shilgan   ✓
DNS CNAME             → cdn.uzcasting.com
                         → 5msryv35jk.cdn.twcstorage.ru
                         → 5msryv35jk.a.trbcdn.net
                         → 91.238.111.224              ✓ ishlaydi
CDN texnik domen      → HTTP 200, 0.77 s               ✓ kontent beryapti
AWS-авторизация       → Включено                       ✓
```

Ya'ni CDN **qurilgan va ishlayapti**. Ikkita to'siq bor edi.

---

## ⚠️ To'siq 1 — SSL sertifikati yo'q edi

```
https://cdn.uzcasting.com/... → HTTP 000 (ulanmaydi)
http://cdn.uzcasting.com/...  → HTTP 200 (ishlaydi)

Sertifikat: CN=*.a.trbcdn.net     ← CDN ning o'z sertifikati
                                     cdn.uzcasting.com ga MOS EMAS
```

Brauzer bunday sertifikatni rad etadi. Ya'ni CDN ni kodga ulasak ham,
brauzer segmentlarni ololmasdi — va xato «video ochilmadi» bo'lib
ko'rinardi, sababi esa sertifikat ekani bilinmasdi.

**Qilingani:** `SSL-сертификаты` → `Выпустить Let's Encrypt`.
Javob: «Отправлена задача на выпуск сертификата», holat «Выпускается…».

Bepul. Let's Encrypt sertifikati 90 kunlik va Timeweb uni o'zi yangilab
turadi.

> ⚠️ Sertifikat chiqarilayotganda resurs **«Применяются настройки»**
> holatiga o'tadi va qolgan sozlama panellari OCHILMAYDI. Shuning uchun
> keyingi qadamlar sertifikat tayyor bo'lgach bajariladi.

**Tekshirish:**
```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  https://cdn.uzcasting.com/videos/146/hls/480p/segment_00000.m4s
# 200 kutiladi

echo | openssl s_client -connect cdn.uzcasting.com:443 \
  -servername cdn.uzcasting.com 2>/dev/null \
  | openssl x509 -noout -subject
# subject=CN=cdn.uzcasting.com kutiladi
```

---

## ⚠️ To'siq 2 — bucket OCHIQ

```bash
curl https://s3.twcstorage.ru/00847558-…/videos/146/hls/480p/segment_00000.m4s
# → HTTP 200, IMZOSIZ
```

Bucket «Публичный». Ya'ni:

- backend segmentlarga imzo qo'yadi (`PresignedUrlProvider`), lekin
  **imzo hech nimani himoya qilmaydi** — manzilni bilgan har kim
  imzosiz ham oladi;
- pullik film havolasini nusxalab tarqatsa, obunasiz odam ham ko'radi.

Bu CDN muammosi emas, lekin CDN ulangandan keyin ham shunday qoladi.

**Qaror:** hozircha shunday qoldiriladi — maqsad web va mobil ilovani
ishga tushirish, parametrlar sinov uchun. Lekin **haqiqiy sotuvdan
oldin** yopilishi shart. Yo'li quyida, «Keyingi bosqich» da.

> **2026-09-06 yangilanishi.** Kod tomoni **tayyor**:
> `SecureTokenUrlProvider` yozildi va sinovdan o'tdi. Qolgani —
> panelda Secure token'ni yoqib, kalitni serverga qo'yish. Batafsil:
> «Keyingi bosqich» bo'limi.
>
> ⚠️ Yana bir marta tekshirildi (2026-09-06): bucket **hali ham
> ochiq**. Ham S3 dan, ham CDN dan imzosiz 200 keladi.

---

## Kodda nima o'zgarishi kerak

### Hozirgi holat

`app.video.cdn.base-url` sozlamasi **ishlatilmaydi**.

`PlaybackUrlService.hlsUrlFor()`:

```java
if (signedUrls.isEmpty() || !signedUrls.get().isAvailable()) {
    return cdnUrlService.masterUrl(master);   // ← CDN faqat SHU YERDA
}
// S3 bor → o'z proksimiz + imzolangan S3 havolalari
```

`app.storage.provider=s3` bo'lgani uchun birinchi shox hech qachon
ishlamaydi. Segment manzilini `PresignedUrlProvider.presign()` yasaydi
va u doim `s3.twcstorage.ru` ni qaytaradi.

### Kerakli o'zgarish

`PresignedUrlProvider.presign()` qaytargan manzilda S3 xosti CDN xostiga
almashtiriladi:

```
https://s3.twcstorage.ru/00847558-…/videos/146/hls/480p/segment_0.m4s?X-Amz-…
              ↓
https://cdn.uzcasting.com/videos/146/hls/480p/segment_0.m4s?X-Amz-…
```

⚠️ Diqqat: bucket nomi yo'ldan **tushib qoladi** — CDN origin allaqachon
o'sha bucket, ya'ni yo'l bucketning ichidan boshlanadi. Texnik domen
bilan tekshirilgani buni tasdiqlaydi:

```
s3.twcstorage.ru/<bucket>/videos/146/…     ← bucket yo'lda
cdn.uzcasting.com/videos/146/…             ← bucket YO'Q
```

CDN sozlanmagan bo'lsa — hozirgidek qoladi (`app.video.cdn.base-url`
bo'sh → S3 manzili).

### Nega imzo saqlanadi

Bucket ochiq bo'lgani uchun imzo hozir shart emas. Lekin uni olib
tashlash keyin bucketni yopganda hamma narsani qaytadan yozishni talab
qilardi. Imzo qoladi — u zarar qilmaydi va kelajakka yo'l ochiq turadi.

---

## CDN panelida qolgan sozlamalar

Sertifikat tayyor bo'lgach bajariladi.

### 1. HTTP-заголовки → CORS

hls.js segmentlarni `fetch` bilan oladi. Javobda
`Access-Control-Allow-Origin` bo'lmasa brauzer segmentni bermaydi va
video ochilmaydi.

Ruxsat etilishi kerak:
```
https://uzcasting.com
http://localhost:3000        ← lokal ishlab chiqish uchun
```

Mobil ilovaga CORS kerak emas (u brauzer emas).

### 2. Кэширование

| Fayl turi | TTL | Sabab |
|---|---|---|
| `.m4s`, `.mp4` (init) | uzoq — 30 kun | Segment hech qachon o'zgarmaydi |
| `.m3u8` | qisqa yoki 0 | Playlistni baribir bizning server beradi |

⚠️ Segment manzilida imzo bor va u har soatda yangilanadi
(`app.video.signed-url-window=1h`). Ya'ni amalda kesh bir soat ishlaydi —
bu me'yorda va ataylab shunday.

**So'rov qatori kesh kalitiga kirishi shart.** Kirmasa CDN imzoli va
imzosiz so'rovlarga bir xil javob berardi.

### 3. Контент и подключение

Yoqilsin:
- **HTTP/3** — mobil tarmoqda sezilarli tezroq
- **Оптимизация доставки больших файлов** — video uchun aynan shu

Gzip video uchun kerak emas (segment allaqachon siqilgan).

### 4. Безопасность

- **Редирект с HTTP на HTTPS** — yoqilsin
- **Secure token** — kod TAYYOR (2026-09-06), panelda hali
  yoqilmagan. Yoqish tartibi «Keyingi bosqich» bo'limida; tartib
  buzilsa barcha video birdan ochilmay qoladi.

### 5. Ограничение исходящего трафика

Sinov bosqichida limit qo'yish tavsiya etiladi — kutilmagan trafik
hisobni oshirib yubormasligi uchun. Kengayganda olib tashlanadi.

---

## Tekshirish rejasi

Kod o'zgarganidan keyin:

```bash
# 1. Segment CDN dan keladimi
curl -s -o /dev/null -w "%{http_code} %{time_total}s\n" \
  "https://cdn.uzcasting.com/videos/146/hls/480p/segment_00000.m4s"

# 2. Ikkinchi so'rovda kesh ishlaydimi (X-Cache: HIT kutiladi)
curl -sI "https://cdn.uzcasting.com/videos/146/hls/480p/segment_00000.m4s" \
  | grep -i "x-cache\|age\|cache-control"

# 3. CORS sarlavhasi bormi
curl -sI -H "Origin: https://uzcasting.com" \
  "https://cdn.uzcasting.com/videos/146/hls/480p/segment_00000.m4s" \
  | grep -i "access-control"
```

Brauzerda: `/tomosha/content/13` ochilib, tarmoq oynasida segment
so'rovlari `cdn.uzcasting.com` ga ketayotgani ko'rilsin.

---

## Keyingi bosqich — bucketni yopish

Haqiqiy sotuvdan oldin bajariladi.

### Kod tomoni — TAYYOR ✅

`SecureTokenUrlProvider` (`Cms/Service/Video/`) yozildi. U Timeweb
hujjatidagi algoritmni bajaradi:

```
raw    = SECRET + PATH + IP + EXPIRES
token  = base64(md5_xom_baytlar(raw)),  + → -,  / → _,  = olib tashlanadi
manzil = {CDN}/md5({token},{EXPIRES}){PATH}
```

⚠️ Uchta nozik joy — uchalasi ham test bilan qulflangan
(`SecureTokenUrlProviderTest`, 12 ta test):

| Nozik joy | Xato qilinsa |
|---|---|
| MD5 dan **xom baytlar** olinadi, o'n oltilik satr emas | CDN har bir segmentga 403 |
| `EXPIRES` **oynaga tekislanadi** | Har tomoshabin boshqa manzil oladi, kesh butunlay ishlamaydi |
| IP imzoga **kirmaydi** | Telefon Wi-Fi'dan uyali tarmoqqa o'tganda film o'rtasida to'xtaydi |

Kutilgan token qiymatlari hujjatdagi Python namunasi bilan
hisoblangan va testga yozib qo'yilgan. Ya'ni algoritm noto'g'ri
bo'lsa test yiqiladi — serverda emas.

**Yoqilishi:** faqat `app.video.cdn.secure-token.secret` berilganda.
Kalit yo'q ekan, bean umuman yaratilmaydi va tizim eskicha ishlaydi.
Shuning uchun yangi kodni yuklash **hech narsani o'zgartirmaydi**.

### Qolgan qadamlar — panelda

⚠️ **Tartib muhim.** Buzilsa hamma video birdan ochilmay qoladi.

1. **Panel** → CDN → Secure token yoqiladi, kalit olinadi
2. Kalit serverdagi `application.properties` ga yoziladi
   (`app.video.cdn.secure-token.secret=`, izoh olib tashlanadi)
3. Jar qayta yig'iladi va yuklanadi — yo'riqnomaning **0-bo'limi**
4. **Video ochilishi tekshiriladi.** Ochilmasa: kalitni izohga
   qaytaring va qayta yuklang — tizim darhol eski holatga qaytadi
5. Ochilsa — **shundagina** bucket «Приватный» qilinadi
6. Yana tekshiriladi:

```bash
# imzosiz — endi 403 bo'lishi SHART
curl -o /dev/null -w "%{http_code}\n" \
  https://cdn.uzcasting.com/videos/146/hls/master.m3u8

# to'g'ridan-to'g'ri S3 — bu ham 403
curl -o /dev/null -w "%{http_code}\n" \
  https://s3.twcstorage.ru/00847558-…/videos/146/hls/master.m3u8
```

⚠️ 5-qadam eng oxirida turishi shart. Bucket avval yopilsa, hali
imzosiz ishlayotgan havolalar **o'sha zahoti** ishlamay qoladi.

⚠️ 4-qadamdagi qaytish yo'li — butun rejaning xavfsizligi shunda.
Har bir qadamdan keyin orqaga qaytish mumkin bo'lgani uchun bu
o'zgarish xavfsiz.

---

## Yo'l-yo'lakay topilgan ikkita kamchilik

Bevosita CDN ga aloqasi yo'q, lekin ombor bilan bog'liq.

### HLS papkasi o'chirilmaydi

Media o'chirilganda faqat asl fayl o'chadi. `videos/{id}/hls/**` — uchala
variant, barcha segmentlar, init fayllar — S3 da **abadiy qoladi**.
2:44 lik sinov videosi uchun bu ~90 ta obyekt.

`MediaController.delete()` da `storageService.delete(storageKey)` bor,
`hlsMasterKey` ga tegilmaydi.

CDN ulangandan keyin bu ikki barobar sezilarli: keshda ham, omborda ham
keraksiz ma'lumot.

### ~~SMS yuborish cheklovsiz~~ — TUZATILDI ✅

`/api/v1/app/auth/register/start` haqiqiy SMS yuborardi (Eskiz — pul
ketadi) va IP bo'yicha cheklanmagan edi. Endpoint butunlay olib
tashlangan, qolgan uchta OTP yo'li cheklangan.

**Lekin kasallik chuqurroq edi.** 2026-09-06 dagi qayta tekshiruvda
`RateLimitFilter.RULES` shunchaki ro'yxat ekani va uni **hech kim
qo'riqlamayotgani** ma'lum bo'ldi. Shu sababli yana uchta yo'l
cheklovsiz qolgan edi:

| Yo'l | Kim ishlatadi | Qo'shilgan cheklov |
|---|---|---|
| `/api/v1/app/auth/refresh` | mobil ilova + sayt tomoshabini | **60/60** |
| `/api/v1/auth/google` | mobil ilova (Google orqali kirish) | **10/60** |
| `/api/v1/auth/refresh` | hech kim (eski yo'l, ochiq turibdi) | **30/60** |

⚠️ Moslash `uri.equals()` orqali — **prefiks bo'yicha tushmaydi**.
Shuning uchun `/app/admin/auth/refresh` uchun yozilgan qoida
`/app/auth/refresh` ni qoplamasdi. Izohida esa nega kerakligi
yozilgan edi — ya'ni sabab bilingan, qoida esa yarmiga qo'llangan.

⚠️ `google` uchun qo'shimcha sabab: har bir so'rov Google serveriga
tashqi murojaat qiladi. Cheklovsiz begona odam bizning nomimizdan
Google'ga flud yuborardi va Google javob berishni to'xtatardi — ya'ni
kirish **hamma uchun** buzilardi.

⚠️ `refresh` chegarasi ataylab kengroq (60, admin'da 30): uyali
operator yuzlab abonentni bitta tashqi manzil ortiga qo'yadi (CGNAT).
Tor chegara hujumchini emas, o'sha operatordagi barcha odamlarni
to'sardi.

**Endi qo'riqlanadi:** `ApiConventionTest.RateLimiting` — kirishga oid
har bir yangi POST yo ro'yxatga tushishi, yo izohi bilan ataylab
istisno qilinishi shart. Unutish mumkin emas.

---

## Sozlama kalitlari

| Kalit | Hozir | Izoh |
|---|---|---|
| `app.storage.provider` | `s3` | Imzolash yo'lini yoqadi |
| `app.storage.s3.endpoint` | `https://s3.twcstorage.ru` | Yuklash uchun. **O'zgarmaydi** |
| `app.storage.s3.bucket` | `00847558-…` | |
| `app.video.cdn.base-url` | `https://cdn.uzcasting.com` | Kod o'zgarishidan keyin ishlaydi |
| `app.video.cdn.secure-token.secret` | **bo'sh** | Berilsa CDN tokeniga o'tadi. Qarang: «Keyingi bosqich» |
| `app.video.signed-url-ttl` | `4h` | Imzo qancha yashaydi |
| `app.video.signed-url-window` | `1h` | Kesh oynasi — CDN samaradorligi shunga bog'liq |
| `app.video.ticket-ttl` | `6h` | Playlist chiptasi. CDN ga aloqasi yo'q |
| `app.video.max-concurrent-jobs` | `1` | ⚠️ 8 GB xotira uchun. Ilgari `3` edi — FFmpeg bilan birga OOM berardi |
| `app.video.min-free-disk` | `10GB` | 80 GB diskda me'yorda |

⚠️ **S3 imzosida** amaldagi muddat ≈ **3 soat** (`ttl` minus `window`):
imzo yasalgan paytdan boshlanadi, oyna esa uni eskirtiradi. Bitta
sahifa seansi shundan uzoq davom etsa, video o'rtasida 403 chiqadi.

⚠️ **CDN tokenida** bu boshqacha va yaxshiroq: muddat oynaning
OXIRIDAN + `ttl` hisoblanadi, ya'ni amaldagi umr **4–5 soat**. Sabab —
tokenda vaqt oldindan qo'yiladi, S3 imzosida esa imzolash vaqti
qatnashadi.

---

## ⚠️ To'siq 3 — panel tahrirlash uchun ochilmadi

Sertifikatdan keyin qolgan sozlama bo'limlari (`Кэширование`,
`HTTP-заголовки`, `Контент и подключение`) **ochilmay qoldi**:
«Настроить» bosilganda oyna chiqmaydi.

Sabab API orqali aniqlandi — resurs holati:

```
GET /api/v1/cdn/http-resources/31973  →  "status": "processing"
```

**Yechim:** sozlamalar **API orqali** qo'llandi. Panel kerak bo'lmadi.

---

## API orqali sozlash

### Manzillar

Panel qanday so'rov yuborishini kuzatib topildi (hujjatda oson
topilmadi):

```
GET   /api/v1/cdn/http-resources/{id}                 — resurs
GET   /api/v1/cdn/http-resources/{id}/configuration   — sozlamalar (faqat O'QISH)
PATCH /api/v1/cdn/http-resources/{id}                 — O'ZGARTIRISH ← shu yerda
POST  /api/v1/cdn/certificates/issue                  — sertifikat
```

To'liq sxema: `https://timeweb.cloud/api-docs-data/bundle.json`
(241 ta endpoint, JS sahifada emas — shu JSON da).

⚠️ `/configuration` ga `PATCH` **405** beradi. O'zgartirish resursning
o'ziga, `config` kaliti bilan yuboriladi.

### Ikkita tuzoq

1. **`null` qiymatlar rad etiladi.** To'liq konfiguratsiyani qaytarib
   yuborsangiz `config.access.property allowed_methods should not
   exist` chiqadi — chunki u `null`. Yuborishdan oldin barcha `null`
   maydonlar olib tashlanishi kerak.

2. **CORS domenlari SXEMASIZ.** `https://uzcasting.com` → 400
   «Wrong data format». `uzcasting.com` → 200. `*` ham qabul
   qilinmaydi.

⚠️ `PATCH` **birlashtiradi**, almashtirmaydi — tekshirildi. Ya'ni faqat
kerakli maydonni yuborish xavfsiz, `origin.aws` va domenlar joyida
qoladi.

### Qo'llangan sozlamalar

```bash
K="<api-kalit>"
curl -X PATCH -H "Authorization: Bearer $K" -H "Content-Type: application/json" \
  -d '{"config":{
        "delivery": {"http3": true, "large_files": true},
        "security": {"redirect": true},
        "http_headers": {"cors": {"domains": ["uzcasting.com"], "always": true}},
        "cache": {"query_args": {"mode": "all"}}
      }}' \
  https://api.timeweb.cloud/api/v1/cdn/http-resources/31973
```

| Sozlama | Bo'lgan | Bo'ldi | Nima uchun |
|---|---|---|---|
| `delivery.http3` | false | **true** | Mobil tarmoqda sezilarli tezroq |
| `delivery.large_files` | false | **true** | Video uchun aynan shu |
| `security.redirect` | false | **true** | HTTP → HTTPS |
| `http_headers.cors` | yo'q | **uzcasting.com** | Brauzer segmentni olishi uchun |
| `cache.query_args` | yo'q | **all** | Imzo kesh kalitiga kirsin |

Tegilmagani: `origin` (AWS kalitlari), `domains`, `certificate_id`,
`cache.cdn.ttl` (3600 s — imzo bir soatda yangilangani uchun mos),
`secure_token` (bucket ochiq, hozircha kerak emas).

---

## ⚠️ Lokal ishlab chiqish uchun MUHIM

CDN ning CORS ro'yxatiga `localhost` ni **qo'shib bo'lmaydi** —
Timeweb faqat haqiqiy domenni qabul qiladi (`localhost` ham,
`localhost:3000` ham 400 beradi).

Tekshirilgani:

```
Origin: https://uzcasting.com   → access-control-allow-origin bor  ✓
Origin: http://localhost:3000   → sarlavha YO'Q                    ✗
```

Ya'ni lokal `application.properties` da CDN manzili turgan bo'lsa,
**lokalda video ochilmaydi**: playlistlar bizning serverdan keladi
(sifat tugmalari chiqadi), segmentlar esa CDN dan — va brauzer ularni
bloklaydi. Video 0:00 da spinner bilan qotib qoladi.

**Nima qilish kerak:** ildizdagi `application.properties` da shu qatorni
izohga oling:

```properties
# app.video.cdn.base-url=https://cdn.uzcasting.com
```

Bo'sh bo'lsa kod S3 manzilini o'zgarishsiz qaytaradi — lokalda aynan shu
kerak. Serverdagi faylda esa qator **qoladi**.

⚠️ Bu faylga men tegmadim — parollar va sozlamalar sizniki.

---

## Kod: bajarilgan

`PresignedUrlProvider.viaCdn()` qo'shildi. Imzolangan manzilda S3
domeni CDN domeniga almashtiriladi va bucket yo'ldan tushiriladi:

```
https://s3.twcstorage.ru/{bucket}/videos/146/hls/480p/segment_0.m4s?X-Amz-…
                            ↓
https://cdn.uzcasting.com/videos/146/hls/480p/segment_0.m4s?X-Amz-…
```

`CdnUrlService.base()` qo'shildi — u tayyor manzildan domenni
almashtirish uchun kerak (`masterUrl()` kalit qabul qiladi, bu yerda
yaramaydi).

**Nega imzo buzilmaydi:** CDN origin'ga o'zining AWS kalitlari bilan
boradi (`origin.aws` panelda sozlangan), ya'ni havoladagi imzo S3 ga
umuman yetib bormaydi. Tekshirilgan — imzoli va imzosiz so'rov ham CDN
orqali 200 qaytaradi.

**Testlar:** `CdnSegmentUrlTest` — 6 ta. Mutatsiya bilan tekshirildi:
bucket tushirilmasa, so'rov qatori tashlansa yoki CDN'siz holat
buzilsa — testlar yiqiladi. Backend jami **1152/1152**.

---

## Natija

```
Segment manzili : https://cdn.uzcasting.com/videos/146/hls/480p/segment_00000.m4s?X-Amz-…
CDN orqali      : HTTP 200 · 0.49 s
To'g'ridan S3   : HTTP 200 · 0.97 s      ← ikki barobar sekin
Sertifikat      : CN=cdn.uzcasting.com · Let's Encrypt · 2026-12-01
CORS            : uzcasting.com uchun ishlaydi
```

---

## ⚠️ To'siq 3 — panel «Применяются настройки» da qotib qolgan

Sertifikat chiqarilgandan keyin qolgan sozlama bo'limlari
(`Кэширование`, `HTTP-заголовки`, `Контент и подключение`,
`Безопасность`) **ochilmay qoldi**: «Настроить» bosilganda oyna
chiqmaydi.

Resurs sarlavhasida sariq nuqta bilan **«Применяются настройки»**
yozuvi turibdi. Bu holat sertifikat chiqarishdan OLDIN ham bor edi,
ya'ni yangi paydo bo'lgani emas.

Sinab ko'rilgani (hammasi natijasiz):
- `Настроить` tugmasini bosish — to'rt xil bo'limda
- Bo'lim sarlavhasining o'ziga bosish
- Sahifani qayta yuklab, birinchi bosish

⚠️ `Источник и домены раздачи` bo'limi **birinchi urinishda ochilgan
edi** — ya'ni panel butunlay bloklangan emas. Sabab aniqlanmadi.

**Nima qilish kerak:**
1. Resurs holati «Активен» ga o'tishini kutish (sarlavhadagi nuqta
   yashil bo'ladi)
2. O'tmasa — Timeweb qo'llab-quvvatlash xizmatiga murojaat qilish
3. Yoki Timeweb API orqali sozlash: `/my/api-keys` da kalit olinadi

---

## SSL: chiqarilgan, lekin hali tarqalmagan

Panelda sertifikat **tayyor** ko'rinadi:

```
cdn.uzcasting.com  ✓   Let's Encrypt   1 dekabr 2026 gacha
```

Lekin tashqaridan hali eski sertifikat qaytyapti:

```
$ openssl s_client -connect cdn.uzcasting.com:443 ...
subject=CN=*.a.trbcdn.net        ← hali eskisi

$ curl https://cdn.uzcasting.com/...
HTTP 000                          ← hali ulanmaydi
```

~20 daqiqa kuzatildi, o'zgarmadi. CDN tugunlariga tarqalishi odatda
shu vaqt ichida tugaydi, lekin ba'zan uzoqroq oladi.

**Tekshirish buyrug'i:**
```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  https://cdn.uzcasting.com/videos/146/hls/480p/segment_00000.m4s
```
`200` chiqsa — tayyor.

---

## Holat

| Qadam | Holat |
|---|---|
| CDN resurs · origin · domen · DNS | ✅ tayyor edi |
| SSL sertifikati | ✅ chiqarildi va ishlaydi |
| HTTP/3, katta fayl optimizatsiyasi | ✅ yoqildi |
| HTTP → HTTPS redirect | ✅ yoqildi |
| CORS (`uzcasting.com`) | ✅ ishlaydi |
| Kesh kaliti (`query_args: all`) | ✅ |
| Kod: segment manzili CDN ga | ✅ testlar bilan |
| Lokal `cdn.base-url` ni izohga olish | ✅ bajarilgan |
| **Kod: CDN secure token** | ✅ **yozildi, 12 ta test** |
| Panelda Secure token'ni yoqish | ⬜ **sizda** |
| Kalitni serverga qo'yish | ⬜ **sizda** (2-qadam) |
| Bucketni «Приватный» qilish | ⬜ **sizda** (eng oxirida) |

⚠️ Resurs holati API da hamon `processing` — lekin barcha sozlamalar
amalda qo'llangan va tekshirilgan. Bu Timeweb tomonidagi ko'rsatkich
xatosi bo'lishi mumkin; ish qilishga xalaqit bermayapti.
