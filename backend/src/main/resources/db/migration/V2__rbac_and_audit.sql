-- ==========================================================================
--  V2 — ROLLAR, RUXSATLAR VA AUDIT
-- ==========================================================================
--
--  UZCASTING admin paneli uchun:
--    user_permission — WORKER'ga berilgan aniq ruxsatlar
--                      (ADMIN va yuqorisi bu jadvalga qaramaydi)
--    audit_log       — muhim admin amallarining o'zgarmas tarixi
--
--  Bu jadvallar production bazasida hali yo'q, shuning uchun baseline'dan
--  KEYIN turadi va birinchi deploy'da yaratiladi.
--
--  ⚠️ ENUM CHECK CONSTRAINT'LARI ATAYLAB YO'Q
--
--  Hibernate har bir enum ustuni uchun `check (x in ('A','B',...))` yasaydi.
--  Bu tuzoq: enum'ga bitta qiymat qo'shilsa (yangi kontent turi, yangi ruxsat,
--  yangi bo'lim turi) — constraint eskiradi va INSERT yiqiladi, ya'ni har bir
--  qiymat uchun alohida migration kerak bo'ladi.
--
--  ТЗ bo'yicha aynan shu enum'lar o'sishi kutilmoqda (ContentType, Permission,
--  HomepageSectionType). Bazaga faqat shu ilova yozadi va Hibernate enum
--  qiymatini o'qishda ham, yozishda ham tekshiradi — shuning uchun constraint
--  qo'shimcha himoya bermaydi, faqat ishlashga xalaqit beradi.
--
-- ==========================================================================

create table audit_log (created_at timestamp(6) not null, id bigserial not null, actor_id uuid, actor_role varchar(32), action varchar(64) not null, entity_id varchar(64), entity_type varchar(64), ip varchar(64), user_agent varchar(512), after_state text, before_state text, primary key (id));
create table user_permission (granted_at timestamp(6) not null, id bigserial not null, granted_by uuid, user_id uuid not null, permission varchar(64) not null, primary key (id), constraint uk_user_permission unique (user_id, permission));
create index idx_audit_actor on audit_log (actor_id);
create index idx_audit_entity on audit_log (entity_type, entity_id);
create index idx_audit_created on audit_log (created_at);
