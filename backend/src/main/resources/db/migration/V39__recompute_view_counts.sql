-- V39 — ko'rishlar soni REAL songa keltiriladi.
--
-- Buyurtmachi (15.09.2026): «viewlar sonini real sonlarga keltir, endi
-- to'g'ri hisoblash boshlansin».
--
-- ⚠️ Nima noto'g'ri edi. Ilova kontent kartochkasi va pleyer HAR OCHILGANDA
-- `CONTENT_VIEW` yuboradi, `AnalyticsService.aggregate()` esa hammasini
-- `view_count` ga qo'shardi: bir odam filmni o'n marta ochsa — o'nta ko'rish.
-- V35 (kontent) va V36 (qism) ham orqaga to'ldirishda xuddi shu xom sonni
-- olgan edi.
--
-- Yangi qoida (`AnalyticsService`, 15.09.2026): bir odam — foydalanuvchi
-- bo'lsa user_id, anonim bo'lsa device_key — bitta kontentni (qismni)
-- sutkada BIR MARTA sanaydi. Bu migratsiya o'sha qoidani saqlangan xom
-- hodisalarga qo'llab, hisoblagichlarni noldan qayta yozadi. Xom hodisalar
-- hech qachon o'chirilmaydi (V15), aynan shunday holat uchun.
--
-- ⚠️ Faqat `processed = true` hodisalar. Hali jamlanmagan hodisalarni
-- keyingi 5 daqiqalik sikl o'zi qo'shadi: kontentda kunlik unikal sanoq
-- farqi orqali (u ham faqat qayta ishlanganlardan yig'ilgan), qismda esa
-- qayta ishlangan hodisalar bilan `not exists` orqali. Ularni shu yerda
-- ham sanasak — o'sha odamlar ikki marta sanalardi.
--
-- ⚠️ Kalitsiz hodisa (na user_id, na device_key) sanalmaydi — jamlash
-- so'rovlaridagi `count(distinct ...)` ham null ni tashlab yuboradi.
--
-- ⚠️ Admin paneldagi kunlik xom `views` (`cms_content_daily_statistic`)
-- TEGILMAYDI: voronka «ochildi → o'ynatildi → tugatildi» xom songa tayanadi.
--
-- `concat` + `cast(... as varchar)` — PostgreSQL ham, testlardagi H2 ham
-- tushunadigan yozuv (qarang V36 dagi qisman indeks haqidagi izoh).

update cms_content c
set view_count = coalesce((
    select count(distinct concat(cast(a.event_date as varchar), '|',
                                 coalesce(cast(a.user_id as varchar), a.device_key)))
    from cms_analytics_event a
    where a.type = 'CONTENT_VIEW'
      and a.processed = true
      and a.target_id = c.id
      and coalesce(cast(a.user_id as varchar), a.device_key) is not null
), 0);

update cms_episode e
set view_count = coalesce((
    select count(distinct concat(cast(a.event_date as varchar), '|',
                                 coalesce(cast(a.user_id as varchar), a.device_key)))
    from cms_analytics_event a
    where a.type = 'CONTENT_VIEW'
      and a.processed = true
      and a.episode_id = e.id
      and coalesce(cast(a.user_id as varchar), a.device_key) is not null
), 0);
