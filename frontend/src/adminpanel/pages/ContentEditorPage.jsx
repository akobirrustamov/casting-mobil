/**
 * Kontent muharririning MARSHRUT qobig'i.
 *
 * ⚠️ Muharrir ilgari ro'yxat ustida modal bo'lib ochilardi. Lekin uning
 * ichida media tanlash, ijodkor qo'shish va video ko'rish — hammasi
 * o'z modalini ochadi. Natijada oyna ustida oyna paydo bo'lardi: orqa
 * fon ikki qavat qorayardi, Escape qaysi oynani yopishi tushunarsiz
 * edi va uzun forma kichkina oynaga sig'masdi.
 *
 * Endi muharrir ALOHIDA sahifa (`/app/panel/content/new`,
 * `/app/panel/content/:contentId`), modal esa faqat uning ichidagi
 * tanlovlar uchun qoladi — ya'ni bir vaqtda faqat bitta oyna.
 *
 * Yopilganda ro'yxatga qaytamiz va uning FILTRLARINI tiklaymiz:
 * ular manzil qatorida saqlanadi (`ContentPage`), shuning uchun
 * ochishda `state.from` orqali uzatiladi.
 */
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import ContentEditor from './ContentEditor';

export default function ContentEditorPage() {
  const { contentId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const backTo = `/app/panel/content${location.state?.from || ''}`;

  return (
    <ContentEditor
      open
      contentId={contentId ? Number(contentId) : null}
      onClose={() => navigate(backTo)}
      // Ro'yxat qaytib kelganda o'zi qayta so'rov yuboradi —
      // bu yerda alohida yangilash kerak emas.
      onSaved={() => {}}
    />
  );
}
