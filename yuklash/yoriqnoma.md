# UZCASTING — serverga yuklash yo'riqnomasi

Server: `root@72.56.247.79`

---

## ⚠️ ENG MUHIM QOIDA — qaysi terminal

Har bir buyruq oldida yorliq turadi:

| yorliq | qayerda yoziladi |
|---|---|
| **[MAC]** | O'z kompyuteringizdagi terminal (Terminal.app) |
| **[SERVER]** | `ssh` bilan kirgandan keyingi terminal |

Ikkalasini aralashtirmang. O'tgan safar xato aynan shundan chiqqan edi:
`scp` buyrug'i server sessiyasida yozilgan va fayllar hech qayerga
bormagan.

Serverga kirish:

```bash
ssh root@72.56.247.79
```

Chiqish: `exit`

---

## 0. [MAC] Jar'ni yig'ish

⚠️ **Bu qadam avval yozilmagan edi va aynan shundan xato chiqdi:**
1-sentyabrda yig'ilgan jar uch kun davomida `yuklash/` da yotdi va
serverda eski kod ishlab turdi. Frontend ham, backend ham eskirgan
edi — lekin buni faqat serverdagi bundle'ni ochib ko'rgandagina
bilish mumkin edi.

Har safar yuklashdan OLDIN shu uch qadam bajariladi.

### 0.1 Frontend'ni yig'ish

```bash
cd ~/Desktop/casting-mobil/frontend
npm run build
```

⚠️ `.env.production` fayli `REACT_APP_API_URL=` (bo'sh) bo'lishi
kerak. Bo'sh qiymat manzillarni NISBIY qiladi — ya'ni brauzer
so'rovlarni o'sha domenning o'ziga yuboradi. Bu yerga
`http://localhost:8080` tushib qolsa, serverdagi sayt o'z
kompyuteringizga urinardi va hech narsa ishlamasdi.

**TEKSHIRUV:**

```bash
grep -c "localhost:8080" build/static/js/main.*.js
```

`1` chiqishi normal — bu ishlatilmaydigan zaxira qiymat. Kompilyatsiya
qilingan kod `null !== "" ? "" : "http://localhost:8080"` ko'rinishida
bo'ladi, ya'ni doim bo'sh satr tanlanadi.

### 0.2 Frontend'ni jar ichiga qo'yish

Spring `classpath:/static/` dan sayt beradi — ya'ni React build jar
ICHIGA kirishi kerak.

```bash
cd ~/Desktop/casting-mobil

rm -rf backend/src/main/resources/static
cp -r frontend/build backend/src/main/resources/static
```

⚠️ `rm -rf` SHART. Fayl nomlarida hash bor
(`main.94172d08.js`), ya'ni har build yangi nom beradi. O'chirmasdan
ustiga ko'chirsangiz eski bundle'lar yig'ilib qoladi: jar shishadi va
`git` da keraksiz fayllar to'planadi.

**TEKSHIRUV:**

```bash
ls backend/src/main/resources/static/static/js/main.*.js
```

BITTA fayl bo'lishi kerak.

### 0.3 Jar'ni yig'ish

```bash
cd ~/Desktop/casting-mobil/backend
./mvnw clean package
```

Testlar ham ishga tushadi (~3 daqiqa). Bittasi yiqilsa jar
YIG'ILMAYDI — bu ataylab: sinovdan o'tmagan kod serverga chiqmaydi.

Natija: `backend/target/backend-0.0.1-SNAPSHOT.jar`

```bash
cp target/backend-0.0.1-SNAPSHOT.jar ../yuklash/backend.jar
```

**TEKSHIRUV — jar HAQIQATAN yangimi:**

```bash
cd ~/Desktop/casting-mobil
ls -l yuklash/backend.jar
```

Sana BUGUNGI bo'lishi kerak. Eski sana — 0.3 bajarilmagan degani.

---

## 1. [MAC] Fayllarni serverga yuborish

Yangi terminal oching (`Cmd+N`) va:

```bash
cd ~/Desktop/casting-mobil/yuklash

scp backend.jar application.properties uzcasting.service check-config.sh \
    root@72.56.247.79:/root/
```

Parol so'raydi — serverga kiradigan parolni yozing.

107 MB, taxminan 1–3 daqiqa.

**TEKSHIRUV [SERVER]:**

```bash
ls -lh /root/
```

To'rtta fayl ko'rinishi kerak: `backend.jar`, `application.properties`,
`uzcasting.service`, `check-config.sh`.

---

## 2. [SERVER] Shart-sharoitni tekshirish

```bash
java -version
ffmpeg -version | head -1
psql --version
df -h /
```

Kutilgan natija:

- `java` — **17 yoki yuqori**
- `ffmpeg` — bor (sizda `8.0.1` allaqachon o'rnatilgan)
- `psql` — PostgreSQL o'rnatilgan
- bo'sh joy — kamida 20 GB

Agar `java` yo'q bo'lsa:

```bash
sudo apt update && sudo apt install -y openjdk-17-jre-headless
```

Agar `psql` yo'q bo'lsa:

```bash
sudo apt install -y postgresql
sudo systemctl enable --now postgresql
```

---

## 3. [SERVER] Baza yaratish

⚠️ `SizningBazaParoli` o'rniga **o'zingiz parol o'ylab toping** va uni
yozib qo'ying — 5-qadamda kerak bo'ladi.

```bash
sudo -u postgres psql -c "CREATE USER uzcasting WITH PASSWORD 'SizningBazaParoli';"
sudo -u postgres psql -c "CREATE DATABASE casting OWNER uzcasting;"
```

**TEKSHIRUV:**

```bash
sudo -u postgres psql -lqt | cut -d'|' -f1 | grep -w casting
```

`casting` chiqishi kerak.

---

## 4. [SERVER] Fayllarni joyiga qo'yish

```bash
sudo mkdir -p /opt/uzcasting

sudo mv /root/backend.jar            /opt/uzcasting/backend.jar
sudo mv /root/application.properties /opt/uzcasting/
sudo chmod 600 /opt/uzcasting/application.properties
```

Eski jar qolgan bo'lsa o'chiring:

```bash
rm -f /root/backend-0.0.1-SNAPSHOT.jar
```

**TEKSHIRUV:**

```bash
ls -lh /opt/uzcasting/
```

---

## 5. [SERVER] Sozlamani to'ldirish

```bash
sudo nano /opt/uzcasting/application.properties
```

Quyidagi to'rtta qatorni toping va to'ldiring
(`nano` da qidirish: `Ctrl+W`, keyin `BU_YERGA`):

| qator | kalit | nima yoziladi |
|---|---|---|
| 34 | `spring.datasource.password=` | 3-qadamda o'ylab topgan baza paroli |
| 43 | `app.storage.s3.bucket=` | S3 bucket nomi |
| 44 | `app.storage.s3.access-key=` | S3 access key |
| 45 | `app.storage.s3.secret-key=` | S3 secret key |

Domen manzillarini ham o'zingiznikiga moslang:

| qator | kalit |
|---|---|
| 50 | `app.video.cdn.base-url=` |
| 54 | `app.cors.allowed-origins=` |

⚠️ `app.jwt.secret` (70-qator) **tayyor** — tegmang.

Saqlash: `Ctrl+O` → `Enter` → `Ctrl+X`

**TEKSHIRUV:**

```bash
sh /root/check-config.sh /opt/uzcasting/application.properties
```

`✓` chiqmaguncha keyingi qadamga o'tmang.

---

## 6. [SERVER] Birinchi ishga tushirish

```bash
java -jar /opt/uzcasting/backend.jar
```

⚠️ `cd` kerak emas — jar sozlamani o'z yonidan topadi.

**Kutilgan natija (loglar oxirida):**

```
Successfully applied 29 migrations to schema "public", now at version v29
Started BackendApplication in ... seconds
Video transcoding tayyor: ffmpeg ... diskda ... GB bo'sh
```

Xato chiqsa — pastdagi «Muammolar» bo'limiga qarang.

**TEKSHIRUV — ikkinchi terminal oching va serverga qayta kiring:**

```bash
curl -i http://localhost:8080/api/v1/app/home
```

`HTTP/1.1 200` kutiladi.

Birinchi terminalga qaytib `Ctrl+C` bilan to'xtating.

---

## 7. [SERVER] Panel hisobini yaratish

⚠️ **Busiz panelga kira olmaysiz.** Parollar bo'sh bo'lgani uchun
hech qanday hisob yaratilmaydi — bu ataylab, aks holda har
o'rnatishda `admin / admin` qolib ketardi.

```bash
sudo nano /opt/uzcasting/application.properties
```

Faylning **eng oxiridagi** ikki qatorni toping — ular `#` bilan
izohga olingan:

```properties
# app.gipersuperadmin.phone=+998901110001
# app.gipersuperadmin.password=BU_YERGA_KUCHLI_PAROL
```

`#` belgilarini olib tashlang va parol qo'ying:

```properties
app.gipersuperadmin.phone=+998901110001
app.gipersuperadmin.password=UzCasting2026
```

**Parol talabi:** kamida 8 belgi, harf **va** raqam aralash.
Zaif parollar (`00000000`, `password`, `12345678`) qabul qilinmaydi.

Qayta ishga tushiring:

```bash
java -jar /opt/uzcasting/backend.jar
```

Logda shu qator chiqishi kerak:

```
Panel hisobi yaratildi: +998901110001 (ROLE_GIPERSUPERADMIN)
```

⚠️ Hisob yaratilgach bu ikki qatorni **qayta izohga oling** (`#`
qo'ying) va xizmatni qayta ishga tushiring. Parol serverda ochiq
faylda turishi shart emas.

**Panelga kirish:**

```
http://72.56.247.79:8080/
```

Login: `+998901110001` · Parol: o'zingiz qo'ygani

---

## 8. [SERVER] Doimiy xizmat qilib qo'yish

Qo'lda ishga tushirish faqat sinov uchun — terminal yopilsa ilova
ham to'xtaydi.

```bash
sudo useradd -r -s /usr/sbin/nologin uzcasting
sudo chown -R uzcasting:uzcasting /opt/uzcasting

sudo mv /root/uzcasting.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now uzcasting
```

**TEKSHIRUV:**

```bash
sudo systemctl status uzcasting
curl -i http://localhost:8080/api/v1/app/home
```

Loglarni kuzatish:

```bash
sudo journalctl -u uzcasting -f
```

To'xtatish / qayta ishga tushirish:

```bash
sudo systemctl stop uzcasting
sudo systemctl restart uzcasting
```

---

## 9. Keyingi safar yangilash

Faqat jar almashadi, sozlamaga tegilmaydi.

⚠️ **Avval 0-qadamni bajaring** — jar'ni qaytadan yig'ing. Bu
o'tkazib yuborilsa `scp` eski jar'ni yuboradi va serverda hech narsa
o'zgarmaydi. Xato jimgina: buyruqlar muvaffaqiyatli tugaydi, xizmat
qayta ishga tushadi, log toza — faqat kod eskiligicha qoladi.

Frontend o'zgarmagan bo'lsa ham 0.2 ni o'tkazib yubormang: jar
ichidagi sayt oxirgi `cp` dan qolgan holatda turadi.

**[MAC]**

```bash
cd ~/Desktop/casting-mobil/yuklash
scp backend.jar root@72.56.247.79:/root/
```

**[SERVER]**

```bash
sudo systemctl stop uzcasting
sudo mv /root/backend.jar /opt/uzcasting/backend.jar
sudo chown uzcasting:uzcasting /opt/uzcasting/backend.jar
sudo systemctl start uzcasting
sudo journalctl -u uzcasting -f
```

**TEKSHIRUV [MAC] — yangi kod HAQIQATAN chiqdimi:**

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -X OPTIONS https://uzcasting.com/api/v1/app/watch-progress/continue
```

`200` — yangi backend. `404` — eski jar hali ishlayapti.

Frontend uchun:

```bash
JS=$(curl -s https://uzcasting.com/ | grep -o '/static/js/main\.[a-z0-9]*\.js' | head -1)
curl -s "https://uzcasting.com$JS" | grep -c "tomosha/:type/:id"
```

`1` — yangi frontend. `0` — eski build.

⚠️ Sahifa ochilgani yetarli DALIL EMAS: `/kirish` va `/tomosha/...`
manzillari eski build'da ham `200` qaytaradi, chunki Spring noma'lum
yo'llarni `index.html` ga yo'naltiradi. Sahifa ochiladi, lekin bo'sh
bo'ladi.

---

## 10. Videoni himoyalash (sotuvdan oldin)

⚠️ **Hozir video HIMOYALANMAGAN.** Manzilni bilgan har kim pullik
filmni imzosiz ko'chirib oladi:

```bash
curl -o /dev/null -w "%{http_code}\n" \
  https://cdn.uzcasting.com/videos/146/hls/master.m3u8
# hozir: 200 — imzosiz ochiq
```

Kod tomoni **tayyor**. Qolgani — to'rtta qadam, va **tartibi muhim**.

### 10.1 Panelda tokenni yoqish

`https://timeweb.cloud/my/cdn/31973/management` → `Безопасность` →
`Secure token` → yoqiladi. Panel maxfiy kalit beradi — uni nusxalab
oling.

### 10.2 Kalitni sozlamaga yozish

```bash
nano /opt/uzcasting/application.properties
```

Quyidagi qatorni toping va izohdan chiqarib, kalitni qo'ying:

```properties
app.video.cdn.secure-token.secret=PANELDAN_OLINGAN_KALIT
```

⚠️ Bo'sh qoldirmang. Kod bo'sh kalitni «o'chiq» deb qabul qiladi —
ya'ni yarim yoqilgan holat bo'lmaydi, lekin himoya ham ishlamaydi.

### 10.3 Qayta yig'ib yuklash va TEKSHIRISH

0-bo'limdan boshlab odatdagidek yuklang, so'ng:

```bash
sudo systemctl restart uzcasting
```

Brauzerda film oching va **ko'rilishiga ishonch hosil qiling**.

⚠️ **Ochilmasa** — kalitni izohga qaytaring va qayta ishga tushiring:

```bash
nano /opt/uzcasting/application.properties   # qator oldiga # qo'ying
sudo systemctl restart uzcasting
```

Tizim darhol eski holatga qaytadi. Bucket hali ochiq bo'lgani uchun
bu bosqichda hech narsa yo'qolmaydi — aynan shuning uchun bucket
ENG OXIRIDA yopiladi.

### 10.4 Bucketni yopish

Faqat 10.3 muvaffaqiyatli bo'lgandan keyin. Timeweb panelida S3 →
`Публичный` → `Приватный`.

Tekshirish — ikkalasi ham **403** qaytarishi shart:

```bash
curl -o /dev/null -w "CDN imzosiz: %{http_code}\n" \
  https://cdn.uzcasting.com/videos/146/hls/master.m3u8

curl -o /dev/null -w "S3 to'g'ridan: %{http_code}\n" \
  https://s3.twcstorage.ru/00847558-22cb-4af0-bdbf-d750dfbdac8a/videos/146/hls/master.m3u8
```

Va brauzerda film **hamon ochilishi** kerak — pleyer endi imzolangan
havolalar bilan ishlaydi.

⚠️ Agar bu bosqichda video ochilmay qolsa, bucketni vaqtincha
`Публичный` ga qaytaring: bu darhol tiklaydi.

---

## Muammolar

### `JWT KALITI BERILMAGAN`

Sozlama fayli topilmagan. Xabar ichida «Hozirgi ishchi papka» yozilgan —
o'sha papkada `application.properties` yo'q degani.

```bash
ls -l /opt/uzcasting/application.properties
```

Fayl yo'q bo'lsa — 1 va 4-qadamlarni qayta bajaring.

### `Port 8080 was already in use`

Eski jarayon qolgan:

```bash
sudo lsof -ti:8080
sudo kill -9 $(sudo lsof -ti:8080)
```

### `Connection refused` (baza)

PostgreSQL ishlamayapti yoki parol noto'g'ri:

```bash
sudo systemctl status postgresql
```

Parolni 5-qadamdagi 34-qator bilan solishtiring.

### Panelga kira olmayapman

7-qadam bajarilmagan. Logda `Panel hisobi yaratildi` qatori bormi:

```bash
sudo journalctl -u uzcasting | grep "Panel hisobi"
```

`hisobi YARATILMADI` yozilgan bo'lsa — parol talabga javob bermayapti
(kamida 8 belgi, harf va raqam).

### 15 daqiqada tizimdan chiqib ketyapman

SSL hali ulanmagan, `http://` orqali kiryapsiz. Refresh cookie
saqlanmayapti.

```bash
sudo nano /opt/uzcasting/application.properties
```

80-qator: `app.auth.cookie-secure=false`

SSL ulangandan keyin `true` ga qaytaring.

### Video yuklanmayapti

S3 kalitlari to'ldirilmagan (5-qadam, 43–45-qatorlar). Ilova
ko'tariladi va panel ishlaydi, faqat yuklash ishlamaydi.

---

## Qisqacha xotira

```
[MAC]     scp ... root@72.56.247.79:/root/
[SERVER]  sudo mv → /opt/uzcasting/
[SERVER]  nano → 4 ta qiymatni to'ldir
[SERVER]  sh check-config.sh → ✓
[SERVER]  java -jar /opt/uzcasting/backend.jar
[SERVER]  nano → panel paroli → qayta ishga tushir
[SERVER]  systemctl enable --now uzcasting
```
