-- ==========================================================================
--  V10 — XODIM PROFILI
-- ==========================================================================
--
--  cms_staff_profile — xodimga oid holat va metadata.
--
--  NEGA ALOHIDA JADVAL
--  Eski `users` jadvali casting moduliga tegishli va MUZLATILGAN
--  (OldCastingFrozenTest). Unga ustun qo'shish eski mijozlarni
--  (Telegram bot, eski admin sayti) xavf ostiga qo'yardi.
--
--  `cms_user_account` ham to'g'ri kelmaydi: u ILOVA foydalanuvchisi uchun
--  (premium, balans, qurilmalar). Xodim metadatasini u yerga aralashtirsak,
--  ikkita mutlaqo boshqa hayot sikli bitta jadvalda yashardi.
--
--  HOLAT MODELI
--    ACTIVE      — ishlayapti
--    INACTIVE    — faolsizlantirilgan (soft delete o'rnida)
--    BLOCKED     — vaqtincha to'xtatilgan
--
--  Hard delete ATAYLAB yo'q: xodim o'chirilsa, uning audit yozuvlaridagi
--  actor_id kimga tegishli ekani noma'lum bo'lib qolardi va o'tmishdagi
--  amallarni tekshirib bo'lmasdi.
-- ==========================================================================

create table cms_staff_profile (
    id bigserial not null,
    user_id uuid not null unique,
    status varchar(16) not null,
    -- Kim yaratgani. NULL = tizim (AutoRun yoki seeder).
    created_by uuid,
    created_at timestamp(6) not null,
    -- Oxirgi muvaffaqiyatli kirish. NULL = hali kirmagan.
    last_login_at timestamp(6),
    -- Faolsizlantirish yoki bloklash tafsilotlari.
    status_changed_at timestamp(6),
    status_changed_by uuid,
    status_reason varchar(500),
    primary key (id)
);

alter table if exists cms_staff_profile
    add constraint fk_staff_profile_user foreign key (user_id) references users;

-- Ro'yxat holat va rol bo'yicha filtrlanadi, sana bo'yicha saralanadi.
create index idx_staff_profile_status on cms_staff_profile (status, created_at);
