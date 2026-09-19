-- V40 — casting anketasini mobil ilova foydalanuvchisiga bog'lash.
--
-- Ilgari anketani faqat Telegram bot yuborardi va egasi `telegram_id`
-- orqali tanilardi. Endi mobil ilova ham anketa yuboradi
-- (`/api/v1/app/casting/applications`), u yerda odam Telegram orqali
-- emas, JWT orqali tanilgan — va «mening arizalarim» shu ustun bo'yicha
-- o'qiladi.
--
-- ⚠️ `null` ruxsat etiladi va bu ATAYLAB: bot yuborgan barcha eski va
-- yangi anketalarda bu ustun bo'sh qoladi. Bot oqimi o'zgarmaydi.
--
-- ⚠️ Chet el kaliti XAVFSIZ: ustun yangi, barcha mavjud satrlarda `null`,
-- `null` esa cheklovni buzmaydi — ya'ni migratsiya ishlab chiqarishdagi
-- ma'lumot tufayli yiqila olmaydi.
--
-- `on delete set null` — cascade EMAS. `users` qatori ilova tomonidan
-- o'chirilmaydi (`AccountDeletionService` faqat shaxsni tozalaydi), lekin
-- kimdir qo'lda o'chirsa, anketa admin uchun qolishi kerak: u allaqachon
-- ko'rib chiqilgan, to'langan bo'lishi mumkin. Bog'lanish uziladi, xolos.
alter table casting_user
    add column if not exists app_user_id uuid;

alter table casting_user
    add constraint fk_casting_user_app_user
        foreign key (app_user_id) references users (id) on delete set null;

-- «Mening arizalarim» (yangisi yuqorida) va «kutilayotgan ariza bormi»
-- tekshiruvi. Ikkinchisi faqat birinchi ustunni ishlatadi — prefiks
-- qoidasi bo'yicha shu indeksning o'zi yetadi, alohida indeks kerak emas.
create index if not exists idx_casting_user_app_user_created
    on casting_user (app_user_id, created_at);
