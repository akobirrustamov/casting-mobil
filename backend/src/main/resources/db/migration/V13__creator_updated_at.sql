-- ==========================================================================
--  V13 — IJODKOR: UPDATED_AT
-- ==========================================================================
--
--  ТЗ §24 `createdAt` va `updatedAt` ni talab qiladi. Birinchisi bor edi,
--  ikkinchisi yo'q.
--
--  NEGA KERAK
--  Ijodkor profili tahrirlanadi: ism, surat, biografiya, «mashhur» bejagi.
--  Oxirgi o'zgarish vaqti bo'lmasa, «qaysi profillar eskirgan» degan
--  savolga javob berib bo'lmaydi va audit jurnalidan qidirishga to'g'ri
--  keladi.
--
--  Boshqa entitylarda (Content, Episode) bu maydon allaqachon bor —
--  izchillik uchun ham qo'shiladi.
--
--  Mavjud satrlar uchun `created_at` qiymati beriladi: profil yaratilgandan
--  beri o'zgarmagan bo'lsa, oxirgi o'zgarish sanasi aynan shu.
-- ==========================================================================

alter table cms_creator add column updated_at timestamp(6);

update cms_creator set updated_at = created_at where updated_at is null;
