-- V34 — promokod NIMA berishini admin tanlaydi.
--
-- Buyurtmachi (04.09.2026): «promokodlar adminka tomonidan yaratiladi,
-- nima uchun yaratilsa o'shanga ulanib ketaveradigan qilish kerak».
-- Ya'ni kod yaratilayotganda uning maqsadi tanlanadi va berilgan huquq
-- aynan o'shanga bog'lanadi.
--
-- Ikki qiymat bilan boshlaymiz:
--   PREMIUM_DAYS  — N kun Premium (V33 dagi yagona xatti-harakat).
--   CASTING_DAYS  — N kun faqat Casting bo'limiga kirish.
--
-- ⚠️ Nima uchun casting uchun ALOHIDA ustun kerak bo'ldi
--
-- `AccessService.canAccessCasting()` bugungacha shunday yozilgan edi:
-- «casting huquqi = faol Premium». Ya'ni casting kirishini Premiumsiz
-- berishning umuman yo'li yo'q edi. Endi hisobda o'z muddati bor:
-- Premium film va seriallarni ochadi, casting kirishi esa faqat
-- casting bo'limini.
--
-- Ustun NULL bo'lishi mumkin: casting huquqi berilmagan hisoblarda u
-- shunchaki yo'q, va bu xato emas.
alter table cms_user_account
    add column if not exists casting_until timestamp(6);

-- Turi. Mavjud qatorlar PREMIUM_DAYS bo'lib qoladi — V33 da boshqa
-- xatti-harakat yo'q edi.
alter table cms_promocode
    add column if not exists grant_type varchar(24) not null default 'PREMIUM_DAYS';

-- ⚠️ `premium_days` nomi endi yolg'on: casting kodida u Premium
-- bermaydi. Jadval kecha yaratilgan va hali hech qayerda tarqalmagan,
-- shuning uchun nom to'g'rilanadi — noto'g'ri nom bilan yashash uzoq
-- muddatda qimmatroq.
alter table cms_promocode
    rename column premium_days to grant_days;

-- Nima berilgani ishlatilgan payt yoziladi.
--
-- Ilgari bu `cms_subscription` orqali bilinardi, lekin casting huquqi
-- obuna EMAS va u yerga yozilmaydi: aks holda «faol obunachilar» soni
-- casting kodlari hisobiga shishib ketardi. Redemption qatorining o'zi
-- endi savolga javob beradi.
alter table cms_promocode_redemption
    add column if not exists granted_until timestamp(6);
