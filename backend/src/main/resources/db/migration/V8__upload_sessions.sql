-- ==========================================================================
--  V8 — BO'LAKLAB YUKLASH SESSIYALARI
-- ==========================================================================
--
--  cms_upload_session — katta video fayllarni bo'laklab yuklash holati.
--
--  NEGA KERAK
--  Bitta multipart so'rov bilan epizod videosini yuborib bo'lmaydi:
--  prod chegarasi 50 MB, haqiqiy epizod esa yuz megabaytdan gigabaytgacha.
--  Bitta ulkan so'rov uzilsa hammasi boshidan boshlanardi.
--
--  QABUL QILINGAN BO'LAKLAR BU YERDA SAQLANMAYDI
--  Ular diskda .part fayllari sifatida yotadi va o'sha ro'yxat yagona
--  haqiqat manbai. Bazada dublikat holat saqlansa, ikkalasi bir-biriga mos
--  kelmay qolishi mumkin (yozildi -> xato -> baza "bor" deydi, disk "yo'q").
--
--  Bu jadval VAQTINCHALIK ma'lumot: tugagan va tashlab ketilgan sessiyalar
--  tozalanadi. Moliyaviy yoki audit ahamiyati yo'q.
-- ==========================================================================

create table cms_upload_session (
    id varchar(36) not null,
    original_filename varchar(255) not null,
    mime_type varchar(128),
    size_bytes bigint not null,
    chunk_size integer not null,
    total_chunks integer not null,
    folder varchar(64) not null,
    status varchar(16) not null,
    created_by uuid not null,
    created_at timestamp(6) not null,
    completed_at timestamp(6),
    media_asset_id bigint,
    primary key (id)
);

-- Tozalash vazifasi shu ikki ustun bo'yicha qidiradi.
create index idx_upload_session_cleanup on cms_upload_session (status, created_at);
