# Готовые ответы для анкет Play Console

> Раздел **Policy and programs → App content**. Ответы сверены с кодом
> приложения 13.09.2026 (после подтягивания изменений с GitHub), а не выписаны
> «как обычно бывает».
> Инструкция целиком — [docs/PLAY_STORE_RU.md](../../docs/PLAY_STORE_RU.md) §5.

⚠️ Если поведение приложения изменится (появится оплата, загрузка фото,
push-уведомления, рекламная сеть) — эти ответы надо пересмотреть до релиза.
Расхождение формы и приложения Google ловит автоматически.

---

## 1. Privacy policy

```
https://uzcasting.com/maxfiylik
```

---

## 2. Ads

**Yes, my app contains ads.**

Обоснование: полноэкранный баннер «Majburiy reklama» и рекламные карточки в
ленте (`src/features/ads/`, `src/features/home/sections.tsx`) приходят с
бэкенда и являются коммерческой рекламой. Сторонней рекламной сети (AdMob и
подобных) в приложении нет.

---

## 3. App access

Выбрать: **All or some functionality is restricted**.

**Add new instructions** — заполнять по-английски:

| Поле | Значение |
|---|---|
| Name | `Demo account (phone OTP)` |
| Username | `+998 90 000 00 00` ← заменить на реальный демо-номер |
| Password | `0000` ← ровно 4 цифры, поле в приложении четырёхзначное |

**Any other instructions:**

```
The app requires sign-in to show any content.

1. Launch the app and swipe through the onboarding screens, then tap the last button.
2. On the sign-in screen enter the phone number from the "Username" field.
3. Tap the confirm button. No real SMS is sent for this number.
4. Enter the code from the "Password" field.
5. Full catalog, casting listings and video playback are available after this step.
6. This demo account has an active Premium subscription, so paid content is unlocked for review.

The interface language can be switched to English in Profile -> Settings -> Language.

Alternative: you can also sign in with any Google account using the "Sign in with Google" button on the sign-in screen.
```

⚠️ Первые четыре шага зависят от демо-номера на бэкенде (§5.4 инструкции).
Пока его нет, оставить только последний абзац про Google — и обязательно
перевести OAuth-проект в статус `In production` (§7 инструкции), иначе у
ревьюера вход не сработает.

---

## 4. Content rating

| Вопрос | Ответ |
|---|---|
| Категория | **Entertainment** |
| Email для IARC | `uzcasting.org@gmail.com` |
| Насилие | по реальному каталогу |
| Сексуальный контент | по реальному каталогу |
| Ненормативная лексика | по реальному каталогу |
| Наркотики, алкоголь, табак | по реальному каталогу |
| Азартные игры (симуляция или реальные ставки) | **No** |
| Покупки внутри приложения | **No** — в сборке нет работающих покупок |
| Обмен сообщениями между пользователями | **No** — личной переписки в приложении нет |
| Доступ к местоположению и его передача другим | **No** |
| **Пользовательский контент** | **Yes** — с 10.09.2026 в приложении есть комментарии к фильмам и сериалам |
| Модерация пользовательского контента | **Yes** — модератор скрывает комментарии, нарушителей блокируют |

⚠️ Четыре первых пункта — про содержимое сериалов и фильмов, а не про само
приложение. Отвечать должен тот, кто знает каталог: заказчик или контент-менеджер.

⚠️ **Ответ про пользовательский контент изменился 10.09.2026.** До появления
комментариев весь контент публиковала только админка, и там честно стоял «нет».
Теперь «да» — а вместе с этим включается UGC-политика Google, у которой свои
требования к приложению: см. [PLAY_STORE_RU.md](../../docs/PLAY_STORE_RU.md) §6a.

---

## 5. Target audience and content

| Вопрос | Ответ |
|---|---|
| Target age groups | **18 and over** (или 16–17 + 18 and over, по итогам рейтинга) |
| Does your app appeal to children? | **No** |
| Store listing presence for children | не заполняется |

⚠️ Указывать возраст ниже 13 нельзя: включатся требования Families policy —
отдельная проверка рекламы, запрет на сбор части данных, свой набор анкет.

---

## 6. Data safety

### Общие ответы

| Вопрос | Ответ |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** — весь трафик идёт по HTTPS на `https://uzcasting.com` |
| Do you provide a way for users to request that their data is deleted? | **Yes** — после того, как появится §6 инструкции (эндпоинт + страница) |

⚠️ Третий пункт нельзя отмечать заранее: Google проверяет ссылку на страницу
удаления. Пока страницы нет — форму не отправлять.

### Что именно собираем

| Тип данных (категория Play) | Собираем | Передаём третьим лицам | Обязательно | Цели |
|---|---|---|---|---|
| Personal info → **Name** | Да | Нет | Да | App functionality, Account management |
| Personal info → **Phone number** | Да | Нет | Да | App functionality, Account management |
| Personal info → **Email address** | Да (только при входе через Google) | Нет | Да | App functionality, Account management |
| App activity → **App interactions** | Да | Нет | Нет | Analytics |
| App activity → **Other user-generated content** | Да | Нет | Нет | App functionality |
| Device or other IDs | Да | Нет | Да | App functionality, Fraud prevention and security |

Пояснения к последним двум строкам:

- **App interactions** — просмотры контента, показы и клики по баннерам
  (`src/features/analytics/api.ts`). Используются для статистики платформы,
  таргетинга по поведению пользователя нет: кому показывать рекламу, решает
  сервер по наличию подписки.
- **Other user-generated content** — тексты комментариев к фильмам и сериалам
  (`src/features/comments/`). Комментарий виден другим пользователям приложения —
  но это не «передача третьим лицам» в смысле формы: сторонним компаниям данные
  не уходят, поэтому в колонке «Передаём» стоит «нет». Автор может удалить свой
  комментарий, модератор — скрыть.
- **Device or other IDs** — случайный UUID установки
  (`src/features/devices/installationId.ts`) и имя устройства, которое человек
  сам задал в настройках телефона. Нужны для списка устройств и ограничения
  числа одновременных сессий. Это не рекламный идентификатор и не IMEI:
  идентификатор генерируется приложением и пропадает при переустановке.

### Чего мы НЕ собираем — отметить «нет» и не ошибиться

| Категория | Почему нет |
|---|---|
| Location | геолокация в коде не запрашивается |
| Financial info | платежей в приложении нет |
| Health and fitness | — |
| Messages | личной переписки между пользователями нет: комментарии публичные и учтены выше |
| Photos and videos | доступа к галерее и камере нет, разрешения убраны из манифеста |
| Audio files | — |
| Files and docs | — |
| Calendar, Contacts | — |
| Web browsing history | — |
| Installed apps | — |

---

## 7. Остальные декларации

| Анкета | Ответ |
|---|---|
| **Financial features** | `My app doesn't provide any financial features` |
| **Health apps** | No |
| **News apps** | No — приложение не новостное |
| **Government apps** | No |
| **COVID-19 contact tracing** | No |
| **Data deletion** | ссылка `https://uzcasting.com/hisobni-ochirish` — после §6 инструкции |
| **Advertising ID** | приложение **не использует** рекламный идентификатор Google |

⚠️ **Advertising ID.** Если когда-нибудь подключим AdMob или любую рекламную
SDK, эту декларацию и раздел Data safety придётся переписать, а в манифест
добавится разрешение `com.google.android.gms.permission.AD_ID`.
