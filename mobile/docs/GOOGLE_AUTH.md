# Google Sign-In — настройка OAuth-клиентов

> Что вписывать в Google Cloud Console для проекта UzCasting и как это работает в коде.
> Статус: **клиентская часть написана**, ждёт client ID и эндпоинт на бэкенде.

Последнее обновление: 13.08.2026

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

⚠️ **Отпечаток выше годится только для локальных сборок.** Если dev build собирается
через EAS в облаке, EAS создаёт собственный keystore — там будет другой SHA-1,
и его нужно добавить в тот же Android-клиент дополнительно (`eas credentials`).

**Релизный:** отпечаток ключа, которым подписывается сборка для Play Store.
При загрузке в Play с включённым Play App Signing Google перевыпускает ключ —
итоговый SHA-1 берётся из консоли Play (Setup → App integrity), и его тоже
нужно добавить в Android-клиент, иначе вход в продакшене не заработает.

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
app.google.client-ids=497193534365-urqt7p6tufge2qpqhdns5qv8nvshjsqe.apps.googleusercontent.com
```

Клиентов будет несколько — перечисляются через запятую, без пробелов:

```properties
app.google.client-ids=<web>.apps.googleusercontent.com,<android>.apps.googleusercontent.com,<ios>.apps.googleusercontent.com
```

Альтернатива без правки файла — переменная окружения `APP_GOOGLE_CLIENT_IDS` с тем же значением.

**Все три client ID должны быть в списке.** Android-приложение получает токен со своим
`aud`, и если его там нет — сервер отклонит вход с «Google token yaroqsiz».

---

## 7. Что блокирует финальную настройку

1. **Android client ID** — создать клиент в консоли с package `uz.uzcasting.app` и SHA-1 выше, ID вписать в `.env` и в `app.google.client-ids` на сервере.
2. **Test users.** Пока проект в статусе `Testing` (Google Auth Platform → Audience), войти могут только перечисленные там аккаунты. Симптом при пропуске — «доступ запрещён» без внятной причины.
3. **`app.google.client-ids` на сервере** — иначе `503`.
4. **Dev build.** В Expo Go Google-вход не работает: Google не принимает redirect на схему Expo Go. Нужен свой APK. Совпадает с другой причиной собрать dev build — Expo Go отстаёт по версии SDK, и для платёжных SDK с защищённым видео его всё равно не хватит.
5. **SHA-1 релизного ключа** — когда определимся, кто подписывает сборку (EAS, локальный keystore или Play App Signing).

### Redirect URI веб-клиента не нужен

Изначально закладывали `https://uzcasting.site/api/v1/auth/google/callback`. По факту флоу вышел
другой: приложение получает ID-токен само, а бэкенд его только проверяет. Браузерного
редиректа на сервер нет, значит и **Authorized redirect URIs у веб-клиента можно оставить пустым**.
Заполнять надо только origins.

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

---

## 8. Наш флоу авторизации

Решено с заказчиком: вход по номеру телефона **или** через Google.

```
Телефон:  +998 → OTP → [если новый] имя и язык → Home
Google:   OAuth → [если новый] телефон + OTP → Home
```

Телефон подтверждаем в обеих ветках — он ключ пользователя по ТЗ, и к нему привязаны узбекские платёжные системы. Подробнее и с разбором экрана Yangi.TV — в [STRUCTURE.md §4](./STRUCTURE.md).
