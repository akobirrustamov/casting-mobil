-- V42 — push bildirishnomalar (Expo Push).
--
-- Push tokeni QURILMAGA tegishli, shuning uchun alohida jadval emas,
-- `cms_user_device` ga ustun: qurilma chiqarilsa (`active = false`) unga
-- push ham ketmaydi, qurilmalar limiti esa tokenlar sonini o'zi cheklaydi.
--
-- ⚠️ Token unikal EMAS: bitta telefonda avval bir odam, keyin boshqasi
-- kirishi mumkin. Servis yangi egaga yozishdan oldin tokenni boshqa
-- qatorlardan tozalaydi (`DeviceService.savePushToken`) — indeks aynan
-- shu qidiruv uchun.
alter table cms_user_device add column if not exists push_token varchar(255);

create index if not exists idx_device_push_token on cms_user_device (push_token);

-- Yuborish natijasi — hisobotdagi «yuborildi / xato» HAQIQIY sonlar.
-- `null` — push hali urinilmagan (V42 dan oldingi xabarlar ham shunday).
alter table cms_notification add column if not exists push_recipients integer;
alter table cms_notification add column if not exists push_accepted integer;
alter table cms_notification add column if not exists push_failed integer;
