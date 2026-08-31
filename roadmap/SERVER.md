# SERVER — UZCASTING uchun talablar

Yozildi: 27.08.2026 · Buyurtmachi qarori bilan tasdiqlangan

Raqamlar **haqiqiy o'lchovlarga** asoslangan, taxminga emas — o'lchov
usuli §5 da.

---

## 1. Nima buyurtma qilinadi

Uchta narsa kerak. Faqat serverni olish **yetarli emas**.

| # | Nima | Nima uchun |
|---|---|---|
| 1 | **VPS/Cloud server** | Spring Boot API + PostgreSQL + FFmpeg |
| 2 | **Timeweb Object Storage (S3)** | Videolar va HLS fayllari |
| 3 | **Timeweb CDN** | Videoni foydalanuvchilarga yetkazish |

⚠️ **Object Storage'siz server diski 8 kunda to'ladi** — hisob §4 da.

---

## 2. Server parametrlari

```
12 vCPU · 16 GB RAM · 200 GB NVMe SSD
Ubuntu 22.04 LTS yoki 24.04 LTS
```

⚠️ Timeweb'da 12 yadro toifasi bo'lmasligi mumkin (odatiy toifalar
2/4/8/16). Bo'lmasa — **8 yadro ham yetadi**, lekin zaxira kamayadi
(§5 dagi jadval).

### Nega aynan shunday

| Parametr | Sabab |
|---|---|
| **12 vCPU** | 30 ta video/kun, eng og'ir holatda 35% bandlik. 8 da 52% — ishlaydi, lekin 45 daqiqalik videoda sutka to'ladi |
| **16 GB** | Spring Boot 2 + PostgreSQL 4 + 3 ta FFmpeg 4.5 + OS 4 ≈ 14 GB. Sig'adi, lekin zaxira kam — 24 GB muvozanatliroq |
| **200 GB NVMe** | S3 bilan juda yetarli. ⚠️ **NVMe shart** — FFmpeg diskka tinimsiz yozadi, oddiy SSD 2–3 barobar sekinlashtiradi |

---

## 3. O'rnatiladigan dasturlar

```bash
# Java 17 (loyiha aynan shu versiyaga qurilgan)
apt install -y openjdk-17-jdk

# PostgreSQL 14+
apt install -y postgresql postgresql-contrib

# ⚠️ FFmpeg — MAJBURIY. Usiz video qayta ishlanmaydi.



# Nginx — reverse proxy va TLS
apt install -y nginx certbot python3-certbot-nginx
```

### Tekshirish

```bash
java -version            # 17.x
ffmpeg -version          # 6.x yoki yuqori
ffprobe -version         # ffmpeg bilan birga keladi
psql --version           # 14+
```

⚠️ `ffmpeg` da `libx264` va `aac` **bo'lishi shart**:

```bash
ffmpeg -hide_banner -encoders | grep -E " libx264 | aac "
```

Ikkalasi chiqmasa — video kodlanmaydi.

---

## 4. Disk hisobi

### Object Storage ISHLATILSA (to'g'ri yo'l)

| Nima | Hajm |
|---|---|
| OS + Java + PostgreSQL | ~40 GB |
| Transcoding vaqtinchalik (3 parallel) | ~2.4 GB |
| Baza (faqat metadata) | ~5 GB / yil |
| **Jami** | **~50 GB** → 200 GB da katta zaxira |

### Object Storage ISHLATILMASA

```
30 video/kun × (600 MB manba + 200 MB HLS) = 24 GB/kun
200 GB ÷ 24 GB = 8 KUN
```

⚠️ Shuning uchun S3 majburiy.

---

## 5. Transcoding tezligi — O'LCHANGAN

### O'lchov usuli

Buyurtmachining haqiqiy videosi: **2160×3840 vertikal 4K, 59.94 fps,
2:44, 591 MB**. Apple M4 (10 yadro) da to'liq HLS zanjiri.

| Manba | 30 soniya → | Tezlik |
|---|---|---|
| 4K 60fps | 13.3s | real vaqtdan 2.3× tez |
| 1080p 30fps | 5.5s | real vaqtdan 5.4× tez |

Server yadrosi M4 dan sekinroq (x264 da ~1.7×) — quyida shu zaxira
bilan.

### Kunlik yuk — 30 ta video

**4K 60fps manba** (og'ir holat):

| Yadro | 30×15 daq | 30×30 daq | 30×45 daq | Bandlik (30 daq) |
|---|---|---|---|---|
| 8 | 6.2 soat | 12.4 soat | 18.7 soat | 52% |
| **12** | **4.1 soat** | **8.3 soat** | **12.4 soat** | **35%** |
| 16 | 3.1 soat | 6.2 soat | 9.3 soat | 26% |

**1080p 30fps manba** (odatiy):

| Yadro | 30×15 daq | 30×30 daq | Bandlik (30 daq) |
|---|---|---|---|
| 8 | 2.6 soat | 5.1 soat | 21% |
| **12** | **1.7 soat** | **3.4 soat** | **14%** |

### ⚠️ Eng arzon optimizatsiya

**Manbani 1080p bilan cheklash yukni 2.4 barobar kamaytiradi.**

Foydalanuvchi baribir 1080p dan yuqorisini ko'rmaydi — sifat
zinapoyasining eng yuqorisi 1080p. 4K yuklash faqat server vaqtini
sarflaydi.

12 yadroda bandlik 35% → **14%** ga tushadi.

---

## 6. CDN — asosiy xarajat

Server oyiga ~$40–80. CDN esa foydalanuvchilarga bog'liq:

| Sifat | Bitrate | 1 soat ko'rish |
|---|---|---|
| 1080p | 5 Mbit/s | 2.2 GB |
| 720p | 2.8 Mbit/s | 1.3 GB |
| 480p | 1.2 Mbit/s | 0.5 GB |

**3000 foydalanuvchi:**

| Kunlik ko'rish | Oyiga |
|---|---|
| 20 daqiqa, 720p | ~39 TB |
| 1 soat, 720p | ~117 TB |

⚠️ **Timeweb CDN narxini BUYURTMA QILISHDAN OLDIN tekshiring.** U
server narxidan bir necha barobar oshib ketishi mumkin va buni kech
bilish og'ir bo'ladi.

Kamaytirish: 480p ni sukut qilish (mobil ekranda farq sezilmaydi),
1080p ni faqat Wi-Fi da berish.

---

## 7. Environment o'zgaruvchilari

⚠️ **Maxfiy qiymatlar fayllarga YOZILMAYDI** — faqat environment orqali.
To'liq ro'yxat: `backend/src/main/resources/application.properties.example`

```bash
# ── Baza ──────────────────────────────────────────────
DB_URL=jdbc:postgresql://localhost:5432/casting
DB_USERNAME=uzcasting
DB_PASSWORD=<kuchli parol>

# ── JWT ───────────────────────────────────────────────
APP_JWT_SECRET=<kamida 64 belgi, tasodifiy>

# ── Panel hisoblari ───────────────────────────────────
APP_GIPERSUPERADMIN_PASSWORD=<kuchli parol>
APP_SUPERADMIN_PASSWORD=<kuchli parol>
APP_ADMIN_PASSWORD=<kuchli parol>
APP_WORKER_PASSWORD=<kuchli parol>

# ── S3 (Timeweb Object Storage) ───────────────────────
STORAGE_PROVIDER=s3
S3_ENDPOINT=https://s3.twcstorage.ru
S3_REGION=ru-1
S3_BUCKET=<bucket nomi>
S3_ACCESS_KEY=<kalit>
S3_SECRET_KEY=<maxfiy kalit>

# ── CDN ───────────────────────────────────────────────
CDN_BASE_URL=https://video.uzcasting.site

# ── Video ─────────────────────────────────────────────
FFMPEG_PATH=/usr/bin/ffmpeg
FFPROBE_PATH=/usr/bin/ffprobe
VIDEO_MAX_JOBS=3
VIDEO_TEMP_DIR=/var/lib/uzcasting/transcoding
VIDEO_MAX_ATTEMPTS=3

# ── CORS ──────────────────────────────────────────────
APP_CORS_ALLOWED_ORIGINS=https://uzcasting.site
```

### ⚠️ `VIDEO_MAX_JOBS` — 12 yadro uchun 3

Har bir FFmpeg ~4 yadroni samarali ishlatadi. 3 ta parallel ish
12 yadroni to'ldiradi va API ga ham joy qoldiradi.

8 yadroda bu **2** bo'lishi kerak.

---

## 8. Ishga tushirish tartibi

```
1. Server olinadi, Ubuntu o'rnatiladi
2. Java 17 · PostgreSQL · FFmpeg · Nginx o'rnatiladi
3. PostgreSQL bazasi va foydalanuvchisi yaratiladi
4. Object Storage bucket ochiladi, kalitlar olinadi
5. CDN sozlanadi, origin = S3 bucket
6. Environment o'zgaruvchilari qo'yiladi
7. `./mvnw clean package` → jar
8. systemd xizmati yoziladi
9. Nginx + TLS (certbot)
10. Birinchi ishga tushirish — Flyway migratsiyalarni o'zi qo'llaydi
```

⚠️ **9-qadamdan keyin, 10-qadamdan oldin** `spring.flyway.clean-disabled=true`
ekaniga ishonch hosil qiling. U `application.properties` da bor va
`DatabaseRulesTest` uni qo'riqlaydi.

---

## 9. Hali hal qilinmagan

| Masala | Holat |
|---|---|
| **Pullik video himoyasi** | ⚠️ Timeweb CDN tokenli havolani qo'llab-quvvatlaydimi — javob kutilmoqda (roadmap §4.10) |
| Docker | Repoda yo'q. FFmpeg qo'lda o'rnatiladi |
| Monitoring | Disk joyi va navbat uzunligi kuzatilishi kerak |
| Zaxira nusxa | PostgreSQL uchun rejalashtirilmagan |

⚠️ **Birinchisi eng muhim.** Hozircha pullik video eski yo'lda
(`/api/v1/app/media/{id}/raw`) qoladi — u server orqali oqadi va
CDN'dan foyda bermaydi. Timeweb javobisiz bu masalani yopib bo'lmaydi.
