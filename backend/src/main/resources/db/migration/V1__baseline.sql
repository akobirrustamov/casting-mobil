-- ==========================================================================
--  V1 — BAZAVIY SXEMA (baseline)
-- ==========================================================================
--
--  Bu migratsiya MAVJUD production bazasida ISHLAMAYDI.
--  `flyway.baseline-on-migrate=true` va `baseline-version=1` sozlamalari
--  tufayli Flyway uni "allaqachon qo'llangan" deb belgilaydi.
--
--  Nega kerak: bo'sh bazada (masalan yangi dev muhitida yoki testda) butun
--  sxema noldan qurilishi uchun. Bu yerdagi jadvallar ilgari Hibernate'ning
--  `ddl-auto=update` rejimi tomonidan yaratilgan edi.
--
--  ⚠️ Bu faylni O'ZGARTIRMANG. Sxema o'zgarishi yangi V-fayl bilan qo'shiladi.
--
--  Jadvallar: attachment, casting_user, casting_user_photos, message,
--             news, news_photos, role, users, users_roles
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

create table attachment (is_web_show boolean, id uuid not null, name varchar(255), prefix varchar(255), primary key (id));
create table casting_user (age integer, first_chan integer, height integer, id serial not null, is_web_show boolean, price float(53), second_chan integer, status integer, birthday timestamp(6), created_at timestamp(6), bust varchar(255), casting_type varchar(255), cloth_size varchar(255), email varchar(255), eye_color varchar(255), facebook varchar(255), gender varchar(255), hair_color varchar(255), instagram varchar(255), name varchar(255), nationality varchar(255), phone varchar(255), region varchar(255), shoe_size varchar(255), son varchar(255), telegram varchar(255), telegram_id varchar(255), waist varchar(255), primary key (id));
create table casting_user_photos (casting_user_id integer not null, photos_id uuid not null unique);
create table message (casting_user_id integer unique, id serial not null, status boolean, telegram_id numeric(38,0), date_time timestamp(6), casting_type varchar(255), message varchar(255), name varchar(255), price varchar(255), primary key (id));
create table news (id serial not null, created_at timestamp(6), main_photo_id uuid unique, description_ru varchar(10000), description_uz varchar(10000), link varchar(10000), title_ru varchar(255), title_uz varchar(255), primary key (id));
create table news_photos (news_id integer not null, photos_id uuid not null unique);
create table role (id integer not null, name varchar(255) not null unique, primary key (id));
create table users (id uuid not null, avatar_url varchar(255), email varchar(255) unique, google_sub varchar(255) unique, name varchar(255), password varchar(255), phone varchar(255) unique, primary key (id));
create table users_roles (roles_id integer not null, user_id uuid not null);
alter table if exists casting_user_photos add constraint FK3y0jq1q72r31ktidcqkk4w245 foreign key (photos_id) references attachment;
alter table if exists casting_user_photos add constraint FKcufh31m3bfc1fk0meapuq7mor foreign key (casting_user_id) references casting_user;
alter table if exists message add constraint FKjtbkv7hdritrr5tksjwb941uc foreign key (casting_user_id) references casting_user;
alter table if exists news add constraint FKavct4f51cb2prd2g2d1q95dic foreign key (main_photo_id) references attachment;
alter table if exists news_photos add constraint FKo1s2a6x19tb44sm8xmayb4v6k foreign key (photos_id) references attachment;
alter table if exists news_photos add constraint FKh0o110v0v312w8pj83cinx7j2 foreign key (news_id) references news;
alter table if exists users_roles add constraint FK15d410tj6juko0sq9k4km60xq foreign key (roles_id) references role;
alter table if exists users_roles add constraint FK2o0jvgh89lemvvo17cbqvdxaa foreign key (user_id) references users;
