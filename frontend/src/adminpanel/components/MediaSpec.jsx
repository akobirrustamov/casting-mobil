import { usePanelI18n } from '../i18n';
import { specLine, specNote } from '../mediaSpecs';

/**
 * Maydon yonidagi o'lcham talabi: «600×900 px · 2:3 · JPG/PNG/WebP · ≤2 MB».
 *
 * <h2>Nega maydonning YONIDA, yo'riqnomada emas</h2>
 * Yo'riqnoma ham bor (`help/guide.js` → «Rasm va video o'lchamlari»), lekin
 * uni fayl tanlash oldidan hech kim ochmaydi. O'lcham qaror qabul
 * qilinadigan JOYDA turishi kerak — «Yuklash» tugmasining yonida.
 *
 * <h2>Nega ogohlantirish rangi emas</h2>
 * Bu xato emas, talab. `uz-field-warn` sariq ramkasi adminni «nimadir
 * buzildi» deb o'ylatardi — u esa hali hech narsa qilmagan.
 *
 * ⚠️ Raqamlar TEKSHIRILMAYDI. Panel yuklangan faylning o'lchamini
 * o'lchamaydi va noto'g'ri o'lchamni rad etmaydi: mavjud kontentning
 * ko'p rasmi bu qoidadan oldin yuklangan va ularni almashtirishning
 * imkoni yo'q. Bu maslahat, cheklov emas.
 */
export default function MediaSpec({ name }) {
  const { locale } = usePanelI18n();

  const line = specLine(name);
  if (!line) return null;

  const note = specNote(name, locale);

  return (
    <div className="uz-spec">
      <span className="uz-spec-size">📐 {line}</span>
      {note && <span className="uz-spec-note">{note}</span>}
    </div>
  );
}
