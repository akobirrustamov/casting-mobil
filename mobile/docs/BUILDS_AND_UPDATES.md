# Сборки APK и обновления по воздуху

> Кто и что должен дать, чтобы APK собиралась, и что можно менять
> в готовой сборке, не пересобирая её.

Последнее обновление: 02.09.2026

---

## 1. Чего не хватает, чтобы сборку запускал ассистент

Три вещи, по убыванию важности.

### 1.1. Токен доступа Expo ✅ сделано 02.09.2026

EAS собирает под аккаунтом `bukakish` (`app.json` → `owner`), проект
`0ce6ce6a-5243-426b-9a5f-adff85882fec`. Без авторизации `eas build` просто
не начнётся.

Интерактивный `eas login` для ассистента не годится: он спрашивает пароль
и код из почты. Нужен **robot access token**:

1. https://expo.dev/accounts/bukakish/settings/access-tokens → **Create token**
2. Скопировать значение (показывается один раз)
3. Положить в `mobile/.env.local` строкой `EXPO_TOKEN=...`

⚠️ **В `.env` класть нельзя** — этот файл читает Expo CLI и подставляет
переменные в бандл; токен от аккаунта не должен уезжать в APK. `.env.local`
закрыт правилом `.env*.local` в `mobile/.gitignore`.

⚠️ **EAS CLI читает токен из окружения, а не из файла.** Файл — только
хранилище, перед командой значение надо экспортировать:

```bash
export EXPO_TOKEN=$(grep '^EXPO_TOKEN=' .env.local | cut -d= -f2)
npx eas whoami          # → bukakish (authenticated using EXPO_TOKEN)
```

Токен даёт право собирать и публиковать обновления от вашего имени. Отозвать
можно на той же странице.

### 1.2. Ключ подписи — отдан EAS (02.09.2026)

Старый debug-keystore потерян (разбор — [GOOGLE_AUTH.md §4](./GOOGLE_AUTH.md)),
а профили стояли на `"credentialsSource": "local"` и искали `credentials.json`,
которого нет: сборка не стартовала вовсе.

`credentialsSource` убран — ключ теперь генерирует и хранит EAS. Он не
теряется вместе с машиной, и это же снимает вопрос «а где ключ у второго
разработчика».

⚠️ **Новый ключ — новый SHA-1, и его надо вписать в Android-клиент Google**,
иначе вход через Google в собранной APK не заработает. Отпечаток:

```bash
npx eas credentials -p android          # Keystore → SHA-1 Fingerprint
```

### 1.3. EAS CLI — ставится ГЛОБАЛЬНО, не в проект

```bash
npm i -g eas-cli
```

⚠️ **В `devDependencies` его класть нельзя**, хотя соблазн есть. Первая же
попытка так сделать (02.09.2026) уронила сборку на фазе «Install
dependencies»:

```
npm ci can only install packages when your package.json and
package-lock.json are in sync.
Missing: typescript@5.9.3 from lock file
```

У `eas-cli` своя ветка зависимостей с другой версией TypeScript. Локально
`npm install` её разложил, а в `package-lock.json` запись не попала — и
`npm ci` на сервере, который в отличие от `npm install` не имеет права
чинить расхождения, честно отказался. Плюс это лишние мегабайты и нативная
сборка `dtrace-provider` на КАЖДОМ билде: EAS ставит и devDependencies.

Сборка:

```bash
export EXPO_TOKEN=$(grep '^EXPO_TOKEN=' .env.local | cut -d= -f2)
eas build --platform android --profile preview --non-interactive
```

⚠️ Сборка расходует квоту аккаунта: на бесплатном тарифе очередь общая и
может занять десятки минут. Ещё один довод не гонять сборку ради правки
текста — см. §2.

### 1.4. Как читать логи упавшей сборки

Веб-страница сборки требует логина, поэтому логи достаются через API. Файл
приходит сжатым **brotli** — `gzip` его не откроет:

```bash
URL=$(curl -s https://api.expo.dev/graphql -H "Authorization: Bearer $EXPO_TOKEN"   -H "Content-Type: application/json"   -d '{"query":"query{builds{byId(buildId:\"<BUILD_ID>\"){logFiles}}}"}'   | python -c "import sys,json;print(json.load(sys.stdin)['data']['builds']['byId']['logFiles'][0])")
curl -s "$URL" -o build.log
node -e "console.log(require('zlib').brotliDecompressSync(require('fs').readFileSync('build.log')).toString())"
```

Каждая строка — JSON с полями `phase` и `msg`.

---

## 2. Обновления без пересборки — EAS Update

**Раньше в проекте этого не было.** `expo-updates` добавлен 02.09.2026,
вместе с настройкой:

| Где | Что |
|---|---|
| `app.json` → `updates.url` | `https://u.expo.dev/<projectId>` |
| `app.json` → `runtimeVersion` | `{ "policy": "fingerprint" }` |
| `eas.json` | у каждого профиля свой `channel` |

Публикация обновления:

```bash
npx eas update --channel preview --message "что поменялось"
```

Телефон подхватит его при следующем запуске приложения.

### Что уезжает по воздуху, а что нет

Это главное, что нужно понимать про OTA, иначе ожидания разойдутся с
реальностью в самый неудобный момент.

| Меняем | Хватит `eas update` | Нужна новая сборка |
|---|---|---|
| экраны, вёрстка, тексты, переводы | ✅ | |
| логика запросов, обработка ответов | ✅ | |
| картинки и шрифты в `assets/` | ✅ | |
| **адрес бэкенда `EXPO_PUBLIC_API_URL`** | ✅ (значение вшивается в бандл обновления) | |
| новая нативная библиотека (`expo install ...`) | | ❌ |
| правки `app.json`: иконка, разрешения, схема, `usesCleartextTraffic` | | ❌ |
| версия Expo SDK | | ❌ |

### ⚠️ Почему политика `fingerprint`, а не `appVersion`

`fingerprint` считает отпечаток нативной части (список модулей, конфиг) и
кладёт его в `runtimeVersion`. Если нативное изменилось, отпечаток другой —
и старая APK **просто не увидит** это обновление.

Альтернатива (`appVersion`) сравнивает только номер версии, и о нативных
изменениях не знает: обновление уехало бы на телефон, где нужного модуля нет,
и приложение падало бы при запуске. Причём падало бы у заказчика, а не у нас.

### ⚠️ Первую APK всё равно надо собрать

`expo-updates` — нативный модуль. Сборка, сделанная до его появления, о
канале обновлений ничего не знает и никогда ничего не получит. Поэтому
порядок такой: сначала §1 (ключ и токен) и сборка, дальше правки уезжают
без пересборки.

---

## 3. Что нужно, чтобы вход в собранной APK писал людей в базу

Три условия. Первое — уже сделано, два других вне мобилки.

### 3.1. `https` в адресе бэкенда ✅

Сделано 02.09.2026. Домен закрыт сертификатом, и nginx отвечает на `http://`
редиректом `301`. GET такой редирект переживает, **POST — нет**: клиент
повторяет запрос как GET и без тела. То есть на `http://` вход и регистрация
не сломались бы с ошибкой, а тихо перестали бы работать.

### 3.2. `app.google.client-ids` на сервере ❌

Пока не задан, `POST /api/v1/auth/google` отвечает `503`
(проверено 02.09.2026). Человек дойдёт до выбора аккаунта Google, приложение
получит `id_token`, а обмен упадёт. Полный разбор —
[GOOGLE_AUTH.md §6](./GOOGLE_AUTH.md).

На сервере (`/opt/uzcasting/application.properties`, туда же смотрит
`deploy/uzcasting.service`):

```sh
echo 'app.google.client-ids=497193534365-urqt7p6tufge2qpqhdns5qv8nvshjsqe.apps.googleusercontent.com,497193534365-03mh4geit6f6n13enrf2bqcs8dhcp1cb.apps.googleusercontent.com'   >> /opt/uzcasting/application.properties
systemctl restart uzcasting
```

Проверка снаружи — должно стать `401` вместо `503`:

```bash
curl -s -o /dev/null -w "%{http_code}
" -X POST https://uzcasting.com/api/v1/auth/google   -H "Content-Type: application/json" -d '{"idToken":"probe"}'
```

### 3.3. SHA-1 ключа сборки в Android-клиенте ❌

См. §1.2 выше.

### Про запись в базу

Со стороны бэкенда всё готово: `AuthServiceImpl.googleLogin` сам заводит
пользователя, если такого `googleSub` ещё нет, и привязывает Google к
существующему аккаунту, если совпал email (`createGoogleUser` / `linkGoogle`).
Вход по телефону работает так же — `verifyOtp` создаёт пользователя при
первом входе. Отдельной «регистрации» нет ни там, ни там.

SMS на сервере настроен: `POST /api/v1/app/auth/otp/send` с заведомо
несуществующим номером отвечает `SMS_SEND_FAILED`, а не `SMS_NOT_CONFIGURED`
(проверено 02.09.2026) — то есть ключи Eskiz на месте и запрос дошёл до него.
Живы ли сами ключи, покажет только настоящий номер.

⚠️ Проверить стоит одно: боевой сервер должен ходить в **PostgreSQL**, а не в
H2 из локального профиля. `deploy/uzcasting.service` запускается после
`postgresql.service`, то есть так и задумано, но сам конфиг лежит только на
сервере (`/opt/uzcasting/application.properties`) и в репозитории его нет.
Если там окажется H2 в памяти, все зарегистрированные исчезнут при первом же
рестарте — и заметят это не сразу.

```sh
grep -E 'datasource|jpa.hibernate.ddl-auto' /opt/uzcasting/application.properties
```

Ждём `jdbc:postgresql://...`. Строка вида `jdbc:h2:mem:` означает, что база
живёт до первого рестарта.
