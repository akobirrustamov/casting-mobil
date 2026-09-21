# Uzum Bank va Google Play: UzCasting'da to'lov siyosati

> 15.09.2026. Google qoidalari shu kuni rasmiy sahifalardan qayta tekshirildi
> (havolalar §10 da). Batafsil tadqiqot: `PLAY_STORE_UZ.md` → §15.
>
> Bu hujjat bitta savolga javob beradi: **Uzum Bank'ni loyihaga qanday ulasak,
> Google Play ilovani olib tashlamaydi.**

---

## 1. Qisqa javob

| Qayerda | Uzum Bank bilan to'lov | Nega |
|---|---|---|
| **Ilova ichida** — Premium, qism, premyera, Stars, tangalar | ❌ **Mumkin emas** | Bu ilovada iste'mol qilinadigan raqamli kontent. Google Play Billing majburiy |
| **Saytda** (`uzcasting.com`) — xuddi shu mahsulotlar | ✅ **Mumkin** | Google ilovadan tashqaridagi to'lovga aralashmaydi |
| **Ilova** saytda sotib olinganini ochib beradi | ✅ **Mumkin** | «Consumption-only» (reader) ilova — Google to'g'ridan-to'g'ri ruxsat bergan |

Ya'ni Uzum Bank'ni **ulasa bo'ladi**, lekin **to'lov oynasi saytda** bo'ladi,
ilova esa hech narsa sotmaydi — faqat sotib olingan narsani ko'rsatadi.

⚠️ «Ilova ichida Uzum Bank tugmasi» varianti hech qanday ko'rinishda qonuniy
emas: tugma ham, webview ham, Uzum Bank ilovasiga deeplink ham, QR-kod ham,
saytga havola ham.

---

## 2. Google qoidasi nima deydi

### 2.1. Asosiy talab

> «Play-distributed apps requiring or accepting payment for access to in-app
> features or services… must use Google Play's billing system»

Talab ostiga aniq nomi bilan tushadi:

- virtual valyuta — `virtual currencies` (bizda **Stars**, **tangalar**);
- obunalar, jumladan `video` obunalar (bizda **Premium**);
- ilova kontenti va funksiyalari, `ad-free version` (bizda **qism**,
  **premyera**, **reklamasiz tomosha**).

### 2.2. Istisnolar — Play Billing kerak bo'lmagan holatlar

- jismoniy tovarlar va jismoniy xizmatlar;
- karta yoki kommunal hisobini to'lash;
- odamlar o'rtasidagi o'tkazmalar (peer-to-peer), onlayn auksionlar;
- soliqdan ozod xayriya ehsonlari.

UzCasting katalogi bularning **birortasiga ham** tushmaydi.

### 2.3. Nega Uzum Bank'ning o'zi Play'da turibdi

Qoida kompaniyaga emas, **tovarga** qo'llanadi. Uzum Bank ilovasida odam bank
operatsiyasi, o'tkazma va kommunal to'lov qiladi — bular istisnolar ro'yxatida.
UzCasting'da esa odam **ilova ichida ko'riladigan video** uchun to'laydi.

### 2.4. Qonuniy yo'l — consumption-only ilova

> «Yes. Google Play allows any app to be consumption-only, even if it is part
> of a paid service. For example, a user could log in when the app opens and
> access content paid for somewhere else.»

Shart:

> «consumption-only means that any product(s) or service(s), whether digital or
> physical, cannot be purchased from within the app.»

Ilova ichida saytni **matn bilan** tilga olish mumkin (havolasiz). Google'ning
o'z misollari:

> «Go to our website to upgrade your subscription to Premium»
> «This movie isn't available to rent in the app. However, any movie you rent
> through our website.com will be immediately available to view in the app»

Ilovadan tashqarida esa cheklov yo'q:

> «Outside of your app, you are free to communicate with your users about
> alternative purchase options. You can use email marketing and other channels
> outside of the app…»

### 2.5. O'zbekiston uchun muqobil billing yo'q

User Choice Billing (ilova ichida Play yonida o'z to'lov tizimi) faqat
belgilangan mamlakatlarda ishlaydi: YeIH, Avstraliya, Braziliya, Indoneziya,
Yaponiya, JAR, Buyuk Britaniya, AQSh. **O'zbekiston ro'yxatda yo'q** — ya'ni
ilova ichida Uzum Bank'ni Play bilan yonma-yon qo'yish ham mumkin emas.

---

## 3. UzCasting mahsulotlari bo'yicha qaror

| Mahsulot | Google tasnifi | Ilova ichida | Saytda Uzum Bank |
|---|---|---|---|
| Premium (1 / 3 / 6 / 12 oy) | video subscription | ❌ faqat Play Billing | ✅ |
| Bitta qism | digital content | ❌ faqat Play Billing | ✅ |
| Bitta premyera | digital content | ❌ faqat Play Billing | ✅ |
| Stars / tangalar paketi | virtual currency | ❌ faqat Play Billing | ✅ |
| Reklamasiz tomosha | app functionality | ❌ faqat Play Billing | ✅ (Premium ichida) |
| Donat (Stars sarflash) | — | ⚠️ §5.3 ga qarang | ✅ |
| Kasting e'lonlari | bepul | to'lov yo'q | to'lov yo'q |

⚠️ **Donat istisnosi bizni qutqarmaydi.** Google donatni peer-to-peer deb
hisoblaydi faqat agar pulning 100 % ijodkorga borsa **va** u hech qanday raqamli
narsa (`stickers, badges, special emojis`) bermasa. Bizda Stars oldindan paket
bilan sotiladi (virtual valyuta) va reyting/maqom beradi — ikkala shart ham
bajarilmaydi.

---

## 4. Uzum Bank'ni ulash sxemasi

### 4.1. Oqim

```
Foydalanuvchi                   uzcasting.com (sayt)            Uzum Bank
─────────────                   ────────────────────            ─────────
1. Saytga kiradi (o'sha raqam)
2. Tarif / qism tanlaydi  ───►  3. Buyurtma yaratadi
                                   PaymentProvider.init()  ───►  4. To'lov sahifasi
                                                                    (Uzcard, Humo,
                                                                     Visa, MC, Uzum)
                                6. Webhook: to'lov tasdiqlandi ◄─ 5. Odam to'laydi
                                7. Entitlement yoziladi
                                   (Premium / qism / Stars)
8. Ilovani ochadi
   ↓
Ilova GET /watch, /me ─────────► 9. Server "ruxsat bor" deydi
   ↓
10. Video ochiladi — ilova hech narsa sotmadi
```

Muhim: **hisob bitta** — saytda ham, ilovada ham bir xil telefon raqami bilan
kirish. Shunda saytdagi xarid ilovada avtomatik ko'rinadi.

### 4.2. Uzum Bank mahsulotlaridan qaysi biri

Uzum Bank rasmiy hujjatlarida (`developer.uzumbank.uz`) onlayn to'lov uchun:

| Mahsulot | Nima | Bizga |
|---|---|---|
| **Checkout API** | sayt, ilova yoki bot uchun to'lov formasi: karta to'lovi, karta bog'lash, qaytarish, status, fiskalizatsiya | ✅ **saytdagi to'lov uchun asosiy** |
| **Merchant API** | odam **Uzum Bank ilovasida** xizmatni tanlaydi, bizning server webhook'larni qayta ishlaydi | ✅ qo'shimcha kanal (ilovamizdan tashqarida) |
| **Fiscalization** | sotuv va qaytarish bo'yicha chek, soliq tizimiga yuborish | ✅ qonun bo'yicha kerak |

⚠️ Merchant API — Uzum Bank ilovasi ichidagi to'lov. Bu **bizning** ilovamiz
emas, shuning uchun Google qoidasiga tegmaydi. Lekin UzCasting ilovasidan
Uzum Bank ilovasiga **deeplink** qo'yish taqiqlangan.

Aniq endpointlar, imzo va test muhiti — Uzum Bank merchant shartnomasidan keyin
beriladigan rekvizitlar va `developer.uzumbank.uz` dagi hujjat bo'yicha. Bu
hujjatda ular ataylab yozilmagan.

### 4.3. Backend'da tayyor turgan joy

- `backend/.../Cms/Payment/PaymentProvider.java` — abstraksiya bor
  (`init(orderId, amount, currency)` → `redirectUrl`). Uzum Bank uchun
  `UzumBankProvider` shu interfeysni amalga oshiradi,
  `app.payment.provider=uzum`.
- `PackagePurchaseService` / `DonationController` — `PAYMENT_SYSTEM` hozir
  503 qaytaradi. Ulangandan keyin shu joy ishga tushadi.
- ⚠️ Summa — `BigDecimal`, valyuta `UZS`. Webhook **idempotent** bo'lishi
  shart: Uzum bir to'lovni qayta yuborsa, Premium ikki marta uzaymasin.
- ⚠️ Entitlement faqat **webhook** kelgandan keyin yoziladi, `redirect`
  sahifasiga qaytgandan keyin emas — foydalanuvchi URL'ni qo'lda ochishi mumkin.

---

## 5. Ilovada nima qilish mumkin va nima mumkin emas

### 5.1. Jadval

| Ilova ichida | Mumkinmi |
|---|---|
| Sotib olingan kontentni ko'rsatish | ✅ |
| «Premium faol, 12.10.2026 gacha» | ✅ |
| To'lovlar tarixi (faqat ko'rish) | ✅ |
| Promokodni faollashtirish (pulsiz) | ✅ |
| Matn: «Premium'ni uzcasting.com saytida rasmiylashtirasiz — ilovada darhol ochiladi» | ✅ havolasiz, bosilmaydigan matn |
| «Sotib olish», «Tanlash», «To'ldirish» tugmalari | ❌ |
| Saytga bosiladigan havola, tugma, QR-kod | ❌ |
| Uzum Bank / Payme / Click logotiplari va nomlari to'lov konteksida | ❌ |
| To'lov formasi bilan webview | ❌ |
| Uzum Bank ilovasiga deeplink | ❌ |
| Push/banner: «Saytda 20 % arzon» | ❌ (ilova ichida). Email, Telegram-kanal, SMS — ✅ |

### 5.2. Hozirgi build'da tuzatiladigan joylar

Hozir buildda ishlaydigan to'lov yo'q — bu toza holat. Lekin **B varianti**
tanlanganda quyidagilar Google tekshiruvchisi uchun «ilova ichida xarid bor»
yoki «boshqa to'lov tizimiga yo'naltirish» bo'lib o'qiladi:

| Fayl | Hozir | Qilinadi |
|---|---|---|
| `mobile/app/(tabs)/profile.tsx:139` | «To'ldirish» qatori, izohi `Click · Payme · Uzum · Stripe` (`uz.json:176`) | qator olib tashlanadi; to'lov tizimlari nomlari ilovada qolmaydi |
| `mobile/app/subscription/tariffs.tsx:131` | o'chirilgan «Tanlash» tugmasi | tugma olib tashlanadi |
| `mobile/app/subscription/tariffs.tsx:60` | «To'lov tez orada ulanadi» | «Premium uzcasting.com saytida rasmiylashtiriladi va ilovada darhol ochiladi» — havolasiz |
| `mobile/src/features/content/LockedPanel.tsx:71-87` | narxli «sotib olish» va «obuna bo'lish» tugmalari | tugmalar olib tashlanadi, faqat izoh matni |
| `mobile/src/features/content/DonateSheet.tsx:171-201` | «N yulduz yuborish» + «to'lov ilovada hali ulanmagan» | §5.3 bo'yicha qaror |
| `frontend/src/viewer/pages/WatchPage.jsx:26` | «to'lov oqimi mobil ilovada» | teskarisi: to'lov **saytda** quriladi |

⚠️ Bitta ishlaydigan xarid tugmasi ilovani consumption-only'dan chiqaradi —
va o'sha zahoti saytni tilga olgan har bir matn ham qoidabuzarlikka aylanadi.

### 5.3. Stars va donat — ehtiyotkor yo'l

Stars **saytda** Uzum Bank orqali sotiladi — bu aniq ruxsat.

Ilova ichida Stars'ni **sarflash** (donat yuborish) — kulrang zona. Google
«ilovada sotib bo'lmaydi» deydi, sarflash haqida esa faqat *yutib olingan*
ballar uchun aniq yozadi. Pul bilan to'ldirilgan balansdan ilova ichida
kontent yoki maqom olish tekshiruvchiga in-app xarid bo'lib ko'rinishi mumkin.

**Tavsiya:** birinchi relizda donat va balansdan qism ochish **saytda**
bo'ladi, ilova faqat natijani (reyting, «siz qo'llab-quvvatladingiz»)
ko'rsatadi. Ilova ichida sarflashni keyinroq, alohida qaror bilan qo'shish.

---

## 6. Play Console'ga nima yoziladi

### 6.1. App access → tekshiruvchi uchun izoh (inglizcha)

```
UzCasting is a consumption-only (reader) app.

No digital goods, subscriptions or virtual currency can be purchased inside
the app. The app has no purchase buttons, no payment forms, no webviews and
no links to external payment pages.

Paid content (Premium subscription, episodes, premieres) is purchased only on
our website uzcasting.com. Users sign in to the app with the same phone number
and access content they have already paid for elsewhere, as permitted by the
Google Play Payments policy FAQ ("Google Play allows any app to be
consumption-only, even if it is part of a paid service").

Test account (Premium already active): <demo phone> / code <demo code>
```

### 6.2. Store listing

- Tavsifda mahsulot **halol** yoziladi: seriallar, qisqa filmlar, Premium
  obuna. Pullik kontentni yashirish — alohida qoidabuzarlik (chalg'ituvchi
  metama'lumotlar), `PLAY_STORE_UZ.md` §15.9.
- Tavsifda to'lov usullari va sayt narxlari **yozilmaydi**.
- «Contains in-app purchases» belgisi **qo'yilmaydi** — ilovada xarid yo'q.

### 6.3. Data safety

Karta ma'lumotlari **ilova ichida** kiritilmaydi → «Financial info»
yig'ilmaydi. ⚠️ Bu javob faqat to'lov haqiqatan saytda bo'lsa to'g'ri; ilovada
to'lov formasi paydo bo'lsa, forma ham o'zgartiriladi.

---

## 7. Foydalanuvchi uchun to'lov siyosati (sayt sahifasi uchun qoralama)

> Joylashuv taklifi: `uzcasting.com/tolov-siyosati`, `/maxfiylik` yonida.
> Bu sahifa **saytda** turadi; ilovadan unga havola qo'yilmaydi.

**UzCasting to'lov siyosati**

1. **Qayerda to'lanadi.** Premium obuna, alohida qismlar, premyeralar va
   Stars faqat `uzcasting.com` saytida sotib olinadi. Mobil ilova ichida
   to'lov qabul qilinmaydi.
2. **To'lov usullari.** Uzum Bank orqali: Uzcard, Humo, Visa, Mastercard
   kartalari va Uzum Bank ilovasi. Narxlar so'mda (UZS).
3. **Ilovada ochilishi.** Xarid saytga kirgan telefon raqamiga bog'lanadi.
   Mobil ilovaga o'sha raqam bilan kirsangiz, kontent darhol ochiladi.
4. **Obuna muddati.** Premium tanlangan muddatga (1, 3, 6 yoki 12 oy) beriladi.
   *[Avtomatik uzaytirish bo'lsa — shu yerda qanday o'chirilishi yoziladi.]*
5. **Chek.** Har bir to'lov bo'yicha elektron chek beriladi.
6. **Qaytarish.** *[Buyurtmachi qoidasi: qaysi holatda, necha kun ichida,
   qanday murojaat qilinadi.]*
7. **Aloqa.** *[Qo'llab-quvvatlash kontakti.]*

⚠️ 4, 6 va 7-bandlar buyurtmachi qarorisiz e'lon qilinmaydi — refund qoidalari
ТЗ'da yozilmagan (`tz/overal.md` §8, 10-savol).

---

## 8. Qizil chiziqlar

Quyidagilarning birortasi ham qilinmaydi — ularning narxi rad javobi emas,
**ilovani olib tashlash** va takrorlansa buyurtmachi nomidagi **dasturchi
hisobini bloklash**:

- ilova ichida Uzum Bank to'lov tugmasi, formasi yoki webview;
- «balansni to'ldirish» ekrani — virtual valyuta ham Play Billing talab qiladi;
- Premium'ni «sovg'a kartasi» deb qayta o'rab ilovada sotish;
- store listing'da pullik kontentni yashirish;
- «raqobatchida ham shunday» degan dalilga tayanish — bu ruxsat emas,
  ular hali tekshiruvga tushmaganining izohi (`PLAY_STORE_UZ.md` §15.9).

---

## 9. Chek-list

**Buyurtmachi**
- [ ] B variantini (saytda to'lov, ilova — consumption-only) tasdiqlash
- [ ] Uzum Bank bilan merchant shartnomasi, Checkout API rekvizitlari
- [ ] Refund qoidasi, avtomatik uzaytirish bo'yicha qaror
- [ ] Stars sarflash qayerda bo'lishi bo'yicha qaror (§5.3)

**Backend**
- [ ] `UzumBankProvider implements PaymentProvider`
- [ ] Webhook: imzo tekshiruvi, idempotentlik, entitlement yozish
- [ ] Fiskalizatsiya
- [ ] `PAYMENT_SYSTEM` yo'lini 503 dan ishlaydigan holatga o'tkazish

**Sayt (`frontend/src/viewer`)**
- [ ] Tariflar va xarid sahifasi, Uzum Bank'ga redirect, natija sahifasi
- [ ] `/tolov-siyosati` sahifasi

**Mobil**
- [ ] §5.2 jadvalidagi hamma joy tuzatildi
- [ ] Ilovada birorta ham xarid tugmasi, havola va to'lov tizimi nomi yo'qligi
      tekshirildi (`grep -ri "uzum\|payme\|click" mobile/src mobile/app`)
- [ ] Play Console: App access izohi (§6.1), listing (§6.2), Data safety (§6.3)

---

## 10. Manbalar (15.09.2026 da tekshirildi)

- Google Play to'lovlar siyosati —
  https://support.google.com/googleplay/android-developer/answer/9858738
- Siyosat FAQ (consumption-only, anti-steering, donatlar, virtual valyuta) —
  https://support.google.com/googleplay/android-developer/answer/10281818
- User Choice Billing mamlakatlari —
  https://support.google.com/googleplay/android-developer/answer/12570971
- Uzum Bank dasturchilar hujjati (Checkout API, Merchant API, Fiscalization) —
  https://developer.uzumbank.uz/en/
- Uzum Bank merchantlar sahifasi — https://merchants.uzumbank.uz/en/
- Ichki tadqiqot — `mobile/docs/PLAY_STORE_UZ.md` §15
