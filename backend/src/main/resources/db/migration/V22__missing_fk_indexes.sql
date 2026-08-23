-- ==========================================================================
--  V22 — SO'ROV NAQSHIGA QARAB INDEKSLAR (ТЗ §56)
-- ==========================================================================
--
--  ⚠️ PostgreSQL FK uchun AVTOMATIK indeks YARATMAYDI (MySQL dan farqli).
--  Loyihada 27 ta indekssiz FK ustuni topildi.
--
--  ⚠️ HAMMASIGA indeks qo'yilmadi. ТЗ: «Sababsiz har bir fieldga index
--  qo'yma». Har bir indeks YOZUV tezligini sekinlashtiradi va joy egallaydi,
--  shuning uchun quyida faqat HAQIQATDAN so'raladigan ustunlar bor.
--
--  Tanlov mezoni: ustun bo'yicha so'rov QANCHALIK TEZ-TEZ va jadval
--  QANCHALIK O'SADI.
-- ==========================================================================


-- --------------------------------------------------------------------------
--  1. ROLLAR — eng issiq yo'l
-- --------------------------------------------------------------------------
--
--  `User.roles` — EAGER: Spring Security uni HAR BIR autentifikatsiyalangan
--  so'rovda yuklaydi. Indekssiz bu `users_roles` jadvalini har safar to'liq
--  skanerlash degani.
--
--  500 000 foydalanuvchida har bir API chaqiruvi 500 000 satrni ko'rib
--  chiqardi.

create index if not exists idx_users_roles_user on users_roles (user_id);

--  Teskari yo'nalish: xodimlarni va ilova foydalanuvchilarini ajratish
--  (§33, §35) `users_roles` dan `role` ga qarab bajariladi.

create index if not exists idx_users_roles_role on users_roles (roles_id);


-- --------------------------------------------------------------------------
--  2. TARJIMALAR — cheklov tartibi noto'g'ri edi
-- --------------------------------------------------------------------------
--
--  Tarjima jadvallarida `UNIQUE(parent_id, locale)` bo'lsa, u `where
--  parent_id = ?` so'roviga ham xizmat qiladi (prefiks qoidasi).
--
--  ⚠️ Lekin BESHTA jadvalda tartib teskari: `UNIQUE(locale, parent_id)`.
--  Unikallik ikkala tartibda ham to'g'ri ishlaydi, LEKIN indeks
--  ishlatilmaydi: birinchi ustun `locale` va unda atigi uchta xil qiymat
--  bor.
--
--  Tarjimalar esa doimiy yuklanadi: har bir kontent ro'yxati, har bir
--  muharrir ochilishi, har bir bosh sahifa.
--
--  Cheklovning O'ZI o'zgartirilmaydi — u to'g'ri ishlayapti va uni
--  qayta qurish mavjud ma'lumotni qayta yozishni talab qilardi.

create index if not exists idx_homepage_tr_section on cms_homepage_section_translation (section_id);
create index if not exists idx_notification_tr_parent on cms_notification_translation (notification_id);
create index if not exists idx_premiere_tr_parent on cms_premiere_translation (premiere_id);
create index if not exists idx_season_tr_parent on cms_season_translation (season_id);
create index if not exists idx_tariff_tr_parent on cms_tariff_translation (tariff_id);


-- --------------------------------------------------------------------------
--  3. JANR BOG'LANISHI
-- --------------------------------------------------------------------------
--
--  ⚠️ FAQAT `genre_id`.
--
--  Jadvalda `PRIMARY KEY (content_id, genre_id)` bor va u `content_id`
--  bo'yicha so'rovlarga allaqachon xizmat qiladi (prefiks qoidasi).
--  Ikkinchi indeks qo'shish AYNAN ТЗ ogohlantirgan holat bo'lardi:
--  «Sababsiz har bir fieldga index qo'yma».
--
--  `genre_id` esa birlamchi kalitning IKKINCHI ustuni — u bo'yicha
--  alohida qidirish (janr bo'yicha kontent) indekssiz qoladi.

create index if not exists idx_content_genre_genre on cms_content_genre (genre_id);


-- --------------------------------------------------------------------------
--  4. OBUNA → TARIF
-- --------------------------------------------------------------------------
--
--  Hisobot tarif bo'yicha filtrlanadi (§47) va tarif daromadi shu ustun
--  bo'yicha jamlanadi.

create index if not exists idx_subscription_tariff on cms_subscription (tariff_id);


-- --------------------------------------------------------------------------
--  5. MEDIA ISHLATILISHI — faqat O'SADIGAN jadvallarda
-- --------------------------------------------------------------------------
--
--  `MediaUsageService` faylni o'chirishdan oldin uni ishlatayotgan
--  jadvallarni tekshiradi (§26: ishlatilayotgan fayl o'chmaydi).
--
--  ⚠️ Bu tekshiruv HAMMA media FK'lari bo'yicha yuradi, lekin indeks
--  faqat ikkitasiga qo'yildi:
--
--    • `cms_content_media` va `cms_episode_video` — kontent hajmi bilan
--      birga o'sadi, minglab satrga yetadi;
--    • `cms_advertisement.image_media_id`, `cms_creator.photo_media_id`
--      va boshqalar — bu jadvallar yuzlab satrdan oshmaydi. Ularga indeks
--      qo'yish yozuv tezligini bekorga sekinlashtirardi.

create index if not exists idx_content_media_media on cms_content_media (media_id);
create index if not exists idx_episode_video_media on cms_episode_video (media_id);


-- --------------------------------------------------------------------------
--  6. IZOH → QISM
-- --------------------------------------------------------------------------
--
--  Moderatsiya ro'yxati qism bo'yicha filtrlanadi (§34).

create index if not exists idx_comment_episode on cms_comment (episode_id);
