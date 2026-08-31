-- V29 — foydalanuvchining sevimlilar ro'yxati.
--
-- Ilgari sevimlilar FAQAT telefonda saqlanardi
-- (`mobile/src/features/favorites/store.ts`). Ilovani qayta o'rnatsa
-- yo'qolardi, ikkinchi qurilmada bo'sh bo'lardi, telefon almashsa
-- hammasi ketardi. Foydalanuvchi buni ma'lumot yo'qolishi deb his
-- qiladi.
--
-- `type` + `target_id` — `cms_purchase` dagi bilan bir xil naqsh.
-- Bugun faqat CREATOR ishlatiladi, lekin bu video platformasi va
-- «saqlangan filmlar» ertami-kechmi kerak bo'ladi. Tursiz jadval
-- o'sha kuni qaytadan yozilardi.
--
-- ⚠️ `target_id` ga chet el kaliti ATAYLAB qo'yilmagan: CREATOR
-- muzlatilgan eski modulning `casting_user` jadvaliga ishora qiladi,
-- va yangi jadvalni uning hayotiy sikliga bog'lash noto'g'ri bo'lardi.
-- Osilib qolgan yozuv zararsiz — klient mavjud ro'yxat bo'yicha
-- filtrlaydi.
create table if not exists cms_user_favorite (
    id         bigserial primary key,
    user_id    uuid        not null references users (id) on delete cascade,
    type       varchar(16) not null,
    target_id  bigint      not null,
    created_at timestamp   not null
);

-- ⚠️ Takroriy qo'shishning oldini oladi.
--
-- Klient «sevimliga qo'shish» ni ikki marta yuborishi mumkin (sekin
-- tarmoq, ikki marta bosish). Bu cheklovsiz bitta ijodkor ro'yxatda
-- ikki marta ko'rinardi.
create unique index if not exists uq_favorite_user_target
    on cms_user_favorite (user_id, type, target_id);

-- Ro'yxatni o'qish uchun: bitta foydalanuvchi, bitta tur, yangisi
-- yuqorida.
create index if not exists idx_favorite_user_type
    on cms_user_favorite (user_id, type, created_at);
