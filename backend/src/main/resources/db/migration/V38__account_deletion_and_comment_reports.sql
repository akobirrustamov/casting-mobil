-- V38 — Google Play talab qiladigan ikkita narsa: hisobni o'chirish va
-- izohga shikoyat.
--
-- <h2>Nega bu ikkalasi bitta migratsiyada</h2>
-- Ikkalasi ham bitta sababdan kelib chiqadi: ilovani Google Play'ga
-- chiqarish. «Data deletion» siyosati hisob yaratish mumkin bo'lgan
-- joyda uni o'chirishni ham talab qiladi, UGC siyosati esa begona
-- izohga shikoyat qilish yo'lini. Ularsiz ilova rad etiladi.
--
-- Batafsil: roadmap/PLAY_BACKEND_TASKS.md

-- ---------------------------------------------------------------- o'chirish
--
-- ⚠️ Hisob HARD DELETE qilinmaydi. `users` qatori o'chirilsa, unga
-- bog'langan to'lovlar, obunalar va moliyaviy tarix ham cascade bilan
-- ketardi — ularni esa qonun talab qilgan muddat davomida saqlash kerak
-- (maxfiylik siyosatining 4-bo'limi shunday va'da qilgan).
--
-- Buning o'rniga shaxsiy ma'lumot tozalanadi (telefon, email, ism),
-- sessiyalar bekor qilinadi, hisob esa «o'chirilgan» deb belgilanadi.
-- Shu sana ham kerak: qachon o'chirilgani so'ralsa, javob bo'lsin.
alter table cms_user_account
    add column if not exists deleted_at timestamp(6);

-- ---------------------------------------------------------------- shikoyat
--
-- `cms_comment.reports_count` ustuni BOSHIDAN bor edi, admin paneldagi
-- moderatsiya ro'yxati esa `reportedOnly` filtri bilan uni allaqachon
-- o'qiydi. Yetishmagan narsa — shikoyatning o'zi: hisoblagichni
-- oshiradigan hech kim yo'q edi, shuning uchun filtr hamisha bo'sh
-- ro'yxat qaytarardi.
--
-- ⚠️ Alohida jadval, `reports_count` ni oshirishning o'zi emas: aks
-- holda bir odam bitta izohga yuz marta shikoyat qilib, uni
-- moderatsiya navbatining tepasiga chiqarib qo'yardi.
create table if not exists cms_comment_report (
    id          bigserial primary key,
    comment_id  bigint       not null references cms_comment (id) on delete cascade,
    user_id     uuid         not null references users (id) on delete cascade,

    -- SPAM · INSULT · ADULT · OTHER ({@code CommentReportReason}).
    reason      varchar(16)  not null,
    created_at  timestamp(6) not null default now(),

    -- Asosiy himoya: bitta odam bitta izohga bir marta. Takroriy so'rov
    -- 409 oladi, hisoblagich esa qo'shimcha oshmaydi.
    constraint uq_comment_report_user unique (comment_id, user_id)
);

-- ⚠️ `comment_id` ga ALOHIDA indeks ATAYLAB qo'yilmadi: yuqoridagi
-- unique cheklovi aynan shu ustundan boshlanadi va u bo'yicha
-- so'rovlarga o'zi xizmat qiladi (ТЗ §56 — «sababsiz har bir fieldga
-- index qo'yma»).
