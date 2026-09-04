/**
 * ⚠️ ВРЕМЕННЫЕ ДАННЫЕ — НЕ ПРОДАКШЕН.
 *
 * Здесь остались ТОЛЬКО кастинг-объявления: эндпоинта для них нет ни в
 * старом API сайта, ни в новой платформе `/api/v1/app/**`. Удалить весь
 * файл, как только он появится.
 *
 * <h2>Что отсюда уже ушло</h2>
 * - Премьеры — главная и вкладка «Premyera» работают на `GET /api/v1/app/home`.
 * - Цены и тарифы Premium — их отдаёт `GET /api/v1/app/tariffs`
 *   (`src/features/subscription`), а цену конкретной серии присылает
 *   `/api/v1/app/watch/**` вместе с правом доступа.
 *
 * ⚠️ Захардкоженные цены были опасны не тем, что «временные», а тем, что
 * выглядели настоящими: заказчик меняет их в админке без релиза, и копия
 * в приложении молча начала бы врать. Ни один экран их больше не читал —
 * они просто лежали здесь после того, как появились тарифы с сервера.
 */

export type PlaceholderCasting = {
  id: string;
  title: string;
  location: string;
  deadline: string;
  paid: boolean;
};

export const CASTINGS: PlaceholderCasting[] = [
  {
    id: 'c1',
    title: 'Reklama roligi uchun yosh aktrisa kerak',
    location: 'Toshkent',
    deadline: '25.08.2026',
    paid: true,
  },
  {
    id: 'c2',
    title: "Klipga bosh rol ijodkori qidirilmoqda",
    location: 'Samarqand',
    deadline: '30.08.2026',
    paid: true,
  },
  {
    id: 'c3',
    title: 'Serial uchun massovka',
    location: 'Toshkent',
    deadline: '02.09.2026',
    paid: false,
  },
];
