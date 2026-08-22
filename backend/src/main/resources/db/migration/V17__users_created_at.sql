-- ==========================================================================
--  V17 — FOYDALANUVCHINING RO'YXATDAN O'TGAN SANASI
-- ==========================================================================
--
--  ТЗ §35 foydalanuvchilar ro'yxatida `createdAt` ni talab qiladi.
--  Bunday ustun HECH QAYERDA yo'q edi.
--
--  ⚠️ NIMA UCHUN `cms_user_account.created_at` YARAMAYDI
--
--  U bor, lekin BOSHQA narsani bildiradi. Hisob satri DANGASA yaratiladi:
--  faqat admin biror amal qilganda (bloklash, premium berish). Ya'ni:
--
--    • ko'pchilik foydalanuvchida u umuman YO'Q — ustun bo'sh chiqardi;
--    • bo'lganda ham u «admin birinchi marta tekkan vaqt» ni bildiradi.
--
--  2020-yilda ro'yxatdan o'tib, 2026-yilda bloklangan foydalanuvchi
--  ro'yxatda «2026» bo'lib ko'rinardi. Bu bo'sh katakdan ham yomon: admin
--  raqamga ISHONADI.
--
--  ⚠️ MAVJUD SATRLAR TO'LDIRILMAYDI
--
--  Ular qachon ro'yxatdan o'tganini BILMAYMIZ. Har qanday qiymat — o'ylab
--  topilgan sana bo'lardi. `null` halol javob: «ma'lum emas».
--
--  Yangi foydalanuvchilar `@PrePersist` orqali avtomatik to'ldiriladi,
--  ya'ni ro'yxatdan o'tish yo'lining o'zi o'zgartirilmaydi.
--
--  ⚠️ USTUN QO'SHILADI, HECH NARSA O'CHIRILMAYDI — eski casting kodi
--  bu ustunni bilmaydi va undan ta'sirlanmaydi.
-- ==========================================================================

alter table users add column created_at timestamp(6);

-- Ro'yxat sana bo'yicha tartiblanadi (ТЗ §35).
create index idx_users_created_at on users (created_at);
