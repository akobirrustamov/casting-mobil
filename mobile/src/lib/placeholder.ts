/**
 * ⚠️ ВРЕМЕННЫЕ ДАННЫЕ — НЕ ПРОДАКШЕН.
 *
 * Осталось только для кастинг-объявлений: эндпоинта для них нет ни в старом
 * API сайта, ни в новой платформе `/api/v1/app/**`. Удалить, как только он
 * появится.
 *
 * Премьеры отсюда убраны — главная и вкладка «Premyera» работают на
 * `GET /api/v1/app/home` (`src/features/home`).
 *
 * Цены ниже — не заглушка: это цифры заказчика от 13.08.2026, они понадобятся
 * экрану Premium. Цена конкретного контента приходит с бэкенда вместе с правом
 * доступа (`/api/v1/app/watch/{episodeId}`) и здесь не выдумывается.
 */

/**
 * Цены — из сообщения заказчика от 13.08.2026, см. docs/MONETIZATION.md.
 * ⚠️ В ТЗ и на мокапах стоит 5 000 за серию — эта цифра устарела.
 */
export const EPISODE_PRICE = 3000;
export const PREMIERE_PRICE = 15000;

/** Тарифы Premium. */
export const PREMIUM_PLANS = [
  { id: 'm1', months: 1, price: 24000, best: false },
  { id: 'm3', months: 3, price: 49999, best: false },
  { id: 'm6', months: 6, price: 99000, best: false },
  { id: 'y1', months: 12, price: 159900, best: true },
] as const;

/** Пакеты UzCasting Stars — донаты креаторам. */
export const STARS_PACKS = [10, 50, 100, 500, 1000] as const;

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
