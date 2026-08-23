-- ==========================================================================
--  V7 — BIR MARTALIK XARIDLAR
-- ==========================================================================
--
--  cms_purchase — qism yoki butun premyera xaridi.
--
--  Bu O'ZGARMAS moliyaviy yozuv: hech qachon o'chirilmaydi. Foydalanuvchi
--  nimaga haq to'laganini isbotlaydigan yagona manba.
--
--  target_id turga qarab: EPISODE → qism id, PREMIERE → kontent id.
--
--  refunded_at maydoni bor, lekin qaytarish MANTIQI yo'q: ТЗ refund'ni
--  eslatadi, ammo qoidalarni yozmagan (roadmap.md §8, 4-savol).
--
--  Bu jadvalsiz AccessService ishlay olmaydi — entitlement to'rt manbadan
--  keladi va ikkitasi shu yerda.
-- ==========================================================================

create table cms_purchase (amount numeric(12,2) not null, created_at timestamp(6) not null, currency varchar(8) not null, id bigserial not null, refunded_at timestamp(6), target_id bigint not null, type varchar(16) not null, user_id uuid not null, payment_reference varchar(128), primary key (id));
create index idx_purchase_lookup on cms_purchase (user_id, type, target_id);
create index idx_purchase_user on cms_purchase (user_id, created_at);
create index idx_purchase_target on cms_purchase (type, target_id);
alter table if exists cms_purchase add constraint FKb4rromf088fsnhucd5wbpcn0w foreign key (user_id) references users;
