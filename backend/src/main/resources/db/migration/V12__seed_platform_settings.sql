-- ==========================================================================
--  V12 — PLATFORMA SOZLAMALARINI URUG'LASH
-- ==========================================================================
--
--  MUAMMO
--  Sozlamalar faqat `SettingsService.all()` chaqirilganda yozilardi, u esa
--  admin «Sozlamalar» sahifasini ochganda ishlaydi. Ya'ni yangi
--  o'rnatishda `cms_platform_setting` BO'SH bo'lib turardi va
--  `getMoney(EPISODE_PRICE)` zaxira sifatida 0 qaytarardi.
--
--  Natijada: narxi alohida ko'rsatilmagan pullik qism foydalanuvchiga
--  «0 so'm» deb ko'rinardi.
--
--  IKKI QAVATLI YECHIM
--  1) Bu migratsiya satrlarni yaratadi — ular admin panelida darhol
--     ko'rinadi va tahrirlanadi.
--  2) Kod tomonda zaxira endi SettingKeys.defaultValue() dan olinadi —
--     satr qandaydir sababga ko'ra yo'q bo'lsa ham to'g'ri qiymat ishlaydi.
--
--  ⚠️ MAVJUD qiymatlar TEGILMAYDI: `where not exists` sharti bor. Admin
--  narxni allaqachon o'zgartirgan bo'lsa, migratsiya uni bosib o'tmaydi.
--
--  STAR_RATE va COIN_RATE ataylab 0 — buyurtmachi kursni hali aytmagan.
--  0 «sozlanmagan» degani, taxminiy raqam emas.
-- ==========================================================================

insert into cms_platform_setting (setting_key, setting_value, description)
select 'pricing.episode.default', '3000', 'Bitta qism narxi (so''m)'
where not exists (select 1 from cms_platform_setting where setting_key = 'pricing.episode.default');

insert into cms_platform_setting (setting_key, setting_value, description)
select 'pricing.premiere.default', '15000', 'Butun premyera narxi (so''m)'
where not exists (select 1 from cms_platform_setting where setting_key = 'pricing.premiere.default');

insert into cms_platform_setting (setting_key, setting_value, description)
select 'currency.star.rate', '0', '1 STAR necha so''m. 0 = kurs hali belgilanmagan'
where not exists (select 1 from cms_platform_setting where setting_key = 'currency.star.rate');

insert into cms_platform_setting (setting_key, setting_value, description)
select 'currency.coin.rate', '0', '1 COIN necha so''m. 0 = kurs hali belgilanmagan'
where not exists (select 1 from cms_platform_setting where setting_key = 'currency.coin.rate');

insert into cms_platform_setting (setting_key, setting_value, description)
select 'account.device.limit', '2', 'Bitta hisobdan maksimum qurilma soni'
where not exists (select 1 from cms_platform_setting where setting_key = 'account.device.limit');

insert into cms_platform_setting (setting_key, setting_value, description)
select 'revenue.creator.percent', '50', 'Ijodkorga tushadigan ulush (%)'
where not exists (select 1 from cms_platform_setting where setting_key = 'revenue.creator.percent');
