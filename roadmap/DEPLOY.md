# Serverga chiqarish — 29.08.2026 relizi

> Bu reliz `uzcasting.site` da **allaqachon turgan** yangi platforma
> ustiga chiqadi. Toza server uchun emas — u `SERVER.md` §8 da.

---

## 0. Bu relizda nima bor

| Ish | Nima o'zgaradi |
|---|---|
| §4.10 | Pullik kontent HLS orqali sizib chiqmaydi — playlist proksi + imzolangan segment |
| §4.13 | Server tayyorligi tekshiruvi, disk monitoringi, panelda navbat banneri, 4K ogohlantirishi |
| Mobil | Sessiya endi 15 daqiqada tugamaydi — refresh token oqimi |
| Tozalash | Muddati o'tgan refresh tokenlar har kuni 03:15 da o'chiriladi |
| Panel | Yo'riqnoma sahifasi, qidiruvli selectlar, transcoding nishonlari |

**Testlar:** 1033 backend · 121 frontend · 14 mobil — hammasi yashil.

**Jar lokalda ishga tushirib sinaldi:** panel ochildi, `/api/v1/app/auth/refresh`
va HLS endpointlari javob berdi, navbat endpointi yangi `system` blokini
qaytardi.

---

## ⚠️ 1. Eng muhim tuzoq — frontend jar ICHIDA

Panel alohida joylashtirilmaydi. `WebMvcConfig` uni `classpath:/static/`
dan beradi, ya'ni **frontend jar ichiga kiradi**.

Bu qadam unutilsa deploy jimgina eskilanadi: backend yangi, panel eski.
Hech qanday xato chiqmaydi — shunchaki yangi sahifalar yo'q bo'ladi.

Aynan shu holat tayyorlov paytida topildi: jarda `main.4a14df90.js`
turgan edi, yangi build esa `main.7107705a.js`.

```bash
# Repozitoriy ILDIZIDAN
cd frontend && CI=false npx react-scripts build && cd ..

# Eskisini butunlay almashtiramiz — qoldiq fayllar qolmasin
rm -rf backend/src/main/resources/static
cp -R frontend/build backend/src/main/resources/static

# TEKSHIRUV: ikkalasi bir xil bo'lishi shart
grep -o 'main\.[a-z0-9]*\.js' frontend/build/index.html | head -1
grep -o 'main\.[a-z0-9]*\.js' backend/src/main/resources/static/index.html | head -1
```

---

## 2. Jar yig'ish

```bash
./backend/mvnw -f backend/pom.xml clean package
```

⚠️ `-DskipTests` **qo'ymang**. Testlar aynan shu relizdagi xavfli
joylarni qo'riqlaydi (pullik kontent sizishi, token rotatsiyasi).

Natija: `backend/target/backend-0.0.1-SNAPSHOT.jar` (~107 MB).

```bash
# Panel jar ichiga tushganini TEKSHIRING
unzip -p backend/target/backend-0.0.1-SNAPSHOT.jar \
  BOOT-INF/classes/static/index.html | grep -o 'main\.[a-z0-9]*\.js' | head -1
```

---

## 3. Serverda — chiqarishdan OLDIN

### 3.1. Baza zaxirasi

```bash
pg_dump -U <foydalanuvchi> casting | gzip > ~/casting-$(date +%F-%H%M).sql.gz
ls -lh ~/casting-*.sql.gz
```

⚠️ Migratsiyalar qaytarilmaydi. Zaxira — yagona orqaga yo'l.

### 3.2. Qaysi migratsiyalar allaqachon qo'llangan

```sql
select version, description, success, installed_on
from flyway_schema_history order by installed_rank desc limit 6;
```

Bu relizda **V27** va **V28** yangi. Ikkalasi ham faqat qo'shadi:

| Migratsiya | Nima qiladi |
|---|---|
| V27 | `cms_upload_session` ga 3 ta ustun (`upload_mode`, `s3_upload_id`, `storage_key`) |
| V28 | `media_asset` ga 3 ta ustun + yangi `cms_transcoding_job` jadvali |

`drop` ham, `delete` ham yo'q. Eski jar bu ustunlar bilan ham ishlayveradi.

⚠️ Ro'yxatda **V25** yoki **V26** ko'rinmasa — deploy o'ylanganidan
eskiroq. Bu holda to'xtang va menga ayting: bir necha migratsiya
birdan qo'llanadi va ularni alohida ko'rib chiqish kerak.

### 3.3. Hozirgi jar zaxirasi

```bash
cp /opt/uzcasting/backend.jar ~/backend-$(date +%F-%H%M).jar.bak
```

> Yo'l sizda boshqacha bo'lishi mumkin — qaysi jar ishlayotganini
> `systemctl cat uzcasting` yoki `ps aux | grep java` ko'rsatadi.

---

## 4. FFmpeg — QAROR TALAB QILINADI

Bu relizda transcoding worker **yoqilgan** (kod sukuti). U har 15
soniyada navbatni tekshiradi va yangi yuklangan videoni HLS'ga
o'girishga urinadi.

```bash
ffmpeg -version && ffprobe -version
ffmpeg -hide_banner -encoders | grep -E ' (libx264|aac) '
```

**Ikkalasi ham chiqsa** — hech narsa qilish kerak emas.

**Chiqmasa** — ikkita yo'l bor:

```bash
# a) O'rnatish (video HLS'ga o'giriladi, CDN'ga tayyorlanadi)
sudo apt update && sudo apt install -y ffmpeg

# b) Yoki worker'ni o'chirish (video eski yo'lda ishlayveradi)
#    application.properties ga:
app.video.worker-enabled=false
```

⚠️ Ikkalasi ham qilinmasa halokat bo'lmaydi: yangi videolar
`FAILED` bo'ladi, lekin **ular baribir `/raw` orqali ko'rinadi**.
Panelda «Server video qayta ishlashga tayyor emas» banneri chiqadi —
u aynan shu holat uchun yozilgan.

---

## 5. Sozlamalar — YANGI MAJBURIY o'zgaruvchi YO'Q

`application.properties` serverda turadi va repozitoriyga kirmaydi.
Bu relizdagi barcha yangi sozlamalarning **kodda sukut qiymati bor**,
ya'ni faylga tegmasangiz ham ilova ko'tariladi.

Ixtiyoriy:

| Sozlama | Sukut | Qachon kerak |
|---|---|---|
| `app.video.worker-enabled` | `true` | FFmpeg yo'q bo'lsa `false` qiling |
| `app.video.max-concurrent-jobs` | `1` | 12 yadroli serverda `3` |
| `app.video.min-free-disk` | `10GB` | Disk kichik bo'lsa oshiring |
| `app.storage.provider` | `local` | S3 olingach `s3` |
| `app.auth.refresh-cleanup-cron` | `0 15 3 * * *` | Boshqa vaqt kerak bo'lsa |
| `app.taxonomy.bootstrap` | `true` | Janr/kategoriya katalogi qo'lda kiritilsa `false` |
| `app.upload.chunk-size-bytes` | `5242880` (5 MB) | Proxy chegarasini oshirib bo'lmasa kichraytiring |

Katalog haqida: birinchi ishga tushishda kategoriya va janr ro'yxati
avtomatik bazaga yoziladi (`TaxonomyCatalog` — 13 kategoriya, 56 janr,
uch tilda). Usiz toza bazada janr ro'yxati bo'sh bo'lardi va admin har
bir janrni qo'lda, UZ/RU/EN da yozib chiqishi kerak bo'lardi.

⚠️ Bir marta ko'chirilgan versiya QAYTA ko'chirilmaydi: admin panelda
o'chirgan janr serverni qayta ishga tushirgach tiklanmaydi, mavjud
satrlarning nomi va tartibi esa hech qachon ustidan yozilmaydi.

⚠️ S3 va CDN **hali sozlanmaydi** — Object Storage sotib olinmagan.
Sozlanmaguncha HLS himoyasi (§4.10) kutib turadi va video eski yo'lda
beriladi. Bu regressiya emas: hozir ham shunday.

### 5.1. ⚠️ PROXY — video yuklash uchun MAJBURIY

8 MB dan katta fayl panelga **bo'laklab** yuklanadi: har bo'lak
`PUT /api/v1/app/admin/uploads/{id}/chunks/{n}` bilan xom tanada
ketadi, hajmi — 5 MB.

Server oldidagi nginx yoki Apache tana hajmini chegaralaydi va bo'lak
undan katta bo'lsa so'rov **ilovaga yetib bormaydi**: panel
«Xatolik (413)» ko'rsatadi, server log'ida esa hech narsa
bo'lmaydi — aynan shu narsa sababni topishni qiyinlashtiradi.
Rasm (odatda 8 MB dan kichik) bitta so'rov bilan ketadi va
ishlayveradi, ya'ni «rasm bo'ladi, video bo'lmaydi» degan chalg'ituvchi
manzara chiqadi.

nginx:

```nginx
location /api/ {
    client_max_body_size 64m;   # sukut — 1m, bo'lak esa 5m
    proxy_read_timeout 600s;    # katta faylni yig'ish uzoq davom etadi
    proxy_request_buffering off;
    proxy_pass http://127.0.0.1:8080;
}
```

Apache:

```apache
LimitRequestBody 67108864
ProxyTimeout 600
```

Tekshirish (401 kutiladi — ya'ni so'rov ilovaga yetdi; 413 bo'lsa
proxy hali ham to'sib turibdi):

```bash
head -c 5242880 /dev/urandom > chunk.bin
curl -s -o /dev/null -w "%{http_code}
" -X PUT --data-binary @chunk.bin   -H "Content-Type: application/octet-stream"   https://uzcasting.site/api/v1/app/admin/uploads/fake/chunks/0
```

Proxy sozlamasiga tegib bo'lmasa: `app.upload.chunk-size-bytes` ni
chegaradan kichik qilib qo'ying (masalan `1048576`) va ilovani qayta
ishga tushiring — qayta build qilish shart emas.

---

## 6. Chiqarish

```bash
# 1. Yangi jar
scp backend/target/backend-0.0.1-SNAPSHOT.jar \
    <user>@uzcasting.site:/opt/uzcasting/backend.jar.new

# 2. Serverda: to'xtatish → almashtirish → ishga tushirish
sudo systemctl stop uzcasting
mv /opt/uzcasting/backend.jar.new /opt/uzcasting/backend.jar
sudo systemctl start uzcasting

# 3. Migratsiyalar birinchi ishga tushishda O'ZI qo'llanadi
sudo journalctl -u uzcasting -f
```

⚠️ Ishlab turgan jar ustidan yozmang. Fat-jar klasslarni **ishlash
davomida** o'qiydi va ustidan yozish ilovani tushunarsiz xatolar bilan
yiqitadi. Shuning uchun avval `stop`, keyin `mv`.

---

## 7. Chiqargandan keyin — TEKSHIRUV

### 7.1. Loglarda bitta qator izlang

```
Video transcoding tayyor: ffmpeg version … · ffprobe version … · diskda … GB bo'sh
```

Yoki FFmpeg yo'q bo'lsa:

```
⚠️ Video transcoding ISHLAMAYDI. Sayt ishlaydi, videolar eski yo'l bilan …
```

Ikkalasi ham normal — muhimi, holat **aytilgan** bo'lsin.

### 7.2. Migratsiyalar

```sql
select version, success from flyway_schema_history
where version in ('27','28');
```

Ikkalasi ham `success = true` bo'lishi shart.

### 7.3. Endpointlar

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://uzcasting.site/api/v1/app/home
# kutilgan: 200

curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  https://uzcasting.site/api/v1/app/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refresh_token":"soxta"}'
# kutilgan: 401  (404 bo'lsa — endpoint yo'q, eski jar ketgan)
```

### 7.4. Panel

Brauzerda `https://uzcasting.site/app/panel` ni oching va tekshiring:

- chap menyuda **«Yo'riqnoma»** sahifasi bor;
- media kutubxonasida videolarda transcoding nishoni ko'rinadi;
- FFmpeg yo'q bo'lsa — yuqorida qizil banner.

Uchalasi ham ko'rinmasa — panel eski, ya'ni **1-qadam o'tkazib
yuborilgan**.

### 7.5. Eski mijozlar buzilmaganini

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://uzcasting.site/api/v1/news
# kutilgan: 200 — Telegram bot va eski admin sayti shu yo'llardan yuradi
```

---

## 8. Orqaga qaytarish

```bash
sudo systemctl stop uzcasting
cp ~/backend-<sana>.jar.bak /opt/uzcasting/backend.jar
sudo systemctl start uzcasting
```

⚠️ **Bazani qaytarish SHART EMAS.** V27 va V28 faqat ustun qo'shadi,
eski jar ularni e'tiborsiz qoldiradi (`ddl-auto=none`). Baza
zaxirasi faqat kutilmagan holat uchun.

---

## 9. Mobil ilova — ALOHIDA reliz

Serverga chiqarish mobil ilovani yangilamaydi. Refresh token oqimi
foydalanuvchida faqat **yangi ilova versiyasi chiqqach** ishlaydi.

⚠️ Backend o'zgarishi eski ilova uchun xavfsiz: u `refresh_token` ni
oladi va e'tiborsiz qoldiradi — ya'ni ilgarigidek ishlaydi, yaxshi
ham bo'lmaydi, yomon ham.

---

## 10. Chiqargandan keyin kuzatiladigan narsalar

| Nima | Qayerda | Nega |
|---|---|---|
| `refresh_token` jadvali hajmi | `select count(*) from refresh_token;` | Endi har kirish va har yangilash qator yozadi. Tozalash har kuni 03:15 da ishlaydi — birinchi kundan keyin son barqarorlashishi kerak |
| Diskda bo'sh joy | Panel banneri yoki `df -h` | Transcoding vaqtinchalik fayllar yozadi |
| `FAILED` transcoding ishlari | Panel → Media → «Faqat yiqilganlar» | FFmpeg holatini ko'rsatadi |

---

## 11. Keyingi safar uchun

`frontend/build` ni `static/` ga ko'chirish **qo'lda** bajariladi va
aynan shu qadam unutilishi oson. Uni Maven'ga bog'lash mumkin, lekin
u holda `frontend/build` bo'lmagan muhitda (CI, toza klon) build
yiqilardi — shuning uchun hozircha hujjatda qoldirildi.
