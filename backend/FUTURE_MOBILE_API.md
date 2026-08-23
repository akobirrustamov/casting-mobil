# Kelajakdagi mobil API kontrakti

> ТЗ §76 va §77. **Bu yerda mobil kod yozilmaydi** — faqat backend
> tayyorligini qayd etadigan kontrakt. Mobil ekranlar hozirgi scope'da
> emas.

Maqsad: mobil jamoa ish boshlaganda backend shaklini qayta o'ylab
o'tirmasin va mavjud ma'lumot modeli ularga to'g'ri kelsin.

---

## 1. Til — har uchala tilda

Barcha foydalanuvchiga ko'rinadigan matn **UZ, RU, EN** da saqlanadi va
nashr paytida uchalasi ham majburiy (`TranslationRules`). Kontent,
qism, kategoriya, janr, ijodkor, reklama, premyera, bildirishnoma —
hammasi shu qoidaga bo'ysunadi.

Klient tilni ikki yo'l bilan bildiradi:

| Usul | Qachon |
|---|---|
| `?lang=RU` so'rov parametri | Aniq tanlov, profil hali yo'q |
| `cms_user_account.language` | Ro'yxatdan o'tgan foydalanuvchi |

Tartib: **so'rovdagi til → profildagi til → UZ**. Push xabar faqat
profildagi tilga tayanadi (so'rov yo'q).

⚠️ Bitta maydonli `title` **yo'q**. Har bir tarjima alohida qatorda
(`cms_content_translation` va h.k.), `UNIQUE(parent_id, locale)` bilan.
Ya'ni yangi til qo'shish migratsiya talab qilmaydi — `Locale` enum'iga
qiymat qo'shiladi, xolos.

---

## 2. Tayyor endpointlar

```
GET  /api/v1/app/home                 bosh sahifa bo'limlari (§31)
GET  /api/v1/app/watch/{episodeId}    qism — entitlement tekshiruvi bilan
GET  /api/v1/app/watch/content/{id}   SINGLE kontent (film, klip, shou)
GET  /api/v1/app/media/{id}/raw       rasm ochiq, video huquq bilan
POST /api/v1/app/analytics/events     hodisa yuborish (§74)
```

## 3. Hali ochilmagan, lekin model tayyor

```
GET  /api/v1/app/catalog              katalog + filtr
GET  /api/v1/app/content/{slug}       kontent sahifasi
GET  /api/v1/app/search               qidiruv (indekslar V21, V23 tayyor)
GET  /api/v1/app/me                   profil, obuna, balans, qurilmalar
POST /api/v1/app/purchases            xarid oqimi
POST /api/v1/app/comments             izoh yozish
GET  /api/v1/app/ads                  ko'rsatiladigan reklama
POST /api/v1/app/auth/**              OTP bilan kirish
```

---

## 4. «Keyinroq ko'raman» (§77)

Foydalanuvchi kino yoki serialni saqlab qo'yishi kerak bo'ladi.
**Hozir admin panelda bunga sahifa kerak emas** va jadval ham
yaratilmadi — bo'sh jadval qo'shish keraksiz.

Muhimi shuki, **kontent ID arxitekturasi barqaror** — bu tekshirildi,
taxmin emas:

| Savol | Javob | Dalil |
|---|---|---|
| ID turi o'zgaradimi? | Yo'q — `Long`, IDENTITY | `IdStrategyTest` (§57) |
| ID qayta ishlatiladimi? | Yo'q | IDENTITY orqaga qaytmaydi |
| Kontent bazadan yo'qoladimi? | Yo'q — arxivlanadi | `SoftDeleteTest` (§58) |
| Slug barqarormi? | **YO'Q** | Admin uni tahrirlashi mumkin |

⚠️ **Saqlanganlar ro'yxati `content_id` ga bog'lansin, slugga emas.**
Slug — odam o'qiydigan manzil, u o'zgarishi mumkin va o'zgarganda
foydalanuvchining saqlangan ro'yxati buzilardi. Mobil klient ID ni
allaqachon ko'radi: `HomeFeedDto.ContentCard` da `id` ham, `slug` ham
qaytariladi.

Ya'ni kelajakdagi jadval shunchaki:

```sql
create table user_watchlist (
    id         bigserial primary key,
    user_id    uuid   not null references users(id),
    content_id bigint not null references cms_content(id),
    created_at timestamp not null,
    unique (user_id, content_id)
);
```

Bundan boshqa hech narsa o'zgarmaydi.

⚠️ Ro'yxatni qaytarishda **arxivlangan kontent filtrlansin**: saqlangan
film arxivga tushsa, u ro'yxatda «ochilmaydigan element» bo'lib
qolardi.

---

## 5. Mobil uchun muhim qarorlar

| Qaror | Sabab |
|---|---|
| Pul `BigDecimal`/tiyin | Suzuvchi nuqta pul uchun ishlatilmaydi (§36) |
| Vaqt `Asia/Tashkent` | §68 — mintaqa aniq belgilangan |
| Qurilma chegarasi entity'si bor, majburlash yo'q | Mobil ilova ulanganda yoqiladi |
| To'lov provayderi ulanmagan | Ulanmagan provayder soxta muvaffaqiyat qaytarmaydi (§44) |
| FCM ulanmagan | Bildirishnoma yoziladi, yuborilmaydi — hisobotda `null`, `0` emas |

---

## 6. Video — QAROR KUTILMOQDA

⚠️ Buyurtmachi `tz/roadmap for bunny stream*.md` qo'shdi: video
**Bunny Stream** orqali, «server — turniket, quvur emas», ya'ni bironta
bayt VPS orqali o'tmaydi.

Bu hozirgi yechimga (lokal saqlash + bo'laklab yuklash + `Range`) **zid**.
Javob kelmaguncha video kodi o'zgartirilmadi. Qaror qabul qilinsa,
`/api/v1/app/watch/*` javobiga imzolangan HLS havolasi qo'shiladi va
mobil ilova to'g'ridan-to'g'ri CDN'dan oladi.
