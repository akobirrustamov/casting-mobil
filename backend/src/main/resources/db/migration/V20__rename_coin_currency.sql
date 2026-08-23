-- ==========================================================================
--  V20 — VALYUTA NOMI: COIN → UZCASTING_COIN
-- ==========================================================================
--
--  ТЗ §39 valyutani aniq nomlaydi:
--
--      STARS
--      UZCASTING_COIN
--
--  Kodda esa `COIN` edi. Farq kichik ko'rinadi, lekin u DOIMIY tarjima
--  qatlamini yaratadi: ТЗ, suhbat va hisobotlarda bir nom, kodda va API
--  javobida boshqa nom. Vaqt o'tib «coin» degani UZCASTING coinmi yoki
--  boshqa valyutami degan savol tug'iladi.
--
--  ⚠️ NIMA UCHUN AYNAN HOZIR
--
--  Donat yuborish endpointi endigina qo'shildi — ya'ni ishlab chiqarishda
--  hali BITTA ham donat tranzaksiyasi yo'q. Keyinroq bu o'zgartirish
--  haqiqiy moliyaviy yozuvlarga tegishi kerak bo'lardi.
--
--  ⚠️ MA'LUMOT SAQLANADI
--
--  Mavjud satrlar o'chirilmaydi — nomi yangilanadi. Valyuta paketlari
--  (V5 dagi 5 ta COIN paketi) va agar bo'lsa, dev donatlari.
-- ==========================================================================

update cms_currency_package  set kind = 'UZCASTING_COIN' where kind = 'COIN';
update cms_donation           set kind = 'UZCASTING_COIN' where kind = 'COIN';
