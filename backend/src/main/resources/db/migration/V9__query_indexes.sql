-- ==========================================================================
--  V9 — RO'YXAT SO'ROVLARI UCHUN INDEKSLAR
-- ==========================================================================
--
--  Bu yerdagi har bir indeks KODDA MAVJUD so'rovga asoslangan, "ehtimol
--  kerak bo'lar" tamoyiliga emas. Keraksiz indeks bepul emas: har bir
--  insert/update uni ham yangilaydi.
--
--  Mavjud indekslar tekshirildi. Quyidagilar ALLAQACHON bor va takrorlanmadi:
--    users.phone / email / google_sub  → unique cheklov indeks yaratadi
--    cms_user_account.user_id          → unique
--    cms_episode (content_id, sort_order), cms_season, cms_comment,
--    cms_purchase, cms_advertisement, cms_analytics_event ...
--
--  ⚠️ PostgreSQL'da "deleted_at is null" uchun QISMAN indeks
--  (create index ... where deleted_at is null) ancha samarali bo'lardi,
--  lekin H2 uni qo'llab-quvvatlamaydi va dev/test muhitlari yiqilardi.
--  Shuning uchun kompozit indeks. Prod faqat PostgreSQL bo'lib qolsa,
--  bu qismanga almashtirilishi mumkin.
-- ==========================================================================

-- Admin kontent ro'yxati: "where deleted_at is null order by created_at desc".
-- Eng ko'p chaqiriladigan so'rov. Indekssiz - butun jadval + saralash.
create index idx_content_alive_created on cms_content (deleted_at, created_at);

-- O'sha ro'yxat holat bo'yicha filtrlanganda.
-- Mavjud idx_content_status faqat status bo'yicha, deleted_at ni qamramaydi.
create index idx_content_alive_status on cms_content (deleted_at, status);

-- Media kutubxonasi: turga ko'ra filtr + sana bo'yicha saralash.
-- Alohida idx_media_type va idx_media_created ikkalasini birga bajara olmaydi.
create index idx_media_type_created on media_asset (type, created_at);

-- Bildirishnomalar ro'yxati faqat created_at bo'yicha saralanadi -
-- mavjud indekslarning hech biri buni qamramaydi.
create index idx_notification_created on cms_notification (created_at);

-- Audit: ikkala ro'yxat ham created_at bo'yicha saralaydi. Kompozit indeks
-- filtr va saralashni BIRGA bajaradi.
create index idx_audit_actor_created on audit_log (actor_id, created_at);
create index idx_audit_entity_created on audit_log (entity_type, entity_id, created_at);

-- Yuqoridagi ikkitasi eskilarini to'liq qamrab oladi (prefiks qoidasi):
-- (actor_id, created_at) → (actor_id) so'rovlariga ham xizmat qiladi.
-- Ikkalasini saqlash yozuv tezligini bekorga sekinlashtiradi.
-- ⚠️ Bu MA'LUMOTGA tegmaydi: indeks o'chirilsa faqat tezlik o'zgaradi,
-- kerak bo'lsa bitta buyruq bilan qayta yaratiladi.
drop index if exists idx_audit_actor;
drop index if exists idx_audit_entity;
