-- V33 — promokodlar.
--
-- Buyurtmachi qarori (04.09.2026): promokod BEPUL PREMIUM KUNLAR beradi
-- va mavjud obuna muddati USTIGA qo'shiladi. Chegirma, foiz, Yulduz —
-- yo'q: bitta tur, bitta qoida.
--
-- Nima uchun ikki jadval
--   cms_promocode            — kodning o'zi: necha kun, nechta odam,
--                              qachongacha.
--   cms_promocode_redemption — kim, qachon ishlatgan. Bu jadvalsiz «bitta
--                              odam bir marta» qoidasini bajarib
--                              bo'lmaydi, va uni ilova darajasida
--                              tekshirish poyga holatida ishlamaydi:
--                              ikkita parallel so'rov ikkalasi ham «hali
--                              ishlatilmagan» ni ko'radi. Qoida BAZADA —
--                              uk_promocode_user.
--
-- Umumiy limit (max_redemptions) esa qatorni qulflab tekshiriladi
-- (PromocodeRepo.lockByCode) — oxirgi o'ringa ikkita so'rov kelganda
-- faqat bittasi o'tadi.
--
-- Berilgan premium cms_subscription ga PROMO manbasi bilan yoziladi:
-- paid_amount NULL — bu daromad EMAS va hisobotda shunday hisoblanadi
-- (ADMIN_GIFT bilan bir xil).

create table cms_promocode (
    id              bigserial     primary key,
    code            varchar(32)   not null,
    premium_days    integer       not null,
    max_redemptions integer,
    valid_from      timestamp(6),
    valid_until     timestamp(6),
    active          boolean       not null default true,
    note            varchar(255),
    created_by      uuid,
    created_at      timestamp(6)  not null,
    constraint uk_promocode_code unique (code)
);

create table cms_promocode_redemption (
    id              bigserial     primary key,
    promocode_id    bigint        not null references cms_promocode (id),
    user_id         uuid          not null references users (id),
    subscription_id bigint        references cms_subscription (id),
    redeemed_at     timestamp(6)  not null,
    constraint uk_promocode_user unique (promocode_id, user_id)
);

create index idx_promocode_redemption_user on cms_promocode_redemption (user_id, redeemed_at);
create index idx_promocode_active on cms_promocode (active, valid_until);
