# UZCASTING — serverga o'rnatish va birinchi sinov

> Sizda bor: **Linux server · S3 · domen · CDN · SSL**, o'zaro ulangan.
> Bu hujjat noldan ishlaydigan tizimgacha bo'lgan yo'lni bosqichma-bosqich
> beradi va har bosqich oxirida **nima ko'rinishi kerakligini** aytadi.
>
> ⚠️ Har bir «TEKSHIRUV» blokini o'tkazib yubormang. Keyingi bosqichda
> chiqadigan xatoning sababi deyarli har doim oldingi bosqichda bo'ladi,
> lekin u yerda hech narsa qichqirmaydi.

---

## 0. Umumiy manzara — nima qayerda ishlaydi

```
   brauzer / mobil ilova
        │
        ├── https://uzcasting.site ──► Nginx ──► Spring Boot (8080)
        │                                          │
        │                                          ├─► PostgreSQL
        │                                          ├─► FFmpeg (transcoding)
        │                                          └─► S3 (fayllar)
        │
        ├── S3 (to'g'ridan-to'g'ri) ◄── katta faylni yuklash
        │                                imzolangan havola bilan
        │
        └── https://cdn.uzcasting.site ◄── video segmentlari
```

Uchta narsani alohida ushlab turing, ular chalkashadi:

| Nima | Qayerda | Kim so'raydi |
|---|---|---|
| **API + admin panel** | Spring Boot, jar ichida | brauzer, mobil |
| **Fayl yuklash** | brauzer → **to'g'ridan S3 ga** | faqat panel |
| **Video segmentlari** | S3/CDN, imzolangan havola | pleyer |

⚠️ **Panel alohida joylashtirilmaydi.** U jar ichida keladi — 3-bosqichga
qarang. Bu eng ko'p unutiladigan narsa.

---

## 1. Serverni tayyorlash

### 1.1. Asosiy dasturlar

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-17-jdk postgresql nginx ffmpeg unzip
```

⚠️ **Java 17 aynan.** Loyiha shu versiyaga yig'ilgan; 21 da ham ishlashi
mumkin, lekin sinalmagan.

**TEKSHIRUV**

```bash
java -version          # 17.x bo'lishi kerak
psql --version
nginx -v
ffmpeg -version
ffprobe -version
```

### 1.2. ⚠️ FFmpeg kodlovchilari — eng ko'p o'tkazib yuboriladigan tekshiruv

```bash
ffmpeg -hide_banner -encoders | grep -E ' (libx264|aac) '
```

**Ikkala qator ham chiqishi shart.** Chiqmasa FFmpeg o'rnatilgan bo'lib
ko'rinadi, lekin transcoding «Unknown encoder» bilan yiqiladi — va buni
faqat birinchi video yuklangandan keyin bilasiz.

Chiqmasa: `apt` dagi yig'ma kesilgan, boshqa manba kerak.

### 1.3. Foydalanuvchi va papkalar

```bash
sudo useradd -r -m -d /opt/uzcasting -s /bin/bash uzcasting
sudo mkdir -p /opt/uzcasting/{files,logs}
sudo chown -R uzcasting:uzcasting /opt/uzcasting
```

⚠️ Ilova **root'dan ishlamaydi**. U tashqi so'rovlarni qabul qiladi va
FFmpeg kabi tashqi jarayonlarni ishga tushiradi.

---

## 2. PostgreSQL

```bash
sudo -u postgres psql
```

```sql
create user uzcasting with password '<KUCHLI_PAROL>';
create database casting owner uzcasting;
\q
```

**TEKSHIRUV**

```bash
psql "postgresql://uzcasting:<PAROL>@localhost:5432/casting" -c "select 1;"
```

⚠️ **Jadval yaratmang.** Sxemani Flyway o'zi qo'yadi, birinchi ishga
tushirishda. Qo'lda yaratilgan jadval migratsiyani yiqitadi.

### Zaxira nusxa — hoziroq

```bash
sudo -u postgres crontab -e
```

```
0 3 * * * pg_dump casting | gzip > /var/backups/casting-$(date +\%F).sql.gz
0 4 * * * find /var/backups -name 'casting-*.sql.gz' -mtime +14 -delete
```

⚠️ Buni «keyin qilaman» ga qoldirmang. Migratsiyalar orqaga
qaytarilmaydi va zaxira — yagona yo'l.

---

## 3. Jar yig'ish — ⚠️ ENG MUHIM BOSQICH

Bu **ishlab chiqish mashinangizda** bajariladi, serverda emas.

### 3.1. Panelni jar ichiga qo'yish

Panel `classpath:/static/` dan beriladi, ya'ni **frontend jar ichiga
kiradi**. Bu qadam o'tkazib yuborilsa deploy jimgina eskilanadi: backend
yangi, panel eski. Hech qanday xato chiqmaydi.

```bash
# Repozitoriy ILDIZIDAN
cd frontend && CI=false npx react-scripts build && cd ..

rm -rf backend/src/main/resources/static
cp -R frontend/build backend/src/main/resources/static

# TEKSHIRUV: ikkala qator BIR XIL bo'lishi shart
grep -o 'main\.[a-z0-9]*\.js' frontend/build/index.html | head -1
grep -o 'main\.[a-z0-9]*\.js' backend/src/main/resources/static/index.html | head -1
```

### 3.2. Yig'ish

```bash
./backend/mvnw -f backend/pom.xml clean package
```

⚠️ `-DskipTests` **qo'ymang**. Testlar aynan xavfli joylarni
qo'riqlaydi: pullik video sizishi, token rotatsiyasi, ruxsatlar.

**TEKSHIRUV**

```bash
ls -lh backend/target/backend-0.0.1-SNAPSHOT.jar    # ~107 MB

unzip -p backend/target/backend-0.0.1-SNAPSHOT.jar \
  BOOT-INF/classes/static/index.html | grep -o 'main\.[a-z0-9]*\.js' | head -1
```

Oxirgi buyruq 3.1 dagi bilan bir xil natija berishi shart.

### 3.3. Serverga yuborish

```bash
scp backend/target/backend-0.0.1-SNAPSHOT.jar \
    <user>@uzcasting.site:/tmp/backend.jar
```

```bash
# Serverda
sudo mv /tmp/backend.jar /opt/uzcasting/backend.jar
sudo chown uzcasting:uzcasting /opt/uzcasting/backend.jar
```

---

## 4. S3 — birinchi marta yoqiladi

⚠️ Bu reliz S3 haqiqatan ishlatiladigan birinchi reliz. Uchta narsa
kerak: **bucket · kalitlar · CORS**.

### 4.1. Bucket

Timeweb panelida bucket yarating va yozib oling:

| Nima | Misol |
|---|---|
| Endpoint | `https://s3.twcstorage.ru` |
| Region | `ru-1` |
| Bucket nomi | `uzcasting-media` |
| Access key | `…` |
| Secret key | `…` |

### 4.2. ⚠️ CORS — bo'lmasa katta fayl yuklanmaydi

8 MB dan katta fayl **brauzerdan to'g'ridan-to'g'ri bucketga** ketadi,
serverni chetlab. Bucketda CORS bo'lmasa brauzer so'rovni o'zi
to'sadi va panelda «Bo'lak yuborilmadi» chiqadi — server loglarida esa
**hech narsa bo'lmaydi**, chunki so'rov serverga umuman kelmagan.

Bucket sozlamalarida CORS qoidasi:

```json
[
  {
    "AllowedOrigins": ["https://uzcasting.site"],
    "AllowedMethods": ["PUT", "GET", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

⚠️ `ExposeHeaders: ETag` **majburiy**. Multipart yig'ilishi har bo'lakning
`ETag` iga tayanadi; usiz yuklash oxirida yig'ish yiqiladi.

### 4.3. Bucket ochiqmi yoki yopiq

**Yopiq qoldiring.** Video segmentlari imzolangan havola bilan beriladi
(§4.10), ya'ni bucketni ochishning hojati yo'q va ochilsa pullik kontent
himoyasi yo'qoladi.

### 4.4. ⚠️ Eski fayllar nima bo'ladi

Hech narsa. `RoutingStorageService` shunday ishlaydi:

```
yozish  → S3
o'qish  → avval lokal disk, topilmasa S3
```

Ya'ni serverda allaqachon yotgan fayllar ishlashda davom etadi, yangilari
S3 ga tushadi. Migratsiya shart emas.

---

## 5. CDN

CDN'ning **origin** i — S3 bucket.

```
CDN domeni:  https://cdn.uzcasting.site
Origin:      https://s3.twcstorage.ru/uzcasting-media
```

⚠️ CDN **so'rov qatorini (query string) origin'ga uzatishi** va uni kesh
kalitiga kiritishi shart. Segmentlar imzolangan havola bilan keladi va
imzo aynan so'rov qatorida turadi. Uzatilmasa — barcha segment 403.

**TEKSHIRUV** (5-bosqichdan keyin, birinchi video yuklangach)

```bash
# Bucketga qo'lda kichik fayl qo'ying va CDN orqali oching
curl -I https://cdn.uzcasting.site/<test-fayl>
```

---

## 6. Sozlama — BITTA fayl

⚠️ **Sirlar jar ichiga qo'yilmaydi.** Jar'ni olgan har kim uni ochib
o'qiy oladi, jar esa `scp` bilan yuboriladi, `/tmp` da yotadi,
zaxira nusxaga tushadi.

O'rniga bitta fayl jar YONIDA turadi. Spring Boot uni **o'zi
topadi** — na bayroq, na muhit o'zgaruvchisi kerak:

```
/opt/uzcasting/
    backend.jar
    application.properties     ← barcha sozlamalar shu yerda
```

```bash
cd /opt/uzcasting && java -jar backend.jar
```

### 6.1. To'ldirish

`deploy/application.properties` da to'rtta joy `BU_YERGA…` deb
belgilangan:

| Nima | Qayerdan |
|---|---|
| `spring.datasource.password` | PostgreSQL'da yaratganingiz (2-bosqich) |
| `app.storage.s3.bucket` | Timeweb Object Storage |
| `app.storage.s3.access-key` | O'sha paneldan |
| `app.storage.s3.secret-key` | O'sha paneldan |

Domen va CDN manzillarini ham o'zingiznikiga moslang.

⚠️ **`app.jwt.secret` allaqachon yasalgan.** Uni o'zgartirmang —
almashtirsangiz barcha foydalanuvchilar tizimdan chiqib ketadi.

### 6.2. TEKSHIRUV — serverga qo'yishdan oldin

```bash
sh deploy/check-config.sh deploy/application.properties
```

⚠️ O'tkazib yubormang. To'ldirilmagan qiymat ilovani **turlicha**
yiqitadi: `app.jwt.secret` yo'q bo'lsa u darhol va tushunarli xato
bilan to'xtaydi, `s3.bucket` to'ldirilmagan bo'lsa esa ilova
**ko'tariladi** va nosozlik faqat birinchi fayl yuklashda, butunlay
boshqa joyda chiqadi.

### 6.3. Serverga qo'yish

```bash
scp backend/target/backend-0.0.1-SNAPSHOT.jar <user>@uzcasting.site:/tmp/backend.jar
scp deploy/application.properties <user>@uzcasting.site:/tmp/
scp deploy/uzcasting.service <user>@uzcasting.site:/tmp/
```

```bash
# Serverda
sudo mv /tmp/backend.jar /opt/uzcasting/backend.jar
sudo install -m 600 -o uzcasting -g uzcasting \
     /tmp/application.properties /opt/uzcasting/application.properties
sudo install -m 644 /tmp/uzcasting.service /etc/systemd/system/uzcasting.service
sudo chown uzcasting:uzcasting /opt/uzcasting/backend.jar
sudo rm -f /tmp/application.properties
```

⚠️ `/tmp` dan o'chirishni unutmang — u yerda fayl hamma uchun
o'qiladigan bo'lib qolishi mumkin.

---

## 7. systemd xizmati

Unit fayl 6.3 da allaqachon qo'yilgan (`deploy/uzcasting.service`).

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now uzcasting
sudo journalctl -u uzcasting -f
```

⚠️ Unit faylda sozlama yo'li **ko'rsatilmagan** va bu ataylab:
`WorkingDirectory=/opt/uzcasting` berilgan, Spring Boot esa o'sha
papkadagi `application.properties` ni o'zi topadi.

**TEKSHIRUV** — loglarda uchta qator:

```
Successfully applied 29 migrations … now at version v29
Started BackendApplication in … seconds
Video transcoding tayyor: ffmpeg version … · diskda … GB bo'sh
```

⚠️ «Video transcoding ISHLAMAYDI» chiqsa — 1.2 ga qayting. Sayt
baribir ishlaydi, faqat video HLS'ga o'girilmaydi.

⚠️ «JWT KALITI BERILMAGAN» chiqsa — `application.properties`
jar yonida emas yoki `WorkingDirectory` boshqa joyni ko'rsatyapti.

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/app/home
# kutilgan: 200
```

---

## 8. Nginx va SSL

```bash
sudo nano /etc/nginx/sites-available/uzcasting
```

```nginx
server {
    listen 80;
    server_name uzcasting.site www.uzcasting.site;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name uzcasting.site www.uzcasting.site;

    ssl_certificate     /etc/letsencrypt/live/uzcasting.site/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/uzcasting.site/privkey.pem;

    # ⚠️ 1 MB (sukut) bo'lsa afisha ham yuklanmaydi.
    # 8 MB gacha fayl bitta so'rovda keladi, kattasi bo'laklab.
    client_max_body_size 60m;

    # ⚠️ Yuklash sekin internetda daqiqalar davom etadi. Sukut 60s
    # bo'lsa u o'rtada uziladi va panel «tarmoq xatosi» ko'rsatadi.
    proxy_read_timeout    300s;
    proxy_send_timeout    300s;
    proxy_request_buffering off;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        # ⚠️ Bu sarlavhasiz rate limiter BARCHA so'rovlarni bitta
        # manzildan kelgan deb hisoblaydi va foydalanuvchilarni
        # bir-birini bloklaydi.
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/uzcasting /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

SSL sertifikat hali olinmagan bo'lsa:

```bash
sudo certbot --nginx -d uzcasting.site -d www.uzcasting.site
```

**TEKSHIRUV**

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://uzcasting.site/api/v1/app/home
curl -sI https://uzcasting.site | head -1
```

---

## 9. Birinchi ishga tushirish — migratsiyalar

Migratsiyalar birinchi startda **o'zi** qo'llanadi. Tekshiring:

```sql
select version, description, success
from flyway_schema_history order by installed_rank;
```

⚠️ Toza bazada **V1 dan V29 gacha** hammasi `success = true` bo'lishi
kerak. V19, V21, V23 ro'yxatda **yo'q** — ular hech qachon
yaratilmagan, bu normal.

Loglarda shu qator chiqadi:

```
Successfully applied 29 migrations to schema "public", now at version v29
```

> Bu yerdagi hamma narsa shu reliz jari bilan **toza bazada haqiqatan
> sinab ko'rilgan**: tashqi sozlama fayli o'qildi, 29 ta migratsiya
> qo'llandi, panel jar ichidan ochildi, `/app/me`, `/app/favorites` va
> `/app/auth/refresh` tokensiz 401 berdi, eski casting yo'llari 200
> qaytardi.

Bitta ham `false` bo'lsa — to'xtang, loglarni o'qing va menga ayting.
Yarim qo'llangan sxema ustiga ishlamang.

---

## 10. Admin hisobini yaratish

`application.properties` ning oxirida (3-QISM) ikkita qator izohda
turibdi. Ularni oching va parol qo'ying:

```properties
app.gipersuperadmin.phone=+998901110001
app.gipersuperadmin.password=<kuchli_parol>
```

```bash
sudo systemctl restart uzcasting
```

Parol talabi: kamida 8 belgi, harf va raqam.

⚠️ Hisob yaratilgach bu ikki qatorni **qayta izohga oling** va
xizmatni qayta ishga tushiring. Parol serverda faylda turishi shart
emas.

⚠️ Ular bo'sh bo'lsa hisob umuman yaratilmaydi — ya'ni tasodifan
zaif parolli admin paydo bo'lmaydi.

---

## 11. BIRINCHI SINOV — ketma-ketlik

Quyidagi tartibda bajaring. Har qadam oldingisiga tayanadi.

### 11.1. Panel ochiladimi

`https://uzcasting.site/app/panel/login`

- [ ] Kirish sahifasi ochiladi
- [ ] Hisob bilan kiriladi
- [ ] Chap menyuda **«Yo'riqnoma»** bor

⚠️ «Yo'riqnoma» yo'q bo'lsa — panel eski, ya'ni **3.1 o'tkazib
yuborilgan**. Qaytadan yig'ing.

### 11.2. Kichik fayl (afisha) — server orqali

Media → rasm yuklang (2–5 MB).

- [ ] Yuklandi va kutubxonada ko'rinadi
- [ ] Rasm ochiladi

⚠️ «Maximum upload size» → 6-bosqichdagi `multipart` satrlari yo'q.
⚠️ `413 Request Entity Too Large` → nginx `client_max_body_size`.

### 11.3. ⚠️ Katta fayl (video) — S3 ga to'g'ridan-to'g'ri

Media → 50–200 MB video yuklang.

- [ ] Progress bar oxirigacha boradi
- [ ] Kutubxonada paydo bo'ladi

⚠️ **Bu yerda yiqilsa sabab deyarli har doim CORS** (4.2). Brauzer
konsolini oching: `CORS policy` haqida xabar bo'ladi. Server
loglarida hech narsa bo'lmaydi — so'rov serverga kelmagan.

Bucketda faylni ko'ring: `/content/…` yo'lida turishi kerak.

### 11.4. Transcoding

Yuklangandan keyin 15 soniya kuting va kutubxonaga qarang.

- [ ] Nishon `Navbatda` → `Qayta ishlanmoqda` → `HLS tayyor`
- [ ] Yuqorida qizil banner **yo'q**

```bash
sudo journalctl -u uzcasting -f | grep -i transcod
```

⚠️ `Yiqildi` bo'lsa nishonni bosing — xato matni ko'rinadi. Eng
ko'p uchraydigani: FFmpeg kodlovchisi yo'q (1.2) yoki diskda joy kam.

Bucketda tekshiring: `/videos/<id>/hls/` ichida `master.m3u8` va
variant papkalari (`1080p`, `720p`, `480p`) bo'lishi kerak.

### 11.5. Video ochiladimi

Kontent yarating, videoni biriktiring, nashr qiling. Keyin:

```bash
curl -s "https://uzcasting.site/api/v1/app/watch/content/<id>" | head -c 400
```

- [ ] `allowed: true`
- [ ] `hlsUrl` bor va `/api/v1/app/media/…/hls/master.m3u8?t=…` bilan boshlanadi

⚠️ `hlsUrl` `null` bo'lsa — transcoding tugamagan yoki CDN
sozlanmagan.

### 11.6. ⚠️ Pullik video himoyasi

Kontentni `PREMIUM_ONLY` qiling va **tokensiz** so'rang:

```bash
curl -s "https://uzcasting.site/api/v1/app/watch/content/<id>" | head -c 300
```

- [ ] `allowed: false`
- [ ] `sources` **bo'sh**

Chiptasiz playlist ham yopiq bo'lishi kerak:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  "https://uzcasting.site/api/v1/app/media/<mediaId>/hls/master.m3u8"
# kutilgan: 400 yoki 404 — 200 BO'LMASLIGI SHART
```

### 11.7. Mobil oqim

```bash
# Profil (token bilan)
curl -s https://uzcasting.site/api/v1/app/me -H "Authorization: Bearer <token>"

# Token yangilash
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  https://uzcasting.site/api/v1/app/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refresh_token":"soxta"}'
# kutilgan: 401 (404 bo'lsa — eski jar ketgan)
```

### 11.8. Eski mijozlar buzilmaganini

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://uzcasting.site/api/v1/news
curl -s -o /dev/null -w "%{http_code}\n" https://uzcasting.site/api/v1/casting-user/web
# ikkalasi ham: 200
```

⚠️ Bu yo'llardan Telegram bot va eski admin sayti yuradi.

---

## 12. Kuzatib turiladigan narsalar

| Nima | Qanday | Nega |
|---|---|---|
| Disk | `df -h` yoki paneldagi banner | Transcoding vaqtinchalik fayl yozadi; 10 GB dan pastda ish to'xtaydi |
| Navbat | Panel → Media | Uzun navbat — FFmpeg ulgurmayapti |
| `refresh_token` jadvali | `select count(*) from refresh_token;` | Har kirish va yangilash qator yozadi; tozalash har kuni 03:15 da |
| Loglar | `journalctl -u uzcasting --since today \| grep -i error` | |
| Zaxira | `ls -lh /var/backups/casting-*` | Har kuni yangisi paydo bo'lishi kerak |

---

## 13. Orqaga qaytarish

```bash
sudo systemctl stop uzcasting
sudo cp /opt/uzcasting/backend.jar.bak /opt/uzcasting/backend.jar
sudo systemctl start uzcasting
```

⚠️ **Bazani qaytarish shart emas.** Migratsiyalar faqat qo'shadi, eski
jar ortiqcha ustunlarni e'tiborsiz qoldiradi (`ddl-auto=none`).

S3 dan lokalga qaytish kerak bo'lsa: `app.storage.provider=local`.
S3 ga tushgan fayllar shundan keyin ochilmaydi — shuning uchun bu
faqat birinchi kunlar uchun.

---

## 14. Hali qilinmagan — bilib turing

| Nima | Holat |
|---|---|
| **To'lov tizimlari** | Ulanmagan. Xarid va donat ishlamaydi |
| **FCM push** | Kalit yo'q. Yuborishga urinilsa 503, urinish `FAILED` saqlanadi |
| **Mobil ilova** | Alohida reliz. Serverga chiqarish uni yangilamaydi |
| **Katalog va qidiruv (video)** | Yo'q. Kontentga faqat bosh sahifa orqali boriladi |
| **Docker** | Yo'q. FFmpeg qo'lda o'rnatiladi |
| **Rate limiter** | Xotirada. Bir nechta instansiya uchun Redis kerak bo'ladi |

---

## 15. Qisqacha tartib

```
1.  apt: java17 · postgresql · nginx · ffmpeg
2.  ffmpeg -encoders | grep libx264 aac        ← O'TKAZIB YUBORMANG
3.  postgres: user + database (jadval YARATMANG)
4.  crontab: kunlik zaxira
5.  [lokalda] frontend build → static/ → mvnw package   ← ENG MUHIM
6.  scp jar → /opt/uzcasting/
7.  S3: bucket + kalitlar + CORS (ExposeHeaders: ETag)
8.  CDN: origin = bucket, query string uzatilsin
9.  application.properties (sirlarsiz)
10. systemd (sirlar shu yerda)
11. nginx + certbot
12. start → loglarda «Video transcoding tayyor»
13. flyway_schema_history: V1…V29 success
14. admin hisobi → keyin parolni systemd'dan olib tashlang
15. 11-bo'lim bo'yicha sinov
```
