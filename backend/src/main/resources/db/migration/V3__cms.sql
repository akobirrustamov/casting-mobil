-- ==========================================================================
--  V3 — KONTENT PLATFORMASI (CMS)
-- ==========================================================================
--
--  Kategoriya, janr, ijodkor, kontent, fasl, qism, media.
--
--  Ko'p tillilik: har bir matnli entity uchun alohida `*_translation`
--  jadvali, `UNIQUE(parent_id, locale)` cheklovi bilan. `slug` tarjima
--  qilinmaydi — u barqaror identifikator.
--
--  Media tilga bog'liq bo'lishi mumkin: `cms_content_media.locale` va
--  `cms_episode_video.locale` NULL bo'lsa — barcha tillar uchun umumiy.
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

create table cms_category (active boolean not null, sort_order integer not null, created_at timestamp(6) not null, icon_media_id bigint, id bigserial not null, slug varchar(128) not null unique, primary key (id));
create table cms_category_translation (category_id bigint not null, id bigserial not null, locale varchar(8) not null, description varchar(1000), name varchar(255) not null, primary key (id), constraint uk_category_locale unique (category_id, locale));
create table cms_content (duration_minutes integer, featured boolean not null, popular boolean not null, premiere_price numeric(12,2), age_rating varchar(8), category_id bigint, created_at timestamp(6) not null, deleted_at timestamp(6), id bigserial not null, premiere_date timestamp(6), publication_date timestamp(6), stars_received bigint not null, updated_at timestamp(6), version bigint, view_count bigint not null, created_by uuid, orientation varchar(16) not null, status varchar(16) not null, structure_type varchar(16) not null, updated_by uuid, access_policy varchar(32) not null, content_type varchar(32) not null, slug varchar(200) not null unique, primary key (id));
create table cms_content_credit (sort_order integer not null, content_id bigint not null, creator_id bigint not null, id bigserial not null, profession varchar(32) not null, character_name varchar(255), primary key (id));
create table cms_content_genre (content_id bigint not null, genre_id bigint not null, primary key (content_id, genre_id));
create table cms_content_media (sort_order integer not null, content_id bigint not null, id bigserial not null, locale varchar(8), media_id bigint not null, role varchar(16) not null, primary key (id));
create table cms_content_translation (content_id bigint not null, id bigserial not null, locale varchar(8) not null, title varchar(500) not null, short_description varchar(1000), description varchar(8000), primary key (id), constraint uk_content_locale unique (content_id, locale));
create table cms_creator (active boolean not null, birth_date date, featured boolean not null, sort_order integer not null, cover_media_id bigint, created_at timestamp(6) not null, id bigserial not null, photo_media_id bigint, stars_received bigint not null, slug varchar(128) not null unique, primary key (id));
create table cms_creator_translation (creator_id bigint not null, id bigserial not null, locale varchar(8) not null, first_name varchar(128), last_name varchar(128), middle_name varchar(128), bio varchar(4000), display_name varchar(255) not null, primary key (id), constraint uk_creator_locale unique (creator_id, locale));
create table cms_episode (duration_seconds integer, episode_number integer not null, price numeric(12,2), sort_order integer not null, content_id bigint not null, id bigserial not null, premiere_date timestamp(6), publication_date timestamp(6), season_id bigint, thumbnail_media_id bigint, version bigint, view_count bigint not null, status varchar(16) not null, access_policy varchar(32), primary key (id));
create table cms_episode_translation (episode_id bigint not null, id bigserial not null, locale varchar(8) not null, title varchar(500) not null, short_description varchar(1000), description varchar(4000), primary key (id), constraint uk_episode_locale unique (episode_id, locale));
create table cms_episode_video (part_number integer not null, sort_order integer not null, episode_id bigint not null, id bigserial not null, locale varchar(8), media_id bigint not null, primary key (id));
create table cms_genre (active boolean not null, sort_order integer not null, id bigserial not null, slug varchar(128) not null unique, primary key (id));
create table cms_genre_translation (genre_id bigint not null, id bigserial not null, locale varchar(8) not null, name varchar(255) not null, primary key (id), constraint uk_genre_locale unique (genre_id, locale));
create table cms_season (season_number integer not null, sort_order integer not null, content_id bigint not null, id bigserial not null, poster_media_id bigint, premiere_date timestamp(6), status varchar(16) not null, primary key (id), constraint uk_season_number unique (season_number, content_id));
create table cms_season_translation (id bigserial not null, locale varchar(8) not null, season_id bigint not null, description varchar(2000), title varchar(255) not null, primary key (id), constraint uk_season_locale unique (locale, season_id));
create table media_asset (duration_seconds integer, height integer, width integer, created_at timestamp(6) not null, id bigserial not null, size_bytes bigint, created_by uuid, status varchar(16) not null, type varchar(16) not null, mime_type varchar(128), storage_key varchar(512) not null, original_filename varchar(255), primary key (id));
create index idx_category_active on cms_category (active, sort_order);
create index idx_content_status on cms_content (status);
create index idx_content_category on cms_content (category_id);
create index idx_content_publication on cms_content (publication_date);
create index idx_content_premiere on cms_content (premiere_date);
create index idx_content_type on cms_content (content_type, orientation);
create index idx_content_featured on cms_content (featured);
create index idx_content_popular on cms_content (popular);
create index idx_credit_content on cms_content_credit (content_id, sort_order);
create index idx_credit_creator on cms_content_credit (creator_id);
create index idx_content_media_lookup on cms_content_media (content_id, role, locale);
create index idx_creator_featured on cms_creator (featured, sort_order);
create index idx_creator_active on cms_creator (active);
create index idx_episode_content on cms_episode (content_id, sort_order);
create index idx_episode_season on cms_episode (season_id, episode_number);
create index idx_episode_status on cms_episode (status);
create index idx_episode_video on cms_episode_video (episode_id, locale, part_number);
create index idx_season_content on cms_season (content_id, sort_order);
create index idx_media_type on media_asset (type);
create index idx_media_created on media_asset (created_at);
alter table if exists cms_category add constraint FKhykcy5u9d7yj3t1hker7kk8rm foreign key (icon_media_id) references media_asset;
alter table if exists cms_category_translation add constraint FK9xjbwf50p77o10xtn3qmsgjmg foreign key (category_id) references cms_category;
alter table if exists cms_content add constraint FK7j8c4kbxbohq12j6ngnmvo04m foreign key (category_id) references cms_category;
alter table if exists cms_content_credit add constraint FK745t669i49gsemn56tamtyyu7 foreign key (content_id) references cms_content;
alter table if exists cms_content_credit add constraint FKcy9v7mvfesonpiay67le7ixan foreign key (creator_id) references cms_creator;
alter table if exists cms_content_genre add constraint FKiibliwlpmsvg1a6iymnel0fkq foreign key (genre_id) references cms_genre;
alter table if exists cms_content_genre add constraint FK283afhgvw6xl5ptonfaf5ltqt foreign key (content_id) references cms_content;
alter table if exists cms_content_media add constraint FKbpq4id4xua36ic2eq9p8y44bl foreign key (content_id) references cms_content;
alter table if exists cms_content_media add constraint FKnbrsaw8qghp31l8urt7o6phk4 foreign key (media_id) references media_asset;
alter table if exists cms_content_translation add constraint FK7o0cay90jf79trh9566a1hcem foreign key (content_id) references cms_content;
alter table if exists cms_creator add constraint FKkwjfeedxnc5ymktmqswydw0qt foreign key (cover_media_id) references media_asset;
alter table if exists cms_creator add constraint FKe11kdts4t1jodvisxyneeyexj foreign key (photo_media_id) references media_asset;
alter table if exists cms_creator_translation add constraint FK8h67dpb08cwmf2m3fy9e4ujo9 foreign key (creator_id) references cms_creator;
alter table if exists cms_episode add constraint FK7j5egldwg1yrxbyd5lrrl1wna foreign key (content_id) references cms_content;
alter table if exists cms_episode add constraint FK15dtsbm6y4631wg107y7sa2y6 foreign key (season_id) references cms_season;
alter table if exists cms_episode add constraint FKen7r9sb7bb1dopucnhl3svlgf foreign key (thumbnail_media_id) references media_asset;
alter table if exists cms_episode_translation add constraint FK8agbb047cujmjtu3xdy115yj8 foreign key (episode_id) references cms_episode;
alter table if exists cms_episode_video add constraint FKqc6l2v9g0ki2gixqmq6lo8noa foreign key (episode_id) references cms_episode;
alter table if exists cms_episode_video add constraint FKmctjylbkxk5vexmi8p6admb4n foreign key (media_id) references media_asset;
alter table if exists cms_genre_translation add constraint FKaxmnnwatfve2jxv919xpmrd7f foreign key (genre_id) references cms_genre;
alter table if exists cms_season add constraint FKj8751c363hbgh0iog7ef9qy5q foreign key (content_id) references cms_content;
alter table if exists cms_season add constraint FK4y0vkbmyj1x495gevttm2vibu foreign key (poster_media_id) references media_asset;
alter table if exists cms_season_translation add constraint FKoqgqaal7uks5hwts1a966k8f foreign key (season_id) references cms_season;
