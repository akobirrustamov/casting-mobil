-- ==========================================================================
--  V5 — IZOHLAR, BILDIRISHNOMALAR, FOYDALANUVCHILAR VA MONETIZATSIYA
-- ==========================================================================
--
--  PHASE 6:
--    cms_comment      — izohlar. Hard delete YO'Q: status HIDDEN/DELETED
--                       bo'ladi, moderator qarori va shikoyat tarixi saqlanadi.
--    cms_notification — push-bildirishnomalar. Yuborish HALI ULANMAGAN
--                       (FCM sozlanmagan), shuning uchun SENT holati faqat
--                       provayder tasdiqlagandan keyin qo'yiladi.
--
--  PHASE 7:
--    cms_user_account — mobil foydalanuvchining holati. `users` jadvaliga
--                       ustun QO'SHILMADI: uni sayt, bot va mobil ilova
--                       birgalikda ishlatadi, unga tegish uchtala klientni
--                       ham xavf ostiga qo'yardi.
--    cms_user_device  — qurilma limiti (buyurtmachi: max 2). Limitning o'zi
--                       cms_platform_setting da, kodda emas.
--    cms_user_balance — pul + Stars + Coin. Pul numeric(14,2), double emas.
--    cms_tariff       — Premium tariflari. Narxlar ADMIN PANELDAN o'zgaradi.
--    cms_subscription — obuna tarixi. ADMIN_GIFT manbali obunada paid_amount
--                       NULL — u daromad emas va hisobotda shunday hisoblanadi.
--    cms_currency_package — Stars/Coin paketlari.
--    cms_donation     — O'ZGARMAS moliyaviy tarix, hech qachon o'chirilmaydi.
--    cms_platform_setting — narx va kurslar. Deploy kutmasdan o'zgaradi.
--
--  Enum check constraint'lari yo'q — sabab V2 sarlavhasida.
-- ==========================================================================

create table cms_comment (reports_count integer not null, content_id bigint not null, created_at timestamp(6) not null, episode_id bigint, id bigserial not null, moderated_at timestamp(6), author_id uuid, moderated_by uuid, status varchar(16) not null, text varchar(2000) not null, primary key (id));
create table cms_currency_package (active boolean not null, price numeric(12,2) not null, sort_order integer not null, amount bigint not null, id bigserial not null, kind varchar(16) not null, primary key (id));
create table cms_donation (amount bigint not null, created_at timestamp(6) not null, id bigserial not null, target_id bigint not null, kind varchar(16) not null, sender_id uuid, target_type varchar(16) not null, primary key (id));
create table cms_notification (created_at timestamp(6) not null, id bigserial not null, image_media_id bigint, internal_target_id bigint, scheduled_at timestamp(6), sent_at timestamp(6), created_by uuid, link_type varchar(16), status varchar(16) not null, audience varchar(32) not null, internal_target_type varchar(32), type varchar(32) not null, failure_reason varchar(500), link_url varchar(1000), primary key (id));
create table cms_notification_translation (id bigserial not null, locale varchar(8) not null, notification_id bigint not null, body varchar(1000) not null, title varchar(255) not null, primary key (id), constraint uk_notification_locale unique (locale, notification_id));
create table cms_platform_setting (updated_at timestamp(6), updated_by uuid, setting_key varchar(128) not null, description varchar(500), setting_value varchar(1000) not null, primary key (setting_key));
create table cms_subscription (paid_amount numeric(12,2), created_at timestamp(6) not null, end_at timestamp(6) not null, id bigserial not null, revoked_at timestamp(6), start_at timestamp(6) not null, tariff_id bigint, granted_by uuid, revoked_by uuid, source varchar(16) not null, user_id uuid not null, primary key (id));
create table cms_tariff (active boolean not null, duration_months integer not null, highlighted boolean not null, price numeric(12,2) not null, sort_order integer not null, currency varchar(8) not null, id bigserial not null, code varchar(64) not null unique, primary key (id));
create table cms_tariff_translation (id bigserial not null, locale varchar(8) not null, tariff_id bigint not null, features varchar(2000), badge varchar(255), name varchar(255) not null, primary key (id), constraint uk_tariff_locale unique (locale, tariff_id));
create table cms_user_account (created_at timestamp(6) not null, id bigserial not null, last_active_at timestamp(6), premium_until timestamp(6), status varchar(16) not null, user_id uuid not null unique, blocked_reason varchar(500), primary key (id));
create table cms_user_balance (money_balance numeric(14,2) not null, coin_balance bigint not null, id bigserial not null, stars_balance bigint not null, version bigint, user_id uuid not null unique, primary key (id));
create table cms_user_device (active boolean not null, created_at timestamp(6) not null, id bigserial not null, last_active_at timestamp(6), user_id uuid not null, platform varchar(32), device_id varchar(128) not null, device_name varchar(255), primary key (id), constraint uk_user_device unique (user_id, device_id));
create index idx_comment_content on cms_comment (content_id, created_at);
create index idx_comment_status on cms_comment (status, created_at);
create index idx_comment_author on cms_comment (author_id);
create index idx_comment_reports on cms_comment (reports_count);
create index idx_package_kind on cms_currency_package (kind, active, sort_order);
create index idx_donation_target on cms_donation (target_type, target_id, created_at);
create index idx_donation_sender on cms_donation (sender_id, created_at);
create index idx_donation_created on cms_donation (created_at);
create index idx_notification_status on cms_notification (status, scheduled_at);
create index idx_notification_type on cms_notification (type);
create index idx_subscription_user on cms_subscription (user_id, end_at);
create index idx_subscription_source on cms_subscription (source);
create index idx_tariff_active on cms_tariff (active, sort_order);
create index idx_account_status on cms_user_account (status);
create index idx_account_premium on cms_user_account (premium_until);
create index idx_device_user on cms_user_device (user_id, active);
alter table if exists cms_comment add constraint FKi1tc477byjakpc0605eiugxah foreign key (author_id) references users;
alter table if exists cms_comment add constraint FKcftuwm3th0hcyy8vgowwrnpf2 foreign key (content_id) references cms_content;
alter table if exists cms_comment add constraint FKh650vm16ohg91lsd37intd5in foreign key (episode_id) references cms_episode;
alter table if exists cms_donation add constraint FK99eyt8y4r7plv86yp4frkcpmt foreign key (sender_id) references users;
alter table if exists cms_notification add constraint FKqgmcxh7rhxy2qp8hmbmghhxpb foreign key (image_media_id) references media_asset;
alter table if exists cms_notification_translation add constraint FKqqwrg83b4klmsndpigqfn6md9 foreign key (notification_id) references cms_notification;
alter table if exists cms_subscription add constraint FKk8jihmvp6n4h9xuwgl77jgq6u foreign key (tariff_id) references cms_tariff;
alter table if exists cms_subscription add constraint FK6v5k1ncepef2qo9s3d6001kq6 foreign key (user_id) references users;
alter table if exists cms_tariff_translation add constraint FK5ft2grdcy072i0xydg4yc4vbd foreign key (tariff_id) references cms_tariff;
alter table if exists cms_user_account add constraint FK6cdmboid1sgc4cireqqudvm03 foreign key (user_id) references users;
alter table if exists cms_user_balance add constraint FKs87aqa71fqw8us408q0x1088w foreign key (user_id) references users;
alter table if exists cms_user_device add constraint FKp9cuq8jhcm1ey4xtqt3rao6sa foreign key (user_id) references users;

-- --------------------------------------------------------------------------
--  Boshlang'ich ma'lumot
-- --------------------------------------------------------------------------
--
--  Tarif narxlari — buyurtmachining 13.08.2026 xabaridan. Bular BOSHLANG'ICH
--  qiymatlar: admin panel orqali istalgan vaqtda o'zgartiriladi (§36).

insert into cms_tariff (id, code, duration_months, price, currency, active, highlighted, sort_order) values
  (1, 'm1',  1,  24000.00, 'UZS', true, false, 0),
  (2, 'm3',  3,  49999.00, 'UZS', true, false, 1),
  (3, 'm6',  6,  99000.00, 'UZS', true, false, 2),
  (4, 'y1', 12, 159900.00, 'UZS', true, true,  3);

insert into cms_tariff_translation (id, tariff_id, locale, name, badge, features) values
  (1, 1, 'UZ', '1 oy',  null, 'Barcha ochiq kontent
Barcha premyeralar
Seriallar va filmlar
Reklamasiz tomosha
Premium kontentga kirish
Casting loyihasiga kirish'),
  (2, 1, 'RU', '1 месяц', null, 'Весь открытый контент
Все премьеры
Сериалы и фильмы
Просмотр без рекламы
Доступ к premium-контенту
Доступ к кастинг-проекту'),
  (3, 1, 'EN', '1 month', null, 'All open content
All premieres
Series and films
Ad-free viewing
Premium content access
Casting project access'),
  (4, 2, 'UZ', '3 oy',  null, 'Barcha Premium imkoniyatlar
3 oy davomida to''liq kirish
Oylik to''lovga qaraganda foydaliroq'),
  (5, 2, 'RU', '3 месяца', null, 'Все Premium-возможности
Полный доступ на 3 месяца
Выгоднее помесячной оплаты'),
  (6, 2, 'EN', '3 months', null, 'All Premium features
Full access for 3 months
Better value than monthly'),
  (7, 3, 'UZ', '6 oy',  null, 'Barcha Premium kontent
Barcha premyeralar
6 oy davomida to''liq kirish'),
  (8, 3, 'RU', '6 месяцев', null, 'Весь Premium-контент
Все премьеры
Полный доступ на 6 месяцев'),
  (9, 3, 'EN', '6 months', null, 'All Premium content
All premieres
Full access for 6 months'),
  (10, 4, 'UZ', '1 yil', 'ENG FOYDALI TARIF', 'Barcha Premium kontent
Barcha premyeralar
Barcha serial va filmlar
12 oy davomida to''liq kirish'),
  (11, 4, 'RU', '1 год', 'САМЫЙ ВЫГОДНЫЙ ТАРИФ', 'Весь Premium-контент
Все премьеры
Все сериалы и фильмы
Полный доступ на 12 месяцев'),
  (12, 4, 'EN', '1 year', 'BEST VALUE', 'All Premium content
All premieres
All series and films
Full access for 12 months');

-- Ketma-ketliklarni qo'lda kiritilgan id'lardan keyinga suramiz
alter sequence if exists cms_tariff_id_seq restart with 100;
alter sequence if exists cms_tariff_translation_id_seq restart with 100;

--  Stars va Coin paketlari — ТЗ dagi miqdorlar (10/50/100/500/1000).
--  ⚠️ NARXLAR 0: buyurtmachi «1 yulduz = X so'm» kursini HALI AYTMAGAN
--  (roadmap.md §8, 1-savol). 0 — «sozlanmagan» degani, taxminiy raqam emas.

insert into cms_currency_package (id, kind, amount, price, active, sort_order) values
  (1, 'STARS',   10, 0.00, true, 0),
  (2, 'STARS',   50, 0.00, true, 1),
  (3, 'STARS',  100, 0.00, true, 2),
  (4, 'STARS',  500, 0.00, true, 3),
  (5, 'STARS', 1000, 0.00, true, 4),
  (6, 'COIN',    10, 0.00, true, 0),
  (7, 'COIN',    50, 0.00, true, 1),
  (8, 'COIN',   100, 0.00, true, 2),
  (9, 'COIN',   500, 0.00, true, 3),
  (10,'COIN',  1000, 0.00, true, 4);

alter sequence if exists cms_currency_package_id_seq restart with 100;
