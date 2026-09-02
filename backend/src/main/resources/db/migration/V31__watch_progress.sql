-- V31 — «Ko'rishda davom eting»: qayerda to'xtaganini eslab qolish.
--
-- Ilgari bu holat HECH QAYERDA saqlanmasdi. Odam ikki soatlik filmni
-- 1:32:45 da to'xtatsa, ertasiga qaytib kelganda 0:00 dan boshlashi
-- kerak edi va qayerda qolganini O'ZI qidirib topishi kerak edi.
--
-- `type` + `target_id` — `cms_purchase` va `cms_user_favorite` dagi
-- bilan AYNI naqsh. Sabab shu yerda ayniqsa aniq: ko'rish ikki xil
-- endpointdan boradi va ikkalasi ham kerak —
--
--   /api/v1/app/watch/{episodeId}          → EPISODE
--   /api/v1/app/watch/content/{contentId}  → CONTENT (SINGLE: film, klip)
--
-- Faqat `episode_id` li jadval filmlarni butunlay tashlab ketardi, ular
-- esa aynan uzun va aynan davom ettirish kerak bo'ladiganlari.
create table if not exists cms_watch_progress (
    id               bigserial primary key,
    user_id          uuid        not null references users (id) on delete cascade,
    type             varchar(16) not null,
    target_id        bigint      not null,

    -- Qayerda to'xtadi. Butun soniya yetarli — kadr aniqligi kerak emas
    -- va u faqat raqamni kattalashtirardi.
    position_seconds integer     not null,

    -- ⚠️ Davomiylik SHU YERDA nusxalanadi.
    --
    -- Uni qismdan yoki kontentdan olish mumkin edi, lekin «davom eting»
    -- ro'yxati uchun har satrga JOIN kerak bo'lardi — va ikki xil
    -- jadvalga, turga qarab. Nusxa ro'yxatni bitta so'rovga aylantiradi.
    --
    -- Bo'sh bo'lishi mumkin: transkodlanmagan videoda davomiylik hali
    -- noma'lum. Unda foiz ko'rsatilmaydi, pozitsiya esa baribir ishlaydi.
    duration_seconds integer,

    -- Odam QO'LDA tanlagan sifat: `1080p`, `720p`, `480p` yoki `auto`.
    -- Keyingi safar o'sha sifatda ochiladi.
    --
    -- ⚠️ Bu TANLOV, joriy sifat emas. Auto rejimda pleyer sifatni
    -- doim o'zgartiradi va uni saqlash tanlovni yolg'on qilardi.
    quality          varchar(16),

    -- Oxirigacha ko'rilgan. «Davom eting» ro'yxatidan chiqariladi —
    -- tugatilgan filmni qayta taklif qilish xato bo'lardi.
    completed        boolean     not null default false,

    updated_at       timestamp   not null
);

-- ⚠️ Bitta odam + bitta video = BITTA satr.
--
-- Progress har 15 soniyada yuboriladi. Cheklovsiz ikki soatlik film
-- bitta ko'rish uchun ~480 satr qoldirardi va «qayerda to'xtadi»
-- degan savolga javob yo'qolardi.
--
-- Server tomonda yozish shu indeksga tayanadi (upsert).
create unique index if not exists uq_watch_progress_user_target
    on cms_watch_progress (user_id, type, target_id);

-- «Ko'rishda davom eting» ro'yxati: bitta odam, oxirgi ko'rilgani
-- yuqorida. `completed` shartga kiritilgan, chunki so'rov aynan
-- tugallanmaganlarni oladi.
create index if not exists idx_watch_progress_continue
    on cms_watch_progress (user_id, completed, updated_at);
