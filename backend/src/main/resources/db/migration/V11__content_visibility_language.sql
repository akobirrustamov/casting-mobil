-- ==========================================================================
--  V11 — KONTENT: VISIBILITY VA LANGUAGE
-- ==========================================================================
--
--  ТЗ §15 ikkita maydonni talab qiladi va ularning ikkalasi ham yo'q edi.
--
--  VISIBILITY — `status` dan FARQLI narsa.
--    status     : hayot sikli  (DRAFT → IN_REVIEW → PUBLISHED → ARCHIVED)
--    visibility : topilishi    (PUBLIC | UNLISTED | PRIVATE)
--  Nashr qilingan film premyeradan oldin UNLISTED bo'lishi mumkin:
--  havola bilan ochiladi, katalogda hali chiqmaydi.
--
--  LANGUAGE — kontentning ASL tili. Bu tarjimalar bilan bir narsa EMAS:
--    cms_content_translation : sarlavha va tavsif UZ/RU/EN da (interfeys)
--    cms_content.language    : asarning o'zi qaysi tilda suratga olingan
--  Koreys seriali RU/EN tarjimasi bilan ham koreyscha qoladi.
--
--  Nega enum emas, ISO 639-1 matn: dunyo tillari ro'yxati enum'ga
--  sig'maydi va har yangi til migratsiya talab qilardi (D18 bilan bir xil
--  sabab).
--
--  Mavjud satrlar uchun standart qiymat beriladi — ular buzilmaydi.
-- ==========================================================================

alter table cms_content add column visibility varchar(16);
alter table cms_content add column language varchar(8);

-- Mavjud kontent katalogda ko'rinib turgan edi — xatti-harakat o'zgarmasin.
update cms_content set visibility = 'PUBLIC' where visibility is null;

alter table cms_content alter column visibility set not null;

-- Katalog so'rovi holat VA ko'rinuvchanlik bo'yicha filtrlaydi.
create index idx_content_visibility on cms_content (visibility, status);
