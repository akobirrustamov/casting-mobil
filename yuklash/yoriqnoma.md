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
