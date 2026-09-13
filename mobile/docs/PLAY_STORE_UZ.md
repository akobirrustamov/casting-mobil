# Google Play'ga chiqarish — bosqichma-bosqich

> Hisob turi: **tashkilot (Organization)**, **buyurtmachi nomiga** rasmiylashtiriladi.
> Shu faylning ruscha varianti — [PLAY_STORE_RU.md](./PLAY_STORE_RU.md).
>
> Oxirgi yangilanish: 13.09.2026

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
| To'lovlar | ilovada ishlaydigan to'lov tugmasi yo'q → Google Play Billing **talab qilinmaydi** |

### Nashrni to'sib turgan narsalar

Yettita band, bittasi allaqachon hal qilindi. Qolganlari — bu shunchaki «izoh»
emas, bu rad javobi yoki tirik foydalanuvchilarda ishlamaydigan kirish.

| # | To'siq | Kim hal qiladi | Batafsil |
|---|---|---|---|
| 1 | Hisobni o'chirish: **backend 13.09.2026 da tayyor**, ilovada ekran kerak | **[MEN]** | §6 |
| ~~2~~ | ~~Google Cloud OAuth `Testing` holatida~~ — **13.09.2026 da hal qilindi**, holat `In production` | — | §7 |
| 3 | Play App Signing kalitining SHA-1'i Google Android-klientiga yozilmagan | **[SIZ]** | §9 |
| 4 | Demo-kirish: **kod 13.09.2026 da tayyor**, yuborishdan oldin prodda yoqiladi | **[SIZ]** | §5.4 |
| 5 | Skrinshotlar yo'q | **[SIZ]** | §8.3 |
| 6 | Play Console hisobi yo'q | **[BUYURTMACHI]** | §1 |
| 7 | Izohga shikoyat: **endpoint 13.09.2026 da tayyor**, ilovada tugma kerak | **[MEN]** | §6a |

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
`+998 90 000 00 00` → `000000`) va haqiqiy SMS yubormaydi.

### ✅ 13.09.2026 da bajarildi — lekin prodda o'chiq

Mexanizm `OtpService` da: bitta raqamga SMS yuborilmaydi, kod esa o'zgarmas.
**Sukut bo'yicha o'chiq** — faqat ikkala sozlama ham berilganda ishlaydi:

```properties
app.otp.demo.phone=+998900000000
app.otp.demo.code=000000
```

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

Qoldi:

| Kim | Nima |
|---|---|
| **[BEK]** | `V38` migratsiyasi bilan deploy — shusiz saytda sahifa yo'q |
| **[MEN]** | sozlamalarda «Hisobni o'chirish» ekrani: tasdiqlash, endpointga so'rov, chiqish, uz/ru/en tarjimalar, test |

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

Qoldi:

| Kim | Nima |
|---|---|
| **[MEN]** | izoh kartochkasida «…» → «Shikoyat qilish» (sabablar ro'yxati); uz/ru/en tarjimalar; testlar |
| **[BEK]** | «muallifni yashirish» bo'yicha qaror: qilamizmi yoki shikoyat yetarli deb yozib qo'yamizmi |

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
| Hisobni o'chirish yo'q | ⏳ §6 — asosiy to'siq |
| Foydalanuvchi kontenti shikoyat va bloklashsiz | ⏳ §6a — izohlar |
| Tekshiruvchi kira olmadi | ⏳ §5.4 — demo-kirish |
| Reklama e'lon qilinmagan | ✅ «Yes» deb e'lon qilamiz (§5.2) |
| Data safety ilova xatti-harakatiga mos emas | ✅ javoblar kod bilan solishtirilgan (§5.7) |
| Raqamli kontent uchun to'lov Google Play Billing'dan tashqarida | ✅ buildda to'lov yo'q |
| Bo'sh yoki kesilgan skrinshotlar | ⏳ §8.3 |
| Target API talabdan past | ✅ API 36 |
| Ilova ishlatmaydigan ruxsatlar | ✅ faqat `INTERNET` qoldi |

⚠️ **To'lovlar haqida alohida.** Ilovada obuna, qismlar yoki Stars uchun
haqiqiy sotib olish paydo bo'lishi bilan Google uni **Google Play Billing**
orqali o'tkazishni talab qiladi (komissiya bilan). Raqamli kontent uchun ilova
ichida tashqi to'lov (Payme/Click/Uzum) taqiqlanadi va olib tashlashga olib
keladi. Bu mahsulot bo'yicha qaror va uni to'lov kodini yozishdan **oldin**
qabul qilish kerak.

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
- [ ] Hisobni o'chirish endpointi
- [ ] `/hisobni-ochirish` sahifasi
- [ ] Prodda o'zgarmas kodli demo raqam
- [ ] Izohga shikoyat + moderatsiya navbati (§6a)

**Men**
- [x] Ruxsatlar `INTERNET` gacha qisqartirildi
- [x] `eas.json` dagi `submit` profili
- [x] 512 ikonka va feature graphic
- [x] Listing matnlari va anketa javoblari
- [ ] Hisobni o'chirish ekrani (endpointdan keyin)
- [ ] Izohlarda «Shikoyat qilish» va «Muallifni yashirish» tugmalari (§6a)
- [ ] `production` build va Play'ga yuborish
