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
