-- V36 — qism ko'rishlari HAQIQATAN sanaladigan bo'ldi.
--
-- ⚠️ `cms_episode.view_count` ustuni boshidanoq bor edi va admin panel
-- uni ko'rsatib turardi. Lekin uni HECH KIM oshirmasdi: hodisalar
-- `episode_id` bilan kelardi, jamlash esa ularni faqat KONTENT bo'yicha
-- guruhlab, qism raqamini tashlab yuborardi.
--
-- Ya'ni panel yolg'on nol ko'rsatardi — bu V35 dagi `cms_content.view_count`
-- bilan bir xil kasallik, faqat bir qavat pastda.
--
-- Endi `AnalyticsService.applyEpisodeRows()` uni oshiradi.

-- Orqaga to'ldirish XOM hodisalardan.
--
-- Kontentdan farqli o'laroq (V35 kunlik jamlanmadan yig'gan edi), qism
-- uchun jamlanma jadvali umuman yo'q. Ammo xom hodisalar aynan shu
-- uchun saqlanadi: `AnalyticsEvent` javadocida yozilganidek, formula
-- o'zgarsa agregatni qayta hisoblash mumkin bo'lsin.
--
-- ⚠️ Faqat nol turgan qatorlar to'ldiriladi: aks holda migratsiya qayta
-- ishga tushsa (yoki hisoblagich allaqachon o'sib ulgursa) sanoq
-- ikkilanardi.
update cms_episode e
set view_count = coalesce((select count(*)
                           from cms_analytics_event a
                           where a.episode_id = e.id
                             and a.type = 'CONTENT_VIEW'), 0)
where coalesce(e.view_count, 0) = 0;

-- Qism bo'yicha qidiruv endi har besh daqiqada bo'ladi (jamlash sikli),
-- va orqaga to'ldirish ham shu ustundan o'tdi.
--
-- ⚠️ QISMAN indeks emas (`where episode_id is not null`). PostgreSQL'da
-- u samaraliroq bo'lardi — qismsiz hodisalar ko'pchilikni tashkil qiladi —
-- lekin H2 uni qo'llab-quvvatlamaydi va dev/test muhitlari yiqilardi. Bu
-- V9 da bir marta hal qilingan savol, javob o'sha: kompozit indeks.
create index if not exists idx_event_episode
    on cms_analytics_event (episode_id, type);
