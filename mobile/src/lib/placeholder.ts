/**
 * ⚠️ ВРЕМЕННЫЕ ДАННЫЕ — НЕ ПРОДАКШЕН.
 *
 * Для премьер и кастинг-объявлений API пока нет (есть только каталог
 * креаторов — он подключён по-настоящему в features/creators).
 * Эти данные нужны, чтобы собрать и посмотреть главные экраны до контракта.
 *
 * Названия сериалов взяты из мокапов ТЗ (V4).
 * Удалить целиком, как только появятся эндпоинты.
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

export type PlaceholderPremiere = {
  id: string;
  title: string;
  episode: string;
  purchased: boolean;
};

export const PREMIERES: PlaceholderPremiere[] = [
  { id: 'p1', title: 'Qalbim egasi', episode: '2-qism', purchased: false },
  { id: 'p2', title: 'Shahar soyasida', episode: '3-qism', purchased: false },
  { id: 'p3', title: 'Meni kechir', episode: '4-qism', purchased: true },
  { id: 'p4', title: 'Orzular ortida', episode: '5-qism', purchased: false },
  { id: 'p5', title: "Yuragim iztirobi", episode: '4-qism', purchased: false },
];

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
