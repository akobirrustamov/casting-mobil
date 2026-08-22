-- V25 — berilgan refresh tokenlar ro'yxati (ТЗ §61).
--
-- JWT o'z-o'zidan tekshiriladi, ya'ni server uni bekor qila olmaydi.
-- Bu jadvalsiz «chiqish» faqat klient tomonida bo'lardi: o'g'irlangan
-- token muddati tugaguncha ishlayverardi.
--
-- Token MATNI saqlanmaydi — faqat uning `jti` identifikatori. Baza
-- o'qilgan taqdirda ham undan sessiyani tiklab bo'lmasin.
create table if not exists refresh_token (
    id          uuid primary key,
    user_id     uuid        not null,
    expires_at  timestamp   not null,
    revoked_at  timestamp,
    replaced_by uuid,
    created_at  timestamp   not null,
    user_agent  varchar(512),
    ip          varchar(64)
);

create index if not exists idx_refresh_user on refresh_token (user_id);
create index if not exists idx_refresh_expires on refresh_token (expires_at);
