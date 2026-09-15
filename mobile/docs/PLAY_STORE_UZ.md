# Google Play'ga chiqarish — bosqichma-bosqich

> Hisob turi: **tashkilot (Organization)**, **buyurtmachi nomiga** rasmiylashtiriladi.
> Shu faylning ruscha varianti — [PLAY_STORE_RU.md](./PLAY_STORE_RU.md).
>
> Oxirgi yangilanish: 14.09.2026

Belgilar: **[MEN]** — men koddа va EAS'da bajaraman. **[SIZ]** — brauzerda
qo'lda bajariladi. **[BUYURTMACHI]** — faqat hisob egasi bajara oladi.
**[BEK]** — backend bo'yicha hamkasb vazifasi.

---

## 0. Hozirgi holat

### Tayyor bo'lgan narsalar

| Nima | Holati |
|---|---|
| Build formati | EAS `production` profili `.aab` yig'adi — Play faqat shuni qabul qiladi |
| Package name | `uz.uzcasting.app` — birinchi yuklashdan keyin hech qachon o'zgarmaydi |
| Versiya | `1.0.0`, `versionCode`ni EAS yuritadi (`appVersionSource: "remote"`, `autoIncrement: true`) |
| Imzo kaliti | EAS'da saqlanadi, kompyuter bilan birga yo'qolmaydi |
| Target API | Expo SDK 57 → API 36. Play talabi (31.08.2026 dan yangi ilovalar uchun) — aynan API 36 |
| 16 KB page size | 01.11.2025 dan yangi ilovalar uchun talab, Expo SDK 57 unga mos |
| Ruxsatlar | bittagina `INTERNET` qoldirildi (13.09.2026, §2.1) |
| Maxfiylik siyosati | https://uzcasting.com/maxfiylik — 200 qaytaradi |
| Foydalanuvchi kelishuvi | https://uzcasting.com/kelishuv — 200 qaytaradi |
| 512×512 ikonka | `store/play/play-icon-512.png` — tayyorladim |
| 1024×500 grafika | `store/play/feature-graphic-1024x500.png` — tayyorladim |
| To'lovlar | ilovada ishlaydigan to'lov tugmasi yo'q → Google Play Billing **talab qilinmaydi** (kelajak variantlari tahlili — §15) |

### Nashrni to'sib turgan narsalar

Yettita band, bittasi allaqachon hal qilindi. Qolganlari — bu shunchaki «izoh»
emas, bu rad javobi yoki tirik foydalanuvchilarda ishlamaydigan kirish.

| # | To'siq | Kim hal qiladi | Batafsil |
|---|---|---|---|
| 1 | ⚠️ Hisobni o'chirish — 13.09.2026 da yopilgan edi, **15.09.2026 da qayta ochildi**: ilovadagi ekran buyurtmachi so'rovi bilan olib tashlandi. Endpoint va sahifa bor | **[SIZ]** | §6 |
| ~~2~~ | ~~Google Cloud OAuth `Testing` holatida~~ — **13.09.2026 da hal qilindi**, holat `In production` | — | §7 |
| 3 | Play App Signing kalitining SHA-1'i Google Android-klientiga yozilmagan | **[SIZ]** | §9 |
| 4 | Demo-kirish: **kod 13.09.2026 da tayyor**, yuborishdan oldin prodda yoqiladi | **[SIZ]** | §5.4 |
| 5 | Skrinshotlar yo'q | **[SIZ]** | §8.3 |
| 6 | Play Console hisobi yo'q | **[BUYURTMACHI]** | §1 |
| ~~7~~ | ~~Izohga shikoyat~~ — **13.09.2026 da yopildi**: endpoint va lentadagi tugma | — | §6a |

---

## 1. A bosqichi — dasturchi hisobi [BUYURTMACHI]

Kalendar bo'yicha eng uzoq bosqich: ish ko'pligidan emas, Google tomonidagi
tekshiruvlar sababli. Shuni birinchi boshlash kerak, qolgani parallel ketadi.

### 1.1. Ro'yxatdan o'tish

1. **https://play.google.com/console/signup** ni ochish
2. Buyurtmachining Google hisobi bilan kirish.
   ⚠️ **Xodimning shaxsiy pochtasi emas, kompaniyaning ish pochtasi.** Hisob
   egasini boshqa odamga o'zgartirib bo'lmaydi, xodim esa ishdan ketishi mumkin.
3. «Select account type» qadamida **Organization** ni tanlash («Yourself» emas).
   ⚠️ **Bu tanlov bir marta qilinadi va keyin o'zgarmaydi.** Shaxsiy hisob
   yopiq test talab qilardi: kamida 12 ta tester, uzluksiz 14 kun, undan keyin
   productionga ariza. Tashkilotlar bundan ozod — biz Organization tanlaganimiz
   sababi ham shu.
4. **$25** ro'yxat yig'imini to'lash (bir marta, har yili emas).
   ⚠️ Karta xalqaro bo'lishi kerak — chet elda to'lovga ruxsat berilgan
   Visa/Mastercard. UzCard/Humo o'tmaydi.

### 1.2. Tashkilotni tasdiqlash

Google so'raydi (O'zbekiston uchun — Google'ning rasmiy yordamiga ko'ra):

- **kompaniyaning ro'yxat hujjati** — quyidagilardan biri: ro'yxatdan o'tganlik
  guvohnomasi, reestrdan ko'chirma, litsenziya, soliq sertifikati;
- **vakilning shaxsini tasdiqlovchi hujjati** — pasport, ID-karta, haydovchilik
  guvohnomasi yoki yashash uchun ruxsatnoma;
- tashkilotning **D-U-N-S raqami**.

⚠️ **D-U-N-S ni birinchi kuniyoq buyurtma qiling.** U bepul, lekin 30 kungacha
berilishi mumkin. Ariza: **https://www.dnb.com/duns/get-a-duns.html** →
«Get a D-U-N-S Number». Arizadagi kompaniya nomi va manzili Play Console'ga
yoziladigan ma'lumot bilan **harfma-harf** bir xil bo'lishi shart, aks holda
Google ularni moslay olmay, qaytarib yuboradi.

Tekshiruv holati: **https://play.google.com/console** → chap past burchakda
**Settings → Developer account → Account details**.

### 1.3. Sizga va menga ruxsat berish [BUYURTMACHI]

1. https://play.google.com/console → chap pastda **Users and permissions**
2. O'ng yuqoridagi **Invite new users** tugmasi
3. Email → **App permissions** blokida ilovani tanlab (§4 dan keyin paydo
   bo'ladi), belgilash:
   - `View app information and download bulk reports`
   - `Manage store presence`
   - `Manage testing tracks`, `Release to testing tracks`
   - **`Release to production` — faqat sizga**
4. **Invite user**

⚠️ `Admin (all permissions)` rolini egasidan boshqa hech kimga bermang: u bilan
ilovani butunlay o'chirib yuborish mumkin.

---

## 2. B bosqichi — men loyihada nima qildim [MEN]

### 2.1. Ruxsatlarni qisqartirdim

Ilgari (Expo o'zi qo'shar edi):

```
android.permission.READ_EXTERNAL_STORAGE
android.permission.WRITE_EXTERNAL_STORAGE
android.permission.INTERNET
```

Endi faqat `INTERNET`. `app.json` ga qo'shildi:

```json
"blockedPermissions": [
  "android.permission.READ_EXTERNAL_STORAGE",
  "android.permission.WRITE_EXTERNAL_STORAGE"
]
```

Nega: ilova foydalanuvchi fayllarini o'qimaydi ham, yozmaydi ham — kodda na
kamera, na fayl tanlash bor. Manifestdagi ortiqcha ruxsat — bu tekshiruvda
ortiqcha savol va Data safety anketasi bilan ziddiyat (u yerda biz halol
tarzda «fayllarni yig'maymiz» deb javob beramiz).

Tekshirish:

```bash
cd mobile
npx expo config --type prebuild --json | grep -o '"permissions":\[[^]]*\]'
# → ["android.permission.INTERNET"]
```

⚠️ Bu nativ o'zgarish: u **faqat yangi build bilan** yetib boradi, `eas update`
orqali emas.

### 2.2. Play'ga yuborish profilini sozladim

`eas.json` da yangi blok:

```json
"submit": {
  "production": {
    "android": {
      "serviceAccountKeyPath": "./play-service-account.json",
      "track": "internal",
      "releaseStatus": "draft",
      "changesNotSentForReview": false
    }
  }
}
```

Shu tufayli buildni fayl sifatida qo'lda yuklash o'rniga `eas submit` buyrug'i
bilan yuborsa bo'ladi. `play-service-account.json` kalitini §3 da olasiz; u
`.gitignore` ga qo'shilgan — **hech qachon repozitoriyga tushmasin**, bu
relizlarga kirish huquqi.

### 2.3. Grafikani tayyorladim

| Fayl | O'lcham | Qayerga |
|---|---|---|
| `mobile/store/play/play-icon-512.png` | 512×512 | Store listing → App icon |
| `mobile/store/play/feature-graphic-1024x500.png` | 1024×500 | Store listing → Feature graphic |

Ikonka shaffoflikssiz, brend foni `#05050A` ustiga tushirildi: Play ikonkada
alfa-kanalni qabul qilmaydi va uni o'zi to'ldiradi, natijasi esa kutilmagan
bo'lishi mumkin. Feature graphic — bazaviy variant (brend foni ustida markazda
logotip). Buyurtmachida dizayner bo'lsa, uning varianti shu nom bilan ustiga
qo'yiladi.

### 2.4. Matnlar va anketa javoblarini tayyorladim

- `mobile/store/play/LISTING.md` — nom, qisqa va to'liq tavsif, «nima yangilik»
  matni;
- `mobile/store/play/FORMS.md` — **App content** bo'limidagi barcha anketalar
  uchun tayyor javoblar (Data safety, Content rating, App access, Ads, Target
  audience).

⚠️ **Play listingida o'zbek tili yo'q.** Qo'llab-quvvatlanadigan tillar rasmiy
jadvalidan tekshirildi: `uz` u yerda yo'q, `ru` va `en-US` bor. Ilova interfeysi
o'zbekcha qolaveradi, do'kondagi tavsif esa ruscha (asosiy til) va inglizcha
(tekshiruvchi uchun) bo'ladi. O'zbek ilovalari uchun bu odatiy holat.

---

## 3. C bosqichi — buildlarni yuklash uchun servis hisobi [SIZ]

Bir marta qilinadi. Usiz har bir buildni qo'lda yuklab olib, qo'lda yuklash
kerak bo'ladi.

1. **Play Console → Setup → API access**:
   https://play.google.com/console → chapda **Setup** → **API access**
2. **Choose a project to link** → **Create new project**. Yoki Google-kirish
   allaqachon joylashgan `497193534365` loyihasini ulash — shunda hammasi bir
   joyda bo'ladi.
3. **Service accounts** bloki → **Create new service account**. Google Cloud'ga
   havola chiqadi, o'sha havola bilan o'tish:
   https://console.cloud.google.com/iam-admin/serviceaccounts
4. **Create service account**:
   - Service account name: `play-publisher`
   - **Create and continue** → Google Cloud'da rol bermasa ham bo'ladi
     (huquqlar Play Console'da beriladi) → **Done**
5. Ro'yxatdan yaratilgan hisobni bosish → **Keys** bo'limi → **Add key** →
   **Create new key** → turi **JSON** → **Create**. Fayl o'zi yuklanadi.
6. Faylni `play-service-account.json` deb nomlab, `eas.json` yonidagi `mobile/`
   papkasiga qo'yish.
   ⚠️ Commit qilmang. U `.gitignore` da, lekin commitdan oldin `git status` ga
   qarab qo'ying.
7. **Play Console → Setup → API access** ga qaytib, **Refresh service accounts**
   → yangi hisobda **Manage Play Console permissions** → `Release to testing
   tracks`, `Release apps to production`, `Manage store presence` ni belgilash →
   **Invite user**.

Tekshirish (birinchi build bo'lganda men ishga tushiraman):

```bash
cd mobile
export EXPO_TOKEN=$(grep '^EXPO_TOKEN=' .env.local | cut -d= -f2)
eas submit -p android --profile production --latest
```

---

## 4. D bosqichi — konsolda ilova yaratish [SIZ]

1. https://play.google.com/console → **All apps** → o'ng yuqorida **Create app**
2. To'ldirish:

| Maydon | Qiymat |
|---|---|
| App name | `UzCasting` (30 belgigacha) |
| Default language | **Russian (ru)** — ro'yxatda o'zbekcha yo'q, §2.4 ga qarang |
| App or game | **App** |
| Free or paid | **Free** |

3. Ikkala deklaratsiyani belgilash (Developer Program Policies, US export laws)
   → **Create app**

⚠️ **Free → Paid keyinchalik o'zgartirilmaydi.** Ilova bepul: ichidagi obunalar
alohida mexanizm, ilovaning o'z narxiga aloqasi yo'q.

Yaratilgandan keyin «Set up your app» vazifalari ro'yxati bilan **Dashboard**
ochiladi — keyin aynan shu ro'yxat bo'yicha yuramiz.

---

## 5. E bosqichi — «App content» bo'limi [SIZ]

Yo'l: ilovani tanlab → chapda **Policy and programs → App content**.

Tayyor javoblar `mobile/store/play/FORMS.md` da — bu yerda tartib va tuzoqlar.

### 5.1. Privacy policy

`https://uzcasting.com/maxfiylik` ni qo'yish → **Save**.

⚠️ **13.09.2026 da siyosat matniga ikkita band qo'shildi** — izohlar va qurilma
identifikatori. Ilgari ilova ularni yig'ardi, siyosat esa jim edi; Data safety
formasi buni e'lon qiladi va nomuvofiqlikni Google avtomatik topadi. O'zgarish
`backend/src/main/resources/legal/maxfiylik.html` da va **saytda faqat backend
deploy qilingandan keyin** ko'rinadi — tekshiruvga yuborishdan oldin
https://uzcasting.com/maxfiylik da shu qatorlar borligini tekshiring.

⚠️ Havola **login talab qilmasdan va boshqa domenga yo'naltirmasdan** ochilishi
kerak. 13.09.2026 da tekshirildi: 200 qaytaradi.

### 5.2. Ads

Javob: **Yes, my app contains ads**.

⚠️ Bu yerda o'z foydamizga xato qilish oson. Ilovada reklama tarmog'i (AdMob)
yo'q, lekin butun ekranni yopadigan «Majburiy reklama» banneri va lentadagi
reklama kartalari bizning backenddan keladi va tijorat reklamasi hisoblanadi.
Bannerlar tirik turib «yo'q» deb javob berish — birinchi tekshiruvdayoq ilovani
olib tashlashga olib keladi.

Natijasi: kartochkada «Reklama mavjud» belgisi chiqadi. Bu kutilgan narsa.

### 5.3. App access

**All or some functionality is restricted** ni tanlash — kirmasdan ilova hech
narsa ko'rsatmaydi.

**Add new instructions**, maydonlar **ingliz tilida** to'ldiriladi (tekshiruv
inglizchada o'qiydi):

| Maydon | Nima yoziladi |
|---|---|
| Name | `Demo account (phone OTP)` |
| Username | demo telefon raqami |
| Password | o'zgarmas tasdiqlash kodi |
| Any other instructions | onboardingdan qanday o'tish va qayerga bosish |

Tayyor inglizcha matn — `FORMS.md` da.

### 5.4. ⚠️ Demo-kirish: usiz tekshiruv to'xtaydi [BEK]

Ilovaga kirish — o'zbek raqamiga keladigan SMS kod orqali. Google tekshiruvchisi
boshqa mamlakatda o'tiradi va SMS ololmaydi. Prodda **o'zgarmas test raqami**
kerak: server u uchun oldindan ma'lum kodni qabul qiladi (masalan
`+998 90 000 00 00` → `0000`) va haqiqiy SMS yubormaydi.

### ✅ 13.09.2026 da bajarildi — lekin prodda o'chiq

Mexanizm `OtpService` da: bitta raqamga SMS yuborilmaydi, kod esa o'zgarmas.
**Sukut bo'yicha o'chiq** — faqat ikkala sozlama ham berilganda ishlaydi:

```properties
app.otp.demo.phone=+998901234567
app.otp.demo.code=0000
```

Tekshiruvchi uchun bu shunday ko'rinadi: kirish ekraniga raqamni teradi,
odatdagi SMS kodi maydonini oladi, `0000` yozadi va ilovaga kiradi. SMS
umuman yuborilmaydi. 13.09.2026 da local profilda tekshirildi: `send` →
`{"sent":true}` va logda SMS yo'q, `0000` bilan `verify` → sessiya, boshqa
kod bilan → `422`, ikkinchi kirishda ism so'ralmaydi.

⚠️ **Prodda BOSHQA raqam va kod oling.** Yuqoridagi juftlik
`application-local.properties` va `application-dev.properties` da, ya'ni
repozitoriyda turadi — u bilan GitHub'ni ochgan har kim productionga
Premium hisob bilan kirardi. Jangovar qiymatlar faqat
`/opt/uzcasting/application.properties` da va App access maydonida.

⚠️ **Kod aynan 4 xonali** — ilovadagi maydon 4 katakli, olti xonalisini
terib bo'lmaydi. Test buni qulflab qo'ygan.

⚠️ **Demo hisobga Premium beriladi** (`app.otp.demo.premium-days`, sukut
bo'yicha 90 kun): katalogning bir qismi obuna ostida, usiz tekshiruvchi
ilovaning yarmini ko'rmasdi va ustiga ishlamaydigan to'lov tugmasiga
urilardi. Faqat shu raqamga va faqat muddat tugayotganda.

⚠️ Raqam va kodni o'zingiz tanlang, bular MISOL. Tekshiruvga yuborishdan
oldin yoqing, **tekshiruvdan keyin o'chiring**. Yoqilganda logga `WARN`
yoziladi — sozlama prodda e'tibordan chetda qolmasin.

`DemoPhoneOtpTest` eng muhimini alohida tekshiradi: demo kod **boshqa
raqamlarda ishlamaydi**.

Backenddan aynan nima kerakligi —
[roadmap/PLAY_BACKEND_TASKS.md](../../roadmap/PLAY_BACKEND_TASKS.md) §3.

Zaxira variant, agar backend ulgurmasa: yo'riqnomada kirish **Sign in with
Google** tugmasi orqali istalgan hisob bilan amalga oshiriladi deb yozish. U
holda §7 (OAuth loyihasini nashr qilish) **yuborishdan oldin qat'iy majburiy**
bo'lib qoladi — aks holda tekshiruvchining Google hisobi «ruxsat rad etildi»
oladi va bu tashqaridan buzilgan ilovaga o'xshaydi.

### 5.5. Content rating

**Start questionnaire** → `uzcasting.org@gmail.com` → kategoriya **Entertainment**.

⚠️ Javoblar **haqiqiy katalog** bo'yicha bo'lsin, niyat bo'yicha emas.
Seriallarda zo'ravonlik yoki kattalar uchun sahnalar bo'lsa — shunday deb
aytish kerak. Reyting kontentga mos kelmasligi — olib tashlash uchun alohida
asos, va u keyinroq, foydalanuvchi shikoyati bilan chiqadi.

⚠️ **10.09.2026 dan anketada yangi javob paydo bo'ldi.** Foydalanuvchilarning
o'zaro muloqoti va kontent almashinuvi haqidagi savolga endi **«Ha»**: ilovada
izohlar bor. Ilgari u yerda halol tarzda «yo'q» turardi — izohlar bo'limi
paydo bo'lgach, bu javob noto'g'ri bo'lib qoldi (§6a).

Anketani istalgan vaqtda qayta to'ldirsa bo'ladi, reyting avtomatik qayta
hisoblanadi.

### 5.6. Target audience and content

Yosh guruhlari: **18 and over** (yoki §5.5 natijasiga ko'ra 16+ — lekin 13 dan
past emas, aks holda Families policy talablari yoqiladi va hammasi bir necha
barobar murakkablashadi).

«Does your app appeal to children?» → **No**.

### 5.7. Data safety

Eng uzun anketa. Tayyor jadval — `FORMS.md` da. Qisqacha:

| Yig'amiz | Nima uchun | Foydalanuvchi uchun majburiymi |
|---|---|---|
| Ism | profil | ha |
| Telefon raqami | kirish | ha |
| Email (Google orqali kirganda) | kirish | ha |
| Ilovadagi harakatlar (ko'rishlar, reklama bosishlari) | analitika | yo'q |
| Kontentga izohlar | nashr qilish | yo'q |

Hamma joyda: **ma'lumot uzatishda shifrlanadi** (butun trafik uzcasting.com ga
HTTPS orqali), **ma'lumot uchinchi shaxslarga sotilmaydi**, **foydalanuvchi
o'chirishni so'rashi mumkin** — oxirgisi uchun §6 kerak.

### 5.8. Qolgan anketalar

- **Financial features** → `My app doesn't provide any financial features`
- **Health apps**, **News apps**, **Government apps** → `No`
- **Data deletion** → §6 ga qarang

---

## 6. ⚠️ To'siq: hisobni o'chirish [BEK] + [MEN]

Google siyosati: ilovada **hisob yaratish** mumkin bo'lsa, uni o'chirish ham
bo'lishi shart — **ikki yo'l bilan**:

1. **ilova ichida** — interfeysda, bir-ikki ekrandan uzoq bo'lmagan yo'l;
2. **veb-havola orqali** — ilovani o'rnatmasdan ochiladigan; u App content →
   **Data deletion** ga yoziladi.

Hozir ikkalasi ham yo'q: `app/settings/profile.tsx` da hisobni o'chirish
mavjud emas.

### ✅ Backendda bajarildi (13.09.2026)

| Nima | Qayerda |
|---|---|
| `DELETE /api/v1/app/me` | `AppProfileController` + `AccountDeletionService` |
| `GET /hisobni-ochirish` sahifasi | `LegalPageController`, HTML `resources/legal/` da |
| `V38` migratsiyasi | `cms_user_account.deleted_at`, `DELETED` holati |
| Testlar | `AccountDeletionTest` — 7 ta, mutatsiya bilan tekshirilgan |

Nima sodir bo'ladi: foydalanuvchi qatori **o'chirilmaydi** (unga to'lovlar
bog'langan), lekin telefon, email, Google sub, ism va rasm tozalanadi,
sessiyalar bekor qilinadi, qurilmalar/saqlanganlar/ko'rish joyi o'chadi.
Izohlar qoladi, muallif «O'chirilgan foydalanuvchi» bo'ladi. **Telefon
bo'shaydi** — u bilan qaytadan ro'yxatdan o'tish mumkin.

### ⚠️ Ilovadagi ekran 15.09.2026 da OLIB TASHLANDI

Buyurtmachi: «hisobni o'chirish degani butunlay olib tashla profildan
umuman kerak emas bu». Profildagi qator, `app/settings/delete-account.tsx`,
`useDeleteAccount` va `settings.delete*` matnlari o'chirildi. Backenddagi
`DELETE /api/v1/app/me` va `/hisobni-ochirish` sahifasi joyida.

Oqibati: Google siyosatining **1-yo'li** (ilova ichida) yo'q, faqat veb-havola
qoldi. Play'ga yuborishda bu rad javobiga sabab bo'lishi mumkin. Qaytarish —
git tarixidan, quyida o'sha ekranning tavsifi.

### Olib tashlangan ekran (13.09.2026 — 15.09.2026)

`Profil → Hisobni o'chirish` («Chiqish» yonida, qizil). Ekran nima
o'chirilishi va nima qolishini tushuntiradi, tasdiqlash — tizim oynasi.
Sessiya **faqat server javobidan keyin** o'chiriladi: aks holda aloqa
uzilsa, odam hisobi butun turib, kirishdan mahrum bo'lardi. Testlar:
`deleteAccount.test.tsx`.

⚠️ O'chirishda parol ham, SMS kod ham **ataylab so'ralmaydi** — siyosat
o'chirish oson bo'lishini talab qiladi, ortiqcha to'siqlar rad javobiga
sabab bo'ladi.

Qoldi:

| Kim | Nima |
|---|---|
| **[BEK]** | `V38` migratsiyasi bilan deploy — shusiz saytda sahifa yo'q |

Hamkasb uchun endpointlarning aniq shartnomasi alohida yozilgan:
[roadmap/PLAY_BACKEND_TASKS.md](../../roadmap/PLAY_BACKEND_TASKS.md) §1.

Klient qismini endpoint paydo bo'lishi bilanoq qilaman. Tugmani havo orqali
yangilanish bilan ham yetkazsa bo'ladi, lekin **tekshiruvga yuborish paytida u
buildda bo'lishi shart**.

---

## 6a. ⚠️ To'siq: izohlar = foydalanuvchi kontenti [BEK] + [MEN]

10.09.2026 da izohlar bo'limi bilan birga paydo bo'ldi
(`src/features/comments/`). Google uchun bu **UGC — user-generated content**,
va bunday ilovalarga alohida siyosat qo'llanadi.

Siyosat nimani talab qiladi — va bizda nima bor:

| Talab | Bormi | Izoh |
|---|---|---|
| Kontent moderatsiyasi | ✅ | moderator izohni yashiradi, muallifga u «yashirilgan» ko'rinadi |
| Qoidabuzarlarni bloklash | ✅ admin panel tomonida | bloklangan foydalanuvchi yuborishda `403` oladi |
| O'z kontentini o'chirish | ✅ | yumshoq o'chirish, moderatsiya uchun yozuv qoladi |
| **Begona izohga «Shikoyat qilish» tugmasi** | ❌ | interfeysda yo'q |
| **Muallifni o'zi uchun yashirish imkoni** | ❌ | interfeysda yo'q |

Oxirgi ikki bandni Google to'g'ridan-to'g'ri tekshiradi: izohlar ro'yxatini
ochib, shikoyat qilish yo'lini qidiradi. Topmasa — User Generated Content
siyosatiga havola bilan rad javobi.

### ✅ Backendda bajarildi (13.09.2026)

`POST /api/v1/app/comments/{id}/report` sabab bilan (`SPAM`, `INSULT`,
`ADULT`, `OTHER`), `cms_comment_report` jadvali, `CommentReportTest` testlari.

Qoidalar: bitta odam bitta izohga bir marta (`409`), o'z izohiga shikoyat
qilib bo'lmaydi (`422`), o'chirilgan izoh — `404`. Shikoyat izohni
**yashirmaydi**: qarorni moderator qabul qiladi.

⚠️ Admin panelda hech narsa qo'shish kerak bo'lmadi: «faqat shikoyat
qilinganlar» filtri va shikoyatlar bo'yicha saralash boshidan bor edi —
shunchaki `reports_count` ni hech kim oshirmasdi va filtr doim bo'sh edi.

### ✅ Ilovadagi tugma 13.09.2026 da tayyor

Begona izohda — bayroqcha «Shikoyat qilish», o'zinikida — savat; ikkalasi
birga chiqmaydi. Bosilganda to'rtta sababli parda ochiladi. Testlar:
`report.test.tsx`.

⚠️ **Tizim oynasi emas, parda** — Android'dagi `Alert` ga ko'pi bilan
uchta tugma sig'adi, sabablar esa to'rtta va yana bekor qilish: ikkitasi
umuman chizilmasdi.

⚠️ Javob har doim ko'rsatiladi, rad etilganda ham: shikoyat ekranda hech
narsani o'zgartirmaydi, va sukut «tugma ishlamadi» dan farq qilmaydi.

### ✅ «Muallifni yashirish» 15.09.2026 da bajarildi

UGC siyosatining ikkinchi talabi — muallifni bloklash imkoni — **server
qismisiz** yopildi: yashirilganlar ro'yxati telefonda yashaydi
(`src/features/comments/mutedAuthors.ts`). Pardada alohida «Muallifni
yashirish» bandi paydo bo'ldi, shikoyat sabablaridan ajratgich bilan
ajratilgan: shikoyat moderatorga ketadi va hammaga ta'sir qiladi, yashirish
esa — telefon egasining shaxsiy sozlamasi.

Backenddan bitta maydon kerak bo'ldi: izoh kartochkasidagi `authorId`.
⚠️ Ism bo'yicha yashirib bo'lmaydi — ismdoshlar qoidabuzar bilan birga
yo'qolardi, o'zi esa profilda ismini almashtirib qaytib kelardi.

Yashirilganlar jimgina yo'qolmaydi: lenta ustida «N ta izoh yashirilgan» va
«Ko'rsatish» tugmasi chiqadi — aks holda odam lenta yuklanmadi deb o'ylardi va
yashirishni bekor qiladigan joy ham bo'lmasdi. Testlar: `mutedAuthors.test.tsx`.

§6a bo'yicha boshqa hech narsa qolmadi.

Shikoyat endpointi shartnomasi va sabablar ro'yxati —
[roadmap/PLAY_BACKEND_TASKS.md](../../roadmap/PLAY_BACKEND_TASKS.md) §2.

⚠️ Agar muddatga ulgurmasak, qonuniy aylanma yo'l bor: **izohlarni vaqtincha
o'chirib qo'yish** (bo'limni ko'rsatmaslik) va birinchi versiyani ularsiz
chiqarish, keyin shikoyat tayyor bo'lgach yangilanish bilan yoqish. Bu
tekshiruvdan «o'tib ketishga» urinishdan halolroq: olib tashlangan ilovani
qaytarish shu ishni qilishdan ko'ra uzoqroq davom etadi.

---

## 7. ✅ 13.09.2026 da bajarildi: OAuth loyihasi productionga o'tkazildi

Holat `Testing` edi va Google orqali kirish **test users ro'yxatidagi to'rtta
hisobdagina** ishlar edi. Qolganlarga — «ruxsat rad etildi», tashqaridan
buzilishdan farq qilmaydi. Endi holat **In production**, cheklov olib tashlandi.

Konsolda loyiha nomi — `uzcasting` (raqami `497193534365`), sahifa manzili:
**https://console.cloud.google.com/auth/audience?project=uzcasting**

⚠️ **`Publish app` tugmasi faol emas edi** — eng muhim eslab qolinadigan narsa
shu: **Branding** sahifasi to'ldirilmaguncha Google productionga o'tkazmaydi.
Ikkita havola yetishmasdi, ular yozildi:

| Maydon | Qiymat |
|---|---|
| App name | `UzCasting` (`Uzcasting` edi, brendga moslandi) |
| User support email | `uzcasting.org@gmail.com` |
| Application home page | `https://uzcasting.com` |
| **Application privacy policy link** | `https://uzcasting.com/maxfiylik` ← 13.09.2026 da yozildi |
| **Application terms of service link** | `https://uzcasting.com/kelishuv` ← 13.09.2026 da yozildi |
| Authorized domain | `uzcasting.com` |
| Developer contact | `uzcasting.org@gmail.com` |

Google verifikatsiyasi talab qilinmadi, va bu tasodif emas: u domenlar 10 tadan
ko'p bo'lsa, logotip yuklangan bo'lsa yoki maxfiy scope so'ralsa yoqiladi.
Bizda bitta domen, logotip yo'q, scope faqat `email` va `profile` — Google
«Push to production?» oynasida shuni yozdi.

⚠️ **Rozilik ekraniga logotip yuklamang.** Vasvasa bor, belgimiz tayyor — lekin
logotip yuklanishi ilovani haftalab davom etadigan verifikatsiyaga yuboradi.
Rozilik ekrani logotipsiz ham yaxshi ishlaydi.

Buni **hozirning o'zida, qayta build qilmasdan** tekshirsa bo'ladi: holat
bandlda emas, Google tomonida saqlanadi. Allaqachon o'rnatilgan APK'ni
(`preview` profili) oling va test users ro'yxatida bo'lmagan Google hisobi
bilan kiring — ilgari u «ruxsat rad etildi» olardi, endi o'tishi kerak.

⚠️ Agar o'tmasa, gap holatda emas, imzoda: APK'da EAS kaliti bo'lishi kerak,
uning `0B:74:39:...:70:CF` barmoq izi `UzCasting Android` klientiga yozilgan.

Masala tarixi — [GOOGLE_AUTH.md](./GOOGLE_AUTH.md), oxiridagi jadval, 2-band.

---

## 8. F bosqichi — do'kondagi kartochka [SIZ]

Yo'l: **Grow → Store presence → Main store listing**.

### 8.1. Matnlar

To'liq `mobile/store/play/LISTING.md` dan olinadi:

| Maydon | Chegara |
|---|---|
| App name | 30 belgi |
| Short description | 80 belgi |
| Full description | 4000 belgi |

⚠️ Tavsifda buildda yo'q narsani **va'da qilib bo'lmaydi**. Obuna to'lovi hozir
ishlamaydi (tariflar ekranidagi tugma o'chirilgan), shuning uchun matnda sotib
olish haqida bitta ham so'z yo'q — aks holda bu «misleading claims» va rad javob.

⚠️ Mumkin emas: «№1», «eng yaxshi», nomda Google/Play ni tilga olish, App name
ichida emoji.

### 8.2. Grafika

| Maydon | Fayl |
|---|---|
| App icon | `store/play/play-icon-512.png` |
| Feature graphic | `store/play/feature-graphic-1024x500.png` |

### 8.3. Telefon skrinshotlari [SIZ]

Play talabi: kamida **2** ta, ko'pi bilan 8 ta; tomoni 320 dan 3840 px gacha;
nisbati 9:16 dan tor va 16:9 dan keng bo'lmasin. Amaliyot: 5–6 ta vertikal,
1080×1920.

Skrinshotlar jangovar builddan olinadi (§10 dan keyin), stenddan emas — ularda
test ma'lumotlari va lokal manzillar bo'lmasligi kerak.

Ulangan telefondan:

```bash
adb exec-out screencap -p > 01-home.png
```

Nimani ko'rsatish (tartib bilan): asosiy lenta → premyera/serial → pleyer →
kasting → profil. Fayllarni menga yuboring — to'plamni yig'aman va o'lchamlarni
tekshiraman.

### 8.4. Store settings va kontaktlar

**Grow → Store presence → Store settings**:

- App category: **Entertainment**
- Tags: video/kino/seriallar bo'yicha 5 ta
- Contact details: `uzcasting.org@gmail.com` (**do'konda ochiq ko'rinadi**), telefon
  majburiy emas, sayt `https://uzcasting.com`

---

## 9. ⚠️ To'siq: Play App Signing kalitining SHA-1'i [SIZ]

Eng ko'p uchraydigan va eng «jimgina» buzilish: ilova tekshiruvdan o'tadi,
nashr qilinadi — va hammada Google orqali kirish ishlamay qoladi.

Sababi: Google do'kondan tarqatadigan faylni **o'zining** kaliti bilan imzolaydi
(Play App Signing), EAS kaliti bilan emas. Barmoq izi boshqacha bo'lib qoladi va
Google Android-klienti ilovani tanimay qo'yadi.

1. Birinchi buildni yuklash (§10) — bo'lim faqat shundan keyin paydo bo'ladi.
2. **Play Console → Release → Setup → App integrity** → **App signing** bo'limi
   → **App signing key certificate** bloki → **SHA-1 certificate fingerprint**
   ni nusxalash.
3. **https://console.cloud.google.com/auth/clients?project=uzcasting** ni ochish
4. **+ Create client** → turi **Android**:
   - Name: `UzCasting Android (Play)`
   - Package name: `uz.uzcasting.app`
   - SHA-1: 2-qadamdagi barmoq izi
   - **Create**

13.09.2026 holatiga ko'ra loyihada uchta klient bor, bu normal:

| Nomi | Turi | Nima uchun |
|---|---|---|
| `UzCasting` | Web application | `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID`, backendda token tekshiruvi |
| `UzCasting Android` | Android | `EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID`, EAS kaliti |
| `UzCasting Android Diyorbek` | Android | hamkasbning o'z ishlab chiqish kaliti uchun klienti |

To'rtinchisi — Play kaliti uchun — yoniga qo'shiladi va hech narsani o'chirmaydi.

⚠️ **Aynan yangi klient, eskisini tahrirlash emas.** Android-klientda SHA-1
maydoni bitta, ikkinchi barmoq izini u yerga qo'shib bo'lmaydi — eskisi
almashinadi va bizning ichki buildlarimizda kirish buziladi. Bir xil package
bilan bir nechta klient bo'lishi mumkin, mosini Google imzo bo'yicha o'zi
tanlaydi.

⚠️ Google tomonida o'zgarishlar 5 daqiqadan bir necha soatgacha qo'llanadi.
Kirishni **Play'dan** yuklab olingan build (internal testing) ustida tekshirish
kerak, diskdagi APK ustida emas — ularning imzosi har xil.

---

## 10. G bosqichi — build va ichki test [MEN] + [SIZ]

### 10.1. AAB yig'ish [MEN]

```bash
cd mobile
export EXPO_TOKEN=$(grep '^EXPO_TOKEN=' .env.local | cut -d= -f2)
eas build --platform android --profile production --non-interactive
```

`versionCode` ni EAS o'zi oshiradi (`autoIncrement`), qo'lda tegish shart emas.

### 10.2. Play'ga yuborish [MEN]

```bash
eas submit -p android --profile production --latest
```

Build **Internal testing** trekiga qoralama sifatida tushadi.

Servis kalitisiz — qo'lda: **Release → Testing → Internal testing → Create new
release → Upload** va `.aab` ni tashlash.

### 10.3. Ichki test [SIZ]

1. **Release → Testing → Internal testing** → **Testers** bo'limi
2. **Create email list** → 100 tagacha manzil (siz, buyurtmachi, hamkasblar)
3. Saqlab, **Copy link** ni bosish — havola
   `https://play.google.com/apps/internaltest/...` ko'rinishida
4. Testerlar havolani ochadi → **Become a tester** → **Download it on Google
   Play**

Ichki trek bir necha daqiqada chiqadi va **tekshiruvni kutmaydi**. Asosiysini
tekshiramiz:

- [ ] SMS orqali kirish haqiqiy raqamda ishlaydi;
- [ ] Google orqali kirish **test users ro'yxatida bo'lmagan hisobda** ishlaydi
      (bu §7 va §9 ni bir yo'la tekshiradi);
- [ ] video o'ynaydi (CDN havolalari tirik);
- [ ] hisobni o'chirish ishlaydi (§6);
- [ ] hech qayerda stend manzili `:8099` chiqmaydi.

---

## 11. H bosqichi — productionga chiqarish [SIZ]

1. **Release → Production** → **Create new release**
2. **Add from library** → internalda tekshirilgan aynan o'sha buildni tanlash
3. **Release name** — borligicha qoldiriladi (`1.0.0 (1)`)
4. **Release notes** — 500 belgigacha, birinchi versiya matni `LISTING.md` da
5. **Countries/regions** → **Add countries/regions** → kamida **Uzbekistan**.
   Barcha mamlakatlarni tanlasa ham bo'ladi: kontent o'zbekcha, cheklov hech
   narsa bermaydi, chetdagilarga esa xalaqit beradi.
6. **Next** → **Save** → **Send for review**

⚠️ **Staged rollout.** Reliz sahifasida **Rollout percentage** bor — 20% dan
boshlang. Birinchi sutkada «kira olmayapman» degan izohlar chiqsa, chiqarish
**Halt rollout** tugmasi bilan to'xtatiladi va hammaga tarqalmaydi.

Birinchi versiya tekshiruvi odatda bir necha kundan bir haftagacha, yangi
hisoblarda uzoqroq bo'lishi mumkin. Holati — chapda **Publishing overview**.

---

## 12. Nashrdan keyin

### Yangi buildsiz va tekshiruvsiz nima o'zgartirsa bo'ladi

`eas update` JS qismini havo orqali yetkazadi: ekranlar, matnlar, tarjimalar,
so'rovlar mantig'i, `assets/` dagi rasmlar. Buning uchun Google tekshiruvi
kerak emas.

```bash
eas update --channel production --environment production --message "nima o'zgardi"
```

⚠️ `eas update` `eas.json` dagi `env` ni **o'qimaydi** — o'zgaruvchilar EAS
muhitlaridan olinadi. Oqibatlari bilan tahlil —
[BUILDS_AND_UPDATES.md](./BUILDS_AND_UPDATES.md) §2.

### Nima uchun yangi build va yangi tekshiruv kerak

Nativ o'zgarishlar: yangi kutubxonalar, `app.json` tahrirlari (ikonka,
ruxsatlar, sxemalar), Expo SDK versiyasini almashtirish. Do'kon kartochkasidagi
o'zgarishlarni ham Google tekshiradi, lekin tezroq.

---

## 13. Rad javobining tez-tez uchraydigan sabablari — va bizdagi holat

| Sabab | Bizda |
|---|---|
| Maxfiylik siyosati ochilmaydi / boshqa | ✅ `/maxfiylik`, 200, o'sha domen |
| Hisobni o'chirish yo'q | ✅ §6 — endpoint, sahifa va ekran tayyor |
| Foydalanuvchi kontenti shikoyatsiz | ✅ §6a — izohga shikoyat tayyor |
| Tekshiruvchi kira olmadi | ✅ §5.4 — Premiumli demo raqam (prodda yoqiladi) |
| Reklama e'lon qilinmagan | ✅ «Yes» deb e'lon qilamiz (§5.2) |
| Data safety ilova xatti-harakatiga mos emas | ✅ javoblar kod bilan solishtirilgan (§5.7) |
| Raqamli kontent uchun to'lov Google Play Billing'dan tashqarida | ✅ buildda to'lov yo'q (§15) |
| Bo'sh yoki kesilgan skrinshotlar | ⏳ §8.3 |
| Target API talabdan past | ✅ API 36 |
| Ilova ishlatmaydigan ruxsatlar | ✅ faqat `INTERNET` qoldi |

⚠️ **To'lovlar uchun alohida §15 bo'limi.** U yerda Google'ning rasmiy
sahifalari bo'yicha tahlil qilingan: siyosat nimani talab qiladi, nega
Payme/Click/Uzum ilova ichida taqiqlangan, Play o'zbek foydalanuvchilarga
qaysi valyutada sotadi (faqat USD) va bizda qanday uchta qonuniy variant bor —
shu jumladan saytda mahalliy usullar bilan sotish.

---

## 14. Yagona ro'yxat — chek-list

**Buyurtmachi**
- [ ] D-U-N-S buyurtma qilindi (birinchi kuniyoq)
- [ ] Organization hisobi yaratildi, $25 to'landi
- [ ] Verifikatsiya o'tildi
- [ ] Sizga va menga ruxsatlar berildi

**Siz**
- [ ] `play-service-account.json` servis kaliti olindi va `mobile/` ga qo'yildi
- [ ] Konsolda ilova yaratildi (Free, ru)
- [ ] App content to'liq to'ldirildi (§5)
- [x] OAuth loyihasi In production ga o'tkazildi (§7) — 13.09.2026
- [ ] Do'kon kartochkasi to'ldirildi, skrinshotlar olindi (§8)
- [ ] Play kalitining SHA-1'i yangi Android-klient bilan yozildi (§9)
- [ ] Internal testing §10.3 ro'yxati bo'yicha o'tildi
- [ ] Productionga chiqarish, rollout 20%

**Hamkasb (backend)**
- [x] Hisobni o'chirish endpointi — 13.09.2026
- [x] `/hisobni-ochirish` sahifasi — 13.09.2026
- [x] O'zgarmas kodli demo raqam — 13.09.2026 (yuborishdan oldin prodda yoqiladi)
- [x] Izohga shikoyat — 13.09.2026
- [ ] **`V38` migratsiyasi bilan deploy** — shusiz bularning hech biri prodda yo'q

**Men**
- [x] Ruxsatlar `INTERNET` gacha qisqartirildi
- [x] `eas.json` dagi `submit` profili
- [x] 512 ikonka va feature graphic
- [x] Listing matnlari va anketa javoblari
- [x] Hisobni o'chirish ekrani — 13.09.2026
- [x] Izohlarda «Shikoyat qilish» tugmasi (§6a) — 13.09.2026
- [ ] `production` build va Play'ga yuborish

---

## 15. To'lovlar: Google Play nimaga ruxsat beradi va O'zbekistonda nimasi ishlaydi

> Alohida tadqiqot, 14.09.2026. «Tekshirildi» deb belgilangan hamma narsa o'sha
> kuni Google'ning rasmiy sahifalaridan olingan — havolalar §15.12 da.
>
> Bu yo'riqnomaning eng qimmat bo'limi: bu yerdagi xato nashrdan rad javobi
> emas, balki **nashr qilingan ilovani olib tashlash** va to'lovlarni
> muzlatishga olib keladi.

### 15.1. Asosiy qoida

Ilova **ichida** sotiladigan va **ichida** iste'mol qilinadigan hamma narsa
Google Play Billing orqali o'tishi shart. Siyosatdan aynan:

> «Play-distributed apps requiring or accepting payment for access to in-app
> features or services… must use Google Play's billing system»

Talab ostiga bevosita tushadi: **obunalar** (video/content subscription
services), **virtual valyuta**, **kontent va funksiyalarni ochish**.

Tushmaydi — bularni istalgan to'lov tizimi bilan o'tkazsa bo'ladi:

- jismoniy tovarlar;
- jismoniy xizmatlar: transport, ovqat yetkazish, sport zali abonementi,
  tadbirga chiptalar;
- kommunal va karta hisoblarini to'lash;
- odamlar o'rtasidagi o'tkazmalar (peer-to-peer);
- ro'yxatdan o'tgan xayriya tashkilotlariga ehsonlar.

Bizning katalogimizga bu istisnolarning birortasi ham tegishli emas:
seriallar, qismlar va Premium — bu ilovada iste'mol qilinadigan raqamli
kontent.

### 15.2. Bu UzCasting'ning har bir mahsuloti uchun nimani anglatadi

| Mahsulot | Google tasnifi bo'yicha nima | Ilovada nima bilan sotish mumkin |
|---|---|---|
| UzCasting Premium (24 000 / 49 999 / 99 000 / 159 900 so'm) | Subscription service | **faqat Play Billing** |
| Bitta qism — 3 000 so'm | Digital content | **faqat Play Billing** |
| Bitta premyera — 15 000 so'm | Digital content | **faqat Play Billing** |
| Stars (10 · 50 · 100 · 500 · 1 000) | Virtual valyuta | **faqat Play Billing** — tahlil §15.6 da |
| Reklamasiz tomosha | App functionality | **faqat Play Billing** |
| Kasting e'lonlari (hozir bepul) | — | to'lov yo'q |

⚠️ **Payme, Click, Uzum, UzCard va Humo ilova ichida bularning hammasi uchun
taqiqlangan.** Taqiq ko'ringanidan kengroq: bu faqat to'lov tugmasi emas,
balki to'lov formasi bilan webview, bank ilovasiga deeplink, QR-kod va hatto
«Payme orqali to'lang» degan matn ham.

### 15.3. O'zbekiston Google Play'da — tekshirilgan faktlar

| Savol | Javob |
|---|---|
| O'zbek kompaniyasi **sotuvchi** (merchant) bo'la oladimi? | **Ha.** O'zbekiston ham dasturchilar, ham merchant ro'yxatida bor; to'lov valyutasi sukut bo'yicha — **USD** |
| O'zbekistondagi foydalanuvchilar Play'da **sotib ola oladimi**? | **Ha**: pullik ilovalar ham, ilova ichidagi xaridlar ham, obunalar ham |
| Narxlar qaysi valyutada bo'ladi? | **Faqat USD**, oralig'i `0.05 – 999.99`. Jadvalda O'zbekiston yonida ★ turibdi — «foydalanuvchilar narxni USD yoki EUR da ko'radi va tranzaksiya shu valyutada o'tadi» |
| Foydalanuvchi nima bilan to'laydi? | Xalqaro kartalar: **Visa, Mastercard**, Amex, Discover. O'zbekiston uchun aloqa operatori orqali to'lov yordamda **ko'rsatilmagan** |
| **User Choice Billing** — Play yonida o'z to'lov tizimi — mavjudmi? | **Yo'q.** Dastur YeIH, Avstraliya, Braziliya, Indoneziya, Yaponiya, JAR, Buyuk Britaniya va AQShda ishlaydi. O'zbekiston ro'yxatda yo'q |

⚠️ **Mahsulot iqtisodini o'zgartiradigan ikkita oqibat.**

1. **Narxlar dollarda bo'ladi.** 24 000 so'm deb qo'yib bo'lmaydi: Play narxni
   USD da so'raydi (masalan `1.99`), odam esa dollarni ko'radi. ТЗ dagi barcha
   narxlarni qayta hisoblab, dollar to'riga moslash kerak, kurs o'zgarganda
   esa qaytadan ko'rib chiqiladi.
2. **UzCard va Humo to'g'ri kelmaydi.** Chet elda to'lovga ruxsat berilgan
   karta kerak. Auditoriyaning sezilarli qismida bunday karta yo'q — bu
   konversiyaga to'g'ridan-to'g'ri zarba, va buni integratsiyadan keyin emas,
   hozir bilgan yaxshiroq.

### 15.4. Google qancha oladi

| Nima | Komissiya |
|---|---|
| Kalendar yilidagi birinchi $1 mln tushum | **15 %** |
| $1 mln dan yuqorisi | **30 %** |
| Avtomatik uzaytiriladigan obunalar | birinchi kundan **15 %**, aylanmadan qat'i nazar |

⚠️ 30.06.2026 dan kuchga kiradigan yangi sxema (10 % + 5 % billing fee,
new/existing installs bo'yicha bo'linish) **faqat YeIH, Buyuk Britaniya va
AQSh uchun**. O'zbek trafigiga u tegishli emas — bizda odatdagi 15 / 30 %.

### 15.5. Uchta qonuniy arxitektura varianti

#### A varianti. Ilova ichida Play Billing

Ilova **ichida** sotishning yagona yo'li. Ustunligi: bir bosishda to'lov,
avtomatik uzaytirish, qaytarish va nizolar — Google tomonida. Kamchiligi:
komissiya, USD dagi narxlar, UzCard va Humo egalarining chetda qolishi.

#### B varianti. Consumption-only («reader»): to'lov saytda

Google bunga to'g'ridan-to'g'ri ruxsat beradi:

> «Yes. Google Play allows any app to be consumption-only, even if it is part
> of a paid service. For example, a user could log in when the app opens and
> access content paid for somewhere else.»

Lekin qattiq shart bilan:

> «Remember, consumption-only means that any product(s) or service(s), whether
> digital or physical, cannot be purchased from within the app.»

Ya'ni: odam `uzcasting.com` da **Payme, Click, Uzum, UzCard, Humo — xohlagan
narsasi** bilan to'laydi, ilova esa faqat sotib olinganini ochadi. Google
komissiyasi umuman yo'q, narxlar — so'mda.

⚠️ **Bunda ilovada nima yozish mumkin.** Bosiladigan havolalar taqiqlangan,
matn esa ruxsat etilgan — va Google o'zi misollar keltiradi:

> «You can purchase this book directly on our website»
> «Go to our website to upgrade your subscription to Premium»
> «This movie isn't available to rent in the app. However, any movie you rent
> through our website.com will be immediately available to view in the app»

Ya'ni tariflar ekrani halol yozishi mumkin: «Premium'ni uzcasting.com da
rasmiylashtirasiz — ilovada darhol ochiladi», **tugmasiz va havolasiz**.

⚠️ «Hech narsa sotib olib bo'lmaydi» sharti so'zma-so'z hech narsani
anglatadi. Bitta ishlaydigan xarid tugmasi — va ilova consumption-only
bo'lishdan to'xtaydi, sayt haqidagi matn esa anti-stiring buzilishiga
aylanadi.

#### C varianti. Gibrid

Ilovada Play Billing **va** saytda Payme/Click — parallel. Bunga ruxsat bor:
ilovadan tashqarida Google hech narsani cheklamaydi —

> «Yes. Outside of your app, you are free to communicate with your users about
> alternative purchase options. You can use email marketing and other channels
> outside of the app to provide subscription offers and even special pricing.»

Narxi: ikkita obuna tizimi va ikkita huquq manbai, qaytarishlar turli
joylarda, hamda ilova **ichida** saytni tilga olish taqiqi — B variantidagi
yon berish bu yerda ishlamaydi, chunki ichkarida xarid bor.

### 15.6. Stars va donatlar — bu yerda xato qilish oson

Aktyorga donat odamlar o'rtasidagi o'tkazmaga o'xshaydi, u esa istisnolarda
bor. Lekin shart qattiq yozilgan:

> «In cases where 100% of the tip or contribution from a user goes to the
> creator and the payment does not grant access to any digital content or
> services (including stickers, badges, special emojis etc.), then we regard
> this as a peer-to-peer payment… If any of these things is not true, then
> Google Play's billing system must be used»

Bizning modelimiz (MONETIZATION.md) ikkala band bo'yicha ham o'tmaydi:

- Stars **oldindan paketlab sotib olinadi** — bu virtual valyuta, ya'ni
  raqamli tovar, odamdan odamga o'tkazma emas;
- Stars **ko'rinadigan maqom** beradi: kreator profiliga, reytingga va oylik
  marosimga tushadi — mazmunan bu o'sha «badges».

Bunga platforma komissiyasini qo'shing, agar u bo'lsa — «100 % kreatorga
ketadi» sharti ham bajarilmaydi.

**Xulosa:** ilovada Stars — faqat Play Billing orqali. Yoki B varianti bo'yicha
saytda sotiladi.

### 15.7. «Axir Payme, Click va Uzum ham Google Play'da turibdi-ku»

Savol o'rinli va birinchi bo'lib paydo bo'ladi. Javob: Google qoidasi **siz
kimligingizga** emas, **nima sotilayotganiga va u qayerda iste'mol
qilinishiga** qaraydi. Ustiga-ustak, tekshiruv **har bir tovar bo'yicha
alohida** boradi, ilova bo'yicha emas:

> «Google Play's billing system must be used for the SKUs in your app that
> include more digital goods or services than physical goods or services, and
> for the SKUs in your app that are marketed to users as digital goods or
> services»

Hammasi shundan kelib chiqadi:

| Ilova | Odam unda nima sotib oladi | Qaysi qoida ostiga tushadi |
|---|---|---|
| Payme, Click | odamlar o'rtasidagi o'tkazmalar, kommunal, internet, aloqa, jarima to'lovlari | siyosatda **to'g'ridan-to'g'ri istisno**: peer-to-peer, «payment of a credit card or utility bill», jismoniy xizmatlar |
| Uzum Market | yetkazib beriladigan jismoniy tovarlar | **istisno**: physical goods |
| Uzum Tezkor | ovqat yetkazish | **istisno**: food delivery siyosatda nomma-nom bor |
| Uzum Bank | bank operatsiyalari | raqamli kontent xaridi emas |
| **UzCasting** | serialga, qismga, Premium'ga, Stars'ga kirish | **ilovada iste'mol qilinadigan raqamli kontent** → Play Billing |

Ya'ni Payme va Click «Play'da o'z to'lov tizimini ishlatishga ruxsat olgan»
emas — ular shunchaki **ilova ichida raqamli kontent sotmaydi**. U yerdagi pul
real dunyodagi narsalar uchun ketadi: yorug'lik, aloqa, tovar, boshqa odamga
o'tkazma.

⚠️ **Tekshiruv oddiy.** Bitta savol bering: «odam to'layotgan narsa ilova
ichida iste'mol qilinadimi yoki tashqarida?» Yorug'lik, aloqa, yetkazib
beriladigan tovar — tashqarida. Serialning qismi — ichkarida. Bizniki
ikkinchisi, va uni boshqacha o'qishning iloji yo'q.

⚠️ Teskarisi ham to'g'ri: ertaga Uzum o'z ilovasida o'zining video obunasini
sota boshlasa, **aynan o'sha tovar** Play Billing orqali o'tishi shart bo'ladi
— ilovadagi qolgan hamma narsa mahalliy to'lovda qolgani holda. Qoida
kompaniyaga emas, tovarga qo'llanadi.

### 15.8. Bizga to'g'ri kelishi mumkin bo'lgan uchta istisno — va nega ular qutqarmaydi

Gap ketganda, faqat asosiy eshikni emas, hammasini tekshirib chiqqan ma'qul.

**1. «Bir kishiga bir kishi» onlayn xizmatlar.** Google o'z billingini talab
qilmaydi, agar:

> «the paid service is between two individuals» va «the paid service is not
> available for replay afterwards (that is, the session is not recorded and
> cannot be accessed or used again) in any Play-distributed app»

Google misollari: musiqa va rasm darslari, murabbiy bilan mashg'ulotlar,
mutaxassis maslahati. Bizda bu, masalan, **kasting-direktor bilan pullik jonli
maslahat** bo'lishi mumkin edi — lekin faqat yozib olinmasa va keyin hech
qayerda ochilmasa. Seriallar katalogiga bunday narsa umuman taalluqli emas.

**2. Aloqa operatori va IPTV.** Telekom operatorlari uchun alohida eshik bor:
kontentni Play Billing'siz sotish mumkin, agar u **jismoniy xizmat hisobi**
(aloqa, kabel, IPTV) bilan to'lansa va faqat o'sha xizmat abonentlariga
sotilsa. UzCasting o'zi bunga tushmaydi — lekin **operator bilan bog'lanma**
tushadi: Premium shartli Uztelecom tarifiga kirsa va uning hisobi bilan
to'lansa. Bu kod emas, biznes-muzokara va Google'ga alohida ariza.

**3. Sovg'a kartalari.** Rasman:

> «No. Google Play's billing system is not required for the sale of in-app gift
> cards, regardless of whether the gift card is an eGift or physically mailed
> to the user.»

⚠️ **Va bu eshikdan foydalanmaslik kerak.** O'z obunangizni ilova ichida
«UzCasting sovg'a kartasi» deb atab sotish — bu istisno emas, siyosatni
aylanib o'tish. Bu band oddiy sovg'a kartalari (do'konlar, brendlar) uchun
yozilgan, o'z Premium'ini qayta o'rash uchun emas. Tekshiruv bunday sxemalarni
ko'radi, xatoning narxi esa tuzatish emas — ilovani olib tashlash.

**Uchalasi bo'yicha xulosa:** seriallar katalogi va Premium uchun ilova
**ichida** Play Billing'dan boshqa qonuniy yo'l yo'q. Payme/Click/Uzum orqali
to'lashning halol yagona usuli — §15.5 dagi B varianti: to'lov saytda, ilova
esa faqat sotib olinganini ko'rsatadi.

### 15.9. «Yangi TV'da hammasi ishlayapti-ku» — tirik misol tahlili

Eng kuchli qarshi dalil: o'zbek video-xizmatlari **allaqachon Play'da turibdi**,
ilova ichida Payme, Click, UzCard va Humo bilan pul oladi, narxlari esa so'mda.
Demak, mumkinmi?

`uz.plus.app` kartochkasi bo'yicha tekshirdim (Yangi.TV+, Kinolar TV LLC,
500 000+ o'rnatish, 21.02.2026 da yangilangan) — 14.09.2026 da ko'rildi.

**Ilovada nima ko'rinadi** (buyurtmachining skrinshotlari): «Tarif sotib olish»
ekrani `15 000 / 42 000 / 80 000 UZS` tariflari va «Sotib olish» tugmasi bilan,
keyin «Balansni to'ldirish» — o'n uchta to'lov usuli: Payme, Click,
Visa/Mastercard/UnionPay, UzCard/Humo, Paynet, BeePul, trastpay, SQB, alif,
Openbank, xazna, Mavrid va naqd pul.

**Google Play kartochkasida nima ko'rinadi:**

| Nimaga qaradim | U yerda nima bor |
|---|---|
| «In-app purchases» belgisi | **yo'q** — konsolda birorta ham Play Billing tovari yaratilmagan |
| Ilova tavsifi | filmlar, seriallar, obuna va to'lov haqida bitta ham so'z yo'q: «interaktiv platforma», «media materiallar», «raqamli xizmatlar» |
| Foydalanuvchi izohlari | «app for movie lovers», «please upload original versions of movies» — ya'ni odamlar aynan kino uchun kelgan |
| Yosh reytingi | **3+** |
| Data safety | «Data isn't encrypted» |

Javob shu. Ilova «Payme orqali sotishga ruxsat olgan» emas — **u shunchaki
do'konga pullik video sotayotganini aytmaydi**. Tavsif shunday yozilganki,
kartochkaga qarab na filmlar katalogini, na pullik tariflarni bilib bo'lmaydi.

⚠️ **«Bor va ishlayapti» degani «ruxsat etilgan» degani emas.** Google tekshiruvi
hammaning to'lov ekranini ketma-ket skanerlamaydi: u shikoyat bo'yicha, tanlab
o'tkaziladigan tekshiruvda yoki tirik tekshiruvchiga tushadigan navbatdagi
yangilanishda ishga tushadi. Ilova shu holatda yillab yashashi mumkin — keyin
bir sutkada yo'q bo'ladi.

**Bunday dasturchi to'lovdan tashqari yana nima bilan xavf ostida:**

- **Store listing.** Ilovaning mohiyatini yashiradigan tavsif — alohida
  qoidabuzarlik (chalg'ituvchi metama'lumotlar).
- **Yosh reytingi.** Jangari va Netflix seriallari katalogi uchun 3+ — IARC
  anketasi haqiqiy kontent bo'yicha to'ldirilmagan.
- **Data safety.** Odam karta kiritsa, forma esa «moliyaviy ma'lumot
  yig'ilmaydi» desa — bu forma bilan ilova xatti-harakatining nomuvofiqligi.

Bularning har biri mustaqil olib tashlash sababi, uchalasi esa qo'shiladi.

⚠️ **Ilova ichidagi balans hech narsani o'zgartirmaydi.** «Balansni to'ldir,
keyin filmga sarfla» sxemasi qoidadan chetlab o'tishdek ko'rinadi, lekin siyosat
Play Billing talab qiladigan ro'yxatning birinchi bandida aynan
`virtual currencies` ni sanaydi. Hamyon — chetlab o'tish emas, yana bitta
raqamli tovar.

**Nega bizga bu yo'ldan bormaslik kerak — qisqa va ishga oid:**

1. **Xavf assimetriyasi.** Yangi TV'da yarim million o'rnatish va ishlayotgan
   tushum bor: ularda yo'qotadigan narsa ham, himoya qiladigan narsa ham bor.
   Bizda esa nol o'rnatish — birovning xavfini takrorlab hech narsa yutmaymiz,
   birinchi kundanoq toza turishimiz esa mumkin.
2. **Muvaffaqiyatsizlik narxi — ilova emas, hisob.** Hisob **buyurtmachi**
   nomiga, D-U-N-S va yuridik shaxs tekshiruvi bilan rasmiylashtiriladi (§1).
   Takroriy buzilishlar dasturchi hisobini butunlay bloklashga olib keladi —
   kompaniyaning kelajakdagi barcha ilovalari bilan birga. Tiklash to'lovni
   qayta yozishdan qimmatroq turadi.
3. **To'lovni qayta qilish do'konga qaytishdan arzon.** Olib tashlangan ilova
   apellyatsiya va qayta tekshiruv orqali qaytadi, va har doim ham qaytmaydi.

**Amalda bu nimani anglatadi:** qaror buyurtmachiniki — hisob ham, pul ham
uniki. Lekin uni ko'zi ochiq holda qabul qilish kerak: «raqobatchida ham
shunday» — bu Google ruxsati emas, balki ularni hali ushlamaganining izohi.
Qonuniy variantlar §15.5 dagi o'sha uchtasi, va B varianti (saytda to'lov)
aynan o'sha Payme, Click va UzCard'ni beradi — faqat hisob uchun xavfsiz.

⚠️ Kirish haqidagi savolga: **avtorizatsiya usuli to'lov siyosatiga hech qanday
ta'sir qilmaydi.** Faqat telefon raqami bilan kirish — Yangi TV'dagidek va
bizdagidek — to'lov qoidalariga aloqador emas.

### 15.10. Mening tavsiyam

Hozir buildda bitta ham ishlaydigan to'lov tugmasi yo'q, va bu **yaxshi
boshlang'ich holat**: birinchi versiya to'lov xavfisiz tekshiruvdan o'tadi.
Keyin yo'l ayrilishi, va uni buyurtmachi hal qiladi:

1. **Birinchi reliz — borligicha.** Tariflar ekrani to'lovsiz vitrina bo'lib
   qoladi.
2. **Agar asosiy bozor O'zbekiston bo'lsa** va odamlar UzCard bilan
   Payme/Click orqali to'lashi kerak bo'lsa, **B varianti** mahalliy to'lov
   usullarini, so'mdagi narxlarni va nol komissiyani beradi. Buning evaziga —
   ilovada umuman hech narsa sotib bo'lmaydi.
3. **Play Billing (A varianti)** bir bosishli konversiya muhim bo'lganda va
   auditoriyada xalqaro kartalar bo'lganda o'zini oqlaydi — masalan, chet
   eldagi diaspora uchun.

⚠️ «Ilova ichida, lekin Payme orqali» varianti hech qanday ko'rinishda mumkin
emas. Aynan u eng tabiiy ko'rinadi — va aynan u ilovani nashrdan olib
tashlashga kafolatli olib boradi.

### 15.11. To'lov kodini yozishdan oldin buyurtmachidan nima kerak

- [ ] Variantni tanlash: A (Play Billing), B (saytda to'lov) yoki C (gibrid)
- [ ] Agar A yoki C: ТЗ dagi narxlarni USD ga qayta hisoblab tasdiqlash
- [ ] Agar A yoki C: merchant to'lov profilini va USD dagi bank rekvizitlarini
      to'ldirish
- [ ] Agar B: saytga Payme/Click/Uzum ni ulash va to'lovdan keyin huquqlar
      ilovada darhol paydo bo'lishini ta'minlash
- [ ] Stars bo'yicha qaror: Play Billing, saytda sotish yoki keyinga qoldirish

### 15.12. Manbalar (14.09.2026 da tekshirildi)

- To'lovlar siyosati — https://support.google.com/googleplay/android-developer/answer/9858738
- Siyosat tahlili va FAQ (reader-ilovalar, donatlar, anti-stiring) — https://support.google.com/googleplay/android-developer/answer/10281818
- Dasturchi va merchant mamlakatlari — https://support.google.com/googleplay/android-developer/answer/9306917
- Mamlakatlar bo'yicha valyuta va narx oralig'i — https://play.google.com/supported-locations
- Foydalanuvchining to'lov usullari — https://support.google.com/googleplay/answer/2651410
- User Choice Billing — https://support.google.com/googleplay/android-developer/answer/12570971
- Komissiyalar — https://support.google.com/googleplay/android-developer/answer/112622

⚠️ App Store o'xshash mantiq bilan yashaydi (raqamli uchun o'z billingi,
reader-ilovalar uchun alohida rejim), lekin u yerda qoidalar va ta'riflar
boshqacha — bu tadqiqot ularni **qamrab olmaydi**.
