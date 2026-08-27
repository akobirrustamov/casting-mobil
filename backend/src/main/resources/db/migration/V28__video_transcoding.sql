-- ==========================================================================
--  V28 — VIDEO TRANSCODING: HOLAT VA NATIJA
-- ==========================================================================
--
--  Ikki narsa qo'shiladi:
--    1. media_asset ga transcoding NATIJASI (nima hosil bo'ldi);
--    2. cms_transcoding_job — ishning O'ZI (qaysi bosqichda, xato bormi).
--
--  ------------------------------------------------------------------------
--  ⚠️ NEGA HOLAT media_asset DA SAQLANMAYDI
--  ------------------------------------------------------------------------
--  Dastlabki reja media_asset ga `processing_status` qo'shish edi. Undan
--  voz kechildi: o'shanda holat IKKI joyda yashardi (media va ish) va
--  birinchi nosozlikdayoq ajralardi — masalan ish FAILED bo'lib, media
--  PROCESSING bo'lib qolardi.
--
--  Endi yagona manba — cms_transcoding_job. media_asset faqat NATIJANI
--  saqlaydi: hls_master_key bor bo'lsa HLS tayyor, yo'q bo'lsa yo'q.
--
--  ------------------------------------------------------------------------
--  ⚠️ NEGA HAR MEDIA UCHUN BITTA ISH (unique media_id)
--  ------------------------------------------------------------------------
--  Qayta urinish yangi qator yaratmaydi, mavjudini yangilaydi va
--  `attempts` ni oshiradi.
--
--  Tarixni saqlash uchun har urinishga alohida qator ham qo'yish mumkin
--  edi, lekin unda «bu medianing HOZIRGI holati nima» degan savol
--  «eng oxirgi qatorni top» ga aylanardi. Kutubxona sahifasi 40 ta
--  media ko'rsatadi — ularning har biri uchun oxirgi qatorni izlash
--  oynali so'rov yoki N+1 bo'lardi.
--
--  Urinishlar TARIXI kerak bo'lsa u audit jurnaliga yoziladi.
--
--  ------------------------------------------------------------------------
--  ⚠️ NEGA `original_object_key` QO'SHILMADI
--  ------------------------------------------------------------------------
--  Rejada bor edi, lekin u ortiqcha: `storage_key` ning O'ZI original
--  faylning kaliti. Ikkinchi ustun bir xil qiymatni saqlab, ular
--  ajralib ketishi uchun yana bir imkoniyat yaratardi.
--
--  ------------------------------------------------------------------------
--  MAVJUD MA'LUMOTLARGA TA'SIRI
--  ------------------------------------------------------------------------
--  Yo'q. Barcha yangi ustunlar `null` bo'lishi mumkin, ish jadvali esa
--  bo'sh boshlanadi. Ilgari yuklangan videolar HLS'siz qoladi va eski
--  yo'l (`/media/{id}/raw`) orqali ochilishda davom etadi.
-- ==========================================================================


-- --------------------------------------------------------------------------
--  1. NATIJA — media_asset
-- --------------------------------------------------------------------------

-- HLS master playlist kaliti. null = HLS yo'q, eski yo'ldan beriladi.
--
-- ⚠️ To'liq URL EMAS, aynan KALIT. CDN domeni sozlamadan olinadi va
-- runtime'da qo'shiladi — shunda domen almashtirish bitta sozlama
-- o'zgarishi bo'ladi, ming qatorli UPDATE emas.
alter table media_asset
    add column hls_master_key varchar(512);

-- ffprobe aniqlagan kodeklar.
--
-- Nega kerak: profil tanlashda va kelajakda «bu videoni qayta
-- transcoding qilish kerakmi» degan savolga javob berishda ishlatiladi.
-- Masalan AV1 ga o'tilsa, H.264 dagilarni topish shu ustun orqali
-- bo'ladi.
alter table media_asset
    add column video_codec varchar(32);

alter table media_asset
    add column audio_codec varchar(32);


-- --------------------------------------------------------------------------
--  2. ISH — cms_transcoding_job
-- --------------------------------------------------------------------------

create table cms_transcoding_job (
    id bigserial not null,

    -- Har media uchun BITTA ish. Qayta urinish shu qatorni yangilaydi.
    media_id bigint not null,

    -- QUEUED · PROBING · TRANSCODING · UPLOADING · READY · FAILED
    status varchar(16) not null,

    -- 0..100. Faqat ko'rsatish uchun — mantiq bunga tayanmaydi.
    progress integer not null default 0,

    -- Nechanchi urinish. Chegaradan oshsa FAILED va qayta olinmaydi.
    attempts integer not null default 0,

    -- ⚠️ Xato MATNI saqlanadi, chunki admin panelda «nima uchun
    -- yiqildi» degan savolga javob kerak. Faqat holatni ko'rsatish
    -- adminni logga qarashga majbur qilardi, logga esa uning kirishi
    -- yo'q.
    error varchar(2000),

    started_at timestamp(6),
    finished_at timestamp(6),
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,

    primary key (id),
    constraint uq_transcoding_job_media unique (media_id),
    constraint fk_transcoding_job_media foreign key (media_id)
        references media_asset (id) on delete cascade
);

-- Worker shu bo'yicha navbatdagi ishni oladi: eng eski QUEUED.
create index idx_transcoding_job_queue on cms_transcoding_job (status, created_at);
