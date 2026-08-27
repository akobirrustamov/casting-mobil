-- ==========================================================================
--  V27 — S3 MULTIPART YUKLASH
-- ==========================================================================
--
--  cms_upload_session jadvaliga UCHTA ustun qo'shiladi. Jadvalning o'zi
--  V8 da yaratilgan va u yerda saqlash lokal disk deb hisoblangan edi.
--
--  NEGA YANGI JADVAL EMAS
--  Sessiya tushunchasi bir xil: ochiladi, bo'laklar keladi, yopiladi.
--  Farqi faqat bo'laklar QAYERGA tushishida. Ikkinchi jadval bo'lsa
--  tozalash vazifasi, egalik tekshiruvi va holat o'tishlari ikki marta
--  yozilardi.
--
--  ⚠️ MAVJUD SESSIYALARGA TA'SIR QILMAYDI
--  upload_mode uchun sukut qiymat 'CHUNKED' — ya'ni V8 dagi xatti-harakat.
--  Yarim qolgan yuklashlar davom ettirilishi mumkin bo'lib qoladi.
--
--  ⚠️ BO'LAKLAR RO'YXATI BU YERDA SAQLANMAYDI
--  Lokal rejimda haqiqat manbai — diskdagi .part fayllari (V8 izohi).
--  S3 rejimida — S3 ning O'ZI: `ListParts` so'rovi qaysi bo'laklar
--  yetib kelganini va ularning ETag'ini aytadi.
--
--  Klient ETag'larni qaytarib yuborishi ham mumkin edi, lekin unda
--  yolg'on ma'lumot yuborish imkoni paydo bo'lardi va server yig'ishga
--  urinib, tushunarsiz xato olardi. Serverning o'zi so'ragani
--  ishonchliroq va V8 dagi qaror bilan bir xil.
-- ==========================================================================

alter table cms_upload_session
    add column upload_mode varchar(16) not null default 'CHUNKED';

-- S3 bergan multipart identifikatori. Lokal rejimda null.
alter table cms_upload_session
    add column s3_upload_id varchar(512);

-- Yakuniy obyekt kaliti. BOSHIDA yasaladi va o'zgarmaydi: bo'laklar
-- aynan shu kalitga yuboriladi, ya'ni yig'ish paytida uni qayta
-- hisoblash mumkin emas.
--
-- Lokal rejimda null — u yerda kalit yig'ish paytida yasaladi.
alter table cms_upload_session
    add column storage_key varchar(512);
