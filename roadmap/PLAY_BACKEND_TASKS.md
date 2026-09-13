# Google Play uchun backend vazifalari

> Mobil ilovani Google Play'ga chiqarish uchun **backend tomonida** kerak
> bo'ladigan narsalar. Har biri Google siyosati talabi — «yaxshi bo'lardi»
> emas, ularsiz ilova rad etiladi.
>
> To'liq nashr yo'riqnomasi: [mobile/docs/PLAY_STORE_UZ.md](../mobile/docs/PLAY_STORE_UZ.md)
>
> Oxirgi yangilanish: 13.09.2026
>
> **Holat: uchala band ham backend tomonida BAJARILDI (13.09.2026).** Qolgani —
> mobil ilovadagi ekranlar va prod sozlamasi (§3), ular quyida belgilangan.

Belgilar: `[ ]` bajarilmagan · `[x]` bajarilgan

---

## 1. `[x]` Hisobni o'chirish — ENG MUHIMI

**Nega:** Google'ning «Data deletion» siyosati: ilovada **hisob yaratish**
mumkin bo'lsa, uni o'chirish ham bo'lishi shart. Va aynan **ikki yo'l bilan** —
ilova ichida va ilovani o'rnatmasdan, veb-sahifa orqali. Ikkinchisining
manzili Play Console'ning App content bo'limiga yoziladi va tekshiruvchi uni
ochib ko'radi.

Bu birinchi raqamli to'siq edi — 13.09.2026 da yopildi, quyida tafsilot.

### 1.1. Endpoint

```
DELETE /api/v1/app/me
Authorization: Bearer <token>
```

| Javob | Qachon |
|---|---|
| `204 No Content` | hisob o'chirildi |
| `401` | token yo'q yoki eskirgan |

Nima qilishi kerak:

- foydalanuvchini o'chirilgan deb belgilash (yumshoq o'chirish bo'lsa ham
  bo'ladi, lekin shaxsiy ma'lumot tozalanishi shart);
- telefon raqami, ism, email, profil surati, qurilmalar ro'yxati —
  tozalanadi yoki anonimlashtiriladi;
- barcha sessiyalar/tokenlar bekor qilinadi;
- izohlar: muallif nomi «O'chirilgan foydalanuvchi» ga almashadi yoki
  izohlar o'chiriladi — qaysi biri bo'lsa ham, oldindan hal qilinsin;
- to'lov yozuvlari qonun talab qilgan muddat davomida qolishi mumkin — bu
  maxfiylik siyosatida allaqachon yozilgan.

⚠️ **O'chirilgan raqam bilan qayta ro'yxatdan o'tish ishlashi kerak.** Aks
holda odam hisobini o'chirib, ilovaga boshqa hech qachon kira olmaydi.

### 1.2. Veb-sahifa

```
GET https://uzcasting.com/hisobni-ochirish
```

`/maxfiylik` va `/kelishuv` yonida, xuddi shu `LegalPageController` orqali
(`backend/src/main/resources/legal/`). Login talab qilmasin — tekshiruvchi
unga ilovasiz kiradi.

Sahifada bo'lishi shart:

- ilova nomi (UzCasting) va dasturchi nomi;
- hisobni o'chirish uchun nima qilish kerakligi: ilovada `Profil →
  Sozlamalar → Hisobni o'chirish`, yoki shu sahifadagi forma orqali ariza
  (telefon raqami + `uzcasting.org@gmail.com` ga xat);
- **qanday ma'lumot o'chirilishi** va **qaysi biri qancha muddat saqlanishi**
  (to'lov yozuvlari) — Google aynan shu ikki ro'yxatni qidiradi;
- so'rov ko'rib chiqilish muddati (maxfiylik siyosatida 30 kun deyilgan).

### ✅ Bajarildi (13.09.2026)

| Nima | Qayerda |
|---|---|
| `DELETE /api/v1/app/me` | `AppProfileController` |
| Qoidalar va tozalash | `AccountDeletionService` |
| `cms_user_account.deleted_at`, `UserStatus.DELETED` | migratsiya `V38` |
| Sahifa | `GET /hisobni-ochirish` — `LegalPageController` + `resources/legal/` |
| Testlar | `AccountDeletionTest` (7 ta), `LegalPageTest` |

Qabul qilingan qarorlar: hisob qatori **o'chirilmaydi**, shaxsiy ma'lumot
tozalanadi (telefon, email, Google sub, ism, rasm), sessiyalar bekor
qilinadi, qurilma/saqlangan/ko'rish joyi o'chadi; izohlar matni qoladi,
muallif «O'chirilgan foydalanuvchi» bo'ladi; to'lov va obuna yozuvlari
qoladi. Telefon bo'shaydi — o'sha raqam bilan qayta ro'yxatdan o'tish
ishlaydi (test bilan qulflangan).

⚠️ **Mobil ilovada ekran hali yo'q** — u alohida ish
(mobile/docs/PLAY_STORE_UZ.md §6).

---

## 2. `[x]` Izohga shikoyat — 10.09.2026 dan keyin paydo bo'lgan to'siq

**Nega:** izohlar qo'shilgani bilan ilova Google uchun **UGC** (foydalanuvchi
kontenti) bo'lib qoldi. UGC siyosati moderatsiyadan tashqari yana ikki narsani
talab qiladi: begona izohga **shikoyat qilish** va muallifni **bloklash**.
Tekshiruvchi izohlar ro'yxatini ochib, shikoyat tugmasini qidiradi — topmasa,
rad javobi.

Bizda moderatsiya bor edi (moderator yashiradi, qoidabuzar `403` oladi),
shikoyat esa yo'q edi — 13.09.2026 da qo'shildi.

### 2.1. Endpoint

```
POST /api/v1/app/comments/{commentId}/report
Authorization: Bearer <token>
Content-Type: application/json

{ "reason": "SPAM" }
```

`reason` qiymatlari (mobil ilovada ro'yxat sifatida ko'rsatiladi):

| Kod | Ma'nosi |
|---|---|
| `SPAM` | reklama yoki spam |
| `INSULT` | haqorat, tahqirlash |
| `ADULT` | nomaqbul, kattalar uchun kontent |
| `OTHER` | boshqa sabab |

| Javob | Qachon |
|---|---|
| `204 No Content` | shikoyat qabul qilindi |
| `409 Conflict` | bu izohga allaqachon shikoyat qilgansiz |
| `401` | kirilmagan |

Shikoyat admin panelda moderatsiya navbatiga tushsin — kamida ro'yxat:
izoh matni, kontent, muallif, shikoyat sababi, shikoyat qilganlar soni.

### ✅ Bajarildi (13.09.2026)

| Nima | Qayerda |
|---|---|
| `POST /api/v1/app/comments/{id}/report` | `AppCommentController` |
| Qoidalar | `AppCommentService.report` |
| `cms_comment_report` jadvali | migratsiya `V38` |
| Testlar | `CommentReportTest` (9 ta) |

⚠️ **Admin panelda yangi ish talab qilinmadi.** Moderatsiya ro'yxatida
`reportedOnly` filtri va shikoyatlar bo'yicha saralash ALLAQACHON bor edi —
faqat `cms_comment.reports_count` ni oshiradigan hech kim yo'q edi, shuning
uchun filtr doim bo'sh ro'yxat qaytarardi. Endi u ishlaydi.

Qoidalar: bitta odam bitta izohga bir marta (409), o'z izohiga shikoyat
qilib bo'lmaydi (422), o'chirilgan izoh — 404. Shikoyat izohni AVTOMATIK
yashirmaydi: qarorni moderator qabul qiladi.

⚠️ **Mobil ilovada tugma hali yo'q** — alohida ish.

### 2.2. Muallifni yashirish (ixtiyoriy, lekin tavsiya etiladi)

```
POST   /api/v1/app/users/{userId}/mute
DELETE /api/v1/app/users/{userId}/mute
```

Yashirilgan muallifning izohlari shikoyat qilgan odamga ko'rinmaydi. Agar buni
qilmaslikka qaror qilsak — shu qaror yozma qayd etilsin, chunki Google
«bloklash» imkonini ham so'raydi va rad javobida aynan shunga havola qiladi.

---

## 3. `[x]` Tekshiruvchi uchun demo raqam (kod tayyor, prod sozlamasi kutilmoqda)

**Nega:** ilovaga kirish — o'zbek raqamiga keladigan SMS kod orqali. Google
tekshiruvchisi boshqa mamlakatda o'tiradi va SMS ololmaydi. Kira olmasa —
«ilova ishlamaydi» deb rad etadi.

Kerak: **prodda bitta o'zgarmas test raqami**, uning uchun server:

- haqiqiy SMS **yubormaydi**;
- oldindan kelishilgan kodni qabul qiladi (masalan `+998 90 000 00 00` →
  `000000`);
- oddiy foydalanuvchi huquqlarini beradi (katalog, kasting, video).

⚠️ Hozir bunday narsa faqat `DevDataSeeder` da bor, u esa serverda yoqilmaydi
(`yuklash/application.properties` da `app.dev.*` yo'q).

⚠️ Raqam va kod Play Console'ning «App access» bo'limiga yoziladi va faqat
Google tekshiruvchisiga ko'rinadi — ommaviy joyda e'lon qilinmaydi.

### ✅ Kod tayyor (13.09.2026), prodda YOQILMAGAN

Mexanizm `OtpService` da. Sukut bo'yicha **o'chiq** — ikkala sozlama ham
bo'sh bo'lsa hech qanday demo raqam yo'q, ya'ni bugungi prodda hech narsa
o'zgarmadi.

Yoqish uchun `/opt/uzcasting/application.properties` ga ikki qator:

```properties
app.otp.demo.phone=+998900000000
app.otp.demo.code=000000
```

Raqam va kodni Play'ga yuborishdan oldin tanlang — yuqoridagilar MISOL.

⚠️ Yoqilganda ishga tushishda `WARN` chiqadi: «Demo telefon raqami
YOQILGAN». Bu ataylab — sozlama prodda tasodifan qolib ketmasin.

⚠️ Tekshiruvdan keyin ikkala qatorni ham o'chiring.

Testlar: `DemoPhoneOtpTest` — SMS yuborilmasligi, kod ishlashi, noto'g'ri
kod o'tmasligi va eng muhimi: demo kod BOSHQA raqamlarda ishlamasligi.

---

## 4. `[x]` Maxfiylik siyosatiga ikkita qator qo'shildi (13.09.2026)

`backend/src/main/resources/legal/maxfiylik.html` ga 1-bo'limdagi jadvalga
ikkita qator qo'shildi:

- **Izohlar** — matn, vaqt, ism boshqa foydalanuvchilarga ko'rinishi;
- **Qurilma identifikatori va nomi** — tasodifiy UUID va qurilma nomi,
  qurilmalar ro'yxati uchun.

**Nega kerak edi:** Play Console'dagi «Data safety» formasida biz shu ikkalasini
yig'amiz deb belgilaymiz. Forma bilan siyosat mos kelmasa, Google buni
avtomatik topadi — bu rad javobining tayyor sababi.

⚠️ **Deploy kerak.** Sahifa jar ichida, ya'ni o'zgarish faqat backend qayta
yig'ilib, serverga chiqqandan keyin ko'rinadi.

### Hali hal qilinmagan ikkita nomuvofiqlik

| Siyosatda | Ilovada | Nima qilish |
|---|---|---|
| «Parol» yig'iladi deyilgan | mobil ilovada parol yo'q (SMS kodi yoki Google) | zarar yo'q: siyosat sayt va admin panelni ham qamraydi. Aniqlik kiritilsa yaxshi |
| «To'lovlar va hisob balansi» yig'iladi deyilgan | ilovada ishlaydigan to'lov yo'q | Data safety'da «moliyaviy ma'lumot yig'ilmaydi» deb javob beramiz. To'lov paydo bo'lgach — ikkalasini birga yangilash |
| 7-bo'lim: «16 yoshdan kichiklarga mo'ljallanmagan» | Play'da yosh guruhini tanlashimiz kerak | IARC anketasi natijasi bilan moslansin: siyosat 16+ desa, Play'da ham 16+ bo'lsin |

---

## Nima qoldi

| Kim | Nima |
|---|---|
| **Backend/DevOps** | `V38` migratsiyasi bilan deploy; `/maxfiylik` va `/hisobni-ochirish` sahifalari faqat shundan keyin yangilanadi |
| **Backend/DevOps** | Play'ga yuborishdan oldin `app.otp.demo.*` ni yoqish, keyin o'chirish |
| **Mobil** | Sozlamalarda «Hisobni o'chirish» ekrani |
| **Mobil** | Izohda «Shikoyat qilish» tugmasi va sabablar ro'yxati |

---

## Muddat haqida

1 va 3-band — birinchi relizga **shart**. 2-band ham shart, lekin muqobili bor:
izohlar bo'limini vaqtincha o'chirib qo'yib, birinchi versiyani ularsiz
chiqarish va shikoyat tayyor bo'lgach yangilanish bilan yoqish.

Buyurtmachi tomonidagi eng uzun ish — Play Console hisobi va D-U-N-S raqami
(30 kungacha), shuning uchun bu uch band bilan parallel ketish mumkin.
