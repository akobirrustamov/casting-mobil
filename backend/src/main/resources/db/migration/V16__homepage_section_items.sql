-- ==========================================================================
--  V16 — BOSH SAHIFA: QO'LDA YIG'ILGAN QATORLAR
-- ==========================================================================
--
--  ТЗ §31 bo'limlar ro'yxatida «Custom content rows» bor. Bo'limning O'ZI
--  (`cms_homepage_section`) bor edi, lekin unga QAYSI kontent kirishini
--  saqlaydigan joy yo'q edi.
--
--  Ya'ni admin «Maxsus qator» bo'limini yoqishi mumkin edi, lekin uni
--  to'ldirolmasdi — mobil ilovada u doim bo'sh chiqardi.
--
--  ⚠️ NIMA UCHUN ALOHIDA JADVAL, KONTENTGA USTUN EMAS
--
--  `content.featured` va `content.popular` — BITTA bayroq, ya'ni bitta
--  qator. Maxsus qatorlar esa bir nechta bo'ladi («Ramazon tanlovi»,
--  «Yangi yil kinolar») va bitta film bir vaqtning o'zida bir nechtasida
--  turishi mumkin. Bayroq buni ifodalay olmaydi.
--
--  Tartib qatorning O'ZIDA saqlanadi (`sort_order`): bir xil film bir
--  qatorda birinchi, boshqasida oxirgi bo'lishi mumkin.
--
--  FEATURED_CONTENT va POPULAR_CONTENT bo'limlari bayroqlar bilan
--  ishlashda davom etadi — bu jadval ularni almashtirmaydi.
-- ==========================================================================

create table cms_homepage_section_item (
    id          bigserial not null,
    section_id  bigint    not null,
    content_id  bigint    not null,
    sort_order  integer   not null default 0,
    primary key (id),
    constraint uk_homepage_item unique (section_id, content_id),
    constraint fk_homepage_item_section foreign key (section_id)
        references cms_homepage_section (id) on delete cascade,
    constraint fk_homepage_item_content foreign key (content_id)
        references cms_content (id) on delete cascade
);

-- Qatorni chizish uchun asosiy so'rov: bo'lim bo'yicha tartibda.
create index idx_homepage_item_order on cms_homepage_section_item (section_id, sort_order);
