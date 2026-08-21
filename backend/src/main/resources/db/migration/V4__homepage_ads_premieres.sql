-- ==========================================================================
--  V4 — BOSH SAHIFA, REKLAMA VA PREMYERALAR
-- ==========================================================================
--
--  cms_homepage_section — bosh sahifa bo'limlari.
--      Mobil ilova bosh sahifasi klientda QOTIRILMAYDI: u shu jadvaldan
--      quriladi, shuning uchun admin bo'limni yoqishi, o'chirishi va
--      tartibini o'zgartirishi mumkin (buyurtmachi talabi).
--
--  cms_advertisement — bosh sahifadagi bannerlar.
--      `audience` ustuni muhim: ADVERTISEMENT faqat faol tarifi YO'Q
--      foydalanuvchilarga ko'rinadi (Premium — «reklamasiz tomosha»),
--      ADMIN_ANNOUNCEMENT esa hammaga.
--
--  cms_premiere — «Yangi premyeralar» kartochkalari.
--
--  Uchalasida ham havola ustunlari bir xil (link_type, link_url,
--  internal_target_type, internal_target_id) — InternalLink @Embeddable.
--  Tugma ham, havola ham IXTIYORIY.
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

create table cms_advertisement (button_enabled boolean not null, sort_order integer not null, created_at timestamp(6) not null, end_at timestamp(6), id bigserial not null, image_media_id bigint, internal_target_id bigint, mobile_image_media_id bigint, start_at timestamp(6), created_by uuid, link_type varchar(16), status varchar(16) not null, audience varchar(32) not null, internal_target_type varchar(32), link_url varchar(1000), name varchar(255) not null, primary key (id));
create table cms_advertisement_translation (advertisement_id bigint not null, id bigserial not null, locale varchar(8) not null, button_text varchar(64), description varchar(1000), title varchar(255), primary key (id), constraint uk_ad_locale unique (advertisement_id, locale));
create table cms_homepage_section (enabled boolean not null, item_limit integer, sort_order integer not null, id bigserial not null, type varchar(32) not null, primary key (id), constraint uk_homepage_type unique (type));
create table cms_homepage_section_translation (id bigserial not null, locale varchar(8) not null, section_id bigint not null, title varchar(255) not null, primary key (id), constraint uk_homepage_locale unique (locale, section_id));
create table cms_premiere (button_enabled boolean not null, sort_order integer not null, content_id bigint, created_at timestamp(6) not null, end_at timestamp(6), id bigserial not null, image_media_id bigint, internal_target_id bigint, start_at timestamp(6), video_media_id bigint, created_by uuid, link_type varchar(16), status varchar(16) not null, internal_target_type varchar(32), link_url varchar(1000), name varchar(255) not null, primary key (id));
create table cms_premiere_translation (id bigserial not null, locale varchar(8) not null, premiere_id bigint not null, button_text varchar(64), description varchar(2000), subtitle varchar(255), title varchar(255) not null, primary key (id), constraint uk_premiere_locale unique (locale, premiere_id));
create index idx_ad_status on cms_advertisement (status, sort_order);
create index idx_ad_window on cms_advertisement (start_at, end_at);
create index idx_ad_audience on cms_advertisement (audience);
create index idx_homepage_order on cms_homepage_section (enabled, sort_order);
create index idx_premiere_status on cms_premiere (status, sort_order);
create index idx_premiere_window on cms_premiere (start_at, end_at);
alter table if exists cms_advertisement add constraint FK8arankms48fq6khnhd7je4h5p foreign key (image_media_id) references media_asset;
alter table if exists cms_advertisement add constraint FKji7btsvcwtkdu2sq09cplex75 foreign key (mobile_image_media_id) references media_asset;
alter table if exists cms_advertisement_translation add constraint FK5lijuhk7mre6mmdpcnsaj5an foreign key (advertisement_id) references cms_advertisement;
alter table if exists cms_homepage_section_translation add constraint FKcpngx253jr8hap3h711stn3wh foreign key (section_id) references cms_homepage_section;
alter table if exists cms_premiere add constraint FKm612xo0hvfd7crrw9jsp0psmj foreign key (content_id) references cms_content;
alter table if exists cms_premiere add constraint FK7vexufdayyip31lq2eofkumri foreign key (image_media_id) references media_asset;
alter table if exists cms_premiere add constraint FK1feivv4nhkdd5i0d2xd7nvdsf foreign key (video_media_id) references media_asset;
alter table if exists cms_premiere_translation add constraint FK5siv1ehqlyicut8qu2ygsb1yt foreign key (premiere_id) references cms_premiere;
