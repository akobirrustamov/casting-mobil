-- V26 — foydalanuvchi tili (ТЗ §32 va mobil talabi).
--
-- Kontent, bildirishnoma va reklama uch tilda saqlanadi, lekin
-- FOYDALANUVCHINING tili hech qayerda yozilmasdi. Bosh sahifa uni so'rov
-- parametridan olardi, push xabar esa umuman hech qayerdan — FCM
-- ulangach barcha foydalanuvchiga o'zbekcha matn ketardi.
--
-- Mavjud qatorlar UZ oladi: bu taxmin emas, davlat tili va mobil ilova
-- birinchi ochilishda haqiqiy qiymatni yuboradi.
alter table cms_user_account add column if not exists language varchar(8) not null default 'UZ';
