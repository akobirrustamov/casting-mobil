# Push bildirishnomalar

Admin panelda «Yuborish» bosilganda xabar ikki joyga ketadi:

1. **Ilovadagi «Xabarlar» ro'yxatiga.** Bu darhol ishlaydi va hech qanday sozlash talab qilmaydi.
2. **Telefonga push.** Expo Push Service orqali yuboriladi: Android'da FCM, iOS'da APNs.

## Qanday ishlaydi

```
Ilova (kirgandan keyin)
  └─ ruxsat so'raydi → Expo push token oladi
  └─ PUT /api/v1/app/devices/push-token   (joriy qurilmaga, X-Device-Id)

Admin → «Yuborish»
  └─ xabar SENT bo'ladi (ilovada ko'rinadi)
  └─ tranzaksiya yopilgach, fon oqimida:
       faol qurilmalar + token → auditoriya filtri (Premium) → foydalanuvchi tili
       → POST https://exp.host/--/api/v2/push/send (100 tadan)
       → natija: cms_notification.push_accepted / push_failed (hisobotda chiqadi)
       → «DeviceNotRegistered» bo'lgan tokenlar bazadan tozalanadi

Chiqish (signOut) → DELETE /api/v1/app/devices/push-token
```

- Kod: backend'da `Cms/Service/Push/*`, mobil ilovada `src/features/notifications/push.ts` va `pushToken.ts`.
- Migratsiya: `V42__device_push_token.sql`.
- Push bosilganda xabarning havolasi ochiladi: kontent, qism yoki ijodkor sahifasi, yoki tashqi URL. Havola bo'lmasa, «Xabarlar» ekrani ochiladi.

## ⚠️ Android uchun bir martalik sozlash (Firebase)

Android'da Expo tokeni faqat FCM orqali olinadi. Bu sozlashsiz ilova ishlayveradi va «Xabarlar» ro'yxati ham chiqadi, lekin telefonga push kelmaydi.

1. https://console.firebase.google.com → **Add project**. Mavjud Google Cloud loyihasini ham tanlash mumkin: `497193534365`, ya'ni Google Sign-In ishlatayotgan loyiha.
2. Loyihada **Add app → Android**. Package name: `uz.uzcasting.app`.
3. **`google-services.json`** faylini yuklab olib, **`mobile/google-services.json`** ga qo'ying va commit qiling. Bu sir emas, unda faqat ochiq identifikatorlar bor. `app.config.js` faylni topsa, o'zi ulaydi.
4. FCM V1 kalitini EAS'ga yuklang:
   - Firebase → Project settings → **Service accounts** → **Generate new private key**. JSON fayl yuklab olinadi.
   - Keyin quyidagini ishga tushiring:
     ```
     cd mobile
     npx eas-cli credentials -p android
     ```
     Menyuda: `preview` (yoki `production`) → **Google Service Account** → **Manage your Google Service Account Key for Push Notifications (FCM V1)** → **Upload** → yuklab olingan JSON'ni tanlang.
   - ⚠️ Bu kalit **sir**. Uni repozitoriyga qo'ymang (`.gitignore`ga tushmaydi, ehtiyot bo'ling).
5. **Yangi APK yig'ing.** `expo-notifications` native modul, shuning uchun OTA update yetmaydi:
   ```
   npx eas-cli build -p android --profile preview
   ```

## iOS (holat: 24.09.2026 — Apple Developer akkaunti hali yo'q)

Push kodi iOS'da ham o'zgarishsiz ishlaydi. Expo iPhone'ga APNs orqali yuboradi, Firebase **kerak emas**. `expo-notifications` plagini `aps-environment` ruxsatini o'zi qo'shadi.

Tayyor:
- `bundleIdentifier: uz.uzcasting.app`
- `ITSAppUsesNonExemptEncryption: false` — App Store har build'da shifrlash haqida so'ramaydi (ilova faqat oddiy HTTPS ishlatadi).

Qilinishi kerak:
1. **Apple Developer Program** — https://developer.apple.com/programs/enroll/ ($99/yil). Jismoniy shaxs yoki tashkilot sifatida ochiladi. Tashkilot uchun D-U-N-S raqami kerak bo'ladi.
2. **Google Sign-In (iOS).** Google Cloud Console (loyiha `497193534365`) → Credentials → **Create OAuth client ID → iOS**, Bundle ID `uz.uzcasting.app`. Undan ikki qiymat olinadi:
   - **Client ID** → EAS env `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID` (kod `src/features/auth/config.ts` uni allaqachon o'qiydi);
   - **iOS URL scheme** (`com.googleusercontent.apps....`) → `app.json` plaginlariga
     `["@react-native-google-signin/google-signin", { "iosUrlScheme": "..." }]`.

   Bu qilinmasa, iPhone'da Google orqali kirish ishlamaydi, telefon raqam + SMS orqali kirish esa ishlayveradi.
3. **Birinchi iOS build** — interaktiv, Apple ID + 2FA so'raladi. Sertifikat, provisioning va **push kaliti (APNs)** EAS'ning o'zi yaratadi:
   ```
   cd mobile
   npx eas-cli build -p ios --profile production
   ```
   «Generate a new Apple Push Notifications service key?» deb so'raganda — **Yes**.
4. Sinov: `npx eas-cli submit -p ios` → TestFlight. `preview` (internal) profilida esa har bir iPhone'ni oldindan ro'yxatdan o'tkazish kerak (`npx eas-cli device:create`).

## O'qilgan / o'qilmagan va logo (25.09.2026)

- Qo'ng'iroqchada qizil belgi — o'qilmaganlar soni: `GET /api/v1/app/notifications/unread-count`. Push kelganda (ilova ochiq bo'lsa) va ilovaga qaytilganda yangilanadi.
- «Xabarlar» ochilganda hammasi o'qilgan bo'ladi: `POST /api/v1/app/notifications/read`. Push bosilib to'g'ridan-to'g'ri havolaga o'tilsa, faqat o'sha xabar o'qilgan bo'ladi: `POST /api/v1/app/notifications/{id}/read`.
- Belgilar `cms_notification_read` jadvalida (`V43`) saqlanadi. Ular odamga tegishli, qurilmaga emas.
- Telefondagi logo:
  - **Kichik ikonka** (`assets/notification-icon.png`) — oq siluet. Android uni `app.json`dagi `color` bilan bo'yaydi. Oldin rang `#05050A` edi, qorong'i panelda logo ko'rinmasdi. Endi `#A855F7`. ⚠️ Bu native sozlama, shuning uchun **yangi APK kerak**.
  - **Rangli logo** xabarning o'ng tomonida rasm bo'lib chiqadi: backend Expo'ga `richContent.image` yuboradi. Admin rasm biriktirgan bo'lsa o'sha rasm, bo'lmasa `https://uzcasting.com/logo.png`. Manzilni `app.push.public-base-url` va `app.push.logo-path` bilan o'zgartirish mumkin. Bu faqat Android'da ishlaydi. iOS'da rasm uchun Notification Service Extension kerak.

## Server

Qo'shimcha sozlash kerak emas: push sukut bo'yicha yoqilgan. Ixtiyoriy parametrlar (`application.properties`):

```
app.push.enabled=true                  # false — push o'chiriladi, «Xabarlar» ishlayveradi
app.push.expo.access-token=            # faqat Expo'da «Enhanced push security» yoqilsa
```

Deploy qilinganda Flyway `V42` ni o'zi qo'llaydi.

## Tekshirish

1. Yangi APK'ni telefonga o'rnating, kiring va bildirishnomaga ruxsat bering.
2. Bazada token paydo bo'lganini tekshiring:
   `select device_name, push_token from cms_user_device where push_token is not null;`
3. Admin panel → Bildirishnomalar → uchala tilda xabar yozing → **Yuborish**.
4. Hisobotda **Yuborildi** ustunida Expo qabul qilgan qurilmalar soni chiqadi. Push o'tmasa, xato sababi ham xabar kartochkasida ko'rinadi.

Serversiz, qo'lda tekshirish (tokenni bazadan oling):

```
curl -H "Content-Type: application/json" -X POST https://exp.host/--/api/v2/push/send \
  -d '{"to":"ExponentPushToken[...]","title":"Test","body":"Salom","channelId":"default"}'
```

## Nimalar o'lchanmaydi

**«Yetkazildi»** o'lchanmaydi. Expo'ning kvitansiyalari (receipts) yig'ilmaydi, shuning uchun hisobotda «o'lchanmaydi» deb ko'rsatiladi. «Yuborildi» — Expo qabul qilgan qurilmalar soni. «Ochildi» va «bosildi» esa haqiqiy analitika hodisalari: `NOTIFICATION_OPEN` va `NOTIFICATION_CLICK`.
