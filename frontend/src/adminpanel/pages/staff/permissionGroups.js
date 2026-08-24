/**
 * WORKER uchun ruxsatlar — mavzular bo'yicha guruhlangan (ТЗ §12).
 *
 * <h2>Nega guruh kerak</h2>
 * Ruxsatlar 40 dan ortiq. Ularni bitta ustunda alifbo bo'yicha
 * ko'rsatish admin uchun foydasiz edi: «bu odam kontent bilan ishlaydi»
 * degan qaror bitta belgilash emas, o'nlab qutichani ro'yxatdan izlab
 * topish bo'lardi. Guruh esa qarorni real vazifaga bog'laydi.
 *
 * ⚠️ Bu ro'yxat backenddagi `Enums/Permission.java` ning AYNAN o'zi
 * bo'lishi shart. Agar backendga yangi ruxsat qo'shilsayu shu yerga
 * qo'shilmasa — u panel orqali HECH QACHON berib bo'lmaydigan huquqqa
 * aylanadi va buni hech kim sezmaydi. Shuning uchun
 * `__tests__/permissionGroups.test.js` ikkala ro'yxatni solishtiradi.
 */
export const PERMISSION_GROUPS = [
  {
    key: 'content',
    permissions: [
      'CONTENT_VIEW', 'CONTENT_CREATE', 'CONTENT_EDIT',
      'CONTENT_DELETE', 'CONTENT_PUBLISH',
    ],
  },
  {
    key: 'taxonomy',
    permissions: [
      'CATEGORY_VIEW', 'CATEGORY_CREATE', 'CATEGORY_EDIT', 'CATEGORY_DELETE',
      'GENRE_VIEW', 'GENRE_CREATE', 'GENRE_EDIT', 'GENRE_DELETE',
    ],
  },
  {
    key: 'creators',
    permissions: ['CREATOR_VIEW', 'CREATOR_CREATE', 'CREATOR_EDIT'],
  },
  {
    key: 'media',
    permissions: ['MEDIA_VIEW', 'MEDIA_UPLOAD', 'MEDIA_DELETE'],
  },
  {
    key: 'homepage',
    permissions: [
      'HOMEPAGE_VIEW', 'HOMEPAGE_EDIT',
      'ADVERTISEMENT_VIEW', 'ADVERTISEMENT_CREATE',
      'ADVERTISEMENT_EDIT', 'ADVERTISEMENT_DELETE',
      'PREMIERE_VIEW', 'PREMIERE_CREATE', 'PREMIERE_EDIT', 'PREMIERE_DELETE',
    ],
  },
  {
    key: 'engagement',
    permissions: [
      'COMMENT_VIEW', 'COMMENT_MODERATE',
      'NOTIFICATION_VIEW', 'NOTIFICATION_CREATE', 'NOTIFICATION_SEND',
    ],
  },
  {
    key: 'users',
    permissions: [
      'USER_VIEW', 'USER_BLOCK', 'USER_PREMIUM_MANAGE', 'USER_DEVICE_MANAGE',
    ],
  },
  {
    key: 'money',
    permissions: [
      'TARIFF_VIEW', 'TARIFF_EDIT', 'SUBSCRIPTION_VIEW',
      'DONATION_VIEW', 'DONATION_PACKAGE_EDIT',
    ],
  },
  {
    key: 'system',
    permissions: ['SETTINGS_VIEW', 'SETTINGS_EDIT', 'REPORT_VIEW'],
  },
];

/** Barcha ruxsatlar — tekis ro'yxat. */
export const ALL_PERMISSIONS = PERMISSION_GROUPS.flatMap((g) => g.permissions);
