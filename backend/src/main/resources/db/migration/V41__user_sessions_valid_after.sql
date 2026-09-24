-- V41 — majburiy chiqarish (admin: bitta foydalanuvchi yoki ommaviy).
--
-- Refresh tokenni bekor qilish yetarli EMAS: access token 15 daqiqa
-- o'z-o'zidan tekshiriladi va server uni «unutolmaydi». Ya'ni admin
-- «chiqarib yubordim» deb turganda, odam yana chorak soat ishlayverardi.
--
-- Bu ustun — «shu vaqtdan OLDIN berilgan har qanday token yaroqsiz».
-- `MyFilter` foydalanuvchini har so'rovda bazadan o'qiydi, shuning uchun
-- tekshiruv qo'shimcha so'rovsiz ishlaydi.
--
-- `null` — cheklov yo'q (barcha mavjud satrlar). Ustun yangi va bo'sh,
-- ya'ni migratsiya ishlab chiqarishdagi ma'lumot tufayli yiqila olmaydi.
alter table users
    add column if not exists sessions_valid_after timestamp;
