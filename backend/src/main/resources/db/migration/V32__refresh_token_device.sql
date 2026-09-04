-- V32 — refresh tokenni qurilmaga bog'laydi.
--
-- ⚠️ Nima uchun bu ustun kerak bo'ldi
--
-- `cms_user_device` jadvali V5 dan beri bor va admin qurilmani
-- «chiqarib yuborishi» mumkin edi — lekin bu FAQAT yozuvni
-- `active = false` qilardi. Qurilmadagi refresh token esa hech narsa
-- bilan bog'lanmagani uchun o'z muddatigacha ishlayverardi: odam
-- chiqarilgan qurilmada tomosha qilishda davom etardi va admin buni
-- bilmasdi ham.
--
-- Endi har bir token qaysi qurilmadan berilgani yoziladi. Qurilma
-- chiqarilganda o'sha qurilmaning tokenlari darhol bekor qilinadi
-- (`revokeAllForDevice`), va yangilash oqimi ham qurilma hali faolmi
-- deb tekshiradi.
--
-- Ustun NULL bo'lishi mumkin: bu migratsiyagacha berilgan tokenlarda
-- qurilma noma'lum. Ular avvalgidek ishlayveradi va tabiiy ravishda
-- muddati bilan yo'qoladi — mavjud sessiyalarni majburan yopish
-- foydalanuvchi uchun sababsiz chiqib qolish bo'lardi.
alter table refresh_token
    add column if not exists device_id varchar(128);

-- Qurilma bo'yicha bekor qilish shu indeksga tayanadi.
create index if not exists idx_refresh_device
    on refresh_token (user_id, device_id);
