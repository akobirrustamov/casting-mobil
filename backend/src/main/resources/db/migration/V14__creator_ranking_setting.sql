-- ==========================================================================
--  V14 — IJODKOR REYTINGI SOZLAMASI
-- ==========================================================================
--
--  ТЗ §25: «Hozir manual featured/sort imkoniyati yetarli, ammo arxitektura
--  analytics rankingga mos bo'lsin.»
--
--  Bo'lim tartibi endi sozlama orqali almashadi:
--    MANUAL — admin tanlagan featured + sortOrder (standart)
--    STARS  — olingan Stars bo'yicha, ko'pi yuqorida
--
--  Kod o'zgartirmasdan, deploy kutmasdan almashtiriladi.
--
--  ⚠️ STARS tanlansa, u donat oqimi `cms_creator.stars_received` ni
--  yangilaganda ma'noga ega bo'ladi. Hozir bu oqim yo'q, shuning uchun
--  standart MANUAL va tanlov o'zgartirilsa log'da ogohlantirish yoziladi.
-- ==========================================================================

insert into cms_platform_setting (setting_key, setting_value, description)
select 'homepage.creators.ranking', 'MANUAL',
       'Mashhur ijodkorlar tartibi: MANUAL (admin tanlaydi) yoki STARS'
where not exists (
    select 1 from cms_platform_setting where setting_key = 'homepage.creators.ranking');
