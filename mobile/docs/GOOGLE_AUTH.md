# Google Sign-In — настройка OAuth-клиентов

> Что вписывать в Google Cloud Console для проекта UzCasting и как это работает в коде.
> Статус: клиент и бэкенд написаны, client ID заведены, dev build собирается.
> Упирается в сервер: нужен деплой бэкенда + `app.google.client-ids` в конфиге (см. §7).

Последнее обновление: 14.08.2026

---

## 1. Нужно три клиента, а не один

Частая ошибка — создать один клиент типа **Web application** и пытаться жить с ним.
Для мобильного приложения нужны три разных:

| Тип клиента | Зачем нужен | Какие поля заполняются |
|---|---|---|
| **Android** | сам вход с телефона | package name + SHA-1. Origins/redirect **нет** |
| **iOS** | 2-й этап по ТЗ | bundle ID. Origins/redirect **нет** |
| **Web** | верификация ID-токена на бэкенде, вход на сайте | JavaScript origins + redirect URIs |

### ⚠️ Кастомную схему в Web-клиент вписывать нельзя

`uzcasting://` в **Authorized redirect URIs** веб-клиента Google не примет — выдаст ошибку валидации. Для нативных приложений ровно поэтому и существуют типы Android и iOS: там redirect строится автоматически из package name / bundle ID.

---

## 2. Значения проекта

Источник — `mobile/app.json`:

| Что | Значение |
|---|---|
| Android package | `uz.uzcasting.app` |
| iOS bundle ID | `uz.uzcasting.app` |
| URL-схема | `uzcasting` |
| Сайт и бэкенд | `https://uzcasting.site` |

---

## 3. Web application

**Authorized JavaScript origins**

```
https://uzcasting.site
http://localhost:3000     # дев сайта (react-scripts)
http://localhost:8081     # дев мобилки в браузере (expo start --web)
```

**Authorized redirect URIs**

```
https://uzcasting.site/api/v1/auth/google/callback
```

⚠️ Этот путь — **предположение по конвенции, а не факт**. Реального эндпоинта в бэкенде нет: сейчас там только админский `/api/v1/auth/login` (см. [API.md](./API.md)). Точный путь должен назвать бэкендер, иначе клиент придётся пересоздавать.

Возможен и вариант, где redirect URI веб-клиенту вообще не нужен: если мобильное приложение само получает ID-токен от Google, а бэкенд его только проверяет, то заполняются одни origins. Какой сценарий выбираем — решается вместе с бэкендером.

---

## 4. Android

```
Package name: uz.uzcasting.app
SHA-1:        7C:DF:FA:38:8C:A2:00:A1:92:48:B9:25:6C:88:2A:C5:60:C3:95:FC
```

Отпечатков нужно **два** — отладочный и релизный, это разные сертификаты.

**Отладочный — уже есть.** Keystore создан 13.08.2026 стандартными параметрами Android
(`~/.android/debug.keystore`, alias `androiddebugkey`, пароли `android`). Gradle подхватит
его при первой локальной сборке и не будет создавать свой.

Перечитать отпечаток:

```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android
```

**Этот же отпечаток работает и в облачных сборках EAS.** По умолчанию EAS генерирует
собственный keystore — это дало бы второй SHA-1, который пришлось бы дописывать
в Android-клиент. Чтобы этого избежать, keystore скопирован в `mobile/credentials/`
и подключён через `credentials.json`, а в `eas.json` у профилей `development`
и `preview` стоит `"credentialsSource": "local"`.

`credentials.json` и папка `credentials/` — в `.gitignore`: там пароли от ключа.
EAS CLI читает их локально и передаёт keystore напрямую в сборку, не через архив проекта,
поэтому гитигнор им не мешает.

Проверить, что EAS взял именно наш ключ, можно по строке в логе запуска:
`✔ Using local Android credentials (credentials.json)`.

**Релизный:** отпечаток ключа, которым подписывается сборка для Play Store.
При загрузке в Play с включённым Play App Signing Google перевыпускает ключ —
итоговый SHA-1 берётся из консоли Play (Setup → App integrity), и его тоже
нужно добавить в Android-клиент, иначе вход в продакшене не заработает.

---

### Redirect на Android — вторая схема в app.json

`expo-auth-session` строит для нативной платформы redirect вида
`${applicationId}:/oauthredirect`, то есть **`uz.uzcasting.app:/oauthredirect`**.

Это не наша основная схема `uzcasting`, поэтому в `app.json` зарегистрированы обе:

```json
"scheme": ["uzcasting", "uz.uzcasting.app"]
```

Без второй схемы браузер после согласия в Google не смог бы вернуться в приложение —
вход завис бы на пустой вкладке. В самой консоли Google для Android-клиента redirect
указывать не надо: он выводится из package name автоматически.

---

## 5. iOS

```
Bundle ID: uz.uzcasting.app
```

Понадобится на 2-м этапе по ТЗ (iOS + English).

Отдельно: если в приложении есть вход через Google, Apple требует ещё и Sign in with Apple — иначе приложение не пройдёт ревью в App Store. Учесть при планировании 2-го этапа.

---

## 6. Настройка бэкенда

Эндпоинт `POST /api/v1/auth/google` проверяет ID-токен по списку разрешённых client ID.
Без этого списка он осознанно отвечает `503`, а не пропускает любой токен.

`application.properties` лежит только на сервере (в git не попадает), поэтому строку
добавляет тот, кто деплоит:

```properties
app.google.client-ids=497193534365-urqt7p6tufge2qpqhdns5qv8nvshjsqe.apps.googleusercontent.com,497193534365-03mh4geit6f6n13enrf2bqcs8dhcp1cb.apps.googleusercontent.com
```

Первый — Web, второй — Android. Перечисляются через запятую, без пробелов.
iOS-клиент добавится сюда же на 2-м этапе.

Альтернатива без правки файла — переменная окружения `APP_GOOGLE_CLIENT_IDS` с тем же значением.

**Все три client ID должны быть в списке.** Android-приложение получает токен со своим
`aud`, и если его там нет — сервер отклонит вход с «Google token yaroqsiz».

---

## 7. Что блокирует финальную настройку

| # | Что | Статус |
|---|---|---|
| 1 | **Android client ID** — клиент с package `uz.uzcasting.app` и SHA-1 выше | ✅ создан, ID в `.env` и `eas.json` |
| 2 | **Test users** в Google Auth Platform → Audience. Пока проект в статусе `Testing`, войти могут только перечисленные аккаунты. Симптом при пропуске — «доступ запрещён» без внятной причины | ✅ добавлен `lazizkhamrakulov@gmail.com` (14.08.2026) |
| 3 | **Dev build.** В Expo Go Google-вход не работает: Google не принимает redirect на схему Expo Go | ✅ EAS-профиль `development` настроен |
| 4 | **Деплой бэкенда.** На `uzcasting.site` работает сборка без Google-логина | ❌ **блокирует вход** |
| 5 | **`app.google.client-ids` на сервере** — иначе эндпоинт отвечает `503` | ❌ **блокирует вход**, делает тот, кто деплоит (см. §6) |
| 6 | **SHA-1 релизного ключа** — когда решим, кто подписывает продакшен (наш keystore или Play App Signing) | ⏳ к этапу публикации |

Осталось 4 и 5, именно в этом порядке: сначала выкатить код, потом дописать свойство.
Флоу дойдёт до выбора аккаунта Google, приложение получит `id_token`, а обмен упадёт.

### Как проверить деплой одной командой

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://uzcasting.site/api/v1/auth/google \
  -H "Content-Type: application/json" -d '{"idToken":"probe"}'
```

| Код | Что значит |
|---|---|
| `405` | эндпоинта нет — **старая сборка на сервере** |
| `503` | код выкачен, но `app.google.client-ids` не задан |
| `401` | всё настроено, токен-пустышка честно отклонён — можно тестировать с телефона |

`405`, а не `404`, потому что на неизвестный путь отвечает дефолтный обработчик статики
Spring Boot: он смотрит `/**`, но только на GET. Проверено 14.08.2026 — сравнение
с заведомо несуществующим путём дало тот же `405`, а живой `/api/v1/auth/login` — `401`.

### Redirect URI веб-клиента: для телефона не нужен, для браузера обязателен

Изначально закладывали `https://uzcasting.site/api/v1/auth/google/callback`. По факту флоу вышел
другой: приложение получает ID-токен само, а бэкенд его только проверяет. Браузерного
редиректа на сервер нет — **для мобильного приложения redirect URI не нужен вообще**,
там всё выводится из package name (см. §4).

Но если гонять вход **через веб-превью** (`expo start --web`), Google требует
зарегистрировать адрес страницы: `expo-auth-session` в браузере подставляет
в `redirect_uri` текущий origin. Без записи — `Ошибка 400: redirect_uri_mismatch`.

В **веб-клиенте** нужно добавить в оба поля:

```
Authorized JavaScript origins:  http://localhost:8082
Authorized redirect URIs:       http://localhost:8082
```

Без хвостового слэша и без пути — ровно origin. Порт тот, на котором поднят
веб-превью (`--port 8082`); при запуске без флага это `8081`, тогда добавляй его.

Как узнать точное значение, если Google опять ругается: в URL страницы ошибки
лежит параметр `authError` — это base64, внутри честно написан отправленный `redirect_uri`.

```bash
node -e "const u=process.argv[1];const s=new URL(u).searchParams.get('authError');const b=Buffer.from(s.replace(/-/g,'+').replace(/_/g,'/'),'base64').toString('utf8');console.log(b.match(/redirect_uri.{0,4}(https?:\/\/[^\s\x00-\x1f]+)/)?.[1])" "<вставить URL ошибки>"
```

---

## 7. Что уже написано

| Файл | Что делает |
|---|---|
| `src/features/auth/config.ts` | читает client ID из `.env`, флаг `isGoogleConfigured` |
| `src/features/auth/useGoogleSignIn.ts` | OAuth-запрос, возвращает `idToken` |
| `src/features/auth/GoogleSignInButton.tsx` | кнопка + состояния: pending, cancelled, error, «не настроено» |
| `src/features/auth/store.ts` | Zustand + expo-secure-store для токена |
| `app/(auth)/sign-in.tsx` | экран входа: телефон +998, Google, согласие |
| `app/(auth)/otp.tsx` | ввод SMS-кода (заглушка) |

### Выбрана реализация на `expo-auth-session`

Браузерный OAuth, без нативных модулей. Альтернатива — нативный `@react-native-google-signin/google-signin`: красивее (системный шит выбора аккаунта), но тянет native-код, Firebase и `google-services.json`.

Начали с `expo-auth-session`, потому что она не требует ничего, кроме client ID, и её можно заменить на нативную позже, не переписывая экраны — вся работа спрятана за `GoogleSignInButton`.

### Ключи

Кладутся в `.env` (шаблон — `.env.example`), в git не попадают:

```
EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID=
EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID=
EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=
```

Пока они пустые, кнопка показывается неактивной с подписью «Google kiritish hozircha sozlanmagan». Приложение при этом работает — вход по телефону доступен.

### Две грабли, на которые уже наступили

**Хук бросает исключение без client ID.** `Google.useIdTokenAuthRequest` падает с `Client Id property 'webClientId' must be defined`, если ключа нет. Условно вызывать хуки нельзя, поэтому вся работа с ним вынесена в `GoogleSignInButton`, и он монтируется только когда ключи заданы. Иначе рисуется заглушка.

**Импорт строчными буквами.** Правильно `expo-auth-session/providers/google`, а не `.../Google`. С заглавной резолвится на Windows (файловая система нечувствительна к регистру) и типы проходят, но Metro падает с `Unable to resolve module`.

**`.env` не доезжает до облачной сборки.** EAS заливает только то, что лежит под git,
а `.env` в гитигноре — значит в APK все `EXPO_PUBLIC_*` оказались бы `undefined`,
и кнопка Google собралась бы в состоянии «не настроено». Ошибки при сборке при этом нет,
видно только на телефоне. Поэтому client ID продублированы в `eas.json` → `env`
у профилей `development` и `preview`. Секретов там нет: client ID и так лежит внутри APK.

Проверка — строка в логе запуска сборки:
`Environment variables loaded from the "development" build profile "env" configuration: ...`.
Если её нет, переменные не доехали.

**В `eas.json` нельзя писать комментарии.** Ключи `"//"` проходят как валидный JSON,
но схема EAS их отклоняет: `"build.development.//" is not allowed`. Пояснения — только сюда.

---

## 8. Наш флоу авторизации

Решено с заказчиком: вход по номеру телефона **или** через Google.

```
Телефон:  +998 → OTP → [если новый] имя и язык → Home
Google:   OAuth → Home
```

**Телефон после Google не спрашиваем** (решение от 14.08.2026). Сначала было наоборот,
но это оказалось нашей выдумкой, а не требованием: ТЗ разрешает создать аккаунт
«telefon/**email** orqali», а соцвход помечен `Google / Apple / Telegram optional`.
Обязательного номера там нет нигде.

Бэкенд по-прежнему возвращает `phone_required` — теперь это подсказка, что номера
у аккаунта нет, а не запрет на вход. Номер попросим точечно, там где он действительно
нужен: выплаты креаторам и оплата через системы, завязанные на номер.

Экран `(auth)/phone-link` остался, но открывается по требованию и имеет «Keyinroq».

Подробнее и с разбором экрана Yangi.TV — в [STRUCTURE.md §4](./STRUCTURE.md).
