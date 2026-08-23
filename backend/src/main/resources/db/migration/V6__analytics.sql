-- ==========================================================================
--  V6 — ANALITIKA
-- ==========================================================================
--
--  Ikki qatlamli sxema (ТЗ §29, §74):
--
--    cms_analytics_event          — XOM hodisalar. Bu jadval millionlab
--                                   qatorga yetadi. Dashboard undan HECH
--                                   QACHON o'qimaydi.
--
--    cms_ad_daily_statistic       — reklama bo'yicha kunlik jamlanma
--    cms_content_daily_statistic  — kontent bo'yicha kunlik jamlanma
--
--  Fon vazifasi (AnalyticsService.aggregate, har 5 daqiqada) xom hodisalarni
--  jamlanmaga qo'shadi va `processed = true` qilib belgilaydi. Ko'rsatkichlar
--  shu qadar kechikadi — admin panel uchun yetarli, baza esa tinch qoladi.
--
--  Agregatni yozish paytida yangilash MUMKIN EMAS edi: bir xil satrga ko'p
--  yozuv urilib qulf raqobati yuzaga kelardi, va COUNT(DISTINCT) ni ham
--  hisoblab bo'lmasdi.
--
--  Xom hodisalar o'chirilmaydi: formula o'zgarsa agregatni qayta hisoblash
--  uchun kerak (`processed` ni false ga qaytarish yetarli).
-- ==========================================================================

create table cms_ad_daily_statistic (stat_date date not null, advertisement_id bigint not null, clicks bigint not null, id bigserial not null, impressions bigint not null, unique_clicks bigint not null, unique_impressions bigint not null, primary key (id), constraint uk_ad_stat_day unique (stat_date, advertisement_id));
create table cms_analytics_event (event_date date not null, processed boolean not null, created_at timestamp(6) not null, episode_id bigint, id bigserial not null, target_id bigint, user_id uuid, type varchar(32) not null, device_key varchar(128), primary key (id));
create table cms_content_daily_statistic (stat_date date not null, completes bigint not null, content_id bigint not null, id bigserial not null, plays bigint not null, unique_viewers bigint not null, views bigint not null, primary key (id), constraint uk_content_stat_day unique (stat_date, content_id));
create index idx_ad_stat_date on cms_ad_daily_statistic (stat_date);
create index idx_event_agg on cms_analytics_event (event_date, type, processed);
create index idx_event_target on cms_analytics_event (type, target_id, event_date);
create index idx_event_created on cms_analytics_event (created_at);
create index idx_content_stat_date on cms_content_daily_statistic (stat_date);
