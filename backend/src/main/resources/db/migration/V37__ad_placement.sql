-- V37 — banner qayerda ko'rinadi: karuselda yoki butun ekranda.
--
-- Buyurtmachi (09.09.2026) «Majburiy reklama» maketini yubordi. Shu
-- paytgacha bannerlar faqat bosh sahifadagi karuselda ko'rinardi.
--
-- ⚠️ Sukut bo'yicha FEED — mavjud barcha bannerlar bugungidek ishlashda
-- davom etadi. Yangi format admin uni ATAYLAB tanlaganda yoqiladi; aks
-- holda migratsiyadan keyin butun reklama birdaniga ekranni yopa
-- boshlardi, va buni hech kim so'ramagan bo'lardi.
alter table cms_advertisement
    add column if not exists placement varchar(16) not null default 'FEED';
