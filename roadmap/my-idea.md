# UZCASTING — ADMIN WEB PLATFORM

## MASTER DEVELOPMENT PROMPT

Sen mavjud **UZCASTING / casting** loyihasi ustida ishlayotgan Senior Software Architect, Senior Java Spring Boot Developer va Senior React Developer sifatida ishlaysan.

Bu greenfield loyiha emas.

Repository ichida oldindan yozilgan **Java backend va React frontend kodi mavjud**. Mavjud kodni tekshirmasdan yangi arxitektura yaratish, ishlayotgan funksiyalarni o‘chirib yuborish yoki bir xil modulni ikkinchi marta yozish mumkin emas.

Asosiy vazifa:

> Mavjud `casting` loyihasi ustiga professional, production-ready, kengaytiriladigan **UZCASTING Admin Web Platform** yaratish.

UZCASTING — qisqa metrajli kinolar, mini-seriallar, seriallar, filmlar, podcastlar va boshqa media kontentlarni joylashtirish, boshqarish, monetizatsiya qilish, ijodkorlarni boshqarish, reklama, premyera, notification, tarif, donat va analitikani boshqaruvchi platforma.

---

# 0. ENG MUHIM QOIDA — ISHNI KOD YOZISHDAN BOSHLAMA

Birinchi navbatda repository'ni to‘liq audit qil.

Avval mavjud:

* backend;
* frontend;
* database;
* authentication;
* authorization;
* user entity;
* role tizimi;
* media upload;
* casting moduli;
* API;
* konfiguratsiyalar;
* migration;
* Docker;
* environment;
* mavjud UI komponentlar;
* mavjud route'lar

ni tekshir.

## Birinchi bajariladigan ish

Repository root'da:

```text
roadmap.md
```

faylini yarat.

Agar:

```text
/backend
/frontend
```

papkalari mavjud bo‘lsa, ularni qayta yaratma.

Agar mavjud bo‘lmasa, loyiha arxitekturasiga mos holda yarat.

Backend ichida:

```text
/backend/BACKEND_ROADMAP.md
/backend/ARCHITECTURE.md
```

Frontend ichida:

```text
/frontend/FRONTEND_ROADMAP.md
/frontend/ARCHITECTURE.md
```

yarat.

Agar repository strukturasida backend/frontend boshqa nom bilan mavjud bo‘lsa, mavjud strukturani buzib majburan boshqa papkaga ko‘chirma. Shu mavjud modul ichiga roadmap fayllarini joylashtir va root `roadmap.md` ichida real path'larni ko‘rsat.

---

# 1. ROADMAP.MD MAJBURIY FORMAT

`roadmap.md` doim loyiha holatini aks ettirib tursin.

Quyidagi bo‘limlar bo‘lsin:

```md
# UZCASTING DEVELOPMENT ROADMAP

## 1. Project Goal

## 2. Existing Project Audit

## 3. Architecture Decisions

## 4. Development Phases

## 5. Current Tasks

## 6. Completed Tasks

## 7. Pending Tasks

## 8. Bugs / Technical Debt

## 9. Database Changes

## 10. API Changes

## 11. Frontend Changes

## 12. Testing Status

## 13. Important Decisions

## 14. Next Exact Steps
```

Task statuslari:

```text
[ ] TODO
[~] IN PROGRESS
[x] DONE
[!] BLOCKED
```

Har bir katta ish tugagandan keyin `roadmap.md` yangilansin.

Faqat kod yozib ketma.

Qaysi ish bajarilgan bo‘lsa:

```text
[x]
```

qilib belgilansin.

Hali bajarilmagan ishni DONE deb belgilama.

Build yoki testdan o‘tmagan funksiyani “completed” deb yozma.

---

# 2. BACKEND_ROADMAP.MD

Backend uchun alohida batafsil checklist yurit:

* existing backend audit;
* package architecture;
* authentication;
* JWT;
* refresh token;
* RBAC;
* permissions;
* users;
* staff;
* categories;
* genres;
* creators;
* content;
* seasons;
* episodes;
* media;
* ads;
* premieres;
* notifications;
* comments;
* subscriptions;
* tariffs;
* donations;
* analytics;
* reports;
* audit logs;
* database migrations;
* indexes;
* validation;
* exception handling;
* tests;
* security;
* OpenAPI;
* performance.

Har bir entity, migration, service va endpoint bo‘yicha tasklarni yoz.

---

# 3. FRONTEND_ROADMAP.MD

Frontend bo‘yicha:

* existing React audit;
* layout;
* routing;
* authentication;
* permission guards;
* dashboard;
* staff management;
* content management;
* movie editor;
* series editor;
* episode editor;
* media uploader;
* creator management;
* category management;
* genre management;
* homepage management;
* ads;
* premieres;
* notifications;
* comments;
* users;
* subscriptions;
* tariffs;
* donations;
* reports;
* audit logs;
* settings;
* forms;
* validation;
* loading/error/empty states;
* responsive;
* tests.

Har bir page bo‘yicha checklist yurit.

---

# 4. SOURCE OF TRUTH

Miro'dagi eski TZ'da “social network / messenger” konsepsiyasi ham mavjud.

**U konsepsiyani yangi platformaning asosiy vazifasi deb qabul qilma.**

Yangi asosiy konsepsiya:

> UZCASTING — video streaming, qisqa kino, mini-serial, serial, ijodkorlar, casting, monetizatsiya va media platformasi.

Hozir:

* friends system;
* messenger;
* social feed;
* private chat

asosiy development scope'ga kirmaydi.

Agar oldingi `casting` kodida ishlayotgan casting funksiyalari mavjud bo‘lsa, ularni o‘chirib tashlama.

Existing Casting module regressiyaga uchramasligi kerak.

---

# 5. TEXNOLOGIYA

## Backend

Mavjud backend Java'da yozilgan.

Mavjud versiya va frameworkni birinchi navbatda aniqlagin.

Agar Spring Boot ishlatilayotgan bo‘lsa, mavjud Spring Boot arxitekturasini davom ettir.

Asosiy stack:

* Java
* Spring Boot
* Spring Security
* PostgreSQL
* REST API
* Bean Validation
* database migration
* OpenAPI/Swagger
* JWT / existing auth mechanism

Mavjud projectda Flyway bo‘lsa Flyway ishlat.

Liquibase bo‘lsa Liquibase davom ettir.

Ikkalasini parallel kiritma.

## Frontend

Mavjud React projectni davom ettir.

Agar TypeScript ishlatilayotgan bo‘lsa TypeScript davom ettir.

Agar JavaScript project bo‘lsa butun loyihani sabab­siz TypeScript'ga rewrite qilma.

Mavjud:

* routing;
* HTTP client;
* state management;
* UI framework;
* design system;
* form library

bo‘lsa qayta yozma.

Avval mavjud implementationni reuse qil.

---

# 6. ASOSIY ROLLAR

Tizimda 5 ta asosiy role mavjud:

```text
1. HYPER_ADMIN
2. SUPER_ADMIN
3. ADMIN
4. WORKER
5. USER
```

## Muhim

Hozir web admin panel faqat:

```text
HYPER_ADMIN
SUPER_ADMIN
ADMIN
WORKER
```

uchun.

`USER` uchun hozir mobil dastur yozilmaydi.

USER admin panelga kira olmaydi.

USER kelajakda React Native mobil dasturdan foydalanadi.

Backend/data model kelajak mobil ilovaga mos bo‘lishi kerak, lekin hozir USER uchun mobil UI yozma.

---

# 7. ROLE HIERARCHY

## HYPER_ADMIN

Platformadagi eng yuqori rol.

Huquqlari:

* barcha modullarga kirish;
* SuperAdmin yaratish;
* Admin yaratish;
* Worker yaratish;
* rollarni boshqarish;
* permissions boshqarish;
* barcha staff accountlarni ko‘rish;
* block/unblock;
* system settings;
* tariflar;
* monetizatsiya;
* audit log;
* analytics;
* platform configuration;
* barcha content;
* barcha userlar.

HyperAdmin boshqa HyperAdmin yaratish masalasida mavjud project logikasiga qarab xavfsiz yechim tanla.

Privilege escalation bo‘lmasin.

---

# 8. SUPER_ADMIN

SuperAdmin:

* Admin yaratishi;
* Worker yaratishi;
* Admin/Worker accountlarini boshqarishi;
* content boshqarishi;
* categories;
* genres;
* creators;
* advertisements;
* premieres;
* notifications;
* comments;
* users;
* subscriptions;
* tariffs;
* donations;
* reports

ni boshqara olsin.

SuperAdmin HyperAdmin yaratolmasin.

---

# 9. ADMIN

Admin:

* Worker yaratishi;
* Workerlarni tahrirlashi;
* content boshqarishi;
* kino/serial joylashtirishi;
* category;
* genre;
* creators;
* advertisements;
* premieres;
* notification;
* comments moderation;
* users;
* premium;
* reports;
* homepage content

bilan ishlay olsin.

Admin:

* SuperAdmin yaratolmasin;
* HyperAdmin yaratolmasin;
* boshqa Admin yaratolmasin.

---

# 10. WORKER

Worker Admin yoki SuperAdmin tomonidan yaratiladi.

HyperAdmin global huquqi sababli Worker yaratishi mumkin.

Worker default holatda:

* content yaratish;
* content tahrirlash;
* media upload;
* categories bilan ishlash;
* genres bilan ishlash;
* creators bilan ishlash;
* reklama qo‘shish;
* premiere qo‘shish;
* comments moderation

kabi operatsiyalarni bajara oladi.

Lekin Worker uchun fine-grained permission tizimi bo‘lsin.

Masalan:

```text
CONTENT_VIEW
CONTENT_CREATE
CONTENT_EDIT
CONTENT_DELETE
CONTENT_PUBLISH

CATEGORY_VIEW
CATEGORY_CREATE
CATEGORY_EDIT

CREATOR_VIEW
CREATOR_CREATE
CREATOR_EDIT

ADVERTISEMENT_VIEW
ADVERTISEMENT_CREATE
ADVERTISEMENT_EDIT

PREMIERE_VIEW
PREMIERE_CREATE
PREMIERE_EDIT

COMMENT_VIEW
COMMENT_MODERATE

REPORT_VIEW
```

Admin/SuperAdmin Worker yaratganda ruxsatlarini tanlay olsin.

Worker o‘zida mavjud bo‘lmagan permissionni boshqaga bera olmaydi.

---

# 11. BACKEND AUTHORIZATION — JUDA MUHIM

Frontend menu yashirilishi xavfsizlik hisoblanmaydi.

Barcha authorization backendda ham tekshirilsin.

Spring Security orqali:

* role-based;
* permission-based

authorization yoz.

Masalan:

```text
HYPER_ADMIN
SUPER_ADMIN
ADMIN
WORKER + permission
```

Endpoint darajasida himoya bo‘lsin.

Privilege escalationga yo‘l qo‘yma.

---

# 12. STAFF MANAGEMENT

Admin panelda:

## Staff list

Ko‘rsat:

* ID;
* avatar;
* full name;
* phone;
* email;
* role;
* status;
* createdBy;
* createdAt;
* lastLoginAt.

Filter:

* role;
* active/inactive;
* qidiruv;
* creation date.

Actions:

* create;
* edit;
* activate;
* deactivate;
* reset password;
* permissions;
* block;
* unblock.

Hard delete o‘rniga imkon qadar deactivate/soft delete ishlat.

---

# 13. CONTENT PLATFORM — ASOSIY MODUL

Platformadagi content turlicha bo‘lishi mumkin.

Masalan:

```text
SHORT_FILM
MOVIE
MINI_SERIES
SERIES
PODCAST
SHOW
INTERVIEW
OTHER
```

Bu enum yoki mavjud projectga mos dictionary orqali boshqarilishi mumkin.

Lekin:

**content type**

va

**category**

bir xil narsa bo‘lmasin.

Misol:

Content type:

```text
MINI_SERIES
```

Category:

```text
Drama
```

Genre:

```text
Romance
```

---

# 14. CONTENT STRUCTURE

Kontent uch xil structure'ni qo‘llab-quvvatlashi kerak:

```text
SINGLE
EPISODIC
SEASONAL
```

## SINGLE

Bitta qismlik kino.

Misol:

```text
Qisqa metrajli film
Film
Podcast episode
```

## EPISODIC

Faslsiz, bir nechta qismdan iborat:

```text
Mini serial
1-qism
2-qism
3-qism
...
```

## SEASONAL

```text
Serial
  Season 1
    Episode 1
    Episode 2
  Season 2
    Episode 1
```

Arxitektura barcha holatlarni qo‘llab-quvvatlasin.

---

# 15. CONTENT ENTITY

Content uchun kamida:

```text
id
title
slug
shortDescription
description

contentType
structureType

status
visibility

premiereDate
publicationDate

durationMinutes

language
ageRating

featured
popular

createdBy
updatedBy
createdAt
updatedAt
deletedAt/version
```

kerak.

Statuslar:

```text
DRAFT
IN_REVIEW
SCHEDULED
PUBLISHED
ARCHIVED
BLOCKED
```

Mavjud loyiha talabiga qarab moslashtir.

---

# 16. CATEGORY

Admin va tegishli Worker:

* category yaratish;
* edit;
* activate/deactivate;
* sort order;
* image/icon;
* description

qila olishi kerak.

Masalan:

```text
Drama
Kulguli
Podcast
Mini seriallar
Intervyu
Romantika
Hujjatli
```

Category foydalanuvchi mobil ilovasining bosh menyusida chiqadi.

Category uchun:

```text
name
slug
description
image/icon
sortOrder
active
```

bo‘lsin.

---

# 17. GENRES

Genre alohida boshqarilsin.

Masalan:

```text
Drama
Comedy
Romance
Action
Thriller
Documentary
Family
Crime
Adventure
```

Bitta content bir nechta genre'ga ega bo‘lishi mumkin.

---

# 18. CONTENT MEDIA

Contentga:

* poster;
* cover;
* thumbnail;
* bir nechta rasm;
* trailer;
* teaser;
* asosiy video;
* qisqa video;
* gallery

yuklash mumkin bo‘lsin.

Bir contentda **bir nechta rasm** bo‘lishi mumkin.

Gallery'dagi tartibni o‘zgartirish imkoniyati bo‘lsin.

---

# 19. VIDEO PARTS

Ba'zi kinolar yoki epizodlar bitta katta video emas, bir nechta video segmentdan iborat bo‘lishi mumkin.

Masalan:

```text
Episode 1
  Part 1
  Part 2
  Part 3
  Part 4
```

Shuning uchun database faqat:

```text
episode.videoUrl
```

degan bitta fieldga bog‘lanmasin.

`VideoAsset / MediaAsset` va tartiblangan relation ishlat.

Har bir video uchun:

```text
id
storageKey
url
originalFilename
mimeType
size
duration
width
height
status
sortOrder
```

kabi metadata saqlansin.

---

# 20. SEASON

Season:

```text
id
contentId
seasonNumber
title
description
poster
premiereDate
status
sortOrder
```

---

# 21. EPISODE

Episode:

```text
id
contentId
seasonId nullable
episodeNumber
title
shortDescription
description
thumbnail
duration
premiereDate
publicationDate
status
sortOrder
```

Faslsiz mini-seriallarda:

```text
seasonId = null
```

bo‘lishi mumkin.

Har bir episode bir yoki bir nechta video assetga ega bo‘lsin.

---

# 22. CONTENT CREATION UI

Admin/Worker content qo‘shayotganda bitta chalkash katta form yasama.

Professional multi-step editor qil.

Masalan:

## Step 1 — Basic information

* title;
* short description;
* full description;
* content type;
* structure;
* category;
* genres;
* language;
* age rating;
* duration;
* premiere date.

## Step 2 — Media

* poster;
* cover;
* gallery;
* trailer;
* videos.

## Step 3 — Creators

* actors;
* actresses;
* director;
* model;
* producer;
* other creators.

## Step 4 — Episodes

Structure EPISODIC/SEASONAL bo‘lsa.

## Step 5 — Monetization / Access

* Free;
* Premium;
* Single purchase;
* Premium or single purchase.

## Step 6 — Publication

* draft;
* schedule;
* publish;
* featured;
* home page visibility.

---

# 23. CONTENT ACCESS MODEL

Kontent access policy:

```text
FREE
PREMIUM_ONLY
PURCHASE_ONLY
PREMIUM_OR_PURCHASE
```

bo‘lishi kerak.

Episode darajasida override qilish imkoniyati kerak bo‘lsa arxitektura bunga tayyor bo‘lsin.

Bitta seriyani sotib olish default narxi:

```text
3 000 UZS
```

Lekin bu kod ichida hardcoded bo‘lmasin.

Admin settings orqali o‘zgartirish mumkin bo‘lsin.

---

# 24. IJODKORLAR / CREATORS

Alohida Creator moduli bo‘lsin.

Creator:

```text
id
firstName
lastName
middleName
displayName
photo
coverImage
bio
birthDate optional
active
featured
createdAt
updatedAt
```

Role/Profession:

```text
ACTOR
DIRECTOR
MODEL
PRODUCER
SCREENWRITER
HOST
CREATOR
OTHER
```

Kelajakda yangi profession qo‘shish zarur bo‘lishi mumkinligi uchun mavjud arxitekturaga qarab extensible qil.

Content va Creator ko‘pdan-ko‘p relationship bo‘lsin.

`ContentCredit` kabi relation ishlat:

```text
contentId
creatorId
role
characterName optional
sortOrder
```

Bitta ijodkor bir kinoda aktyor, boshqa kinoda rejissyor bo‘lishi mumkin.

---

# 25. MASHHUR IJODKORLAR

Homepage'da:

**Mashhur ijodkorlar**

section mavjud bo‘ladi.

Admin:

* featured creator tanlashi;
* tartibini belgilashi;
* activate/deactivate qilishi

mumkin bo‘lsin.

Keyinchalik analytics asosida avtomatik ranking qo‘shish mumkin.

Hozir manual featured/sort imkoniyati yetarli, ammo arxitektura analytics rankingga mos bo‘lsin.

---

# 26. MEDIA LIBRARY

Markazlashtirilgan Media Library yarat.

Image va videoni har modul uchun qayta-qayta alohida upload logika bilan yozma.

`MediaAsset` abstraction yarat.

Media:

```text
IMAGE
VIDEO
DOCUMENT
```

turlarini qo‘llab-quvvatlasin.

Media library orqali:

* upload;
* preview;
* search;
* filter;
* reuse;
* remove/archive;
* metadata

boshqarilsin.

Upload vaqtida faylni butunlay RAMga yuklab olib keyin storagega jo‘natadigan xavfli implementation qilma.

Katta videolar uchun stream/multipart yechim ishlat.

Mavjud Timeweb/local storage integratsiyasi bo‘lsa reuse qil.

Provider nomini business logic ichiga hardcode qilma.

Masalan:

```text
StorageService
```

abstraction yaratish mumkin.

Kelajakda:

```text
Local
Timeweb
S3-compatible
```

provider almashtirilishi mumkin bo‘lsin.

---

# 27. REKLAMA / ADVERTISEMENT MODULE

Homepage'da reklama carousel bo‘ladi.

Admin va permissionli Worker reklama qo‘sha olsin.

Advertisement:

```text
id
title
description optional

image
mobileImage optional

buttonEnabled
buttonText optional

linkType
linkUrl optional

internalTargetType optional
internalTargetId optional

startAt
endAt

status
sortOrder

createdBy
createdAt
updatedAt
```

Link optional.

Button ham optional.

Misol:

```text
buttonEnabled = false
```

bo‘lsa tugma umuman chiqmaydi.

---

# 28. AD LINK TYPES

Link:

```text
NONE
EXTERNAL
INTERNAL
```

Internal target:

```text
CONTENT
EPISODE
CATEGORY
CREATOR
CASTING
PREMIERE
OTHER
```

Bir xil internal-link mexanizmini advertisement, premiere va notification uchun reuse qilishga harakat qil.

---

# 29. ADVERTISEMENT ANALYTICS

Har bir reklama uchun Admin ko‘ra olishi kerak:

```text
Impressions
Clicks
Unique impressions
Unique clicks
CTR
```

Eng kamida:

```text
necha kishi ko‘rdi
necha kishi bosdi
```

ko‘rsatilishi shart.

Future mobile client event yubora oladigan yengil analytics endpoint design qil.

Admin dashboard har safar millionlab raw events ustida `COUNT(*)` qilmasin.

Kerak bo‘lsa daily aggregate design qil.

Masalan:

```text
AdDailyStatistic
```

---


# 30. PREMIERE MODULE

“Yangi premyeralar” alohida CMS moduli bo‘lsin.

Premiere card quyidagilarni qo‘llab-quvvatlasin:

* rasm;
* video;
* title;
* text;
* description;
* external link;
* internal link;
* contentga link;
* CTA button;
* start date;
* end date;
* sort order;
* active/inactive.

Masalan:

```text
Qalbing egasi
Tez kunda
Treylerni ko‘rish
```

Premiere boshqa website'ga yoki shu app ichidagi:

* film;
* serial;
* episode;
* creator;
* casting

sahifasiga o'tishi mumkin.

---

# 31. HOMEPAGE MANAGEMENT

Mobil app keyinchalik bosh sahifani backenddan oladi.

Shuning uchun homepage hardcoded bo‘lmasin.

Admin web'da homepage management yaratilishi kerak.

Asosiy sectionlar:

```text
Advertisement carousel
New premieres
Categories
Featured content
Popular content
Mini series
Popular creators
Custom content rows
```

Har section:

* enabled;
* disabled;
* sortOrder

ga ega bo‘lsin.

Category/content rows tartibi boshqarilsin.

---

# 32. NOTIFICATIONS

Notificationlar 2 asosiy turda:

```text
APP_NOTIFICATION
CASTING_NOTIFICATION
```

## APP_NOTIFICATION

Umumiy ilova notification.

## CASTING_NOTIFICATION

Casting bilan bog‘liq maxsus notification.

Notification:

```text
id
type
title
body
image optional

linkType
internalTargetType
internalTargetId
externalUrl

audienceType

scheduledAt
sentAt

status
createdBy
```

Status:

```text
DRAFT
SCHEDULED
SENDING
SENT
FAILED
CANCELLED
```

Admin/SuperAdmin notification yaratib schedule qila olsin.

Worker uchun permission orqali berilsin.

Agar mavjud projectda Firebase FCM integration bo‘lsa reuse qil.

Credentiallarni source code'ga yozma.

Agar provider configure qilinmagan bo‘lsa fake successful response qaytarma.

---

# 33. NOTIFICATION REPORT

Ko‘rsatish imkoniyati:

```text
sent
delivered
opened
clicked
failed
```

Mavjud infrastructure qaysi metricni real berishi mumkinligini aniqlab ishlat.

Real ma'lumot bo‘lmasa fake statistic yaratma.

---

# 34. COMMENTS

USER kelajakda kino/seriallarga comment yozadi.

Admin panelda comment moderation bo‘lsin.

List:

```text
user
content
episode optional
comment
createdAt
status
reportsCount
```

Actions:

* view;
* hide;
* restore;
* delete/soft delete;
* block user where authorized.

Filter:

* content;
* user;
* date;
* status;
* text search.

---

# 35. USER MANAGEMENT

USER mobil uchun bo‘lsa ham admin panelda userlarni ko‘rish kerak.

User list:

```text
id
avatar
name
phone
email
status
premium status
premium expiresAt
createdAt
lastActiveAt
```

Admin:

* search;
* view;
* block;
* unblock;
* premium grant;
* premium revoke

qila olishi kerak.

User uchun web admin login mavjud bo‘lmasin.

---

# 36. PREMIUM TARIFLAR

Admin panelda tariff management bo‘lsin.

Initial seed sifatida:

```text
1 oy   — 24 000 UZS
3 oy   — 49 999 UZS
6 oy   — 99 000 UZS
12 oy  — 159 900 UZS
```

qo‘yish mumkin.

Lekin narxlar hardcoded bo‘lmasin.

Admin panel orqali o‘zgartirilishi shart.

Tariff:

```text
id
name
durationDays/months
price
currency
active
sortOrder
description
features
```

Pul qiymatini floating point bilan saqlama.

`BigDecimal` yoki currency modelga mos integer/minor-unit strategiya ishlat.

---

# 37. PREMIUM HUQUQLARI

Premium foydalanuvchiga:

* barcha Premium kontent;
* premyeralar;
* seriallar;
* filmlar;
* reklamasiz ko‘rish;
* Premium content;
* Casting loyihasiga kirish

kabi ruxsatlar berishi mumkin.

Bu business logic markazlashtirilgan bo‘lsin.

Frontend/mobile ichida scattered condition yozib tashlama.

---

# 38. PREMIUM SOVG‘A QILISH

Admin panel orqali userni:

```text
telefon raqami
email
ID
```

orqali topib:

* premium sovg‘a qilish;
* muddat belgilash;
* premiumni uzaytirish;
* premiumni bekor qilish/tortib olish

mumkin bo‘lsin.

Har bir action Audit Logga tushsin.

---

# 39. DONATION SYSTEM

UZCASTING'da ikkita virtual donation currency bo‘lishi mumkin:

```text
STARS
UZCASTING_COIN
```

User sevimli:

* creator;
* actor;
* actress;
* content

ni qo‘llab-quvvatlashi mumkin.

Donat har bir kontent/ijodkor bo‘yicha alohida hisoblanishi kerak.

---

# 40. STARS

Packages:

```text
10
50
100
500
1000
```

boshlang‘ich qiymatlar bo‘lishi mumkin.

Lekin:

```text
1 STAR = X UZS
```

admin panel orqali boshqarilishi kerak.

---

# 41. UZCASTING COIN

```text
1 UZCASTING_COIN = X UZS
```

qiymati ham admin tomonidan boshqarilsin.

Admin:

* exchange rate;
* packages;
* active/inactive

ni boshqara olsin.

---

# 42. DONATION REPORT

Admin dashboard/reportda:

* total donations;
* Stars donations;
* Coin donations;
* top creators;
* top donated content;
* daily/monthly amounts;
* transaction list

bo‘lsin.

Transaction uchun immutable history saqla.

Financial historyni hard delete qilma.

---

# 43. DONATION BALANCE

Kelajak mobil User profilida:

```text
Stars balance
UZCASTING Coin balance
```

ko‘rinadi.

Hozir mobil UI yozilmaydi.

Lekin backend/data model buning uchun tayyor bo‘lsin.

---

# 44. PAYMENT

Donation sotib olish:

1. user internal balance;
2. payment system

orqali bo‘lishi mumkin.

Hozir mavjud payment integration bo‘lsa audit qilib reuse qil.

Mavjud bo‘lmasa fake payment gateway yaratib production-ready deb ko‘rsatma.

Payment provider uchun abstraction/interface tayyorlash mumkin.

---

# 45. ANALYTICS

Admin dashboard'da imkon qadar:

```text
Total users
Active users
Premium users
New users

Total contents
Published contents
Draft contents

Total views
Top viewed content

Total ads
Ad impressions
Ad clicks
Ad CTR

Total subscriptions
Subscription revenue

Single purchase revenue

Donation revenue

Top creators

Comments
Notifications
```

ko‘rsat.

Data mavjud bo‘lmagan statisticni fake qilib chiqarma.

Empty state ko‘rsat.

---

# 46. CONTENT ANALYTICS

Kelajak mobil app:

```text
CONTENT_VIEW
CONTENT_PLAY
CONTENT_COMPLETE
```

eventlari yuborishi mumkin.

Hozir admin hisobot tizimini shu analytics modelga mos qilib design qil.

Kerakli minimal event ingestion endpointlarni backendda tayyorlash mumkin.

Mobil UI yozilmaydi.

---

# 47. REPORT FILTERS

Reportlar:

* today;
* yesterday;
* last 7 days;
* last 30 days;
* custom period

bo‘yicha filterlansin.

Shuningdek:

* content;
* category;
* creator;
* tariff;
* advertisement

bo‘yicha filter bo‘lsin.

---

# 48. DASHBOARD

Login bo‘lgandan keyingi birinchi page professional dashboard bo‘lsin.

Cards:

* users;
* premium;
* content;
* views;
* revenue;
* ads;
* donations.

Charts:

* user growth;
* views;
* subscription revenue;
* donations.

Tables:

* latest content;
* top content;
* latest users;
* best ads;
* top creators.

Faqat real API data ishlat.

---

# 49. ADMIN WEB SIDEBAR

Taxminiy struktura:

```text
Dashboard

Content
  All Content
  Movies
  Mini Series
  Series
  Podcasts

Categories
Genres

Creators

Media Library

Homepage

Advertisements
Premieres

Notifications

Comments

Users
Subscriptions
Tariffs

Donations

Analytics
Reports

Staff
  Super Admins
  Admins
  Workers

Casting
  Existing casting modules

Audit Logs

Settings
```

Actual route/module mavjud projectga moslashtirilsin.

Role/permissionga qarab menu yashirilsin.

Lekin backend authorization baribir majburiy.

---

# 50. BRAND / UI

Miro design konsepsiyasidagi asosiy ranglar:

* dark maroon / bordo;
* dark olive green.

Agar existing design tokenlar mavjud bo‘lsa ulardan foydalan.

Exact color aniqlanmagan bo‘lsa kodning hamma joyiga hex rang tarqatib yuborma.

CSS variables/design tokens yarat:

```text
brand-primary
brand-secondary
background
surface
text
muted
danger
warning
success
```

UI professional admin dashboard bo‘lsin.

---

# 51. UI TALABLARI

Har bir list page'da:

* search;
* filters;
* pagination;
* sorting;
* loading state;
* empty state;
* error state;
* retry

bo‘lsin.

Har bir CRUD page'da:

* create;
* edit;
* details;
* activate/deactivate;
* confirmation dialog

kerak.

Delete actionlarga confirmation qo‘y.

---

# 52. FORM VALIDATION

Faqat frontend validationga tayanma.

Validation:

Frontend + Backend.

Xatolar field yonida aniq ko‘rsatilsin.

Backend bir xil error format ishlatsin.

Masalan:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "...",
  "errors": []
}
```

Mavjud projectda boshqa error convention bo‘lsa uni davom ettir.

---

# 53. CONTENT EDITOR UX

Content editor:

* tab yoki stepper;
* unsaved changes warning;
* upload progress;
* image preview;
* video preview;
* reorder;
* creator search/add;
* category search/select;
* genre multi-select;
* schedule publishing.

Content form 100 ta inputli bitta giant component bo‘lmasin.

Componentlarga ajrat.

---

# 54. CREATOR SELECTION

Content qo‘shayotganda:

```text
Add creator
```

bosilganda:

1. existing creator qidirish;
2. topilmasa yangi creator yaratish

mumkin bo‘lsin.

Yangi creator uchun:

* image;
* first name;
* last name;
* middle name;
* display name;
* role/profession.

Keyin shu creator contentga avtomatik biriktirilsin.

---

# 55. SEARCH

Global yoki module-specific search:

* content title;
* creator;
* user;
* phone;
* email;
* category

uchun samarali ishlasin.

Backend querylarda katta database uchun indexlarni hisobga ol.

---

# 56. DATABASE QOIDALARI

PostgreSQL.

Migration-based development.

Production database'ni drop qiladigan migration yozma.

Existing data saqlansin.

Relationshiplar to‘g‘ri FK bilan ishlasin.

Muhim ustunlarda index:

* slug;
* status;
* category;
* publication/premiere dates;
* phone;
* email;
* role;
* content relations

kabi query patternlarga qarab qo‘yilsin.

Sababsiz har bir fieldga index qo‘yma.

---

# 57. ID STRATEGY

Existing project qaysi ID strategiyasidan foydalansa, uni imkon qadar davom ettir.

Mavjud entity `Long` ishlatsa hamma narsani bir kunda UUIDga rewrite qilma.

Consistency saqla.

---

# 58. SOFT DELETE

Quyidagi business datalarda hard delete ehtiyotkorlik bilan ishlatilsin:

* content;
* creator;
* user;
* staff;
* advertisements;
* comments;
* tariffs.

Kerakli joyda:

```text
deletedAt
active
archived
```

strategiyasidan foydalan.

Audit/financial transaction tarixini o‘chirib yuborma.

---

# 59. AUDIT LOG

Muhim admin actions log qilinsin.

Masalan:

```text
ADMIN_CREATED
WORKER_CREATED

ROLE_CHANGED
PERMISSION_CHANGED

CONTENT_CREATED
CONTENT_UPDATED
CONTENT_PUBLISHED
CONTENT_ARCHIVED

ADVERTISEMENT_CREATED
ADVERTISEMENT_UPDATED

PREMIUM_GRANTED
PREMIUM_REVOKED

TARIFF_CHANGED

COMMENT_HIDDEN

NOTIFICATION_SENT
```

Audit log:

```text
actorId
actorRole
action
entityType
entityId
before optional
after optional
ip
userAgent
timestamp
```

Sensitive password/tokenlarni auditga yozma.

Audit log oddiy Admin tomonidan o‘chirib tashlanmasin.

---

# 60. CONCURRENCY

Bir contentni ikki admin bir vaqtda edit qilganda ikkinchisi birinchining o‘zgarishini indamay overwrite qilmasin.

Mavjud arxitekturaga mos bo‘lsa optimistic locking:

```java
@Version
```

ishlat.

---



AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
# 61. AUTHENTICATION

Existing authni audit qil.

Agar mavjud auth yaxshi va xavfsiz bo‘lsa rewrite qilma.

Agar yangi auth kerak bo‘lsa:

* Spring Security;
* short-lived access token;
* refresh token;
* refresh token rotation;
* logout/revoke;
* secure password hashing;
* rate limiting;
* failed login protection

ko‘zda tutilsin.

Refresh tokenni localStorage'ga tashlashdan oldin xavfsizlikni hisobga ol.

Existing architecturega mos secure implementation tanla.

---

# 62. PASSWORD

Password plain text saqlanmasin.

Mavjud project:

```text
BCrypt
Argon2
```

dan qaysi birini ishlatayotgan bo‘lsa, consistency saqla.

---

# 63. 2FA

Kamida arxitektura quyidagi rollar uchun 2FA qo‘shishga tayyor bo‘lsin:

```text
HYPER_ADMIN
SUPER_ADMIN
```

Agar hozir implementation katta scope talab qilsa roadmapga alohida security task qilib yoz.

---

# 64. API DESIGN

API versioning ishlat.

Masalan:

```text
/api/admin/v1/...
```

Admin API misollar:

```text
/api/admin/v1/auth

/api/admin/v1/staff
/api/admin/v1/roles
/api/admin/v1/permissions

/api/admin/v1/users

/api/admin/v1/content
/api/admin/v1/seasons
/api/admin/v1/episodes

/api/admin/v1/categories
/api/admin/v1/genres
/api/admin/v1/creators

/api/admin/v1/media

/api/admin/v1/advertisements
/api/admin/v1/premieres
/api/admin/v1/homepage

/api/admin/v1/notifications

/api/admin/v1/comments

/api/admin/v1/tariffs
/api/admin/v1/subscriptions

/api/admin/v1/donations

/api/admin/v1/analytics
/api/admin/v1/reports

/api/admin/v1/audit-logs

/api/admin/v1/settings
```

Lekin existing API convention bo‘lsa uni to‘satdan sindirma.

---

# 65. DTO QOIDASI

JPA Entity'ni controllerdan bevosita JSON qilib qaytarma.

DTO/request/response layer ishlat.

Circular references yuzaga kelmasin.

API payload faqat kerakli ma'lumotni qaytarsin.

---

# 66. N+1 MUAMMOSI

Content listda:

```text
content
creator
genres
categories
media
```

larni noto‘g‘ri lazy loading bilan yuzlab queryga aylantirma.

Pagination, projections, entity graph yoki mos query strategy ishlat.

Performance'ni profilingga asoslanib optimallashtir.

---

# 67. MEDIA SECURITY

Upload uchun:

* MIME type;
* extension;
* file size;
* allowed formats;
* generated safe filename;
* storage path

tekshir.

User yuborgan original filename'ni server path sifatida ishlatma.

Path traversal bo‘lmasin.

---

# 68. TIME

Database vaqtlarni imkon qadar UTC saqlasin.

Frontend Uzbekistonda:

```text
Asia/Tashkent
```

bo‘yicha ko‘rsatishi mumkin.

Date/time conversionni tartibli qil.

---

# 69. FRONTEND API LAYER

Component ichida har joyga:

```javascript
axios.get(...)
```

yozib ketma.

Centralized API client/service layer ishlat.

Existing loyiha patternini reuse qil.

Auth refresh/retry/error handling bitta joyda boshqarilsin.

---

# 70. SERVER STATE

Agar projectda TanStack Query yoki boshqa server-state library mavjud bo‘lsa reuse qil.

Bo‘lmasa yangi dependency qo‘shishdan oldin mavjud architecture'ni tekshir.

Har qanday yangi library:

* real muammoni hal qilishi;
* maintained;
* projectga mos

bo‘lishi kerak.

Dependency'larni sababsiz ko‘paytirma.

---

# 71. FRONTEND ROUTE GUARDS

Misol:

```text
/auth/login
/dashboard

/content
/content/new
/content/:id
/content/:id/edit

/creators
/categories
/genres

/ads
/premieres
/homepage

/notifications
/comments

/users

/tariffs
/subscriptions
/donations

/reports

/staff
/audit

/settings
```

Permission guard qo‘y.

Unauthorized bo‘lsa:

```text
403
```

page ko‘rsat.

---

# 72. TABLE COMPONENTS

Bir xil pagination/filter/table logikasini 20 marta copy-paste qilma.

Reusable components/hooks yarat:

```text
DataTable
Pagination
SearchInput
FilterPanel
StatusBadge
ConfirmDialog
MediaUploader
ImagePicker
VideoUploader
PermissionGuard
PageHeader
EmptyState
ErrorState
```

Lekin haddan tashqari abstraction ham qilma.

---

# 73. DASHBOARD PERFORMANCE

Dashboard ochilganda 20 ta alohida serial request qilishdan qoch.

Backendda zarur bo‘lsa optimized dashboard summary endpoint yarat.

Masalan:

```text
GET /api/admin/v1/dashboard/summary
```

---

# 74. AD / CONTENT TRACKING API

Mobil client keyinchalik quyidagi eventlarni yuborishi mumkin:

```text
AD_IMPRESSION
AD_CLICK

CONTENT_VIEW
CONTENT_PLAY
CONTENT_COMPLETE

NOTIFICATION_OPEN
```

Bu uchun scalable design qil.

Har bir impression uchun Admin dashboard'da og‘ir query ishlatmaslikni hisobga ol.

---

# 75. EXISTING CASTING MODULE

Repository ichidagi eski casting kodini top.

Aniq yoz:

* qaysi entity;
* qaysi controller;
* qaysi frontend pages;
* qanday route;
* qanday database table

mavjud.

Ularni `roadmap.md -> Existing Project Audit` ichida qayd qil.

Agar casting module ishlayotgan bo‘lsa:

**o‘chirib yuborma.**

Yangi UZCASTING platformasiga integrate qil.

---

# 76. MOBILE — HOZIR SCOPE EMAS

Hozir quyidagilarni yozma:

* React Native mobile screens;
* USER login mobile UI;
* movie player mobile screen;
* watchlist mobile UI;
* subscription mobile UI;
* donation mobile UI.

Lekin backend/content schema kelajak mobile uchun mos bo‘lishi kerak.

Kerak bo‘lsa:

```text
/backend/FUTURE_MOBILE_API.md
```

faylida future API contract qayd qil.

Mobil kod yozma.

---

# 77. WATCH LATER / SAVED

Kelajakda user kino yoki serialni:

```text
Save / Watch later
```

qila oladi.

Hozir Admin panelda bunga page kerak emas.

Lekin content ID architecture stable bo‘lsin.

Bu feature'ni roadmapda Future Mobile Scope sifatida yoz.

---

# 78. ACCEPTANCE CRITERIA — AUTH/RBAC

Quyidagilar test qilinsin:

1. USER admin panelga kira olmaydi.
2. WORKER staff yarata olmaydi.
3. ADMIN faqat Worker yarata oladi.
4. SUPER_ADMIN Admin va Worker yarata oladi.
5. SUPER_ADMIN HyperAdmin yarata olmaydi.
6. Permission yo‘q Worker protected endpointga kira olmaydi.
7. Frontend menu ham permissionga qarab yashiriladi.
8. Backend baribir 403 qaytaradi.

---

# 79. ACCEPTANCE CRITERIA — CONTENT

Admin/Worker tegishli permission bilan:

* qisqa film;
* film;
* mini serial;
* faslli serial;
* podcast

yarata olishi kerak.

Content:

* rasm;
* gallery;
* trailer;
* video;
* category;
* genres;
* creators;
* premiere date;
* access policy

bilan saqlanishi kerak.

---

# 80. ACCEPTANCE CRITERIA — SERIAL

Quyidagi structure muammosiz yaratilishi kerak:

```text
Serial A

Season 1
 Episode 1
   video part 1
   video part 2

 Episode 2
   video

Season 2
 Episode 1
   video
```

Shuningdek:

```text
Mini Serial B

Episode 1
Episode 2
Episode 3
```

season yaratmasdan ishlashi kerak.

---

# 81. ACCEPTANCE CRITERIA — AD

Admin:

1. rasmli banner yaratadi;
2. buttonli/buttonless tanlaydi;
3. internal yoki external link beradi;
4. carousel order tanlaydi;
5. start/end date beradi.

Reportda:

```text
views/impressions
clicks
CTR
```

ko‘rinadi.

---

# 82. ACCEPTANCE CRITERIA — CREATOR

Admin:

* yangi creator yaratadi;
* existing creator qidiradi;
* bir contentga bir nechta creator biriktiradi;
* har creator uchun content-specific rol belgilaydi.

---

# 83. ACCEPTANCE CRITERIA — PREMIUM

Admin:

* tariflarni ko‘radi;
* narxni o‘zgartiradi;
* userni telefon orqali topadi;
* premium beradi;
* premiumni bekor qiladi.

Barcha action auditga tushadi.

---

# 84. ACCEPTANCE CRITERIA — ROADMAP

Development davomida:

```text
roadmap.md
BACKEND_ROADMAP.md
FRONTEND_ROADMAP.md
```

doim aktual bo‘lishi shart.

Claude context tugab qolsa ham keyingi sessiya shu fayllardan loyiha holatini tiklay olishi kerak.

Bu juda muhim.

---

# 85. TESTING

Backendda kamida critical business logic test qil:

* RBAC;
* staff creation hierarchy;
* content creation;
* episode relationship;
* tariffs;
* premium grant/revoke;
* ad analytics;
* validation.

Agar projectda integration test infrastructure mavjud bo‘lsa reuse qil.

PostgreSQL-specific behavior bo‘lsa real PostgreSQL/Testcontainers mos yechim ishlatish mumkin.

---

# 86. FRONTEND TEST

Critical flows:

* login;
* forbidden route;
* create content;
* edit content;
* create creator;
* add episode;
* create advertisement;
* staff permission.

Mavjud testing stackni reuse qil.

---

# 87. HAR BIR BOSQICHDA BUILD

Backend o‘zgarishidan keyin tegishli:

```bash
test
build
```

ishga tushir.

Frontend uchun:

```bash
lint
typecheck
test
build
```

projectda mavjud scriptlarga qarab ishlat.

Command nomini taxmin qilma.

Avval `package.json`, `pom.xml` yoki `build.gradle`ni tekshir.

---

# 88. BASELINE TEST

Kodni o‘zgartirishdan AVVAL mavjud projectni build/test qilib ko‘r.

Agar avvaldan xato bo‘lsa:

`roadmap.md` ichida:

```text
Pre-existing issue
```

sifatida yoz.

O‘zing yaratmagan eski xatoni yangi development xatosi deb ko‘rsatma.

---

# 89. NO DUPLICATE CODE

Yangi entity/service yaratishdan oldin repository bo‘ylab qidir:

* `User`;
* `Role`;
* `Media`;
* `Content`;
* `Movie`;
* `Series`;
* `Subscription`;
* `Payment`;
* `Notification`;
* `Casting`.

Mavjud modelni kengaytirish mumkin bo‘lsa, duplicate yaratma.

---

# 90. REFACTOR POLICY

Mavjud kod ishlayotgan bo‘lsa faqat yangi arxitektura chiroyli ko‘rinishi uchun butun projectni rewrite qilma.

Refactor:

* konkret muammo bo‘lsa;
* test bilan;
* incremental

qilinsin.

---

# 91. DATABASE MIGRATION POLICY

Hech qachon:

```text
DROP DATABASE
DROP ALL TABLES
delete production data
```

kabi destructive ish qilma.

Development database ekanligi aniq bo‘lmasa data preservation asosiy qoida.

---

# 92. SECRET SECURITY

Hech qachon repositoryga:

```text
JWT_SECRET
DB_PASSWORD
FCM_KEY
S3_KEY
PAYMENT_KEY
```

hardcode qilma.

Environment/config orqali ol.

`.env.example` yoki existing config example yangilanishi mumkin, ammo real secret yozilmasin.

---

# 93. LOGGING

Production logging:

* meaningful;
* structured;
* sensitive data free

bo‘lsin.

Password, JWT, refresh token, payment credential logga chiqmasin.

---

# 94. ERROR HANDLING

Global exception handler ishlat.

Frontendga raw stacktrace yuborma.

Masalan:

```text
404 CONTENT_NOT_FOUND
403 ACCESS_DENIED
409 DUPLICATE_PHONE
422 VALIDATION_ERROR
```

kabi tushunarli business errorlar bo‘lsin.

Existing convention bo‘lsa reuse qil.

---

# 95. PAGINATION

Users, content, comments, creators, ads, audit logs va transactions uchun pagination majburiy.

100 000 ta recordni bitta response bilan qaytarma.

---

# 96. SEARCH DEBOUNCE

Frontend search har bir keyboard bosilishida serverga nazoratsiz request yubormasin.

Debounce yoki explicit search pattern ishlat.

---

# 97. ACCESSIBILITY

Admin panelda:

* button labels;
* keyboard usage;
* modal focus;
* form labels;
* contrast

ga e'tibor ber.

---

# 98. RESPONSIVE

Admin website desktop-first.

Lekin:

* laptop;
* tablet;
* kichik ekranlarda

butunlay buzilib ketmasin.

Mobile USER app alohida bo‘ladi, admin panelni mobil appga aylantirish shart emas.

---

# 99. IMPLEMENTATION PHASES

Developmentni quyidagi tartibda olib bor.

## PHASE 0 — AUDIT

* inspect repository;
* identify backend/frontend;
* baseline build;
* database;
* existing auth;
* existing casting;
* existing media;
* existing UI;
* create/update roadmap files.

**Kod yozishdan oldin bajar.**

---

## PHASE 1 — CORE ARCHITECTURE

* RBAC;
* permissions;
* auth corrections;
* audit log foundation;
* shared backend errors;
* frontend layout;
* route guards;
* API client.

---

## PHASE 2 — STAFF MANAGEMENT

* HyperAdmin;
* SuperAdmin;
* Admin;
* Worker;
* Worker permissions.

---

## PHASE 3 — CMS FOUNDATION

* Category;
* Genre;
* Creator;
* Media Library.

---

## PHASE 4 — CONTENT

* Content;
* Content Type;
* Single;
* Episodic;
* Seasonal;
* Season;
* Episode;
* Video Parts;
* Gallery;
* Creators;
* Access Policy.

---

## PHASE 5 — HOMEPAGE

* Homepage configuration;
* Ads;
* carousel;
* Premieres;
* Featured creators;
* Featured content.

---

## PHASE 6 — ENGAGEMENT

* Comments moderation;
* Notifications;
* Casting notifications.

---

## PHASE 7 — USERS & MONETIZATION

* User admin management;
* tariffs;
* subscription;
* premium grant/revoke;
* single content price;
* Stars;
* UzCasting Coin;
* donation reports.

---

## PHASE 8 — ANALYTICS

* ad impressions;
* ad clicks;
* content views;
* dashboard;
* reports;
* aggregation.

---

## PHASE 9 — HARDENING

* permission tests;
* performance;
* security;
* indexes;
* frontend UX;
* error states;
* loading states;
* audit;
* build;
* regression tests.

---

# 100. CLAUDE ISH USLUBI

Mendan har bir kichik qaror uchun savol so‘rab developmentni to‘xtatib qo‘yma.

Avval existing code va documentationdan javob top.

Agar ikki variant ham to‘g‘ri bo‘lsa:

1. production uchun xavfsizroq;
2. maintenance osonroq;
3. existing projectga mosroq

variantni tanla.

Qarorni:

```text
roadmap.md -> Important Decisions
```

ichiga yoz.

Faqat haqiqiy blocker:

* credential;
* production access;
* irreversible business decision

bo‘lsa savol ber.

---

# 101. CONTEXT LIMIT QOIDASI

Agar bir sessiyada hamma ishni tugatish imkoni bo‘lmasa:

1. kodni stabil holatda qoldir;
2. build holatini tekshir;
3. `roadmap.md`ni update qil;
4. `BACKEND_ROADMAP.md`ni update qil;
5. `FRONTEND_ROADMAP.md`ni update qil;
6. aynan keyingi bajariladigan tasklarni yoz.

Masalan:

```md
## Next Exact Steps

1. Implement ContentMedia migration.
2. Add ContentMediaRepository.
3. Add media section to ContentEditor.
4. Integrate POST /content/{id}/media.
5. Add integration test.
```

“Continue backend” kabi umumiy gap yozma.

---

# 102. HAR BIR TASK TUGAGANDA

Claude quyidagilarni tekshirsin:

```text
[ ] code completed
[ ] validation completed
[ ] permission checked
[ ] API integrated
[ ] frontend state handled
[ ] loading handled
[ ] errors handled
[ ] test added/updated
[ ] build passed
[ ] roadmap updated
```

Shundan keyingina taskni `[x] DONE` qil.

---

# 103. CODE QUALITY

Maqsad:

**ko‘p kod yozish emas, optimal va maintainable kod yozish.**

Quyidagilardan qoch:

* huge controllers;
* huge services;
* god components;
* duplicate DTO;
* duplicate API calls;
* magic strings;
* scattered authorization;
* scattered currency logic;
* scattered link logic;
* N+1 queries;
* uncontrolled useEffect;
* prop drilling where inappropriate;
* 1000+ line React page;
* unnecessary abstraction;
* unnecessary dependency.

---

# 104. SHARED BUSINESS COMPONENTS

Imkon qadar markazlashtir:

```text
AccessPolicy
InternalLink
MediaAsset
Money
Audit
Permission
ContentStatus
Publication
Pagination
```

Lekin “clean architecture” nomi bilan 15 layerlik keraksiz murakkablik yaratma.

---

# 105. ARCHITECTURE.md

Backend `ARCHITECTURE.md` ichida yoz:

```text
Current architecture
Packages/modules
Authentication
Authorization
Database
Media storage
Content model
Monetization model
Analytics model
Audit model
Important dependencies
```

Frontend `ARCHITECTURE.md`:

```text
Routing
Layouts
Auth
Permissions
API layer
Server state
Forms
Tables
Media upload
Design system
Shared components
```

Documentation kod bilan birga yangilansin.

---

# 106. API DOCUMENTATION

Swagger/OpenAPI mavjud bo‘lsa barcha yangi endpointlarni document qil.

Request/response modellari tushunarli bo‘lsin.

Frontend developer endpointni backend kodini o‘qimasdan ishlata olishi kerak.

---

# 107. FINAL PRODUCT HOLATI

Natijada web platformaga:

```text
HYPER_ADMIN
SUPER_ADMIN
ADMIN
WORKER
```

kiradi.

Role/permissioniga qarab:

* staff;
* content;
* kino;
* serial;
* fasl;
* epizod;
* creator;
* category;
* genre;
* media;
* advertisement;
* premiere;
* homepage;
* notification;
* comments;
* user;
* subscriptions;
* tariffs;
* donation;
* analytics;
* reports;
* casting;
* audit

modullarini boshqaradi.

USER mobil app orqali foydalanadi, lekin bu development bosqichida USER mobil interfeysi yaratilmaydi.

---

# 108. HOZIR BOSHLASH TARTIBI

Endi darhol developmentni quyidagi tartibda boshla:

### 1.

Repository rootni inspect qil.

### 2.

Backend va frontend real pathlarini aniqlagin.

### 3.

Existing Java va React stack/versionlarni aniqlagin.

### 4.

Git statusni tekshir. Userning mavjud o‘zgarishlarini o‘chirib yuborma.

### 5.

Backendni build/test qil.

### 6.

Frontendni build/lint/test qil.

### 7.

Existing database/model/auth/casting/media kodini audit qil.

### 8.

`roadmap.md` yarat yoki yangila.

### 9.

Backend roadmap va architecture MD yarat.

### 10.

Frontend roadmap va architecture MD yarat.

### 11.

Audit natijasiga asoslangan real implementation plan yoz.

### 12.

Shundan keyin PHASE 1 developmentni boshlagin.

Faqat reja yozib to‘xtab qolma.

Audit va roadmap tugagach mavjud project ustida real kod implementationni boshlagin.

---

# 109. ENG MUHIM YAKUNIY QOIDALAR

**DO NOT:**

* projectni noldan rewrite qilma;
* existing casting modulini yo‘qotma;
* mavjud database'ni buzma;
* duplicate User/Role/Auth yaratma;
* fake analytics yaratma;
* fake payment success qilma;
* secret hardcode qilma;
* authorizationni faqat frontendga qo‘yma;
* barcha tariflarni hardcode qilma;
* movie/series modelini faqat bitta `videoUrl` bilan cheklama;
* USER mobile appni hozir yozma;
* roadmapni eskirtirib qo‘yma.

**DO:**

* audit first;
* reuse existing code;
* incremental implementation;
* backend security first;
* clean RBAC;
* scalable content model;
* reusable media architecture;
* proper DB migrations;
* role-aware frontend;
* reports;
* audit logs;
* tests;
* production-quality code;
* doim roadmapni yangila.

---

# 110. BIRINCHI JAVOBING

Ish boshlanganda menga faqat umumiy reja qaytarib to‘xtab qolma.

Repositoryni real tekshir.

Keyin qisqa qilib:

```text
1. Existing architecture found
2. Existing modules found
3. Main problems found
4. Files created/updated
5. Current implementation phase
6. First code changes made
7. Build/test status
```

ko‘rsat.

So‘ng implementationni davom ettir.

**START NOW.**
