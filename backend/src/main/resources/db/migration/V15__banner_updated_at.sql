-- ==========================================================================
--  V15 — REKLAMA VA PREMYERA: UPDATED_AT
-- ==========================================================================
--
--  ТЗ §27 reklama uchun `updatedAt` ni talab qiladi. `createdAt` bor edi,
--  ikkinchisi yo'q.
--
--  PREMYERA HAM QO'SHILADI (§30 da aniq so'ralmagan bo'lsa-da): ikkalasi
--  admin panelida BITTA sahifada boshqariladi va bir xil hayot sikliga
--  ega — yaratiladi, tahrirlanadi, muddati o'tadi. Birida maydon bo'lib
--  ikkinchisida bo'lmasligi izchil emas va panelda ustun bo'sh qolardi.
--
--  ⚠️ Barcha entityga qo'shilmadi. `cms_purchase`, `cms_donation` va
--  `cms_analytics_event` — O'ZGARMAS yozuvlar: ular hech qachon
--  tahrirlanmaydi, shuning uchun `updated_at` u yerda chalg'ituvchi
--  bo'lardi.
--
--  Mavjud satrlarga `created_at` qiymati beriladi: yaratilgandan beri
--  o'zgarmagan bo'lsa, oxirgi o'zgarish sanasi aynan shu.
-- ==========================================================================

alter table cms_advertisement add column updated_at timestamp(6);
alter table cms_premiere add column updated_at timestamp(6);

update cms_advertisement set updated_at = created_at where updated_at is null;
update cms_premiere set updated_at = created_at where updated_at is null;
