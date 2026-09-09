-- V35 — «yoqdi» tugmasi va ko'rishlar soni ilovada.
--
-- Buyurtmachi (07.09.2026): «like bosish va prasmotrlani mobilga qilish
-- kerak».
--
-- ⚠️ «Yoqdi» — «Saqlanganlar» EMAS. Sevimlilar (`cms_user_favorite`) —
-- bu odamning shaxsiy ro'yxati, uni faqat egasi ko'radi. «Yoqdi» esa
-- ommaviy hisoblagich: uni hamma ko'radi va u kontentning o'zi haqida
-- gapiradi. Ikkalasini bitta jadvalga qo'shish degani — ro'yxatdan
-- o'chirgan odam kontentdan «yoqdi» ni ham olib qo'yadi, va aksincha.
create table if not exists cms_content_like (
    id          bigserial primary key,
    content_id  bigint      not null references cms_content (id) on delete cascade,
    user_id     uuid        not null references users (id) on delete cascade,
    created_at  timestamp(6) not null default now(),

    -- ⚠️ Asosiy himoya: bitta odam bitta kontentga bir marta. Ikki marta
    -- bosilsa yoki so'rov takrorlansa (aloqa uzilib qayta yuborilsa),
    -- baza ikkinchi yozuvni qabul qilmaydi.
    constraint uq_content_like_user unique (content_id, user_id)
);

-- Kontent sahifasi «nechta yoqdi» ni har safar so'raydi; foydalanuvchi
-- bo'yicha esa «men bosganmanmi» tekshiriladi.
create index if not exists idx_content_like_content on cms_content_like (content_id);
create index if not exists idx_content_like_user on cms_content_like (user_id, created_at);

-- Hisoblagich kontentning o'zida: sahifa ochilganda count(*) qilmaslik
-- uchun. Haqiqat manbai baribir jadval — sanoq undan qayta hisoblanishi
-- mumkin.
alter table cms_content
    add column if not exists like_count bigint not null default 0;

-- ⚠️ `view_count` ustuni bor edi, lekin uni HECH KIM oshirmasdi: hodisalar
-- faqat `cms_content_daily_statistic` ga tushardi. Ya'ni ustun nol turardi
-- va admin paneldagi «views» tartiblash ham shu nolni tartiblardi.
--
-- Endi uni `AnalyticsService.aggregate()` oshiradi. Mavjud kunlik
-- jamlanmalar bo'yicha bir marta to'ldirib qo'yamiz, aks holda ilovada
-- eski kontent nol ko'rish bilan turadi.
update cms_content c
set view_count = coalesce((select sum(s.views)
                           from cms_content_daily_statistic s
                           where s.content_id = c.id), 0)
where coalesce(c.view_count, 0) = 0;
