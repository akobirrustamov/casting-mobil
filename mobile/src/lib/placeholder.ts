/**
 * ⚠️ ВРЕМЕННЫЕ ДАННЫЕ — НЕ ПРОДАКШЕН.
 *
 * Для премьер и кастинг-объявлений API пока нет (есть только каталог
 * креаторов — он подключён по-настоящему в features/creators).
 * Эти данные нужны, чтобы собрать и посмотреть главные экраны до контракта.
 *
 * Названия сериалов взяты из мокапов ТЗ (V4), цена 5 000 сум — из ТЗ.
 * Удалить целиком, как только появятся эндпоинты.
 */

export const EPISODE_PRICE = 5000;

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
