/**
 * Kontent media bog'lanishlari ↔ forma o'rtasidagi o'girma.
 *
 * <h2>Nega alohida fayl</h2>
 * `ContentEditor` §53 bo'yicha 400 qatordan oshmasligi kerak va bu
 * mantiq unga sig'madi. Ajratish yaxshi ham tushdi: bu yerdagi qoidalar
 * muharrirning ko'rinishiga emas, BACKEND SHARTLARIGA bog'liq va ular
 * alohida sinaladi.
 *
 * <h2>⚠️ Asosiy shart</h2>
 * Backend saqlashda media ro'yxatini BUTUNLAY almashtiradi
 * (`content.getMedia().clear()`). Ya'ni forma yubormagan har qanday
 * bog'lanish O'CHADI — xato chiqmasdan.
 */

/**
 * Panel O'ZI boshqaradigan rollar.
 *
 * ⚠️ Bu ro'yxatda BO'LMAGAN har qanday rol «tegilmaydigan» deb
 * hisoblanadi va saqlashda o'zgarishsiz qaytariladi.
 *
 * Ro'yxatga rol qo'shsangiz — unga forma maydoni ham qo'shing, aks
 * holda u o'chib ketadi.
 */
export const MANAGED_ROLES = ['POSTER', 'COVER', 'GALLERY', 'VIDEO', 'TRAILER'];

/**
 * Xom ro'yxatdan bitta rolning umumiy (tilsiz) bog'lanishini oladi.
 *
 * ⚠️ Tilga bog'langanlari ATAYLAB o'tkazib yuboriladi: asosiy video va
 * treyler uchun panelda til tanlash yo'q. Ular yo'qolmaydi —
 * `passthroughMedia` ularni saqlab qoladi.
 */
export function pickMedia(media, role) {
  if (!Array.isArray(media)) {
    return null;
  }
  const found = media.find((m) => m.role === role && !m.locale);
  return found ? found.mediaId : null;
}

/**
 * Panel ko'rsatmaydigan bog'lanishlar — saqlashda QAYTARILADI.
 *
 * ⚠️ NEGA BU SHART
 * Ilgari muharrir DTO'dagi xom `media` ro'yxatini umuman O'QIMASDI.
 * Natijada `VIDEO` bog'lanishi bor kontentda sarlavhadagi bitta harfni
 * tuzatish ham filmning O'ZINI uzardi — panelda hech qanday xato
 * ko'rinmasdan.
 *
 * DTO'dagi `media` maydoni aynan shu uchun qo'shilgan edi, lekin
 * frontend unga hech qachon ulanmagan.
 */
export function passthroughMedia(media) {
  if (!Array.isArray(media)) {
    return [];
  }
  return media
    .filter((m) => !MANAGED_ROLES.includes(m.role)
                   // tilga bog'langan video/treyler ham shu yerda saqlanadi
                   || ((m.role === 'VIDEO' || m.role === 'TRAILER') && m.locale))
    .map((m) => ({
      role: m.role,
      locale: m.locale ?? undefined,
      mediaId: m.mediaId,
      sortOrder: m.sortOrder ?? 0,
    }));
}

/**
 * Formadan backend kutadigan media ro'yxatini yig'adi.
 *
 * ⚠️ Tartib raqami galereyada ro'yxatdagi joydan olinadi — admin
 * ko'rgan tartib aynan shunday saqlanadi.
 */
export function buildMediaLinks(form) {
  const media = [];

  if (form.posterDefault) {
    media.push({ role: 'POSTER', mediaId: form.posterDefault, sortOrder: 0 });
  }
  Object.entries(form.posterByLocale).forEach(([loc, id]) => {
    if (id) media.push({ role: 'POSTER', locale: loc, mediaId: id, sortOrder: 0 });
  });
  if (form.cover) {
    media.push({ role: 'COVER', mediaId: form.cover, sortOrder: 0 });
  }
  form.gallery.forEach((id, i) => media.push({ role: 'GALLERY', mediaId: id, sortOrder: i }));

  if (form.video) {
    media.push({ role: 'VIDEO', mediaId: form.video, sortOrder: 0 });
  }
  if (form.trailer) {
    media.push({ role: 'TRAILER', mediaId: form.trailer, sortOrder: 0 });
  }

  // ⚠️ Panel ko'rsatmaydigan rollar QAYTARILADI, aks holda o'chib ketardi.
  form.otherMedia.forEach((m) => media.push(m));

  return media;
}
